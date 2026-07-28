package com.soumyajit.gradlemc.investigation.planning;

import com.soumyajit.gradlemc.foundation.TaskCore;
import com.soumyajit.gradlemc.investigation.profile.InvestigationProfile;
import java.util.Objects;

/** Immutable, live-object-free planning input. Foundation availability remains authoritative. */
public record InvestigationPlanningContext(Side side, TaskCore.Context foundationContext) {
    public enum Side { CLIENT, SERVER, COMMON }
    public InvestigationPlanningContext {
        side = Objects.requireNonNull(side, "side");
        foundationContext = Objects.requireNonNull(foundationContext, "foundationContext");
    }
    public boolean allows(InvestigationProfile.SideEligibility eligibility) {
        return eligibility == InvestigationProfile.SideEligibility.ANY
                || eligibility == InvestigationProfile.SideEligibility.CLIENT && side == Side.CLIENT
                || eligibility == InvestigationProfile.SideEligibility.SERVER && side == Side.SERVER;
    }
}
