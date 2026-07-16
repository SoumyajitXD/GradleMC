package com.soumyajit.gradlemc.util;

import com.soumyajit.gradlemc.config.GradleMCConfig;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraftforge.fml.loading.FMLPaths;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public final class GradleMcPaths {
    private GradleMcPaths() {
    }

    public static Path gameDirectory() {
        return FMLPaths.GAMEDIR.get().normalize();
    }

    public static Path configDirectory() {
        return FMLPaths.CONFIGDIR.get().normalize();
    }

    public static Path gradleMcDirectory() {
        return resolveGradleMcRoot(gameDirectory());
    }

    public static Path resolveGradleMcRoot(Path gameDirectory) {
        return gameDirectory.normalize().resolve("gradlemc").normalize();
    }

    public static Path reportDirectory() {
        return outputDirectory(GradleMCConfig.REPORT_DIRECTORY_NAME.get(), "reports");
    }

    public static Path exportDirectory() {
        return gradleMcDirectory().resolve("exports").normalize();
    }

    public static Path issueBundleDirectory() {
        return gradleMcDirectory().resolve("issue-bundles").normalize();
    }

    public static Path profileDirectory() {
        return gradleMcDirectory().resolve("profiles").normalize();
    }

    public static Path experimentDirectory() {
        return gradleMcDirectory().resolve("experiments").normalize();
    }

    public static Path incidentDirectory() {
        return gradleMcDirectory().resolve("incidents").normalize();
    }

    public static Path instanceLockFile() {
        return gradleMcDirectory().resolve("gradlemc-instance-lock.json").normalize();
    }

    public static Path rulesDirectory() {
        return gradleMcDirectory().resolve("rules").normalize();
    }

    public static Path adaptiveBaselineFile() {
        return gradleMcDirectory().resolve("adaptive-baseline.properties").normalize();
    }

    public static Path legacyGradleMcDirectory() {
        return configDirectory().resolve("gradlemc").normalize();
    }

    public static Path legacyReportDirectory() {
        return legacyGradleMcDirectory().resolve("reports").normalize();
    }

    public static List<Path> reportSearchDirectories() {
        List<Path> directories = new ArrayList<>();
        addDistinct(directories, reportDirectory());
        addDistinct(directories, exportDirectory());
        addDistinct(directories, legacyReportDirectory());
        return List.copyOf(directories);
    }

    public static String displayPath(Path path) {
        Path normalized = path.normalize();
        Path gameDir = gameDirectory();
        if (normalized.startsWith(gameDir)) {
            return gameDir.relativize(normalized).toString();
        }
        return normalized.toString();
    }

    public static Component pathComponent(String prefix, Path path) {
        String safePath = displayPath(path);
        return Component.literal(prefix)
                .append(Component.literal(safePath)
                        .withStyle(style -> style
                                .withClickEvent(new ClickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, safePath))
                                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal(safePath)))));
    }

    private static Path outputDirectory(String configuredName, String fallbackName) {
        Path root = gradleMcDirectory();
        String name = safeOutputDirectoryName(configuredName, fallbackName);
        Path configured = root.resolve(name).normalize();
        if (!configured.startsWith(root)) {
            return root.resolve(fallbackName).normalize();
        }
        return configured;
    }

    static String safeOutputDirectoryName(String value, String fallbackName) {
        String name = value;
        if (name == null || name.isBlank()) {
            return fallbackName;
        }
        if ("gradlemc/reports".equals(name) || "config/gradlemc/reports".equals(name)) {
            return "reports";
        }
        if (!name.matches("[A-Za-z0-9][A-Za-z0-9._-]{0,63}") || ".".equals(name) || "..".equals(name)) {
            return fallbackName;
        }
        return name;
    }

    private static void addDistinct(List<Path> paths, Path path) {
        if (!paths.contains(path)) {
            paths.add(path);
        }
    }
}
