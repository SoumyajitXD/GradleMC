package com.soumyajit.gradlemc.profiler.tick;

import java.time.Instant;

public record TickRecord(
        Instant timestamp,
        double durationMillis,
        double approximateTps,
        int dimensionCount,
        int playerCount,
        int loadedChunkCount,
        int entityCount,
        int blockEntityCount,
        long heapUsedBeforeMiB,
        long heapUsedAfterMiB,
        long gcCountDelta,
        long gcTimeDeltaMillis,
        boolean worldgenObserved
) {
}
