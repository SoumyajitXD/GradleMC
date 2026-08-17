package com.soumyajit.gradlemc.performance

internal const val INVALID_FRAME_INTERVAL_NANOS = -1L
internal fun completedFrameIntervalNanos(previous: Long, now: Long): Long =
    (now - previous).takeIf { it in 1..1_000_000_000L } ?: INVALID_FRAME_INTERVAL_NANOS

internal data class FrameTimingPolicy(val samplingStride: Int, val windowNanos: Long, val maxBuckets: Int) {
    init { require(samplingStride > 0 && windowNanos > 0 && maxBuckets in 1..288_000) }
}
internal data class FrameTimingStats(val currentFps: Double?, val averageFps: Double?, val observedFrames: Long)
internal enum class FrameRecordOutcome { BASELINE, ACCEPTED, DISCONTINUITY }

/** Allocation-free bounded rolling history of completed rendered frame intervals. */
internal class RollingFrameTiming(private var policy: FrameTimingPolicy) {
    private var durations = LongArray(0); private var frames = IntArray(0); private var ends = LongArray(0)
    private var head = 0; private var size = 0; private var totalNanos = 0L; private var totalFrames = 0L
    private var previous = -1L; private var pendingNanos = 0L; private var pendingFrames = 0; private var pendingEnd = -1L
    private var current = Double.NaN
    fun record(now: Long): FrameRecordOutcome {
        val old = previous; previous = now; if (old < 0) return FrameRecordOutcome.BASELINE
        val duration = completedFrameIntervalNanos(old, now)
        if (duration == INVALID_FRAME_INTERVAL_NANOS) { discontinuity(); previous = now; return FrameRecordOutcome.DISCONTINUITY }
        pendingNanos += duration; pendingFrames++; pendingEnd = now
        if (pendingFrames >= policy.samplingStride) flush()
        return FrameRecordOutcome.ACCEPTED
    }
    fun discontinuity() { previous = -1; pendingNanos = 0; pendingFrames = 0; pendingEnd = -1; current = Double.NaN }
    fun reset() { head = 0; size = 0; totalNanos = 0; totalFrames = 0; discontinuity() }
    fun setPolicy(value: FrameTimingPolicy) { if (value != policy) { policy = value; durations = LongArray(0); frames = IntArray(0); ends = LongArray(0); reset() } }
    fun snapshot() = FrameTimingStats(current.takeIf(Double::isFinite), if (totalFrames == 0L) null else totalFrames * 1_000_000_000.0 / totalNanos, totalFrames)
    private fun flush() {
        if (durations.isEmpty()) { durations = LongArray(policy.maxBuckets); frames = IntArray(policy.maxBuckets); ends = LongArray(policy.maxBuckets) }
        if (size == policy.maxBuckets) remove(); val i = (head + size) % durations.size
        durations[i] = pendingNanos; frames[i] = pendingFrames; ends[i] = pendingEnd; size++; totalNanos += pendingNanos; totalFrames += pendingFrames
        current = pendingFrames * 1_000_000_000.0 / pendingNanos; pendingNanos = 0; pendingFrames = 0
        val oldest = ends[i] - policy.windowNanos; while (size > 1 && ends[head] < oldest) remove()
    }
    private fun remove() { totalNanos -= durations[head]; totalFrames -= frames[head]; head = (head + 1) % durations.size; size-- }
}
