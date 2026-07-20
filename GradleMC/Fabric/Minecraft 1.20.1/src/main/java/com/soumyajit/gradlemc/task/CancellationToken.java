package com.soumyajit.gradlemc.task;

import java.util.concurrent.atomic.AtomicReference;

/** Idempotent workflow-wide cancellation signal. */
public final class CancellationToken {
    private final AtomicReference<String> reason = new AtomicReference<>();
    public boolean cancel(String value) { return reason.compareAndSet(null, value == null || value.isBlank() ? "cancelled" : value); }
    public boolean isCancelled() { return reason.get() != null || Thread.currentThread().isInterrupted(); }
    public String reason() { return reason.get() == null ? "" : reason.get(); }
    public void throwIfCancelled() {
        if (isCancelled()) throw new DiagnosticCancelledException(reason().isBlank() ? "interrupted" : reason());
    }
}
