package com.soumyajit.gradlemc.report;

import com.soumyajit.gradlemc.metrics.FpsTestResult;
import com.soumyajit.gradlemc.util.GradleMcPaths;
import com.soumyajit.gradlemc.util.ManagedPathSafety;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class FpsTestReportWriter {
    public Path write(FpsTestResult result, Path reportDirectory) throws IOException {
        ManagedPathSafety.ensureDirectory(GradleMcPaths.gameDirectory(), reportDirectory);
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
        lines.add("FPS uses completed active-gameplay frame intervals observed once from Forge's post-GUI render callback and System.nanoTime().");
        lines.add("Average FPS is completed rendered frames divided by summed valid frame-interval seconds; paused, menu, unfocused, and invalid-gap time is excluded.");
        lines.add("The 1% low value is calculated from the slowest 1% of the bounded collected frame intervals.");
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
