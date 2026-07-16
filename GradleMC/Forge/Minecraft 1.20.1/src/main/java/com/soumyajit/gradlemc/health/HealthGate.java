package com.soumyajit.gradlemc.health;

public record HealthGate(String id, String description, HealthGateKind kind, String evidenceKey,
                         double threshold, boolean enabled) {
    public HealthGate {
        if (id == null || !id.matches("[a-z0-9][a-z0-9_.-]{0,63}")) throw new IllegalArgumentException("Invalid health gate ID");
        description = description == null ? "" : description;
        evidenceKey = evidenceKey == null ? "" : evidenceKey;
    }
}
