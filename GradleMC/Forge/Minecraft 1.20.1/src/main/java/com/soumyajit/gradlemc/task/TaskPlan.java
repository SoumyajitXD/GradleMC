package com.soumyajit.gradlemc.task;

import java.util.List;
public record TaskPlan(String requestedId, List<DiagnosticTask> orderedTasks) { public TaskPlan { orderedTasks = List.copyOf(orderedTasks); } }
