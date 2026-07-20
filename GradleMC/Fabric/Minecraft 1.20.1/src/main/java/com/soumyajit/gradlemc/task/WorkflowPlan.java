package com.soumyajit.gradlemc.task;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/** Immutable execution plan shared verbatim by dry run, UI/command explanation and execution provenance. */
public record WorkflowPlan(String workflowId, Instant createdAt, List<DiagnosticTask> orderedTasks,
                           Map<String, String> plannedSkips, List<EvidenceType> expectedEvidence, long timeoutNanos) {
    public WorkflowPlan {
        orderedTasks = List.copyOf(orderedTasks == null ? List.of() : orderedTasks);
        plannedSkips = Map.copyOf(plannedSkips == null ? Map.of() : plannedSkips);
        expectedEvidence = List.copyOf(expectedEvidence == null ? List.of() : expectedEvidence);
    }
}
