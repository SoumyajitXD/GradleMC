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
        public MetricStats add(double value, int maxSamples) {
            int boundedSamples = Math.max(1, Math.min(samples, maxSamples - 1));
            int nextSamples = Math.min(maxSamples, samples + 1);
            double nextAverage = ((average * boundedSamples) + value) / (boundedSamples + 1);
            return new MetricStats(nextSamples, nextAverage, Math.min(min, value), Math.max(max, value));
        }
    }
}
