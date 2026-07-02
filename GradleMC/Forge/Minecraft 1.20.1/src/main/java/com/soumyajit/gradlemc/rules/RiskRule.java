package com.soumyajit.gradlemc.rules;

import com.soumyajit.gradlemc.check.Severity;

import java.util.List;

public record RiskRule(
        String id,
        RiskRuleType type,
        String modId,
        List<String> modIds,
        int maxPresent,
        String configFile,
        boolean expectExists,
        Severity severity,
        String message,
        String suggestion
) {
}
