package com.soumyajit.gradlemc.smart;

import java.util.List;

/** Local deterministic heuristic result. It records observations and uncertainty rather than asserting mod causality. */
public record DiagnosticFinding(String id, String title, AnomalySeverity severity, ConfidenceLevel confidence,
                                String category, String interpretation, String likelyImpact, String nextInvestigation,
                                List<String> missingEvidence, List<DiagnosticEvidence> evidence, List<String> supportingEvidenceIds,
                                String ruleVersion) {
    private static final String RULE_VERSION = "local-deterministic-v1";

    public DiagnosticFinding {
        id = id == null || id.isBlank() ? "finding." + safe(title).replaceAll("[^a-zA-Z0-9_.-]", "_").toLowerCase(java.util.Locale.ROOT) : id;
        title = safe(title); severity = severity == null ? AnomalySeverity.NONE : severity; confidence = confidence == null ? ConfidenceLevel.LOW : confidence;
        category = category == null || category.isBlank() ? "stability" : category; interpretation = safe(interpretation); likelyImpact = safe(likelyImpact); nextInvestigation = safe(nextInvestigation);
        missingEvidence = List.copyOf(missingEvidence == null ? List.of() : missingEvidence); evidence = List.copyOf(evidence == null ? List.of() : evidence);
        supportingEvidenceIds = List.copyOf(supportingEvidenceIds == null ? evidence.stream().map(DiagnosticEvidence::id).toList() : supportingEvidenceIds);
        ruleVersion = ruleVersion == null || ruleVersion.isBlank() ? RULE_VERSION : ruleVersion;
    }
    public DiagnosticFinding(String title, AnomalySeverity severity, ConfidenceLevel confidence, List<DiagnosticEvidence> evidence) {
        this(null, title, severity, confidence, "stability", "Local rule matched the recorded observation.", "May warrant investigation; correlation is not proof of cause.", "Collect a comparable bounded follow-up sample.", List.of(), evidence, null, RULE_VERSION);
    }
    private static String safe(String value) { return value == null ? "" : value; }
}
