package com.soumyajit.gradlemc.smart;

import java.util.List;

public record SmartRecommendation(
        String title,
        String reason,
        String action,
        int priority,
        ConfidenceLevel confidence,
        List<DiagnosticEvidence> evidence
) {
}
