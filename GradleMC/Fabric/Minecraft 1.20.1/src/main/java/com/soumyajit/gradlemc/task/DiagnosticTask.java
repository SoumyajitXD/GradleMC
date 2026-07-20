package com.soumyajit.gradlemc.task;

import java.time.Duration;
import java.util.List;
import java.util.Objects;

/** Immutable, centrally registered definition of one bounded diagnostic operation. */
public record DiagnosticTask(
        String id,
        String displayName,
        String description,
        String category,
        TaskEnvironment environment,
        List<TaskDependency> dependencies,
        Duration timeout,
        boolean cancellationSupported,
        List<EvidenceType> evidenceTypes,
        DiagnosticTaskAction action
) {
    public DiagnosticTask {
        if (id == null || !id.matches("[a-z][a-z0-9_]*")) {
            throw new IllegalArgumentException("Invalid task id: " + id);
        }
        displayName = requireText(displayName, "displayName");
        description = description == null ? "" : description;
        category = requireText(category, "category");
        environment = Objects.requireNonNullElse(environment, TaskEnvironment.ANY);
        dependencies = List.copyOf(dependencies == null ? List.of() : dependencies);
        if (dependencies.stream().anyMatch(dependency -> dependency.id().equals(id))) {
            throw new IllegalArgumentException("Task cannot depend on itself: " + id);
        }
        timeout = timeout == null ? Duration.ofSeconds(5) : timeout;
        if (timeout.isZero() || timeout.isNegative()) throw new IllegalArgumentException("Task timeout must be positive: " + id);
        evidenceTypes = List.copyOf(evidenceTypes == null ? List.of() : evidenceTypes);
        action = Objects.requireNonNull(action, "action");
    }

    private static String requireText(String value, String name) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(name + " must not be blank");
        return value;
    }
}
