package com.soumyajit.gradlemc.task;

import com.soumyajit.gradlemc.GradleMC;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.TreeMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/** Sequential workflow runner: dependencies are deterministic; task calls are bounded by real Future timeouts. */
public final class WorkflowEngine {
    private final TaskEngine registry;
    private final ExecutorService taskExecutor;

    public WorkflowEngine(TaskEngine registry, ExecutorService taskExecutor) {
        this.registry = registry;
        this.taskExecutor = taskExecutor;
    }

    public WorkflowPlan plan(String workflowId) {
        WorkflowDefinition workflow = DiagnosticWorkflows.workflow(workflowId);
        if (workflow == null) throw new IllegalArgumentException("Unknown workflow: " + workflowId);
        return plan(workflow);
    }

    public WorkflowPlan plan(WorkflowDefinition workflow) {
        List<DiagnosticTask> tasks = registry.planAll(workflow.rootTaskIds());
        Map<String, String> skips = new TreeMap<>();
        tasks.forEach(task -> task.dependencies().stream().filter(dependency -> !dependency.required() && registry.task(dependency.id()) == null)
                .forEach(dependency -> skips.put(task.id() + ":" + dependency.id(), "optional dependency is not registered")));
        List<EvidenceType> evidence = tasks.stream().flatMap(task -> task.evidenceTypes().stream()).distinct().sorted(Comparator.comparing(Enum::name)).toList();
        return new WorkflowPlan(workflow.id(), Instant.now(), tasks, skips, evidence, workflow.timeout().toNanos());
    }

    public WorkflowResult execute(WorkflowPlan plan, Map<String, String> snapshot, EnumSet<TaskEnvironment> available, CancellationToken cancellation) {
        long startedNanos = System.nanoTime();
        Instant startedAt = Instant.now();
        List<TaskResult> results = new ArrayList<>();
        Map<String, TaskResult> byId = new HashMap<>();
        TaskState finalState = TaskState.SUCCEEDED;
        for (DiagnosticTask task : plan.orderedTasks()) {
            long remaining = plan.timeoutNanos() - (System.nanoTime() - startedNanos);
            TaskResult result;
            if (cancellation.isCancelled()) {
                result = terminal(task, TaskState.CANCELLED, "", cancellation.reason(), List.of(), List.of());
            } else if (remaining <= 0) {
                cancellation.cancel("workflow-timeout");
                result = terminal(task, TaskState.TIMED_OUT, "", "Workflow deadline elapsed", List.of(), List.of());
            } else if (!available.contains(task.environment()) && task.environment() != TaskEnvironment.ANY) {
                result = terminal(task, TaskState.UNAVAILABLE, task.environment().name().toLowerCase() + " required", "", List.of(), List.of());
            } else if (requiredDependencyFailed(task, byId)) {
                result = terminal(task, TaskState.SKIPPED, "required dependency did not complete", "", List.of(), List.of());
            } else {
                result = executeTask(task, snapshot, cancellation, Math.min(remaining, task.timeout().toNanos()));
            }
            results.add(result);
            byId.put(task.id(), result);
            finalState = combine(finalState, result.state());
        }
        return new WorkflowResult(UUID.randomUUID().toString(), plan, finalState, startedAt, Instant.now(), System.nanoTime() - startedNanos,
                results, cancellation.reason(), "workflow-engine-v1; monotonic deadlines; immutable input snapshot");
    }

    private TaskResult executeTask(DiagnosticTask task, Map<String, String> snapshot, CancellationToken cancellation, long allowedNanos) {
        Instant started = Instant.now(); long startedNanos = System.nanoTime();
        final Future<TaskOutcome> future;
        try {
            future = taskExecutor.submit(() -> task.action().execute(new TaskExecutionContext(snapshot, cancellation, System.nanoTime() + allowedNanos)));
        } catch (RejectedExecutionException rejected) {
            return terminal(task, TaskState.FAILED, "", "Task executor rejected work", List.of(), List.of());
        }
        try {
            TaskOutcome outcome = future.get(Math.max(1, allowedNanos), TimeUnit.NANOSECONDS);
            return new TaskResult(task.id(), outcome.state(), started, Instant.now(), System.nanoTime() - startedNanos,
                    outcome.evidence(), outcome.warnings(), outcome.errors(), "", outcome.state() == TaskState.UNAVAILABLE ? outcome.reason() : "", task.environment(), "task:" + task.id());
        } catch (TimeoutException timeout) {
            cancellation.cancel("task-timeout:" + task.id()); future.cancel(true);
            return terminal(task, TaskState.TIMED_OUT, "", "Task deadline elapsed", List.of(), List.of());
        } catch (ExecutionException execution) {
            Throwable cause = execution.getCause();
            if (cause instanceof DiagnosticCancelledException) return terminal(task, TaskState.CANCELLED, "", cancellation.reason(), List.of(), List.of());
            if (cause instanceof TaskTimeoutException) return terminal(task, TaskState.TIMED_OUT, "", "Task checkpoint deadline elapsed", List.of(), List.of());
            GradleMC.LOGGER.warn("GradleMC workflow task {} failed", task.id(), cause == null ? execution : cause);
            return terminal(task, TaskState.FAILED, "", cause == null ? "Task failed" : cause.getClass().getSimpleName() + ": " + safe(cause.getMessage()), List.of(), List.of());
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt(); cancellation.cancel("workflow-interrupted"); future.cancel(true);
            return terminal(task, TaskState.CANCELLED, "", cancellation.reason(), List.of(), List.of());
        }
    }

    private static boolean requiredDependencyFailed(DiagnosticTask task, Map<String, TaskResult> results) {
        return task.dependencies().stream().filter(TaskDependency::required).map(TaskDependency::id).map(results::get)
                .anyMatch(result -> result == null || !result.completedSuccessfully());
    }
    private static TaskResult terminal(DiagnosticTask task, TaskState state, String missing, String detail, List<DiagnosticEvidence> evidence, List<String> warnings) {
        Instant now = Instant.now();
        return new TaskResult(task.id(), state, now, now, 0, evidence, warnings, detail.isBlank() ? List.of() : List.of(detail),
                state == TaskState.CANCELLED || state == TaskState.TIMED_OUT ? detail : "", missing, task.environment(), "task:" + task.id());
    }
    private static TaskState combine(TaskState current, TaskState next) {
        if (next == TaskState.TIMED_OUT) return TaskState.TIMED_OUT;
        if (next == TaskState.CANCELLED && current != TaskState.TIMED_OUT) return TaskState.CANCELLED;
        if (next == TaskState.FAILED && current != TaskState.TIMED_OUT && current != TaskState.CANCELLED) return TaskState.FAILED;
        if ((next == TaskState.PARTIAL || next == TaskState.SKIPPED || next == TaskState.UNAVAILABLE) && current == TaskState.SUCCEEDED) return TaskState.PARTIAL;
        return current;
    }
    private static String safe(String value) { return value == null ? "" : value; }
}
