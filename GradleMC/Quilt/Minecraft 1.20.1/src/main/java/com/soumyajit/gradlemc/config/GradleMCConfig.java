package com.soumyajit.gradlemc.config;

public final class GradleMCConfig {
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
        public void save() {
            // TODO Fabric port: persist mutable GradleMC config to a local JSON/properties file.
        }
    }
}
