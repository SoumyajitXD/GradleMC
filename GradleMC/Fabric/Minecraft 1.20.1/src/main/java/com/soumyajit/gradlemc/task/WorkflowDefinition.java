package com.soumyajit.gradlemc.task;

import java.time.Duration;
import java.util.List;

public record WorkflowDefinition(String id, String displayName, String description, List<String> rootTaskIds, Duration timeout) {
    public WorkflowDefinition {
        if (id == null || !id.matches("[a-z][a-z0-9_]*")) throw new IllegalArgumentException("Invalid workflow id: " + id);
        if (displayName == null || displayName.isBlank()) throw new IllegalArgumentException("Workflow display name is required");
        description = description == null ? "" : description;
        rootTaskIds = List.copyOf(rootTaskIds == null ? List.of() : rootTaskIds);
        if (rootTaskIds.isEmpty()) throw new IllegalArgumentException("Workflow must have tasks: " + id);
        timeout = timeout == null ? Duration.ofSeconds(30) : timeout;
        if (timeout.isNegative() || timeout.isZero()) throw new IllegalArgumentException("Workflow timeout must be positive: " + id);
    }
}
