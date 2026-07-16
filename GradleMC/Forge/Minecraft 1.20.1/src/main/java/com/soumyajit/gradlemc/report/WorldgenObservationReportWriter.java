package com.soumyajit.gradlemc.report;

import com.soumyajit.gradlemc.metrics.WorldgenObservationResult;
import com.soumyajit.gradlemc.util.GradleMcPaths;
import com.soumyajit.gradlemc.util.ManagedPathSafety;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class WorldgenObservationReportWriter {
    public Path write(WorldgenObservationResult result, Path reportDirectory) throws IOException {
        ManagedPathSafety.ensureDirectory(GradleMcPaths.gameDirectory(), reportDirectory);
        Path reportFile = ReportFileNames.unique(reportDirectory, "gradlemc-worldgen-observation-", result.endedAt(), ".txt");
        Files.write(reportFile, linesFor(result), StandardCharsets.UTF_8);
        return reportFile;
    }

    private List<String> linesFor(WorldgenObservationResult result) {
        List<String> lines = new ArrayList<>();
        lines.add("GradleMC Worldgen Observation Report");
        lines.add("====================================");
        lines.add("Started: " + ReportFileNames.DISPLAY_TIMESTAMP.format(result.startedAt()));
        lines.add("Ended: " + ReportFileNames.DISPLAY_TIMESTAMP.format(result.endedAt()));
        lines.add("End reason: " + result.endReason());
        lines.add("");
        lines.add("Environment");
        lines.add("-----------");
        lines.addAll(ReportEnvironment.lines());
        lines.add("");
        lines.add("Session");
        lines.add("-------");
        lines.add("Requested duration: " + result.requestedSeconds() + " seconds");
        lines.add("Actual duration: " + format(result.actualSeconds()) + " seconds");
        lines.add("Samples: " + result.sampleCount());
        lines.add("");
        lines.add("Observed Dimensions");
        lines.add("-------------------");
        if (result.dimensions().isEmpty()) {
            lines.add("No dimensions observed.");
        } else {
            result.dimensions().forEach(dimension -> lines.add("- " + dimension));
        }
        lines.add("");
        lines.add("Chunk And Tick Summary");
        lines.add("----------------------");
        lines.add("Loaded chunks start/end: " + result.loadedChunksStart() + " -> " + result.loadedChunksEnd());
        lines.add("Loaded chunks min/max: " + result.loadedChunksMin() + " -> " + result.loadedChunksMax());
        lines.add("Average sampled MSPT: " + format(result.averageTickMs()));
        lines.add("Maximum sampled MSPT: " + format(result.maxTickMs()));
        lines.add("Memory used start/end: " + result.memoryStartMiB() + " MiB -> " + result.memoryEndMiB() + " MiB");
        lines.add("");
        lines.add("Player Location Snapshots");
        lines.add("-------------------------");
        if (result.playerSnapshots().isEmpty()) {
            lines.add("No player location snapshots were available.");
        } else {
            result.playerSnapshots().forEach(snapshot -> lines.add("- " + snapshot));
        }
        lines.add("");
        lines.add("Warnings");
        lines.add("--------");
        if (result.warnings().isEmpty()) {
            lines.add("No major chunk, tick, or memory spikes detected by this lightweight observation.");
        } else {
            result.warnings().forEach(warning -> lines.add("- " + warning));
        }
        lines.add("");
        lines.add("Notes");
        lines.add("-----");
        lines.add("Worldgen pressure is inferred from loaded chunk, tick-time, memory, dimension, and player movement changes.");
        lines.add("GradleMC does not force-generate chunks, teleport players, or scan unloaded chunks.");
        lines.add("");
        lines.add("Interpretation");
        lines.add("--------------");
        lines.add("Chunk count growth during movement can indicate active terrain loading or worldgen pressure.");
        lines.add("Normal chunk loading is not a problem by itself; warnings are based on bounded thresholds and short-sample context.");
        if (result.maxTickMs() >= 100.0D) {
            lines.add("The maximum MSPT spike is notable. Reproduce with a longer worldgen observation before blaming a specific mod.");
        } else if (result.maxTickMs() >= 70.0D) {
            lines.add("The maximum MSPT spike is moderate. Treat it as proportional context, not a confirmed worldgen failure.");
        }
        return lines;
    }

    private String format(double value) {
        return String.format(Locale.ROOT, "%.2f", value);
    }
}
