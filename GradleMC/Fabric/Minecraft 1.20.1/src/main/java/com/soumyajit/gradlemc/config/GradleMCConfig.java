package com.soumyajit.gradlemc.config;

import com.soumyajit.gradlemc.GradleMC;
import com.soumyajit.gradlemc.util.AtomicFiles;
import com.soumyajit.gradlemc.util.GradleMcPaths;
import com.soumyajit.gradlemc.util.GradleMcLimits;
import java.io.IOException;
import java.io.Reader;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;
import java.util.function.Function;

/** Fabric-local, schema-versioned properties configuration. Unknown valid keys are retained. */
public final class GradleMCConfig {
    public static final ConfigSpec SPEC = new ConfigSpec();
    private static final Map<String, ConfigValue<?>> VALUES = new LinkedHashMap<>();

    public static final ConfigValue<Boolean> REPORTS_ENABLED = bool("reportsEnabled", true);
    public static final ConfigValue<String> REPORT_DIRECTORY_NAME = string("reportDirectoryName", "reports");
    public static final ConfigValue<Integer> MAX_REPORT_FILES_LISTED = integer("maxReportFilesListed", 10);
    public static final ConfigValue<Integer> DEFAULT_ENTITY_SCAN_RADIUS = integer("defaultEntityScanRadius", 64);
    public static final ConfigValue<Integer> DEFAULT_BLOCK_ENTITY_SCAN_RADIUS = integer("defaultBlockEntityScanRadius", 64);
    public static final ConfigValue<Integer> MAX_SCAN_RADIUS = integer("maxScanRadius", 256);
    public static final ConfigValue<Integer> MAX_PERF_SECONDS = integer("maxPerfSeconds", 600);
    public static final ConfigValue<Integer> MAX_WORLDGEN_OBSERVATION_SECONDS = integer("maxWorldgenObservationSeconds", 900);
    public static final ConfigValue<Integer> MAX_FPS_TEST_SECONDS = integer("maxFpsTestSeconds", 600);
    public static final ConfigValue<Boolean> ISSUE_BUNDLE_ENABLED = bool("issueBundleEnabled", true);
    public static final ConfigValue<Boolean> ENABLE_RULE_CHECKS = bool("enableRuleChecks", true);
    public static final ConfigValue<String> RULES_FILE_NAME = string("rulesFileName", "gradlemc-rules.json");
    public static final ConfigValue<Boolean> VERBOSE_CHAT_OUTPUT = bool("verboseChatOutput", false);
    public static final ConfigValue<Boolean> SMART_DIAGNOSTICS_ENABLED = bool("smartDiagnosticsEnabled", true);
    public static final ConfigValue<Boolean> ADAPTIVE_BASELINE_ENABLED = bool("adaptiveBaselineEnabled", true);
    public static final ConfigValue<Integer> MIN_BASELINE_SAMPLES = integer("minBaselineSamples", 3);
    public static final ConfigValue<Integer> MAX_BASELINE_SAMPLES_STORED = integer("maxBaselineSamplesStored", 20);
    public static final ConfigValue<Integer> SMART_ADVICE_MAX_ITEMS = integer("smartAdviceMaxItems", 5);
    public static final ConfigValue<String> ANOMALY_SENSITIVITY = string("anomalySensitivity", "NORMAL");
    public static final ConfigValue<Boolean> SMART_SCORE_USES_ADAPTIVE_THRESHOLDS = bool("smartScoreUsesAdaptiveThresholds", true);
    public static final ConfigValue<Boolean> ENABLE_ADAPTIVE_SMART_AI = bool("enableAdaptiveSmartAi", true);
    public static final ConfigValue<Boolean> ENABLE_ADAPTIVE_AMBIENCE = bool("enableAdaptiveAmbience", true);
    public static final ConfigValue<Boolean> ENABLE_ADAPTIVE_EVENTS = bool("enableAdaptiveEvents", true);
    public static final ConfigValue<Integer> BASE_THREAT_GAIN = integer("baseThreatGain", 8);
    public static final ConfigValue<Integer> MAX_THREAT_LEVEL = integer("maxThreatLevel", 100);
    public static final ConfigValue<Integer> EVENT_COOLDOWN_TICKS = integer("eventCooldownTicks", 2400);
    public static final ConfigValue<Integer> AMBIENCE_COOLDOWN_TICKS = integer("ambienceCooldownTicks", 1200);
    public static final ConfigValue<Boolean> DEBUG_SMART_AI_LOGGING = bool("debugSmartAiLogging", false);
    public static final ConfigValue<Boolean> ALLOW_HIGH_INTENSITY_EVENTS = bool("allowHighIntensityEvents", false);
    public static final ConfigValue<Boolean> REDUCE_INTENSITY_AFTER_DEATH = bool("reduceIntensityAfterDeath", true);
    public static final ConfigValue<Double> ADAPTIVE_DIFFICULTY_MULTIPLIER = decimal("adaptiveDifficultyMultiplier", 1.0D);
    public static final ConfigValue<Integer> GUI_STATUS_REFRESH_TICKS = integer("guiStatusRefreshTicks", 100);
    public static final ConfigValue<Boolean> OVERLAY_ENABLED = bool("overlay.enabled", OverlayDefaults.ENABLED);
    public static final ConfigValue<String> OVERLAY_MODE = string("overlay.mode", OverlayDefaults.MODE);
    public static final ConfigValue<String> OVERLAY_POSITION = string("overlay.position", OverlayDefaults.POSITION);
    public static final ConfigValue<Double> OVERLAY_SCALE = decimal("overlay.scale", OverlayDefaults.SCALE);
    public static final ConfigValue<Boolean> OVERLAY_BACKGROUND_ENABLED = bool("overlay.backgroundEnabled", OverlayDefaults.BACKGROUND);
    public static final ConfigValue<Double> OVERLAY_BACKGROUND_OPACITY = decimal("overlay.backgroundOpacity", OverlayDefaults.OPACITY);
    public static final ConfigValue<Integer> OVERLAY_SAMPLING_WINDOW_SECONDS = integer("overlay.samplingWindowSeconds", OverlayDefaults.SAMPLING_WINDOW_SECONDS);
    public static final ConfigValue<Integer> OVERLAY_UPDATE_INTERVAL_MS = integer("overlay.updateIntervalMs", OverlayDefaults.UPDATE_INTERVAL_MS);
    public static final ConfigValue<Boolean> OVERLAY_SHOW_TITLE = bool("overlay.showTitle", OverlayDefaults.SHOW_TITLE);
    public static final ConfigValue<Boolean> OVERLAY_SHOW_FPS = bool("overlay.showFps", OverlayDefaults.SHOW_FPS);
    public static final ConfigValue<Boolean> OVERLAY_SHOW_AVERAGE_FPS = bool("overlay.showAverageFps", OverlayDefaults.SHOW_AVERAGE_FPS);
    public static final ConfigValue<Boolean> OVERLAY_SHOW_ONE_PERCENT_LOW = bool("overlay.showOnePercentLow", OverlayDefaults.SHOW_ONE_PERCENT_LOW);
    public static final ConfigValue<Boolean> OVERLAY_SHOW_POINT_ONE_PERCENT_LOW = bool("overlay.showPointOnePercentLow", OverlayDefaults.SHOW_POINT_ONE_PERCENT_LOW);
    public static final ConfigValue<Boolean> OVERLAY_SHOW_JVM_MEMORY = bool("overlay.showJvmMemory", OverlayDefaults.SHOW_JVM_MEMORY);
    public static final ConfigValue<Boolean> OVERLAY_SHOW_SYSTEM_MEMORY = bool("overlay.showSystemMemory", OverlayDefaults.SHOW_SYSTEM_MEMORY);
    public static final ConfigValue<Boolean> OVERLAY_SHOW_CPU = bool("overlay.showCpu", OverlayDefaults.SHOW_CPU);
    public static final ConfigValue<Boolean> OVERLAY_SHOW_GPU_NAME = bool("overlay.showGpuName", OverlayDefaults.SHOW_GPU_NAME);
    public static final ConfigValue<Boolean> OVERLAY_SHOW_GPU_USAGE = bool("overlay.showGpuUsage", OverlayDefaults.SHOW_GPU_USAGE);
    public static final ConfigValue<Boolean> OVERLAY_SHOW_INTEGRATED_SERVER = bool("overlay.showIntegratedServer", OverlayDefaults.SHOW_INTEGRATED_SERVER);
    public static final ConfigValue<Boolean> OVERLAY_SHOW_TEST_STATUS = bool("overlay.showTestStatus", OverlayDefaults.SHOW_TEST_STATUS);
    public static final ConfigValue<Boolean> OVERLAY_SHOW_PROFILER_STATUS = bool("overlay.showProfilerStatus", OverlayDefaults.SHOW_PROFILER_STATUS);
    public static final ConfigValue<Boolean> OVERLAY_SHOW_STABILITY = bool("overlay.showStability", OverlayDefaults.SHOW_STABILITY);

    private GradleMCConfig() {
    }

    private static ConfigValue<Boolean> bool(String key, boolean value) {
        return register(key, value, raw -> {
            if ("true".equalsIgnoreCase(raw) || "false".equalsIgnoreCase(raw)) return Boolean.valueOf(raw);
            throw new IllegalArgumentException("expected true or false");
        });
    }

    private static ConfigValue<Integer> integer(String key, int value) {
        return register(key, value, Integer::valueOf);
    }

    private static ConfigValue<Double> decimal(String key, double value) {
        return register(key, value, raw -> {
            double parsed = Double.parseDouble(raw);
            if (!Double.isFinite(parsed)) throw new IllegalArgumentException("expected a finite number");
            return parsed;
        });
    }

    private static ConfigValue<String> string(String key, String value) {
        return register(key, value, raw -> raw == null || raw.isBlank() ? value : raw.trim());
    }

    private static <T> ConfigValue<T> register(String key, T value, Function<String, T> parser) {
        ConfigValue<T> configValue = new ConfigValue<>(key, value, parser);
        VALUES.put(key, configValue);
        return configValue;
    }

    public static final class ConfigValue<T> {
        private final String key;
        private final T defaultValue;
        private final Function<String, T> parser;
        private T value;

        private ConfigValue(String key, T value, Function<String, T> parser) {
            this.key = key;
            this.defaultValue = value;
            this.parser = parser;
            this.value = value;
        }

        public T get() {
            return value;
        }

        public void set(T value) {
            this.value = value == null ? defaultValue : value;
        }

        private void reset() { value = defaultValue; }
        private void load(String raw) { value = parser.apply(raw); }
        private String serialized() { return String.valueOf(value); }
    }

    public static final class ConfigSpec {
        private static final String SCHEMA_KEY = "schemaVersion";
        private static final String SCHEMA_VERSION = "2";
        private final Path configuredPath;
        private final Properties properties = new Properties();
        private boolean loaded;

        private ConfigSpec() {
            this(null);
        }

        /** Package-visible for deterministic configuration persistence tests. */
        ConfigSpec(Path path) {
            this.configuredPath = path;
        }

        public synchronized Path path() {
            return configuredPath != null ? configuredPath
                    : GradleMcPaths.configFile();
        }

        public synchronized void load() {
            final Path configPath;
            try { configPath = path(); }
            catch (IllegalArgumentException unsafePath) { GradleMC.LOGGER.warn("Unsafe GradleMC Fabric config path; using defaults."); loaded = true; return; }
            properties.clear();
            VALUES.values().forEach(ConfigValue::reset);
            boolean fileExists = Files.isRegularFile(configPath);
            boolean fileLoaded = false;
            boolean unreadablePreserved = false;
            if (fileExists) {
                try (Reader input = Files.newBufferedReader(configPath, StandardCharsets.UTF_8)) {
                    if (Files.size(configPath) > GradleMcLimits.MAX_CONFIG_FILE_BYTES) throw new IOException("Config file exceeds the GradleMC size limit");
                    properties.load(input);
                    fileLoaded = true;
                } catch (IOException | IllegalArgumentException exception) {
                    GradleMC.LOGGER.warn("Could not read the GradleMC Fabric config; using defaults.", exception);
                    unreadablePreserved = quarantineUnreadable(configPath);
                }
            }
            for (Map.Entry<String, ConfigValue<?>> entry : VALUES.entrySet()) {
                String raw = properties.getProperty(entry.getKey());
                if (raw == null) continue;
                try {
                    entry.getValue().load(raw);
                } catch (RuntimeException exception) {
                    GradleMC.LOGGER.warn("Invalid GradleMC config value for {}; using default.", entry.getKey());
                    entry.getValue().reset();
                }
            }
            boolean migrationRequired = fileLoaded && migrateOverlayValues();
            loaded = true;
            if (!fileExists || migrationRequired || (!fileLoaded && unreadablePreserved)) {
                save();
            }
        }

        private boolean migrateOverlayValues() {
            // v1.0.0 had no persisted Fabric config. Explicit new controls therefore use the safe v1.0.1 defaults.
            boolean migrationRequired = !SCHEMA_VERSION.equals(properties.getProperty(SCHEMA_KEY));
            if (!properties.containsKey("overlay.showTitle")) OVERLAY_SHOW_TITLE.reset();
            if (!properties.containsKey("overlay.showAverageFps")) OVERLAY_SHOW_AVERAGE_FPS.reset();
            return migrationRequired;
        }

        public synchronized void save() {
            if (!loaded) load();
            Path configPath = path();
            VALUES.forEach((key, value) -> properties.setProperty(key, value.serialized()));
            properties.setProperty(SCHEMA_KEY, SCHEMA_VERSION);
            try {
                StringWriter rendered = new StringWriter();
                properties.store(rendered, "GradleMC Fabric configuration");
                AtomicFiles.writeUtf8(configPath, rendered.toString());
            } catch (IOException exception) {
                GradleMC.LOGGER.warn("Could not save the GradleMC Fabric config.", exception);
            }
        }

        private boolean quarantineUnreadable(Path configPath) {
            try {
                Path backup = configPath.resolveSibling(configPath.getFileName() + ".corrupt");
                if (Files.exists(backup)) return false;
                Files.move(configPath, backup);
                return true;
            } catch (IOException backupFailure) {
                GradleMC.LOGGER.warn("Could not preserve the unreadable GradleMC Fabric config.", backupFailure);
                return false;
            }
        }
    }
}
