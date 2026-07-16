package com.soumyajit.gradlemc.task;

import java.util.LinkedHashMap;
import java.util.Map;

public record TaskOutcome(TaskState state, String reason, String message, Map<String, String> outputs) {
    public TaskOutcome { outputs = Map.copyOf(outputs == null ? Map.of() : new LinkedHashMap<>(outputs)); }
    public static TaskOutcome success(Map<String, String> outputs) { return new TaskOutcome(TaskState.SUCCESS, "completed", "Completed", outputs); }
    public static TaskOutcome unavailable(String reason, String message) { return new TaskOutcome(TaskState.UNAVAILABLE, reason, message, Map.of()); }
    public static TaskOutcome failed(String reason, String message) { return new TaskOutcome(TaskState.FAILED, reason, message, Map.of()); }
}
