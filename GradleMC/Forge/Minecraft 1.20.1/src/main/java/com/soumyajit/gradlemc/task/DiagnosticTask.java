package com.soumyajit.gradlemc.task;

import java.util.List;
import java.util.Map;

public interface DiagnosticTask {
    String id(); String displayName(); String description(); String group(); String version();
    List<TaskDependency> dependencies(); CachePolicy cachePolicy(); boolean requiresServer(); long timeoutMillis();
    Map<String, String> inputs(TaskRunContext context);
    TaskOutcome execute(TaskRunContext context) throws Exception;

    default TaskSide physicalSide() { return requiresServer() ? TaskSide.SERVER : TaskSide.ANY; }
    default TaskSide logicalSide() { return requiresServer() ? TaskSide.SERVER : TaskSide.ANY; }
    default int permissionLevel() { return 2; }
    default TaskCost cost() { return TaskCost.CHEAP; }
    default List<String> declaredOutputs() { return List.of(); }
    default ConcurrencyPolicy concurrencyPolicy() { return ConcurrencyPolicy.EXCLUSIVE; }
    default boolean cancellationSupported() { return false; }
    default String failurePolicy() { return "fail-task"; }
    default List<String> capabilities() { return List.of(); }
}
