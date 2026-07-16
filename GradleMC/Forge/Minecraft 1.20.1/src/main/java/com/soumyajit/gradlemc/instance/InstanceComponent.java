package com.soumyajit.gradlemc.instance;

import java.time.Instant;
import java.util.Objects;

/** Immutable, privacy-safe provider result shared by commands, tasks, scans, and UI. */
public record InstanceComponent<T>(ComponentAvailability availability, String source, Instant collectedAt,
                                   String side, ComponentScope scope, boolean fresh, String limitation, T value) {
    public InstanceComponent {
        availability = Objects.requireNonNull(availability, "availability");
        source = source == null ? "unknown" : source;
        collectedAt = Objects.requireNonNull(collectedAt, "collectedAt");
        side = side == null ? "unknown" : side;
        scope = Objects.requireNonNull(scope, "scope");
        limitation = limitation == null ? "" : limitation;
    }

    public static <T> InstanceComponent<T> unavailable(String source, ComponentScope scope, String limitation) {
        return new InstanceComponent<>(ComponentAvailability.UNAVAILABLE, source, Instant.now(), "common", scope, true, limitation, null);
    }
}
