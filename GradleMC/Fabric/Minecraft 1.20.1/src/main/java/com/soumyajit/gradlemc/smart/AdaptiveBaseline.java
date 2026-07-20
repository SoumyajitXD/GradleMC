package com.soumyajit.gradlemc.smart;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

public final class AdaptiveBaseline {
    private final Map<String, MetricStats> metrics;
    private final Instant lastUpdated;

    public AdaptiveBaseline(Map<String, MetricStats> metrics, Instant lastUpdated) {
        this.metrics = new LinkedHashMap<>(metrics);
        this.lastUpdated = lastUpdated;
    }

    public static AdaptiveBaseline empty() {
        return new AdaptiveBaseline(Map.of(), null);
    }

    public Map<String, MetricStats> metrics() {
        return Map.copyOf(metrics);
    }

    public Optional<MetricStats> metric(String name) {
        return Optional.ofNullable(metrics.get(name));
    }

    public Instant lastUpdated() {
        return lastUpdated;
    }

    public boolean isEmpty() {
        return metrics.isEmpty();
    }

    public record MetricStats(int samples, double average, double min, double max) {
        public MetricStats {
            if (samples < 1) throw new IllegalArgumentException("samples must be positive");
            if (!Double.isFinite(average) || !Double.isFinite(min) || !Double.isFinite(max)) {
                throw new IllegalArgumentException("baseline metrics must be finite");
            }
        }

        public MetricStats add(double value, int maxSamples) {
            if (!Double.isFinite(value)) return this;
            int boundedMaximum = Math.max(1, maxSamples);
            int boundedSamples = Math.max(0, Math.min(samples, boundedMaximum - 1));
            int nextSamples = Math.min(boundedMaximum, samples + 1);
            double nextAverage = ((average * boundedSamples) + value) / (boundedSamples + 1);
            return new MetricStats(nextSamples, nextAverage, Math.min(min, value), Math.max(max, value));
        }
    }
}
