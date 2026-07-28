package com.soumyajit.gradlemc.investigation.profile;

import com.soumyajit.gradlemc.investigation.InvestigationProfileId;
import java.util.List;
import java.util.Objects;

/** Immutable compiled declaration. No task dependencies or capabilities are copied here. */
public record InvestigationProfile(InvestigationProfileId id, String nameKey, String description,
                                   List<InvestigationProfileTask> roots, BudgetPreset budget,
                                   SideEligibility sideEligibility, List<String> evidenceCategories,
                                   List<String> limitations, int declarationVersion) {
    public enum BudgetPreset { QUICK, STANDARD }
    public enum SideEligibility { ANY, CLIENT, SERVER }
    public InvestigationProfile {
        id = Objects.requireNonNull(id, "id");
        if (blank(nameKey) || blank(description) || budget == null || sideEligibility == null || declarationVersion < 1)
            throw new IllegalArgumentException("Profile identity, text, budget, side and version are required");
        roots = List.copyOf(roots == null ? List.of() : roots);
        evidenceCategories = List.copyOf(evidenceCategories == null ? List.of() : evidenceCategories);
        limitations = List.copyOf(limitations == null ? List.of() : limitations);
    }
    private static boolean blank(String value) { return value == null || value.isBlank(); }
}
