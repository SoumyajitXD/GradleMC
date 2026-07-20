package com.soumyajit.gradlemc.task;

import com.soumyajit.gradlemc.GradleMC;
import com.soumyajit.gradlemc.util.RuntimeSnapshots;
import com.soumyajit.gradlemc.util.GradleMcLimits;
import net.fabricmc.loader.api.FabricLoader;

import java.time.Instant;
import java.util.ArrayDeque;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.nio.file.Path;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executor;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

/** Process-owned workflow facade. One workflow at a time; workers are bounded, named, daemon and shut down on lifecycle stop. */
public final class FabricDiagnosticService {
    private static final int HISTORY_LIMIT = GradleMcLimits.MAX_WORKFLOW_HISTORY;
    private static final ThreadPoolExecutor CONTROLLER = executor("GradleMC diagnostics controller");
    private static final ThreadPoolExecutor TASKS = executor("GradleMC diagnostics worker");
    private static final TaskEngine REGISTRY = new TaskEngine(List.of(
            task("runtime", "Runtime environment", "Captures normalized local environment identity.", "environment", TaskEnvironment.ANY, List.of(), List.of(EvidenceType.ENVIRONMENT), context -> {
                context.checkpoint();
                return TaskOutcome.success(List.of(evidence("environment.runtime", EvidenceType.ENVIRONMENT, "runtime", context.snapshot(), "", "process", "Static runtime snapshot; no paths, users, tokens or world seed.")));
            }),
            task("memory", "Memory snapshot", "Captures JVM heap values at one bounded instant.", "memory", TaskEnvironment.ANY, List.of(TaskDependency.required("runtime")), List.of(EvidenceType.MEMORY), context -> {
                context.checkpoint(); RuntimeSnapshots.MemorySnapshot memory = RuntimeSnapshots.memory();
                Map<String, String> values = Map.of("used", Long.toString(memory.usedMiB()), "committed", Long.toString(memory.totalMiB()), "maximum", Long.toString(memory.maxMiB()), "free", Long.toString(memory.freeMiB()));
                return TaskOutcome.success(List.of(evidence("memory.heap", EvidenceType.MEMORY, "memory", values, "MiB", "jvm", "Point-in-time JVM heap only; does not claim total system memory.")));
            }),
            task("mods", "Installed mods", "Uses a client/server-thread snapshot of Fabric Loader metadata counts.", "mods", TaskEnvironment.ANY, List.of(TaskDependency.required("runtime")), List.of(EvidenceType.INSTALLED_MODS), context -> {
                context.checkpoint();
                String count = context.snapshot().get("mods.count");
                if (count == null) return TaskOutcome.unavailable("Installed-mod snapshot was not available on the owning thread.");
                return TaskOutcome.success(List.of(evidence("mods.inventory", EvidenceType.INSTALLED_MODS, "mods", Map.of("count", count, "fingerprint", context.snapshot().getOrDefault("mods.fingerprint", "")), "mods", "loader", "Normalized mod IDs and versions only; no jars are scanned.")));
            }),
            task("entity_observation", "Entity observation", "Consumes a bounded server-thread observation if supplied.", "server", TaskEnvironment.WORLD, List.of(TaskDependency.required("runtime")), List.of(EvidenceType.ENTITIES, EvidenceType.BLOCK_ENTITIES), context -> unavailableOrObservation(context, "entities.count", "entities.observation", EvidenceType.ENTITIES, "entities", "server-world")),
            task("fps_client", "FPS observation", "Consumes the authoritative client render-frame sampler snapshot if supplied.", "client", TaskEnvironment.CLIENT, List.of(TaskDependency.required("runtime")), List.of(EvidenceType.FPS), context -> unavailableOrObservation(context, "fps.average", "fps.session", EvidenceType.FPS, "FPS", "client-render")),
            task("worldgen_observation", "World-generation observation", "Consumes a completed bounded observation; it never scans saves or world files.", "server", TaskEnvironment.WORLD, List.of(TaskDependency.required("runtime")), List.of(EvidenceType.WORLDGEN), context -> unavailableOrObservation(context, "worldgen.average_mspt", "worldgen.observation", EvidenceType.WORLDGEN, "milliseconds", "server-world"))
    ));
    private static final WorkflowEngine ENGINE = new WorkflowEngine(REGISTRY, TASKS);
    private static final AtomicReference<ActiveRun> ACTIVE = new AtomicReference<>();
    private static final ArrayDeque<WorkflowResult> HISTORY = new ArrayDeque<>();
    private static volatile WorkflowResult latest;
    private static volatile WorkflowReportArtifact latestArtifact;

    private FabricDiagnosticService() { }

    public static TaskEngine registry() { return REGISTRY; }
    public static WorkflowPlan plan(String workflowId) { return ENGINE.plan(workflowId); }
    public static Optional<WorkflowResult> latest() { return Optional.ofNullable(latest); }
    public static Optional<WorkflowReportArtifact> latestArtifact() { return Optional.ofNullable(latestArtifact); }
    public static synchronized List<WorkflowResult> history() { return List.copyOf(HISTORY); }
    public static Optional<String> activeWorkflow() { ActiveRun active = ACTIVE.get(); return active == null ? Optional.empty() : Optional.of(active.plan.workflowId()); }

    public static Map<String, String> captureSnapshot() {
        List<String> mods = FabricLoader.getInstance().getAllMods().stream().map(container -> container.getMetadata().getId() + "@" + container.getMetadata().getVersion().getFriendlyString()).sorted().toList();
        return Map.of("schema", DiagnosticSchemas.SNAPSHOT, "gradlemc.version", GradleMC.version(), "minecraft.version", GradleMC.CURRENT_MINECRAFT_VERSION,
                "loader", GradleMC.CURRENT_LOADER_NAME, "java.major", Integer.toString(Runtime.version().feature()), "mods.count", Integer.toString(mods.size()),
                "mods.fingerprint", TaskFingerprint.sha256(mods));
    }

    public static synchronized Future<WorkflowResult> start(String workflowId, Map<String, String> snapshot, EnumSet<TaskEnvironment> environments) {
        if (ACTIVE.get() != null) throw new IllegalStateException("A GradleMC workflow is already active.");
        WorkflowPlan plan = ENGINE.plan(workflowId);
        CancellationToken cancellation = new CancellationToken();
        ActiveRun activeRun = new ActiveRun(plan, cancellation);
        FutureTask<WorkflowResult> future = new FutureTask<>(() -> {
            try {
                WorkflowResult result = ENGINE.execute(plan, snapshot, environments, cancellation);
                synchronized (FabricDiagnosticService.class) { latest = result; HISTORY.addFirst(result); while (HISTORY.size() > HISTORY_LIMIT) HISTORY.removeLast(); }
                return result;
            } finally { ACTIVE.compareAndSet(activeRun, null); }
        });
        ACTIVE.set(activeRun);
        try { CONTROLLER.execute(future); } catch (RuntimeException exception) { ACTIVE.compareAndSet(activeRun, null); throw exception; }
        return future;
    }

    public static boolean cancel(String reason) {
        ActiveRun active = ACTIVE.get();
        return active != null && active.cancellation.cancel(reason);
    }

    /** Report formatting/writing runs off Minecraft threads and consumes the already-final immutable result. */
    public static CompletableFuture<WorkflowReportArtifact> writeLatestReport(Path directory) {
        WorkflowResult result = latest;
        if (result == null) throw new IllegalStateException("No completed workflow result exists.");
        if (ACTIVE.get() != null) throw new IllegalStateException("Cannot write a workflow report while a workflow is active.");
        return submitReportTask(CONTROLLER, () -> {
            try {
                WorkflowReportArtifact artifact = new WorkflowReportWriter().write(result, directory);
                WorkflowHistoryIndex.record(directory, result, artifact);
                latestArtifact = artifact;
                return artifact;
            } catch (java.io.IOException exception) {
                throw new CompletionException(exception);
            }
        });
    }

    static <T> CompletableFuture<T> submitReportTask(Executor executor, Supplier<T> task) {
        try {
            return CompletableFuture.supplyAsync(task, executor);
        } catch (RejectedExecutionException exception) {
            throw new IllegalStateException("GradleMC diagnostics are shutting down; the report was not started.", exception);
        }
    }

    /** Cancels only work whose definition needs a world/player snapshot. */
    public static void onWorldUnavailable(String reason) {
        ActiveRun active = ACTIVE.get();
        if (active != null && active.plan.orderedTasks().stream().anyMatch(task -> task.environment() == TaskEnvironment.WORLD
                || task.environment() == TaskEnvironment.PLAYER || task.evidenceTypes().contains(EvidenceType.FPS))) active.cancellation.cancel(reason);
    }

    public static void shutdown() {
        cancel("client-or-server-stopping");
        CONTROLLER.shutdownNow(); TASKS.shutdownNow();
        try { CONTROLLER.awaitTermination(2, TimeUnit.SECONDS); TASKS.awaitTermination(2, TimeUnit.SECONDS); } catch (InterruptedException interrupted) { Thread.currentThread().interrupt(); }
        ACTIVE.set(null);
        synchronized (FabricDiagnosticService.class) { HISTORY.clear(); latest = null; latestArtifact = null; }
    }

    /** Dedicated server process shutdown owns the common workers; integrated-server stop only invalidates world work. */
    public static void onServerStopping(boolean dedicated) {
        onWorldUnavailable("server-stopping");
        if (dedicated) shutdown();
    }

    private static DiagnosticTask task(String id, String display, String description, String category, TaskEnvironment environment, List<TaskDependency> dependencies, List<EvidenceType> evidence, DiagnosticTaskAction action) {
        return new DiagnosticTask(id, display, description, category, environment, dependencies, java.time.Duration.ofSeconds(8), true, evidence, action);
    }
    private static TaskOutcome unavailableOrObservation(TaskExecutionContext context, String key, String evidenceId, EvidenceType type, String unit, String scope) {
        context.checkpoint(); String value = context.snapshot().get(key);
        if (value == null) return TaskOutcome.unavailable("No completed bounded " + type.name().toLowerCase() + " observation is available for this workflow.");
        return TaskOutcome.success(List.of(evidence(evidenceId, type, context.snapshot().getOrDefault("source.task", type.name().toLowerCase()), Map.of("value", value), unit, scope, "Previously completed bounded observation.")));
    }
    private static DiagnosticEvidence evidence(String id, EvidenceType type, String source, Map<String, String> values, String unit, String scope, String limitation) {
        return new DiagnosticEvidence(id, type, source, Instant.now(), scope, values, unit, EvidenceAvailability.AVAILABLE, "high", limitation, DiagnosticSchemas.COLLECTOR);
    }
    private static ThreadPoolExecutor executor(String name) {
        ThreadPoolExecutor result = new ThreadPoolExecutor(1, 1, 0L, TimeUnit.MILLISECONDS,
                new ArrayBlockingQueue<>(GradleMcLimits.MAX_EXECUTOR_QUEUE), runnable -> {
            Thread thread = new Thread(runnable, name); thread.setDaemon(true);
            thread.setUncaughtExceptionHandler((failed, error) -> GradleMC.LOGGER.error("Uncaught GradleMC worker failure on {}", failed.getName(), error));
            return thread;
        }, new ThreadPoolExecutor.AbortPolicy());
        return result;
    }
    private record ActiveRun(WorkflowPlan plan, CancellationToken cancellation) { }
}
