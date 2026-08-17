package com.soumyajit.gradlemc.performance

import java.time.Instant
import java.util.Locale
import kotlin.math.roundToInt

data class FpsTestResult(val requestedSeconds: Int, val elapsedSeconds: Double, val sampleCount: Long, val averageFps: Double, val minFps: Int, val maxFps: Int, val latestFps: Double?, val startedAt: Instant, val endedAt: Instant, val endReason: EndReason) { enum class EndReason { COMPLETED, STOPPED } }
/**
 * A small, explicit state model for all UI and command consumers.  `WARMING_UP`
 * deliberately means that a test has started but has not observed a completed
 * frame interval yet; presenting that as 0 FPS would be misleading.
 */
enum class FpsTestLifecycle { IDLE, WARMING_UP, RUNNING, COMPLETED, STOPPED }
data class FpsTestState(
    val lifecycle: FpsTestLifecycle,
    val requestedSeconds: Int,
    val elapsedSeconds: Double,
    val latestResult: FpsTestResult?
) {
    val isRunning: Boolean get() = lifecycle == FpsTestLifecycle.WARMING_UP || lifecycle == FpsTestLifecycle.RUNNING
    val progress: Double get() = if (requestedSeconds <= 0) 0.0 else (elapsedSeconds / requestedSeconds).coerceIn(0.0, 1.0)
}
data class FpsTestActionResult(val success: Boolean, val message: String, val result: FpsTestResult? = null)

/** The one authoritative explicit FPS session. It consumes only completed render-frame timestamps. */
object FpsTestService {
    private var active: Session? = null; @Volatile private var running = false; @Volatile private var latest: FpsTestResult? = null
    @Synchronized fun hasActiveTest() = active != null
    @Synchronized fun start(seconds: Int): FpsTestActionResult {
        if (seconds !in 1..3600) return FpsTestActionResult(false, "FPS test duration must be between 1 and 3600 seconds.")
        if (active != null) return FpsTestActionResult(false, "An FPS test is already running. Use /gradlemc testfps stop first.")
        active = Session(seconds, Instant.now()); running = true; return FpsTestActionResult(true, "FPS test started for $seconds rendered seconds.")
    }
    @Synchronized fun stop(): FpsTestActionResult = active?.let { finish(it, FpsTestResult.EndReason.STOPPED) } ?: FpsTestActionResult(false, "No FPS test is currently running.")
    @Synchronized fun recordRenderedFrame(now: Long) { active?.let { it.add(now); if (it.elapsed >= it.requestedSeconds * 1_000_000_000L) finish(it, FpsTestResult.EndReason.COMPLETED) } }
    @Synchronized fun markFrameDiscontinuity() { active?.resetFrame() }
    @Synchronized fun resetClientSession(): Boolean { val had = active != null || latest != null; active = null; running = false; latest = null; return had }
    @Synchronized fun state(): FpsTestState {
        val s = active
        val lifecycle = when {
            s == null && latest == null -> FpsTestLifecycle.IDLE
            s == null -> when (latest!!.endReason) {
                FpsTestResult.EndReason.COMPLETED -> FpsTestLifecycle.COMPLETED
                FpsTestResult.EndReason.STOPPED -> FpsTestLifecycle.STOPPED
            }
            s.samples == 0L -> FpsTestLifecycle.WARMING_UP
            else -> FpsTestLifecycle.RUNNING
        }
        return FpsTestState(lifecycle, s?.requestedSeconds ?: 0, (s?.elapsed ?: 0) / 1e9, latest)
    }
    @Synchronized internal fun evidence(): FrameTimingStats { val s = active; return if (s != null) FrameTimingStats(s.latestFps(), s.averageFps(), s.samples) else latest?.let { FrameTimingStats(it.latestFps, it.averageFps, it.sampleCount) } ?: FrameTimingStats(null, null, 0) }
    private fun finish(s: Session, reason: FpsTestResult.EndReason): FpsTestActionResult { active = null; running = false; val elapsed = s.elapsed / 1e9; val result = FpsTestResult(s.requestedSeconds, elapsed, s.samples, if (elapsed == 0.0) 0.0 else s.samples / elapsed, if (s.samples == 0L) 0 else (1e9 / s.maxFrame).roundToInt(), if (s.samples == 0L) 0 else (1e9 / s.minFrame).roundToInt(), s.latestFps(), s.started, Instant.now(), reason); latest = result; return FpsTestActionResult(true, "FPS test ${reason.name.lowercase(Locale.ROOT)}: average ${"%.1f".format(Locale.ROOT, result.averageFps)} FPS from ${result.sampleCount} rendered frame intervals.", result) }
    private class Session(val requestedSeconds: Int, val started: Instant) { var previous = -1L; var elapsed = 0L; var samples = 0L; var minFrame = Long.MAX_VALUE; var maxFrame = 0L; var latestFrame = 0L
        fun add(now: Long) { val old = previous; previous = now; if (old < 0) return; val d = completedFrameIntervalNanos(old, now); if (d == INVALID_FRAME_INTERVAL_NANOS) { resetFrame(); previous = now; return }; elapsed += d; samples++; minFrame = minOf(minFrame,d); maxFrame = maxOf(maxFrame,d); latestFrame = d }
        fun resetFrame() { previous = -1L }; fun latestFps() = latestFrame.takeIf { it > 0 }?.let { 1e9 / it }; fun averageFps() = elapsed.takeIf { it > 0 }?.let { samples * 1e9 / it }
    }
}
