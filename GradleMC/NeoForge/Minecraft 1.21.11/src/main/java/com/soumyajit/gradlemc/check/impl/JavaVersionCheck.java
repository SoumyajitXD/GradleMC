package com.soumyajit.gradlemc.check.impl;

import com.soumyajit.gradlemc.check.CheckCategory;
import com.soumyajit.gradlemc.check.CheckContext;
import com.soumyajit.gradlemc.check.CheckResult;
import com.soumyajit.gradlemc.check.Severity;
import com.soumyajit.gradlemc.check.StabilityCheck;

import java.util.List;

public class JavaVersionCheck implements StabilityCheck {
    @Override
    public String name() {
        return "Java version";
    }

    @Override
    public List<CheckResult> run(CheckContext context) {
        int feature = Runtime.version().feature();
        Severity severity = feature >= 21 ? Severity.PASS : Severity.FAIL;
        String suggestion = feature >= 21
                ? "Java meets the Minecraft 1.21.11 baseline."
                : "Run this modpack with Java 21 or newer.";
        return List.of(CheckResult.of(
                severity,
                CheckCategory.JAVA,
                "Java " + feature + " detected",
                System.getProperty("java.version", "unknown"),
                suggestion
        ));
    }
}
