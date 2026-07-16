package com.soumyajit.gradlemc.task;

import java.util.List;
import java.util.Map;

public record TaskExplanation(
        String requestedId,
        String taskId,
        List<String> dependencyPath,
        TaskState predictedState,
        String reason,
        Map<String, String> changedInputs,
        List<String> missingCapabilities
) {
    public TaskExplanation {
        dependencyPath = List.copyOf(dependencyPath);
        changedInputs = Map.copyOf(changedInputs);
        missingCapabilities = List.copyOf(missingCapabilities);
    }
}
