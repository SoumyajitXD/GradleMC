package com.soumyajit.gradlemc.performance

import com.soumyajit.gradlemc.config.GradleMcConfig
import java.util.Locale

enum class PerformanceMode(val configValue: String, val label: String, val samplingStride: Int) {
    LOW_IMPACT("low_impact", "Low Impact", 4), BALANCED("balanced", "Balanced", 2), DETAILED("detailed", "Detailed", 1);
    companion object { fun parse(value: String?) = entries.firstOrNull { it.configValue == value?.trim()?.lowercase(Locale.ROOT) } ?: BALANCED }
}
data class PerformanceSnapshot(val mode: PerformanceMode, val currentFps: Double?, val averageFps: Double?, val observedFrames: Long, val message: String, val averagingWindowSeconds: Int, val samplingStride: Int)
data class PerformanceModeChangeResult(val success: Boolean, val message: String)
data class ServerTickTimingStats(val elapsedNanos: Long, val completedTicks: Long, val totalExecutionNanos: Long) { val averageExecutionMillis get() = if (completedTicks == 0L) null else totalExecutionNanos / completedTicks / 1e6 }
internal class ServerTickTiming(private val started: Long) { private var tickStart = -1L; private var elapsed = 0L; private var completed = 0L; private var total = 0L; val elapsedNanos get() = elapsed
    fun start(now: Long) { update(now); tickStart = now }; fun end(now: Long) { update(now); val s=tickStart; tickStart=-1; val duration=now-s; if(s>=0 && duration in 1..60_000_000_000L) { completed++; total+=duration } }; fun snapshot()=ServerTickTimingStats(elapsed,completed,total); private fun update(now:Long){elapsed=(now-started).coerceAtLeast(0)} }

/** Common-safe owner for client render timing and server START-to-END tick timing. */
object PerformanceService {
    private var selected = PerformanceMode.BALANCED; private var window = 60; private var demand = false
    private val timing = RollingFrameTiming(policy(selected, window)); private var sample: TimedSample? = null
    @Synchronized fun setMode(value: PerformanceMode): PerformanceModeChangeResult = try { GradleMcConfig.update { it.copy(performanceMode = value.configValue) }; selected=value; timing.setPolicy(policy(value,window)); PerformanceModeChangeResult(true,"Performance mode set to ${value.label}.") } catch(e:Exception) { PerformanceModeChangeResult(false,"Unable to persist performance mode: ${e.message ?: e.javaClass.simpleName}") }
    @Synchronized fun setOverlayFrameDemand(enabled: Boolean) { if (demand != enabled) { demand=enabled; timing.discontinuity() } }
    @Synchronized fun configureFromConfig() { val c=GradleMcConfig.current(); val newMode=PerformanceMode.parse(c.performanceMode); val newWindow=c.overlaySamplingWindowSeconds; if(newMode!=selected || newWindow!=window) { selected=newMode; window=newWindow; timing.setPolicy(policy(selected,window)) } }
    fun recordRenderedFrame(now: Long = System.nanoTime()) {
        if (!demand && !FpsTestService.hasActiveTest()) return
        synchronized(this) {
            val outcome = if (demand) timing.record(now) else null
            if (outcome == FrameRecordOutcome.DISCONTINUITY) FpsTestService.markFrameDiscontinuity()
            if (FpsTestService.hasActiveTest()) FpsTestService.recordRenderedFrame(now)
        }
    }
    @Synchronized fun resetFrameTiming() { timing.discontinuity(); FpsTestService.markFrameDiscontinuity() }
    @Synchronized fun resetClientSession() { timing.reset(); FpsTestService.resetClientSession() }
    @Synchronized fun snapshot(): PerformanceSnapshot { configureFromConfig(); val rolling=timing.snapshot(); val evidence=if(rolling.observedFrames>0) rolling else FpsTestService.evidence(); return PerformanceSnapshot(selected,evidence.currentFps,evidence.averageFps,evidence.observedFrames,if(rolling.observedFrames>0) "Rendered-frame timing uses a ${window}s rolling window." else if(evidence.observedFrames>0) "FPS values use the explicit rendered-frame test; passive rolling sampling is inactive." else "No rendered-frame samples have been collected yet.",window,selected.samplingStride) }
    fun startTimedSample(seconds:Int):FpsTestActionResult { synchronized(this) { if(seconds !in 1..3600)return FpsTestActionResult(false,"Performance sample duration must be between 1 and 3600 seconds."); if(sample!=null)return FpsTestActionResult(false,"A performance sample is already running. Use /gradlemc perf stop first."); sample=TimedSample(seconds,System.nanoTime());return FpsTestActionResult(true,"Server tick execution sample started for $seconds seconds.") } }
    @Synchronized fun stopTimedSample():FpsTestActionResult { val active=sample?:return FpsTestActionResult(false,"No performance sample is currently running.");sample=null;return FpsTestActionResult(true,active.summary("stopped")) }
    @Synchronized fun recordServerTickStart(now:Long=System.nanoTime()){sample?.timing?.start(now)}
    @Synchronized fun recordServerTickEnd(now:Long=System.nanoTime()):String? { val active=sample?:return null;active.timing.end(now);return if(active.timing.elapsedNanos>=active.seconds*1_000_000_000L){sample=null;active.summary("completed")}else null }
    @Synchronized fun resetServerTiming()= (sample!=null).also { sample=null }; @Synchronized fun timedSampleState()=sample?.summary("running")?:"No performance sample is currently running."
    fun overheadDescription()="GradleMC uses bounded primitive frame buffers; mode controls bucket detail while the FPS window is configured separately."; fun guardDescription()="Performance modes change only GradleMC bucket detail: Low Impact groups 4 frame intervals, Balanced 2, Detailed 1."; fun explainDescription()="FPS uses completed render-frame intervals and a bounded duration-weighted rolling window, never game ticks."; fun selfTest()=true
    private fun policy(m:PerformanceMode,w:Int)=FrameTimingPolicy(m.samplingStride,w*1_000_000_000L,(w*2400+m.samplingStride-1)/m.samplingStride)
    private class TimedSample(val seconds:Int,started:Long){val timing=ServerTickTiming(started);fun summary(end:String):String{val s=timing.snapshot();val mspt=s.averageExecutionMillis?:return "Server tick execution sample $end with no completed ticks yet.";return "Server tick execution sample $end: ${"%.2f".format(Locale.ROOT,mspt)} ms/tick from ${s.completedTicks} completed ticks."}}
}
