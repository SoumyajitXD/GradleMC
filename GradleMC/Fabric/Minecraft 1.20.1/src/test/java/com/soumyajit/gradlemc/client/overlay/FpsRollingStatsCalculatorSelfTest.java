package com.soumyajit.gradlemc.client.overlay;

public final class FpsRollingStatsCalculatorSelfTest {
    private FpsRollingStatsCalculatorSelfTest() {
    }

    public static void run() {
        FpsRollingStatsCalculator calculator = new FpsRollingStatsCalculator(60);
        calculator.recordFrameTimeNanos(10_000_000L, 11_000_000L);
        calculator.recordFrameTimeNanos(20_000_000L, 31_000_000L);
        FpsRollingStatsCalculator.Snapshot snapshot = calculator.snapshot();
        require(snapshot.sampleCount() == 2, "two validated frame intervals must be stored");
        require(Math.abs(snapshot.averageFps() - (2_000_000_000.0D / 30_000_000.0D)) < 0.0001D,
                "average FPS must derive from total frame duration");
        calculator.recordFrameTimeNanos(2_000_000_000L, 2_000_000_000L); // invalid long gap must not poison results
        require(calculator.snapshot().sampleCount() == 2, "invalid long interval must be ignored");
        calculator.clear();
        require(calculator.snapshot().sampleCount() == 0, "clear must discard bounded rolling samples");
        long now = 0L;
        FpsRollingStatsCalculator longSession = new FpsRollingStatsCalculator(120);
        for (int index = 0; index < 100_000; index++) {
            now += 1_000_000L;
            longSession.recordFrameTimeNanos(1_000_000L, now);
        }
        require(longSession.snapshot(false).sampleCount() <= longSession.capacity(), "long-session FPS samples must remain bounded");
        require(longSession.capacity() <= 57_600, "FPS primitive buffer budget must be enforced");
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
