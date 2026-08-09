package com.soumyajit.gradlemc.performance

internal data class ServerTickTimingStats(
    val elapsedNanos: Long,
    val completedTicks: Long,
    val totalExecutionNanos: Long,
) {
    val averageExecutionMillis: Double?
        get() = if (completedTicks == 0L) null else totalExecutionNanos / completedTicks / 1_000_000.0
}

/** Measures tick execution from START to END; it does not confuse the 50 ms tick cadence with MSPT. */
internal class ServerTickTiming(private val startedNanos: Long) {
    private var tickStartedNanos = UNSET
    private var elapsedNanos = 0L
    private var completedTicks = 0L
    private var totalExecutionNanos = 0L

    val elapsed: Long get() = elapsedNanos

    fun onStart(nowNanos: Long) {
        updateElapsed(nowNanos)
        tickStartedNanos = nowNanos
    }

    fun onEnd(nowNanos: Long) {
        updateElapsed(nowNanos)
        val started = tickStartedNanos
        tickStartedNanos = UNSET
        if (started == UNSET) return
        val duration = nowNanos - started
        if (duration !in 1..MAX_VALID_TICK_EXECUTION_NANOS) return
        completedTicks++
        totalExecutionNanos += duration
    }

    fun snapshot(): ServerTickTimingStats = ServerTickTimingStats(elapsedNanos, completedTicks, totalExecutionNanos)

    private fun updateElapsed(nowNanos: Long) {
        elapsedNanos = (nowNanos - startedNanos).coerceAtLeast(0L)
    }

    private companion object {
        const val UNSET = -1L
        const val MAX_VALID_TICK_EXECUTION_NANOS = 60_000_000_000L
    }
}
