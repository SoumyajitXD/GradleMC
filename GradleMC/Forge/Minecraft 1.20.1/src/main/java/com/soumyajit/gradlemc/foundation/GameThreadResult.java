package com.soumyajit.gradlemc.foundation;

import java.time.Duration;
import java.util.Optional;

/** Immutable completion result. Errors are deliberately bounded to a type name. */
public record GameThreadResult<T>(String requestId, GameThreadBridgeStatus status, Optional<T> value,
                                  Duration queueDelay, Duration captureDuration, String detail) {
    public GameThreadResult {
        value = value == null ? Optional.empty() : value;
        queueDelay = queueDelay == null || queueDelay.isNegative() ? Duration.ZERO : queueDelay;
        captureDuration = captureDuration == null || captureDuration.isNegative() ? Duration.ZERO : captureDuration;
        detail = detail == null ? "" : detail.substring(0, Math.min(192, detail.length()));
    }
}
