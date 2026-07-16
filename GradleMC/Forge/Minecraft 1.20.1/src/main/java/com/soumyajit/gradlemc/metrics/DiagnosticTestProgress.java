package com.soumyajit.gradlemc.metrics;

public record DiagnosticTestProgress(boolean running, int requestedSeconds, int elapsedSeconds) {
    private static final int MAX_DURATION_SECONDS = 1_800;

    public DiagnosticTestProgress {
        requestedSeconds = Math.max(0, Math.min(MAX_DURATION_SECONDS, requestedSeconds));
        elapsedSeconds = Math.max(0, Math.min(requestedSeconds, elapsedSeconds));
        if (requestedSeconds == 0) running = false;
    }
    public static DiagnosticTestProgress idle() {
        return new DiagnosticTestProgress(false, 0, 0);
    }

    public int clampedElapsedSeconds() {
        if (!running) {
            return 0;
        }
        return Math.max(0, Math.min(elapsedSeconds, requestedSeconds));
    }

    public int percent() {
        if (!running || requestedSeconds <= 0) {
            return 0;
        }
        return Math.max(0, Math.min(100, (int) Math.round((clampedElapsedSeconds() * 100.0D) / requestedSeconds)));
    }
}
