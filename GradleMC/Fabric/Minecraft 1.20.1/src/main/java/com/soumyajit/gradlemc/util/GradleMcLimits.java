package com.soumyajit.gradlemc.util;

/**
 * Regression budgets for GradleMC-owned work.  They deliberately describe this
 * mod's bounded state, not a promise about a particular Minecraft installation.
 */
public final class GradleMcLimits {
    public static final int MAX_WORKFLOW_HISTORY = 16;
    public static final int MAX_PENDING_REQUESTS = 32;
    public static final int MAX_REPORT_INDEX_ENTRIES = 16;
    public static final int MAX_ISSUE_BUNDLE_INPUTS = 12;
    public static final long MAX_REPORT_BYTES = 512L * 1024L;
    public static final long MAX_ISSUE_BUNDLE_BYTES = 4L * 1024L * 1024L;
    public static final long MAX_ISSUE_BUNDLE_ENTRY_BYTES = 512L * 1024L;
    public static final int MAX_EXECUTOR_QUEUE = 1;
    public static final int MAX_REPORT_NAME_COLLISION_ATTEMPTS = 1_024;
    public static final long MAX_CONFIG_FILE_BYTES = 512L * 1024L;
    public static final long MAX_RULES_FILE_BYTES = 512L * 1024L;
    public static final int MAX_RISK_RULES = 256;

    private GradleMcLimits() { }
}
