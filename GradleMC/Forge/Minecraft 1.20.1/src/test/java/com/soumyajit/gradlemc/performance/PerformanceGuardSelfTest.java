package com.soumyajit.gradlemc.performance;

/** Deterministic policy checks; deliberately avoids wall-clock assertions. */
public final class PerformanceGuardSelfTest {
    private PerformanceGuardSelfTest() { }
    public static void run() {
        PerformanceGuard guard = new PerformanceGuard();
        long now = 20_000_000_000L;
        guard.observe(now, 200, 0, 0, 0, false);
        require(guard.snapshot().state() == PerformanceGuard.State.NORMAL, "one bad frame must not trigger guard");
        guard.observe(now += 1_000_000_000L, 200, 0, 0, 0, false);
        guard.observe(now += 1_000_000_000L, 200, 0, 0, 0, false);
        require(guard.snapshot().state() == PerformanceGuard.State.CONSTRAINED && guard.deferOptionalWork(), "sustained pressure must defer optional work");
        for (int index = 0; index < 6; index++) guard.observe(now += 2_000_000_000L, 10, 10, 0, 0, false);
        require(guard.snapshot().state() != PerformanceGuard.State.CONSTRAINED, "recovery must use bounded hysteresis");
        require(PerformanceMode.parse("invalid") == PerformanceMode.BALANCED, "invalid mode must migrate to balanced");
    }
    private static void require(boolean condition, String message) { if (!condition) throw new IllegalStateException(message); }
}
