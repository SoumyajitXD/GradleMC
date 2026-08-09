package com.soumyajit.gradlemc.performance

import java.time.Instant
import java.util.Locale
import kotlin.math.roundToInt

data class FpsTestResult(
    val requestedSeconds: Int,
    val elapsedSeconds: Double,
    val sampleCount: Long,
    val averageFps: Double,
    val minFps: Int,
    val maxFps: Int,
    val latestFps: Double?,
    val startedAt: Instant,
    val endedAt: Instant,
    val endReason: EndReason,
) {
    enum class EndReason { COMPLETED, STOPPED }
}

data class FpsTestState(
    val isRunning: Boolean,
    val requestedSeconds: Int,
    val elapsedSeconds: Double,
    val latestResult: FpsTestResult?,
)

data class FpsTestActionResult(
    val success: Boolean,
    val message: String,
    val result: FpsTestResult? = null,
)

internal data class FpsPerformanceEvidence(
    val currentFps: Double?,
    val averageFps: Double?,
    val sampleCount: Long,
    val running: Boolean,
)

/** Explicit, bounded FPS test using the physical client's completed-frame producer. */
object FpsTestService {
    private var session: Session? = null
    @Volatile private var running = false
    @Volatile private var latest: FpsTestResult? = null

    /** Lock-free fast-path check so inactive tests add no second monitor to every frame. */
    fun hasActiveTest(): Boolean = running

    @Synchronized
    fun start(seconds: Int): FpsTestActionResult {
        if (seconds !in 1..3600) {
            return FpsTestActionResult(false, "FPS test duration must be between 1 and 3600 seconds.")
        }
        if (session != null) {
            return FpsTestActionResult(false, "An FPS test is already running. Use /gradlemc testfps stop first.")
        }
        session = Session(seconds, Instant.now())
        running = true
        return FpsTestActionResult(true, "FPS test started for $seconds rendered seconds.")
    }

    @Synchronized
    fun stop(): FpsTestActionResult {
        val active = session ?: return FpsTestActionResult(false, "No FPS test is currently running.")
        return complete(active, FpsTestResult.EndReason.STOPPED)
    }

    @Synchronized
    fun recordRenderedFrame(nowNanos: Long) {
        val active = session ?: return
        active.addFrame(nowNanos)
        if (active.elapsedNanos >= active.requestedSeconds * 1_000_000_000L) {
            complete(active, FpsTestResult.EndReason.COMPLETED)
        }
    }

    /** Prevents a pause, inactive window, or transient world gap from becoming a frame interval. */
    @Synchronized
    fun markFrameDiscontinuity() {
        session?.markDiscontinuity()
    }

    /** Cancels active work and removes results owned by the previous client world/session. */
    @Synchronized
    fun resetClientSession(): Boolean {
        val hadState = session != null || latest != null
        session = null
        running = false
        latest = null
        return hadState
    }

    @Synchronized
    fun state(): FpsTestState {
        val active = session
        return FpsTestState(
            isRunning = active != null,
            requestedSeconds = active?.requestedSeconds ?: 0,
            elapsedSeconds = (active?.elapsedNanos ?: 0L) / 1_000_000_000.0,
            latestResult = latest,
        )
    }

    /** Bounded aggregate evidence for GUI/report snapshots when passive rolling collection is off. */
    @Synchronized
    internal fun performanceEvidence(): FpsPerformanceEvidence {
        val active = session
        if (active != null) {
            return FpsPerformanceEvidence(
                currentFps = active.latestFps(),
                averageFps = active.averageFps(),
                sampleCount = active.samples,
                running = true,
            )
        }
        val completed = latest
        return FpsPerformanceEvidence(
            currentFps = completed?.latestFps,
            averageFps = completed?.averageFps?.takeIf { completed.sampleCount > 0L },
            sampleCount = completed?.sampleCount ?: 0L,
            running = false,
        )
    }

    private fun complete(active: Session, reason: FpsTestResult.EndReason): FpsTestActionResult {
        session = null
        running = false
        val elapsed = active.elapsedNanos / 1_000_000_000.0
        val result = FpsTestResult(
            requestedSeconds = active.requestedSeconds,
            elapsedSeconds = elapsed,
            sampleCount = active.samples,
            averageFps = if (elapsed == 0.0) 0.0 else active.samples / elapsed,
            minFps = if (active.samples == 0L) 0 else (1_000_000_000.0 / active.maxFrameNanos).roundToInt(),
            maxFps = if (active.samples == 0L) 0 else (1_000_000_000.0 / active.minFrameNanos).roundToInt(),
            latestFps = active.latestFps(),
            startedAt = active.startedAt,
            endedAt = Instant.now(),
            endReason = reason,
        )
        latest = result
        return FpsTestActionResult(
            true,
            "FPS test ${reason.name.lowercase(Locale.ROOT)}: average ${"%.1f".format(Locale.ROOT, result.averageFps)} FPS " +
                "from ${result.sampleCount} rendered frame intervals.",
            result,
        )
    }

    private class Session(val requestedSeconds: Int, val startedAt: Instant) {
        private var previousNanos = UNSET
        var elapsedNanos = 0L
            private set
        var samples = 0L
            private set
        var minFrameNanos = Long.MAX_VALUE
            private set
        var maxFrameNanos = 0L
            private set
        var latestFrameNanos = 0L
            private set

        fun addFrame(nowNanos: Long) {
            val previous = previousNanos
            previousNanos = nowNanos
            if (previous == UNSET) return
            val frameNanos = completedFrameIntervalNanos(previous, nowNanos)
            if (frameNanos == INVALID_FRAME_INTERVAL_NANOS) {
                markDiscontinuity()
                previousNanos = nowNanos
                return
            }
            elapsedNanos += frameNanos
            samples++
            minFrameNanos = minOf(minFrameNanos, frameNanos)
            maxFrameNanos = maxOf(maxFrameNanos, frameNanos)
            latestFrameNanos = frameNanos
        }

        fun latestFps(): Double? = latestFrameNanos.takeIf { it > 0L }
            ?.let { 1_000_000_000.0 / it }

        fun averageFps(): Double? = elapsedNanos.takeIf { it > 0L }
            ?.let { samples * 1_000_000_000.0 / it }

        fun markDiscontinuity() {
            previousNanos = UNSET
        }

        private companion object { const val UNSET = -1L }
    }
}
