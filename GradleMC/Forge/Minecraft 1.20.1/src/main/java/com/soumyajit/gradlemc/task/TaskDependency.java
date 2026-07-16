package com.soumyajit.gradlemc.task;

import java.util.Objects;

public record TaskDependency(String taskId, boolean required) {
    public TaskDependency { Objects.requireNonNull(taskId, "taskId"); }
    public static TaskDependency required(String id) { return new TaskDependency(id, true); }
    public static TaskDependency optional(String id) { return new TaskDependency(id, false); }
}
