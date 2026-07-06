package com.soumyajit.gradlemc.metrics;

public record DiagnosticTestProgress(boolean running, int requestedSeconds, int elapsedSeconds) {
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
