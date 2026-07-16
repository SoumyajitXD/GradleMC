package com.soumyajit.gradlemc.incident;

import java.time.Instant;
import java.util.Map;

public record IncidentSignal(Instant timestamp, String kind, Map<String, Double> metrics) {
    public IncidentSignal { kind = kind == null ? "unknown" : kind; metrics = Map.copyOf(metrics); }
}
