package com.soumyajit.gradlemc.smart;

import java.util.List;

public record StabilityScore(
        int score,
        RiskLevel riskLevel,
        ConfidenceLevel confidence,
        List<DiagnosticFinding> findings,
        List<SmartRecommendation> recommendations,
        List<String> missingDataNotes,
        List<String> trendNotes,
        AdaptiveBaseline baseline
) {
    public String summaryLine() {
        return "Technical Stability Score: " + score + "/100 - " + riskLevel + " (confidence " + confidence + ")";
    }
}
