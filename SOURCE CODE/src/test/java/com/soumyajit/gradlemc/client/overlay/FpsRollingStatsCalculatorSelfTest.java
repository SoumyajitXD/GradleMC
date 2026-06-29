package com.soumyajit.gradlemc.client.overlay;

public final class FpsRollingStatsCalculatorSelfTest {
    private static final long SIXTY_FPS_NANOS = 16_666_667L;
    private static final long THIRTY_FPS_NANOS = 33_333_333L;

    private FpsRollingStatsCalculatorSelfTest() {
    }

    public static void run() {
        emptySnapshotIsFinite();
        averageFpsUsesFrameTimes();
        lowsNeedEnoughSamples();
        onePercentLowUsesWorstFrameTimes();
        pointOnePercentLowUsesWorstFrameTimes();
    }

    private static void emptySnapshotIsFinite() {
        FpsRollingStatsCalculator.Snapshot snapshot = new FpsRollingStatsCalculator(60).snapshot();
        assertEquals(0, snapshot.sampleCount(), "empty sample count");
        assertFinite(snapshot.currentFps(), "empty current FPS");
        assertFinite(snapshot.averageFps(), "empty average FPS");
        assertTrue(!snapshot.hasOnePercentLow(), "empty 1% low should be unavailable");
        assertTrue(!snapshot.hasPointOnePercentLow(), "empty 0.1% low should be unavailable");
    }

    private static void averageFpsUsesFrameTimes() {
        FpsRollingStatsCalculator calculator = new FpsRollingStatsCalculator(60);
        for (int index = 0; index < 60; index++) {
            calculator.recordFrameTimeNanos(SIXTY_FPS_NANOS);
        }
        FpsRollingStatsCalculator.Snapshot snapshot = calculator.snapshot();
        assertBetween(snapshot.averageFps(), 59.0D, 61.0D, "average FPS should be near 60");
    }

    private static void lowsNeedEnoughSamples() {
        FpsRollingStatsCalculator calculator = new FpsRollingStatsCalculator(60);
        for (int index = 0; index < 99; index++) {
            calculator.recordFrameTimeNanos(SIXTY_FPS_NANOS);
        }
        FpsRollingStatsCalculator.Snapshot snapshot = calculator.snapshot();
        assertTrue(!snapshot.hasOnePercentLow(), "1% low should warm up until 100 samples");
        assertTrue(!snapshot.hasPointOnePercentLow(), "0.1% low should warm up until 1000 samples");
    }

    private static void onePercentLowUsesWorstFrameTimes() {
        FpsRollingStatsCalculator calculator = new FpsRollingStatsCalculator(60);
        for (int index = 0; index < 99; index++) {
            calculator.recordFrameTimeNanos(SIXTY_FPS_NANOS);
        }
        calculator.recordFrameTimeNanos(THIRTY_FPS_NANOS);
        FpsRollingStatsCalculator.Snapshot snapshot = calculator.snapshot();
        assertTrue(snapshot.hasOnePercentLow(), "1% low should be available");
        assertBetween(snapshot.onePercentLowFps(), 29.0D, 31.0D, "1% low should reflect the worst 1 frame out of 100");
    }

    private static void pointOnePercentLowUsesWorstFrameTimes() {
        FpsRollingStatsCalculator calculator = new FpsRollingStatsCalculator(60);
        for (int index = 0; index < 999; index++) {
            calculator.recordFrameTimeNanos(SIXTY_FPS_NANOS);
        }
        calculator.recordFrameTimeNanos(THIRTY_FPS_NANOS);
        FpsRollingStatsCalculator.Snapshot snapshot = calculator.snapshot();
        assertTrue(snapshot.hasPointOnePercentLow(), "0.1% low should be available");
        assertBetween(snapshot.pointOnePercentLowFps(), 29.0D, 31.0D, "0.1% low should reflect the worst 1 frame out of 1000");
    }

    private static void assertFinite(double value, String message) {
        if (!Double.isFinite(value)) {
            throw new AssertionError(message + " was not finite: " + value);
        }
    }

    private static void assertBetween(double actual, double minInclusive, double maxInclusive, String message) {
        if (actual < minInclusive || actual > maxInclusive) {
            throw new AssertionError(message + " actual=" + actual + " expectedRange=" + minInclusive + ".." + maxInclusive);
        }
    }

    private static void assertEquals(int expected, int actual, String message) {
        if (expected != actual) {
            throw new AssertionError(message + " expected=" + expected + " actual=" + actual);
        }
    }

    private static void assertTrue(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
