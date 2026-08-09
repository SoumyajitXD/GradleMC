package com.soumyajit.gradlemc.performance

internal const val INVALID_FRAME_INTERVAL_NANOS = -1L
internal const val MAX_VALID_FRAME_INTERVAL_NANOS = 1_000_000_000L

/** Shared validation for intervals emitted by the single post-render timestamp producer. */
internal fun completedFrameIntervalNanos(previousNanos: Long, nowNanos: Long): Long {
    val duration = nowNanos - previousNanos
    return duration.takeIf { it in 1..MAX_VALID_FRAME_INTERVAL_NANOS }
        ?: INVALID_FRAME_INTERVAL_NANOS
}

internal data class FrameTimingPolicy(
    val samplingStride: Int,
    val windowNanos: Long,
    val maxBuckets: Int,
) {
    init {
        require(samplingStride > 0)
        require(windowNanos > 0L)
        require(maxBuckets > 0)
    }
}

internal data class FrameTimingStats(
    val currentFps: Double?,
    val averageFps: Double?,
    val observedFrames: Long,
)

internal enum class FrameRecordOutcome { BASELINE, ACCEPTED, DISCONTINUITY }

/**
 * Steady-state allocation-free frame producer backed by bounded primitive arrays.
 *
 * Each bucket represents [FrameTimingPolicy.samplingStride] adjacent frames. The rolling
 * average is duration-weighted (`frames / elapsed time`), never an average of rounded FPS
 * readings. Monotonic bucket age and bucket count both bound the retained history.
 */
internal class RollingFrameTiming(initialPolicy: FrameTimingPolicy) {
    private var durations = LongArray(0)
    private var frameCounts = IntArray(0)
    private var bucketEndNanos = LongArray(0)

    private var policy = initialPolicy.validated()
    private var head = 0
    private var size = 0
    private var totalDurationNanos = 0L
    private var totalFrames = 0L
    private var previousFrameNanos = UNSET
    private var pendingDurationNanos = 0L
    private var pendingFrames = 0
    private var pendingEndNanos = UNSET
    private var currentFps = Double.NaN

    fun setPolicy(value: FrameTimingPolicy) {
        val validated = value.validated()
        if (validated == policy) return
        policy = validated
        reset()
        durations = LongArray(0)
        frameCounts = IntArray(0)
        bucketEndNanos = LongArray(0)
    }

    fun record(nowNanos: Long): FrameRecordOutcome {
        val previous = previousFrameNanos
        previousFrameNanos = nowNanos
        if (previous == UNSET) return FrameRecordOutcome.BASELINE

        val duration = completedFrameIntervalNanos(previous, nowNanos)
        if (duration == INVALID_FRAME_INTERVAL_NANOS) {
            pendingDurationNanos = 0L
            pendingFrames = 0
            currentFps = Double.NaN
            return FrameRecordOutcome.DISCONTINUITY
        }

        pendingDurationNanos += duration
        pendingFrames++
        pendingEndNanos = nowNanos
        if (pendingFrames >= policy.samplingStride) flushPending()
        return FrameRecordOutcome.ACCEPTED
    }

    /** Breaks timestamp continuity without carrying a pause into the next rendered interval. */
    fun markDiscontinuity() {
        previousFrameNanos = UNSET
        pendingDurationNanos = 0L
        pendingFrames = 0
        pendingEndNanos = UNSET
        currentFps = Double.NaN
    }

    /** Clears all client-session state, including the rolling average. */
    fun reset() {
        head = 0
        size = 0
        totalDurationNanos = 0L
        totalFrames = 0L
        markDiscontinuity()
    }

    fun snapshot(): FrameTimingStats {
        val average = if (totalFrames == 0L || totalDurationNanos <= 0L) {
            null
        } else {
            totalFrames * NANOS_PER_SECOND / totalDurationNanos
        }
        return FrameTimingStats(currentFps.takeIf(Double::isFinite), average, totalFrames)
    }

    private fun flushPending() {
        ensureStorage()
        while (size >= policy.maxBuckets) removeOldest()

        val index = (head + size) % durations.size
        durations[index] = pendingDurationNanos
        frameCounts[index] = pendingFrames
        bucketEndNanos[index] = pendingEndNanos
        size++
        totalDurationNanos += pendingDurationNanos
        totalFrames += pendingFrames
        currentFps = pendingFrames * NANOS_PER_SECOND / pendingDurationNanos
        pendingDurationNanos = 0L
        pendingFrames = 0
        val newestEndNanos = pendingEndNanos
        pendingEndNanos = UNSET

        val oldestAllowed = newestEndNanos - policy.windowNanos
        while (size > 1 && bucketEndNanos[head] < oldestAllowed) removeOldest()
    }

    private fun removeOldest() {
        if (size == 0) return
        totalDurationNanos -= durations[head]
        totalFrames -= frameCounts[head].toLong()
        durations[head] = 0L
        frameCounts[head] = 0
        bucketEndNanos[head] = 0L
        head = (head + 1) % durations.size
        size--
    }

    /** Allocated only when a consumer has accepted enough frames to produce a bucket. */
    private fun ensureStorage() {
        if (durations.size == policy.maxBuckets) return
        check(size == 0) { "frame timing storage cannot be resized while samples are retained" }
        durations = LongArray(policy.maxBuckets)
        frameCounts = IntArray(policy.maxBuckets)
        bucketEndNanos = LongArray(policy.maxBuckets)
    }

    private fun FrameTimingPolicy.validated(): FrameTimingPolicy {
        require(maxBuckets <= MAX_BUCKET_CAPACITY) {
            "maxBuckets must not exceed the fixed capacity of $MAX_BUCKET_CAPACITY"
        }
        return this
    }

    private companion object {
        // Matches the authoritative donor's bounded 2,400-samples/second contract at 120 seconds.
        const val MAX_BUCKET_CAPACITY = 288_000
        const val NANOS_PER_SECOND = 1_000_000_000.0
        const val UNSET = -1L
    }
}
