package com.soumyajit.gradlemc.performance

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class PerformanceModeTest {
    @Test
    fun `balanced is the safe default for missing or invalid configuration`() {
        assertEquals(PerformanceMode.BALANCED, PerformanceMode.parse(null))
        assertEquals(PerformanceMode.BALANCED, PerformanceMode.parse("not-a-mode"))
        assertEquals(PerformanceMode.LOW_IMPACT, PerformanceMode.parse(" LOW_IMPACT "))
    }

    @Test
    fun `performance modes have materially different bounded sampling policies`() {
        assertEquals(4, PerformanceMode.LOW_IMPACT.samplingStride)
        assertEquals(2, PerformanceMode.BALANCED.samplingStride)
        assertEquals(1, PerformanceMode.DETAILED.samplingStride)
        val policies = PerformanceMode.entries.map { it.timingPolicy(60) }
        assertTrue(policies.all { it.windowNanos == 60_000_000_000L })
        assertEquals(listOf(36_000, 72_000, 144_000), policies.map(FrameTimingPolicy::maxBuckets))
    }

    @Test
    fun `no frame samples means no fabricated fps values`() {
        PerformanceService.resetClientSession()
        val snapshot = PerformanceService.snapshot()

        assertEquals(0L, snapshot.observedFrames)
        assertNull(snapshot.currentFps)
        assertNull(snapshot.averageFps)
    }

    @Test
    fun `fps test validates duration and can stop honestly`() {
        FpsTestService.resetClientSession()
        val invalid = FpsTestService.start(0)
        assertFalse(invalid.success)

        assertTrue(FpsTestService.start(5).success)
        val stopped = FpsTestService.stop()
        assertTrue(stopped.success)
        assertEquals(FpsTestResult.EndReason.STOPPED, stopped.result?.endReason)
        assertEquals(0L, stopped.result?.sampleCount)
    }

    @Test
    fun `disabled overlay demand does not collect rolling samples`() {
        PerformanceService.resetClientSession()
        PerformanceService.setOverlayFrameDemand(false)

        PerformanceService.recordRenderedFrame(0L)
        PerformanceService.recordRenderedFrame(16_666_667L)

        assertEquals(0L, PerformanceService.snapshot().observedFrames)
        assertFalse(PerformanceService.hasOverlayFrameDemand())
    }
}
