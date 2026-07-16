package com.soumyajit.gradlemc.task;

/** Best-effort GradleMC execution overhead, not process CPU accounting. */
public record TaskOverhead(
        long wallNanos,
        long gameThreadNanos,
        long workerThreadNanos,
        long filesInspected,
        long bytesRead,
        long samplesCollected,
        long recordsProduced,
        long estimatedRetainedBytes,
        long outputBytes,
        long cancellationLatencyMillis,
        boolean timedOut,
        boolean truncated,
        String truncationReason
) {
    public TaskOverhead {
        truncationReason = truncationReason == null ? "" : truncationReason;
    }

    public static TaskOverhead empty() {
        return new TaskOverhead(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, false, false, "");
    }
}
