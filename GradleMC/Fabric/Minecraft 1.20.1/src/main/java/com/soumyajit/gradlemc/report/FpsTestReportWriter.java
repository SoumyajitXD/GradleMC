package com.soumyajit.gradlemc.report;

import com.soumyajit.gradlemc.metrics.FpsTestResult;
import com.soumyajit.gradlemc.util.AtomicFiles;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class FpsTestReportWriter {
    public Path write(FpsTestResult result, Path reportDirectory) throws IOException {
        Path reportFile = ReportFileNames.unique(reportDirectory, "gradlemc-fps-test-", result.endedAt(), ".txt");
        AtomicFiles.writeUtf8(reportFile, String.join(System.lineSeparator(), linesFor(result)) + System.lineSeparator());
        Path jsonFile = reportFile.resolveSibling(reportFile.getFileName().toString().replaceFirst("\\.txt$", ".json"));
        AtomicFiles.writeUtf8(jsonFile, jsonFor(result));
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
        lines.add("Average FPS: " + fps(result.averageFps(), "%.1f"));
        lines.add("Minimum FPS: " + fps(result.minFps(), "%d"));
        lines.add("Maximum FPS: " + fps(result.maxFps(), "%d"));
        lines.add("1% low FPS: " + result.onePercentLowFps()
                .stream()
                .mapToObj(value -> String.format("%.1f", value))
                .findFirst()
                .orElse("not available"));
        lines.add("");
        lines.add("Notes");
        lines.add("-----");
        lines.add("FPS is calculated from completed client render-frame intervals using a monotonic clock.");
        lines.add("Paused, menu, loading, invalid, and over-one-second intervals are excluded from active gameplay samples.");
        lines.add("The 1% low is computed from the slowest completed frame durations, not rounded instantaneous FPS values.");
        lines.add("");
        lines.add("Interpretation");
        lines.add("--------------");
        lines.add("Higher average FPS and 1% low values indicate smoother client performance.");
        if (result.hasSamples() && result.onePercentLowFps().isPresent() && result.averageFps() > 0.0D
                && result.onePercentLowFps().getAsDouble() < result.averageFps() * 0.60D) {
            lines.add("The 1% low is much lower than the average, which suggests client-side stutter or intermittent frame drops.");
        }
        lines.add("Short samples are useful context, not benchmark certification.");
        lines.add("FPS evidence is a client smoothness signal; it is not server TPS/MSPT evidence.");
        return lines;
    }

    private static String fps(Number value, String format) {
        return value == null ? "unavailable" : String.format(java.util.Locale.ROOT, format, value);
    }

    /** JSON is a second representation of the immutable completed result, never a second FPS calculation. */
    static String jsonFor(FpsTestResult result) {
        return "{\n"
                + "  \"schema\": \"gradlemc.fps_test.v1\",\n"
                + "  \"loader\": \"Fabric\",\n"
                + "  \"startedAt\": \"" + result.startedAt() + "\",\n"
                + "  \"endedAt\": \"" + result.endedAt() + "\",\n"
                + "  \"endReason\": \"" + result.endReason() + "\",\n"
                + "  \"requestedSeconds\": " + result.requestedSeconds() + ",\n"
                + "  \"actualSeconds\": " + finiteNumber(result.actualSeconds()) + ",\n"
                + "  \"samples\": " + result.samples() + ",\n"
                + "  \"averageFps\": " + nullableNumber(result.averageFps()) + ",\n"
                + "  \"minimumFps\": " + nullableNumber(result.minFps()) + ",\n"
                + "  \"maximumFps\": " + nullableNumber(result.maxFps()) + ",\n"
                + "  \"onePercentLowFps\": " + (result.onePercentLowFps().isPresent()
                ? finiteNumber(result.onePercentLowFps().getAsDouble()) : "null") + "\n"
                + "}\n";
    }

    private static String nullableNumber(Number value) {
        return value == null ? "null" : finiteNumber(value.doubleValue());
    }

    private static String finiteNumber(double value) {
        return Double.isFinite(value) ? String.format(java.util.Locale.ROOT, "%.6f", value) : "null";
    }
}
