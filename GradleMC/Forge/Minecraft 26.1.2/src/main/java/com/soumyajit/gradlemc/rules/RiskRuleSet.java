package com.soumyajit.gradlemc.rules;

import com.soumyajit.gradlemc.check.CheckResult;

import java.nio.file.Path;
import java.time.Instant;
import java.util.List;

public record RiskRuleSet(Path path, List<RiskRule> rules, List<CheckResult> loadResults, Instant loadedAt) {
}
