package com.soumyajit.gradlemc.config;

import com.soumyajit.gradlemc.GradleMC;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Locale;
import java.util.Properties;

public final class GradleMCConfig {
    public static final String CONFIG_FILE_NAME = "gradlemc.properties";
    public static final ConfigSpec SPEC = new ConfigSpec();

    public static final ConfigValue<Boolean> REPORTS_ENABLED = bool(true);
    public static final ConfigValue<String> REPORT_DIRECTORY_NAME = value("reports");
    public static final ConfigValue<Integer> MAX_REPORT_FILES_LISTED = value(10);
    public static final ConfigValue<Integer> DEFAULT_ENTITY_SCAN_RADIUS = value(64);
    public static final ConfigValue<Integer> DEFAULT_BLOCK_ENTITY_SCAN_RADIUS = value(64);
    public static final ConfigValue<Integer> MAX_SCAN_RADIUS = value(256);
    public static final ConfigValue<Integer> DEFAULT_PERF_SECONDS = value(60);
    public static final ConfigValue<Integer> MAX_PERF_SECONDS = value(600);
    public static final ConfigValue<Integer> DEFAULT_WORLDGEN_OBSERVATION_SECONDS = value(120);
    public static final ConfigValue<Integer> MAX_WORLDGEN_OBSERVATION_SECONDS = value(900);
    public static final ConfigValue<Integer> DEFAULT_FPS_TEST_SECONDS = value(60);
    public static final ConfigValue<Integer> MAX_FPS_TEST_SECONDS = value(600);
    public static final ConfigValue<Boolean> ISSUE_BUNDLE_ENABLED = bool(true);
    public static final ConfigValue<Boolean> INCLUDE_LOG_SNIPPET_IN_ISSUE_BUNDLE = bool(false);
    public static final ConfigValue<Integer> LOG_SNIPPET_LINE_LIMIT = value(200);
    public static final ConfigValue<Boolean> ENABLE_RULE_CHECKS = bool(true);
    public static final ConfigValue<String> RULES_FILE_NAME = value("gradlemc-rules.json");
    public static final ConfigValue<Boolean> VERBOSE_CHAT_OUTPUT = bool(false);
    public static final ConfigValue<Boolean> SMART_DIAGNOSTICS_ENABLED = bool(true);
    public static final ConfigValue<Boolean> ADAPTIVE_BASELINE_ENABLED = bool(true);
    public static final ConfigValue<Integer> MIN_BASELINE_SAMPLES = value(3);
    public static final ConfigValue<Integer> MAX_BASELINE_SAMPLES_STORED = value(20);
    public static final ConfigValue<Integer> SMART_ADVICE_MAX_ITEMS = value(5);
    public static final ConfigValue<String> ANOMALY_SENSITIVITY = value("NORMAL");
    public static final ConfigValue<Boolean> SMART_SCORE_USES_ADAPTIVE_THRESHOLDS = bool(true);
    public static final ConfigValue<Boolean> ENABLE_ADAPTIVE_SMART_AI = bool(true);
    public static final ConfigValue<Boolean> ENABLE_ADAPTIVE_AMBIENCE = bool(true);
    public static final ConfigValue<Boolean> ENABLE_ADAPTIVE_EVENTS = bool(true);
    public static final ConfigValue<Integer> BASE_THREAT_GAIN = value(8);
    public static final ConfigValue<Integer> MAX_THREAT_LEVEL = value(100);
    public static final ConfigValue<Integer> THREAT_DECAY_RATE = value(2);
    public static final ConfigValue<Integer> EVENT_COOLDOWN_TICKS = value(2400);
    public static final ConfigValue<Integer> AMBIENCE_COOLDOWN_TICKS = value(1200);
    public static final ConfigValue<Boolean> DEBUG_SMART_AI_LOGGING = bool(false);
    public static final ConfigValue<Boolean> ALLOW_HIGH_INTENSITY_EVENTS = bool(false);
    public static final ConfigValue<Boolean> REDUCE_INTENSITY_AFTER_DEATH = bool(true);
    public static final ConfigValue<Double> ADAPTIVE_DIFFICULTY_MULTIPLIER = value(1.0D);
    public static final ConfigValue<Integer> GUI_STATUS_REFRESH_TICKS = value(100);
    public static final ConfigValue<Boolean> OVERLAY_ENABLED = bool(OverlayDefaults.ENABLED);
    public static final ConfigValue<String> OVERLAY_MODE = value(OverlayDefaults.MODE);
    public static final ConfigValue<String> OVERLAY_POSITION = value(OverlayDefaults.POSITION);
    public static final ConfigValue<Double> OVERLAY_SCALE = value(OverlayDefaults.SCALE);
    public static final ConfigValue<Boolean> OVERLAY_BACKGROUND_ENABLED = bool(OverlayDefaults.BACKGROUND);
    public static final ConfigValue<Double> OVERLAY_BACKGROUND_OPACITY = value(OverlayDefaults.OPACITY);
    public static final ConfigValue<Integer> OVERLAY_SAMPLING_WINDOW_SECONDS = value(OverlayDefaults.SAMPLING_WINDOW_SECONDS);
    public static final ConfigValue<Integer> OVERLAY_UPDATE_INTERVAL_MS = value(OverlayDefaults.UPDATE_INTERVAL_MS);
    public static final ConfigValue<Boolean> OVERLAY_SHOW_FPS = bool(OverlayDefaults.SHOW_FPS);
    public static final ConfigValue<Boolean> OVERLAY_SHOW_ONE_PERCENT_LOW = bool(OverlayDefaults.SHOW_ONE_PERCENT_LOW);
    public static final ConfigValue<Boolean> OVERLAY_SHOW_POINT_ONE_PERCENT_LOW = bool(OverlayDefaults.SHOW_POINT_ONE_PERCENT_LOW);
    public static final ConfigValue<Boolean> OVERLAY_SHOW_JVM_MEMORY = bool(OverlayDefaults.SHOW_JVM_MEMORY);
    public static final ConfigValue<Boolean> OVERLAY_SHOW_SYSTEM_MEMORY = bool(OverlayDefaults.SHOW_SYSTEM_MEMORY);
    public static final ConfigValue<Boolean> OVERLAY_SHOW_CPU = bool(OverlayDefaults.SHOW_CPU);
    public static final ConfigValue<Boolean> OVERLAY_SHOW_GPU_NAME = bool(OverlayDefaults.SHOW_GPU_NAME);
    public static final ConfigValue<Boolean> OVERLAY_SHOW_GPU_USAGE = bool(OverlayDefaults.SHOW_GPU_USAGE);

    private GradleMCConfig() {
    }

    public static Path path() {
        return FabricLoader.getInstance().getConfigDir().resolve(CONFIG_FILE_NAME).normalize();
    }

    private static ConfigValue<Boolean> bool(boolean value) {
        return value(value);
    }

    private static <T> ConfigValue<T> value(T value) {
        return new ConfigValue<>(value);
    }

    public static final class ConfigValue<T> {
        private T value;

        private ConfigValue(T value) {
            this.value = value;
        }

        public T get() {
            return value;
        }

        public void set(T value) {
            this.value = value;
        }
    }

    public static final class ConfigSpec {
        public synchronized void load() {
            Path path = GradleMCConfig.path();
            if (!Files.isRegularFile(path)) {
                save();
                return;
            }
            Properties properties = new Properties();
            try (InputStream input = Files.newInputStream(path)) {
                properties.load(input);
                OVERLAY_ENABLED.set(booleanValue(properties, "overlay.enabled", OVERLAY_ENABLED.get()));
                OVERLAY_MODE.set(listValue(properties, "overlay.mode", OVERLAY_MODE.get(), List.of("COMPACT", "DETAILED")));
                OVERLAY_POSITION.set(listValue(properties, "overlay.position", OVERLAY_POSITION.get(),
                        List.of("TOP_LEFT", "TOP_RIGHT", "BOTTOM_LEFT", "BOTTOM_RIGHT")));
                OVERLAY_SCALE.set(doubleValue(properties, "overlay.scale", OVERLAY_SCALE.get(), 0.75D, 2.0D));
                OVERLAY_BACKGROUND_ENABLED.set(booleanValue(properties, "overlay.backgroundEnabled", OVERLAY_BACKGROUND_ENABLED.get()));
                OVERLAY_BACKGROUND_OPACITY.set(doubleValue(properties, "overlay.backgroundOpacity", OVERLAY_BACKGROUND_OPACITY.get(), 0.0D, 1.0D));
                OVERLAY_SAMPLING_WINDOW_SECONDS.set(listIntValue(properties, "overlay.samplingWindowSeconds",
                        OVERLAY_SAMPLING_WINDOW_SECONDS.get(), List.of(30, 60, 120)));
                OVERLAY_UPDATE_INTERVAL_MS.set(listIntValue(properties, "overlay.updateIntervalMs",
                        OVERLAY_UPDATE_INTERVAL_MS.get(), List.of(250, 500, 1000)));
                OVERLAY_SHOW_FPS.set(booleanValue(properties, "overlay.showFps", OVERLAY_SHOW_FPS.get()));
                OVERLAY_SHOW_ONE_PERCENT_LOW.set(booleanValue(properties, "overlay.showOnePercentLow", OVERLAY_SHOW_ONE_PERCENT_LOW.get()));
                OVERLAY_SHOW_POINT_ONE_PERCENT_LOW.set(booleanValue(properties, "overlay.showPointOnePercentLow", OVERLAY_SHOW_POINT_ONE_PERCENT_LOW.get()));
                OVERLAY_SHOW_JVM_MEMORY.set(booleanValue(properties, "overlay.showJvmMemory", OVERLAY_SHOW_JVM_MEMORY.get()));
                OVERLAY_SHOW_SYSTEM_MEMORY.set(booleanValue(properties, "overlay.showSystemMemory", OVERLAY_SHOW_SYSTEM_MEMORY.get()));
                OVERLAY_SHOW_CPU.set(booleanValue(properties, "overlay.showCpu", OVERLAY_SHOW_CPU.get()));
                OVERLAY_SHOW_GPU_NAME.set(booleanValue(properties, "overlay.showGpuName", OVERLAY_SHOW_GPU_NAME.get()));
                OVERLAY_SHOW_GPU_USAGE.set(booleanValue(properties, "overlay.showGpuUsage", OVERLAY_SHOW_GPU_USAGE.get()));
            } catch (IOException | IllegalArgumentException exception) {
                GradleMC.LOGGER.warn("Could not load GradleMC Fabric config from {}: {}", path, exception.getMessage());
            }
        }

        public synchronized void save() {
            Path path = GradleMCConfig.path();
            Path temporary = path.resolveSibling(path.getFileName() + ".tmp");
            Properties properties = new Properties();
            properties.setProperty("overlay.enabled", String.valueOf(OVERLAY_ENABLED.get()));
            properties.setProperty("overlay.mode", OVERLAY_MODE.get());
            properties.setProperty("overlay.position", OVERLAY_POSITION.get());
            properties.setProperty("overlay.scale", String.valueOf(OVERLAY_SCALE.get()));
            properties.setProperty("overlay.backgroundEnabled", String.valueOf(OVERLAY_BACKGROUND_ENABLED.get()));
            properties.setProperty("overlay.backgroundOpacity", String.valueOf(OVERLAY_BACKGROUND_OPACITY.get()));
            properties.setProperty("overlay.samplingWindowSeconds", String.valueOf(OVERLAY_SAMPLING_WINDOW_SECONDS.get()));
            properties.setProperty("overlay.updateIntervalMs", String.valueOf(OVERLAY_UPDATE_INTERVAL_MS.get()));
            properties.setProperty("overlay.showFps", String.valueOf(OVERLAY_SHOW_FPS.get()));
            properties.setProperty("overlay.showOnePercentLow", String.valueOf(OVERLAY_SHOW_ONE_PERCENT_LOW.get()));
            properties.setProperty("overlay.showPointOnePercentLow", String.valueOf(OVERLAY_SHOW_POINT_ONE_PERCENT_LOW.get()));
            properties.setProperty("overlay.showJvmMemory", String.valueOf(OVERLAY_SHOW_JVM_MEMORY.get()));
            properties.setProperty("overlay.showSystemMemory", String.valueOf(OVERLAY_SHOW_SYSTEM_MEMORY.get()));
            properties.setProperty("overlay.showCpu", String.valueOf(OVERLAY_SHOW_CPU.get()));
            properties.setProperty("overlay.showGpuName", String.valueOf(OVERLAY_SHOW_GPU_NAME.get()));
            properties.setProperty("overlay.showGpuUsage", String.valueOf(OVERLAY_SHOW_GPU_USAGE.get()));
            try {
                Files.createDirectories(path.getParent());
                try (OutputStream output = Files.newOutputStream(temporary)) {
                    properties.store(output, "GradleMC Fabric client settings");
                }
                try {
                    Files.move(temporary, path, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
                } catch (AtomicMoveNotSupportedException exception) {
                    Files.move(temporary, path, StandardCopyOption.REPLACE_EXISTING);
                }
            } catch (IOException exception) {
                GradleMC.LOGGER.error("Could not save GradleMC Fabric config to {}", path, exception);
            }
        }

        private static boolean booleanValue(Properties properties, String key, boolean fallback) {
            String raw = properties.getProperty(key);
            if (raw == null) return fallback;
            if (raw.equalsIgnoreCase("true")) return true;
            if (raw.equalsIgnoreCase("false")) return false;
            throw new IllegalArgumentException(key + " must be true or false");
        }

        private static String listValue(Properties properties, String key, String fallback, List<String> allowed) {
            String raw = properties.getProperty(key);
            if (raw == null) return fallback;
            String normalized = raw.trim().toUpperCase(Locale.ROOT);
            if (!allowed.contains(normalized)) throw new IllegalArgumentException(key + " has unsupported value " + raw);
            return normalized;
        }

        private static double doubleValue(Properties properties, String key, double fallback, double min, double max) {
            String raw = properties.getProperty(key);
            if (raw == null) return fallback;
            double parsed = Double.parseDouble(raw.trim());
            if (!Double.isFinite(parsed) || parsed < min || parsed > max) {
                throw new IllegalArgumentException(key + " must be between " + min + " and " + max);
            }
            return parsed;
        }

        private static int listIntValue(Properties properties, String key, int fallback, List<Integer> allowed) {
            String raw = properties.getProperty(key);
            if (raw == null) return fallback;
            int parsed = Integer.parseInt(raw.trim());
            if (!allowed.contains(parsed)) throw new IllegalArgumentException(key + " has unsupported value " + raw);
            return parsed;
        }
    }
}
