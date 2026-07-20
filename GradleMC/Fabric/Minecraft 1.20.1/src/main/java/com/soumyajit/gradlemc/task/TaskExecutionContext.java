package com.soumyajit.gradlemc.task;

import java.util.Map;

/** Contains only immutable snapshot values and cancellation/deadline state; never live game objects. */
public record TaskExecutionContext(Map<String, String> snapshot, CancellationToken cancellation, long deadlineNanos) {
    public TaskExecutionContext {
        snapshot = Map.copyOf(snapshot == null ? Map.of() : snapshot);
        if (cancellation == null) throw new IllegalArgumentException("cancellation is required");
    }
    public void checkpoint() {
        cancellation.throwIfCancelled();
        if (System.nanoTime() > deadlineNanos) throw new TaskTimeoutException();
    }
}
