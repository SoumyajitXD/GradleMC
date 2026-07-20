package com.soumyajit.gradlemc.client.overlay;

import com.soumyajit.gradlemc.metrics.FpsTestResult;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class FpsAndOverlayRegressionTest {
    @Test
    void renderedFramesProduceAnIsolatedCompletedSession() {
        FpsSamplingService sampler = new FpsSamplingService(30);
        assertTrue(sampler.startTest(5, Instant.EPOCH));
        long now = 1_000L;
        sampler.onRenderedFrame(now, Instant.EPOCH); // the first frame has no interval
        FpsTestResult result = null;
        for (int frame = 0; frame < 100; frame++) {
            now += 50_000_000L;
            result = sampler.onRenderedFrame(now, Instant.EPOCH).orElse(result);
        }

        assertNotNull(result);
        assertEquals(FpsTestResult.EndReason.COMPLETED, result.endReason());
        assertEquals(20.0D, result.averageFps(), 0.0001D);
        assertEquals(20, result.minFps());
        assertEquals(20, result.maxFps());
        assertFalse(sampler.isTestRunning());
    }

    @Test
    void rejectsBadIntervalsAndCancellationDoesNotLeakIntoTheNextSession() {
        FpsSamplingService sampler = new FpsSamplingService(30);
        sampler.onRenderedFrame(100L, Instant.EPOCH);
        sampler.onRenderedFrame(100L, Instant.EPOCH);
        sampler.onRenderedFrame(99L, Instant.EPOCH);
        sampler.onRenderedFrame(2_000_000_100L, Instant.EPOCH);
        assertNull(sampler.snapshot(false).currentFps());

        assertTrue(sampler.startTest(5, Instant.EPOCH));
        FpsTestResult cancelled = sampler.cancelTest(Instant.EPOCH).orElseThrow();
        assertEquals(FpsTestResult.EndReason.CANCELLED, cancelled.endReason());
        assertTrue(sampler.startTest(5, Instant.EPOCH));
        assertFalse(sampler.stopTest(Instant.EPOCH).orElseThrow().hasSamples());
    }

    @Test
    void overlayRowsRemainIndependentAndEmptyWhenEveryComponentIsOff() {
        FpsRollingStatsCalculator.Snapshot snapshot = new FpsRollingStatsCalculator.Snapshot(4, 120.0D, 90.0D, null, null);
        assertEquals(List.of(), OverlayLineComposer.compose(true, false, false, false, snapshot));
        assertEquals(List.of("FPS: 120"), OverlayLineComposer.compose(false, false, true, false, snapshot));
        assertEquals(List.of("Average FPS: 90"), OverlayLineComposer.compose(false, false, false, true, snapshot));
    }
}
