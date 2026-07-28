package com.soumyajit.gradlemc.performance;

import java.util.ArrayDeque;
import java.util.List;

/**
 * Bounded hysteretic admission controller for GradleMC-owned optional work.  Evidence which is
 * missing or stale is explicitly represented and never interpreted as healthy performance.
 */
public final class PerformanceGuard {
    public enum State { NORMAL, CONSTRAINED, CRITICAL, RECOVERING }
    public enum Action { COALESCE_PRESENTATION, REDUCE_PRESENTATION_REFRESH, DEFER_STATIC_ANALYSIS,
        SUSPEND_PASSIVE_OBSERVATION, DELAY_MAINTENANCE, PREVENT_SECOND_HEAVY_OPERATION }
    private static final int PRESSURE_SAMPLES = 3;
    private static final int CRITICAL_SAMPLES = 6;
    private static final int RECOVERY_SAMPLES = 6;
    private static final long MIN_DWELL_NANOS = 10_000_000_000L;
    private static final int HISTORY_LIMIT = 16;
    private State state = State.NORMAL;
    private int pressureSamples;
    private int recoverySamples;
    private long transitionedAtNanos;
    private String reason = "No sustained fresh GradleMC pressure.";
    private Evidence lastEvidence = Evidence.unavailable();
    private final ArrayDeque<String> history = new ArrayDeque<>();

    public synchronized Snapshot observe(long nowNanos, Evidence evidence) {
        lastEvidence = evidence == null ? Evidence.unavailable() : evidence;
        boolean pressured = lastEvidence.fresh() && lastEvidence.pressured();
        if (pressured) {
            pressureSamples = Math.min(CRITICAL_SAMPLES, pressureSamples + 1);
            recoverySamples = 0;
        } else if (lastEvidence.fresh()) {
            recoverySamples = Math.min(RECOVERY_SAMPLES, recoverySamples + 1);
            pressureSamples = 0;
        }
        if (state == State.NORMAL && pressureSamples >= PRESSURE_SAMPLES && elapsed(nowNanos) >= MIN_DWELL_NANOS) {
            transition(nowNanos, State.CONSTRAINED, "Sustained fresh " + lastEvidence.pressureSummary() + " pressure.");
        } else if (state == State.CONSTRAINED && pressureSamples >= CRITICAL_SAMPLES && elapsed(nowNanos) >= MIN_DWELL_NANOS) {
            transition(nowNanos, State.CRITICAL, "Sustained critical GradleMC workload pressure.");
        } else if ((state == State.CONSTRAINED || state == State.CRITICAL) && lastEvidence.fresh()
                && !pressured && recoverySamples >= RECOVERY_SAMPLES && elapsed(nowNanos) >= MIN_DWELL_NANOS) {
            transition(nowNanos, State.RECOVERING, "Fresh evidence remained below guard thresholds.");
        } else if (state == State.RECOVERING && lastEvidence.fresh() && !pressured
                && recoverySamples >= RECOVERY_SAMPLES && elapsed(nowNanos) >= MIN_DWELL_NANOS) {
            transition(nowNanos, State.NORMAL, "Recovered after bounded hysteresis.");
        }
        return snapshot();
    }

    /** Compatibility entry point; non-finite fields are unavailable rather than healthy. */
    public synchronized Snapshot observe(long nowNanos, double frameMillis, double mspt, double heapPressure,
                                         int pendingJobs, boolean worldLoading) {
        return observe(nowNanos, new Evidence(frameMillis, mspt, heapPressure, pendingJobs, 0, 0D,
                Double.isFinite(frameMillis), Double.isFinite(mspt), Double.isFinite(heapPressure), true, true,
                worldLoading));
    }

    public synchronized boolean deferOptionalWork() { return state != State.NORMAL; }
    public synchronized boolean permitsForegroundHeavyWork(int activeHeavyOperations) {
        return activeHeavyOperations < 1 && state != State.CRITICAL;
    }
    public synchronized Snapshot snapshot() {
        return new Snapshot(state, pressureSamples, recoverySamples, transitionedAtNanos, reason,
                deferOptionalWork(), lastEvidence, List.copyOf(history), allowedWork(), deferredWork());
    }
    private List<Action> deferredWork() {
        return state == State.NORMAL ? List.of() : List.of(Action.COALESCE_PRESENTATION,
                Action.REDUCE_PRESENTATION_REFRESH, Action.DEFER_STATIC_ANALYSIS,
                Action.SUSPEND_PASSIVE_OBSERVATION, Action.DELAY_MAINTENANCE, Action.PREVENT_SECOND_HEAVY_OPERATION);
    }
    private List<String> allowedWork() {
        return List.of("Underlying FPS and server measurements", "Explicit foreground diagnostics when capacity permits",
                "Minecraft and third-party settings (untouched)");
    }
    private void transition(long now, State next, String nextReason) {
        state = next; transitionedAtNanos = now; reason = nextReason;
        if (history.size() == HISTORY_LIMIT) history.removeFirst();
        history.addLast(next + ": " + nextReason);
    }
    private long elapsed(long now) { return transitionedAtNanos == 0L ? Long.MAX_VALUE : Math.max(0L, now - transitionedAtNanos); }

    public record Evidence(double frameMillis, double mspt, double heapPressure, int queueDepth,
                           int activeHeavyOperations, double recentOverheadMillis, boolean frameFresh,
                           boolean serverFresh, boolean heapFresh, boolean queueFresh, boolean overheadFresh,
                           boolean worldLoading) {
        public static Evidence unavailable() { return new Evidence(Double.NaN, Double.NaN, Double.NaN, -1, -1,
                Double.NaN, false, false, false, false, false, false); }
        public boolean fresh() { return frameFresh || serverFresh || heapFresh || queueFresh || overheadFresh; }
        public boolean pressured() { return (frameFresh && frameMillis >= 100D) || (serverFresh && mspt >= 75D)
                || (heapFresh && heapPressure >= .90D) || (queueFresh && queueDepth >= 2)
                || (overheadFresh && recentOverheadMillis >= 25D) || worldLoading || activeHeavyOperations > 1; }
        public String pressureSummary() {
            StringBuilder value = new StringBuilder();
            if (frameFresh) value.append("frame"); if (serverFresh) append(value, "server");
            if (heapFresh) append(value, "heap"); if (queueFresh) append(value, "queue");
            if (overheadFresh) append(value, "overhead");
            return value.isEmpty() ? "available" : value.toString();
        }
        private static void append(StringBuilder value, String field) { if (!value.isEmpty()) value.append(','); value.append(field); }
    }
    public record Snapshot(State state, int sustainedPressureSamples, int recoverySamples, long lastTransitionNanos,
                           String reason, boolean optionalWorkDeferred, Evidence evidence, List<String> history,
                           List<String> allowedWork, List<Action> deferredWork) { }
}
