package com.soumyajit.gradlemc.task;

import java.util.List;
import java.util.EnumSet;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public final class TaskEngineSelfTest {
    private TaskEngineSelfTest() {
    }

    public static void run() {
        TaskEngine engine = new TaskEngine(List.of(
                planningDefinition("runtime", List.of(), "runtime fingerprint"),
                planningDefinition("memory", List.of("runtime"), "memory snapshot"),
                planningDefinition("mods", List.of("runtime"), "mod metadata"),
                planningDefinition("release", List.of("mods", "memory"), "release plan")
        ));
        require(engine.plan("release").equals(List.of("runtime", "memory", "mods", "release")), "task order must be deterministic");
        boolean cycleRejected = false;
        try {
            new TaskEngine(List.of(planningDefinition("a", List.of("b"), ""), planningDefinition("b", List.of("a"), "")));
        } catch (IllegalArgumentException expected) {
            cycleRejected = true;
        }
        require(cycleRejected, "task cycles must be rejected");
        boolean duplicateRejected = false;
        try { new TaskEngine(List.of(planningDefinition("a", List.of(), ""), planningDefinition("a", List.of(), ""))); }
        catch (IllegalArgumentException expected) { duplicateRejected = true; }
        require(duplicateRejected, "duplicate IDs must be rejected");
        boolean missingRequiredRejected = false;
        try { new TaskEngine(List.of(planningDefinition("missing", List.of("absent"), ""))); }
        catch (IllegalArgumentException expected) { missingRequiredRejected = true; }
        require(missingRequiredRejected, "missing required dependencies must be rejected");
        boolean selfDependencyRejected = false;
        try { new TaskEngine(List.of(planningDefinition("self", List.of("self"), ""))); }
        catch (IllegalArgumentException expected) { selfDependencyRejected = true; }
        require(selfDependencyRejected, "self dependencies must be rejected as cycles");

        ThreadPoolExecutor worker = new ThreadPoolExecutor(1, 1, 0, TimeUnit.MILLISECONDS, new ArrayBlockingQueue<>(2));
        try {
            TaskEngine executionRegistry = new TaskEngine(List.of(
                    definition("a", List.of(), context -> TaskOutcome.success(List.of())),
                    definition("b", List.of(TaskDependency.required("a")), context -> TaskOutcome.partial(List.of(), "limited sample")),
                    definition("c", List.of(TaskDependency.required("b")), context -> TaskOutcome.success(List.of()))
            ));
            WorkflowEngine workflowEngine = new WorkflowEngine(executionRegistry, worker);
            WorkflowPlan workflowPlan = workflowEngine.plan(new WorkflowDefinition("synthetic", "Synthetic", "", List.of("c"), java.time.Duration.ofSeconds(1)));
            require(workflowPlan.orderedTasks().stream().map(DiagnosticTask::id).toList().equals(List.of("a", "b", "c")), "workflow plan must reuse deterministic graph");
            WorkflowResult partial = workflowEngine.execute(workflowPlan, Map.of(), EnumSet.of(TaskEnvironment.ANY), new CancellationToken());
            require(partial.state() == TaskState.PARTIAL, "partial collector result must remain partial");

            TaskEngine optionalRegistry = new TaskEngine(List.of(definition("optional_root", List.of(TaskDependency.optional("not_present")), context -> TaskOutcome.success(List.of()))));
            WorkflowPlan optionalPlan = new WorkflowEngine(optionalRegistry, worker).plan(new WorkflowDefinition("optional", "Optional", "", List.of("optional_root"), java.time.Duration.ofSeconds(1)));
            require(optionalPlan.plannedSkips().containsKey("optional_root:not_present"), "optional missing dependencies must be explained in the plan");

            TaskEngine failureRegistry = new TaskEngine(List.of(
                    definition("a", List.of(), context -> new TaskOutcome(TaskState.FAILED, List.of(), List.of(), List.of("failed"), "failed")),
                    definition("b", List.of(TaskDependency.required("a")), context -> TaskOutcome.success(List.of()))
            ));
            WorkflowEngine failedEngine = new WorkflowEngine(failureRegistry, worker);
            WorkflowResult failed = failedEngine.execute(failedEngine.plan(new WorkflowDefinition("failure", "Failure", "", List.of("b"), java.time.Duration.ofSeconds(1))), Map.of(), EnumSet.of(TaskEnvironment.ANY), new CancellationToken());
            require(failed.taskResults().get(1).state() == TaskState.SKIPPED, "failed dependencies must suppress dependents");

            CancellationToken cancelled = new CancellationToken(); require(cancelled.cancel("test-cancel"), "first cancellation request must win"); require(!cancelled.cancel("again"), "repeated cancellation must be idempotent");
            WorkflowResult cancelledResult = workflowEngine.execute(workflowPlan, Map.of(), EnumSet.of(TaskEnvironment.ANY), cancelled);
            require(cancelledResult.state() == TaskState.CANCELLED, "manual cancellation must be terminal and idempotent");

            TaskEngine timeoutRegistry = new TaskEngine(List.of(definition("slow", List.of(), context -> { while (true) context.checkpoint(); })));
            WorkflowEngine timeoutEngine = new WorkflowEngine(timeoutRegistry, worker);
            WorkflowResult timedOut = timeoutEngine.execute(timeoutEngine.plan(new WorkflowDefinition("timeout", "Timeout", "", List.of("slow"), java.time.Duration.ofMillis(50))), Map.of(), EnumSet.of(TaskEnvironment.ANY), new CancellationToken());
            require(timedOut.state() == TaskState.TIMED_OUT, "task timeout must cancel bounded work before success is reported");
            WorkflowResult rerunAfterTimeout = workflowEngine.execute(workflowPlan, Map.of(), EnumSet.of(TaskEnvironment.ANY), new CancellationToken());
            require(rerunAfterTimeout.state() == TaskState.PARTIAL, "a second run must work after timeout cleanup");

            ThreadPoolExecutor rejectedWorker = new ThreadPoolExecutor(1, 1, 0, TimeUnit.MILLISECONDS, new ArrayBlockingQueue<>(1));
            rejectedWorker.shutdownNow();
            WorkflowEngine rejectedEngine = new WorkflowEngine(executionRegistry, rejectedWorker);
            WorkflowResult rejected = rejectedEngine.execute(rejectedEngine.plan(new WorkflowDefinition("rejected", "Rejected", "", List.of("a"), java.time.Duration.ofSeconds(1))), Map.of(), EnumSet.of(TaskEnvironment.ANY), new CancellationToken());
            require(rejected.state() == TaskState.FAILED && rejected.taskResults().get(0).errors().stream().anyMatch(value -> value.contains("rejected")),
                    "executor rejection must become a truthful terminal failure");
        } finally { worker.shutdownNow(); }
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private static DiagnosticTask definition(String id, List<TaskDependency> dependencies, DiagnosticTaskAction action) {
        return new DiagnosticTask(id, id, id, "test", TaskEnvironment.ANY, dependencies, java.time.Duration.ofMillis(100), true, List.of(), action);
    }

    private static DiagnosticTask planningDefinition(String id, List<String> dependencies, String description) {
        return definition(id, dependencies.stream().map(TaskDependency::required).toList(),
                context -> TaskOutcome.unavailable(description));
    }
}
