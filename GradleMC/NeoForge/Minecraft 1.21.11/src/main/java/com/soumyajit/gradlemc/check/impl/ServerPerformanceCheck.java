package com.soumyajit.gradlemc.check.impl;

import com.soumyajit.gradlemc.check.CheckCategory;
import com.soumyajit.gradlemc.check.CheckContext;
import com.soumyajit.gradlemc.check.CheckResult;
import com.soumyajit.gradlemc.check.Severity;
import com.soumyajit.gradlemc.check.StabilityCheck;

import java.util.List;
import java.util.Locale;

public final class ServerPerformanceCheck implements StabilityCheck {
    @Override
    public String name() { return "Server performance"; }

    @Override
    public List<CheckResult> run(CheckContext context) {
        if (context.server() == null) {
            return List.of(CheckResult.of(Severity.INFO, CheckCategory.PERFORMANCE,
                    "Server tick sample unavailable", "No MinecraftServer was available in this command context.",
                    "Run performance diagnostics from an active server command context."));
        }
        double averageTickTime = Math.max(0.0D, context.server().getAverageTickTimeNanos() / 1_000_000.0D);
        Severity severity = averageTickTime >= 50.0D ? Severity.FAIL : averageTickTime >= 40.0D ? Severity.WARN : Severity.PASS;
        return List.of(CheckResult.of(severity, CheckCategory.PERFORMANCE, "Server tick health",
                String.format(Locale.ROOT, "Average server tick time is %.2f ms", averageTickTime),
                severity == Severity.PASS ? "No sustained server-tick pressure is visible in this lightweight snapshot."
                        : "Use /gradlemc perf start <seconds> for a bounded sample before changing the modpack."));
    }
}
