package com.soumyajit.gradlemc.task;

import com.soumyajit.gradlemc.health.*;
import com.soumyajit.gradlemc.foundation.FoundationService;
import com.soumyajit.gradlemc.foundation.TaskCore;
import com.soumyajit.gradlemc.foundation.TaskId;
import com.soumyajit.gradlemc.util.GradleMcPaths;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import java.io.IOException;
import java.nio.file.*;
import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

/** Command-facing facade. Minecraft-facing tasks remain sequential on their owning command thread. */
public final class DiagnosticRunService {
    private static final TaskEngine ENGINE = new TaskEngine();
    private static volatile List<TaskResult> latest = List.of();
    private static volatile String latestRequested = "";
    private static volatile TaskRunContext current;
    static { BuiltinTasks.register(ENGINE); }
    private DiagnosticRunService() { }

    public static Collection<DiagnosticTask> tasks() { return ENGINE.tasks(); }
    public static Set<String> workflows() { return DiagnosticWorkflows.ids(); }
    public static List<TaskResult> latestResults() { return latest; }
    public static String latestRequested() { return latestRequested; }
    public static List<String> registryIssues() {
        List<String> issues=new ArrayList<>();
        for(DiagnosticTask task:ENGINE.tasks())try{ENGINE.plan(task.id());}catch(IllegalArgumentException exception){issues.add(task.id()+": "+exception.getMessage());}
        return List.copyOf(issues);
    }

    public static int listTasks(CommandSourceStack source) { return listTasks(source, false); }
    public static int listTasks(CommandSourceStack source, boolean all) {
        int limit = all ? 128 : 12;
        List<DiagnosticTask> tasks = ENGINE.tasks().stream().limit(limit).toList();
        tasks.forEach(task -> send(source, task.id() + " - " + task.displayName() + " [" + task.group() + "]"));
        if (!all && ENGINE.tasks().size() > limit) send(source, "Showing " + limit + " tasks; use /gradlemc tasks all.");
        return 1;
    }

    public static int info(CommandSourceStack source, String id) {
        Optional<DiagnosticTask> optional = ENGINE.task(id);
        if (optional.isEmpty()) return failure(source, "Unknown GradleMC task: " + id);
        DiagnosticTask task = optional.get();
        send(source, task.id() + ": " + task.description());
        send(source, "version=" + task.version() + ", group=" + task.group() + ", cache=" + task.cachePolicy()
                + ", cost=" + task.cost() + ", timeoutMs=" + task.timeoutMillis() + ", concurrency=" + task.concurrencyPolicy());
        send(source, "sides=" + task.physicalSide() + "/" + task.logicalSide() + ", permission=" + task.permissionLevel()
                + ", cancellation=" + task.cancellationSupported() + ", failurePolicy=" + task.failurePolicy());
        send(source, "capabilities=" + join(task.capabilities()) + ", outputs=" + join(task.declaredOutputs()));
        return 1;
    }

    public static int graph(CommandSourceStack source, String id) {
        try {
            if (DiagnosticWorkflows.isFoundationWorkflow(id)) {
                TaskCore.Plan plan = FoundationService.dryRun(TaskId.of("scan:" + id), java.time.Clock.systemUTC());
                send(source, "Plan " + id + ": " + plan.nodes().stream().map(node -> node.id().toString()).collect(Collectors.joining(" -> ")));
                return 1;
            }
            TaskPlan plan = DiagnosticWorkflows.plan(ENGINE, id);
            send(source, "Plan " + id + ": " + plan.orderedTasks().stream().map(DiagnosticTask::id).collect(Collectors.joining(" -> ")));
            return 1;
        } catch (IllegalArgumentException exception) { return failure(source, exception.getMessage()); }
    }

    public static int why(CommandSourceStack source, String requested, String taskId) {
        try {
            TaskPlan plan = DiagnosticWorkflows.plan(ENGINE, requested);
            if (plan.orderedTasks().stream().noneMatch(task -> task.id().equals(taskId))) return failure(source, taskId + " is not included in " + requested);
            TaskExplanation explanation = ENGINE.explain(taskId, taskId, new TaskRunContext(source.getServer(), false));
            send(source, taskId + " is included by requested target " + requested + ". Predicted state: " + explanation.predictedState());
            send(source, explanation.reason());
            if (!explanation.changedInputs().isEmpty()) send(source, "Changed inputs: " + explanation.changedInputs());
            if (!explanation.missingCapabilities().isEmpty()) send(source, "Missing capabilities: " + explanation.missingCapabilities());
            return 1;
        } catch (IllegalArgumentException exception) { return failure(source, exception.getMessage()); }
    }

    public static int why(CommandSourceStack source, String taskId) {
        String requested = latestRequested.isBlank() ? taskId : latestRequested;
        try { return why(source, requested, taskId); }
        catch (RuntimeException ignored) { return why(source, taskId, taskId); }
    }

    public static int inputs(CommandSourceStack source, String id) {
        Optional<DiagnosticTask> task = ENGINE.task(id);
        if (task.isEmpty()) return failure(source, "Unknown GradleMC task: " + id);
        send(source, id + " inputs: " + new TreeMap<>(task.get().inputs(new TaskRunContext(source.getServer(), false))));
        return 1;
    }

    public static int outputs(CommandSourceStack source, String id) {
        Optional<DiagnosticTask> task = ENGINE.task(id);
        if (task.isEmpty()) return failure(source, "Unknown GradleMC task: " + id);
        send(source, id + " declared outputs: " + join(task.get().declaredOutputs()));
        return 1;
    }

    public static int history(CommandSourceStack source, String id) {
        if (ENGINE.task(id).isEmpty()) return failure(source, "Unknown GradleMC task: " + id);
        List<TaskResult> values = ENGINE.history(id).stream().limit(8).toList();
        if (values.isEmpty()) send(source, "No session history for " + id + ".");
        else values.forEach(result -> send(source, result.endedAt() + " | " + result.state() + " | " + result.reason()
                + " | " + result.overhead().wallNanos() / 1_000_000L + " ms"));
        return 1;
    }

    public static int run(CommandSourceStack source, String id, boolean dry, boolean rerun) {
        if (current != null) return failure(source, "A GradleMC run is already active.");
        if (DiagnosticWorkflows.isFoundationWorkflow(id)) return runFoundation(source, id, dry, rerun);
        long started = System.nanoTime();
        try {
            TaskPlan plan = DiagnosticWorkflows.plan(ENGINE, id);
            TaskRunContext context = new TaskRunContext(source.getServer(), rerun, ExecutionBudget.configured(), id);
            current = context;
            latest = ENGINE.execute(plan, context, dry);
            latestRequested = id;
            long elapsed = (System.nanoTime() - started) / 1_000_000L;
            Map<TaskState, Long> counts = latest.stream().collect(Collectors.groupingBy(TaskResult::state, () -> new EnumMap<>(TaskState.class), Collectors.counting()));
            send(source, "GradleMC " + (dry ? "dry-run" : "run") + " " + id + " completed in " + elapsed + " ms: " + summary(counts));
            latest.stream().filter(result -> result.taskId().equals("gradlemc:export_scan")).findFirst().ifPresent(result -> {
                String text = result.outputs().get("text"), json = result.outputs().get("json");
                if (text != null && json != null) send(source, "GradleMC Scan: " + text + ", " + json);
            });
            return counts.getOrDefault(TaskState.FAILED, 0L) + counts.getOrDefault(TaskState.TIMED_OUT, 0L) > 0 ? 0 : 1;
        } catch (IllegalArgumentException exception) {
            return failure(source, "GradleMC run failed: " + exception.getMessage());
        } catch (RuntimeException exception) {
            return failure(source, "GradleMC run failed: " + exception.getClass().getSimpleName());
        } finally {
            current = null;
        }
    }

    public static int status(CommandSourceStack source) {
        if (current != null) send(source, "GradleMC run: running " + current.requestedId() + (current.cancelled() ? "; cancellation requested" : ""));
        else if (DiagnosticWorkflows.isFoundationWorkflow(latestRequested) && !FoundationService.history().isEmpty()) send(source, "GradleMC run: idle; latest=" + latestRequested + ", tasks=" + FoundationService.history().size());
        else if (latest.isEmpty()) send(source, "GradleMC run: idle; no completed run.");
        else send(source, "GradleMC run: idle; latest=" + latestRequested + ", tasks=" + latest.size());
        return 1;
    }

    public static int cancel(CommandSourceStack source) {
        TaskRunContext value = current;
        if (value == null) return failure(source, "No GradleMC task run is active.");
        value.cancel(); FoundationService.cancel(); send(source, "GradleMC run cancellation requested; the current task will finish at its next supported boundary."); return 1;
    }

    public static int gates(CommandSourceStack source, boolean evaluate) {
        List<HealthGateResult> results = evaluate ? HealthGateService.evaluateResults(latest) : null;
        if (evaluate && latest.isEmpty()) send(source, "No completed run evidence exists; gates are INCONCLUSIVE where evidence is required.");
        if (results == null) HealthGateService.configuredGates().forEach(gate -> send(source, gate.id() + " - " + gate.description() + " [" + (gate.enabled() ? "enabled" : "disabled") + "]"));
        else results.forEach(result -> send(source, result.gateId() + " | " + result.state() + " | " + result.explanation()));
        return 1;
    }

    public static int explainGate(CommandSourceStack source, String id) {
        Optional<HealthGate> gate = HealthGateService.configuredGates().stream().filter(value -> value.id().equals(id)).findFirst();
        if (gate.isEmpty()) return failure(source, "Unknown health gate: " + id);
        HealthGate value = gate.get();
        HealthGateResult result = HealthGateService.evaluateResults(latest).stream().filter(item -> item.gateId().equals(id)).findFirst().orElseThrow();
        send(source, value.id() + ": " + value.description() + "; kind=" + value.kind() + ", evidence=" + value.evidenceKey() + ", threshold=" + value.threshold());
        send(source, "Latest evaluation: " + result.state() + " - " + result.explanation());
        return 1;
    }

    public static int scans(CommandSourceStack source, boolean latestOnly) {
        Path directory = GradleMcPaths.gradleMcDirectory().resolve("scans").normalize();
        if (!directory.startsWith(GradleMcPaths.gradleMcDirectory()) || !Files.isDirectory(directory)) { send(source, "No GradleMC Scans found."); return 1; }
        try (var files = Files.list(directory)) {
            List<Path> scans = files.filter(path -> Files.isDirectory(path, LinkOption.NOFOLLOW_LINKS) && !Files.isSymbolicLink(path))
                    .filter(path -> path.getFileName().toString().matches("scan-[a-z0-9-]{7,75}"))
                    .filter(path -> Files.isRegularFile(path.resolve("manifest.json"), LinkOption.NOFOLLOW_LINKS))
                    .sorted(Comparator.comparing(path -> path.getFileName().toString(), Comparator.reverseOrder())).limit(latestOnly ? 1 : 8).toList();
            if (scans.isEmpty()) send(source, "No GradleMC Scans found.");
            else scans.forEach(path -> send(source, (latestOnly ? "Latest GradleMC Scan: " : "GradleMC Scan: ") + path.getFileName()));
            return 1;
        } catch (IOException exception) { return failure(source, "Could not list GradleMC Scans: " + exception.getClass().getSimpleName()); }
    }

    private static int runFoundation(CommandSourceStack source, String id, boolean dry, boolean rerun) {
        long started = System.nanoTime();
        TaskRunContext marker = new TaskRunContext(source.getServer(), rerun, ExecutionBudget.configured(), id);
        current = marker;
        try {
            TaskId root = TaskId.of("scan:" + id);
            if (dry) {
                TaskCore.Plan plan = FoundationService.dryRun(root, java.time.Clock.systemUTC());
                send(source, "GradleMC dry-run " + id + ": " + plan.nodes().stream().map(node -> node.id().toString()).collect(Collectors.joining(" -> ")));
                return 1;
            }
            List<TaskCore.TaskResult> results = FoundationService.run(root, java.time.Clock.systemUTC(), rerun);
            latestRequested = id;
            long elapsed = (System.nanoTime() - started) / 1_000_000L;
            Map<TaskCore.State, Long> counts = results.stream().collect(Collectors.groupingBy(TaskCore.TaskResult::state, () -> new EnumMap<>(TaskCore.State.class), Collectors.counting()));
            send(source, "GradleMC run " + id + " completed in " + elapsed + " ms: " + counts);
            results.stream().filter(result -> result.id().equals(root)).flatMap(result -> result.output().stream())
                    .flatMap(output -> output.evidence().stream()).map(evidence -> evidence.fields().get("location"))
                    .filter(Objects::nonNull).findFirst().ifPresent(location -> send(source, "GradleMC Scan v1: " + location));
            return counts.getOrDefault(TaskCore.State.FAILED, 0L) + counts.getOrDefault(TaskCore.State.TIMED_OUT, 0L)
                    + counts.getOrDefault(TaskCore.State.CANCELLED, 0L) > 0 ? 0 : 1;
        } catch (IllegalArgumentException | IllegalStateException exception) {
            return failure(source, "GradleMC run failed: " + exception.getMessage());
        } finally { current = null; }
    }

    private static String summary(Map<TaskState, Long> counts) {
        return Arrays.stream(TaskState.values()).filter(counts::containsKey).map(state -> state + "=" + counts.get(state)).collect(Collectors.joining(", "));
    }
    private static String join(List<String> values) { return values.isEmpty() ? "none" : String.join(", ", values); }
    private static int failure(CommandSourceStack source, String message) { source.sendFailure(Component.literal(message == null ? "Unknown GradleMC error" : message)); return 0; }
    private static void send(CommandSourceStack source, String message) { source.sendSuccess(() -> Component.literal(message), false); }
}
