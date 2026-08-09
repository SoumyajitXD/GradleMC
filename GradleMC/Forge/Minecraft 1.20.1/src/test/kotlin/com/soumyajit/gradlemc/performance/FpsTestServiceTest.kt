package com.soumyajit.gradlemc.performance

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class FpsTestServiceTest {
    @Test
    fun `fps test rejects an invalid duration`() {
        FpsTestService.resetClientSession()
        val result = FpsTestService.start(0)
        assertFalse(result.success)
    }

    @Test
    fun `fps test collects completed rendered frames rather than ticks`() {
        FpsTestService.resetClientSession()
        assertTrue(FpsTestService.start(5).success)
        FpsTestService.recordRenderedFrame(1_000_000_000L)
        FpsTestService.recordRenderedFrame(1_016_666_667L)
        val stopped = FpsTestService.stop()
        assertTrue(stopped.success)
        assertTrue(stopped.result!!.sampleCount == 1L)
        assertTrue(stopped.result!!.averageFps > 59.0)
    }

    @Test
    fun `frame discontinuity does not count pause time`() {
        FpsTestService.resetClientSession()
        assertTrue(FpsTestService.start(5).success)
        FpsTestService.recordRenderedFrame(0L)
        FpsTestService.recordRenderedFrame(20_000_000L)
        FpsTestService.markFrameDiscontinuity()
        FpsTestService.recordRenderedFrame(1_000_000_000L)
        FpsTestService.recordRenderedFrame(1_020_000_000L)

        val result = FpsTestService.stop().result!!
        assertEquals(2L, result.sampleCount)
        assertEquals(0.04, result.elapsedSeconds, 0.000_001)
        assertEquals(50.0, result.averageFps, 0.000_001)
    }

    @Test
    fun `test completes from accepted rendered duration`() {
        FpsTestService.resetClientSession()
        assertTrue(FpsTestService.start(1).success)
        FpsTestService.recordRenderedFrame(0L)
        FpsTestService.recordRenderedFrame(500_000_000L)
        FpsTestService.recordRenderedFrame(1_000_000_000L)

        val state = FpsTestService.state()
        assertFalse(state.isRunning)
        assertEquals(FpsTestResult.EndReason.COMPLETED, state.latestResult?.endReason)
        assertEquals(2L, state.latestResult?.sampleCount)
    }

    @Test
    fun `client session reset cancels work and clears stale result`() {
        FpsTestService.resetClientSession()
        assertTrue(FpsTestService.start(5).success)
        FpsTestService.recordRenderedFrame(0L)
        assertTrue(FpsTestService.stop().success)
        assertTrue(FpsTestService.state().latestResult != null)

        assertTrue(FpsTestService.resetClientSession())
        val state = FpsTestService.state()
        assertFalse(state.isRunning)
        assertNull(state.latestResult)
        assertFalse(FpsTestService.hasActiveTest())
    }

    @Test
    fun `explicit fps test remains active when optional overlay collection is disabled`() {
        PerformanceService.resetClientSession()
        PerformanceService.setOverlayFrameDemand(false)
        assertTrue(FpsTestService.start(5).success)

        PerformanceService.recordRenderedFrame(0L)
        PerformanceService.recordRenderedFrame(20_000_000L)

        val result = FpsTestService.stop().result!!
        assertEquals(1L, result.sampleCount)
        assertEquals(50.0, result.averageFps, 0.000_001)
        assertEquals(50.0, result.latestFps!!, 0.000_001)

        val shared = PerformanceService.snapshot()
        assertEquals(1L, shared.observedFrames)
        assertEquals(50.0, shared.currentFps!!, 0.000_001)
        assertEquals(50.0, shared.averageFps!!, 0.000_001)
        assertTrue(shared.message.contains("latest explicit rendered-frame test"))
    }
}
