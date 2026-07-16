package com.soumyajit.gradlemc.experiment;

import java.time.Instant;

public record ExperimentRecord(String name, String workflow, Instant createdAt, boolean cancelled,
                               ExperimentSnapshot baseline, ExperimentSnapshot candidate,
                               String manualAction, String rollback) {
    public ExperimentRecord {
        if (!validName(name)) throw new IllegalArgumentException("Experiment names must match [a-z0-9][a-z0-9_-]{0,31}");
        workflow = workflow == null ? "" : workflow;
        manualAction = manualAction == null ? "Make exactly one reversible manual instance change, then rerun the same workflow." : manualAction;
        rollback = rollback == null ? "Restore the changed mod, pack, setting, or configuration to its baseline state." : rollback;
    }
    public static boolean validName(String value) { return value != null && value.matches("[a-z0-9][a-z0-9_-]{0,31}"); }
}
