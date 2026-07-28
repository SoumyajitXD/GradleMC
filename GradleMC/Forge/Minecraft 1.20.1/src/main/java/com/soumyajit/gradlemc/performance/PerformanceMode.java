package com.soumyajit.gradlemc.performance;

import java.util.Locale;

/** Explicit diagnostic detail policy; it never changes Minecraft or third-party settings. */
public enum PerformanceMode {
    LOW_IMPACT(new PerformancePolicy("LOW_IMPACT", 1_000, 2_000, 2_000, 2_000, 2_000, 1,
            false, false, false, true, "REDUCED", "REDUCED", "CONSERVATIVE", 250, 2_000,
            "Low Impact coalesces duplicate optional refresh work; explicit foreground diagnostics remain available.")),
    BALANCED(new PerformancePolicy("BALANCED", 500, 1_000, 1_000, 1_000, 1_000, 1,
            false, false, false, false, "NORMAL", "NORMAL", "NORMAL", 250, 1_000, "")),
    DETAILED(new PerformancePolicy("DETAILED", 250, 500, 1_000, 1_000, 500, 1,
            false, true, true, false, "DETAILED", "DETAILED", "SENSITIVE", 250, 1_000,
            "Detailed mode increases active diagnostic detail; it does not add idle sampling."));

    private final PerformancePolicy policy;
    PerformanceMode(PerformancePolicy policy) { this.policy = policy; }
    public PerformancePolicy policy() { return policy; }

    public static PerformanceMode parse(String value) {
        try { return value == null ? BALANCED : valueOf(value.trim().toUpperCase(Locale.ROOT)); }
        catch (IllegalArgumentException ignored) { return BALANCED; }
    }
}
