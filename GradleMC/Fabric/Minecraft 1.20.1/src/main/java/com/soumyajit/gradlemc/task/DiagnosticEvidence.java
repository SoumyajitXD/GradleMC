package com.soumyajit.gradlemc.task;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;

/** Plain immutable observation. Interpretations and recommendations deliberately live elsewhere. */
public record DiagnosticEvidence(
        String id, EvidenceType type, String sourceTaskId, Instant observedAt, String scope,
        Map<String, String> values, String unit, EvidenceAvailability availability,
        String confidence, String limitation, String provenance
) {
    public DiagnosticEvidence {
        if (id == null || !id.matches("[a-z][a-z0-9_.-]*")) throw new IllegalArgumentException("Invalid evidence id: " + id);
        type = Objects.requireNonNull(type, "type");
        if (sourceTaskId == null || sourceTaskId.isBlank()) throw new IllegalArgumentException("Evidence source is required");
        observedAt = Objects.requireNonNull(observedAt, "observedAt");
        scope = scope == null ? "process" : scope;
        values = Map.copyOf(Objects.requireNonNull(values, "values"));
        unit = unit == null ? "" : unit;
        availability = Objects.requireNonNull(availability, "availability");
        confidence = confidence == null ? "medium" : confidence;
        limitation = limitation == null ? "" : limitation;
        provenance = provenance == null ? "collector:" + sourceTaskId : provenance;
    }
}
