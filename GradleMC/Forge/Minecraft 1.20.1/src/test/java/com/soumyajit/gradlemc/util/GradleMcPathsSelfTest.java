package com.soumyajit.gradlemc.util;

import java.nio.file.Path;
import java.nio.file.Files;
import java.io.IOException;

public final class GradleMcPathsSelfTest {
    private GradleMcPathsSelfTest() {
    }

    public static void run() throws Exception {
        Path gameDir = Path.of("build", "self-test-game").toAbsolutePath().normalize();
        Path root = GradleMcPaths.resolveGradleMcRoot(gameDir);
        assertEquals(gameDir.resolve("gradlemc").normalize(), root, "output root should be gameDir/gradlemc");
        assertTrue(root.startsWith(gameDir), "output root must stay under the game directory");
        assertTrue(!root.toString().contains("config" + java.io.File.separator + "gradlemc"),
                "new output root must not be config/gradlemc");
        assertEquals("reports", GradleMcPaths.safeOutputDirectoryName("nested/reports", "reports"), "nested report directory should be rejected");
        assertEquals("reports", GradleMcPaths.safeOutputDirectoryName("C:\\private", "reports"), "absolute report directory should be rejected");
        assertEquals("my-reports", GradleMcPaths.safeOutputDirectoryName("my-reports", "reports"), "simple report directory should be accepted");
        managedDirectorySafety();
    }

    private static void managedDirectorySafety() throws Exception {
        Path base = Files.createTempDirectory(Path.of("build").toAbsolutePath(), "managed-path-");
        Path outside = Files.createTempDirectory(Path.of("build").toAbsolutePath(), "managed-outside-");
        Path normal = base.resolve("gradlemc").resolve("reports");
        assertEquals(normal, ManagedPathSafety.ensureDirectory(base, normal), "managed directory should be created");
        try {
            ManagedPathSafety.ensureDirectory(base, base.resolve("..").resolve("escape"));
            throw new AssertionError("lexical escape should be rejected");
        } catch (IOException expected) { }
        Path link = base.resolve("linked");
        try {
            Files.createSymbolicLink(link, outside);
            try {
                ManagedPathSafety.ensureDirectory(base, link.resolve("incidents"));
                throw new AssertionError("symlinked component should be rejected");
            } catch (IOException expected) { }
        } catch (UnsupportedOperationException | SecurityException | IOException unavailable) {
            // Windows may deny test symlink creation without Developer Mode; lexical containment remains tested.
        } finally {
            Files.deleteIfExists(link);
            Files.deleteIfExists(normal);
            Files.deleteIfExists(normal.getParent());
            Files.deleteIfExists(base);
            Files.deleteIfExists(outside);
        }
    }

    private static void assertEquals(Object expected, Object actual, String message) {
        if (!expected.equals(actual)) {
            throw new AssertionError(message + " expected=" + expected + " actual=" + actual);
        }
    }

    private static void assertTrue(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
