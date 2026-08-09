package com.soumyajit.gradlemc.performance

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class RollingFrameTimingTest {
    @Test
    fun `average is weighted by represented frame duration`() {
        val timing = RollingFrameTiming(FrameTimingPolicy(1, 1_000_000_000L, 8))
        timing.record(0L)
        timing.record(10_000_000L)
        timing.record(30_000_000L)

        val snapshot = timing.snapshot()
        assertEquals(2L, snapshot.observedFrames)
        assertEquals(50.0, snapshot.currentFps!!, 0.000_001)
        assertEquals(66.666_666, snapshot.averageFps!!, 0.000_001)
    }

    @Test
    fun `window evicts buckets by monotonic age`() {
        val timing = RollingFrameTiming(FrameTimingPolicy(1, 25_000_000L, 4))
        timing.record(0L)
        timing.record(10_000_000L)
        timing.record(20_000_000L)
        timing.record(30_000_000L)

        val snapshot = timing.snapshot()
        assertEquals(3L, snapshot.observedFrames)
        assertEquals(100.0, snapshot.averageFps!!, 0.000_001)
    }

    @Test
    fun `sampling stride aggregates frames without averaging rounded fps`() {
        val timing = RollingFrameTiming(FrameTimingPolicy(4, 1_000_000_000L, 8))
        timing.record(0L)
        timing.record(10_000_000L)
        timing.record(20_000_000L)
        timing.record(30_000_000L)
        assertEquals(0L, timing.snapshot().observedFrames)

        timing.record(40_000_000L)
        val snapshot = timing.snapshot()
        assertEquals(4L, snapshot.observedFrames)
        assertEquals(100.0, snapshot.currentFps!!, 0.000_001)
        assertEquals(100.0, snapshot.averageFps!!, 0.000_001)
    }

    @Test
    fun `discontinuity never bridges timestamps and full reset clears history`() {
        val timing = RollingFrameTiming(FrameTimingPolicy(1, 1_000_000_000L, 8))
        timing.record(0L)
        timing.record(10_000_000L)
        timing.markDiscontinuity()
        timing.record(1_000_000_000L)
        timing.record(1_020_000_000L)

        assertEquals(1L, timing.snapshot().observedFrames)
        assertEquals(50.0, timing.snapshot().currentFps!!, 0.000_001)

        timing.reset()
        val reset = timing.snapshot()
        assertEquals(0L, reset.observedFrames)
        assertNull(reset.currentFps)
        assertNull(reset.averageFps)
    }

    @Test
    fun `non monotonic timestamp becomes a discontinuity`() {
        val timing = RollingFrameTiming(FrameTimingPolicy(1, 1_000_000_000L, 8))
        assertEquals(FrameRecordOutcome.BASELINE, timing.record(100L))
        assertEquals(FrameRecordOutcome.DISCONTINUITY, timing.record(90L))
        assertNull(timing.snapshot().currentFps)
    }

    @Test
    fun `bucket capacity bounds retained storage independently of elapsed window`() {
        val timing = RollingFrameTiming(FrameTimingPolicy(1, 1_000_000_000L, 3))
        timing.record(0L)
        repeat(5) { index -> timing.record((index + 1) * 10_000_000L) }

        val snapshot = timing.snapshot()
        assertEquals(3L, snapshot.observedFrames)
        assertEquals(100.0, snapshot.averageFps!!, 0.000_001)
    }

    @Test
    fun `intervals longer than donor limit break continuity instead of becoming low fps`() {
        val timing = RollingFrameTiming(FrameTimingPolicy(1, 120_000_000_000L, 8))
        timing.record(0L)
        assertEquals(FrameRecordOutcome.DISCONTINUITY, timing.record(1_000_000_001L))
        timing.record(1_010_000_001L)

        assertEquals(1L, timing.snapshot().observedFrames)
        assertEquals(100.0, timing.snapshot().averageFps!!, 0.000_001)
    }
}
