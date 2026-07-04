package com.soumyajit.gradlemc.check.impl;

import com.soumyajit.gradlemc.check.CheckCategory;
import com.soumyajit.gradlemc.check.CheckContext;
import com.soumyajit.gradlemc.check.CheckResult;
import com.soumyajit.gradlemc.check.Severity;
import com.soumyajit.gradlemc.check.StabilityCheck;
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
                    configDir.toString(),
                    "Start Minecraft/Fabric once or check the game directory."
            ));
        }
        if (!Files.isDirectory(configDir)) {
            return List.of(CheckResult.of(
                    Severity.FAIL,
                    CheckCategory.CONFIG,
                    "Config path is not a directory",
                    configDir.toString(),
                    "Check the Minecraft config path."
            ));
        }
        if (!Files.isReadable(configDir)) {
            return List.of(CheckResult.of(
                    Severity.WARN,
                    CheckCategory.CONFIG,
                    "Config directory is not readable",
                    configDir.toString(),
                    "Check file permissions."
            ));
        }

        results.add(CheckResult.of(
                Severity.PASS,
                CheckCategory.CONFIG,
                "Config directory is readable",
                configDir.toString(),
                "GradleMC only scans this directory for lightweight config diagnostics."
        ));

        scanConfigFiles(configDir, results);
        Path gradleMcConfig = configDir.resolve("gradlemc-common.toml").normalize();
        results.add(CheckResult.of(
                Files.isRegularFile(gradleMcConfig) ? Severity.PASS : Severity.INFO,
                CheckCategory.CONFIG,
                "GradleMC config file " + (Files.isRegularFile(gradleMcConfig) ? "is present" : "will use Fabric defaults until config persistence is ported"),
                gradleMcConfig.toString(),
                "Fabric port currently uses safe built-in defaults; GradleMC does not auto-edit user configs."
        ));
        Path rulesPath = RiskRuleLoader.rulesPath();
        results.add(CheckResult.of(
                Files.isRegularFile(rulesPath) ? Severity.PASS : Severity.INFO,
                CheckCategory.CONFIG,
                "Risk rule file " + (Files.isRegularFile(rulesPath) ? "is present" : "is missing"),
                rulesPath.toString(),
                "Missing rule files are allowed. Run /gradlemc rules example for a template."
        ));
        return results;
    }

    public static List<Path> listConfigFiles(int limit) {
        Path configDir = GradleMcPaths.configDirectory();
        if (!Files.isDirectory(configDir)) {
            return List.of();
        }
        try (Stream<Path> paths = Files.list(configDir)) {
            return paths.filter(Files::isRegularFile)
                    .sorted()
                    .limit(Math.max(1, limit))
                    .toList();
        } catch (IOException exception) {
            return List.of();
        }
    }

    public static long countConfigFiles() {
        Path configDir = GradleMcPaths.configDirectory();
        if (!Files.isDirectory(configDir)) {
            return 0L;
        }
        try (Stream<Path> paths = Files.list(configDir)) {
            return paths.filter(Files::isRegularFile).count();
        } catch (IOException exception) {
            return 0L;
        }
    }

    private static void scanConfigFiles(Path configDir, List<CheckResult> results) {
        int inspected = 0;
        int empty = 0;
        int huge = 0;
        try (Stream<Path> paths = Files.list(configDir)) {
            List<Path> files = paths.filter(Files::isRegularFile)
                    .filter(ConfigSanityCheck::looksLikeConfig)
                    .limit(MAX_FILES_INSPECTED)
                    .toList();
            for (Path path : files) {
                inspected++;
                long size = Files.size(path);
                if (size == 0L) {
                    empty++;
                }
                if (size > HUGE_CONFIG_BYTES) {
                    huge++;
                }
            }
        } catch (IOException exception) {
            results.add(CheckResult.of(
                    Severity.WARN,
                    CheckCategory.CONFIG,
                    "Config files could not be inspected",
                    exception.getMessage(),
                    "GradleMC performs only shallow config file checks."
            ));
            return;
        }

        Severity severity = empty > 0 || huge > 0 ? Severity.WARN : Severity.PASS;
        results.add(CheckResult.of(
                severity,
                CheckCategory.CONFIG,
                "Config file sanity scan",
                inspected + " shallow config file(s) inspected, empty=" + empty + ", huge=" + huge,
                "Empty or very large configs may need manual review. GradleMC does not parse or modify them."
        ));
    }

    private static boolean looksLikeConfig(Path path) {
        String name = path.getFileName().toString().toLowerCase(Locale.ROOT);
        return name.endsWith(".toml")
                || name.endsWith(".json")
                || name.endsWith(".cfg")
                || name.endsWith(".properties");
    }
}
