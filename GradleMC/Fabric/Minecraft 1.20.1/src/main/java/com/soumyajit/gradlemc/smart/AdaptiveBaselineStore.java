package com.soumyajit.gradlemc.smart;

import com.soumyajit.gradlemc.GradleMC;
import com.soumyajit.gradlemc.config.GradleMCConfig;
import com.soumyajit.gradlemc.metrics.FpsTestResult;
import com.soumyajit.gradlemc.metrics.PerformanceTestResult;
import com.soumyajit.gradlemc.metrics.WorldgenObservationResult;
import com.soumyajit.gradlemc.util.GradleMcPaths;
import com.soumyajit.gradlemc.util.RuntimeSnapshots;
import com.soumyajit.gradlemc.util.AtomicFiles;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;

public final class AdaptiveBaselineStore {
    private static final String MEMORY_USED_MIB = "memory.usedMiB";
    private static final String ENTITY_COUNT = "entities.count";
    private static final String BLOCK_ENTITY_COUNT = "blockEntities.count";
    private static final String FPS_AVERAGE = "fps.average";
    private static final String TPS = "perf.tps";
    private static final String MSPT = "perf.mspt";
    private static final String WORLDGEN_TICK_MS = "worldgen.tickMs";
    private static final String WORLDGEN_MEMORY_GROWTH_MIB = "worldgen.memoryGrowthMiB";

    private AdaptiveBaselineStore() {
    }

    public static Path path() {
        return GradleMcPaths.adaptiveBaselineFile();
    }

    public static AdaptiveBaseline load() {
        Path path = path();
        if (!Files.isRegularFile(path)) {
            return AdaptiveBaseline.empty();
        }
        Properties properties = new Properties();
        try (InputStream inputStream = Files.newInputStream(path)) {
            properties.load(inputStream);
        } catch (IOException exception) {
            GradleMC.LOGGER.warn("Could not read GradleMC adaptive baseline", exception);
            return AdaptiveBaseline.empty();
        }

        Map<String, AdaptiveBaseline.MetricStats> metrics = new LinkedHashMap<>();
        for (String name : metricNames()) {
            int samples = intValue(properties, name + ".samples", 0);
            if (samples <= 0) {
                continue;
            }
            double average = doubleValue(properties, name + ".average", 0.0D);
            double min = doubleValue(properties, name + ".min", average);
            double max = doubleValue(properties, name + ".max", average);
            metrics.put(name, new AdaptiveBaseline.MetricStats(samples, average, min, max));
        }
        Instant lastUpdated = null;
        String updated = properties.getProperty("lastUpdated");
        if (updated != null && !updated.isBlank()) {
            try {
                lastUpdated = Instant.parse(updated);
            } catch (RuntimeException ignored) {
                lastUpdated = null;
            }
        }
        return new AdaptiveBaseline(metrics, lastUpdated);
    }

    public static void updateMemorySnapshot() {
        RuntimeSnapshots.MemorySnapshot memory = RuntimeSnapshots.memory();
        updateMetric(MEMORY_USED_MIB, memory.usedMiB());
    }

    public static void recordEntityScan(int count) {
        updateMetric(ENTITY_COUNT, count);
    }

    public static void recordBlockEntityScan(int count) {
        updateMetric(BLOCK_ENTITY_COUNT, count);
    }

    public static void recordFps(FpsTestResult result) {
        if (result.endReason() != FpsTestResult.EndReason.ERROR && result.hasSamples()) {
            updateMetric(FPS_AVERAGE, result.averageFps());
        }
    }

    public static void recordPerformance(PerformanceTestResult result) {
        if (result.endReason() != PerformanceTestResult.EndReason.ERROR && result.sampleCount() > 0) {
            updateMetrics(Map.of(
                    TPS, result.approximateTps(),
                    MSPT, result.averageTickMs(),
                    MEMORY_USED_MIB, (double) result.memoryEndMiB()
            ));
        }
    }

    public static void recordWorldgen(WorldgenObservationResult result) {
        if (result.endReason() != WorldgenObservationResult.EndReason.ERROR && result.sampleCount() > 0) {
            updateMetrics(Map.of(
                    WORLDGEN_TICK_MS, result.averageTickMs(),
                    WORLDGEN_MEMORY_GROWTH_MIB, (double) Math.max(0L, result.memoryEndMiB() - result.memoryStartMiB()),
                    MEMORY_USED_MIB, (double) result.memoryEndMiB()
            ));
        }
    }

    public static boolean reset() throws IOException {
        Path path = path();
        if (!Files.exists(path)) {
            return false;
        }
        Files.delete(path);
        return true;
    }

    private static void updateMetric(String name, double value) {
        if (!Double.isFinite(value)) return;
        updateMetrics(Map.of(name, value));
    }

    private static void updateMetrics(Map<String, Double> values) {
        if (!GradleMCConfig.ADAPTIVE_BASELINE_ENABLED.get()) {
            return;
        }
        AdaptiveBaseline baseline = load();
        Map<String, AdaptiveBaseline.MetricStats> metrics = new LinkedHashMap<>(baseline.metrics());
        int maxSamples = Math.max(1, Math.min(1_000, GradleMCConfig.MAX_BASELINE_SAMPLES_STORED.get()));
        for (Map.Entry<String, Double> entry : values.entrySet()) {
            AdaptiveBaseline.MetricStats previous = metrics.get(entry.getKey());
            if (previous == null) {
                double value = entry.getValue();
                metrics.put(entry.getKey(), new AdaptiveBaseline.MetricStats(1, value, value, value));
            } else {
                metrics.put(entry.getKey(), previous.add(entry.getValue(), maxSamples));
            }
        }
        write(new AdaptiveBaseline(metrics, Instant.now()));
    }

    private static void write(AdaptiveBaseline baseline) {
        Properties properties = new Properties();
        if (baseline.lastUpdated() != null) {
            properties.setProperty("lastUpdated", baseline.lastUpdated().toString());
        }
        for (Map.Entry<String, AdaptiveBaseline.MetricStats> entry : baseline.metrics().entrySet()) {
            AdaptiveBaseline.MetricStats stats = entry.getValue();
            properties.setProperty(entry.getKey() + ".samples", String.valueOf(stats.samples()));
            properties.setProperty(entry.getKey() + ".average", String.format(java.util.Locale.ROOT, "%.3f", stats.average()));
            properties.setProperty(entry.getKey() + ".min", String.format(java.util.Locale.ROOT, "%.3f", stats.min()));
            properties.setProperty(entry.getKey() + ".max", String.format(java.util.Locale.ROOT, "%.3f", stats.max()));
        }
        try {
            StringWriter output = new StringWriter();
            properties.store(output, "GradleMC local adaptive diagnostics baseline. Aggregate metrics only.");
            AtomicFiles.writeUtf8(path(), output.toString());
        } catch (IOException exception) {
            GradleMC.LOGGER.warn("Could not write GradleMC adaptive baseline", exception);
        }
    }

    public static String memoryMetric() {
        return MEMORY_USED_MIB;
    }

    public static String entityMetric() {
        return ENTITY_COUNT;
    }

    public static String blockEntityMetric() {
        return BLOCK_ENTITY_COUNT;
    }

    public static String fpsMetric() {
        return FPS_AVERAGE;
    }

    public static String tpsMetric() {
        return TPS;
    }

    public static String msptMetric() {
        return MSPT;
    }

    public static String worldgenTickMetric() {
        return WORLDGEN_TICK_MS;
    }

    public static String worldgenMemoryGrowthMetric() {
        return WORLDGEN_MEMORY_GROWTH_MIB;
    }

    private static java.util.List<String> metricNames() {
        return java.util.List.of(MEMORY_USED_MIB, ENTITY_COUNT, BLOCK_ENTITY_COUNT, FPS_AVERAGE,
                TPS, MSPT, WORLDGEN_TICK_MS, WORLDGEN_MEMORY_GROWTH_MIB);
    }

    private static int intValue(Properties properties, String key, int fallback) {
        try {
            return Integer.parseInt(properties.getProperty(key, String.valueOf(fallback)));
        } catch (NumberFormatException exception) {
            return fallback;
        }
    }

    private static double doubleValue(Properties properties, String key, double fallback) {
        try {
            double value = Double.parseDouble(properties.getProperty(key, String.valueOf(fallback)));
            return Double.isFinite(value) ? value : fallback;
        } catch (NumberFormatException exception) {
            return fallback;
        }
    }
}
