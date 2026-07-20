package com.soumyajit.gradlemc.client.overlay;

import com.soumyajit.gradlemc.metrics.FrameTimeStatistics;

import java.util.Arrays;

/** Bounded statistics storage for intervals validated by {@link FpsSamplingService}. */
public final class FpsRollingStatsCalculator {
    private static final int MAX_SAMPLES_PER_SECOND = 480;
    private static final long MAX_VALID_FRAME_TIME_NANOS = FrameTimeStatistics.NANOS_PER_SECOND;
    private static final int MIN_ONE_PERCENT_SAMPLES = 100;
    private static final int MIN_POINT_ONE_PERCENT_SAMPLES = 1_000;
    private static final long PERCENTILE_REFRESH_NANOS = 5L * FrameTimeStatistics.NANOS_PER_SECOND;

    private long[] frameTimesNanos;
    private long[] frameEndNanos;
    private long windowNanos;
    private int nextIndex;
    private int sampleCount;
    private long totalFrameTimeNanos;
    private Double cachedOnePercentLow;
    private Double cachedPointOnePercentLow;
    private long lastPercentileRefreshNanos = Long.MIN_VALUE;
    private long latestFrameEndNanos;

    public FpsRollingStatsCalculator(int windowSeconds) { setWindowSeconds(windowSeconds); }

    public int capacity() { return frameTimesNanos.length; }

    public void setWindowSeconds(int windowSeconds) {
        int boundedWindow = Math.max(30, Math.min(120, windowSeconds));
        int capacity = boundedWindow * MAX_SAMPLES_PER_SECOND;
        windowNanos = boundedWindow * FrameTimeStatistics.NANOS_PER_SECOND;
        if (frameTimesNanos != null && frameTimesNanos.length == capacity) return;
        frameTimesNanos = new long[capacity];
        frameEndNanos = new long[capacity];
        clear();
    }

    public void clear() {
        nextIndex = 0;
        sampleCount = 0;
        totalFrameTimeNanos = 0L;
        cachedOnePercentLow = null;
        cachedPointOnePercentLow = null;
        lastPercentileRefreshNanos = Long.MIN_VALUE;
        latestFrameEndNanos = 0L;
    }

    /** Adds an already-validated interval from the authoritative frame sampler. */
    public void recordFrameTimeNanos(long frameTimeNanos, long frameEndNanos) {
        recordFrameTime(frameTimeNanos, frameEndNanos);
    }

    private void recordFrameTime(long frameTimeNanos, long frameEndNanos) {
        if (frameTimeNanos <= 0L || frameTimeNanos > MAX_VALID_FRAME_TIME_NANOS || frameTimesNanos.length == 0) return;
        discardExpired(frameEndNanos);
        if (sampleCount == frameTimesNanos.length) totalFrameTimeNanos -= frameTimesNanos[nextIndex];
        else sampleCount++;
        frameTimesNanos[nextIndex] = frameTimeNanos;
        this.frameEndNanos[nextIndex] = frameEndNanos;
        latestFrameEndNanos = frameEndNanos;
        totalFrameTimeNanos += frameTimeNanos;
        nextIndex = (nextIndex + 1) % frameTimesNanos.length;
    }

    private void discardExpired(long nowNanos) {
        long oldestAllowed = nowNanos - windowNanos;
        while (sampleCount > 0) {
            int oldestIndex = nextIndex - sampleCount;
            if (oldestIndex < 0) oldestIndex += frameTimesNanos.length;
            if (frameEndNanos[oldestIndex] >= oldestAllowed) return;
            totalFrameTimeNanos -= frameTimesNanos[oldestIndex];
            sampleCount--;
        }
    }

    public Snapshot snapshot() { return snapshot(true); }

    public Snapshot snapshot(boolean refreshPercentiles) {
        if (sampleCount <= 0 || totalFrameTimeNanos <= 0L) return Snapshot.empty();
        if (refreshPercentiles && sampleCount >= MIN_ONE_PERCENT_SAMPLES
                && (lastPercentileRefreshNanos == Long.MIN_VALUE || latestFrameEndNanos - lastPercentileRefreshNanos >= PERCENTILE_REFRESH_NANOS)) {
            long[] copy = orderedSamples();
            Arrays.sort(copy);
            cachedOnePercentLow = sampleCount >= MIN_ONE_PERCENT_SAMPLES ? lowFps(copy, 0.01D) : null;
            cachedPointOnePercentLow = sampleCount >= MIN_POINT_ONE_PERCENT_SAMPLES ? lowFps(copy, 0.001D) : null;
            lastPercentileRefreshNanos = latestFrameEndNanos;
        }
        double average = FrameTimeStatistics.NANOS_PER_SECOND * (double) sampleCount / totalFrameTimeNanos;
        return new Snapshot(sampleCount, finite(recentFps()), finite(average), cachedOnePercentLow, cachedPointOnePercentLow);
    }

    private double recentFps() {
        long elapsed = 0L;
        int frames = 0;
        int index = nextIndex - 1;
        if (index < 0) index = frameTimesNanos.length - 1;
        while (frames < sampleCount && elapsed < FrameTimeStatistics.NANOS_PER_SECOND) {
            elapsed += frameTimesNanos[index];
            frames++;
            index--;
            if (index < 0) index = frameTimesNanos.length - 1;
        }
        return elapsed <= 0L ? 0.0D : FrameTimeStatistics.NANOS_PER_SECOND * (double) frames / elapsed;
    }

    private long[] orderedSamples() {
        if (sampleCount < frameTimesNanos.length) return Arrays.copyOf(frameTimesNanos, sampleCount);
        long[] copy = new long[sampleCount];
        int tailLength = frameTimesNanos.length - nextIndex;
        System.arraycopy(frameTimesNanos, nextIndex, copy, 0, tailLength);
        System.arraycopy(frameTimesNanos, 0, copy, tailLength, nextIndex);
        return copy;
    }

    private static double lowFps(long[] sorted, double percentile) {
        int count = Math.max(1, (int) Math.ceil(sorted.length * percentile));
        long total = 0L;
        for (int index = sorted.length - count; index < sorted.length; index++) total += sorted[index];
        return finite(FrameTimeStatistics.fpsForFrameTime(total / (double) count));
    }

    private static double finite(double value) { return Double.isFinite(value) ? Math.max(0.0D, value) : 0.0D; }

    public record Snapshot(int sampleCount, Double currentFps, Double averageFps, Double onePercentLowFps,
                           Double pointOnePercentLowFps) {
        public static Snapshot empty() { return new Snapshot(0, null, null, null, null); }
    }
}
