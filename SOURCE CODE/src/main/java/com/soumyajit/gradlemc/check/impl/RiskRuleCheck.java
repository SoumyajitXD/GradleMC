package com.soumyajit.gradlemc.check.impl;

import com.soumyajit.gradlemc.check.CheckCategory;
import com.soumyajit.gradlemc.check.CheckContext;
import com.soumyajit.gradlemc.check.CheckResult;
import com.soumyajit.gradlemc.check.Severity;
import com.soumyajit.gradlemc.check.StabilityCheck;
import com.soumyajit.gradlemc.config.GradleMCConfig;
import com.soumyajit.gradlemc.rules.RiskRule;
import com.soumyajit.gradlemc.rules.RiskRuleLoader;
import com.soumyajit.gradlemc.rules.RiskRuleSet;
import com.soumyajit.gradlemc.rules.RiskRuleType;
import com.soumyajit.gradlemc.util.GradleMcPaths;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.loading.FMLEnvironment;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

public class RiskRuleCheck implements StabilityCheck {
    @Override
    public String name() {
        return "Risk rules";
    }

    @Override
    public List<CheckResult> run(CheckContext context) {
        List<CheckResult> results = new ArrayList<>();
        if (!GradleMCConfig.ENABLE_RULE_CHECKS.get()) {
            results.add(CheckResult.of(
                    Severity.INFO,
                    CheckCategory.CONFIG,
                    "Risk rule checks disabled",
                    "enableRuleChecks=false in GradleMC config.",
                    "Enable rule checks if local pack policy checks are desired."
            ));
            return results;
        }

        RiskRuleSet ruleSet = RiskRuleLoader.current();
        results.addAll(ruleSet.loadResults());
        if (ruleSet.rules().isEmpty()) {
            return results;
        }

        Set<String> loadedMods = ModList.get().getMods().stream()
                .map(mod -> mod.getModId().toLowerCase(Locale.ROOT))
                .collect(Collectors.toSet());
        for (RiskRule rule : ruleSet.rules()) {
            evaluate(rule, loadedMods).ifPresent(results::add);
        }
        if (results.stream().noneMatch(RiskRuleCheck::isActionable)) {
            results.add(CheckResult.of(
                    Severity.PASS,
                    CheckCategory.CONFIG,
                    "Risk rules passed",
                    ruleSet.rules().size() + " rule(s) evaluated.",
                    "Keep the local rule file updated for this pack."
            ));
        }
        return results;
    }

    private java.util.Optional<CheckResult> evaluate(RiskRule rule, Set<String> loadedMods) {
        return switch (rule.type()) {
            case MOD_PRESENT -> loadedMods.contains(rule.modId()) ? hit(rule) : java.util.Optional.empty();
            case MOD_MISSING -> !loadedMods.contains(rule.modId()) ? hit(rule) : java.util.Optional.empty();
            case CLIENT_ONLY_ON_SERVER -> FMLEnvironment.dist == Dist.DEDICATED_SERVER && loadedMods.contains(rule.modId())
                    ? hit(rule)
                    : java.util.Optional.empty();
            case SERVER_ONLY_ON_CLIENT -> FMLEnvironment.dist == Dist.CLIENT && loadedMods.contains(rule.modId())
                    ? hit(rule)
                    : java.util.Optional.empty();
            case MOD_GROUP_COUNT -> evaluateGroup(rule, loadedMods);
            case CONFIG_FILE_EXISTS -> evaluateConfigFile(rule);
        };
    }

    private java.util.Optional<CheckResult> evaluateGroup(RiskRule rule, Set<String> loadedMods) {
        List<String> present = rule.modIds().stream()
                .filter(loadedMods::contains)
                .toList();
        if (present.size() <= rule.maxPresent()) {
            return java.util.Optional.empty();
        }
        return java.util.Optional.of(CheckResult.of(
                rule.severity(),
                CheckCategory.MODS,
                rule.message(),
                present.size() + " matching mods present: " + String.join(", ", present),
                rule.suggestion()
        ));
    }

    private java.util.Optional<CheckResult> evaluateConfigFile(RiskRule rule) {
        Path configPath = GradleMcPaths.configDirectory().resolve(rule.configFile()).normalize();
        if (!configPath.startsWith(GradleMcPaths.configDirectory())) {
            return java.util.Optional.of(CheckResult.of(
                    Severity.WARN,
                    CheckCategory.CONFIG,
                    "Risk rule path rejected",
                    rule.id() + " tried to inspect outside the config directory.",
                    "Keep config_file_exists paths relative to the Minecraft config directory."
            ));
        }
        boolean exists = Files.exists(configPath);
        if (exists == rule.expectExists()) {
            return hit(rule);
        }
        return java.util.Optional.empty();
    }

    private java.util.Optional<CheckResult> hit(RiskRule rule) {
        CheckCategory category = rule.type() == RiskRuleType.CONFIG_FILE_EXISTS ? CheckCategory.CONFIG : CheckCategory.MODS;
        return java.util.Optional.of(CheckResult.of(
                rule.severity(),
                category,
                rule.message(),
                "Rule " + rule.id() + " matched (" + rule.type().name().toLowerCase(Locale.ROOT) + ").",
                rule.suggestion()
        ));
    }

    private static boolean isActionable(CheckResult result) {
        return result.severity() == Severity.WARN
                || result.severity() == Severity.FAIL
                || result.severity() == Severity.CRITICAL;
    }
}
