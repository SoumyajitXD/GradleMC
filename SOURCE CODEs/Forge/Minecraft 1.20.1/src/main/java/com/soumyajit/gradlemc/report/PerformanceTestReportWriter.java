package com.soumyajit.gradlemc.report;

import com.soumyajit.gradlemc.metrics.PerformanceTestResult;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class PerformanceTestReportWriter {
    public Path write(PerformanceTestResult result, Path reportDirectory) throws IOException {
        Files.createDirectories(reportDirectory);
        Path reportFile = ReportFileNames.unique(reportDirectory, "gradlemc-perf-test-", result.endedAt(), ".txt");
        Files.write(reportFile, linesFor(result), StandardCharsets.UTF_8);
        return reportFile;
    }

    private List<String> linesFor(PerformanceTestResult result) {
        List<String> lines = new ArrayList<>();
        lines.add("GradleMC Performance Test Report");
        lines.add("================================");
        lines.add("Started: " + ReportFileNames.DISPLAY_TIMESTAMP.format(result.startedAt()));
        lines.add("Ended: " + ReportFileNames.DISPLAY_TIMESTAMP.format(result.endedAt()));
        lines.add("End reason: " + result.endReason());
        lines.add("Requested duration: " + result.requestedSeconds() + " seconds");
        lines.add("Actual duration: " + format(result.actualSeconds()) + " seconds");
        lines.add("Samples: " + result.sampleCount());
        lines.add("");
        lines.add("Server Tick Summary");
        lines.add("-------------------");
        lines.add("Approximate TPS: " + format(result.approximateTps()));
        lines.add("Average MSPT: " + format(result.averageTickMs()));
        lines.add("Minimum sampled MSPT: " + format(result.minTickMs()));
        lines.add("Maximum sampled MSPT: " + format(result.maxTickMs()));
        lines.add("Minimum tick interval: " + format(result.minTickIntervalMs()) + " ms");
        lines.add("Maximum tick interval: " + format(result.maxTickIntervalMs()) + " ms");
        lines.add("");
        lines.add("Runtime Snapshot");
        lines.add("----------------");
        lines.add("Memory used start/end: " + result.memoryStartMiB() + " MiB -> " + result.memoryEndMiB() + " MiB");
        lines.add("Players start/end: " + result.playersStart() + " -> " + result.playersEnd());
        lines.add("Worlds start/end: " + result.worldsStart() + " -> " + result.worldsEnd());
        lines.add("");
        lines.add("Notes");
        lines.add("-----");
        lines.add("TPS and MSPT are approximate, bounded samples from the running Minecraft server.");
        lines.add("This is a lightweight diagnostic window, not a profiler and not a Spark replacement.");
        return lines;
    }

    private String format(double value) {
        return String.format(Locale.ROOT, "%.2f", value);
    }
}
