package com.soumyajit.gradlemc.check.impl;

import com.soumyajit.gradlemc.check.CheckCategory;
import com.soumyajit.gradlemc.check.CheckContext;
import com.soumyajit.gradlemc.check.CheckResult;
import com.soumyajit.gradlemc.check.Severity;
import com.soumyajit.gradlemc.check.StabilityCheck;

import java.util.List;

public class MemoryInfoCheck implements StabilityCheck {
    private static final long MIB = 1024L * 1024L;

    @Override
    public String name() {
        return "Memory info";
    }

    @Override
    public List<CheckResult> run(CheckContext context) {
        Runtime runtime = Runtime.getRuntime();
        long max = runtime.maxMemory() / MIB;
        long total = runtime.totalMemory() / MIB;
        long free = runtime.freeMemory() / MIB;
        long used = total - free;
        double pressure = max <= 0 ? 0.0D : (double) used / max;
        Severity severity = pressure >= 0.95D ? Severity.CRITICAL : pressure >= 0.80D ? Severity.WARN : Severity.PASS;
        return List.of(CheckResult.of(
                severity,
                CheckCategory.PERFORMANCE,
                "Runtime memory snapshot",
                "Used " + used + " MiB, free " + free + " MiB, allocated " + total + " MiB, max " + max + " MiB",
                "This is a point-in-time JVM snapshot, not a full memory leak analysis."
        ));
    }
}
