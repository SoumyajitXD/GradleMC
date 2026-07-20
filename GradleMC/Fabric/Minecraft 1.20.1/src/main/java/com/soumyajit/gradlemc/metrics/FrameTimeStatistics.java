package com.soumyajit.gradlemc.metrics;

import java.util.Arrays;

/** Pure frame-duration calculations shared by the Fabric HUD and bounded FPS tests. */
public final class FrameTimeStatistics {
    public static final long NANOS_PER_SECOND = 1_000_000_000L;

    private FrameTimeStatistics() {
    }

    public static double lowFps(long[] frameTimesNanos, int sampleCount, double fraction) {
        if (frameTimesNanos == null || sampleCount <= 0 || fraction <= 0.0D) return 0.0D;
        int count = Math.min(sampleCount, frameTimesNanos.length);
        long[] sorted = Arrays.copyOf(frameTimesNanos, count);
        Arrays.sort(sorted);
        int lowCount = Math.max(1, (int) Math.ceil(count * Math.min(1.0D, fraction)));
        double total = 0.0D;
        for (int index = count - lowCount; index < count; index++) total += sorted[index];
        return fpsForFrameTime(total / lowCount);
    }

    public static double fpsForFrameTime(double frameTimeNanos) {
        return frameTimeNanos <= 0.0D ? 0.0D : NANOS_PER_SECOND / frameTimeNanos;
    }
}
