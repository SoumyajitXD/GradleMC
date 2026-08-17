package com.soumyajit.gradlemc.performance

import kotlin.test.*

class FpsTestServiceTest {
    @BeforeTest fun reset() { FpsTestService.resetClientSession() }
    @Test fun `rejects invalid durations and concurrent tests`() { assertFalse(FpsTestService.start(0).success); assertFalse(FpsTestService.start(3601).success); assertTrue(FpsTestService.start(5).success); assertFalse(FpsTestService.start(5).success) }
    @Test fun `computes rendered frame results and retains completed state`() { FpsTestService.start(1); FpsTestService.recordRenderedFrame(0); FpsTestService.recordRenderedFrame(500_000_000); FpsTestService.recordRenderedFrame(1_000_000_000); val result=FpsTestService.state().latestResult!!; assertEquals(2,result.sampleCount);assertEquals(2.0,result.averageFps);assertEquals(2,result.minFps);assertEquals(2,result.maxFps);assertEquals(FpsTestResult.EndReason.COMPLETED,result.endReason) }
    @Test fun `manual stop and disconnect reset are honest`() { FpsTestService.start(5);FpsTestService.recordRenderedFrame(0);val stopped=FpsTestService.stop();assertTrue(stopped.success);assertEquals(FpsTestResult.EndReason.STOPPED,stopped.result!!.endReason);assertTrue(FpsTestService.resetClientSession());assertNull(FpsTestService.state().latestResult);assertFalse(FpsTestService.state().isRunning) }
    @Test fun `lifecycle separates warming up active and completed states`() {
        assertEquals(FpsTestLifecycle.IDLE, FpsTestService.state().lifecycle)
        assertTrue(FpsTestService.start(1).success)
        assertEquals(FpsTestLifecycle.WARMING_UP, FpsTestService.state().lifecycle)
        FpsTestService.recordRenderedFrame(0)
        FpsTestService.recordRenderedFrame(500_000_000)
        assertEquals(FpsTestLifecycle.RUNNING, FpsTestService.state().lifecycle)
        assertEquals(0.5, FpsTestService.state().progress)
        FpsTestService.recordRenderedFrame(1_000_000_000)
        assertEquals(FpsTestLifecycle.COMPLETED, FpsTestService.state().lifecycle)
    }
    @Test fun `discontinuity does not create a fake low fps sample`() { FpsTestService.start(5);FpsTestService.recordRenderedFrame(0);FpsTestService.recordRenderedFrame(20_000_000);FpsTestService.markFrameDiscontinuity();FpsTestService.recordRenderedFrame(2_000_000_000);FpsTestService.recordRenderedFrame(2_020_000_000);val r=FpsTestService.stop().result!!;assertEquals(2,r.sampleCount);assertEquals(50.0,r.averageFps) }
    @Test fun `explicit test progresses while passive overlay sampling is off`() { PerformanceService.resetClientSession();PerformanceService.setOverlayFrameDemand(false);assertTrue(FpsTestService.start(5).success);PerformanceService.recordRenderedFrame(0);PerformanceService.recordRenderedFrame(20_000_000);val r=FpsTestService.stop().result!!;assertEquals(1,r.sampleCount);assertEquals(50.0,r.averageFps);assertEquals(1,PerformanceService.snapshot().observedFrames) }
    @Test fun `reversed timestamps are discarded without invalid metrics`() { FpsTestService.start(5);FpsTestService.recordRenderedFrame(100);FpsTestService.recordRenderedFrame(50);FpsTestService.recordRenderedFrame(70);val r=FpsTestService.stop().result!!;assertEquals(1,r.sampleCount);assertTrue(r.averageFps.isFinite());assertTrue(r.averageFps >= 0);assertTrue(r.minFps >= 0);assertTrue(r.maxFps >= 0) }
}
