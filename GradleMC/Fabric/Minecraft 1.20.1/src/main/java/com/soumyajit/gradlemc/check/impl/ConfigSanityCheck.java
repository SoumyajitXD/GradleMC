package com.soumyajit.gradlemc.check.impl;

import com.soumyajit.gradlemc.check.CheckCategory;
import com.soumyajit.gradlemc.check.CheckContext;
import com.soumyajit.gradlemc.check.CheckResult;
import com.soumyajit.gradlemc.check.Severity;
import com.soumyajit.gradlemc.check.StabilityCheck;
import com.soumyajit.gradlemc.config.GradleMCConfig;
import com.soumyajit.gradlemc.rules.RiskRuleLoader;
import com.soumyajit.gradlemc.util.GradleMcPaths;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;

public class ConfigSanityCheck implements StabilityCheck {
    private static final int MAX_FILES_INSPECTED = 500;
    private static final long HUGE_CONFIG_BYTES = 5L * 1024L * 1024L;

    @Override
    public String name() {
        return "Config sanity";
    }

    @Override
    public List<CheckResult> run(CheckContext context) {
        List<CheckResult> results = new ArrayList<>();
        Path configDir = GradleMcPaths.configDirectory();
        if (!Files.exists(configDir)) {
            return List.of(CheckResult.of(
                    Severity.FAIL,
                    CheckCategory.CONFIG,
                    "Config directory is missing",
                    GradleMcPaths.displayPath(configDir),
                    "Start Minecraft/Fabric once or check the game directory."
            ));
        }
        if (!Files.isDirectory(configDir)) {
            return List.of(CheckResult.of(
                    Severity.FAIL,
                    CheckCategory.CONFIG,
                    "Config path is not a directory",
                    GradleMcPaths.displayPath(configDir),
                    "Check the Minecraft config path."
            ));
        }
        if (!Files.isReadable(configDir)) {
            return List.of(CheckResult.of(
                    Severity.WARN,
                    CheckCategory.CONFIG,
                    "Config directory is not readable",
                    GradleMcPaths.displayPath(configDir),
                    "Check file permissions."
            ));
        }

        results.add(CheckResult.of(
                Severity.PASS,
                CheckCategory.CONFIG,
                "Config directory is readable",
                GradleMcPaths.displayPath(configDir),
                "GradleMC only scans this directory for lightweight config diagnostics."
        ));

        Path gradleMcConfig = GradleMCConfig.SPEC.path();
        results.add(CheckResult.of(
                Files.isRegularFile(gradleMcConfig) ? Severity.PASS : Severity.INFO,
                CheckCategory.CONFIG,
                "GradleMC Fabric config file " + (Files.isRegularFile(gradleMcConfig) ? "is present" : "will be created when a setting changes"),
                GradleMcPaths.displayPath(gradleMcConfig),
                "Fabric uses safe defaults and writes only explicit GradleMC settings."
        ));
        Path rulesPath = RiskRuleLoader.rulesPath();
        results.add(CheckResult.of(
                Files.isRegularFile(rulesPath) ? Severity.PASS : Severity.INFO,
                CheckCategory.CONFIG,
                "Risk rule file " + (Files.isRegularFile(rulesPath) ? "is present" : "is missing"),
                GradleMcPaths.displayPath(rulesPath),
                "Missing rule files are allowed. Run /gradlemc rules example for a template."
        ));
        return results;
    }

    public static List<Path> listConfigFiles(int limit) {
        List<Path> owned = List.of(GradleMCConfig.SPEC.path(), RiskRuleLoader.rulesPath());
        return owned.stream().filter(Files::isRegularFile).limit(Math.max(1, limit)).toList();
    }

    public static long countConfigFiles() {
        return listConfigFiles(2).size();
    }
}
