package com.soumyajit.gradlemc.check.impl;

import com.soumyajit.gradlemc.check.CheckCategory;
import com.soumyajit.gradlemc.check.CheckContext;
import com.soumyajit.gradlemc.check.CheckResult;
import com.soumyajit.gradlemc.check.Severity;
import com.soumyajit.gradlemc.check.StabilityCheck;
import com.soumyajit.gradlemc.config.GradleMCConfig;
import com.soumyajit.gradlemc.util.GradleMcPaths;

import java.io.IOException;
import java.nio.file.Files;
import java.util.List;

public class ReportDirectoryCheck implements StabilityCheck {
    @Override
    public String name() {
        return "Report directory";
    }

    @Override
    public List<CheckResult> run(CheckContext context) {
        if (!GradleMCConfig.REPORTS_ENABLED.get()) {
            return List.of(CheckResult.of(
                    Severity.INFO,
                    CheckCategory.FILES,
                    "Report exports disabled",
                    "reportsEnabled=false in GradleMC config.",
                    "Enable reports if text exports are needed."
            ));
        }
        try {
            Files.createDirectories(context.reportDirectory());
            boolean writable = Files.isWritable(context.reportDirectory());
            return List.of(CheckResult.of(
                    writable ? Severity.PASS : Severity.WARN,
                    CheckCategory.FILES,
                    "Report directory " + (writable ? "is writable" : "is not writable"),
                    GradleMcPaths.displayPath(context.reportDirectory()),
                    writable ? "Reports can be exported here." : "Check file permissions for the GradleMC output directory."
            ));
        } catch (IOException exception) {
            return List.of(CheckResult.of(
                    Severity.FAIL,
                    CheckCategory.FILES,
                    "Report directory could not be prepared",
                    exception.getMessage(),
                    "Check file permissions for the GradleMC output directory."
            ));
        }
    }
}
