package com.soumyajit.gradlemc.config;

import net.minecraftforge.common.ForgeConfigSpec;

public final class GradleMCConfig {
    public static final ForgeConfigSpec SPEC;

    public static final ForgeConfigSpec.BooleanValue REPORTS_ENABLED;
    public static final ForgeConfigSpec.ConfigValue<String> REPORT_DIRECTORY_NAME;
    public static final ForgeConfigSpec.IntValue MAX_REPORT_FILES_LISTED;
    public static final ForgeConfigSpec.IntValue DEFAULT_ENTITY_SCAN_RADIUS;
    public static final ForgeConfigSpec.IntValue DEFAULT_BLOCK_ENTITY_SCAN_RADIUS;
    public static final ForgeConfigSpec.IntValue MAX_SCAN_RADIUS;
    public static final ForgeConfigSpec.IntValue DEFAULT_PERF_SECONDS;
    public static final ForgeConfigSpec.IntValue MAX_PERF_SECONDS;
    public static final ForgeConfigSpec.IntValue DEFAULT_WORLDGEN_OBSERVATION_SECONDS;
    public static final ForgeConfigSpec.IntValue MAX_WORLDGEN_OBSERVATION_SECONDS;
    public static final ForgeConfigSpec.IntValue DEFAULT_FPS_TEST_SECONDS;
    public static final ForgeConfigSpec.IntValue MAX_FPS_TEST_SECONDS;
    public static final ForgeConfigSpec.BooleanValue ISSUE_BUNDLE_ENABLED;
    public static final ForgeConfigSpec.BooleanValue INCLUDE_LOG_SNIPPET_IN_ISSUE_BUNDLE;
    public static final ForgeConfigSpec.IntValue LOG_SNIPPET_LINE_LIMIT;
    public static final ForgeConfigSpec.BooleanValue ENABLE_RULE_CHECKS;
    public static final ForgeConfigSpec.ConfigValue<String> RULES_FILE_NAME;
    public static final ForgeConfigSpec.BooleanValue VERBOSE_CHAT_OUTPUT;
    public static final ForgeConfigSpec.BooleanValue SMART_DIAGNOSTICS_ENABLED;
    public static final ForgeConfigSpec.BooleanValue ADAPTIVE_BASELINE_ENABLED;
    public static final ForgeConfigSpec.IntValue MIN_BASELINE_SAMPLES;
    public static final ForgeConfigSpec.IntValue MAX_BASELINE_SAMPLES_STORED;
    public static final ForgeConfigSpec.IntValue SMART_ADVICE_MAX_ITEMS;
    public static final ForgeConfigSpec.ConfigValue<String> ANOMALY_SENSITIVITY;
    public static final ForgeConfigSpec.BooleanValue SMART_SCORE_USES_ADAPTIVE_THRESHOLDS;
    public static final ForgeConfigSpec.BooleanValue ENABLE_ADAPTIVE_SMART_AI;
    public static final ForgeConfigSpec.BooleanValue ENABLE_ADAPTIVE_AMBIENCE;
    public static final ForgeConfigSpec.BooleanValue ENABLE_ADAPTIVE_EVENTS;
    public static final ForgeConfigSpec.IntValue BASE_THREAT_GAIN;
    public static final ForgeConfigSpec.IntValue MAX_THREAT_LEVEL;
    public static final ForgeConfigSpec.IntValue THREAT_DECAY_RATE;
    public static final ForgeConfigSpec.IntValue EVENT_COOLDOWN_TICKS;
    public static final ForgeConfigSpec.IntValue AMBIENCE_COOLDOWN_TICKS;
    public static final ForgeConfigSpec.BooleanValue DEBUG_SMART_AI_LOGGING;
    public static final ForgeConfigSpec.BooleanValue ALLOW_HIGH_INTENSITY_EVENTS;
    public static final ForgeConfigSpec.BooleanValue REDUCE_INTENSITY_AFTER_DEATH;
    public static final ForgeConfigSpec.DoubleValue ADAPTIVE_DIFFICULTY_MULTIPLIER;
    public static final ForgeConfigSpec.IntValue GUI_STATUS_REFRESH_TICKS;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
        builder.push("diagnostics");
        REPORTS_ENABLED = builder
                .comment("Allow GradleMC to write local diagnostic reports.")
                .define("reportsEnabled", true);
        REPORT_DIRECTORY_NAME = builder
                .comment("Report directory under the Minecraft config directory.")
                .define("reportDirectoryName", "gradlemc/reports");
        MAX_REPORT_FILES_LISTED = builder
                .comment("Maximum recent report files shown in chat.")
                .defineInRange("maxReportFilesListed", 10, 1, 50);
        DEFAULT_ENTITY_SCAN_RADIUS = builder
                .comment("Default radius for nearby entity scans.")
                .defineInRange("defaultEntityScanRadius", 64, 8, 256);
        DEFAULT_BLOCK_ENTITY_SCAN_RADIUS = builder
                .comment("Default radius for nearby block entity scans.")
                .defineInRange("defaultBlockEntityScanRadius", 64, 8, 256);
        MAX_SCAN_RADIUS = builder
                .comment("Maximum accepted radius for bounded scan commands.")
                .defineInRange("maxScanRadius", 256, 32, 512);
        DEFAULT_PERF_SECONDS = builder
                .comment("Default duration for future bounded performance sampling.")
                .defineInRange("defaultPerfSeconds", 60, 5, 600);
        MAX_PERF_SECONDS = builder
                .comment("Maximum accepted duration for performance commands.")
                .defineInRange("maxPerfSeconds", 600, 30, 1800);
        DEFAULT_WORLDGEN_OBSERVATION_SECONDS = builder
                .comment("Default duration for lightweight worldgen/chunk observation.")
                .defineInRange("defaultWorldgenObservationSeconds", 120, 10, 900);
        MAX_WORLDGEN_OBSERVATION_SECONDS = builder
                .comment("Maximum accepted duration for lightweight worldgen/chunk observation.")
                .defineInRange("maxWorldgenObservationSeconds", 900, 30, 1800);
        DEFAULT_FPS_TEST_SECONDS = builder
                .comment("Default duration for client-only FPS tests.")
                .defineInRange("defaultFpsTestSeconds", 60, 5, 600);
        MAX_FPS_TEST_SECONDS = builder
                .comment("Maximum accepted duration for client-only FPS tests.")
                .defineInRange("maxFpsTestSeconds", 600, 30, 1800);
        ISSUE_BUNDLE_ENABLED = builder
                .comment("Allow GradleMC to create safe issue bundle ZIP files.")
                .define("issueBundleEnabled", true);
        INCLUDE_LOG_SNIPPET_IN_ISSUE_BUNDLE = builder
                .comment("Include a redacted latest.log tail snippet in issue bundles. Full logs are never included.")
                .define("includeLogSnippetInIssueBundle", false);
        LOG_SNIPPET_LINE_LIMIT = builder
                .comment("Maximum latest.log tail lines included when log snippets are enabled.")
                .defineInRange("logSnippetLineLimit", 200, 1, 1000);
        ENABLE_RULE_CHECKS = builder
                .comment("Run local GradleMC risk rules as part of quick checks and exports.")
                .define("enableRuleChecks", true);
        RULES_FILE_NAME = builder
                .comment("Risk rule file name inside config/gradlemc.")
                .define("rulesFileName", "gradlemc-rules.json");
        VERBOSE_CHAT_OUTPUT = builder
                .comment("Show slightly more diagnostic detail in chat. Reports still contain the longer output.")
                .define("verboseChatOutput", false);
        SMART_DIAGNOSTICS_ENABLED = builder
                .comment("Enable local smart diagnostics. This is rule-based/adaptive only; no cloud AI, LLMs, or telemetry.")
                .define("smartDiagnosticsEnabled", true);
        ADAPTIVE_BASELINE_ENABLED = builder
                .comment("Allow GradleMC to store small local aggregate baseline metrics under config/gradlemc.")
                .define("adaptiveBaselineEnabled", true);
        MIN_BASELINE_SAMPLES = builder
                .comment("Minimum samples before adaptive baseline comparisons are treated as stronger evidence.")
                .defineInRange("minBaselineSamples", 3, 1, 20);
        MAX_BASELINE_SAMPLES_STORED = builder
                .comment("Maximum weighted sample count retained per aggregate baseline metric.")
                .defineInRange("maxBaselineSamplesStored", 20, 5, 100);
        SMART_ADVICE_MAX_ITEMS = builder
                .comment("Maximum smart recommendations shown in chat.")
                .defineInRange("smartAdviceMaxItems", 5, 1, 10);
        ANOMALY_SENSITIVITY = builder
                .comment("Anomaly sensitivity. Allowed values: LOW, NORMAL, HIGH.")
                .defineInList("anomalySensitivity", "NORMAL", java.util.List.of("LOW", "NORMAL", "HIGH"));
        SMART_SCORE_USES_ADAPTIVE_THRESHOLDS = builder
                .comment("Use local adaptive baseline thresholds when enough samples exist.")
                .define("smartScoreUsesAdaptiveThresholds", true);
        builder.pop();

        builder.push("adaptiveSmartAI");
        ENABLE_ADAPTIVE_SMART_AI = builder
                .comment("Enable lightweight local adaptive diagnostics. No LLMs, cloud APIs, telemetry, or external AI services are used.")
                .define("enableAdaptiveSmartAI", true);
        ENABLE_ADAPTIVE_AMBIENCE = builder
                .comment("Allow adaptive diagnostics to queue occasional low-impact ambience messages/effects.")
                .define("enableAdaptiveAmbience", true);
        ENABLE_ADAPTIVE_EVENTS = builder
                .comment("Allow adaptive diagnostics to queue occasional bounded player-facing adaptive events.")
                .define("enableAdaptiveEvents", true);
        BASE_THREAT_GAIN = builder
                .comment("Base stability risk score before local behavior signals are applied.")
                .defineInRange("baseThreatGain", 8, 0, 50);
        MAX_THREAT_LEVEL = builder
                .comment("Maximum adaptive stability risk score.")
                .defineInRange("maxThreatLevel", 100, 25, 100);
        THREAT_DECAY_RATE = builder
                .comment("Reserved decay tuning value for future persisted adaptive data. Runtime signals currently decay in bounded steps.")
                .defineInRange("threatDecayRate", 2, 0, 20);
        EVENT_COOLDOWN_TICKS = builder
                .comment("Base cooldown between adaptive events for a player, in ticks.")
                .defineInRange("eventCooldownTicks", 2400, 200, 24000);
        AMBIENCE_COOLDOWN_TICKS = builder
                .comment("Base cooldown between adaptive ambience responses for a player, in ticks.")
                .defineInRange("ambienceCooldownTicks", 1200, 100, 12000);
        DEBUG_SMART_AI_LOGGING = builder
                .comment("Log sampled adaptive diagnostics state transitions. Keep false unless debugging.")
                .define("debugSmartAILogging", false);
        ALLOW_HIGH_INTENSITY_EVENTS = builder
                .comment("Allow HIGH/EXTREME adaptive event responses. When false, only low/medium responses can trigger.")
                .define("allowHighIntensityEvents", false);
        REDUCE_INTENSITY_AFTER_DEATH = builder
                .comment("Reduce adaptive intensity after a player death to avoid repeated pressure.")
                .define("reduceIntensityAfterDeath", true);
        ADAPTIVE_DIFFICULTY_MULTIPLIER = builder
                .comment("Multiplier applied to base adaptive risk contribution.")
                .defineInRange("adaptiveDifficultyMultiplier", 1.0D, 0.1D, 5.0D);
        GUI_STATUS_REFRESH_TICKS = builder
                .comment("Minimum interval for the GradleMC GUI to request fresh adaptive diagnostics status while open, in ticks.")
                .defineInRange("guiStatusRefreshTicks", 100, 20, 1200);
        builder.pop();
        SPEC = builder.build();
    }

    private GradleMCConfig() {
    }
}
