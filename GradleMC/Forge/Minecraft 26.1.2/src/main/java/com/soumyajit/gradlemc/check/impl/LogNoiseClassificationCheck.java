package com.soumyajit.gradlemc.check.impl;

import com.soumyajit.gradlemc.check.CheckCategory;
import com.soumyajit.gradlemc.check.CheckContext;
import com.soumyajit.gradlemc.check.CheckResult;
import com.soumyajit.gradlemc.check.Severity;
import com.soumyajit.gradlemc.check.StabilityCheck;
import com.soumyajit.gradlemc.util.GradleMcPaths;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;

public class LogNoiseClassificationCheck implements StabilityCheck {
    private static final int MAX_TAIL_LINES = 500;

    @Override
    public String name() {
        return "Log noise classification";
    }

    @Override
    public List<CheckResult> run(CheckContext context) {
        Path latestLog = GradleMcPaths.gameDirectory().resolve("logs").resolve("latest.log").normalize();
        Path gameDir = GradleMcPaths.gameDirectory();
        if (!latestLog.startsWith(gameDir) || !Files.isRegularFile(latestLog)) {
            return List.of();
        }
        List<String> tail;
        try {
            tail = tail(latestLog);
        } catch (IOException exception) {
            return List.of(CheckResult.of(
                    Severity.INFO,
                    CheckCategory.CONFIG,
                    "Latest log was not classified",
                    "Could not read bounded latest.log tail: " + safeMessage(exception),
                    "This does not affect GradleMC reports."
            ));
        }

        List<CheckResult> results = new ArrayList<>();
        if (containsAny(tail, "api.minecraftservices.com", "authentication servers are down", "mojang")) {
            results.add(note(
                    "External auth or network failures noted",
                    "latest.log mentions Mojang/Minecraft service connectivity. This is environmental/network evidence, not a GradleMC failure.",
                    "Retry when network/auth services are available; do not treat this alone as modpack instability."
            ));
        }
        if (containsAny(tail, "modrinth", "github.com", "updater", "update check")) {
            results.add(note(
                    "Updater/network checks noted",
                    "latest.log mentions updater or remote version checks. These are low-confidence environmental notes unless they crash the game.",
                    "Disable noisy updater checks or ignore them unless a stack trace points to a real failure."
            ));
        }
        if (containsAny(tail, "embeddium", "oculus", "taint", "modifying internals")) {
            results.add(note(
                    "Rendering compatibility risk noted",
                    "latest.log mentions Embeddium/Oculus/internal rendering compatibility warnings.",
                    "Treat this as compatibility context. Reproduce with rendering mods isolated before assigning blame."
            ));
        }
        if (containsAny(tail, "classnotfoundexception", "noclassdeffounderror", "optional")) {
            results.add(note(
                    "Missing optional compatibility classes noted",
                    "latest.log mentions missing classes. These are often optional compatibility probes unless followed by a crash.",
                    "Look for a crash or fatal error before treating optional missing classes as the cause."
            ));
        }
        if (containsAny(tail, "failed to load function", "failed to parse recipe", "couldn't parse recipe", "datapack")) {
            results.add(note(
                    "Datapack or content errors noted",
                    "latest.log mentions recipe/function/datapack content errors. These usually point to pack content/config issues.",
                    "Inspect the named datapack, recipe, or function if gameplay content is broken."
            ));
        }
        return results;
    }

    private static List<String> tail(Path path) throws IOException {
        try (Stream<String> stream = Files.lines(path, StandardCharsets.UTF_8)) {
            List<String> lines = stream.toList();
            int from = Math.max(0, lines.size() - MAX_TAIL_LINES);
            return lines.subList(from, lines.size());
        }
    }

    private static boolean containsAny(List<String> lines, String... needles) {
        for (String line : lines) {
            String lower = line.toLowerCase(Locale.ROOT);
            for (String needle : needles) {
                if (lower.contains(needle.toLowerCase(Locale.ROOT))) {
                    return true;
                }
            }
        }
        return false;
    }

    private static CheckResult note(String title, String detail, String suggestion) {
        return CheckResult.of(Severity.INFO, CheckCategory.CONFIG, title, detail, suggestion);
    }

    private static String safeMessage(Exception exception) {
        String message = exception.getMessage();
        return message == null || message.isBlank() ? exception.getClass().getSimpleName() : message;
    }
}
