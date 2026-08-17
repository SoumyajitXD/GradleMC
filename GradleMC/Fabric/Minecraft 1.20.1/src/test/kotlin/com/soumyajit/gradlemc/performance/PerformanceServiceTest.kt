package com.soumyajit.gradlemc.performance

import kotlin.test.*

class PerformanceServiceTest {
    @Test fun `mode parsing and bounded rolling timing`() { assertEquals(PerformanceMode.BALANCED,PerformanceMode.parse("bad"));assertEquals(PerformanceMode.LOW_IMPACT,PerformanceMode.parse(" LOW_IMPACT "));val timing=RollingFrameTiming(FrameTimingPolicy(1,1_000_000_000,2));timing.record(0);timing.record(20_000_000);timing.record(40_000_000);assertEquals(2,timing.snapshot().observedFrames);assertEquals(50.0,timing.snapshot().averageFps);timing.record(2_000_000_000);assertEquals(2,timing.snapshot().observedFrames) }
    @Test fun `server timing measures execution not tick cadence`() { val t=ServerTickTiming(0);t.start(100_000_000);t.end(110_000_000);t.start(200_000_000);t.end(220_000_000);val s=t.snapshot();assertEquals(2,s.completedTicks);assertEquals(15.0,s.averageExecutionMillis) }
    @Test fun `performance sample state machine rejects invalid and idle stop`() { assertFalse(PerformanceService.startTimedSample(0).success);assertFalse(PerformanceService.stopTimedSample().success);assertTrue(PerformanceService.startTimedSample(1).success);assertFalse(PerformanceService.startTimedSample(1).success);assertTrue(PerformanceService.stopTimedSample().success) }
}
