package com.soumyajit.gradlemc.profiler.report;

import com.soumyajit.gradlemc.profiler.sampling.StackTraceAggregator;
import com.soumyajit.gradlemc.profiler.tick.SlowTickSnapshot;
import com.soumyajit.gradlemc.profiler.tick.TickRecord;
import com.soumyajit.gradlemc.profiler.tick.TickSummary;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

public record ProfilingSummary(
        String gradleMcVersion,
        String minecraftVersion,
        String neoForgeVersion,
        String javaVersion,
        String javaVendor,
        String os,
        int loadedModCount,
        Instant startedAt,
        Instant endedAt,
        Duration duration,
        TickSummary tickSummary,
        List<TickRecord> slowestTicks,
        List<SlowTickSnapshot> slowTickSnapshots,
        long cpuSamples,
        List<StackTraceAggregator.FrameCount> topThreads,
        List<StackTraceAggregator.FrameCount> topFrames,
        List<StackTraceAggregator.FrameCount> topLeaves,
        List<StackTraceAggregator.FrameCount> topPackages,
        List<ModAttribution> attributions,
        long usedHeapStartMiB,
        long usedHeapEndMiB,
        long maxHeapMiB,
        long gcCountDelta,
        long gcTimeDeltaMillis,
        String interpretation
) {
}
