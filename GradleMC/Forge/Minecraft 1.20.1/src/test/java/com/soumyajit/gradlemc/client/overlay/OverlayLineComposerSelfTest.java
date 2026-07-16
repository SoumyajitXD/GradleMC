package com.soumyajit.gradlemc.client.overlay;

import java.util.List;

public final class OverlayLineComposerSelfTest {
    private static final FpsRollingStatsCalculator.Snapshot FPS =
            new FpsRollingStatsCalculator.Snapshot(60, 60.0D, 58.0D, null, null);

    private OverlayLineComposerSelfTest() {
    }

    public static void run() {
        detailedVisibilityCombinations();
        compactVisibilityCombinations();
    }

    private static void detailedVisibilityCombinations() {
        assertLines(List.of(), false, false, false, "everything disabled");
        assertLines(List.of("GradleMC"), true, false, false, "title only");
        assertLines(List.of("FPS: 60"), false, true, false, "current only");
        assertLines(List.of("Average FPS: 58"), false, false, true, "average only");
        assertLines(List.of("FPS: 60", "Average FPS: 58"), false, true, true, "both FPS values");
        assertLines(List.of("GradleMC", "FPS: 60"), true, true, false, "title plus current");
        assertLines(List.of("GradleMC", "Average FPS: 58"), true, false, true, "title plus average");
        assertLines(List.of("GradleMC", "FPS: 60", "Average FPS: 58"), true, true, true, "all FPS content");
    }

    private static void compactVisibilityCombinations() {
        assertCompact("", false, false, false);
        assertCompact("GradleMC", true, false, false);
        assertCompact("60 FPS", false, true, false);
        assertCompact("avg 58", false, false, true);
        assertCompact("GradleMC | 60 FPS | avg 58", true, true, true);
    }

    private static void assertLines(List<String> expected, boolean title, boolean current, boolean average, String message) {
        List<String> actual = OverlayLineComposer.compose(false, title, current, average, FPS);
        if (!expected.equals(actual)) throw new AssertionError(message + " expected=" + expected + " actual=" + actual);
    }

    private static void assertCompact(String expected, boolean title, boolean current, boolean average) {
        List<String> actual = OverlayLineComposer.compose(true, title, current, average, FPS);
        List<String> expectedLines = expected.isEmpty() ? List.of() : List.of(expected);
        if (!expectedLines.equals(actual)) throw new AssertionError("compact expected=" + expectedLines + " actual=" + actual);
    }
}
