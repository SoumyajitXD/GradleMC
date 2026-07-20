package com.soumyajit.gradlemc.config;

/** Authoritative command and configuration bounds for bounded diagnostic sessions. */
public final class DiagnosticDurationPolicy {
    public static final int MIN_FPS_SECONDS = 5;
    public static final int MIN_PERFORMANCE_SECONDS = 5;
    public static final int MIN_WORLDGEN_SECONDS = 10;
    public static final int HARD_MAX_SECONDS = 1_800;

    private DiagnosticDurationPolicy() {
    }

    public static int maxFpsSeconds() {
        return boundedMaximum(GradleMCConfig.MAX_FPS_TEST_SECONDS.get(), MIN_FPS_SECONDS);
    }

    public static int maxPerformanceSeconds() {
        return boundedMaximum(GradleMCConfig.MAX_PERF_SECONDS.get(), MIN_PERFORMANCE_SECONDS);
    }

    public static int maxWorldgenSeconds() {
        return boundedMaximum(GradleMCConfig.MAX_WORLDGEN_OBSERVATION_SECONDS.get(), MIN_WORLDGEN_SECONDS);
    }

    static int boundedMaximum(int configuredSeconds, int minimumSeconds) {
        return Math.max(minimumSeconds, Math.min(HARD_MAX_SECONDS, configuredSeconds));
    }
}
