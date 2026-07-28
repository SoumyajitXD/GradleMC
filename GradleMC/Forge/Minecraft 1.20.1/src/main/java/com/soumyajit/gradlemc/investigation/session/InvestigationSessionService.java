package com.soumyajit.gradlemc.investigation.session;

import com.soumyajit.gradlemc.foundation.FoundationService;
import com.soumyajit.gradlemc.foundation.GradleMcRuntimeExecutor;
import com.soumyajit.gradlemc.foundation.TaskCore;
import com.soumyajit.gradlemc.foundation.TaskId;
import com.soumyajit.gradlemc.investigation.*;
import com.soumyajit.gradlemc.investigation.planning.*;
import com.soumyajit.gradlemc.investigation.storage.InvestigationStore;

import java.io.IOException;
import java.time.Clock;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * One bounded asynchronous investigation coordinator.  The worker receives only the immutable
 * request/context; Foundation remains responsible for task routing and game-thread capture.
 */
public final class InvestigationSessionService implements AutoCloseable {
    @FunctionalInterface public interface FoundationExecution {
        List<TaskCore.TaskResult> execute(List<TaskId> roots, TaskCore.Context context) throws Exception;
    }

    private final InvestigationPlanningAdapter planning;
    private final InvestigationStore store;
    private final Clock clock;
    private final FoundationExecution execution;
    private final ThreadPoolExecutor worker;
    private final Object guard = new Object();
    private volatile Active active;

    public InvestigationSessionService(InvestigationPlanningAdapter planning, InvestigationStore store, Clock clock) {
        this(planning, store, clock, (roots, context) -> FoundationService.run(roots, context, false));
    }

    public InvestigationSessionService(InvestigationPlanningAdapter planning, InvestigationStore store, Clock clock, FoundationExecution execution) {
        this.planning = Objects.requireNonNull(planning, "planning");
        this.store = Objects.requireNonNull(store, "store");
        this.clock = Objects.requireNonNull(clock, "clock");
        this.execution = Objects.requireNonNull(execution, "execution");
        this.worker = GradleMcRuntimeExecutor.lane(GradleMcRuntimeExecutor.Lane.DIAGNOSTIC_COORDINATION);
    }

    public Optional<InvestigationSessionSnapshot> active() {
        Active value = active;
        return Optional.ofNullable(value == null ? null : value.snapshot);
    }

    /** Read-only bounded history view; this never schedules, retries, or mutates a session. */
    public List<com.soumyajit.gradlemc.investigation.storage.InvestigationIndexEntry> list(int requestedLimit) throws IOException {
        int limit = Math.max(1, Math.min(16, requestedLimit));
        return store.loadIndex().index().entries().stream()
                .sorted(Comparator.comparing(com.soumyajit.gradlemc.investigation.storage.InvestigationIndexEntry::createdAt).reversed())
                .limit(limit).toList();
    }

    /** Reads a persisted immutable manifest.  Missing/corrupt entries stay explicit to the caller. */
    public InvestigationManifest show(InvestigationId id) throws IOException { return store.read(Objects.requireNonNull(id, "id")); }

    /** Explicit recovery only: non-terminal records are marked interrupted and never rerun. */
    public InvestigationStore.RecoverySummary recoverInterrupted(int requestedCap) throws IOException {
        Active run = active;
        return store.recoverInterrupted(Math.max(1, Math.min(16, requestedCap)), run == null ? Set.of() : Set.of(run.id));
    }

    /** Bounded lifecycle seam for shutdown/tests; it does not execute work on the caller thread. */
    public boolean awaitIdle(long timeout, TimeUnit unit) throws InterruptedException {
        Active run = active;
        if (run == null) return true;
        try {
            run.finished.get(timeout, unit);
            return true;
        } catch (ExecutionException | TimeoutException exception) {
            return false;
        }
    }

    /** Creates the manifest before accepting work, then returns without running Foundation inline. */
    public InvestigationSessionSnapshot start(InvestigationSessionRequest request) {
        Objects.requireNonNull(request, "request");
        InvestigationPlan plan = planning.plan(request.profileId(), request.planningContext());
        if (plan.status() == InvestigationPlanStatus.BLOCKED
                || plan.status() == InvestigationPlanStatus.PARTIAL && !request.policy().allowPartialWhenRequiredRootsViable()) {
            throw new IllegalArgumentException("Investigation plan cannot start: " + plan.status());
        }
        Instant now = clock.instant();
        InvestigationId id = InvestigationId.generate(clock, UUID.randomUUID());
        Active run = new Active(id, request, plan, snapshot(id, request, plan, InvestigationSessionStatus.PLANNED, now, now, 0, "Plan accepted and queued.", true));
        synchronized (guard) {
            if (active != null) throw new IllegalStateException("An investigation session is already active");
            active = run;
            try {
                store.create(manifest(run, InvestigationState.CREATED, now, null, null, List.of(), planLimitations(plan), Optional.empty()));
                run.revision = 0;
                run.diskState = InvestigationState.CREATED;
                worker.execute(() -> execute(run));
            } catch (IOException | RejectedExecutionException exception) {
                // A rejected executor must not strand a non-terminal accepted manifest.  Keep the
                // original create failure as the caller-visible cause, and do not delete evidence.
                if (run.revision >= 0) {
                    try {
                        InvestigationManifest failed = manifest(run, InvestigationState.FAILED, run.createdAt, null, clock.instant(), List.of(),
                                planLimitations(plan), Optional.of(new InvestigationFailure("orchestration-unavailable", "Investigation worker was unavailable.", true)));
                        store.update(failed, run.revision, run.diskState);
                        run.revision = failed.revision();
                        run.diskState = InvestigationState.FAILED;
                    } catch (Exception ignored) {
                        // The last successfully persisted state remains authoritative when storage is unavailable.
                    }
                }
                active = null;
                throw new IllegalStateException("Investigation could not be started: " + exception.getClass().getSimpleName(), exception);
            }
        }
        return run.snapshot;
    }

    /** Idempotent request; the terminal worker owns the only slot release. */
    public boolean cancel() {
        Active run = active;
        if (run == null) return false;
        synchronized (run) {
            if (run.snapshot.status().terminal()) return true;
            run.cancelling = true;
            run.snapshot = snapshot(run.id, run.request, run.plan, InvestigationSessionStatus.CANCELLING,
                    run.createdAt, clock.instant(), run.completed, "Cancellation requested.", run.persistenceSynchronized);
        }
        // The exact request token is carried into Foundation.  Do not use the global Foundation
        // cancellation hook here: unrelated direct Foundation work must remain unaffected.
        run.request.planningContext().foundationContext().cancellation().cancel();
        return true;
    }

    /** Called by the owning Forge lifecycle; it never leaves queued or executing work active. */
    public void stopServer() { cancel(); }

    private void execute(Active run) {
        try {
            if (run.cancelling) {
                terminal(run, InvestigationState.CANCELLED, List.of(), Optional.empty(), "Cancelled before execution.");
                return;
            }
            transitionRunning(run);
            List<TaskId> roots = run.plan.tasks().stream()
                    .filter(task -> task.role() != InvestigationPlanTask.Role.DEPENDENCY)
                    .map(InvestigationPlanTask::taskId).toList();
            List<TaskCore.TaskResult> results = execution.execute(roots, run.request.planningContext().foundationContext());
            terminalFromResults(run, results == null ? List.of() : results);
        } catch (Exception exception) {
            terminal(run, InvestigationState.FAILED, List.of(), Optional.of(new InvestigationFailure(
                    "foundation-execution-failure", "Foundation execution failed: " + exception.getClass().getSimpleName(), true)),
                    "Foundation execution failed.");
        } finally {
            synchronized (guard) { if (active == run) active = null; }
            run.finished.complete(null);
        }
    }

    private void transitionRunning(Active run) throws IOException {
        if (run.cancelling) return;
        Instant now = clock.instant();
        InvestigationManifest next = manifest(run, InvestigationState.RUNNING, run.createdAt, now, null, List.of(), planLimitations(run.plan), Optional.empty());
        store.update(next, run.revision, InvestigationState.CREATED);
        run.revision = next.revision();
        run.diskState = InvestigationState.RUNNING;
        run.startedAt = now;
        run.snapshot = snapshot(run.id, run.request, run.plan, InvestigationSessionStatus.RUNNING,
                run.createdAt, now, 0, "Executing Foundation plan.", true);
    }

    private void terminalFromResults(Active run, List<TaskCore.TaskResult> results) {
        List<InvestigationStepRecord> steps = new ArrayList<>();
        List<InvestigationLimitation> limitations = new ArrayList<>(planLimitations(run.plan));
        Map<TaskId, InvestigationPlanTask> views = new HashMap<>();
        run.plan.tasks().forEach(view -> views.put(view.taskId(), view));
        boolean requiredBad = false;
        boolean optionalBad = false;
        boolean cancelled = run.cancelling;
        for (TaskCore.TaskResult result : results) {
            InvestigationStepState state = map(result.state());
            steps.add(new InvestigationStepRecord(result.id().value(), state, result.startedAt(), result.completedAt(), result.reason()));
            InvestigationPlanTask view = views.get(result.id());
            boolean bad = state != InvestigationStepState.SUCCEEDED;
            requiredBad |= bad && view != null && view.role() == InvestigationPlanTask.Role.REQUIRED_ROOT;
            optionalBad |= bad && view != null && view.role() == InvestigationPlanTask.Role.OPTIONAL_ROOT;
            cancelled |= state == InvestigationStepState.CANCELLED;
        }
        if (optionalBad) limitations.add(new InvestigationLimitation("optional-root-limited", "Optional profile evidence did not complete."));
        InvestigationState state = cancelled ? InvestigationState.CANCELLED
                : requiredBad ? InvestigationState.FAILED
                : limitations.isEmpty() ? InvestigationState.COMPLETED : InvestigationState.COMPLETED_WITH_LIMITATIONS;
        Optional<InvestigationFailure> failure = state == InvestigationState.FAILED
                ? Optional.of(new InvestigationFailure("required-root-failed", "A required Foundation root did not complete.", true)) : Optional.empty();
        terminal(run, state, steps, limitations, failure, state == InvestigationState.CANCELLED ? "Investigation cancelled." : "Investigation completed.");
    }

    private void terminal(Active run, InvestigationState state, List<InvestigationStepRecord> steps,
                          Optional<InvestigationFailure> failure, String detail) {
        terminal(run, state, steps, planLimitations(run.plan), failure, detail);
    }

    private void terminal(Active run, InvestigationState state, List<InvestigationStepRecord> steps,
                          List<InvestigationLimitation> limitations, Optional<InvestigationFailure> failure, String detail) {
        Instant end = clock.instant();
        boolean synchronizedToDisk = false;
        try {
            if (run.revision >= 0) {
                InvestigationManifest next = manifest(run, state, run.createdAt, run.startedAt, end, steps, limitations, failure);
                store.update(next, run.revision, run.diskState);
                run.revision = next.revision();
                run.diskState = state;
                synchronizedToDisk = true;
            }
        } catch (Exception ignored) {
            // Preserve the last safe revision. A later explicit store reconciliation may repair the index;
            // automatic retries would risk overwriting a concurrent/manual recovery.
        }
        run.completed = Math.min(run.plan.tasks().size(), steps.size());
        run.persistenceSynchronized = synchronizedToDisk;
        InvestigationSessionStatus status = state == InvestigationState.CANCELLED ? InvestigationSessionStatus.CANCELLED
                : state == InvestigationState.FAILED ? InvestigationSessionStatus.FAILED : InvestigationSessionStatus.COMPLETED;
        String boundedDetail = synchronizedToDisk ? detail : "Terminal result is retained in memory; persistence is out of sync.";
        run.snapshot = snapshot(run.id, run.request, run.plan, status, run.createdAt, end, run.completed, boundedDetail, synchronizedToDisk);
    }

    private InvestigationManifest manifest(Active run, InvestigationState state, Instant created, Instant started, Instant ended,
                                           List<InvestigationStepRecord> steps, List<InvestigationLimitation> limitations,
                                           Optional<InvestigationFailure> failure) {
        boolean limited = state == InvestigationState.COMPLETED_WITH_LIMITATIONS;
        return new InvestigationManifest(InvestigationManifest.SCHEMA_VERSION, Math.max(0, run.revision + 1), run.id,
                run.request.profileId(), "1", state, created, started, ended, run.request.runtimeIdentity(), run.request.inputFingerprint(),
                steps, List.of(), limitations, failure, false, List.of(), 1, state == InvestigationState.COMPLETED || limited);
    }

    private static List<InvestigationLimitation> planLimitations(InvestigationPlan plan) {
        return plan.limitations().stream().map(value -> new InvestigationLimitation(value.code(), value.message())).toList();
    }

    private static InvestigationSessionSnapshot snapshot(InvestigationId id, InvestigationSessionRequest request, InvestigationPlan plan,
                                                          InvestigationSessionStatus status, Instant created, Instant updated,
                                                          int completed, String detail, boolean persistenceSynchronized) {
        return new InvestigationSessionSnapshot(id, request.profileId(), status, plan, created, updated, completed, plan.tasks().size(),
                planLimitations(plan).stream().map(InvestigationLimitation::message).toList(), detail, persistenceSynchronized);
    }

    private static InvestigationStepState map(TaskCore.State state) {
        return switch (state) {
            case SUCCEEDED -> InvestigationStepState.SUCCEEDED;
            case UNAVAILABLE -> InvestigationStepState.UNAVAILABLE;
            case SKIPPED -> InvestigationStepState.SKIPPED;
            case TIMED_OUT -> InvestigationStepState.TIMED_OUT;
            case CANCELLED -> InvestigationStepState.CANCELLED;
            default -> InvestigationStepState.FAILED;
        };
    }

    @Override public void close() {
        stopServer();
    }

    private static final class Active {
        final InvestigationId id;
        final InvestigationSessionRequest request;
        final InvestigationPlan plan;
        final Instant createdAt;
        volatile InvestigationSessionSnapshot snapshot;
        volatile Instant startedAt;
        long revision = -1;
        InvestigationState diskState;
        int completed;
        boolean cancelling;
        boolean persistenceSynchronized = true;
        final CompletableFuture<Void> finished = new CompletableFuture<>();
        Active(InvestigationId id, InvestigationSessionRequest request, InvestigationPlan plan, InvestigationSessionSnapshot snapshot) {
            this.id = id; this.request = request; this.plan = plan; this.snapshot = snapshot; this.createdAt = snapshot.createdAt();
        }
    }
}
