package com.soumyajit.gradlemc.health;

import com.soumyajit.gradlemc.config.GradleMCConfig;
import com.soumyajit.gradlemc.task.TaskOutcome;
import com.soumyajit.gradlemc.task.TaskResult;
import com.soumyajit.gradlemc.task.TaskState;
import java.util.*;

public final class HealthGateService {
    private HealthGateService() { }

    public static List<HealthGate> configuredGates() {
        boolean enabled = GradleMCConfig.HEALTH_GATES_ENABLED.get();
        return List.of(
                new HealthGate("median-fps", "Minimum median FPS", HealthGateKind.MINIMUM, "medianFps", GradleMCConfig.GATE_MIN_MEDIAN_FPS.get(), enabled),
                new HealthGate("p95-frame-time", "Maximum p95 frame time", HealthGateKind.MAXIMUM, "p95FrameTimeMs", GradleMCConfig.GATE_MAX_P95_FRAME_TIME_MS.get(), enabled),
                new HealthGate("tps", "Minimum TPS", HealthGateKind.MINIMUM, "tps", GradleMCConfig.GATE_MIN_TPS.get(), enabled),
                new HealthGate("p95-mspt", "Maximum p95 MSPT", HealthGateKind.MAXIMUM, "p95Mspt", GradleMCConfig.GATE_MAX_P95_MSPT.get(), enabled),
                new HealthGate("gc-pause", "Maximum GC pause", HealthGateKind.MAXIMUM, "maxGcPauseMs", GradleMCConfig.GATE_MAX_GC_PAUSE_MS.get(), enabled),
                new HealthGate("critical-findings", "Maximum critical findings", HealthGateKind.MAXIMUM, "criticalFindings", GradleMCConfig.GATE_MAX_CRITICAL_FINDINGS.get(), enabled),
                new HealthGate("instance-inspection", "Instance inspection completed", HealthGateKind.REQUIRED_TASK, "gradlemc:inspectInstance", 0, enabled),
                new HealthGate("scan-export", "Scan export completed", HealthGateKind.REQUIRED_TASK, "gradlemc:export_scan", 0, enabled)
        );
    }

    public static List<HealthGateResult> evaluateResults(List<TaskResult> results) {
        Map<String, TaskState> states = new HashMap<>();
        Map<String, Double> metrics = new HashMap<>();
        Map<String, String> ids = new HashMap<>();
        for (TaskResult result : results) {
            states.put(result.taskId(), result.state());
            addMetrics(result.taskId(), result.outputs(), metrics, ids);
        }
        return HealthGateEvaluator.evaluate(configuredGates(), new HealthGateEvidence(metrics, states, ids));
    }

    public static List<HealthGateResult> evaluateOutcomes(Map<String, TaskOutcome> outcomes) {
        Map<String, TaskState> states = new HashMap<>();
        Map<String, Double> metrics = new HashMap<>();
        Map<String, String> ids = new HashMap<>();
        outcomes.forEach((task, outcome) -> { states.put(task, outcome.state()); addMetrics(task, outcome.outputs(), metrics, ids); });
        return HealthGateEvaluator.evaluate(configuredGates(), new HealthGateEvidence(metrics, states, ids));
    }

    private static void addMetrics(String task, Map<String, String> outputs, Map<String, Double> metrics, Map<String, String> ids) {
        outputs.forEach((key, value) -> {
            try {
                double parsed = Double.parseDouble(value);
                if (Double.isFinite(parsed)) { metrics.put(key, parsed); ids.put(key, "task:" + task + ":" + key); }
            } catch (NumberFormatException ignored) { }
        });
    }
}
