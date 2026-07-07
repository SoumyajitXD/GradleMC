package com.soumyajit.gradlemc.client.overlay;

import java.util.Locale;

public enum OverlayPosition {
    TOP_LEFT,
    TOP_RIGHT,
    BOTTOM_LEFT,
    BOTTOM_RIGHT;

    public static OverlayPosition fromConfig(String value) {
        if (value == null) {
            return TOP_LEFT;
        }
        try {
            return OverlayPosition.valueOf(value.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            return TOP_LEFT;
        }
    }

    public OverlayPosition next() {
        OverlayPosition[] values = values();
        return values[(ordinal() + 1) % values.length];
    }
}
