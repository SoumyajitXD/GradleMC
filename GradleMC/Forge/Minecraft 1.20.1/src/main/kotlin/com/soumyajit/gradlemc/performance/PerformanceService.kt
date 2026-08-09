package com.soumyajit.gradlemc.performance

import com.soumyajit.gradlemc.config.ForgeGradleMCConfig
import com.soumyajit.gradlemc.config.GradleMCDefaults
import net.minecraftforge.event.TickEvent
import net.minecraftforge.event.server.ServerStoppedEvent
import java.util.Locale
import java.util.concurrent.atomic.AtomicReference
import kotlin.math.roundToInt

/**
 * The modes use different fixed-cost frame sampling policies. They affect GradleMC collection
 * only and never alter Minecraft, rendering, or another mod.
 */
enum class PerformanceMode(
    val configValue: String,
    val label: String,
    val samplingStride: Int,
) {
    LOW_IMPACT("low_impact", "Low Impact", samplingStride = 4),
    BALANCED("balanced", "Balanced", samplingStride = 2),
    DETAILED("detailed", "Detailed", samplingStride = 1);

    internal fun timingPolicy(windowSeconds: Int): FrameTimingPolicy {
        val intervals = windowSeconds * MAX_FRAME_INTERVALS_PER_SECOND
        return FrameTimingPolicy(
            samplingStride = samplingStride,
            windowNanos = windowSeconds * NANOS_PER_SECOND,
            maxBuckets = (intervals + samplingStride - 1) / samplingStride,
        )
    }

    companion object {
        fun parse(value: String?): PerformanceMode = entries.firstOrNull {
            it.configValue == value?.trim()?.lowercase(Locale.ROOT)
        } ?: BALANCED

        private const val MAX_FRAME_INTERVALS_PER_SECOND = 2_400
        private const val NANOS_PER_SECOND = 1_000_000_000L
    }
}

data class PerformanceSnapshot(
    val mode: PerformanceMode,
    val currentFps: Double?,
    val averageFps: Double?,
    val observedFrames: Long,
    val message: String,
    val averagingWindowSeconds: Int,
    val samplingStride: Int,
)

data class PerformanceModeChangeResult(val success: Boolean, val message: String)

/** Common-safe owner for bounded client frame timing and explicit server tick execution timing. */
object PerformanceService {
    private val mode = AtomicReference(PerformanceMode.BALANCED)
    private val frameTiming = RollingFrameTiming(
        PerformanceMode.BALANCED.timingPolicy(GradleMCDefaults.OVERLAY_SAMPLING_WINDOW_SECONDS),
    )
    @Volatile private var overlayFrameDemand = false
    private var averagingWindowSeconds = GradleMCDefaults.OVERLAY_SAMPLING_WINDOW_SECONDS
    private var timedSample: TimedSample? = null

    @Synchronized
    fun setMode(value: PerformanceMode): PerformanceModeChangeResult {
        val previous = mode.get()
        return try {
            ForgeGradleMCConfig.performanceMode.set(value.configValue)
            ForgeGradleMCConfig.spec.save()
            applyFramePolicy(value, configuredWindowSeconds())
            PerformanceModeChangeResult(true, "Performance mode set to ${value.label}.")
        } catch (failure: RuntimeException) {
            ForgeGradleMCConfig.performanceMode.set(previous.configValue)
            applyFramePolicy(previous, configuredWindowSeconds())
            PerformanceModeChangeResult(false, "Unable to persist performance mode: ${failure.message ?: failure.javaClass.simpleName}")
        }
    }

    @Synchronized
    fun mode(): PerformanceMode {
        val configured = runCatching { ForgeGradleMCConfig.snapshot() }.getOrNull()
        if (configured != null) {
            applyFramePolicy(
                PerformanceMode.parse(configured.performanceMode),
                configured.overlaySamplingWindowSeconds,
            )
        }
        return mode.get()
    }

    /** Enables rolling collection only while an enabled overlay component consumes FPS. */
    fun setOverlayFrameDemand(enabled: Boolean) {
        if (overlayFrameDemand == enabled) return
        synchronized(this) {
            if (overlayFrameDemand == enabled) return
            overlayFrameDemand = enabled
            frameTiming.markDiscontinuity()
        }
    }

    internal fun hasOverlayFrameDemand(): Boolean = overlayFrameDemand

    /** Call once after a completed rendered frame on the physical client only. */
    fun recordRenderedFrame(nowNanos: Long = System.nanoTime()) {
        val testActive = FpsTestService.hasActiveTest()
        if (!overlayFrameDemand && !testActive) return
        synchronized(this) {
            val collectRolling = overlayFrameDemand
            if (!collectRolling) {
                if (FpsTestService.hasActiveTest()) FpsTestService.recordRenderedFrame(nowNanos)
                return
            }
            when (frameTiming.record(nowNanos)) {
                FrameRecordOutcome.DISCONTINUITY -> {
                    FpsTestService.markFrameDiscontinuity()
                    if (FpsTestService.hasActiveTest()) FpsTestService.recordRenderedFrame(nowNanos)
                }
                FrameRecordOutcome.BASELINE,
                FrameRecordOutcome.ACCEPTED -> if (FpsTestService.hasActiveTest()) {
                    FpsTestService.recordRenderedFrame(nowNanos)
                }
            }
        }
    }

    private fun configuredWindowSeconds(): Int = runCatching {
        ForgeGradleMCConfig.snapshot().overlaySamplingWindowSeconds
    }.getOrDefault(GradleMCDefaults.OVERLAY_SAMPLING_WINDOW_SECONDS)

    @Synchronized
    internal fun applyFramePolicy(mode: PerformanceMode, windowSeconds: Int) {
        val normalizedWindow = com.soumyajit.gradlemc.config.GradleMCConfigSnapshot
            .normalizedOverlaySamplingWindowSeconds(windowSeconds)
        this.mode.set(mode)
        averagingWindowSeconds = normalizedWindow
        frameTiming.setPolicy(mode.timingPolicy(normalizedWindow))
    }

    /**
     * Breaks continuity for pauses, inactive windows, and temporary missing world state.
     * The rolling history remains available, but no interval can bridge the discontinuity.
     */
    fun resetFrameTiming() {
        if (!overlayFrameDemand && !FpsTestService.hasActiveTest()) return
        synchronized(this) {
            if (overlayFrameDemand) frameTiming.markDiscontinuity()
            FpsTestService.markFrameDiscontinuity()
        }
    }

    /** Clears all client-owned samples and cancels an active FPS test for logout/reconnect. */
    @Synchronized
    fun resetClientSession() {
        frameTiming.reset()
        FpsTestService.resetClientSession()
    }

    @Synchronized
    fun snapshot(): PerformanceSnapshot {
        val selectedMode = mode()
        val timing = frameTiming.snapshot()
        val testEvidence = FpsTestService.performanceEvidence()
        val hasRollingEvidence = timing.observedFrames > 0L
        val observedFrames = if (hasRollingEvidence) timing.observedFrames else testEvidence.sampleCount
        return PerformanceSnapshot(
            mode = selectedMode,
            currentFps = if (hasRollingEvidence) timing.currentFps else testEvidence.currentFps,
            averageFps = if (hasRollingEvidence) timing.averageFps else testEvidence.averageFps,
            observedFrames = observedFrames,
            message = when {
                hasRollingEvidence -> "Rendered-frame timing uses a ${averagingWindowSeconds}s rolling window."
                testEvidence.sampleCount > 0L && testEvidence.running ->
                    "FPS values use the active explicit rendered-frame test; passive rolling sampling is inactive."
                testEvidence.sampleCount > 0L ->
                    "FPS values use the latest explicit rendered-frame test; passive rolling sampling is inactive."
                else -> "No rendered-frame samples have been collected yet."
            },
            averagingWindowSeconds = averagingWindowSeconds,
            samplingStride = selectedMode.samplingStride,
        )
    }

    fun overheadDescription(): String =
        "GradleMC uses bounded primitive frame buffers; mode controls bucket detail while the FPS window is configured separately."

    fun guardDescription(): String =
        "Performance modes change only GradleMC bucket detail: Low Impact groups 4 frame intervals, Balanced 2, Detailed 1."

    fun explainDescription(): String =
        "FPS uses completed render-frame intervals and a bounded duration-weighted rolling window, never game ticks."

    fun selfTest(): Boolean = (1_000_000_000.0 / 16_666_667L).roundToInt() == 60

    /** Starts a conventional START-to-END server tick execution sample. */
    @Synchronized
    fun startTimedSample(seconds: Int): FpsTestActionResult {
        if (seconds !in 1..3600) {
            return FpsTestActionResult(false, "Performance sample duration must be between 1 and 3600 seconds.")
        }
        if (timedSample != null) {
            return FpsTestActionResult(false, "A performance sample is already running. Use /gradlemc perf stop first.")
        }
        timedSample = TimedSample(seconds, System.nanoTime())
        return FpsTestActionResult(true, "Server tick execution sample started for $seconds seconds.")
    }

    @Synchronized
    fun stopTimedSample(): FpsTestActionResult {
        val active = timedSample
            ?: return FpsTestActionResult(false, "No performance sample is currently running.")
        timedSample = null
        return FpsTestActionResult(true, active.summary("stopped"))
    }

    /** Existing Forge event target; both phases are required for execution-time MSPT. */
    @Synchronized
    fun onServerTick(event: TickEvent.ServerTickEvent) {
        val nowNanos = System.nanoTime()
        when (event.phase) {
            TickEvent.Phase.START -> recordServerTickStart(nowNanos)
            TickEvent.Phase.END -> recordServerTickEnd(nowNanos)
        }
    }

    /** Narrow timestamped API used by lifecycle integration and pure timing verification. */
    @Synchronized
    fun recordServerTickStart(nowNanos: Long = System.nanoTime()) {
        timedSample?.timing?.onStart(nowNanos)
    }

    /** Narrow timestamped API used by lifecycle integration and pure timing verification. */
    @Synchronized
    fun recordServerTickEnd(nowNanos: Long = System.nanoTime()) {
        val active = timedSample ?: return
        active.timing.onEnd(nowNanos)
        if (active.timing.elapsed >= active.requestedNanos) {
            timedSample = null
            GradleMCLog.message(active.summary("completed"))
        }
    }

    /** Cancels any sample owned by a stopping server. */
    @Synchronized
    fun resetServerTiming(): Boolean {
        val wasRunning = timedSample != null
        timedSample = null
        return wasRunning
    }

    fun onServerStopped(event: ServerStoppedEvent) {
        if (resetServerTiming()) {
            GradleMCLog.message("Cancelled GradleMC server performance sample during server shutdown")
        }
    }

    @Synchronized
    fun timedSampleState(): String = timedSample?.summary("running")
        ?: "No performance sample is currently running."

    private class TimedSample(val requestedSeconds: Int, startedNanos: Long) {
        val timing = ServerTickTiming(startedNanos)
        val requestedNanos: Long = requestedSeconds * 1_000_000_000L

        fun summary(end: String): String {
            val stats = timing.snapshot()
            val mspt = stats.averageExecutionMillis
                ?: return "Server tick execution sample $end with no completed ticks yet."
            return "Server tick execution sample $end: ${"%.2f".format(Locale.ROOT, mspt)} ms/tick " +
                "from ${stats.completedTicks} completed ticks."
        }
    }
}

/** Avoids pulling Minecraft client classes into the shared performance service. */
private object GradleMCLog {
    fun message(value: String) = com.soumyajit.gradlemc.GradleMC.LOGGER.info(value)
}
