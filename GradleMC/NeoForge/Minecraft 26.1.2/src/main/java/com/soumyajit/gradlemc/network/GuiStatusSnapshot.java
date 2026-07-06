package com.soumyajit.gradlemc.network;

import com.soumyajit.gradlemc.metrics.DiagnosticTestProgress;
import com.soumyajit.gradlemc.metrics.PerformanceTestManager;
import com.soumyajit.gradlemc.metrics.WorldgenObservationManager;
import com.soumyajit.gradlemc.smart.StabilityAdvisor;
import com.soumyajit.gradlemc.smart.StabilityScore;
import com.soumyajit.gradlemc.util.GradleMcPaths;
import net.minecraft.server.MinecraftServer;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

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

    public static GuiStatusSnapshot empty() {
        return EMPTY;
    }

    public static GuiStatusSnapshot capture(MinecraftServer server) {
        StabilityScore score = StabilityAdvisor.evaluate(server, List.of());
        Path latestReport = latestIn(List.of(GradleMcPaths.reportDirectory(), GradleMcPaths.legacyReportDirectory()), "gradlemc-");
        Path latestExport = latestIn(List.of(GradleMcPaths.exportDirectory()), "gradlemc-");
        Path latestIssueBundle = latestIn(List.of(GradleMcPaths.issueBundleDirectory()), "gradlemc-");
        Path latestProfile = latestIn(List.of(GradleMcPaths.profileDirectory()), "gradlemc-profile-");
        return new GuiStatusSnapshot(
                score.score(),
                score.riskLevel().name(),
                score.confidence().name(),
                PerformanceTestManager.progress(),
                WorldgenObservationManager.progress(),
                display(latestReport),
                latestSummary(latestReport),
                display(PerformanceTestManager.latestReportPath()),
                display(WorldgenObservationManager.latestReportPath()),
                display(latestExport),
                display(latestIssueBundle),
                display(latestProfile),
                latestSummary(latestProfile)
        );
    }

    private static Path latestIn(List<Path> directories, String prefix) {
        return directories.stream()
                .filter(Files::isDirectory)
                .flatMap(GuiStatusSnapshot::safeList)
                .filter(Files::isRegularFile)
                .filter(path -> path.getFileName().toString().startsWith(prefix))
                .max(Comparator.comparing(GuiStatusSnapshot::modifiedTimeSafe))
                .orElse(null);
    }

    private static Stream<Path> safeList(Path directory) {
        try {
            return Files.list(directory);
        } catch (IOException exception) {
            return Stream.empty();
        }
    }

    private static long modifiedTimeSafe(Path path) {
        try {
            return Files.getLastModifiedTime(path).toMillis();
        } catch (IOException exception) {
            return 0L;
        }
    }

    private static String latestSummary(Path path) {
        if (path == null) {
            return "";
        }
        try (Stream<String> lines = Files.lines(path)) {
            return lines.filter(line -> line.startsWith("Summary:")
                            || line.startsWith("Stability Score:")
                            || line.startsWith("Technical Stability Score:")
                            || line.startsWith("Average FPS:")
                            || line.startsWith("Approximate TPS:")
                            || line.startsWith("Loaded chunks start/end:")
                            || line.startsWith("End reason:")
                            || line.startsWith("Mode:")
                            || line.startsWith("Max MSPT:")
                            || line.startsWith("Slow ticks:"))
                    .findFirst()
                    .orElse("");
        } catch (IOException exception) {
            return "";
        }
    }

    private static String display(Path path) {
        return path == null ? "" : GradleMcPaths.displayPath(path);
    }
}
