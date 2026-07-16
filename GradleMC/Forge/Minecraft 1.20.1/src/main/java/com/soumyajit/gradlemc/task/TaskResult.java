package com.soumyajit.gradlemc.task;

import java.time.Instant;
import java.util.Map;
public record TaskResult(String taskId, TaskState state, String reason, String message, Instant startedAt,
                         Instant endedAt, String fingerprint, boolean reused, Map<String, String> outputs,
                         Map<String, String> changedInputs, TaskOverhead overhead) {
    public TaskResult {
        outputs = Map.copyOf(outputs);
        changedInputs = Map.copyOf(changedInputs);
        overhead = overhead == null ? TaskOverhead.empty() : overhead;
    }
}
