package com.soumyajit.gradlemc.client.overlay;

import java.util.Arrays;

public final class FpsRollingStatsCalculator {
    private static final long NANOS_PER_SECOND = 1_000_000_000L;
    private static final int ASSUMED_MAX_FPS = 1000;
    private static final int MIN_ONE_PERCENT_SAMPLES = 100;
    private static final int MIN_POINT_ONE_PERCENT_SAMPLES = 1000;

    private long[] frameTimesNanos;
    private int nextIndex;
    private int sampleCount;
    private long totalFrameTimeNanos;
    private long lastFrameTimeNanos;

    public FpsRollingStatsCalculator(int windowSeconds) {
        setWindowSeconds(windowSeconds);
    }

    public void setWindowSeconds(int windowSeconds) {
        int boundedWindow = Math.max(30, Math.min(120, windowSeconds));
        int capacity = boundedWindow * ASSUMED_MAX_FPS;
        if (frameTimesNanos != null && frameTimesNanos.length == capacity) {
            return;
        }
        frameTimesNanos = new long[capacity];
        nextIndex = 0;
        sampleCount = 0;
        totalFrameTimeNanos = 0L;
        lastFrameTimeNanos = 0L;
    }

    public void recordFrameTimeNanos(long frameTimeNanos) {
        if (frameTimeNanos <= 0L || frameTimesNanos.length == 0) {
            return;
        }
        long bounded = Math.min(frameTimeNanos, NANOS_PER_SECOND);
        if (sampleCount < frameTimesNanos.length) {
            sampleCount++;
        } else {
            totalFrameTimeNanos -= frameTimesNanos[nextIndex];
        }
        frameTimesNanos[nextIndex] = bounded;
        totalFrameTimeNanos += bounded;
        lastFrameTimeNanos = bounded;
        nextIndex = (nextIndex + 1) % frameTimesNanos.length;
    }

    public Snapshot snapshot() {
        if (sampleCount <= 0 || totalFrameTimeNanos <= 0L) {
            return Snapshot.empty();
        }
        long[] copy = Arrays.copyOf(frameTimesNanos, sampleCount);
        Arrays.sort(copy);
        double currentFps = fpsFromFrameTime(lastFrameTimeNanos);
        double averageFps = (NANOS_PER_SECOND * (double) sampleCount) / totalFrameTimeNanos;
        Double onePercentLow = sampleCount >= MIN_ONE_PERCENT_SAMPLES
                ? lowFps(copy, 0.01D)
                : null;
        Double pointOnePercentLow = sampleCount >= MIN_POINT_ONE_PERCENT_SAMPLES
                ? lowFps(copy, 0.001D)
                : null;
        return new Snapshot(sampleCount, finite(currentFps), finite(averageFps), onePercentLow, pointOnePercentLow);
    }

    private static double lowFps(long[] sortedFrameTimesNanos, double percentile) {
        int count = Math.max(1, (int) Math.ceil(sortedFrameTimesNanos.length * percentile));
        long total = 0L;
        for (int index = sortedFrameTimesNanos.length - count; index < sortedFrameTimesNanos.length; index++) {
            total += sortedFrameTimesNanos[index];
        }
        return finite(fpsFromFrameTime(total / (double) count));
    }

    private static double fpsFromFrameTime(double frameTimeNanos) {
        return frameTimeNanos <= 0.0D ? 0.0D : NANOS_PER_SECOND / frameTimeNanos;
    }

    private static double finite(double value) {
        return Double.isFinite(value) ? Math.max(0.0D, value) : 0.0D;
    }

    public record Snapshot(int sampleCount, double currentFps, double averageFps, Double onePercentLowFps,
                           Double pointOnePercentLowFps) {
        public static Snapshot empty() {
            return new Snapshot(0, 0.0D, 0.0D, null, null);
        }

        public boolean hasOnePercentLow() {
            return onePercentLowFps != null;
        }

        public boolean hasPointOnePercentLow() {
            return pointOnePercentLowFps != null;
        }

        public Snapshot withCurrentFps(int currentFps) {
            return new Snapshot(sampleCount, Math.max(0, currentFps), averageFps, onePercentLowFps, pointOnePercentLowFps);
        }
    }
}
