package com.soumyajit.gradlemc.smart;

import com.soumyajit.gradlemc.config.GradleMCConfig;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class AdaptiveThresholds {
    private AdaptiveThresholds() {
    }

    public static Threshold higherIsWorse(String metric, double fixedWarn, double fixedCritical, AdaptiveBaseline baseline) {
        Optional<AdaptiveBaseline.MetricStats> stats = baseline.metric(metric);
        if (useAdaptive(stats)) {
            double factor = sensitivityFactor();
            double adaptiveWarn = Math.max(fixedWarn, stats.get().average() * factor);
            return new Threshold(fixedWarn, fixedCritical, adaptiveWarn, Math.max(fixedCritical, adaptiveWarn * 1.35D), true);
        }
        return new Threshold(fixedWarn, fixedCritical, fixedWarn, fixedCritical, false);
    }

    public static Threshold lowerIsWorse(String metric, double fixedWarn, double fixedCritical, AdaptiveBaseline baseline) {
        Optional<AdaptiveBaseline.MetricStats> stats = baseline.metric(metric);
        if (useAdaptive(stats)) {
            double factor = switch (normalizedSensitivity()) {
                case "LOW" -> 0.60D;
                case "HIGH" -> 0.85D;
                default -> 0.75D;
            };
            double adaptiveWarn = Math.min(fixedWarn, stats.get().average() * factor);
            return new Threshold(fixedWarn, fixedCritical, adaptiveWarn, Math.min(fixedCritical, adaptiveWarn * 0.70D), true);
        }
        return new Threshold(fixedWarn, fixedCritical, fixedWarn, fixedCritical, false);
    }

    public static List<String> describe(AdaptiveBaseline baseline) {
        List<String> lines = new ArrayList<>();
        lines.add("Memory used: warn 80% heap, critical 95% heap; adaptive compares to local used-memory baseline when available.");
        lines.add("Loaded mods: watch 150, risky 250, unstable 350.");
        lines.add("Entities near player: warn 180, critical 350; adaptive compares latest scan to baseline.");
        lines.add("Block entities near player: warn 256, critical 512; adaptive compares latest scan to baseline.");
        lines.add("FPS average: warn below 45, critical below 25; adaptive compares latest client FPS to baseline.");
        lines.add("TPS/MSPT: warn below 18 TPS or above 45 MSPT, critical below 15 TPS or above 70 MSPT.");
        lines.add("Worldgen: warn above 50 average MSPT, critical above 100 max MSPT or large memory/chunk growth.");
        lines.add("Adaptive enabled: " + (GradleMCConfig.ADAPTIVE_BASELINE_ENABLED.get()
                && GradleMCConfig.SMART_SCORE_USES_ADAPTIVE_THRESHOLDS.get()));
        lines.add("Baseline metrics available: " + baseline.metrics().keySet());
        return lines;
    }

    private static boolean useAdaptive(Optional<AdaptiveBaseline.MetricStats> stats) {
        return GradleMCConfig.ADAPTIVE_BASELINE_ENABLED.get()
                && GradleMCConfig.SMART_SCORE_USES_ADAPTIVE_THRESHOLDS.get()
                && stats.isPresent()
                && stats.get().samples() >= GradleMCConfig.MIN_BASELINE_SAMPLES.get();
    }

    private static double sensitivityFactor() {
        return switch (normalizedSensitivity()) {
            case "LOW" -> 1.75D;
            case "HIGH" -> 1.25D;
            default -> 1.50D;
        };
    }

    private static String normalizedSensitivity() {
        String value = GradleMCConfig.ANOMALY_SENSITIVITY.get();
        return value == null ? "NORMAL" : value.toUpperCase(java.util.Locale.ROOT);
    }

    public record Threshold(double fixedWarn, double fixedCritical, double warn, double critical, boolean adaptive) {
    }
}
