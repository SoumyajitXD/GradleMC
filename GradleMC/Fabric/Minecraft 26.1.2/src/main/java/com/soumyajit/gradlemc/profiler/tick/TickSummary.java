package com.soumyajit.gradlemc.profiler.tick;

public record TickSummary(
        int sampleCount,
        double averageMspt,
        double medianMspt,
        double p95Mspt,
        double p99Mspt,
        double minMspt,
        double maxMspt,
        int slowTickCount
) {
    public static TickSummary empty() {
        return new TickSummary(0, 0.0D, 0.0D, 0.0D, 0.0D, 0.0D, 0.0D, 0);
    }
}
