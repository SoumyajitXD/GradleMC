package com.soumyajit.gradlemc.task;

import java.time.Instant;

/** Provenance attached to a result payload; consumers must not present reused static data as live evidence. */
public record EvidenceProvenance(String producingTaskId, String executionId, Instant collectedAt,
                                 Freshness freshness, TaskSide physicalSide, boolean dynamic,
                                 boolean reused, String unit, Availability availability,
                                 double confidenceContribution) {
    public enum Freshness { FRESH, STATIC_REUSED, STALE, UNKNOWN }
    public enum Availability { AVAILABLE, MISSING, UNAVAILABLE }

    public EvidenceProvenance {
        producingTaskId = producingTaskId == null ? "" : producingTaskId;
        executionId = executionId == null ? "" : executionId;
        freshness = freshness == null ? Freshness.UNKNOWN : freshness;
        physicalSide = physicalSide == null ? TaskSide.ANY : physicalSide;
        unit = unit == null ? "" : unit;
        availability = availability == null ? Availability.MISSING : availability;
        confidenceContribution = Math.max(0.0d, Math.min(1.0d, confidenceContribution));
    }
}
