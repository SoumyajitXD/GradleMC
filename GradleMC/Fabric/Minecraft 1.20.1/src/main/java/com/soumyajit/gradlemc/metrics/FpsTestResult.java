package com.soumyajit.gradlemc.metrics;

import java.time.Instant;
import java.util.OptionalDouble;

public record FpsTestResult(
        int requestedSeconds,
        double actualSeconds,
        int samples,
        Double averageFps,
        Integer minFps,
        Integer maxFps,
        OptionalDouble onePercentLowFps,
        Instant startedAt,
        Instant endedAt,
        EndReason endReason
) {
    public boolean hasSamples() {
        return samples > 0 && averageFps != null && minFps != null && maxFps != null;
    }
    public enum EndReason {
        COMPLETED,
        STOPPED,
        CANCELLED,
        ERROR
    }
}
