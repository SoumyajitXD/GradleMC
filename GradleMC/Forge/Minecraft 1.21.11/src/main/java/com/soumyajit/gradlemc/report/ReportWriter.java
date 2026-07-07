package com.soumyajit.gradlemc.report;

import com.soumyajit.gradlemc.GradleMC;
import com.soumyajit.gradlemc.check.CheckCategory;
import com.soumyajit.gradlemc.check.CheckResult;
import com.soumyajit.gradlemc.check.Severity;
import com.soumyajit.gradlemc.config.GradleMCConfig;
import com.soumyajit.gradlemc.smart.DiagnosticEvidence;
import com.soumyajit.gradlemc.smart.DiagnosticFinding;
import com.soumyajit.gradlemc.smart.SmartRecommendation;
import com.soumyajit.gradlemc.smart.StabilityAdvisor;
import com.soumyajit.gradlemc.smart.StabilityScore;
import com.soumyajit.gradlemc.util.GradleMcPaths;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.versions.forge.ForgeVersion;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ReportWriter {
    public Path write(Report report, Path reportDirectory) throws IOException {
        Files.createDirectories(reportDirectory);
        Path reportFile = ReportFileNames.unique(reportDirectory, "gradlemc-", report.createdAt(), ".txt");
        Files.write(reportFile, linesFor(report, reportFile), StandardCharsets.UTF_8);
        return reportFile;
    }

    private List<String> linesFor(Report report, Path reportFile) {
        List<String> lines = new ArrayList<>();
        lines.add("GradleMC Stability Report");
        lines.add("=========================");
        lines.add("Title: " + report.title());
        lines.add("Generated: " + ReportFileNames.DISPLAY_TIMESTAMP.format(report.createdAt()));
        lines.add("");
        lines.add("Environment");
        lines.add("-----------");
        lines.add("Product: " + GradleMC.PRODUCT_NAME);
        lines.add("Version: " + modVersion());
        lines.add("Variant: " + GradleMC.CURRENT_DISPLAY_VARIANT);
        lines.add("Minecraft: " + GradleMC.CURRENT_MINECRAFT_VERSION);
        lines.add("Loader: " + GradleMC.CURRENT_LOADER_NAME);
        lines.add("Forge: " + ForgeVersion.getVersion());
        lines.add("Java: " + System.getProperty("java.version", "unknown"));
        lines.add("Physical side: " + FMLEnvironment.dist);
        lines.add("Output root: " + GradleMcPaths.gradleMcDirectory());
        lines.add("Loaded mods: " + ModList.get().getMods().size());
        lines.add("");
        lines.add("Command Summary");
        lines.add("---------------");
        lines.add("/gradlemc check: quick Java, memory, mod, report, config, and rule checks.");
        lines.add("/gradlemc config check: shallow config directory diagnostics.");
        lines.add("/gradlemc rules check: local risk-rule diagnostics.");
        lines.add("/gradlemc perf start <seconds>: bounded server TPS/MSPT sampling.");
        lines.add("/gradlemc worldgen start <seconds>: lightweight chunk/worldgen pressure observation.");
        lines.add("/gradlemc issuebundle create: safe support ZIP with GradleMC reports and summaries.");
        lines.add("/gradlemc testfps start <seconds>: client-only bounded FPS test.");
        lines.add("");
        lines.add("Summary");
        lines.add("-------");
        lines.add("Summary: " + report.summaryLine());
        lines.add("Pass: " + report.count(Severity.PASS));
        lines.add("Info: " + report.count(Severity.INFO));
        lines.add("Warn: " + report.count(Severity.WARN));
        lines.add("Fail: " + report.count(Severity.FAIL));
        lines.add("Critical: " + report.count(Severity.CRITICAL));
        if (GradleMCConfig.SMART_DIAGNOSTICS_ENABLED.get()) {
            addSmartDiagnostics(lines, report);
        }
        lines.add("");
        lines.add("Attention");
        lines.add("---------");
        List<CheckResult> attention = report.results().stream()
                .filter(result -> result.severity() == Severity.WARN
                        || result.severity() == Severity.FAIL
                        || result.severity() == Severity.CRITICAL)
                .toList();
        if (attention.isEmpty()) {
            lines.add("No warnings or failures.");
        } else {
            for (CheckResult result : attention) {
                lines.add("[" + result.severity() + "] " + result.category() + " - " + result.title());
                lines.add("Detail: " + result.detail());
                if (result.suggestion() != null && !result.suggestion().isBlank()) {
                    lines.add("Suggestion: " + result.suggestion());
                }
                lines.add("");
            }
        }
        lines.add("");
        lines.add("Suggested Actions");
        lines.add("-----------------");
        List<String> suggestions = attention.stream()
                .map(CheckResult::suggestion)
                .filter(suggestion -> suggestion != null && !suggestion.isBlank())
                .distinct()
                .toList();
        if (suggestions.isEmpty()) {
            lines.add("No immediate action suggested.");
        } else {
            suggestions.forEach(suggestion -> lines.add("- " + suggestion));
        }

        Map<CheckCategory, List<CheckResult>> byCategory = report.results().stream()
                .sorted(Comparator.comparing(CheckResult::category).thenComparing(CheckResult::severity))
                .collect(Collectors.groupingBy(CheckResult::category, java.util.LinkedHashMap::new, Collectors.toList()));
        lines.add("");
        lines.add("Results By Category");
        lines.add("-------------------");
        for (Map.Entry<CheckCategory, List<CheckResult>> entry : byCategory.entrySet()) {
            lines.add(String.valueOf(entry.getKey()));
            lines.add("-".repeat(String.valueOf(entry.getKey()).length()));
            for (CheckResult result : entry.getValue()) {
                lines.add("[" + result.severity() + "] " + result.title());
                lines.add("Detail: " + result.detail());
                if (result.suggestion() != null && !result.suggestion().isBlank()) {
                    lines.add("Suggestion: " + result.suggestion());
                }
                lines.add("");
            }
        }
        lines.add("Report path: " + reportFile);
        return lines;
    }

    private void addSmartDiagnostics(List<String> lines, Report report) {
        StabilityScore score = StabilityAdvisor.evaluate(null, report.results());
        lines.add("");
        lines.add("Smart Diagnostics");
        lines.add("-----------------");
        lines.add(score.summaryLine());
        lines.add("Technical stability measures modpack/runtime health. Higher is better.");
        lines.add("Local adaptive diagnostics only. No cloud AI, LLMs, telemetry, or external API calls.");
        lines.add("");
        lines.add("Top Findings");
        lines.add("------------");
        if (score.findings().isEmpty()) {
            lines.add("No smart diagnostic findings from available data.");
        } else {
            for (DiagnosticFinding finding : score.findings().stream().limit(8).toList()) {
                lines.add("[" + finding.severity() + "] " + finding.title() + " (confidence " + finding.confidence() + ")");
                for (DiagnosticEvidence evidence : finding.evidence()) {
                    lines.add("- " + evidence.metric() + ": observed " + evidence.observed()
                            + ", threshold " + evidence.threshold() + ". " + evidence.detail());
                }
            }
        }
        lines.add("");
        lines.add("Recommendations");
        lines.add("---------------");
        if (score.recommendations().isEmpty()) {
            lines.add("No smart recommendations from available data.");
        } else {
            for (SmartRecommendation recommendation : score.recommendations().stream().limit(10).toList()) {
                lines.add("- " + recommendation.title() + " [" + recommendation.confidence() + "]");
                lines.add("  Reason: " + recommendation.reason());
                lines.add("  Action: " + recommendation.action());
            }
        }
        lines.add("");
        lines.add("Adaptive Baseline");
        lines.add("-----------------");
        if (score.baseline().isEmpty()) {
            lines.add("No adaptive baseline exists yet.");
        } else {
            lines.add("Last updated: " + score.baseline().lastUpdated());
            score.baseline().metrics().forEach((name, stats) -> lines.add("- " + name
                    + ": samples=" + stats.samples()
                    + ", avg=" + String.format(java.util.Locale.ROOT, "%.2f", stats.average())
                    + ", min=" + String.format(java.util.Locale.ROOT, "%.2f", stats.min())
                    + ", max=" + String.format(java.util.Locale.ROOT, "%.2f", stats.max())));
        }
        lines.add("");
        lines.add("Missing Data Notes");
        lines.add("------------------");
        if (score.missingDataNotes().isEmpty()) {
            lines.add("No major missing data notes.");
        } else {
            score.missingDataNotes().forEach(note -> lines.add("- " + note));
        }
        if (!score.trendNotes().isEmpty()) {
            lines.add("");
            lines.add("Trend Notes");
            lines.add("-----------");
            score.trendNotes().forEach(note -> lines.add("- " + note));
        }
    }

    private String modVersion() {
        return ModList.get().getModContainerById(GradleMC.MOD_ID)
                .map(container -> container.getModInfo().getVersion().toString())
                .orElse("unknown");
    }
}
