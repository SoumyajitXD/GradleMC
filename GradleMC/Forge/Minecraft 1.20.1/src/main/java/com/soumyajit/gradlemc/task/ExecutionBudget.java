package com.soumyajit.gradlemc.task;

import com.soumyajit.gradlemc.config.GradleMCConfig;

/** Internal safety ceilings. Reaching a ceiling produces explicit partial evidence. */
public record ExecutionBudget(
        int maxTasks,
        long maxRunMillis,
        long maxFilesInspected,
        long maxBytesRead,
        long maxSamples,
        long maxRecords,
        long maxOutputBytes
) {
    public ExecutionBudget {
        if (maxTasks < 1 || maxRunMillis < 1 || maxFilesInspected < 0 || maxBytesRead < 0
                || maxSamples < 0 || maxRecords < 0 || maxOutputBytes < 0) {
            throw new IllegalArgumentException("Execution budget values are out of range");
        }
    }

    public static ExecutionBudget defaults() {
        return new ExecutionBudget(128, 300_000, 10_000, 256L * 1024 * 1024,
                100_000, 100_000, 16L * 1024 * 1024);
    }
    public static ExecutionBudget configured(){try{return new ExecutionBudget(GradleMCConfig.BUDGET_MAX_TASKS.get(),GradleMCConfig.BUDGET_MAX_RUN_SECONDS.get()*1000L,GradleMCConfig.BUDGET_MAX_FILES.get(),GradleMCConfig.BUDGET_MAX_READ_MIB.get()*1024L*1024L,GradleMCConfig.BUDGET_MAX_RECORDS.get(),GradleMCConfig.BUDGET_MAX_RECORDS.get(),GradleMCConfig.BUDGET_MAX_SCAN_MIB.get()*1024L*1024L);}catch(RuntimeException exception){return defaults();}}
}
