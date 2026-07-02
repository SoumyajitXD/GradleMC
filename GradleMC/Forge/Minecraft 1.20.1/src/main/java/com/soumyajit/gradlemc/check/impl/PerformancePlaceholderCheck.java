package com.soumyajit.gradlemc.check.impl;

import com.soumyajit.gradlemc.check.CheckCategory;
import com.soumyajit.gradlemc.check.CheckContext;
import com.soumyajit.gradlemc.check.CheckResult;
import com.soumyajit.gradlemc.check.Severity;
import com.soumyajit.gradlemc.check.StabilityCheck;

import java.util.List;

public class PerformancePlaceholderCheck implements StabilityCheck {
    @Override
    public String name() {
        return "Performance placeholder";
    }

    @Override
    public List<CheckResult> run(CheckContext context) {
        if (context.server() == null) {
            return List.of(CheckResult.of(
                    Severity.INFO,
                    CheckCategory.PERFORMANCE,
                    "Server tick sample unavailable",
                    "No MinecraftServer was available in this command context.",
                    "Run performance diagnostics from an active server command context."
            ));
        }
        double averageTickTime = context.server().getAverageTickTime();
        return List.of(CheckResult.of(
                Severity.INFO,
                CheckCategory.PERFORMANCE,
                "Server tick sample",
                "Average tick time is approximately " + String.format("%.2f", averageTickTime) + " ms",
                "Use this only as a lightweight snapshot until a bounded benchmark command is implemented."
        ));
    }
}
