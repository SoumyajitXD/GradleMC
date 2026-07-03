package com.soumyajit.gradlemc.ai;

public enum ThreatLevel {
    LOW,
    MEDIUM,
    HIGH,
    EXTREME;

    public static ThreatLevel fromScore(int score) {
        if (score >= 85) {
            return EXTREME;
        }
        if (score >= 60) {
            return HIGH;
        }
        if (score >= 30) {
            return MEDIUM;
        }
        return LOW;
    }
}
