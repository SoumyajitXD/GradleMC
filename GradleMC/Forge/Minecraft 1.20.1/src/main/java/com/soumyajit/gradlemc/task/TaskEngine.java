package com.soumyajit.gradlemc.task;

import com.soumyajit.gradlemc.incident.IncidentRecorder;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.*;

/** Sequential, bounded task orchestrator. Runtime diagnostics are never cached unless declared static. */
public final class TaskEngine {
    private static final int MAX_HISTORY_PER_TASK = 32;
    private final Map<String, DiagnosticTask> tasks = new TreeMap<>();
    private final Map<String, Cached> cache = new HashMap<>();
    private final Map<String, Map<String, String>> lastInputs = new HashMap<>();
    private final Map<String, ArrayDeque<TaskResult>> history = new HashMap<>();

    public synchronized void register(DiagnosticTask task) {
        Objects.requireNonNull(task, "task");
        if (!task.id().matches("[a-z0-9_.-]+:[a-z0-9_.-]+")) {
            throw new IllegalArgumentException("Invalid namespaced diagnostic task ID: " + task.id());
        }
        if (task.timeoutMillis() < 1) throw new IllegalArgumentException("Task timeout must be positive: " + task.id());
        if (tasks.putIfAbsent(task.id(), task) != null) throw new IllegalArgumentException("Duplicate diagnostic task ID: " + task.id());
    }

    public synchronized Collection<DiagnosticTask> tasks() { return List.copyOf(tasks.values()); }

    public synchronized Optional<DiagnosticTask> task(String id) { return Optional.ofNullable(tasks.get(id)); }

    public synchronized TaskPlan plan(String requestedId) {
        List<DiagnosticTask> result = new ArrayList<>();
        Set<String> visiting = new LinkedHashSet<>(), done = new HashSet<>();
        visit(requestedId, true, visiting, done, result);
        return new TaskPlan(requestedId, result);
    }

    private void visit(String id, boolean required, Set<String> visiting, Set<String> done, List<DiagnosticTask> result) {
        if (done.contains(id)) return;
        DiagnosticTask task = tasks.get(id);
        if (task == null) {
            if (!required) return;
            throw new IllegalArgumentException("Missing diagnostic task dependency: " + id);
        }
        if (!visiting.add(id)) throw new IllegalArgumentException("Diagnostic task cycle: " + String.join(" -> ", visiting) + " -> " + id);
        for (TaskDependency dep : task.dependencies()) visit(dep.taskId(), dep.required(), visiting, done, result);
        visiting.remove(id);
        done.add(id);
        result.add(task);
    }

    public synchronized List<TaskResult> execute(TaskPlan plan, TaskRunContext context, boolean dryRun) {
        List<TaskResult> results = new ArrayList<>();
        Map<String, TaskResult> byId = new HashMap<>();
        long runStarted = System.nanoTime();
        for (int index = 0; index < plan.orderedTasks().size(); index++) {
            DiagnosticTask task = plan.orderedTasks().get(index);
            Instant start = Instant.now();
            long taskStarted = System.nanoTime();
            Map<String, String> inputs = safeInputs(task, context);
            String fingerprint = fingerprint(inputs);
            Map<String, String> changes = changedInputs(cache.get(task.id()), lastInputs.get(task.id()), inputs);
            TaskResult result;
            String budgetReason = budgetReason(index, runStarted, context);
            if (!budgetReason.isEmpty()) {
                result = result(task, TaskState.SKIPPED, "budget-reached", "Partial run: " + budgetReason,
                        start, fingerprint, false, Map.of(), changes, overhead(taskStarted, context, context.counters(), Map.of(), false, true, budgetReason));
            } else if (context.cancelled()) {
                result = result(task, TaskState.CANCELLED, "cancelled", "Run was cancelled", start, fingerprint,
                        false, Map.of(), changes, overhead(taskStarted, context, context.counters(), Map.of(), false, false, ""));
            } else if (task.requiresServer() && context.server() == null) {
                result = result(task, TaskState.UNAVAILABLE, "server-required", "This task requires an active server",
                        start, fingerprint, false, Map.of(), changes, overhead(taskStarted, context, context.counters(), Map.of(), false, false, ""));
            } else if (blocked(task, byId)) {
                result = result(task, TaskState.SKIPPED, "required-dependency-not-successful", "A required dependency did not complete",
                        start, fingerprint, false, Map.of(), changes, overhead(taskStarted, context, context.counters(), Map.of(), false, false, ""));
            } else if (dryRun) {
                result = result(task, TaskState.PLANNED, "dry-run", "Planned; no diagnostic executed", start,
                        fingerprint, false, Map.of(), changes, overhead(taskStarted, context, context.counters(), Map.of(), false, false, ""));
            } else {
                Cached previous = cache.get(task.id());
                if (!context.rerun() && task.cachePolicy() == CachePolicy.STATIC_INPUTS && previous != null && previous.fingerprint.equals(fingerprint)) {
                    context.record(task.id(), previous.outcome);
                    result = result(task, TaskState.UP_TO_DATE, "static-inputs-match", "Reused bounded session result with matching declared inputs",
                            start, fingerprint, true, previous.outcome.outputs(), Map.of(), overhead(taskStarted, context, context.counters(), previous.outcome.outputs(), false, false, ""));
                } else {
                    result = run(task, context, start, taskStarted, fingerprint, inputs, changes);
                }
            }
            results.add(result);
            byId.put(task.id(), result);
            remember(result);
            lastInputs.put(task.id(), inputs);
            if (result.state() == TaskState.FAILED || result.state() == TaskState.TIMED_OUT) {
                IncidentRecorder.instance().trigger("diagnostic-task-failure",
                        Map.of("task", result.taskId(), "state", result.state().name(), "reason", result.reason()),
                        List.of("task:" + result.taskId()));
            }
        }
        return List.copyOf(results);
    }

    private TaskResult run(DiagnosticTask task, TaskRunContext context, Instant start, long startedNanos,
                           String fingerprint, Map<String, String> inputs, Map<String, String> changes) {
        TaskRunContext.Counters before = context.counters();
        long memoryBefore = usedMemory();
        try {
            TaskOutcome outcome = Objects.requireNonNull(task.execute(context), "Task returned null outcome");
            long elapsed = System.nanoTime() - startedNanos;
            boolean timedOut = elapsed > task.timeoutMillis() * 1_000_000L;
            String budget = context.budgetExceeded();
            boolean truncated = !budget.isEmpty();
            TaskState state = timedOut ? TaskState.TIMED_OUT : outcome.state();
            String reason = timedOut ? "timeout" : truncated && state == TaskState.SUCCESS ? "budget-reached" : outcome.reason();
            String message = timedOut ? "Task exceeded its " + task.timeoutMillis() + " ms timeout"
                    : truncated && state == TaskState.SUCCESS ? outcome.message() + " Partial result: " + budget : outcome.message();
            TaskOutcome recorded = new TaskOutcome(state, reason, message, outcome.outputs());
            context.record(task.id(), recorded);
            if (state == TaskState.SUCCESS && !truncated && task.cachePolicy() == CachePolicy.STATIC_INPUTS) {
                cache.put(task.id(), new Cached(fingerprint, inputs, outcome));
            }
            TaskOverhead overhead = overhead(startedNanos, context, before, outcome.outputs(), timedOut, truncated, budget,
                    Math.max(0, usedMemory() - memoryBefore));
            return result(task, state, reason, message, start, fingerprint, false, outcome.outputs(), changes, overhead);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            return result(task, TaskState.CANCELLED, "interrupted", "Task thread was interrupted", start, fingerprint,
                    false, Map.of(), changes, overhead(startedNanos, context, before, Map.of(), false, false, ""));
        } catch (Exception exception) {
            String message = exception.getMessage() == null ? exception.getClass().getSimpleName() : exception.getClass().getSimpleName() + ": " + exception.getMessage();
            return result(task, TaskState.FAILED, "exception", message, start, fingerprint, false, Map.of(), changes,
                    overhead(startedNanos, context, before, Map.of(), false, false, ""));
        }
    }

    public synchronized TaskExplanation explain(String requestedId, String taskId, TaskRunContext context) {
        TaskPlan plan = plan(requestedId);
        DiagnosticTask task = plan.orderedTasks().stream().filter(t -> t.id().equals(taskId)).findFirst()
                .orElseThrow(() -> new IllegalArgumentException(taskId + " is not included in " + requestedId));
        List<String> path = dependencyPath(requestedId, taskId);
        Map<String, String> inputs = safeInputs(task, context);
        Cached previous = cache.get(taskId);
        Map<String, String> changes = changedInputs(previous, lastInputs.get(taskId), inputs);
        List<String> missing = new ArrayList<>();
        if (task.requiresServer() && context.server() == null) missing.add("active-server");
        TaskState state;
        String reason;
        if (!missing.isEmpty()) { state = TaskState.UNAVAILABLE; reason = "Missing capability: " + String.join(", ", missing); }
        else if (!context.rerun() && task.cachePolicy() == CachePolicy.STATIC_INPUTS && previous != null && changes.isEmpty()) { state = TaskState.UP_TO_DATE; reason = "Declared static inputs match the cached session result"; }
        else { state = TaskState.PLANNED; reason = previous == null ? "No prior cache entry" : context.rerun() ? "Rerun was explicitly requested" : task.cachePolicy() == CachePolicy.NEVER_CACHE ? "Runtime task policy forbids reuse" : "Declared inputs changed"; }
        return new TaskExplanation(requestedId, taskId, path, state, reason, changes, missing);
    }

    public synchronized List<TaskResult> history(String taskId) {
        ArrayDeque<TaskResult> values = history.get(taskId);
        return values == null ? List.of() : List.copyOf(values);
    }

    private List<String> dependencyPath(String requestedId, String target) {
        if (requestedId.equals(target)) return List.of(target);
        List<String> path = new ArrayList<>();
        if (findPath(requestedId, target, new HashSet<>(), path)) return path;
        return List.of(requestedId, target);
    }

    private boolean findPath(String current, String target, Set<String> seen, List<String> path) {
        if (!seen.add(current)) return false;
        path.add(current);
        if (current.equals(target)) return true;
        DiagnosticTask task = tasks.get(current);
        if (task != null) for (TaskDependency dependency : task.dependencies()) if (findPath(dependency.taskId(), target, seen, path)) return true;
        path.remove(path.size() - 1);
        return false;
    }

    private void remember(TaskResult result) {
        ArrayDeque<TaskResult> values = history.computeIfAbsent(result.taskId(), ignored -> new ArrayDeque<>());
        values.addFirst(result);
        while (values.size() > MAX_HISTORY_PER_TASK) values.removeLast();
    }

    private static Map<String, String> safeInputs(DiagnosticTask task, TaskRunContext context) {
        Map<String, String> inputs = task.inputs(context);
        return inputs == null ? Map.of() : Map.copyOf(inputs);
    }

    private static String budgetReason(int index, long runStarted, TaskRunContext context) {
        if (index >= context.budget().maxTasks()) return "maximum task count reached";
        if ((System.nanoTime() - runStarted) / 1_000_000L > context.budget().maxRunMillis()) return "maximum run duration reached";
        return context.budgetExceeded();
    }

    private static boolean blocked(DiagnosticTask task, Map<String, TaskResult> results) {
        return task.dependencies().stream().filter(TaskDependency::required).map(d -> results.get(d.taskId()))
                .anyMatch(r -> r == null || (r.state() != TaskState.SUCCESS && r.state() != TaskState.UP_TO_DATE));
    }

    private static TaskResult result(DiagnosticTask task, TaskState state, String reason, String message,
                                     Instant started, String fp, boolean reused, Map<String, String> outputs,
                                     Map<String, String> changedInputs, TaskOverhead overhead) {
        return new TaskResult(task.id(), state, safe(reason), safe(message), started, Instant.now(), fp, reused,
                outputs, changedInputs, overhead);
    }

    private static TaskOverhead overhead(long started, TaskRunContext context, TaskRunContext.Counters before,
                                         Map<String, String> outputs, boolean timedOut, boolean truncated, String reason) {
        return overhead(started, context, before, outputs, timedOut, truncated, reason, 0);
    }

    private static TaskOverhead overhead(long started, TaskRunContext context, TaskRunContext.Counters before,
                                         Map<String, String> outputs, boolean timedOut, boolean truncated, String reason,
                                         long retainedBytes) {
        TaskRunContext.Counters after = context.counters();
        long wall = Math.max(0, System.nanoTime() - started);
        long outputBytes = outputBytes(outputs);
        return new TaskOverhead(wall, wall, 0,
                Math.max(0, after.filesInspected() - before.filesInspected()),
                Math.max(0, after.bytesRead() - before.bytesRead()),
                Math.max(0, after.samplesCollected() - before.samplesCollected()),
                Math.max(0, after.recordsProduced() - before.recordsProduced()),
                retainedBytes, outputBytes, 0, timedOut, truncated, reason);
    }

    private static long outputBytes(Map<String, String> outputs) {
        long total = 0;
        for (Map.Entry<String, String> entry : outputs.entrySet()) {
            total += entry.getKey().getBytes(StandardCharsets.UTF_8).length;
            total += safe(entry.getValue()).getBytes(StandardCharsets.UTF_8).length;
        }
        return total;
    }

    private static long usedMemory() {
        Runtime runtime = Runtime.getRuntime();
        return runtime.totalMemory() - runtime.freeMemory();
    }

    private static Map<String, String> changedInputs(Cached cached, Map<String, String> last, Map<String, String> current) {
        Map<String, String> previous = cached == null ? last : cached.inputs;
        if (previous == null) return Map.of("<cache>", "no-prior-entry");
        Map<String, String> changes = new TreeMap<>();
        Set<String> keys = new TreeSet<>(previous.keySet());
        keys.addAll(current.keySet());
        for (String key : keys) {
            String before = previous.get(key), after = current.get(key);
            if (!Objects.equals(before, after)) changes.put(key, safe(before) + " -> " + safe(after));
        }
        return Map.copyOf(changes);
    }

    public static String fingerprint(Map<String, String> inputs) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            new TreeMap<>(inputs).forEach((key, value) -> {
                digest.update(key.getBytes(StandardCharsets.UTF_8)); digest.update((byte) 0);
                digest.update(safe(value).getBytes(StandardCharsets.UTF_8)); digest.update((byte) 0);
            });
            return HexFormat.of().formatHex(digest.digest()).substring(0, 16);
        } catch (Exception exception) { throw new IllegalStateException(exception); }
    }

    private static String safe(String value) { return value == null ? "" : value; }

    private record Cached(String fingerprint, Map<String, String> inputs, TaskOutcome outcome) { }
}
