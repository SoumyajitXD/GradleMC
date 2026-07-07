package com.soumyajit.gradlemc.metrics;

import java.time.Instant;

public record PerformanceTestResult(
        int requestedSeconds,
        double actualSeconds,
        int sampleCount,
        double approximateTps,
        double averageTickMs,
        double minTickMs,
        double maxTickMs,
        double minTickIntervalMs,
        double maxTickIntervalMs,
        long memoryStartMiB,
        long memoryEndMiB,
        int playersStart,
        int playersEnd,
        int worldsStart,
        int worldsEnd,
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
