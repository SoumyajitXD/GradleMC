package com.soumyajit.gradlemc.performance

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ServerTickTimingTest {
    @Test
    fun `tick timing measures start to end execution rather than end cadence`() {
        val timing = ServerTickTiming(startedNanos = 0L)
        timing.onStart(50_000_000L)
        timing.onEnd(52_000_000L)
        timing.onStart(100_000_000L)
        timing.onEnd(105_000_000L)

        val snapshot = timing.snapshot()
        assertEquals(2L, snapshot.completedTicks)
        assertEquals(7_000_000L, snapshot.totalExecutionNanos)
        assertEquals(3.5, snapshot.averageExecutionMillis!!, 0.000_001)
        assertEquals(105_000_000L, snapshot.elapsedNanos)
    }

    @Test
    fun `end without start does not fabricate a tick`() {
        val timing = ServerTickTiming(startedNanos = 0L)
        timing.onEnd(50_000_000L)

        val snapshot = timing.snapshot()
        assertEquals(0L, snapshot.completedTicks)
        assertNull(snapshot.averageExecutionMillis)
    }
}
