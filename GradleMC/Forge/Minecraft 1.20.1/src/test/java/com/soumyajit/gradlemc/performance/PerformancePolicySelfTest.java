package com.soumyajit.gradlemc.performance;

public final class PerformancePolicySelfTest {
    private PerformancePolicySelfTest() { }
    public static void run() {
        check(PerformanceMode.parse(null) == PerformanceMode.BALANCED, "balanced default");
        check(PerformanceMode.parse("bad") == PerformanceMode.BALANCED, "invalid fallback");
        check(PerformanceMode.LOW_IMPACT.policy().aggressiveCoalescing(), "low coalesces");
        check(!PerformanceMode.DETAILED.policy().passiveObservation(), "detailed has no idle sampling");
        check(PerformanceMode.DETAILED.policy().maxHeavyOperations() == 1, "hard heavy cap");
        GradleMcOverheadMonitor monitor = new GradleMcOverheadMonitor();
        monitor.record(GradleMcOverheadMonitor.Category.FILE_WRITING, 1_000_000L);
        monitor.record(GradleMcOverheadMonitor.Category.FILE_WRITING, 3_000_000L);
        monitor.coalesced(); monitor.rejected();
        var snapshot = monitor.snapshot();
        var stat = snapshot.categories()[GradleMcOverheadMonitor.Category.FILE_WRITING.ordinal()];
        check(stat.samples() == 2 && stat.medianMillis() >= 1.0D && !Double.isFinite(stat.p95Millis()), "bounded warming-up timings");
        check(snapshot.rejectionCount() == 1 && snapshot.coalescingOrDeferralCount() == 1, "bounded counters");
    }
    private static void check(boolean value, String label) { if (!value) throw new AssertionError(label); }
}
