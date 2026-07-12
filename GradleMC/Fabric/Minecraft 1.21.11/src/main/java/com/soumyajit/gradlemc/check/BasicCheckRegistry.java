package com.soumyajit.gradlemc.check;

import com.soumyajit.gradlemc.check.impl.JavaVersionCheck;
import com.soumyajit.gradlemc.check.impl.LoadedModsCheck;
import com.soumyajit.gradlemc.check.impl.LogNoiseClassificationCheck;
import com.soumyajit.gradlemc.check.impl.MemoryInfoCheck;
import com.soumyajit.gradlemc.check.impl.ConfigSanityCheck;
import com.soumyajit.gradlemc.check.impl.ReportDirectoryCheck;
import com.soumyajit.gradlemc.check.impl.RiskRuleCheck;
import com.soumyajit.gradlemc.check.impl.ServerPerformanceCheck;

import java.util.ArrayList;
import java.util.List;

public final class BasicCheckRegistry {
    private BasicCheckRegistry() {
    }

    public static List<StabilityCheck> defaultChecks() {
        return List.of(
                new JavaVersionCheck(),
                new MemoryInfoCheck(),
                new ServerPerformanceCheck(),
                new LoadedModsCheck(),
                new ReportDirectoryCheck(),
                new ConfigSanityCheck(),
                new RiskRuleCheck(),
                new LogNoiseClassificationCheck()
        );
    }

    public static List<CheckResult> runDefaultChecks(CheckContext context) {
        List<CheckResult> results = new ArrayList<>();
        for (StabilityCheck check : defaultChecks()) {
            try {
                results.addAll(check.run(context));
            } catch (RuntimeException exception) {
                results.add(CheckResult.of(
                        Severity.WARN,
                        CheckCategory.CONFIG,
                        check.name() + " could not complete",
                        exception.getClass().getSimpleName() + ": " + exception.getMessage(),
                        "Keep playing; GradleMC should report failed diagnostics instead of crashing."
                ));
            }
        }
        return results;
    }
}
