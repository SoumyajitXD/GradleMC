package com.soumyajit.gradlemc.metrics;

import java.time.Instant;
import java.util.List;
import java.util.Set;

public record WorldgenObservationResult(
        int requestedSeconds,
        double actualSeconds,
        int sampleCount,
        Set<String> dimensions,
        List<String> playerSnapshots,
        int loadedChunksStart,
        int loadedChunksEnd,
        int loadedChunksMin,
        int loadedChunksMax,
        double averageTickMs,
        double maxTickMs,
        long memoryStartMiB,
        long memoryEndMiB,
        List<String> warnings,
        Instant startedAt,
        Instant endedAt,
        EndReason endReason
) {
    public enum EndReason {
        COMPLETED,
        STOPPED,
        ERROR
    }
}
