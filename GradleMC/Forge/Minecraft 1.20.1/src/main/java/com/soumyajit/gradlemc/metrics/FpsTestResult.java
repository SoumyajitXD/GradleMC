package com.soumyajit.gradlemc.metrics;

import java.time.Instant;
import java.util.OptionalDouble;

public record FpsTestResult(
        int requestedSeconds,
        double actualSeconds,
        int samples,
        double averageFps,
        int minFps,
        int maxFps,
        OptionalDouble onePercentLowFps,
        Instant startedAt,
        Instant endedAt,
        EndReason endReason
) {
    public enum EndReason {
        COMPLETED,
        STOPPED,
        CANCELLED,
        ERROR
    }
}
