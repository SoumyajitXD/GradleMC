package com.soumyajit.gradlemc.ai;

public final class AdaptiveRiskCalculatorSelfTest {
    private AdaptiveRiskCalculatorSelfTest() {
    }

    public static void run() {
        lowRiskScenarioStaysLow();
        mediumRiskScenarioBecomesMedium();
        highRiskScenarioBecomesHigh();
        repeatedDeathScenarioDoesNotFreezeAtBase();
        scoreRespondsToIncreasingSignals();
        stableServerSignalsAreNotAdaptiveRiskInputs();
        fpsCollapseIsClientSmoothnessContext();
        worldgenSpikeIsProportionalContext();
    }

    private static void lowRiskScenarioStaysLow() {
        AdaptiveRiskCalculator.RiskResult result = calculate(0, 0, 0, 0, 0, 0, 0, 100, 20, 0, false, false, 0);
        assertEquals(8, result.score(), "calm baseline should remain the configured low risk");
        assertEquals(ThreatLevel.LOW, result.level(), "calm baseline should be LOW");
    }

    private static void mediumRiskScenarioBecomesMedium() {
        AdaptiveRiskCalculator.RiskResult result = calculate(800, 480, 5, 1, 0, 2, 0, 55, 14, 2, false, false, 4_000);
        assertBetween(result.score(), 30, 59, "moderate danger signals should produce MEDIUM risk");
        assertEquals(ThreatLevel.MEDIUM, result.level(), "moderate danger signals should be MEDIUM");
    }

    private static void highRiskScenarioBecomesHigh() {
        AdaptiveRiskCalculator.RiskResult result = calculate(2_400, 2_400, 12, 2, 1, 6, 0, 18, 5, 5, true, false, 20_000);
        assertTrue(result.score() >= 60, "heavy danger signals should produce HIGH or EXTREME risk");
        assertTrue(result.level() == ThreatLevel.HIGH || result.level() == ThreatLevel.EXTREME,
                "heavy danger signals should not be LOW or MEDIUM");
    }

    private static void repeatedDeathScenarioDoesNotFreezeAtBase() {
        AdaptiveRiskCalculator.RiskResult result = calculate(0, 0, 0, 0, 3, 0, 0, 100, 20, 0, false, false, 0);
        assertTrue(result.score() > 8, "repeated deaths must move adaptive risk above the base value");
        assertTrue(result.topFactors().contains("recent deaths"), "death contribution should be visible in top factors");
    }

    private static void scoreRespondsToIncreasingSignals() {
        int calm = calculate(0, 0, 0, 0, 0, 0, 0, 100, 20, 0, false, false, 0).score();
        int danger = calculate(400, 0, 3, 0, 0, 2, 1, 70, 18, 0, false, false, 0).score();
        int severe = calculate(1_600, 1_200, 8, 1, 2, 5, 4, 25, 8, 0, true, false, 19_000).score();
        assertTrue(calm == 8, "calm scenario should document the configured base value");
        assertTrue(danger > calm, "danger signals must raise adaptive risk above the calm baseline");
        assertTrue(severe > danger, "severe stacked signals must score higher than moderate danger");
    }

    private static void stableServerSignalsAreNotAdaptiveRiskInputs() {
        AdaptiveRiskCalculator.RiskResult result = calculate(0, 0, 0, 0, 0, 0, 0, 100, 20, 0, false, false, 0);
        assertEquals(8, result.score(), "stable TPS/MSPT should not punish player pressure risk by itself");
    }

    private static void fpsCollapseIsClientSmoothnessContext() {
        AdaptiveRiskCalculator.RiskResult result = calculate(0, 0, 0, 0, 0, 0, 0, 100, 20, 0, false, false, 0);
        assertEquals(8, result.score(), "FPS collapse belongs to client smoothness reports, not adaptive player pressure risk");
    }

    private static void worldgenSpikeIsProportionalContext() {
        AdaptiveRiskCalculator.RiskResult result = calculate(0, 0, 0, 0, 0, 0, 0, 100, 20, 0, false, false, 0);
        assertEquals(8, result.score(), "worldgen tick spikes are technical context, not automatic player pressure risk");
    }

    private static AdaptiveRiskCalculator.RiskResult calculate(int darknessTicks, int undergroundTicks,
                                                               int recentDamageTaken, int recentMobKills,
                                                               int recentDeaths, int nearbyHostileMobs,
                                                               int movementPressure, int healthPercent,
                                                               int foodLevel, int unusedServerMspt,
                                                               boolean inNether, boolean inEnd,
                                                               int ticksSinceSleep) {
        return AdaptiveRiskCalculator.calculate(new AdaptiveRiskCalculator.RiskSignals(
                8,
                1.0D,
                100,
                darknessTicks,
                undergroundTicks,
                recentDamageTaken,
                recentMobKills,
                recentDeaths,
                nearbyHostileMobs,
                healthPercent,
                foodLevel,
                movementPressure,
                inNether,
                inEnd,
                ticksSinceSleep
        ));
    }

    private static void assertEquals(Object expected, Object actual, String message) {
        if (!expected.equals(actual)) {
            throw new AssertionError(message + " expected=" + expected + " actual=" + actual);
        }
    }

    private static void assertBetween(int actual, int minInclusive, int maxInclusive, String message) {
        if (actual < minInclusive || actual > maxInclusive) {
            throw new AssertionError(message + " actual=" + actual + " expectedRange=" + minInclusive + ".." + maxInclusive);
        }
    }

    private static void assertTrue(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
