package com.soumyajit.gradlemc.client.overlay;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Pure title/FPS composition so presentation choices are testable without a Minecraft client. */
public final class OverlayLineComposer {
    private OverlayLineComposer() {
    }

    public static List<String> compose(boolean compact, boolean showTitle, boolean showFps, boolean showAverageFps,
                                       FpsRollingStatsCalculator.Snapshot fps) {
        List<String> lines = new ArrayList<>();
        if (compact) {
            List<String> parts = new ArrayList<>();
            if (showTitle) {
                parts.add("GradleMC");
            }
            if (showFps) {
                parts.add(whole(fps.currentFps()) + " FPS");
            }
            if (showAverageFps) {
                parts.add("avg " + whole(fps.averageFps()));
            }
            if (!parts.isEmpty()) {
                lines.add(String.join(" | ", parts));
            }
            return List.copyOf(lines);
        }
        if (showTitle) {
            lines.add("GradleMC");
        }
        if (showFps) {
            lines.add("FPS: " + whole(fps.currentFps()));
        }
        if (showAverageFps) {
            lines.add("Average FPS: " + whole(fps.averageFps()));
        }
        return List.copyOf(lines);
    }

    private static String whole(double value) {
        return String.format(Locale.ROOT, "%.0f", Double.isFinite(value) ? Math.max(0.0D, value) : 0.0D);
    }
}
