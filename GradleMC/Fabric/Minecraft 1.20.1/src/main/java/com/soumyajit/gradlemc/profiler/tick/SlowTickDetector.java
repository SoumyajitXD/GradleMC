package com.soumyajit.gradlemc.profiler.tick;

public final class SlowTickDetector {
    private final double thresholdMillis;

    public SlowTickDetector(double thresholdMillis) {
        this.thresholdMillis = Math.max(50.0D, thresholdMillis);
    }

    public boolean isSlow(double tickMillis) {
        return tickMillis >= thresholdMillis;
    }

    public String thresholdBand(double tickMillis) {
        if (tickMillis >= 500.0D) {
            return ">500ms";
        }
        if (tickMillis >= 250.0D) {
            return ">250ms";
        }
        if (tickMillis >= 150.0D) {
            return ">150ms";
        }
        if (tickMillis >= 100.0D) {
            return ">100ms";
        }
        if (tickMillis >= 75.0D) {
            return ">75ms";
        }
        if (tickMillis >= 50.0D) {
            return ">50ms";
        }
        return "normal";
    }

    public double thresholdMillis() {
        return thresholdMillis;
    }
}
