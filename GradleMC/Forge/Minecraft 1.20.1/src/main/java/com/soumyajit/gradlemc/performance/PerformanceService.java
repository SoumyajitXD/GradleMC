package com.soumyajit.gradlemc.performance;

import com.soumyajit.gradlemc.config.GradleMCConfig;

/** Single owner for persisted mode and the process-local guard state. */
public final class PerformanceService {
    private static final PerformanceGuard GUARD = new PerformanceGuard();
    private static final GradleMcOverheadMonitor OVERHEAD = new GradleMcOverheadMonitor();
    private static volatile PerformanceMode lastMode = PerformanceMode.BALANCED;
    private static volatile long policyRevision;
    private PerformanceService() { }
    public static PerformanceMode mode() {
        PerformanceMode parsed = PerformanceMode.parse(GradleMCConfig.PERFORMANCE_MODE.get());
        if (parsed != lastMode) { lastMode = parsed; policyRevision++; }
        return parsed;
    }
    public static PerformancePolicy policy() { return mode().policy(); }
    public static PolicySnapshot policySnapshot() { return new PolicySnapshot(policy(), policyRevision); }
    public static void setMode(PerformanceMode mode) {
        lastMode = mode == null ? PerformanceMode.BALANCED : mode;
        policyRevision++;
        GradleMCConfig.PERFORMANCE_MODE.set(lastMode.name());
        GradleMCConfig.SPEC.save();
    }
    public static PerformanceGuard guard() { return GUARD; }
    public static GradleMcOverheadMonitor overhead() { return OVERHEAD; }
    public record PolicySnapshot(PerformancePolicy policy, long revision) { }
}
