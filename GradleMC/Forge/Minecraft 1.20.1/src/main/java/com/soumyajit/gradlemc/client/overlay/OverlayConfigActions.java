package com.soumyajit.gradlemc.client.overlay;

import com.soumyajit.gradlemc.config.GradleMCConfig;

public final class OverlayConfigActions {
    private OverlayConfigActions() {
    }

    public static boolean toggleEnabled() {
        boolean enabled = !GradleMCConfig.OVERLAY_ENABLED.get();
        GradleMCConfig.OVERLAY_ENABLED.set(enabled);
        save();
        return enabled;
    }

    public static String cyclePosition() {
        OverlayPosition next = OverlayPosition.fromConfig(GradleMCConfig.OVERLAY_POSITION.get()).next();
        GradleMCConfig.OVERLAY_POSITION.set(next.name());
        save();
        return next.name();
    }

    public static String toggleMode() {
        String next = "DETAILED".equalsIgnoreCase(GradleMCConfig.OVERLAY_MODE.get()) ? "COMPACT" : "DETAILED";
        GradleMCConfig.OVERLAY_MODE.set(next);
        save();
        return next;
    }

    public static void setBoolean(net.minecraftforge.common.ForgeConfigSpec.BooleanValue value, boolean enabled) {
        value.set(enabled);
        save();
    }

    public static void setPosition(OverlayPosition position) {
        GradleMCConfig.OVERLAY_POSITION.set(position.name());
        save();
    }

    public static void setMode(String mode) {
        GradleMCConfig.OVERLAY_MODE.set(mode);
        save();
    }

    public static void setScale(double scale) {
        GradleMCConfig.OVERLAY_SCALE.set(Math.max(0.75D, Math.min(2.0D, scale)));
        save();
    }

    public static void setSamplingWindow(int seconds) {
        GradleMCConfig.OVERLAY_SAMPLING_WINDOW_SECONDS.set(seconds);
        save();
    }

    public static void setUpdateInterval(int millis) {
        GradleMCConfig.OVERLAY_UPDATE_INTERVAL_MS.set(millis);
        save();
    }

    public static void save() {
        GradleMCConfig.SPEC.save();
    }
}
