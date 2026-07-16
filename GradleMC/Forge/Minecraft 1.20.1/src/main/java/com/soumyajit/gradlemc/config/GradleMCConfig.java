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
    public static final ForgeConfigSpec.BooleanValue MOD_AUDIT_ENABLED;
    public static final ForgeConfigSpec.BooleanValue MOD_AUDIT_INCLUDE_JAR_FILENAMES;
    public static final ForgeConfigSpec.IntValue MOD_AUDIT_MAX_CHAT_FINDINGS;
    public static final ForgeConfigSpec.ConfigValue<String> RULES_FILE_NAME;
    public static final ForgeConfigSpec.BooleanValue VERBOSE_CHAT_OUTPUT;
    public static final ForgeConfigSpec.BooleanValue SMART_DIAGNOSTICS_ENABLED;
    public static final ForgeConfigSpec.BooleanValue ADAPTIVE_BASELINE_ENABLED;
    public static final ForgeConfigSpec.IntValue MIN_BASELINE_SAMPLES;
    public static final ForgeConfigSpec.IntValue MAX_BASELINE_SAMPLES_STORED;
    public static final ForgeConfigSpec.IntValue SMART_ADVICE_MAX_ITEMS;
    public static final ForgeConfigSpec.ConfigValue<String> ANOMALY_SENSITIVITY;
    public static final ForgeConfigSpec.BooleanValue SMART_SCORE_USES_ADAPTIVE_THRESHOLDS;
    public static final ForgeConfigSpec.BooleanValue HEALTH_GATES_ENABLED;
    public static final ForgeConfigSpec.DoubleValue GATE_MIN_MEDIAN_FPS;
    public static final ForgeConfigSpec.DoubleValue GATE_MAX_P95_FRAME_TIME_MS;
    public static final ForgeConfigSpec.DoubleValue GATE_MIN_TPS;
    public static final ForgeConfigSpec.DoubleValue GATE_MAX_P95_MSPT;
    public static final ForgeConfigSpec.DoubleValue GATE_MAX_GC_PAUSE_MS;
    public static final ForgeConfigSpec.IntValue GATE_MAX_CRITICAL_FINDINGS;
    public static final ForgeConfigSpec.IntValue BUDGET_MAX_TASKS;
    public static final ForgeConfigSpec.IntValue BUDGET_MAX_RUN_SECONDS;
    public static final ForgeConfigSpec.IntValue BUDGET_MAX_FILES;
    public static final ForgeConfigSpec.IntValue BUDGET_MAX_READ_MIB;
    public static final ForgeConfigSpec.IntValue BUDGET_MAX_RECORDS;
    public static final ForgeConfigSpec.IntValue BUDGET_MAX_SCAN_MIB;
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
    public static final ForgeConfigSpec.BooleanValue OVERLAY_ENABLED;
    public static final ForgeConfigSpec.ConfigValue<String> OVERLAY_MODE;
    public static final ForgeConfigSpec.ConfigValue<String> OVERLAY_POSITION;
    public static final ForgeConfigSpec.DoubleValue OVERLAY_SCALE;
    public static final ForgeConfigSpec.BooleanValue OVERLAY_BACKGROUND_ENABLED;
    public static final ForgeConfigSpec.DoubleValue OVERLAY_BACKGROUND_OPACITY;
    public static final ForgeConfigSpec.ConfigValue<Integer> OVERLAY_SAMPLING_WINDOW_SECONDS;
    public static final ForgeConfigSpec.ConfigValue<Integer> OVERLAY_UPDATE_INTERVAL_MS;
    public static final ForgeConfigSpec.BooleanValue OVERLAY_SHOW_TITLE;
    public static final ForgeConfigSpec.BooleanValue OVERLAY_SHOW_FPS;
    public static final ForgeConfigSpec.BooleanValue OVERLAY_SHOW_AVERAGE_FPS;
    public static final ForgeConfigSpec.BooleanValue OVERLAY_SHOW_ONE_PERCENT_LOW;
    public static final ForgeConfigSpec.BooleanValue OVERLAY_SHOW_POINT_ONE_PERCENT_LOW;
    public static final ForgeConfigSpec.BooleanValue OVERLAY_SHOW_JVM_MEMORY;
    public static final ForgeConfigSpec.BooleanValue OVERLAY_SHOW_SYSTEM_MEMORY;
    public static final ForgeConfigSpec.BooleanValue OVERLAY_SHOW_CPU;
    public static final ForgeConfigSpec.BooleanValue OVERLAY_SHOW_GPU_NAME;
    public static final ForgeConfigSpec.BooleanValue OVERLAY_SHOW_GPU_USAGE;
    public static final ForgeConfigSpec.BooleanValue OVERLAY_SHOW_INTEGRATED_SERVER;
    public static final ForgeConfigSpec.BooleanValue OVERLAY_SHOW_TEST_STATUS;
    public static final ForgeConfigSpec.BooleanValue OVERLAY_SHOW_PROFILER_STATUS;
    public static final ForgeConfigSpec.BooleanValue OVERLAY_SHOW_STABILITY;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
        builder.push("diagnostics");
        REPORTS_ENABLED = builder
                .comment("Allow GradleMC to write local diagnostic reports.")
                .define("reportsEnabled", true);
        REPORT_DIRECTORY_NAME = builder
                .comment("Report directory name under the Minecraft game directory's gradlemc output root.")
                .define("reportDirectoryName", "reports");
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
        MOD_AUDIT_ENABLED = builder
                .comment("Enable local installed-mod metadata audits. No network, jar scanning, or telemetry is used.")
                .define("modAuditEnabled", true);
        MOD_AUDIT_INCLUDE_JAR_FILENAMES = builder
                .comment("Include JAR file names, never absolute paths, in local mod-audit reports.")
                .define("modAuditIncludeJarFilenames", true);
        MOD_AUDIT_MAX_CHAT_FINDINGS = builder
                .comment("Maximum actionable mod-audit findings shown in chat.")
                .defineInRange("modAuditMaxChatFindings", 5, 1, 20);
        RULES_FILE_NAME = builder
                .comment("Risk rule file name inside the Minecraft game directory's gradlemc/rules folder.")
                .define("rulesFileName", "gradlemc-rules.json");
        VERBOSE_CHAT_OUTPUT = builder
                .comment("Show slightly more diagnostic detail in chat. Reports still contain the longer output.")
                .define("verboseChatOutput", false);
        SMART_DIAGNOSTICS_ENABLED = builder
                .comment("Enable local smart diagnostics. This is rule-based/adaptive only; no cloud AI, LLMs, or telemetry.")
                .define("smartDiagnosticsEnabled", true);
        ADAPTIVE_BASELINE_ENABLED = builder
                .comment("Allow GradleMC to store small local aggregate baseline metrics under the Minecraft game directory's gradlemc folder.")
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
        HEALTH_GATES_ENABLED = builder
                .comment("Evaluate local GradleMC health gates. Missing evidence is INCONCLUSIVE, never PASS.")
                .define("healthGatesEnabled", true);
        GATE_MIN_MEDIAN_FPS = builder.comment("Minimum acceptable median FPS when FPS evidence exists.")
                .defineInRange("gateMinMedianFps", 60.0D, 1.0D, 1000.0D);
        GATE_MAX_P95_FRAME_TIME_MS = builder.comment("Maximum acceptable p95 frame time in milliseconds.")
                .defineInRange("gateMaxP95FrameTimeMs", 33.4D, 1.0D, 1000.0D);
        GATE_MIN_TPS = builder.comment("Minimum acceptable server TPS when server evidence exists.")
                .defineInRange("gateMinTps", 18.0D, 1.0D, 20.0D);
        GATE_MAX_P95_MSPT = builder.comment("Maximum acceptable p95 server tick time in milliseconds.")
                .defineInRange("gateMaxP95Mspt", 50.0D, 1.0D, 10000.0D);
        GATE_MAX_GC_PAUSE_MS = builder.comment("Maximum acceptable observed GC pause in milliseconds.")
                .defineInRange("gateMaxGcPauseMs", 200.0D, 1.0D, 60000.0D);
        GATE_MAX_CRITICAL_FINDINGS = builder.comment("Maximum acceptable critical finding count.")
                .defineInRange("gateMaxCriticalFindings", 0, 0, 1000);
        BUDGET_MAX_TASKS = builder.comment("Maximum tasks in one bounded diagnostic run.").defineInRange("budgetMaxTasks",128,8,512);
        BUDGET_MAX_RUN_SECONDS = builder.comment("Maximum wall duration for one diagnostic run.").defineInRange("budgetMaxRunSeconds",300,10,3600);
        BUDGET_MAX_FILES = builder.comment("Maximum files accounted across a diagnostic run.").defineInRange("budgetMaxFiles",10000,100,100000);
        BUDGET_MAX_READ_MIB = builder.comment("Maximum bytes read across providers, in MiB.").defineInRange("budgetMaxReadMiB",256,1,2048);
        BUDGET_MAX_RECORDS = builder.comment("Maximum retained records or samples in a run.").defineInRange("budgetMaxRecords",100000,1000,1000000);
        BUDGET_MAX_SCAN_MIB = builder.comment("Maximum GradleMC Scan output size in MiB.").defineInRange("budgetMaxScanMiB",16,1,64);
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
                .comment("Calm baseline adaptive risk before local behavior and danger signals are applied.")
                .defineInRange("baseThreatGain", 8, 0, 50);
        MAX_THREAT_LEVEL = builder
                .comment("Maximum adaptive player pressure/risk score.")
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

        builder.push("overlay");
        OVERLAY_ENABLED = builder
                .comment("Show the optional GradleMC in-game stats overlay. Disabled by default.")
                .define("overlayEnabled", OverlayDefaults.ENABLED);
        OVERLAY_MODE = builder
                .comment("Overlay mode. Allowed values: COMPACT, DETAILED.")
                .defineInList("overlayMode", OverlayDefaults.MODE, java.util.List.of("COMPACT", "DETAILED"));
        OVERLAY_POSITION = builder
                .comment("Overlay position. Allowed values: TOP_LEFT, TOP_RIGHT, BOTTOM_LEFT, BOTTOM_RIGHT.")
                .defineInList("overlayPosition", OverlayDefaults.POSITION, java.util.List.of("TOP_LEFT", "TOP_RIGHT", "BOTTOM_LEFT", "BOTTOM_RIGHT"));
        OVERLAY_SCALE = builder
                .comment("Overlay text scale.")
                .defineInRange("overlayScale", OverlayDefaults.SCALE, 0.75D, 2.0D);
        OVERLAY_BACKGROUND_ENABLED = builder
                .comment("Draw a semi-transparent overlay background.")
                .define("overlayBackgroundEnabled", OverlayDefaults.BACKGROUND);
        OVERLAY_BACKGROUND_OPACITY = builder
                .comment("Overlay background opacity from 0.0 to 1.0.")
                .defineInRange("overlayBackgroundOpacity", OverlayDefaults.OPACITY, 0.0D, 1.0D);
        OVERLAY_SAMPLING_WINDOW_SECONDS = builder
                .comment("Rolling FPS statistics window in seconds. Allowed values: 30, 60, 120.")
                .defineInList("overlaySamplingWindowSeconds", OverlayDefaults.SAMPLING_WINDOW_SECONDS, java.util.List.of(30, 60, 120));
        OVERLAY_UPDATE_INTERVAL_MS = builder
                .comment("Overlay display refresh interval in milliseconds. Allowed values: 250, 500, 1000.")
                .defineInList("overlayUpdateIntervalMs", OverlayDefaults.UPDATE_INTERVAL_MS, java.util.List.of(250, 500, 1000));
        OVERLAY_SHOW_TITLE = builder
                .comment("Displays the \"GradleMC\" heading above enabled overlay statistics.")
                .define("showOverlayTitle", OverlayDefaults.SHOW_TITLE);
        OVERLAY_SHOW_FPS = builder
                .comment("Show the recent current FPS value only.")
                .define("overlayShowFps", OverlayDefaults.SHOW_FPS);
        OVERLAY_SHOW_AVERAGE_FPS = builder
                .comment("Show average FPS across the configured rolling measurement window. Disabled by default.")
                .define("showAverageFps", OverlayDefaults.SHOW_AVERAGE_FPS);
        OVERLAY_SHOW_ONE_PERCENT_LOW = builder
                .comment("Show 1% low FPS after the rolling window has enough samples.")
                .define("overlayShowOnePercentLow", OverlayDefaults.SHOW_ONE_PERCENT_LOW);
        OVERLAY_SHOW_POINT_ONE_PERCENT_LOW = builder
                .comment("Show 0.1% low FPS after the rolling window has enough samples.")
                .define("overlayShowPointOnePercentLow", OverlayDefaults.SHOW_POINT_ONE_PERCENT_LOW);
        OVERLAY_SHOW_JVM_MEMORY = builder
                .comment("Show JVM heap memory usage.")
                .define("overlayShowJvmMemory", OverlayDefaults.SHOW_JVM_MEMORY);
        OVERLAY_SHOW_SYSTEM_MEMORY = builder
                .comment("Show system memory when Java exposes it safely.")
                .define("overlayShowSystemMemory", OverlayDefaults.SHOW_SYSTEM_MEMORY);
        OVERLAY_SHOW_CPU = builder
                .comment("Show process CPU usage and CPU name when Java exposes them safely.")
                .define("overlayShowCpu", OverlayDefaults.SHOW_CPU);
        OVERLAY_SHOW_GPU_NAME = builder
                .comment("Show OpenGL GPU renderer/vendor metadata on the client.")
                .define("overlayShowGpuName", OverlayDefaults.SHOW_GPU_NAME);
        OVERLAY_SHOW_GPU_USAGE = builder
                .comment("Reserved for accurate GPU usage providers. GradleMC does not fake GPU usage.")
                .define("overlayShowGpuUsage", OverlayDefaults.SHOW_GPU_USAGE);
        OVERLAY_SHOW_INTEGRATED_SERVER = builder
                .comment("Show integrated-server TPS/MSPT when playing single-player.")
                .define("overlayShowIntegratedServer", OverlayDefaults.SHOW_INTEGRATED_SERVER);
        OVERLAY_SHOW_TEST_STATUS = builder
                .comment("Show bounded diagnostic test progress.")
                .define("overlayShowTestStatus", OverlayDefaults.SHOW_TEST_STATUS);
        OVERLAY_SHOW_PROFILER_STATUS = builder
                .comment("Show local profiler status.")
                .define("overlayShowProfilerStatus", OverlayDefaults.SHOW_PROFILER_STATUS);
        OVERLAY_SHOW_STABILITY = builder
                .comment("Show the synchronized stability summary.")
                .define("overlayShowStability", OverlayDefaults.SHOW_STABILITY);
        builder.pop();
        SPEC = builder.build();
    }

    private GradleMCConfig() {
    }
}
