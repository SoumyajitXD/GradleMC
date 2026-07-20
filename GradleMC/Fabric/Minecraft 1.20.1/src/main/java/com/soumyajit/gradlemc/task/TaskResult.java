package com.soumyajit.gradlemc.task;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

public record TaskResult(String taskId, TaskState state, Instant startedAt, Instant endedAt, long elapsedNanos,
                         List<DiagnosticEvidence> evidence, List<String> warnings, List<String> errors,
                         String cancellationReason, String missingPrerequisite, TaskEnvironment environment, String provenance) {
    public TaskResult {
        if (taskId == null || taskId.isBlank()) throw new IllegalArgumentException("taskId is required");
        state = Objects.requireNonNull(state, "state");
        startedAt = Objects.requireNonNull(startedAt, "startedAt");
        endedAt = Objects.requireNonNull(endedAt, "endedAt");
        if (elapsedNanos < 0L) throw new IllegalArgumentException("elapsedNanos must not be negative");
        environment = Objects.requireNonNull(environment, "environment");
        evidence = List.copyOf(evidence == null ? List.of() : evidence);
        warnings = List.copyOf(warnings == null ? List.of() : warnings);
        errors = List.copyOf(errors == null ? List.of() : errors);
        cancellationReason = cancellationReason == null ? "" : cancellationReason;
        missingPrerequisite = missingPrerequisite == null ? "" : missingPrerequisite;
        provenance = provenance == null ? "" : provenance;
    }
    public boolean completedSuccessfully() { return state == TaskState.SUCCEEDED || state == TaskState.PARTIAL; }
}
