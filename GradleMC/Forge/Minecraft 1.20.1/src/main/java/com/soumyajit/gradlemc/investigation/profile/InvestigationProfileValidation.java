package com.soumyajit.gradlemc.investigation.profile;

import com.soumyajit.gradlemc.foundation.TaskCore;
import com.soumyajit.gradlemc.foundation.TaskId;
import java.util.*;

/** Declaration checks deliberately consult Foundation's catalog, never a copied task list. */
public final class InvestigationProfileValidation {
    private InvestigationProfileValidation() { }
    public static void validate(List<InvestigationProfile> profiles, Collection<TaskCore.Definition> foundationTasks) {
        if (profiles == null || profiles.isEmpty()) throw new IllegalArgumentException("At least one profile is required");
        Set<TaskId> known = new HashSet<>();
        for (TaskCore.Definition task : foundationTasks == null ? List.<TaskCore.Definition>of() : foundationTasks) known.add(task.id());
        Set<String> ids = new HashSet<>();
        String prior = "";
        for (InvestigationProfile profile : profiles) {
            if (profile == null || !ids.add(profile.id().value())) throw new IllegalArgumentException("Duplicate or null profile ID");
            if (profile.id().value().compareTo(prior) < 0) throw new IllegalArgumentException("Profiles must have stable ID order");
            prior = profile.id().value();
            if (profile.roots().isEmpty()) throw new IllegalArgumentException("Profile has no root tasks: " + profile.id());
            if (profile.evidenceCategories().isEmpty()) throw new IllegalArgumentException("Profile has no evidence categories: " + profile.id());
            if (profile.limitations().size() > 16 || profile.limitations().stream().anyMatch(x -> x == null || x.isBlank() || x.length() > 512)) throw new IllegalArgumentException("Invalid profile limitations");
            Map<TaskId, InvestigationProfileTask.Requirement> selected = new HashMap<>();
            for (InvestigationProfileTask root : profile.roots()) {
                if (!known.contains(root.taskId())) throw new IllegalArgumentException("Unknown Foundation task: " + root.taskId());
                InvestigationProfileTask.Requirement previous = selected.putIfAbsent(root.taskId(), root.requirement());
                if (previous != null) throw new IllegalArgumentException("Duplicate or contradictory profile root: " + root.taskId());
            }
        }
    }
}
