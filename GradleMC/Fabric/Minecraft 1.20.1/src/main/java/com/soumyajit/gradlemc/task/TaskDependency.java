package com.soumyajit.gradlemc.task;

/** Dependency metadata belongs to a task definition, never to a button or formatter. */
public record TaskDependency(String id, boolean required) {
    public TaskDependency {
        if (id == null || !id.matches("[a-z][a-z0-9_]*")) throw new IllegalArgumentException("Invalid dependency id: " + id);
    }

    public static TaskDependency required(String id) { return new TaskDependency(id, true); }
    public static TaskDependency optional(String id) { return new TaskDependency(id, false); }
}
