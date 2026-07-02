package com.soumyajit.gradlemc.smart;

public enum RiskLevel {
    EXCELLENT,
    GOOD,
    WATCH,
    RISKY,
    UNSTABLE,
    CRITICAL;

    public static RiskLevel fromScore(int score) {
        if (score >= 90) {
            return EXCELLENT;
        }
        if (score >= 80) {
            return GOOD;
        }
        if (score >= 65) {
            return WATCH;
        }
        if (score >= 45) {
            return RISKY;
        }
        if (score >= 25) {
            return UNSTABLE;
        }
        return CRITICAL;
    }
}
