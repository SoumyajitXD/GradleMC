package com.soumyajit.gradlemc.util;

import net.minecraft.network.chat.HoverEvent;

import java.nio.file.Files;
import java.nio.file.Path;

/** Structural tests for security boundaries; fixtures contain synthetic values only. */
public final class StoragePrivacySelfTest {
    private StoragePrivacySelfTest() { }
    public static void run() { rejectsEscapingNames(); rejectsLinkedRootsWhenSupported(); hidesAbsoluteGamePath(); pathComponentsDoNotEmbedAbsoluteGamePaths(); atomicReplacementLeavesACompleteDestination(); failedReplacementPreservesExistingDestination(); redactsSyntheticSecretsDeterministically(); }
    private static void rejectsEscapingNames() {
        Path root = Path.of("build", "gradlemc-selftest-root").toAbsolutePath().normalize();
        require(GradleMcPaths.resolveOwnedFile(root, "report-1.txt").startsWith(root), "safe name must stay inside root");
        for (String name : new String[] { "../outside", "..\\outside", "/etc/passwd", "C:/outside", "\\\\server\\share", "", "CON", "PRN.txt", "x/../y", "x:name", "line\nbreak", "control\u0001", "a".repeat(129) }) {
            try { GradleMcPaths.resolveOwnedFile(root, name); throw new AssertionError("unsafe name accepted: " + name); } catch (IllegalArgumentException expected) { }
        }
    }
    private static void hidesAbsoluteGamePath() {
        Path game = Path.of("C:/Users/synthetic/game").toAbsolutePath().normalize();
        Path output = game.resolve("gradlemc/reports");
        require("gradlemc\\reports".equals(GradleMcPaths.displayPath(output, game)), "owned report paths must not expose the game-directory prefix");
    }
    private static void pathComponentsDoNotEmbedAbsoluteGamePaths() {
        Path game = Path.of("C:/Users/synthetic/game").toAbsolutePath().normalize();
        Path report = game.resolve("gradlemc/reports/report.txt");
        var component = GradleMcPaths.pathComponent("Report: ", report, game);
        var pathSibling = component.getSiblings().get(0);
        String copied = pathSibling.getStyle().getClickEvent().getValue();
        String hovered = pathSibling.getStyle().getHoverEvent().getValue(HoverEvent.Action.SHOW_TEXT).getString();
        require("gradlemc\\reports\\report.txt".equals(copied), "click payload must contain only the owned relative display path");
        require(copied.equals(hovered) && !hovered.contains("Users"), "hover payload must not expose the absolute game-directory prefix");
    }
    private static void rejectsLinkedRootsWhenSupported() {
        Path parent = null;
        try {
            parent = Files.createTempDirectory("gradlemc-link-root-");
            Path target = Files.createDirectories(parent.resolve("target"));
            Path link = parent.resolve("owned");
            Files.createSymbolicLink(link, target);
            try { GradleMcPaths.resolveOwnedFile(link, "report.txt"); throw new AssertionError("symbolic-link root accepted"); }
            catch (IllegalArgumentException expected) { }
        } catch (UnsupportedOperationException | java.nio.file.FileSystemException unavailable) {
            // Windows hosts may deny symbolic-link creation; the direct path-traversal coverage still runs.
        } catch (Exception failure) {
            throw new AssertionError("symbolic-link root test failed", failure);
        } finally {
            if (parent != null) {
                try {
                    Files.deleteIfExists(parent.resolve("owned"));
                    Files.deleteIfExists(parent.resolve("target"));
                    Files.deleteIfExists(parent);
                } catch (Exception ignored) { }
            }
        }
    }
    private static void atomicReplacementLeavesACompleteDestination() {
        try {
            Path root = Files.createTempDirectory("gradlemc-atomic-test-"); Path file = root.resolve("state.txt");
            AtomicFiles.writeUtf8(file, "first"); AtomicFiles.writeUtf8(file, "second");
            require("second".equals(Files.readString(file)), "replacement must expose complete new state");
            try (var entries = Files.list(root)) { require(entries.count() == 1L, "temporary files must be cleaned"); }
            Files.deleteIfExists(file); Files.deleteIfExists(root);
        } catch (Exception failure) { throw new AssertionError("atomic file test failed", failure); }
    }
    private static void failedReplacementPreservesExistingDestination() {
        try {
            Path root = Files.createTempDirectory("gradlemc-atomic-failure-");
            Path file = root.resolve("state.txt");
            Files.writeString(file, "known-valid");
            try {
                AtomicFiles.replace(root.resolve("missing.tmp"), file);
                throw new AssertionError("missing staged file must fail replacement");
            } catch (java.io.IOException expected) {
                require("known-valid".equals(Files.readString(file)), "failed replacement must preserve the previous valid file");
            }
            Files.deleteIfExists(file); Files.deleteIfExists(root);
        } catch (Exception failure) { throw new AssertionError("atomic failure preservation test failed", failure); }
    }
    private static void redactsSyntheticSecretsDeterministically() {
        Path home = Path.of("C:/Users/synthetic");
        RedactionService service = new RedactionService(Path.of("C:/game"), home);
        String input = "Authorization: Bearer synthetic-token\napi_key=synthetic-key\nhttps://example.invalid/api/webhooks/synthetic-hook\n" + home.toAbsolutePath().normalize() + "\\file";
        RedactionService.Result result = service.redact(input);
        require(!result.text().contains("synthetic-token") && !result.text().contains("synthetic-key") && !result.text().contains("synthetic-hook"), "raw synthetic secret must be absent");
        require(result.text().contains("<redacted>") && result.text().contains("<home>"), "redaction markers must retain context");
        require(result.redactionCount() >= 3, "redaction count must reflect categories"); require(result.text().equals(service.redact(input).text()), "redaction must be deterministic");
    }
    private static void require(boolean condition, String message) { if (!condition) throw new AssertionError(message); }
}
