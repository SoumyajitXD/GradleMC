package com.soumyajit.gradlemc.investigation.planning;

import com.soumyajit.gradlemc.foundation.TaskCore;
import com.soumyajit.gradlemc.foundation.TaskId;
import com.soumyajit.gradlemc.investigation.profile.InvestigationProfileTask;
import java.util.List;
import java.util.Optional;

/** Explanation view over a Foundation plan node; it is not an executable task. */
public record InvestigationPlanTask(TaskId taskId, Role role, Optional<InvestigationProfileTask.Requirement> requirement,
                                    boolean available, String foundationReason, String profileInterpretation,
                                    List<TaskCore.Reason> foundationReasons) {
    public enum Role { REQUIRED_ROOT, OPTIONAL_ROOT, DEPENDENCY }
    public InvestigationPlanTask {
        requirement = requirement == null ? Optional.empty() : requirement;
        foundationReason = foundationReason == null ? "" : foundationReason;
        profileInterpretation = profileInterpretation == null ? "" : profileInterpretation;
        foundationReasons = List.copyOf(foundationReasons == null ? List.of() : foundationReasons);
    }
}
