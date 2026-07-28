package com.soumyajit.gradlemc.network;

import com.soumyajit.gradlemc.metrics.DiagnosticTestProgress;
import com.soumyajit.gradlemc.metrics.PerformanceTestManager;
import com.soumyajit.gradlemc.metrics.WorldgenObservationManager;
import com.soumyajit.gradlemc.capability.ServerOperation;
import com.soumyajit.gradlemc.util.GradleMcPaths;
import net.minecraft.server.MinecraftServer;
import com.soumyajit.gradlemc.smart.StabilityAdvisor;
import com.soumyajit.gradlemc.smart.StabilityScore;
import java.nio.file.Path;
import java.util.Set;

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
        String latestProfileSummary,
        boolean administrativeAllowed,
        Set<ServerOperation> supportedOperations
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
            "",
            false,
            Set.of()
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
        supportedOperations = supportedOperations == null ? Set.of() : Set.copyOf(supportedOperations);
    }

    /** Compatibility constructor for local call sites that only provide display evidence. */
    public GuiStatusSnapshot(int technicalStabilityScore, String technicalRiskLevel, String technicalConfidence,
                             DiagnosticTestProgress performanceProgress, DiagnosticTestProgress worldgenProgress,
                             String latestReportPath, String latestReportSummary, String latestPerformanceReportPath,
                             String latestWorldgenReportPath, String latestExportPath, String latestIssueBundlePath,
                             String latestProfilePath, String latestProfileSummary) {
        this(technicalStabilityScore, technicalRiskLevel, technicalConfidence, performanceProgress, worldgenProgress,
                latestReportPath, latestReportSummary, latestPerformanceReportPath, latestWorldgenReportPath,
                latestExportPath, latestIssueBundlePath, latestProfilePath, latestProfileSummary, false, Set.of());
    }

    public static GuiStatusSnapshot empty() {
        return EMPTY;
    }

    public static GuiStatusSnapshot capture(MinecraftServer server) {
        return capture(server, false);
    }

    public static GuiStatusSnapshot capture(MinecraftServer server, boolean administrativeAllowed) {
        // This is intentionally a bounded, in-memory snapshot.  It supports both a
        // dedicated server and the logical integrated server without sharing mutable
        // state across sides.
        StabilityScore stability = StabilityAdvisor.evaluate(server, java.util.List.of());
        return new GuiStatusSnapshot(
                stability.score(),
                stability.riskLevel().name(),
                stability.confidence().name(),
                PerformanceTestManager.progress(),
                WorldgenObservationManager.progress(),
                newest(PerformanceTestManager.latestReportPath(), WorldgenObservationManager.latestReportPath()),
                stability.missingDataNotes().isEmpty() ? "Current bounded evidence" : "Partial evidence: " + stability.missingDataNotes().get(0),
                display(PerformanceTestManager.latestReportPath()),
                display(WorldgenObservationManager.latestReportPath()),
                "",
                "",
                "",
                "",
                administrativeAllowed,
                Set.of(ServerOperation.STATUS, ServerOperation.TPS_MSPT, ServerOperation.SERVER_PROFILER,
                        ServerOperation.TICK_PROFILER, ServerOperation.SERVER_MEMORY, ServerOperation.ENTITIES,
                        ServerOperation.BLOCK_ENTITIES, ServerOperation.WORLDGEN, ServerOperation.ADMIN_ACTIONS)
        );
    }

    private static String display(Path path) {
        return path == null ? "" : GradleMcPaths.displayPath(path);
    }
    private static String newest(Path first, Path second) {
        if (first == null) return display(second);
        if (second == null) return display(first);
        try { return java.nio.file.Files.getLastModifiedTime(first).compareTo(java.nio.file.Files.getLastModifiedTime(second)) >= 0 ? display(first) : display(second); }
        catch (java.io.IOException ignored) { return display(first); }
    }
    private static String bounded(String value) { if (value == null) return ""; return value.length() <= MAX_TEXT ? value : value.substring(0, MAX_TEXT); }
}
