package com.soumyajit.gradlemc.investigation.profile;

import com.soumyajit.gradlemc.foundation.TaskId;
import java.util.Objects;

/** A profile-owned root selection; Foundation retains task metadata and dependencies. */
public record InvestigationProfileTask(TaskId taskId, Requirement requirement) {
    public enum Requirement { REQUIRED, OPTIONAL }
    public InvestigationProfileTask {
        taskId = Objects.requireNonNull(taskId, "taskId");
        requirement = Objects.requireNonNull(requirement, "requirement");
    }
}
