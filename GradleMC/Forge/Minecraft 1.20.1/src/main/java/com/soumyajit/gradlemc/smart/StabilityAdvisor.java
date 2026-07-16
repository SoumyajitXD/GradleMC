package com.soumyajit.gradlemc.smart;

import com.soumyajit.gradlemc.check.CheckCategory;
import com.soumyajit.gradlemc.check.CheckResult;
import com.soumyajit.gradlemc.check.Severity;
import com.soumyajit.gradlemc.metrics.PerformanceTestManager;
import com.soumyajit.gradlemc.metrics.PerformanceTestResult;
import com.soumyajit.gradlemc.metrics.WorldgenObservationManager;
import com.soumyajit.gradlemc.metrics.WorldgenObservationResult;
import com.soumyajit.gradlemc.util.RuntimeSnapshots;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.fml.ModList;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public final class StabilityAdvisor {
    private StabilityAdvisor() {
    }

    public static StabilityScore evaluate(MinecraftServer server, List<CheckResult> results) {
        AdaptiveBaseline baseline = AdaptiveBaselineStore.load();
        List<DiagnosticFinding> findings = new ArrayList<>();
        List<SmartRecommendation> recommendations = new ArrayList<>();
        List<String> missing = new ArrayList<>();
        List<String> trends = new ArrayList<>();
        ScoreAccumulator score = new ScoreAccumulator();

        evaluateChecks(results, findings, recommendations, score);
        evaluateMemory(findings, recommendations, trends, score, baseline);
        evaluateMods(findings, recommendations, score);
        evaluatePerf(findings, recommendations, missing, trends, score, baseline);
        evaluateWorldgen(findings, recommendations, missing, trends, score, baseline);
        evaluateLatestScans(findings, recommendations, missing, trends, score, baseline);
        evaluateMissingFps(missing, recommendations, score, baseline);

        int finalScore = Math.max(0, Math.min(100, (int) Math.round(score.value)));
        ConfidenceLevel confidence = confidence(missing.size(), baseline);
        RecommendationValidator.Validation validated = RecommendationValidator.validate(recommendations, missing);
        trends.addAll(validated.notes());
        return new StabilityScore(
                finalScore,
                RiskLevel.fromScore(finalScore),
                confidence,
                findings.stream()
                        .sorted(Comparator.comparing(DiagnosticFinding::severity).reversed())
                        .toList(),
                validated.recommendations().stream()
                        .sorted(Comparator.comparingInt(SmartRecommendation::priority).reversed())
                        .toList(),
                List.copyOf(missing),
                List.copyOf(trends),
                baseline
        );
    }

    private static void evaluateChecks(List<CheckResult> results, List<DiagnosticFinding> findings,
                                       List<SmartRecommendation> recommendations, ScoreAccumulator score) {
        long warn = results.stream().filter(result -> result.severity() == Severity.WARN).count();
        long fail = results.stream().filter(result -> result.severity() == Severity.FAIL).count();
        long critical = results.stream().filter(result -> result.severity() == Severity.CRITICAL).count();
        score.deduct(warn * 4 + fail * 12 + critical * 20);
        results.stream()
                .filter(result -> result.severity() == Severity.WARN
                        || result.severity() == Severity.FAIL
                        || result.severity() == Severity.CRITICAL)
                .limit(6)
                .forEach(result -> {
                    DiagnosticEvidence evidence = new DiagnosticEvidence(
                            result.category().name(),
                            result.severity() + " - " + result.title(),
                            "Default GradleMC check result",
                            result.detail()
                    );
                    findings.add(new DiagnosticFinding(result.title(), severityFrom(result.severity()), ConfidenceLevel.HIGH, List.of(evidence)));
                    if (result.category() == CheckCategory.CONFIG || result.category() == CheckCategory.MODS) {
                        recommendations.add(new SmartRecommendation(
                                result.title(),
                                result.detail(),
                                result.suggestion(),
                                priority(severityFrom(result.severity())),
                                ConfidenceLevel.HIGH,
                                List.of(evidence)
                        ));
                    }
                });
    }

    private static void evaluateMemory(List<DiagnosticFinding> findings, List<SmartRecommendation> recommendations,
                                       List<String> trends, ScoreAccumulator score, AdaptiveBaseline baseline) {
        RuntimeSnapshots.MemorySnapshot memory = RuntimeSnapshots.memory();
        double pressure = memory.maxMiB() <= 0 ? 0.0D : (double) memory.usedMiB() / memory.maxMiB();
        DiagnosticEvidence evidence = new DiagnosticEvidence(
                "memory.usedMiB",
                memory.usedMiB() + "/" + memory.maxMiB() + " MiB (" + percent(pressure) + ")",
                "warn 80%, critical 95%",
                "Point-in-time JVM heap snapshot"
        );
        if (pressure >= 0.95D) {
            score.deduct(22);
            addMemoryRecommendation(findings, recommendations, evidence, AnomalySeverity.CRITICAL);
        } else if (pressure >= 0.80D) {
            score.deduct(10);
            addMemoryRecommendation(findings, recommendations, evidence, AnomalySeverity.MEDIUM);
        }
        baseline.metric(AdaptiveBaselineStore.memoryMetric()).ifPresent(stats -> {
            if (stats.samples() >= 2 && memory.usedMiB() > stats.average() * 1.5D) {
                trends.add("Memory is above the local baseline average (" + round(stats.average()) + " MiB).");
            }
        });
    }

    private static void addMemoryRecommendation(List<DiagnosticFinding> findings, List<SmartRecommendation> recommendations,
                                                DiagnosticEvidence evidence, AnomalySeverity severity) {
        findings.add(new DiagnosticFinding("Memory pressure is high", severity, ConfidenceLevel.HIGH, List.of(evidence)));
        recommendations.add(new SmartRecommendation(
                "Review memory pressure",
                "Used heap is high for the configured maximum heap.",
                "Increase heap only if GC/memory evidence supports it; otherwise reduce heavy mods/configs.",
                priority(severity),
                ConfidenceLevel.HIGH,
                List.of(evidence)
        ));
    }

    private static void evaluateMods(List<DiagnosticFinding> findings, List<SmartRecommendation> recommendations, ScoreAccumulator score) {
        int modCount = ModList.get().getMods().size();
        AnomalySeverity severity = modCount >= 350 ? AnomalySeverity.HIGH : modCount >= 250 ? AnomalySeverity.MEDIUM
                : modCount >= 150 ? AnomalySeverity.LOW : AnomalySeverity.NONE;
        if (severity != AnomalySeverity.NONE) {
            score.deduct(severity == AnomalySeverity.HIGH ? 12 : severity == AnomalySeverity.MEDIUM ? 7 : 3);
            DiagnosticEvidence evidence = new DiagnosticEvidence("mods.count", String.valueOf(modCount), "watch 150, risky 250, unstable 350", "Large packs have more interaction surface.");
            findings.add(new DiagnosticFinding("Large loaded mod set", severity, ConfidenceLevel.MEDIUM, List.of(evidence)));
            recommendations.add(new SmartRecommendation(
                    "Check local risk rules",
                    "A large mod set benefits from explicit compatibility rules.",
                    "Run /gradlemc rules check and add local rules for known risky combinations.",
                    priority(severity),
                    ConfidenceLevel.MEDIUM,
                    List.of(evidence)
            ));
        }
    }

    private static void evaluatePerf(List<DiagnosticFinding> findings, List<SmartRecommendation> recommendations,
                                     List<String> missing, List<String> trends, ScoreAccumulator score, AdaptiveBaseline baseline) {
        PerformanceTestResult result = PerformanceTestManager.latestResult();
        if (result == null) {
            missing.add("No latest server performance sample in this session.");
            recommendations.add(new SmartRecommendation(
                    "Collect server tick data",
                    "TPS/MSPT evidence is missing, so score confidence is lower.",
                    "Run /gradlemc perf start 60.",
                    30,
                    ConfidenceLevel.HIGH,
                    List.of()
            ));
            return;
        }
        DiagnosticEvidence tps = new DiagnosticEvidence("perf.tps", format(result.approximateTps()), "warn <18, critical <15", "Latest bounded perf sample.");
        DiagnosticEvidence mspt = new DiagnosticEvidence("perf.mspt", format(result.averageTickMs()), "warn >45, critical >70", "Latest bounded perf sample.");
        if (result.approximateTps() < 15.0D || result.averageTickMs() > 70.0D) {
            score.deduct(22);
            addPerfRecommendation(findings, recommendations, List.of(tps, mspt), AnomalySeverity.CRITICAL);
        } else if (result.approximateTps() < 18.0D || result.averageTickMs() > 45.0D) {
            score.deduct(12);
            addPerfRecommendation(findings, recommendations, List.of(tps, mspt), AnomalySeverity.HIGH);
        }
        baseline.metric(AdaptiveBaselineStore.msptMetric()).ifPresent(stats -> {
            if (stats.samples() >= 2 && result.averageTickMs() > stats.average() * 1.5D) {
                trends.add("Average MSPT is above local baseline (" + format(stats.average()) + " ms).");
            }
        });
    }

    private static void addPerfRecommendation(List<DiagnosticFinding> findings, List<SmartRecommendation> recommendations,
                                              List<DiagnosticEvidence> evidence, AnomalySeverity severity) {
        findings.add(new DiagnosticFinding("Server tick performance is degraded", severity, ConfidenceLevel.HIGH, evidence));
        recommendations.add(new SmartRecommendation(
                "Investigate server tick pressure",
                "Latest TPS/MSPT sample indicates server-side tick cost.",
                "Run /gradlemc entities 64 and /gradlemc blockentities 64 near laggy areas.",
                priority(severity),
                ConfidenceLevel.HIGH,
                evidence
        ));
    }

    private static void evaluateWorldgen(List<DiagnosticFinding> findings, List<SmartRecommendation> recommendations,
                                         List<String> missing, List<String> trends, ScoreAccumulator score, AdaptiveBaseline baseline) {
        WorldgenObservationResult result = WorldgenObservationManager.latestResult();
        if (result == null) {
            missing.add("No latest passive worldgen observation in this session.");
            recommendations.add(new SmartRecommendation(
                    "Collect worldgen pressure data",
                    "Worldgen pressure cannot be assessed without an observation while exploring.",
                    "Run /gradlemc worldgen start 120 while exploring new terrain.",
                    20,
                    ConfidenceLevel.MEDIUM,
                    List.of()
            ));
            return;
        }
        DiagnosticEvidence evidence = new DiagnosticEvidence("worldgen.maxMspt", format(result.maxTickMs()), "warn avg >50 or max >100", "Latest passive worldgen observation.");
        if (!result.warnings().isEmpty() || result.maxTickMs() >= 100.0D) {
            score.deduct(12);
            findings.add(new DiagnosticFinding("Worldgen pressure suspected", AnomalySeverity.HIGH, ConfidenceLevel.MEDIUM, List.of(evidence)));
            recommendations.add(new SmartRecommendation(
                    "Repeat worldgen observation",
                    "Latest passive observation found chunk, memory, or tick warnings.",
                    "Run /gradlemc worldgen start 120 while reproducing terrain exploration.",
                    70,
                    ConfidenceLevel.MEDIUM,
                    List.of(evidence)
            ));
        }
        baseline.metric(AdaptiveBaselineStore.worldgenTickMetric()).ifPresent(stats -> {
            if (stats.samples() >= 2 && result.averageTickMs() > stats.average() * 1.5D) {
                trends.add("Worldgen average tick time is above local baseline (" + format(stats.average()) + " ms).");
            }
        });
    }

    private static void evaluateLatestScans(List<DiagnosticFinding> findings, List<SmartRecommendation> recommendations,
                                            List<String> missing, List<String> trends, ScoreAccumulator score, AdaptiveBaseline baseline) {
        SmartMetricSnapshots.latestEntityScan().ifPresentOrElse(scan -> {
            AdaptiveThresholds.Threshold threshold = AdaptiveThresholds.higherIsWorse(AdaptiveBaselineStore.entityMetric(), 180, 350, baseline);
            DiagnosticEvidence evidence = new DiagnosticEvidence("entities.count", String.valueOf(scan.count()), "warn " + round(threshold.warn()) + ", critical " + round(threshold.critical()), "Latest /gradlemc entities scan.");
            if (scan.count() >= threshold.critical()) {
                score.deduct(14);
                addScanRecommendation(findings, recommendations, evidence, "High entity count detected", "Run /gradlemc entities 64 and inspect top entity types.", AnomalySeverity.HIGH);
            } else if (scan.count() >= threshold.warn()) {
                score.deduct(7);
                addScanRecommendation(findings, recommendations, evidence, "Elevated entity count detected", "Run /gradlemc entities 64 near laggy areas.", AnomalySeverity.MEDIUM);
            }
            baseline.metric(AdaptiveBaselineStore.entityMetric()).ifPresent(stats -> {
                if (stats.samples() >= 2 && scan.count() > stats.average() * 1.5D) {
                    trends.add("Entity count is above local scan baseline (" + round(stats.average()) + ").");
                }
            });
        }, () -> missing.add("No entity scan result in this session."));

        SmartMetricSnapshots.latestBlockEntityScan().ifPresentOrElse(scan -> {
            AdaptiveThresholds.Threshold threshold = AdaptiveThresholds.higherIsWorse(AdaptiveBaselineStore.blockEntityMetric(), 256, 512, baseline);
            DiagnosticEvidence evidence = new DiagnosticEvidence("blockEntities.count", String.valueOf(scan.count()), "warn " + round(threshold.warn()) + ", critical " + round(threshold.critical()), "Latest /gradlemc blockentities scan.");
            if (scan.count() >= threshold.critical()) {
                score.deduct(14);
                addScanRecommendation(findings, recommendations, evidence, "High block entity density detected", "Run /gradlemc blockentities 64 near laggy bases.", AnomalySeverity.HIGH);
            } else if (scan.count() >= threshold.warn()) {
                score.deduct(7);
                addScanRecommendation(findings, recommendations, evidence, "Elevated block entity density detected", "Run /gradlemc blockentities 64 near laggy bases.", AnomalySeverity.MEDIUM);
            }
            baseline.metric(AdaptiveBaselineStore.blockEntityMetric()).ifPresent(stats -> {
                if (stats.samples() >= 2 && scan.count() > stats.average() * 1.5D) {
                    trends.add("Block entity count is above local scan baseline (" + round(stats.average()) + ").");
                }
            });
        }, () -> missing.add("No block entity scan result in this session."));
    }

    private static void addScanRecommendation(List<DiagnosticFinding> findings, List<SmartRecommendation> recommendations,
                                              DiagnosticEvidence evidence, String title, String action, AnomalySeverity severity) {
        findings.add(new DiagnosticFinding(title, severity, ConfidenceLevel.MEDIUM, List.of(evidence)));
        recommendations.add(new SmartRecommendation(title, evidence.detail(), action, priority(severity), ConfidenceLevel.MEDIUM, List.of(evidence)));
    }

    private static void evaluateMissingFps(List<String> missing, List<SmartRecommendation> recommendations, ScoreAccumulator score, AdaptiveBaseline baseline) {
        if (baseline.metric(AdaptiveBaselineStore.fpsMetric()).isEmpty()) {
            missing.add("No local FPS baseline is available.");
            score.deduct(2);
            recommendations.add(new SmartRecommendation(
                    "Collect client FPS data",
                    "Client FPS evidence is missing; dedicated servers will not have FPS.",
                    "Run /gradlemc testfps start 60 on a client.",
                    20,
                    ConfidenceLevel.MEDIUM,
                    List.of()
            ));
        }
    }

    private static ConfidenceLevel confidence(int missingCount, AdaptiveBaseline baseline) {
        long matureMetrics = baseline.metrics().values().stream()
                .filter(stats -> stats.samples() >= 3)
                .count();
        if (missingCount <= 1 && matureMetrics >= 3) {
            return ConfidenceLevel.HIGH;
        }
        if (missingCount <= 3 || matureMetrics >= 1) {
            return ConfidenceLevel.MEDIUM;
        }
        return ConfidenceLevel.LOW;
    }

    private static AnomalySeverity severityFrom(Severity severity) {
        return switch (severity) {
            case CRITICAL -> AnomalySeverity.CRITICAL;
            case FAIL -> AnomalySeverity.HIGH;
            case WARN -> AnomalySeverity.MEDIUM;
            default -> AnomalySeverity.NONE;
        };
    }

    private static int priority(AnomalySeverity severity) {
        return switch (severity) {
            case CRITICAL -> 100;
            case HIGH -> 80;
            case MEDIUM -> 60;
            case LOW -> 30;
            default -> 10;
        };
    }

    private static String percent(double value) {
        return String.format(Locale.ROOT, "%.0f%%", value * 100.0D);
    }

    private static String format(double value) {
        return String.format(Locale.ROOT, "%.2f", value);
    }

    private static String round(double value) {
        return String.format(Locale.ROOT, "%.0f", value);
    }

    private static final class ScoreAccumulator {
        private double value = 100.0D;

        private void deduct(double amount) {
            value -= amount;
        }
    }
}
