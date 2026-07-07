package com.soumyajit.gradlemc.report;

import com.soumyajit.gradlemc.metrics.FpsTestResult;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class FpsTestReportWriter {
    public Path write(FpsTestResult result, Path reportDirectory) throws IOException {
        Files.createDirectories(reportDirectory);
        Path reportFile = ReportFileNames.unique(reportDirectory, "gradlemc-fps-test-", result.endedAt(), ".txt");
        Files.write(reportFile, linesFor(result), StandardCharsets.UTF_8);
        return reportFile;
    }

    private List<String> linesFor(FpsTestResult result) {
        List<String> lines = new ArrayList<>();
        lines.add("GradleMC FPS Test Report");
        lines.add("========================");
        lines.add("Started: " + ReportFileNames.DISPLAY_TIMESTAMP.format(result.startedAt()));
        lines.add("Ended: " + ReportFileNames.DISPLAY_TIMESTAMP.format(result.endedAt()));
        lines.add("End reason: " + result.endReason());
        lines.add("");
        lines.add("Environment");
        lines.add("-----------");
        lines.addAll(ReportEnvironment.lines());
        lines.add("");
        lines.add("Client Smoothness Summary");
        lines.add("-------------------------");
        lines.add("Requested duration: " + result.requestedSeconds() + " seconds");
        lines.add("Actual duration: " + String.format("%.2f", result.actualSeconds()) + " seconds");
        lines.add("Samples: " + result.samples());
        lines.add("Average FPS: " + String.format("%.1f", result.averageFps()));
        lines.add("Minimum FPS: " + result.minFps());
        lines.add("Maximum FPS: " + result.maxFps());
        lines.add("1% low FPS: " + result.onePercentLowFps()
                .stream()
                .mapToObj(value -> String.format("%.1f", value))
                .findFirst()
                .orElse("not available"));
        lines.add("");
        lines.add("Notes");
        lines.add("-----");
        lines.add("FPS is sampled on the physical Minecraft client once per client tick.");
        lines.add("The 1% low value is an approximation from the lowest 1% of collected samples.");
        lines.add("");
        lines.add("Interpretation");
        lines.add("--------------");
        lines.add("Higher average FPS and 1% low values indicate smoother client performance.");
        if (result.onePercentLowFps().isPresent() && result.averageFps() > 0.0D
                && result.onePercentLowFps().getAsDouble() < result.averageFps() * 0.60D) {
            lines.add("The 1% low is much lower than the average, which suggests client-side stutter or intermittent frame drops.");
        }
        lines.add("Short samples are useful context, not benchmark certification.");
        lines.add("FPS evidence is a client smoothness signal; it is not server TPS/MSPT evidence.");
        return lines;
    }
}
