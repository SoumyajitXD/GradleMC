package com.soumyajit.gradlemc.network;

import com.soumyajit.gradlemc.metrics.DiagnosticTestProgress;
import com.soumyajit.gradlemc.metrics.PerformanceTestManager;
import com.soumyajit.gradlemc.metrics.WorldgenObservationManager;
import com.soumyajit.gradlemc.util.GradleMcPaths;
import net.minecraft.server.MinecraftServer;
import java.nio.file.Path;

public record GuiStatusSnapshot(
        int technicalStabilityScore,
        String technicalRiskLevel,
        String technicalConfidence,
        DiagnosticTestProgress performanceProgress,
        DiagnosticTestProgress worldgenProgress,
        String latestReportPath,
        String latestReportSummary,
        String latestPerformanceReportPath,
        String latestWorldgenReportPath,
        String latestExportPath,
        String latestIssueBundlePath,
        String latestProfilePath,
        String latestProfileSummary
) {
    private static final int MAX_TEXT = 192;
    private static final GuiStatusSnapshot EMPTY = new GuiStatusSnapshot(
            -1,
            "",
            "",
            DiagnosticTestProgress.idle(),
            DiagnosticTestProgress.idle(),
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            ""
    );

    public GuiStatusSnapshot {
        technicalStabilityScore = technicalStabilityScore < 0 ? -1 : Math.min(100, technicalStabilityScore);
        technicalRiskLevel = bounded(technicalRiskLevel);
        technicalConfidence = bounded(technicalConfidence);
        performanceProgress = performanceProgress == null ? DiagnosticTestProgress.idle() : performanceProgress;
        worldgenProgress = worldgenProgress == null ? DiagnosticTestProgress.idle() : worldgenProgress;
        latestReportPath = bounded(latestReportPath);
        latestReportSummary = bounded(latestReportSummary);
        latestPerformanceReportPath = bounded(latestPerformanceReportPath);
        latestWorldgenReportPath = bounded(latestWorldgenReportPath);
        latestExportPath = bounded(latestExportPath);
        latestIssueBundlePath = bounded(latestIssueBundlePath);
        latestProfilePath = bounded(latestProfilePath);
        latestProfileSummary = bounded(latestProfileSummary);
    }

    public static GuiStatusSnapshot empty() {
        return EMPTY;
    }

    public static GuiStatusSnapshot capture(MinecraftServer server) {
        // Packet handling must remain cheap: never enumerate or parse files on the server thread.
        // Detailed scores and artifact indexes are populated by explicit diagnostic services instead.
        return new GuiStatusSnapshot(
                -1,
                "UNAVAILABLE",
                "LOW",
                PerformanceTestManager.progress(),
                WorldgenObservationManager.progress(),
                "",
                "",
                display(PerformanceTestManager.latestReportPath()),
                display(WorldgenObservationManager.latestReportPath()),
                "",
                "",
                "",
                ""
        );
    }

    private static String display(Path path) {
        return path == null ? "" : GradleMcPaths.displayPath(path);
    }
    private static String bounded(String value) { if (value == null) return ""; return value.length() <= MAX_TEXT ? value : value.substring(0, MAX_TEXT); }
}
