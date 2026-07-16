package com.soumyajit.gradlemc.health;

import java.util.List;

public record HealthGateResult(String gateId, HealthGateState state, String explanation,
                               Double observedValue, double threshold, List<String> evidenceIds) {
    public HealthGateResult {
        explanation = explanation == null ? "" : explanation;
        evidenceIds = List.copyOf(evidenceIds);
    }
}
