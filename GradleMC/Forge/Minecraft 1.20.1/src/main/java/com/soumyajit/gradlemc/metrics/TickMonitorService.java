package com.soumyajit.gradlemc.metrics;

import com.soumyajit.gradlemc.incident.IncidentRecorder;
import com.soumyajit.gradlemc.incident.IncidentSignal;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Cheap server-tick monitor.  It records correlation-only incidents and deliberately does not
 * start a profiler, force GC, or inspect live world objects from the tick callback.
 */
public final class TickMonitorService {
    public static final int WARMUP_TICKS = 20;
    public static final int HYSTERESIS_TICKS = 2;
    public static final int COOLDOWN_TICKS = 20 * 60;
    public static final double DEFAULT_THRESHOLD_MILLIS = 100.0D;
    public static final double MAX_THRESHOLD_MILLIS = 5_000.0D;
    private static volatile State state = State.idle();

    private TickMonitorService() { }

    public static synchronized boolean start(double thresholdMillis, double relativeMultiplier) {
        if (state.running) return false;
        boolean ownsRecorder = !IncidentRecorder.instance().recording();
        if (ownsRecorder) IncidentRecorder.instance().start();
        state = new State(true, clamp(thresholdMillis), clampRelative(relativeMultiplier), 0, 0, -COOLDOWN_TICKS, 0, 0, 0.0D, 0.0D, ownsRecorder);
        return true;
    }

    public static synchronized boolean stop() {
        if (!state.running) return false;
        if (state.ownsRecorder) IncidentRecorder.instance().stop();
        state = State.idle();
        return true;
    }

    public static synchronized Snapshot snapshot() {
        return new Snapshot(state.running, state.thresholdMillis, state.relativeMultiplier, state.ticks, state.incidents, state.lastDurationMillis, state.consecutiveSlow);
    }

    public static synchronized void onTick(long durationNanos) {
        if (!state.running || durationNanos < 0L) return;
        double millis = durationNanos / 1_000_000.0D;
        long tick = state.ticks + 1;
        double baseline = state.baselineMillis <= 0.0D ? millis : state.baselineMillis * 0.95D + millis * 0.05D;
        double relative = state.relativeMultiplier <= 0.0D ? 0.0D : baseline * state.relativeMultiplier;
        boolean slow = tick > WARMUP_TICKS && millis >= state.thresholdMillis && (relative <= 0.0D || millis >= relative);
        int consecutive = slow ? Math.min(HYSTERESIS_TICKS, state.consecutiveSlow + 1) : 0;
        int incidents = state.incidents;
        long lastIncident = state.lastIncidentTick;
        if (consecutive >= HYSTERESIS_TICKS && tick - lastIncident >= COOLDOWN_TICKS) {
            IncidentRecorder.instance().signal(new IncidentSignal(Instant.now(), "slow-tick", Map.of("mspt", millis, "threshold", state.thresholdMillis)));
            if (IncidentRecorder.instance().trigger("slow-tick", Map.of("mspt", String.format(java.util.Locale.ROOT, "%.2f", millis), "threshold", String.format(java.util.Locale.ROOT, "%.2f", state.thresholdMillis)), List.of("health:tick-monitor")).isPresent()) incidents++;
            lastIncident = tick;
            consecutive = 0;
        }
        state = new State(true, state.thresholdMillis, state.relativeMultiplier, tick, incidents, lastIncident, consecutive, state.lastTriggers, millis, baseline, state.ownsRecorder);
    }

    private static double clamp(double value) { return Double.isFinite(value) ? Math.max(50.0D, Math.min(MAX_THRESHOLD_MILLIS, value)) : DEFAULT_THRESHOLD_MILLIS; }
    private static double clampRelative(double value) { return Double.isFinite(value) ? Math.max(0.0D, Math.min(10.0D, value)) : 0.0D; }
    private record State(boolean running, double thresholdMillis, double relativeMultiplier, long ticks, int incidents, long lastIncidentTick, int consecutiveSlow, int lastTriggers, double lastDurationMillis, double baselineMillis, boolean ownsRecorder) {
        static State idle() { return new State(false, DEFAULT_THRESHOLD_MILLIS, 0.0D, 0, 0, -COOLDOWN_TICKS, 0, 0, 0.0D, 0.0D, false); }
    }
    public record Snapshot(boolean running, double thresholdMillis, double relativeMultiplier, long observedTicks, int incidents, double lastDurationMillis, int consecutiveSlow) { }
}
