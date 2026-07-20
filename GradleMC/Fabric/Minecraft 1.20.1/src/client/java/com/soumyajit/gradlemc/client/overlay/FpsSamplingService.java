package com.soumyajit.gradlemc.client.overlay;

import com.soumyajit.gradlemc.metrics.DiagnosticTestProgress;
import com.soumyajit.gradlemc.metrics.FpsTestResult;
import com.soumyajit.gradlemc.metrics.FrameTimeStatistics;

import java.time.Instant;
import java.util.Optional;
import java.util.OptionalDouble;

/**
 * The one client-side authority for rendered-frame timing. Call it exactly once from the HUD render
 * callback; consumers receive immutable snapshots instead of maintaining their own FPS counters.
 */
public final class FpsSamplingService {
    private static final long MAX_VALID_FRAME_TIME_NANOS = FrameTimeStatistics.NANOS_PER_SECOND;
    private static final int MAX_TEST_SAMPLES = 144_000;

    private final FpsRollingStatsCalculator rollingStats;
    private long lastFrameNanos = -1L;
    private TestSession activeTest;

    public FpsSamplingService(int rollingWindowSeconds) {
        rollingStats = new FpsRollingStatsCalculator(rollingWindowSeconds);
    }

    public void setRollingWindowSeconds(int seconds) {
        rollingStats.setWindowSeconds(seconds);
    }

    public FpsRollingStatsCalculator.Snapshot snapshot(boolean refreshPercentiles) {
        return rollingStats.snapshot(refreshPercentiles);
    }

    public void resetInterval() {
        lastFrameNanos = -1L;
    }

    public void reset() {
        lastFrameNanos = -1L;
        rollingStats.clear();
        activeTest = null;
    }

    public void clearRollingStatistics() {
        lastFrameNanos = -1L;
        rollingStats.clear();
    }

    public boolean startTest(int seconds, Instant startedAt) {
        if (activeTest != null || seconds <= 0) {
            return false;
        }
        activeTest = new TestSession(seconds, startedAt);
        return true;
    }

    public boolean isTestRunning() {
        return activeTest != null;
    }

    public DiagnosticTestProgress progress() {
        TestSession test = activeTest;
        return test == null ? DiagnosticTestProgress.idle() : test.progress();
    }

    /** Pauses only the pending interval; menus and focus changes do not become artificial stutters. */
    public void pause() {
        resetInterval();
    }

    public Optional<FpsTestResult> cancelTest(Instant endedAt) {
        return finish(FpsTestResult.EndReason.CANCELLED, endedAt);
    }

    public Optional<FpsTestResult> stopTest(Instant endedAt) {
        return finish(FpsTestResult.EndReason.STOPPED, endedAt);
    }

    public Optional<FpsTestResult> failTest(Instant endedAt) {
        return finish(FpsTestResult.EndReason.ERROR, endedAt);
    }

    /**
     * Records one active rendered frame. Invalid, duplicate and over-one-second intervals are ignored,
     * not converted into a zero-FPS sample. Completion is returned once and clears the active test.
     */
    public Optional<FpsTestResult> onRenderedFrame(long nowNanos, Instant endedAt) {
        if (nowNanos <= 0L) {
            resetInterval();
            return Optional.empty();
        }
        if (lastFrameNanos < 0L) {
            lastFrameNanos = nowNanos;
            return Optional.empty();
        }
        long frameTimeNanos = nowNanos - lastFrameNanos;
        lastFrameNanos = nowNanos;
        if (frameTimeNanos <= 0L || frameTimeNanos > MAX_VALID_FRAME_TIME_NANOS) {
            return Optional.empty();
        }
        rollingStats.recordFrameTimeNanos(frameTimeNanos, nowNanos);
        TestSession test = activeTest;
        if (test == null) {
            return Optional.empty();
        }
        test.record(frameTimeNanos);
        return test.elapsedNanos >= test.requestedNanos ? finish(FpsTestResult.EndReason.COMPLETED, endedAt) : Optional.empty();
    }

    /** Avoids allocating a wall-clock timestamp for ordinary sampled frames. */
    public Optional<FpsTestResult> onRenderedFrame(long nowNanos) {
        if (nowNanos <= 0L) { resetInterval(); return Optional.empty(); }
        if (lastFrameNanos < 0L) { lastFrameNanos = nowNanos; return Optional.empty(); }
        long frameTimeNanos = nowNanos - lastFrameNanos;
        lastFrameNanos = nowNanos;
        if (frameTimeNanos <= 0L || frameTimeNanos > MAX_VALID_FRAME_TIME_NANOS) return Optional.empty();
        rollingStats.recordFrameTimeNanos(frameTimeNanos, nowNanos);
        TestSession test = activeTest;
        if (test == null) return Optional.empty();
        test.record(frameTimeNanos);
        return test.elapsedNanos >= test.requestedNanos ? finish(FpsTestResult.EndReason.COMPLETED, Instant.now()) : Optional.empty();
    }

    private Optional<FpsTestResult> finish(FpsTestResult.EndReason reason, Instant endedAt) {
        TestSession test = activeTest;
        if (test == null) {
            return Optional.empty();
        }
        activeTest = null;
        resetInterval();
        return Optional.of(test.result(reason, endedAt));
    }

    private static final class TestSession {
        private final int requestedSeconds;
        private final long requestedNanos;
        private final Instant startedAt;
        private final long[] sampledFrameTimes;
        private long elapsedNanos;
        private long observedFrames;
        private long minFrameNanos = Long.MAX_VALUE;
        private long maxFrameNanos;
        private int sampleCount;

        private TestSession(int requestedSeconds, Instant startedAt) {
            this.requestedSeconds = requestedSeconds;
            this.requestedNanos = requestedSeconds * FrameTimeStatistics.NANOS_PER_SECOND;
            this.startedAt = startedAt;
            this.sampledFrameTimes = new long[Math.max(120, Math.min(MAX_TEST_SAMPLES, requestedSeconds * 240))];
        }

        private void record(long frameTimeNanos) {
            elapsedNanos += frameTimeNanos;
            observedFrames++;
            minFrameNanos = Math.min(minFrameNanos, frameTimeNanos);
            maxFrameNanos = Math.max(maxFrameNanos, frameTimeNanos);
            if (sampleCount < sampledFrameTimes.length) {
                sampledFrameTimes[sampleCount++] = frameTimeNanos;
            }
        }

        private DiagnosticTestProgress progress() {
            return new DiagnosticTestProgress(true, requestedSeconds,
                    (int) Math.min(requestedSeconds, elapsedNanos / FrameTimeStatistics.NANOS_PER_SECOND));
        }

        private FpsTestResult result(FpsTestResult.EndReason reason, Instant endedAt) {
            if (observedFrames == 0L || elapsedNanos <= 0L) {
                return new FpsTestResult(requestedSeconds, 0.0D, 0, null, null, null,
                        OptionalDouble.empty(), startedAt, endedAt, reason);
            }
            double elapsedSeconds = elapsedNanos / (double) FrameTimeStatistics.NANOS_PER_SECOND;
            int reportedFrames = (int) Math.min(Integer.MAX_VALUE, observedFrames);
            return new FpsTestResult(requestedSeconds, elapsedSeconds, reportedFrames,
                    observedFrames / elapsedSeconds,
                    (int) Math.round(FrameTimeStatistics.fpsForFrameTime(maxFrameNanos)),
                    (int) Math.round(FrameTimeStatistics.fpsForFrameTime(minFrameNanos)),
                    sampleCount == 0 ? OptionalDouble.empty()
                            : OptionalDouble.of(FrameTimeStatistics.lowFps(sampledFrameTimes, sampleCount, 0.01D)),
                    startedAt, endedAt, reason);
        }
    }
}
