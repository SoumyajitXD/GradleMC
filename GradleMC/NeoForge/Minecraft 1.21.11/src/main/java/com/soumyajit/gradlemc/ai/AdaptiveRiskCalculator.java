package com.soumyajit.gradlemc.ai;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public final class AdaptiveRiskCalculator {
    private AdaptiveRiskCalculator() {
    }

    public static RiskResult calculate(RiskSignals signals) {
        int maxRisk = clamp(signals.maxRisk(), 25, 100);
        double multiplier = Math.max(0.1D, signals.multiplier());
        List<Contribution> contributions = new ArrayList<>();
        int score = contribution(contributions, "calm baseline", Math.round(signals.baseRisk() * (float) multiplier));

        score += contribution(contributions, "darkness exposure", Math.min(18, signals.darknessTicks() / 160));
        score += contribution(contributions, "underground exposure", Math.min(14, signals.undergroundTicks() / 240));
        score += contribution(contributions, "recent damage", Math.min(24, signals.recentDamageTaken() * 2));
        score += contribution(contributions, "recent deaths", Math.min(28, signals.recentDeaths() * 18));
        score += contribution(contributions, "nearby hostile mobs", Math.min(18, signals.nearbyHostileMobs() * 4));
        score += contribution(contributions, "recent combat", Math.min(12, signals.recentMobKills() * 3));
        score += contribution(contributions, "movement pressure", Math.min(8, signals.movementPressure()));
        score += contribution(contributions, "low health", healthRisk(signals.healthPercent()));
        score += contribution(contributions, "low food", foodRisk(signals.foodLevel()));
        score += contribution(contributions, "nether dimension", signals.inNether() ? 8 : 0);
        score += contribution(contributions, "end dimension", signals.inEnd() ? 12 : 0);
        score += contribution(contributions, "long time since sleep", signals.ticksSinceSleep() > 18_000 ? 6 : 0);

        int boundedScore = clamp(score, 0, maxRisk);
        return new RiskResult(boundedScore, ThreatLevel.fromScore(boundedScore), topFactors(contributions));
    }

    private static int healthRisk(int healthPercent) {
        if (healthPercent <= 20) {
            return 22;
        }
        if (healthPercent <= 35) {
            return 14;
        }
        if (healthPercent <= 50) {
            return 8;
        }
        return 0;
    }

    private static int foodRisk(int foodLevel) {
        if (foodLevel <= 6) {
            return 10;
        }
        if (foodLevel <= 12) {
            return 5;
        }
        return 0;
    }

    private static int contribution(List<Contribution> contributions, String label, int amount) {
        int bounded = Math.max(0, amount);
        if (bounded > 0) {
            contributions.add(new Contribution(label, bounded));
        }
        return bounded;
    }

    private static String topFactors(List<Contribution> contributions) {
        return contributions.stream()
                .sorted(Comparator.comparingInt(Contribution::amount).reversed())
                .limit(4)
                .map(contribution -> contribution.label() + " +" + contribution.amount())
                .reduce((left, right) -> left + ", " + right)
                .orElse("no active pressure signals");
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    public record RiskSignals(
            int baseRisk,
            double multiplier,
            int maxRisk,
            int darknessTicks,
            int undergroundTicks,
            int recentDamageTaken,
            int recentMobKills,
            int recentDeaths,
            int nearbyHostileMobs,
            int healthPercent,
            int foodLevel,
            int movementPressure,
            boolean inNether,
            boolean inEnd,
            int ticksSinceSleep
    ) {
        public RiskSignals {
            baseRisk = clamp(baseRisk, 0, 50);
            maxRisk = clamp(maxRisk, 25, 100);
            darknessTicks = clamp(darknessTicks, 0, 24_000);
            undergroundTicks = clamp(undergroundTicks, 0, 24_000);
            recentDamageTaken = clamp(recentDamageTaken, 0, 100);
            recentMobKills = clamp(recentMobKills, 0, 100);
            recentDeaths = clamp(recentDeaths, 0, 100);
            nearbyHostileMobs = clamp(nearbyHostileMobs, 0, 100);
            healthPercent = clamp(healthPercent, 0, 100);
            foodLevel = clamp(foodLevel, 0, 20);
            movementPressure = clamp(movementPressure, 0, 100);
            ticksSinceSleep = clamp(ticksSinceSleep, 0, 24_000);
        }
    }

    public record RiskResult(int score, ThreatLevel level, String topFactors) {
        public String summary() {
            return String.format(Locale.ROOT, "Adaptive Risk: %s (%d/100)", level, score);
        }
    }

    private record Contribution(String label, int amount) {
    }
}
