package com.soumyajit.gradlemc.task;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/** Canonical immutable per-task execution result, suitable for bounded persistence. */
public record ExecutionResult(int schemaVersion, String executionId, String taskId,
                              String taskImplementationVersion, String requestedOperation,
                              Status status, Instant startedAt, Instant endedAt,
                              long durationNanos, String category, TaskSide physicalSide,
                              TaskSide logicalSide, String threadAffinity,
                              String inputFingerprint, String environmentFingerprint,
                              String outputSummaryType, List<EvidenceProvenance> evidence,
                              CacheDecision cacheDecision, boolean executed, boolean reused,
                              Map<String, String> outputs, List<String> warnings,
                              TaskFailure failure, List<String> generatedOutputPaths,
                              String gradleMcVersion, String minecraftVersion,
                              String forgeVersion, String javaVersion) {
    public static final int SCHEMA_VERSION = 1;
    public enum Status { PLANNED, RUNNING, SUCCEEDED, SUCCEEDED_WITH_WARNINGS, REUSED_FROM_CACHE,
        SKIPPED, CANCELLED, TIMED_OUT, DEPENDENCY_FAILED, UNSUPPORTED, PERMISSION_DENIED, FAILED }

    public ExecutionResult {
        executionId = safe(executionId); taskId = safe(taskId); taskImplementationVersion = safe(taskImplementationVersion);
        requestedOperation = safe(requestedOperation); status = status == null ? Status.FAILED : status;
        category = safe(category); physicalSide = physicalSide == null ? TaskSide.ANY : physicalSide;
        logicalSide = logicalSide == null ? TaskSide.ANY : logicalSide; threadAffinity = safe(threadAffinity);
        inputFingerprint = safe(inputFingerprint); environmentFingerprint = safe(environmentFingerprint);
        outputSummaryType = safe(outputSummaryType); evidence = List.copyOf(evidence == null ? List.of() : evidence);
        cacheDecision = cacheDecision == null ? CacheDecision.notCacheable("No cache decision recorded") : cacheDecision;
        outputs = Map.copyOf(outputs == null ? Map.of() : outputs);
        warnings = List.copyOf(warnings == null ? List.of() : warnings);
        generatedOutputPaths = List.copyOf(generatedOutputPaths == null ? List.of() : generatedOutputPaths);
        gradleMcVersion = safe(gradleMcVersion); minecraftVersion = safe(minecraftVersion);
        forgeVersion = safe(forgeVersion); javaVersion = safe(javaVersion);
        durationNanos = Math.max(0L, durationNanos);
    }
    private static String safe(String value) { return value == null ? "" : value; }
}
