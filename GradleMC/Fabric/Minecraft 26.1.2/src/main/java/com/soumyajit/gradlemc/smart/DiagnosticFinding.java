package com.soumyajit.gradlemc.smart;

import java.util.List;

public record DiagnosticFinding(
        String title,
        AnomalySeverity severity,
        ConfidenceLevel confidence,
        List<DiagnosticEvidence> evidence
) {
}
