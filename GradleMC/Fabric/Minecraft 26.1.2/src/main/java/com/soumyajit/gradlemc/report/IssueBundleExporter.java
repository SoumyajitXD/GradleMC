package com.soumyajit.gradlemc.report;

import com.soumyajit.gradlemc.GradleMC;
import com.soumyajit.gradlemc.config.GradleMCConfig;
import com.soumyajit.gradlemc.metrics.PerformanceTestManager;
import com.soumyajit.gradlemc.metrics.WorldgenObservationManager;
import com.soumyajit.gradlemc.rules.RiskRuleLoader;
import com.soumyajit.gradlemc.util.GradleMcPaths;
import com.soumyajit.gradlemc.util.RuntimeSnapshots;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.SharedConstants;
import net.minecraft.server.MinecraftServer;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class IssueBundleExporter {
    public Path create(MinecraftServer server) throws IOException {
        Path bundleDirectory = GradleMcPaths.issueBundleDirectory();
        Path reportDirectory = GradleMcPaths.reportDirectory();
        Files.createDirectories(bundleDirectory);
        Path bundle = ReportFileNames.unique(bundleDirectory, "gradlemc-issue-bundle-", Instant.now(), ".zip");
        try (OutputStream outputStream = Files.newOutputStream(bundle);
             ZipOutputStream zip = new ZipOutputStream(outputStream, StandardCharsets.UTF_8)) {
            addText(zip, "HOW_TO_REPORT.txt", howToReport());
            addText(zip, "environment-summary.txt", environmentSummary(server));
            addText(zip, "mod-list-summary.txt", modListSummary());
            addText(zip, "gradlemc-config-summary.txt", configSummary());
            addText(zip, "rule-check-summary.txt", ruleSummary());
            addText(zip, "latest-test-summaries.txt", latestTestSummaries());
            addLatestReports(zip, reportDirectory);
            if (GradleMCConfig.INCLUDE_LOG_SNIPPET_IN_ISSUE_BUNDLE.get()) {
                addLogSnippet(zip);
            } else {
                addText(zip, "log-snippet-skipped.txt",
                        "latest.log was not included because includeLogSnippetInIssueBundle=false.\n"
                                + "GradleMC never includes full logs by default.\n");
            }
        }
        return bundle;
    }

    private void addLatestReports(ZipOutputStream zip, Path reportDirectory) throws IOException {
        addLatestReport(zip, reportDirectory, "gradlemc-", "reports/latest-gradlemc-report.txt");
        addLatestReport(zip, reportDirectory, "gradlemc-fps-test-", "reports/latest-fps-test-report.txt");
        addLatestReport(zip, reportDirectory, "gradlemc-perf-test-", "reports/latest-performance-test-report.txt");
        addLatestReport(zip, reportDirectory, "gradlemc-worldgen-observation-", "reports/latest-worldgen-observation-report.txt");
    }

    private void addLatestReport(ZipOutputStream zip, Path reportDirectory, String prefix, String entryName) throws IOException {
        Optional<Path> latest = latestReport(reportDirectory, prefix);
        if (latest.isPresent() && Files.size(latest.get()) <= 512L * 1024L) {
            addFile(zip, entryName, latest.get());
        }
    }

    private Optional<Path> latestReport(Path reportDirectory, String prefix) throws IOException {
        if (!Files.isDirectory(reportDirectory)) {
            return Optional.empty();
        }
        try (Stream<Path> paths = Files.list(reportDirectory)) {
            return paths.filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().startsWith(prefix))
                    .filter(path -> path.getFileName().toString().endsWith(".txt"))
                    .filter(path -> prefix.equals("gradlemc-")
                            ? !path.getFileName().toString().startsWith("gradlemc-fps-test-")
                            && !path.getFileName().toString().startsWith("gradlemc-perf-test-")
                            && !path.getFileName().toString().startsWith("gradlemc-worldgen-observation-")
                            : true)
                    .max(Comparator.comparing(path -> {
                        try {
                            return Files.getLastModifiedTime(path).toMillis();
                        } catch (IOException exception) {
                            return 0L;
                        }
                    }));
        }
    }

    private void addLogSnippet(ZipOutputStream zip) throws IOException {
        Path gameDir = GradleMcPaths.gameDirectory();
        Path latestLog = gameDir.resolve("logs").resolve("latest.log").normalize();
        if (!latestLog.startsWith(gameDir) || !Files.isRegularFile(latestLog) || Files.size(latestLog) > 4L * 1024L * 1024L) {
            addText(zip, "log-snippet-skipped.txt", "latest.log was missing or too large for safe snippet export.\n");
            return;
        }
        int limit = GradleMCConfig.LOG_SNIPPET_LINE_LIMIT.get();
        List<String> lines = Files.readAllLines(latestLog, StandardCharsets.UTF_8);
        int from = Math.max(0, lines.size() - limit);
        StringBuilder builder = new StringBuilder();
        builder.append("Redacted tail of latest.log. Full logs are not included by GradleMC.\n\n");
        for (String line : lines.subList(from, lines.size())) {
            builder.append(redact(line)).append('\n');
        }
        addText(zip, "latest-log-tail-redacted.txt", builder.toString());
    }

    private String howToReport() {
        return """
                GradleMC Issue Bundle
                =====================
                Attach this ZIP to a GitHub issue or support request.

                Please include:
                - What you were doing when the issue happened.
                - Whether this was singleplayer, LAN, or dedicated server.
                - The command you ran before creating the bundle.
                - Any crash report or full log only if you reviewed it and are comfortable sharing it.

                This bundle intentionally excludes full logs, crash reports, the full config folder, the full mods folder, and private files.
                """;
    }

    private String environmentSummary(MinecraftServer server) {
        RuntimeSnapshots.MemorySnapshot memory = RuntimeSnapshots.memory();
        return "Product: " + GradleMC.PRODUCT_NAME + "\n"
                + "Version: " + modVersion() + "\n"
                + "Variant: " + GradleMC.CURRENT_DISPLAY_VARIANT + "\n"
                + "Minecraft: " + SharedConstants.getCurrentVersion().name() + "\n"
                + "Loader: " + GradleMC.CURRENT_LOADER_NAME + "\n"
                + "Fabric Loader: " + loaderVersion() + "\n"
                + "Java: " + System.getProperty("java.version", "unknown") + "\n"
                + "Output root: " + GradleMcPaths.gradleMcDirectory() + "\n"
                + "Loaded mods: " + FabricLoader.getInstance().getAllMods().size() + "\n"
                + "Players online: " + server.getPlayerCount() + "\n"
                + "Memory used/max: " + memory.usedMiB() + "/" + memory.maxMiB() + " MiB\n"
                + "Average server tick time: " + String.format(Locale.ROOT, "%.2f", server.getAverageTickTimeNanos() / 1_000_000.0D) + " ms\n";
    }

    private String modListSummary() {
        StringBuilder builder = new StringBuilder();
        builder.append("Loaded mods: ").append(FabricLoader.getInstance().getAllMods().size()).append("\n\n");
        FabricLoader.getInstance().getAllMods().stream()
                .sorted(Comparator.comparing(mod -> mod.getMetadata().getId()))
                .forEach(mod -> builder.append(mod.getMetadata().getId())
                        .append(" (").append(mod.getMetadata().getName()).append(") ")
                        .append(mod.getMetadata().getVersion().getFriendlyString()).append('\n'));
        return builder.toString();
    }

    private String configSummary() {
        return "reportsEnabled: " + GradleMCConfig.REPORTS_ENABLED.get() + "\n"
                + "reportDirectoryName: " + GradleMCConfig.REPORT_DIRECTORY_NAME.get() + "\n"
                + "outputRoot: " + GradleMcPaths.gradleMcDirectory() + "\n"
                + "reportDirectory: " + GradleMcPaths.reportDirectory() + "\n"
                + "exportDirectory: " + GradleMcPaths.exportDirectory() + "\n"
                + "issueBundleDirectory: " + GradleMcPaths.issueBundleDirectory() + "\n"
                + "rulesDirectory: " + GradleMcPaths.rulesDirectory() + "\n"
                + "defaultPerfSeconds: " + GradleMCConfig.DEFAULT_PERF_SECONDS.get() + "\n"
                + "maxPerfSeconds: " + GradleMCConfig.MAX_PERF_SECONDS.get() + "\n"
                + "defaultWorldgenObservationSeconds: " + GradleMCConfig.DEFAULT_WORLDGEN_OBSERVATION_SECONDS.get() + "\n"
                + "maxWorldgenObservationSeconds: " + GradleMCConfig.MAX_WORLDGEN_OBSERVATION_SECONDS.get() + "\n"
                + "issueBundleEnabled: " + GradleMCConfig.ISSUE_BUNDLE_ENABLED.get() + "\n"
                + "includeLogSnippetInIssueBundle: " + GradleMCConfig.INCLUDE_LOG_SNIPPET_IN_ISSUE_BUNDLE.get() + "\n"
                + "logSnippetLineLimit: " + GradleMCConfig.LOG_SNIPPET_LINE_LIMIT.get() + "\n"
                + "enableRuleChecks: " + GradleMCConfig.ENABLE_RULE_CHECKS.get() + "\n"
                + "smartDiagnosticsEnabled: " + GradleMCConfig.SMART_DIAGNOSTICS_ENABLED.get() + "\n"
                + "adaptiveBaselineEnabled: " + GradleMCConfig.ADAPTIVE_BASELINE_ENABLED.get() + "\n"
                + "minBaselineSamples: " + GradleMCConfig.MIN_BASELINE_SAMPLES.get() + "\n"
                + "maxBaselineSamplesStored: " + GradleMCConfig.MAX_BASELINE_SAMPLES_STORED.get() + "\n"
                + "smartAdviceMaxItems: " + GradleMCConfig.SMART_ADVICE_MAX_ITEMS.get() + "\n"
                + "anomalySensitivity: " + GradleMCConfig.ANOMALY_SENSITIVITY.get() + "\n"
                + "smartScoreUsesAdaptiveThresholds: " + GradleMCConfig.SMART_SCORE_USES_ADAPTIVE_THRESHOLDS.get() + "\n"
                + "enableAdaptiveSmartAI: " + GradleMCConfig.ENABLE_ADAPTIVE_SMART_AI.get() + "\n"
                + "enableAdaptiveAmbience: " + GradleMCConfig.ENABLE_ADAPTIVE_AMBIENCE.get() + "\n"
                + "enableAdaptiveEvents: " + GradleMCConfig.ENABLE_ADAPTIVE_EVENTS.get() + "\n"
                + "baseRiskScore (baseThreatGain): " + GradleMCConfig.BASE_THREAT_GAIN.get() + "\n"
                + "maxRiskScore (maxThreatLevel): " + GradleMCConfig.MAX_THREAT_LEVEL.get() + "\n"
                + "riskDecayRate (threatDecayRate): " + GradleMCConfig.THREAT_DECAY_RATE.get() + "\n"
                + "eventCooldownTicks: " + GradleMCConfig.EVENT_COOLDOWN_TICKS.get() + "\n"
                + "ambienceCooldownTicks: " + GradleMCConfig.AMBIENCE_COOLDOWN_TICKS.get() + "\n"
                + "debugSmartAILogging: " + GradleMCConfig.DEBUG_SMART_AI_LOGGING.get() + "\n"
                + "allowHighIntensityEvents: " + GradleMCConfig.ALLOW_HIGH_INTENSITY_EVENTS.get() + "\n"
                + "reduceIntensityAfterDeath: " + GradleMCConfig.REDUCE_INTENSITY_AFTER_DEATH.get() + "\n"
                + "adaptiveDifficultyMultiplier: " + GradleMCConfig.ADAPTIVE_DIFFICULTY_MULTIPLIER.get() + "\n";
    }

    private String ruleSummary() {
        return "Rule file name: " + GradleMCConfig.RULES_FILE_NAME.get() + "\n"
                + "Loaded rules: " + RiskRuleLoader.current().rules().size() + "\n"
                + "Rule load messages: " + RiskRuleLoader.current().loadResults().size() + "\n";
    }

    private String latestTestSummaries() {
        StringBuilder builder = new StringBuilder();
        builder.append("Latest performance report: ")
                .append(PerformanceTestManager.latestReportPath() == null ? "none" : GradleMcPaths.displayPath(PerformanceTestManager.latestReportPath()))
                .append('\n');
        builder.append("Latest worldgen observation report: ")
                .append(WorldgenObservationManager.latestReportPath() == null ? "none" : GradleMcPaths.displayPath(WorldgenObservationManager.latestReportPath()))
                .append('\n');
        return builder.toString();
    }

    private void addText(ZipOutputStream zip, String name, String content) throws IOException {
        zip.putNextEntry(new ZipEntry(name));
        zip.write(content.getBytes(StandardCharsets.UTF_8));
        zip.closeEntry();
    }

    private void addFile(ZipOutputStream zip, String name, Path path) throws IOException {
        zip.putNextEntry(new ZipEntry(name));
        Files.copy(path, zip);
        zip.closeEntry();
    }

    private String redact(String line) {
        String gamePath = GradleMcPaths.gameDirectory().toString();
        return line.replace(gamePath, "[game-dir]")
                .replace(System.getProperty("user.home", ""), "[user-home]")
                .replaceAll("(?i)(token|password|apikey|api_key|secret)=\\S+", "$1=[redacted]");
    }

    private String modVersion() {
        return FabricLoader.getInstance()
                .getModContainer(GradleMC.MOD_ID)
                .map(container -> container.getMetadata().getVersion().getFriendlyString())
                .orElse("unknown");
    }

    private String loaderVersion() {
        return FabricLoader.getInstance()
                .getModContainer("fabricloader")
                .map(container -> container.getMetadata().getVersion().getFriendlyString())
                .orElse("unknown");
    }
}
