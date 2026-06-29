package com.soumyajit.gradlemc.util;

import java.nio.file.Path;

public final class GradleMcPathsSelfTest {
    private GradleMcPathsSelfTest() {
    }

    public static void run() {
        Path gameDir = Path.of("build", "self-test-game").toAbsolutePath().normalize();
        Path root = GradleMcPaths.resolveGradleMcRoot(gameDir);
        assertEquals(gameDir.resolve("gradlemc").normalize(), root, "output root should be gameDir/gradlemc");
        assertTrue(root.startsWith(gameDir), "output root must stay under the game directory");
        assertTrue(!root.toString().contains("config" + java.io.File.separator + "gradlemc"),
                "new output root must not be config/gradlemc");
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
