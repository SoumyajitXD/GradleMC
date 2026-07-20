package com.soumyajit.gradlemc.metrics;

public final class FrameTimeStatisticsSelfTest {
    private FrameTimeStatisticsSelfTest() {
    }

    public static void run() {
        require(FrameTimeStatistics.fpsForFrameTime(0.0D) == 0.0D, "zero interval must not divide by zero");
        require(FrameTimeStatistics.fpsForFrameTime(20_000_000.0D) == 50.0D, "20 ms must be 50 FPS");
        long[] frames = {10_000_000L, 10_000_000L, 100_000_000L, 20_000_000L};
        require(Math.abs(FrameTimeStatistics.lowFps(frames, 4, 0.25D) - 10.0D) < 0.0001D,
                "slowest-duration percentile must produce low FPS");
        require(FrameTimeStatistics.lowFps(frames, 0, 0.01D) == 0.0D, "empty sample must be safe");
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
