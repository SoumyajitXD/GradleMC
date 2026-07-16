package com.soumyajit.gradlemc.experiment;

import java.time.Instant;
import java.util.Map;

public record ExperimentSnapshot(Instant capturedAt, String fingerprint, Map<String, String> context,
                                 Map<String, Double> metrics) {
    public ExperimentSnapshot {
        fingerprint = fingerprint == null ? "" : fingerprint;
        context = Map.copyOf(context);
        metrics = Map.copyOf(metrics);
    }
}
