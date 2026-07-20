package com.soumyajit.gradlemc.task;

import java.util.List;

/** The action may retain valid evidence while honestly signalling partial collection. */
public record TaskOutcome(TaskState state, List<DiagnosticEvidence> evidence, List<String> warnings, List<String> errors, String reason) {
    public TaskOutcome {
        if (state == null || state == TaskState.RUNNING || state == TaskState.QUEUED || state == TaskState.PLANNED) throw new IllegalArgumentException("TaskOutcome requires a terminal state");
        evidence = List.copyOf(evidence == null ? List.of() : evidence);
        warnings = List.copyOf(warnings == null ? List.of() : warnings);
        errors = List.copyOf(errors == null ? List.of() : errors);
        reason = reason == null ? "" : reason;
    }
    public static TaskOutcome success(List<DiagnosticEvidence> evidence) { return new TaskOutcome(TaskState.SUCCEEDED, evidence, List.of(), List.of(), ""); }
    public static TaskOutcome partial(List<DiagnosticEvidence> evidence, String reason) { return new TaskOutcome(TaskState.PARTIAL, evidence, List.of(reason), List.of(), reason); }
    public static TaskOutcome unavailable(String reason) { return new TaskOutcome(TaskState.UNAVAILABLE, List.of(), List.of(), List.of(), reason); }
}
