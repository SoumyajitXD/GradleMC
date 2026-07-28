package com.soumyajit.gradlemc.performance;

/** Immutable, process-wide policy for GradleMC-owned optional work only. */
public record PerformancePolicy(String mode, int overlayRefreshMillis, int guiRefreshMillis, int memoryPublishMillis,
                                int serverPublishMillis, int adaptiveReevaluationMillis, int maxHeavyOperations,
                                boolean passiveObservation, boolean detailedReports, boolean detailedOverhead,
                                boolean aggressiveCoalescing, String reportDetail, String overheadDetail,
                                String guardSensitivity, int internalMinimumRefreshMillis,
                                int internalMaximumRefreshMillis, String overheadWarning) {
    public PerformancePolicy {
        mode = mode == null || mode.isBlank() ? "BALANCED" : mode;
        overlayRefreshMillis = clamp(overlayRefreshMillis, 250, 5_000);
        guiRefreshMillis = clamp(guiRefreshMillis, 250, 5_000);
        memoryPublishMillis = clamp(memoryPublishMillis, 250, 10_000);
        serverPublishMillis = clamp(serverPublishMillis, 250, 10_000);
        adaptiveReevaluationMillis = clamp(adaptiveReevaluationMillis, 250, 10_000);
        maxHeavyOperations = 1; // release hard cap; concurrent heavy diagnostics are not proven safe.
        reportDetail = detail(reportDetail, "NORMAL");
        overheadDetail = detail(overheadDetail, "NORMAL");
        guardSensitivity = detail(guardSensitivity, "NORMAL");
        internalMinimumRefreshMillis = clamp(internalMinimumRefreshMillis, 250, 5_000);
        internalMaximumRefreshMillis = clamp(internalMaximumRefreshMillis, internalMinimumRefreshMillis, 10_000);
        overheadWarning = overheadWarning == null ? "" : overheadWarning;
    }
    private static int clamp(int value, int min, int max) { return Math.max(min, Math.min(max, value)); }
    private static String detail(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }
}
