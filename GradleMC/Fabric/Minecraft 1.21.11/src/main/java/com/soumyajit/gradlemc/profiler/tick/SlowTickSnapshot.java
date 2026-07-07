package com.soumyajit.gradlemc.profiler.tick;

import com.soumyajit.gradlemc.profiler.sampling.StackTraceAggregator;

import java.time.Instant;
import java.util.List;

public record SlowTickSnapshot(
        Instant timestamp,
        double durationMillis,
        String level,
        int loadedChunks,
        int entities,
        int blockEntities,
        long heapUsedMiB,
        long gcCountDelta,
        long gcTimeDeltaMillis,
        String likelyCategory,
        String confidence,
        List<StackTraceAggregator.FrameCount> topStackFrames
) {
}
