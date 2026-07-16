package com.soumyajit.gradlemc.experiment;

import java.util.*;

public final class ExperimentComparator {
    private static final Set<String> HARD_CONTEXT = Set.of("minecraft", "forge", "javaMajor", "world", "workflow", "taskParameters", "testDuration");
    private static final Set<String> LOWER_IS_BETTER = Set.of("p95FrameTimeMs", "p95Mspt", "maxGcPauseMs", "usedMiB", "criticalFindings");
    private static final double MEANINGFUL_CHANGE = 0.05D;
    private ExperimentComparator() { }

    public static ExperimentComparison compare(ExperimentSnapshot baseline, ExperimentSnapshot candidate) {
        List<String> issues = new ArrayList<>();
        for (String key : HARD_CONTEXT) if (!Objects.equals(baseline.context().get(key), candidate.context().get(key))) issues.add(key + " changed");
        if (!issues.isEmpty()) return new ExperimentComparison(ExperimentVerdict.NOT_COMPARABLE,
                "Material test context changed", Map.of(), issues);
        if (baseline.fingerprint().equals(candidate.fingerprint())) return new ExperimentComparison(ExperimentVerdict.INCONCLUSIVE,
                "No instance fingerprint change was detected", Map.of(), List.of("manual change not detected"));
        Set<String> contextChanges = new TreeSet<>();
        Set<String> contextKeys = new TreeSet<>(baseline.context().keySet()); contextKeys.addAll(candidate.context().keySet()); contextKeys.removeAll(HARD_CONTEXT);
        for (String key : contextKeys) if (!Objects.equals(baseline.context().get(key), candidate.context().get(key))) contextChanges.add(key);
        if (contextChanges.size() > 1) return new ExperimentComparison(ExperimentVerdict.NOT_COMPARABLE,
                "More than one instance context category changed", Map.of(), contextChanges.stream().map(key -> key + " changed").toList());
        Map<String, Double> changes = new TreeMap<>();
        int improved = 0, regressed = 0, stable = 0;
        for (Map.Entry<String, Double> entry : baseline.metrics().entrySet()) {
            Double after = candidate.metrics().get(entry.getKey());
            double before = entry.getValue();
            if (after == null || !Double.isFinite(before) || !Double.isFinite(after) || before == 0) continue;
            double raw = (after - before) / Math.abs(before);
            double normalized = LOWER_IS_BETTER.contains(entry.getKey()) ? -raw : raw;
            changes.put(entry.getKey(), normalized);
            if (normalized >= MEANINGFUL_CHANGE) improved++; else if (normalized <= -MEANINGFUL_CHANGE) regressed++; else stable++;
        }
        if (changes.isEmpty()) return new ExperimentComparison(ExperimentVerdict.INCONCLUSIVE,
                "No common finite metrics were collected", Map.of(), List.of("compatible metric evidence missing"));
        ExperimentVerdict verdict = improved > 0 && regressed == 0 ? ExperimentVerdict.IMPROVED
                : regressed > 0 && improved == 0 ? ExperimentVerdict.REGRESSED
                : improved == 0 && regressed == 0 ? ExperimentVerdict.NO_MEANINGFUL_CHANGE
                : ExperimentVerdict.INCONCLUSIVE;
        String explanation = verdict == ExperimentVerdict.INCONCLUSIVE ? "Metrics moved in conflicting directions"
                : improved + " improved, " + regressed + " regressed, " + stable + " below the meaningful-change threshold";
        return new ExperimentComparison(verdict, explanation, changes, contextChanges.stream().map(key -> key + " changed as the experimental variable").toList());
    }
}
