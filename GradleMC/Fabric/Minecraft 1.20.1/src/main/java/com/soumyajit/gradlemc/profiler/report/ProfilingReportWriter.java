package com.soumyajit.gradlemc.profiler.report;

import com.soumyajit.gradlemc.profiler.ProfilerSessionConfig;
import com.soumyajit.gradlemc.profiler.sampling.StackTraceAggregator;
import com.soumyajit.gradlemc.profiler.tick.TickRecord;
import com.soumyajit.gradlemc.report.ReportFileNames;
import com.soumyajit.gradlemc.util.AtomicFiles;
import com.soumyajit.gradlemc.util.GradleMcPaths;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;

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
        AtomicFiles.writeUtf8(text, String.join(System.lineSeparator(), textLines(summary, config, directory)) + System.lineSeparator());
        AtomicFiles.writeUtf8(json, json(summary, config));
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
        lines.add("Reports directory: " + GradleMcPaths.displayPath(directory));
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
        JsonObject root = new JsonObject(); root.addProperty("format", "gradlemc-profile-v1"); root.addProperty("mode", config.mode().id()); root.addProperty("durationSeconds", summary.duration().toSeconds()); root.addProperty("sampleIntervalMillis", config.intervalMillis()); root.addProperty("threadPattern", config.threadPattern()); root.addProperty("slowTickThresholdMillis", finite(config.onlyTicksOverMillis())); root.addProperty("startedAt", summary.startedAt().toString()); root.addProperty("endedAt", summary.endedAt().toString());
        JsonObject environment = new JsonObject(); environment.addProperty("gradlemc", summary.gradleMcVersion()); environment.addProperty("minecraft", summary.minecraftVersion()); environment.addProperty("loader", summary.loaderVersion()); environment.addProperty("java", summary.javaVersion()); environment.addProperty("mods", summary.loadedModCount()); root.add("environment", environment);
        JsonObject ticks = new JsonObject(); ticks.addProperty("samples", summary.tickSummary().sampleCount()); ticks.addProperty("averageMspt", finite(summary.tickSummary().averageMspt())); ticks.addProperty("medianMspt", finite(summary.tickSummary().medianMspt())); ticks.addProperty("p95Mspt", finite(summary.tickSummary().p95Mspt())); ticks.addProperty("p99Mspt", finite(summary.tickSummary().p99Mspt())); ticks.addProperty("maxMspt", finite(summary.tickSummary().maxMspt())); ticks.addProperty("slowTicks", summary.tickSummary().slowTickCount()); root.add("tickSummary", ticks);
        root.addProperty("cpuLiteSamples", summary.cpuSamples()); root.addProperty("interpretation", summary.interpretation());
        return new GsonBuilder().setPrettyPrinting().create().toJson(root) + System.lineSeparator();
    }

    private static Double finite(double value) { return Double.isFinite(value) ? value : null; }

    private static String format(double value) {
        return String.format(Locale.ROOT, "%.2f", Double.isFinite(value) ? value : 0.0D);
    }

    public record Result(Path textPath, Path jsonPath) {
    }
}
