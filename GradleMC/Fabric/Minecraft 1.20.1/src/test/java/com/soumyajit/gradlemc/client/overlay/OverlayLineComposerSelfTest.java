package com.soumyajit.gradlemc.client.overlay;

public final class OverlayLineComposerSelfTest {
    private OverlayLineComposerSelfTest() {
    }

    public static void run() {
        FpsRollingStatsCalculator.Snapshot fps = new FpsRollingStatsCalculator.Snapshot(1, 120.0D, 90.0D, null, null);
        require(OverlayLineComposer.compose(true, false, false, false, fps).isEmpty(),
                "all compact title/FPS controls disabled must create no layout residue");
        require(OverlayLineComposer.compose(false, true, false, false, fps).equals(java.util.List.of("GradleMC")),
                "branding must be independently configurable");
        require(OverlayLineComposer.compose(false, false, true, false, fps).equals(java.util.List.of("FPS: 120")),
                "current FPS must not force average FPS");
        require(OverlayLineComposer.compose(false, false, false, true, fps).equals(java.util.List.of("Average FPS: 90")),
                "average FPS must be independently configurable");
        require(OverlayLineComposer.compose(false, false, true, true, fps).equals(java.util.List.of("FPS: 120", "Average FPS: 90")),
                "current and average FPS row order must be deterministic");
        FpsRollingStatsCalculator.Snapshot unavailable = FpsRollingStatsCalculator.Snapshot.empty();
        require(OverlayLineComposer.compose(false, false, false, true, unavailable).equals(java.util.List.of("Average FPS: unavailable")),
                "average FPS without samples must remain unavailable rather than zero");
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
