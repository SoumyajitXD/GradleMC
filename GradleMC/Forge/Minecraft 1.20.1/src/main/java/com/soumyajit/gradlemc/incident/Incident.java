package com.soumyajit.gradlemc.incident;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public record Incident(String id, String trigger, Instant timestamp, List<IncidentSignal> preWindow,
                       List<IncidentSignal> postWindow, Map<String, String> context, List<String> evidenceIds,
                       boolean truncated) {
    public Incident {
        preWindow = List.copyOf(preWindow); postWindow = List.copyOf(postWindow);
        context = Map.copyOf(context); evidenceIds = List.copyOf(evidenceIds);
    }
}
