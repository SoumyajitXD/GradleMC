package com.soumyajit.gradlemc.client.overlay;

import com.soumyajit.gradlemc.metrics.FrameTimeStatistics;

import java.util.Arrays;

/**
 * Bounded, monotonic-clock statistics for completed rendered-frame intervals.
 *
 * <p>Every accepted observation is one interval between two {@code RenderGuiEvent.Post}
 * callbacks. The rolling average is {@code completedIntervals / summedIntervalSeconds}; it
 * is never derived from game ticks or from averages of earlier samples. Paused, unfocused,
 * menu, and invalid-gap observations reset the interval baseline instead of inventing a frame.</p>
 */
public final class FpsRollingStatsCalculator {
    private static final int MAX_SAMPLES_PER_SECOND = 2_400;
    private static final long MAX_VALID_FRAME_TIME_NANOS = FrameTimeStatistics.NANOS_PER_SECOND;
    private static final int MIN_ONE_PERCENT_SAMPLES = 100;
    private static final int MIN_POINT_ONE_PERCENT_SAMPLES = 1_000;

    private long[] frameTimesNanos;
    private long[] frameEndNanos;
    private long windowNanos;
    private int nextIndex;
    private int sampleCount;
    private long totalFrameTimeNanos;
    private long lastFrameNanos = -1L;
    private long syntheticNowNanos;
    private Double cachedOnePercentLow;
    private Double cachedPointOnePercentLow;

    public FpsRollingStatsCalculator(int windowSeconds) {
        setWindowSeconds(windowSeconds);
    }

    public void setWindowSeconds(int windowSeconds) {
        int boundedWindow = Math.max(30, Math.min(120, windowSeconds));
        int capacity = boundedWindow * MAX_SAMPLES_PER_SECOND;
        windowNanos = boundedWindow * FrameTimeStatistics.NANOS_PER_SECOND;
        if (frameTimesNanos != null && frameTimesNanos.length == capacity) {
            return;
        }
        frameTimesNanos = new long[capacity];
        frameEndNanos = new long[capacity];
        clear();
    }

    /** Records one completed frame observation from a monotonic timestamp. */
    public void recordRenderedFrame(long nowNanos) {
        if (nowNanos <= 0L) {
            resetInterval();
            return;
        }
        if (lastFrameNanos < 0L) {
            lastFrameNanos = nowNanos;
            return;
        }
        long frameTimeNanos = nowNanos - lastFrameNanos;
        lastFrameNanos = nowNanos;
        recordFrameTimeNanos(frameTimeNanos, nowNanos);
    }

    /** Clears only the pending timestamp after a non-measuring client state. */
    public void resetInterval() {
        lastFrameNanos = -1L;
        syntheticNowNanos = 0L;
    }

    /** Clears the rolling window, including percentile caches. */
    public void clear() {
        nextIndex = 0;
        sampleCount = 0;
        totalFrameTimeNanos = 0L;
        lastFrameNanos = -1L;
        cachedOnePercentLow = null;
        cachedPointOnePercentLow = null;
    }

    /** Package-visible for deterministic tests of frame durations. */
    void recordFrameTimeNanos(long frameTimeNanos) {
        syntheticNowNanos += Math.max(0L, frameTimeNanos);
        recordFrameTimeNanos(frameTimeNanos, syntheticNowNanos);
    }

    private void recordFrameTimeNanos(long frameTimeNanos, long frameEndNanos) {
        if (frameTimeNanos <= 0L || frameTimeNanos > MAX_VALID_FRAME_TIME_NANOS || frameTimesNanos.length == 0) {
            return;
        }
        discardExpired(frameEndNanos);
        if (sampleCount == frameTimesNanos.length) {
            totalFrameTimeNanos -= frameTimesNanos[nextIndex];
        } else {
            sampleCount++;
        }
        frameTimesNanos[nextIndex] = frameTimeNanos;
        this.frameEndNanos[nextIndex] = frameEndNanos;
        totalFrameTimeNanos += frameTimeNanos;
        nextIndex = (nextIndex + 1) % frameTimesNanos.length;
        cachedOnePercentLow = null;
        cachedPointOnePercentLow = null;
    }

    private void discardExpired(long nowNanos) {
        long oldestAllowed = nowNanos - windowNanos;
        while (sampleCount > 0) {
            int oldestIndex = nextIndex - sampleCount;
            if (oldestIndex < 0) {
                oldestIndex += frameTimesNanos.length;
            }
            if (frameEndNanos[oldestIndex] >= oldestAllowed) {
                return;
            }
            totalFrameTimeNanos -= frameTimesNanos[oldestIndex];
            sampleCount--;
        }
    }

    public Snapshot snapshot() {
        return snapshot(true);
    }

    /** Percentiles require a sort; callers that refresh display frequently can reuse the last values. */
    public Snapshot snapshot(boolean refreshPercentiles) {
        if (sampleCount <= 0 || totalFrameTimeNanos <= 0L) {
            return Snapshot.empty();
        }
        double averageFps = FrameTimeStatistics.NANOS_PER_SECOND * (double) sampleCount / totalFrameTimeNanos;
        double currentFps = recentFps();
        if (refreshPercentiles && (cachedOnePercentLow == null || cachedPointOnePercentLow == null)) {
            long[] copy = orderedSamples();
            Arrays.sort(copy);
            cachedOnePercentLow = sampleCount >= MIN_ONE_PERCENT_SAMPLES ? lowFps(copy, 0.01D) : null;
            cachedPointOnePercentLow = sampleCount >= MIN_POINT_ONE_PERCENT_SAMPLES ? lowFps(copy, 0.001D) : null;
        }
        return new Snapshot(sampleCount, finite(currentFps), finite(averageFps), cachedOnePercentLow, cachedPointOnePercentLow);
    }

    /** A recent approximately-one-second frame-time window, which remains responsive to stutters. */
    private double recentFps() {
        long elapsed = 0L;
        int frames = 0;
        int index = nextIndex - 1;
        if (index < 0) {
            index = frameTimesNanos.length - 1;
        }
        while (frames < sampleCount && elapsed < FrameTimeStatistics.NANOS_PER_SECOND) {
            elapsed += frameTimesNanos[index];
            frames++;
            index--;
            if (index < 0) {
                index = frameTimesNanos.length - 1;
            }
        }
        return elapsed <= 0L ? 0.0D : FrameTimeStatistics.NANOS_PER_SECOND * (double) frames / elapsed;
    }

    private long[] orderedSamples() {
        if (sampleCount < frameTimesNanos.length) {
            return Arrays.copyOf(frameTimesNanos, sampleCount);
        }
        long[] copy = new long[sampleCount];
        int tailLength = frameTimesNanos.length - nextIndex;
        System.arraycopy(frameTimesNanos, nextIndex, copy, 0, tailLength);
        System.arraycopy(frameTimesNanos, 0, copy, tailLength, nextIndex);
        return copy;
    }

    private static double lowFps(long[] sortedFrameTimesNanos, double percentile) {
        int count = Math.max(1, (int) Math.ceil(sortedFrameTimesNanos.length * percentile));
        long total = 0L;
        for (int index = sortedFrameTimesNanos.length - count; index < sortedFrameTimesNanos.length; index++) {
            total += sortedFrameTimesNanos[index];
        }
        return finite(FrameTimeStatistics.fpsForFrameTime(total / (double) count));
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
    }
}
