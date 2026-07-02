package com.soumyajit.gradlemc.ai;

public record SmartAIStatus(
        boolean adaptiveSmartAIEnabled,
        boolean adaptiveAmbienceEnabled,
        boolean adaptiveEventsEnabled,
        int threatScore,
        ThreatLevel threatLevel,
        String recentAdaptation,
        int ticksUntilNextEvent,
        int ticksUntilNextAmbience,
        int darknessTicks,
        int undergroundTicks,
        int ticksSinceSleep,
        int movementPressure,
        int recentDamageTaken,
        int recentMobKills,
        int recentDeaths,
        int nearbyHostileMobs,
        int healthPercent,
        int foodLevel,
        String topRiskFactors
) {
    private static final int MAX_STATUS_VALUE = 100;
    private static final int MAX_TICKS_VALUE = 24_000;

    public SmartAIStatus {
        threatScore = clamp(threatScore, 0, MAX_STATUS_VALUE);
        threatLevel = ThreatLevel.fromScore(threatScore);
        recentAdaptation = recentAdaptation == null ? "" : recentAdaptation;
        ticksUntilNextEvent = clamp(ticksUntilNextEvent, 0, MAX_TICKS_VALUE);
        ticksUntilNextAmbience = clamp(ticksUntilNextAmbience, 0, MAX_TICKS_VALUE);
        darknessTicks = clamp(darknessTicks, 0, MAX_TICKS_VALUE);
        undergroundTicks = clamp(undergroundTicks, 0, MAX_TICKS_VALUE);
        ticksSinceSleep = clamp(ticksSinceSleep, 0, MAX_TICKS_VALUE);
        movementPressure = clamp(movementPressure, 0, MAX_STATUS_VALUE);
        recentDamageTaken = clamp(recentDamageTaken, 0, MAX_STATUS_VALUE);
        recentMobKills = clamp(recentMobKills, 0, MAX_STATUS_VALUE);
        recentDeaths = clamp(recentDeaths, 0, MAX_STATUS_VALUE);
        nearbyHostileMobs = clamp(nearbyHostileMobs, 0, MAX_STATUS_VALUE);
        healthPercent = clamp(healthPercent, 0, MAX_STATUS_VALUE);
        foodLevel = clamp(foodLevel, 0, 20);
        topRiskFactors = topRiskFactors == null || topRiskFactors.isBlank() ? "no active pressure signals" : topRiskFactors;
    }

    public static SmartAIStatus disabled() {
        return new SmartAIStatus(false, false, false, 0, ThreatLevel.LOW,
                "Adaptive diagnostics are disabled", 0, 0, 0, 0, 0, 0, 0, 0, 0,
                0, 100, 20, "adaptive diagnostics disabled");
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}
