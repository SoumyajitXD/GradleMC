package com.soumyajit.gradlemc.investigation.session;

import com.soumyajit.gradlemc.foundation.TaskCore;
import com.soumyajit.gradlemc.investigation.InvestigationProfileId;
import com.soumyajit.gradlemc.investigation.InvestigationRuntimeIdentity;
import com.soumyajit.gradlemc.investigation.planning.InvestigationPlanningContext;
import java.util.Objects;

/** Snapshot-only request; callers must not retain Minecraft live objects here. */
public record InvestigationSessionRequest(InvestigationProfileId profileId,
                                          InvestigationPlanningContext planningContext,
                                          InvestigationRuntimeIdentity runtimeIdentity,
                                          String inputFingerprint,
                                          InvestigationSessionPolicy policy) {
    public InvestigationSessionRequest {
        profileId = Objects.requireNonNull(profileId, "profileId");
        planningContext = Objects.requireNonNull(planningContext, "planningContext");
        runtimeIdentity = Objects.requireNonNull(runtimeIdentity, "runtimeIdentity");
        inputFingerprint = inputFingerprint == null ? "" : inputFingerprint;
        if (inputFingerprint.length() > 1024) throw new IllegalArgumentException("input fingerprint is too long");
        policy = policy == null ? InvestigationSessionPolicy.SAFE_DEFAULT : policy;
    }
}
