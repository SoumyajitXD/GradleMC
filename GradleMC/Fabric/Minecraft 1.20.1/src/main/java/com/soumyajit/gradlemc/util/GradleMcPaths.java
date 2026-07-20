package com.soumyajit.gradlemc.util;

import com.soumyajit.gradlemc.config.GradleMCConfig;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public final class GradleMcPaths {
    private GradleMcPaths() {
    }

    public static Path gameDirectory() {
        return FabricLoader.getInstance().getGameDir().normalize();
    }

    public static Path configDirectory() {
        return FabricLoader.getInstance().getConfigDir().normalize();
    }

    public static Path configFile() {
        return resolveOwnedFile(configDirectory().resolve("gradlemc"), "gradlemc.properties");
    }

    public static Path gradleMcDirectory() {
        return resolveGradleMcRoot(gameDirectory());
    }

    public static Path resolveGradleMcRoot(Path gameDirectory) {
        return gameDirectory.toAbsolutePath().normalize().resolve("gradlemc").normalize();
    }

    public static Path reportDirectory() {
        return outputDirectory(GradleMCConfig.REPORT_DIRECTORY_NAME.get(), "reports");
    }

    public static Path exportDirectory() {
        return resolveOwnedDirectory(gradleMcDirectory(), "exports");
    }

    public static Path issueBundleDirectory() {
        return resolveOwnedDirectory(gradleMcDirectory(), "issue-bundles");
    }

    public static Path profileDirectory() {
        return resolveOwnedDirectory(gradleMcDirectory(), "profiles");
    }

    public static Path rulesDirectory() {
        return resolveOwnedDirectory(gradleMcDirectory(), "rules");
    }

    public static Path adaptiveBaselineFile() {
        return resolveOwnedFile(gradleMcDirectory(), "adaptive-baseline.properties");
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
        return displayPath(path, gameDirectory());
    }

    /** Formats a GradleMC-owned path without leaking its absolute game-directory prefix. */
    public static String displayPath(Path path, Path gameDir) {
        Path normalized = path.toAbsolutePath().normalize();
        Path normalizedGameDir = gameDir.toAbsolutePath().normalize();
        if (normalized.startsWith(normalizedGameDir)) {
            return normalizedGameDir.relativize(normalized).toString();
        }
        return normalized.toString();
    }

    public static Component pathComponent(String prefix, Path path) {
        return pathComponent(prefix, path, gameDirectory());
    }

    static Component pathComponent(String prefix, Path path, Path gameDir) {
        String safeDisplayPath = displayPath(path, gameDir);
        return Component.literal(prefix)
                .append(Component.literal(safeDisplayPath)
                        .withStyle(style -> style
                                .withClickEvent(new ClickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, safeDisplayPath))
                                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal(safeDisplayPath)))));
    }

    private static Path outputDirectory(String configuredName, String fallbackName) {
        Path root = gradleMcDirectory();
        String name = safeOutputDirectoryName(configuredName, fallbackName);
        try { return resolveOwnedDirectory(root, name); }
        catch (IllegalArgumentException ignored) { return resolveOwnedDirectory(root, fallbackName); }
    }

    private static String safeOutputDirectoryName(String value, String fallbackName) {
        String name = value;
        if (name == null || name.isBlank() || name.length() > 64 || name.contains("..")) {
            return fallbackName;
        }
        name = name.replace('\\', '/');
        if ("gradlemc/reports".equals(name) || "config/gradlemc/reports".equals(name)) {
            return "reports";
        }
        if (name.startsWith("/") || name.startsWith("\\\\") || name.matches("^[A-Za-z]:.*") || name.contains(":")) {
            return fallbackName;
        }
        if (!name.matches("[A-Za-z0-9._-]+(?:/[A-Za-z0-9._-]+)*")) return fallbackName;
        return name;
    }

    /** Rejects user-controlled escapes before any GradleMC write. Existing links are never followed. */
    public static Path resolveOwnedFile(Path root, String relativeName) {
        if (relativeName == null || relativeName.isBlank() || relativeName.length() > 128
                || relativeName.indexOf('\u0000') >= 0 || relativeName.startsWith("/") || relativeName.startsWith("\\\\")
                || relativeName.matches("^[A-Za-z]:.*") || relativeName.contains("\\") || relativeName.contains("..")
                || !relativeName.matches("[A-Za-z0-9][A-Za-z0-9._-]*") || reservedWindowsName(relativeName)) {
            throw new IllegalArgumentException("Unsafe GradleMC relative filename");
        }
        Path normalizedRoot = root.toAbsolutePath().normalize();
        Path candidate = normalizedRoot.resolve(relativeName).normalize();
        if (!candidate.startsWith(normalizedRoot)) throw new IllegalArgumentException("GradleMC path escapes its root");
        rejectExistingSymlink(normalizedRoot, candidate);
        return candidate;
    }

    public static Path resolveOwnedDirectory(Path root, String relativeName) {
        if (relativeName == null || relativeName.isBlank() || relativeName.length() > 128
                || relativeName.indexOf('\u0000') >= 0 || relativeName.startsWith("/") || relativeName.startsWith("\\\\")
                || relativeName.matches("^[A-Za-z]:.*") || relativeName.contains("\\") || relativeName.contains("..")
                || !relativeName.matches("[A-Za-z0-9][A-Za-z0-9._-]*(?:/[A-Za-z0-9][A-Za-z0-9._-]*)*") || trailingWindowsDotOrSpace(relativeName)) {
            throw new IllegalArgumentException("Unsafe GradleMC relative directory");
        }
        Path normalizedRoot = root.toAbsolutePath().normalize();
        Path candidate = normalizedRoot.resolve(relativeName).normalize();
        if (!candidate.startsWith(normalizedRoot)) throw new IllegalArgumentException("GradleMC path escapes its root");
        rejectExistingSymlink(normalizedRoot, candidate);
        return candidate;
    }

    /** Resolves a nested regular-file name without permitting traversal or symbolic-link components. */
    public static Path resolveOwnedRelativeFile(Path root, String relativeName) {
        if (relativeName == null || relativeName.isBlank() || relativeName.length() > 128
                || relativeName.indexOf('\u0000') >= 0 || relativeName.startsWith("/") || relativeName.startsWith("\\\\")
                || relativeName.matches("^[A-Za-z]:.*") || relativeName.contains("\\") || relativeName.contains("..")
                || !relativeName.matches("[A-Za-z0-9][A-Za-z0-9._-]*(?:/[A-Za-z0-9][A-Za-z0-9._-]*)*")) {
            throw new IllegalArgumentException("Unsafe GradleMC relative file path");
        }
        for (String part : relativeName.split("/")) if (reservedWindowsName(part)) throw new IllegalArgumentException("Unsafe GradleMC relative file path");
        Path normalizedRoot = root.toAbsolutePath().normalize();
        Path candidate = normalizedRoot.resolve(relativeName).normalize();
        if (!candidate.startsWith(normalizedRoot)) throw new IllegalArgumentException("GradleMC path escapes its root");
        rejectExistingSymlink(normalizedRoot, candidate);
        return candidate;
    }

    private static void rejectExistingSymlink(Path root, Path candidate) {
        if (Files.isSymbolicLink(root)) throw new IllegalArgumentException("GradleMC paths may not use a symbolic-link root");
        Path cursor = root;
        for (Path segment : root.relativize(candidate)) {
            cursor = cursor.resolve(segment);
            if (Files.isSymbolicLink(cursor)) throw new IllegalArgumentException("GradleMC paths may not traverse symbolic links");
        }
    }

    private static boolean reservedWindowsName(String name) {
        if (name.endsWith(".") || name.endsWith(" ")) return true;
        String base = name.contains(".") ? name.substring(0, name.indexOf('.')) : name;
        return base.matches("(?i)CON|PRN|AUX|NUL|COM[1-9]|LPT[1-9]");
    }

    private static boolean trailingWindowsDotOrSpace(String name) {
        for (String part : name.split("/")) if (reservedWindowsName(part)) return true;
        return false;
    }

    private static void addDistinct(List<Path> paths, Path path) {
        if (!paths.contains(path)) {
            paths.add(path);
        }
    }
}
