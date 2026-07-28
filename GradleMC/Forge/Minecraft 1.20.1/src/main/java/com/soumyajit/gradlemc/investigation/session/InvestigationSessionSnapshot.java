package com.soumyajit.gradlemc.investigation.session;

import com.soumyajit.gradlemc.investigation.InvestigationId;
import com.soumyajit.gradlemc.investigation.InvestigationProfileId;
import com.soumyajit.gradlemc.investigation.planning.InvestigationPlan;
import java.time.Instant;
import java.util.List;
import java.util.Objects;

public record InvestigationSessionSnapshot(InvestigationId id, InvestigationProfileId profileId,
                                           InvestigationSessionStatus status, InvestigationPlan plan,
                                           Instant createdAt, Instant updatedAt, int completedTasks,
                                           int totalTasks, List<String> limitations, String detail,
                                           boolean persistenceSynchronized) {
    public InvestigationSessionSnapshot {
        id = Objects.requireNonNull(id, "id"); profileId = Objects.requireNonNull(profileId, "profileId");
        status = Objects.requireNonNull(status, "status"); plan = Objects.requireNonNull(plan, "plan");
        createdAt = Objects.requireNonNull(createdAt, "createdAt"); updatedAt = Objects.requireNonNull(updatedAt, "updatedAt");
        if (completedTasks < 0 || totalTasks < 0 || completedTasks > totalTasks) throw new IllegalArgumentException("invalid progress");
        limitations = List.copyOf(limitations == null ? List.of() : limitations);
        detail = detail == null ? "" : detail.substring(0, Math.min(512, detail.length()));
    }
}
