package com.soumyajit.gradlemc.profiler.report;

import com.soumyajit.gradlemc.profiler.ProfilerSessionConfig;
import com.soumyajit.gradlemc.profiler.sampling.StackTraceAggregator;
import com.soumyajit.gradlemc.profiler.tick.TickRecord;
import com.soumyajit.gradlemc.report.ReportFileNames;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class ProfilingReportWriter {
    public Result write(ProfilingSummary summary, ProfilerSessionConfig config, Path directory) throws IOException {
        Files.createDirectories(directory);
        Instant now = Instant.now();
        Path text = ReportFileNames.unique(directory, "gradlemc-profile-", now, ".txt");
        Path json = text.resolveSibling(text.getFileName().toString().replace(".txt", ".json"));
        Files.write(text, textLines(summary, config, directory), StandardCharsets.UTF_8);
        Files.writeString(json, json(summary, config), StandardCharsets.UTF_8);
        return new Result(text, json);
    }

    private static List<String> textLines(ProfilingSummary summary, ProfilerSessionConfig config, Path directory) {
        List<String> lines = new ArrayList<>();
        lines.add("GradleMC Profiling Summary");
        lines.add("==========================");
        lines.add("Summary: " + summary.interpretation());
        lines.add("Mode: " + config.mode().id());
        lines.add("Duration: " + summary.duration().toSeconds() + "s");
        lines.add("Sample interval: " + config.intervalMillis() + "ms");
        lines.add("Thread filter: " + config.threadPattern());
        lines.add("Slow tick threshold: " + format(config.onlyTicksOverMillis()) + "ms");
        lines.add("Include sleeping threads: " + config.includeSleeping());
        lines.add("");
        lines.add("Environment");
        lines.add("GradleMC: " + summary.gradleMcVersion());
        lines.add("Minecraft: " + summary.minecraftVersion());
        lines.add("Fabric Loader: " + summary.loaderVersion());
        lines.add("Java: " + summary.javaVersion() + " (" + summary.javaVendor() + ")");
        lines.add("OS: " + summary.os());
        lines.add("Loaded mods: " + summary.loadedModCount());
        lines.add("");
        lines.add("Tick Summary");
        lines.add("Samples: " + summary.tickSummary().sampleCount());
        lines.add("Average MSPT: " + format(summary.tickSummary().averageMspt()));
        lines.add("Median MSPT: " + format(summary.tickSummary().medianMspt()));
        lines.add("P95/P99 MSPT: " + format(summary.tickSummary().p95Mspt()) + " / " + format(summary.tickSummary().p99Mspt()));
        lines.add("Max MSPT: " + format(summary.tickSummary().maxMspt()));
        lines.add("Slow ticks: " + summary.tickSummary().slowTickCount());
        lines.add("");
        lines.add("Slowest Ticks");
        for (TickRecord tick : summary.slowestTicks()) {
            lines.add("- " + tick.timestamp() + " " + format(tick.durationMillis()) + "ms, players=" + tick.playerCount()
                    + ", dimensions=" + tick.dimensionCount() + ", chunks=" + tick.loadedChunkCount()
                    + ", entities=" + tick.entityCount() + ", GC +" + tick.gcCountDelta() + "/" + tick.gcTimeDeltaMillis() + "ms");
        }
        lines.add("");
        lines.add("CPU-lite Java Stack Sampling");
        lines.add("Samples: " + summary.cpuSamples());
        addCounts(lines, "Top threads", summary.topThreads());
        addCounts(lines, "Top leaf methods", summary.topLeaves());
        addCounts(lines, "Top packages", summary.topPackages());
        lines.add("");
        lines.add("Possible Contributors");
        for (ModAttribution attribution : summary.attributions()) {
            lines.add("- " + attribution.source() + " [" + attribution.confidence() + "] samples=" + attribution.samples()
                    + " evidence=" + attribution.reason());
        }
        lines.add("");
        lines.add("Memory/GC");
        lines.add("Heap start/end/max: " + summary.usedHeapStartMiB() + "/" + summary.usedHeapEndMiB() + "/" + summary.maxHeapMiB() + " MiB");
        lines.add("GC delta: " + summary.gcCountDelta() + " collections, " + summary.gcTimeDeltaMillis() + "ms");
        lines.add("");
        lines.add("What could not be proven");
        lines.add("- This is Java-level sampling, not async-profiler native CPU profiling.");
        lines.add("- Memory-lite reports pressure and GC correlation; it is not true allocation profiling.");
        lines.add("- Low-confidence attribution means possible contributor, not culprit.");
        lines.add("Reports directory: " + directory.normalize());
        return lines;
    }

    private static void addCounts(List<String> lines, String title, List<StackTraceAggregator.FrameCount> counts) {
        lines.add(title + ":");
        if (counts.isEmpty()) {
            lines.add("- none");
            return;
        }
        counts.stream().limit(10).forEach(count -> lines.add("- " + count.name() + ": " + count.count()));
    }

    private static String json(ProfilingSummary summary, ProfilerSessionConfig config) {
        return "{\n"
                + "  \"format\": \"gradlemc-profile-v1\",\n"
                + "  \"mode\": \"" + escape(config.mode().id()) + "\",\n"
                + "  \"durationSeconds\": " + summary.duration().toSeconds() + ",\n"
                + "  \"sampleIntervalMillis\": " + config.intervalMillis() + ",\n"
                + "  \"threadPattern\": \"" + escape(config.threadPattern()) + "\",\n"
                + "  \"slowTickThresholdMillis\": " + format(config.onlyTicksOverMillis()) + ",\n"
                + "  \"startedAt\": \"" + summary.startedAt() + "\",\n"
                + "  \"endedAt\": \"" + summary.endedAt() + "\",\n"
                + "  \"environment\": {\"gradlemc\": \"" + escape(summary.gradleMcVersion()) + "\", \"minecraft\": \"" + escape(summary.minecraftVersion())
                + "\", \"loader\": \"" + escape(summary.loaderVersion()) + "\", \"java\": \"" + escape(summary.javaVersion()) + "\", \"mods\": " + summary.loadedModCount() + "},\n"
                + "  \"tickSummary\": {\"samples\": " + summary.tickSummary().sampleCount()
                + ", \"averageMspt\": " + format(summary.tickSummary().averageMspt())
                + ", \"medianMspt\": " + format(summary.tickSummary().medianMspt())
                + ", \"p95Mspt\": " + format(summary.tickSummary().p95Mspt())
                + ", \"p99Mspt\": " + format(summary.tickSummary().p99Mspt())
                + ", \"maxMspt\": " + format(summary.tickSummary().maxMspt())
                + ", \"slowTicks\": " + summary.tickSummary().slowTickCount() + "},\n"
                + "  \"cpuLiteSamples\": " + summary.cpuSamples() + ",\n"
                + "  \"memory\": {\"heapStartMiB\": " + summary.usedHeapStartMiB() + ", \"heapEndMiB\": " + summary.usedHeapEndMiB()
                + ", \"heapMaxMiB\": " + summary.maxHeapMiB() + ", \"gcCountDelta\": " + summary.gcCountDelta()
                + ", \"gcTimeDeltaMillis\": " + summary.gcTimeDeltaMillis() + "},\n"
                + "  \"interpretation\": \"" + escape(summary.interpretation()) + "\"\n"
                + "}\n";
    }

    private static String escape(String value) {
        return value == null ? "" : value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private static String format(double value) {
        return String.format(Locale.ROOT, "%.2f", Double.isFinite(value) ? value : 0.0D);
    }

    public record Result(Path textPath, Path jsonPath) {
    }
}
