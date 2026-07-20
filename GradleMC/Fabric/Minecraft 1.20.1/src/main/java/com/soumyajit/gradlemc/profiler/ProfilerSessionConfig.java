package com.soumyajit.gradlemc.profiler;

public record ProfilerSessionConfig(
        int timeoutSeconds,
        int intervalMillis,
        String threadPattern,
        double onlyTicksOverMillis,
        boolean includeSleeping,
        ProfilerMode mode
) {
    public static final int MIN_TIMEOUT_SECONDS = 5;
    public static final int MAX_TIMEOUT_SECONDS = 1800;
    public static final int MIN_INTERVAL_MILLIS = 4;
    public static final int MAX_INTERVAL_MILLIS = 1000;
    public static final double MIN_SLOW_TICK_MILLIS = 50.0D;
    public static final double MAX_SLOW_TICK_MILLIS = 5000.0D;

    public static ProfilerSessionConfig defaults() {
        return new ProfilerSessionConfig(60, 20, "server", 50.0D, false, ProfilerMode.COMBINED);
    }

    public ProfilerSessionConfig sanitized() {
        int safeTimeout = clamp(timeoutSeconds, MIN_TIMEOUT_SECONDS, MAX_TIMEOUT_SECONDS);
        int safeInterval = clamp(intervalMillis, MIN_INTERVAL_MILLIS, MAX_INTERVAL_MILLIS);
        String safeThread = threadPattern == null || threadPattern.isBlank() ? "server" : threadPattern.trim();
        double safeThreshold = !Double.isFinite(onlyTicksOverMillis) ? MIN_SLOW_TICK_MILLIS : onlyTicksOverMillis <= 0.0D
                ? 0.0D
                : Math.max(MIN_SLOW_TICK_MILLIS, Math.min(MAX_SLOW_TICK_MILLIS, onlyTicksOverMillis));
        ProfilerMode safeMode = mode == null ? ProfilerMode.COMBINED : mode;
        return new ProfilerSessionConfig(safeTimeout, safeInterval, safeThread, safeThreshold, includeSleeping, safeMode);
    }

    public boolean onlySlowTicks() {
        return onlyTicksOverMillis > 0.0D;
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}
