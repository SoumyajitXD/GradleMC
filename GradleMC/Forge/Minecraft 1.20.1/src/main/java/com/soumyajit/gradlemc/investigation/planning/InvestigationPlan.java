package com.soumyajit.gradlemc.investigation.planning;

import com.soumyajit.gradlemc.investigation.InvestigationProfileId;
import com.soumyajit.gradlemc.investigation.profile.InvestigationProfile;
import java.util.List;
import java.util.Objects;

public record InvestigationPlan(InvestigationProfileId profileId, InvestigationProfile.BudgetPreset budget,
                                InvestigationPlanStatus status, List<InvestigationPlanTask> tasks,
                                List<InvestigationPlanLimitation> limitations) {
    public InvestigationPlan {
        profileId = Objects.requireNonNull(profileId, "profileId"); budget = Objects.requireNonNull(budget, "budget"); status = Objects.requireNonNull(status, "status");
        tasks = List.copyOf(tasks == null ? List.of() : tasks); limitations = List.copyOf(limitations == null ? List.of() : limitations);
    }
}
