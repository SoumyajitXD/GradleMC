package com.soumyajit.gradlemc.health;

import com.soumyajit.gradlemc.task.TaskState;
import java.util.Map;

public record HealthGateEvidence(Map<String, Double> metrics, Map<String, TaskState> taskStates,
                                 Map<String, String> evidenceIds) {
    public HealthGateEvidence {
        metrics = Map.copyOf(metrics);
        taskStates = Map.copyOf(taskStates);
        evidenceIds = Map.copyOf(evidenceIds);
    }
}
