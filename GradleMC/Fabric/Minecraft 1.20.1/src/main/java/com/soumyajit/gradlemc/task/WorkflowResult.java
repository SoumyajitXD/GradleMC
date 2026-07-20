package com.soumyajit.gradlemc.task;

import java.time.Instant;
import java.util.List;

public record WorkflowResult(String runId, WorkflowPlan plan, TaskState state, Instant startedAt, Instant endedAt,
                             long elapsedNanos, List<TaskResult> taskResults, String cancellationReason, String provenance) {
    public WorkflowResult {
        taskResults = List.copyOf(taskResults == null ? List.of() : taskResults);
        cancellationReason = cancellationReason == null ? "" : cancellationReason;
        provenance = provenance == null ? "workflow-engine-v1" : provenance;
    }
}
