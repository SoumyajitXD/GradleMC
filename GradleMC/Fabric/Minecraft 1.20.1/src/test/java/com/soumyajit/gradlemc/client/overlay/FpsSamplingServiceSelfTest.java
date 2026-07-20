package com.soumyajit.gradlemc.client.overlay;

import com.soumyajit.gradlemc.metrics.FpsTestResult;

import java.time.Instant;

/** Deterministic edge coverage for the one authoritative rendered-frame sampler. */
public final class FpsSamplingServiceSelfTest {
    private FpsSamplingServiceSelfTest() {
    }

    public static void run() {
        firstAndInvalidFramesAreUnavailable();
        completedSessionUsesOnlyValidFrames();
        boundedPercentileStorageDoesNotUnderReportObservedFrames();
        cancellationAndResetDoNotLeakSessions();
    }

    private static void firstAndInvalidFramesAreUnavailable() {
        FpsSamplingService sampler = new FpsSamplingService(30);
        sampler.onRenderedFrame(100L, Instant.EPOCH);
        require(sampler.snapshot(false).sampleCount() == 0, "first frame must not produce a sample");
        sampler.onRenderedFrame(100L, Instant.EPOCH);
        sampler.onRenderedFrame(99L, Instant.EPOCH);
        sampler.onRenderedFrame(2_000_000_100L, Instant.EPOCH);
        require(sampler.snapshot(false).currentFps() == null, "invalid intervals must remain unavailable");
        require(sampler.startTest(5, Instant.EPOCH), "test should start");
        FpsTestResult stopped = sampler.stopTest(Instant.EPOCH).orElseThrow();
        require(!stopped.hasSamples(), "stopping before valid samples must remain unavailable");
    }

    private static void completedSessionUsesOnlyValidFrames() {
        FpsSamplingService sampler = new FpsSamplingService(30);
        require(sampler.startTest(5, Instant.EPOCH), "test should start");
        long now = 1_000L;
        sampler.onRenderedFrame(now, Instant.EPOCH);
        FpsTestResult completed = null;
        for (int index = 0; index < 100; index++) {
            now += 50_000_000L;
            completed = sampler.onRenderedFrame(now, Instant.EPOCH).orElse(completed);
        }
        require(completed != null, "five seconds of valid frames must complete the test");
        require(completed.endReason() == FpsTestResult.EndReason.COMPLETED, "session must complete once");
        require(completed.hasSamples(), "completed valid session must have data");
        require(Math.abs(completed.averageFps() - 20.0D) < 0.0001D, "average must use frame durations");
        require(completed.minFps() == 20 && completed.maxFps() == 20, "min/max must use valid frames");
        require(!sampler.isTestRunning(), "completed test must clear active state");
    }

    private static void cancellationAndResetDoNotLeakSessions() {
        FpsSamplingService sampler = new FpsSamplingService(30);
        require(sampler.startTest(5, Instant.EPOCH), "first session should start");
        sampler.onRenderedFrame(100L, Instant.EPOCH);
        sampler.onRenderedFrame(20_000_100L, Instant.EPOCH);
        FpsTestResult cancelled = sampler.cancelTest(Instant.EPOCH).orElseThrow();
        require(cancelled.endReason() == FpsTestResult.EndReason.CANCELLED, "cancel must preserve reason");
        require(sampler.startTest(5, Instant.EPOCH), "new session should start after cancellation");
        sampler.clearRollingStatistics();
        require(sampler.snapshot(false).sampleCount() == 0, "world reset must clear rolling samples");
        require(sampler.stopTest(Instant.EPOCH).isPresent(), "active test remains controllable after a rolling reset");
    }

    private static void boundedPercentileStorageDoesNotUnderReportObservedFrames() {
        FpsSamplingService sampler = new FpsSamplingService(30);
        require(sampler.startTest(600, Instant.EPOCH), "long test should start");
        long now = 1_000L;
        sampler.onRenderedFrame(now, Instant.EPOCH);
        for (int frame = 0; frame < 145_000; frame++) {
            now += 1_000_000L;
            sampler.onRenderedFrame(now, Instant.EPOCH);
        }
        FpsTestResult stopped = sampler.stopTest(Instant.EPOCH).orElseThrow();
        require(stopped.samples() == 145_000, "reported samples must count every observed frame, not only the bounded percentile buffer");
        require(Math.abs(stopped.actualSeconds() - 145.0D) < 0.0001D, "duration must use the same observed frames");
        require(Math.abs(stopped.averageFps() - 1_000.0D) < 0.0001D, "average must agree with sample count and duration");
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
