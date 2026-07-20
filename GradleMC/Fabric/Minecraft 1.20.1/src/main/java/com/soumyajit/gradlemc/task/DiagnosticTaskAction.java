package com.soumyajit.gradlemc.task;

@FunctionalInterface
public interface DiagnosticTaskAction { TaskOutcome execute(TaskExecutionContext context) throws Exception; }
