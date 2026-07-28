package com.soumyajit.gradlemc.foundation;

import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.Callable;

public record GameThreadRequest<T>(String id, String taskName, GameThreadTarget target,
                                   Duration timeout, TaskCore.CancellationToken cancellation, Callable<T> capture) {
    public GameThreadRequest {
        if (id == null || !id.matches("[a-z0-9][a-z0-9-]{0,63}")) throw new IllegalArgumentException("Invalid request id");
        if (taskName == null || taskName.isBlank() || taskName.length() > 96 || target == null || timeout == null
                || timeout.isZero() || timeout.isNegative() || capture == null) throw new IllegalArgumentException("Invalid game-thread request");
        cancellation = cancellation == null ? new TaskCore.CancellationToken() : cancellation;
    }
}
