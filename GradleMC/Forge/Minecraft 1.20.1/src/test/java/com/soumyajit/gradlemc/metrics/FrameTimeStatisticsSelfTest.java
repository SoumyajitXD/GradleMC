package com.soumyajit.gradlemc.metrics;

/** Regression checks for frame-duration percentile semantics. */
public final class FrameTimeStatisticsSelfTest {
    private FrameTimeStatisticsSelfTest() {
    }

    public static void run() {
        long[] frameTimes = {10_000_000L, 10_000_000L, 10_000_000L, 100_000_000L};
        assertClose(10.0D, FrameTimeStatistics.lowFps(frameTimes, frameTimes.length, 0.01D), "slowest frame is the 1% low");
        assertClose(100.0D, FrameTimeStatistics.lowFps(frameTimes, 3, 0.01D), "sample count bounds the calculation");
        assertClose(0.0D, FrameTimeStatistics.lowFps(frameTimes, 0, 0.01D), "empty sample");
        assertClose(50.0D, FrameTimeStatistics.fpsForFrameTime(20_000_000L), "frame-time conversion");
    }

    private static void assertClose(double expected, double actual, String label) {
        if (Math.abs(expected - actual) > 0.0001D) {
            throw new AssertionError(label + ": expected " + expected + " but got " + actual);
        }
    }
}
