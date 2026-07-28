package com.soumyajit.gradlemc.client.gui;

import com.soumyajit.gradlemc.config.GradleMCConfig;
import com.soumyajit.gradlemc.config.OverlayDefaults;
import com.soumyajit.gradlemc.client.overlay.GradleMCStatsOverlay;
import net.minecraftforge.common.ForgeConfigSpec;

import java.util.EnumMap;
import java.util.Map;

/**
 * Client-only, in-memory settings transaction.  Forge config values are not
 * touched until {@link #apply()} succeeds, so Escape/Back can never save a
 * partially edited overlay configuration.
 */
final class OverlaySettingsDraft {
    enum Key {
        ENABLED, TITLE, FPS, AVERAGE_FPS, ONE_PERCENT_LOW, POINT_ONE_PERCENT_LOW,
        JVM_MEMORY, SYSTEM_MEMORY, CPU, GPU_NAME, GPU_USAGE, INTEGRATED_SERVER,
        TEST_STATUS, PROFILER_STATUS, STABILITY, BACKGROUND
    }

    private final EnumMap<Key, Boolean> persisted;
    private final EnumMap<Key, Boolean> values;
    private String mode;
    private String position;
    private double scale;
    private double opacity;
    private int samplingWindow;
    private int updateInterval;
    private String persistedMode;
    private String persistedPosition;
    private double persistedScale;
    private double persistedOpacity;
    private int persistedSamplingWindow;
    private int persistedUpdateInterval;

    private OverlaySettingsDraft() {
        persisted = readBooleans();
        values = new EnumMap<>(persisted);
        mode = persistedMode = GradleMCConfig.OVERLAY_MODE.get();
        position = persistedPosition = GradleMCConfig.OVERLAY_POSITION.get();
        scale = persistedScale = GradleMCConfig.OVERLAY_SCALE.get();
        opacity = persistedOpacity = GradleMCConfig.OVERLAY_BACKGROUND_OPACITY.get();
        samplingWindow = persistedSamplingWindow = GradleMCConfig.OVERLAY_SAMPLING_WINDOW_SECONDS.get();
        updateInterval = persistedUpdateInterval = GradleMCConfig.OVERLAY_UPDATE_INTERVAL_MS.get();
    }

    static OverlaySettingsDraft open() { return new OverlaySettingsDraft(); }
    boolean value(Key key) { return values.get(key); }
    void toggle(Key key) { values.put(key, !value(key)); }
    String mode() { return mode; }
    void mode(String value) { mode = value; }
    String position() { return position; }
    void position(String value) { position = value; }
    double scale() { return scale; }
    void scale(double value) { scale = clamp(value, .75D, 2D); }
    double opacity() { return opacity; }
    void opacity(double value) { opacity = clamp(value, 0D, 1D); }
    int samplingWindow() { return samplingWindow; }
    void samplingWindow(int value) { samplingWindow = value == 30 || value == 60 || value == 120 ? value : 60; }
    int updateInterval() { return updateInterval; }
    void updateInterval(int value) { updateInterval = value == 250 || value == 500 || value == 1000 ? value : 500; }

    boolean dirty() {
        return !values.equals(persisted) || !mode.equals(persistedMode) || !position.equals(persistedPosition)
                || Double.compare(scale, persistedScale) != 0 || Double.compare(opacity, persistedOpacity) != 0
                || samplingWindow != persistedSamplingWindow || updateInterval != persistedUpdateInterval;
    }

    void resetToDefaults() {
        values.replaceAll((key, ignored) -> switch (key) {
            case ENABLED -> OverlayDefaults.ENABLED; case TITLE -> OverlayDefaults.SHOW_TITLE;
            case FPS -> OverlayDefaults.SHOW_FPS; case AVERAGE_FPS -> OverlayDefaults.SHOW_AVERAGE_FPS;
            case ONE_PERCENT_LOW -> OverlayDefaults.SHOW_ONE_PERCENT_LOW;
            case POINT_ONE_PERCENT_LOW -> OverlayDefaults.SHOW_POINT_ONE_PERCENT_LOW;
            case JVM_MEMORY -> OverlayDefaults.SHOW_JVM_MEMORY; case SYSTEM_MEMORY -> OverlayDefaults.SHOW_SYSTEM_MEMORY;
            case CPU -> OverlayDefaults.SHOW_CPU; case GPU_NAME -> OverlayDefaults.SHOW_GPU_NAME;
            case GPU_USAGE -> OverlayDefaults.SHOW_GPU_USAGE; case INTEGRATED_SERVER -> OverlayDefaults.SHOW_INTEGRATED_SERVER;
            case TEST_STATUS -> OverlayDefaults.SHOW_TEST_STATUS; case PROFILER_STATUS -> OverlayDefaults.SHOW_PROFILER_STATUS;
            case STABILITY -> OverlayDefaults.SHOW_STABILITY; case BACKGROUND -> OverlayDefaults.BACKGROUND;
        });
        mode = OverlayDefaults.MODE; position = OverlayDefaults.POSITION; scale = OverlayDefaults.SCALE;
        opacity = OverlayDefaults.OPACITY; samplingWindow = OverlayDefaults.SAMPLING_WINDOW_SECONDS;
        updateInterval = OverlayDefaults.UPDATE_INTERVAL_MS;
    }

    /** Applies all validated values together; a save failure restores prior in-memory values. */
    void apply() {
        Map<Key, Boolean> before = readBooleans();
        String beforeMode = GradleMCConfig.OVERLAY_MODE.get(), beforePosition = GradleMCConfig.OVERLAY_POSITION.get();
        double beforeScale = GradleMCConfig.OVERLAY_SCALE.get(), beforeOpacity = GradleMCConfig.OVERLAY_BACKGROUND_OPACITY.get();
        int beforeWindow = GradleMCConfig.OVERLAY_SAMPLING_WINDOW_SECONDS.get(), beforeInterval = GradleMCConfig.OVERLAY_UPDATE_INTERVAL_MS.get();
        try {
            values.forEach((key, value) -> spec(key).set(value));
            GradleMCConfig.OVERLAY_MODE.set(mode); GradleMCConfig.OVERLAY_POSITION.set(position);
            GradleMCConfig.OVERLAY_SCALE.set(clamp(scale, .75D, 2D)); GradleMCConfig.OVERLAY_BACKGROUND_OPACITY.set(clamp(opacity, 0D, 1D));
            GradleMCConfig.OVERLAY_SAMPLING_WINDOW_SECONDS.set(samplingWindow); GradleMCConfig.OVERLAY_UPDATE_INTERVAL_MS.set(updateInterval);
            GradleMCConfig.SPEC.save();
            GradleMCStatsOverlay.onSettingsChanged();
            persisted.clear(); persisted.putAll(values); persistedMode = mode; persistedPosition = position;
            persistedScale = scale; persistedOpacity = opacity; persistedSamplingWindow = samplingWindow; persistedUpdateInterval = updateInterval;
        } catch (RuntimeException exception) {
            before.forEach((key, value) -> spec(key).set(value));
            GradleMCConfig.OVERLAY_MODE.set(beforeMode); GradleMCConfig.OVERLAY_POSITION.set(beforePosition);
            GradleMCConfig.OVERLAY_SCALE.set(beforeScale); GradleMCConfig.OVERLAY_BACKGROUND_OPACITY.set(beforeOpacity);
            GradleMCConfig.OVERLAY_SAMPLING_WINDOW_SECONDS.set(beforeWindow); GradleMCConfig.OVERLAY_UPDATE_INTERVAL_MS.set(beforeInterval);
            throw exception;
        }
    }

    private static EnumMap<Key, Boolean> readBooleans() {
        EnumMap<Key, Boolean> result = new EnumMap<>(Key.class);
        for (Key key : Key.values()) result.put(key, spec(key).get());
        return result;
    }
    private static ForgeConfigSpec.BooleanValue spec(Key key) {
        return switch (key) {
            case ENABLED -> GradleMCConfig.OVERLAY_ENABLED; case TITLE -> GradleMCConfig.OVERLAY_SHOW_TITLE;
            case FPS -> GradleMCConfig.OVERLAY_SHOW_FPS; case AVERAGE_FPS -> GradleMCConfig.OVERLAY_SHOW_AVERAGE_FPS;
            case ONE_PERCENT_LOW -> GradleMCConfig.OVERLAY_SHOW_ONE_PERCENT_LOW; case POINT_ONE_PERCENT_LOW -> GradleMCConfig.OVERLAY_SHOW_POINT_ONE_PERCENT_LOW;
            case JVM_MEMORY -> GradleMCConfig.OVERLAY_SHOW_JVM_MEMORY; case SYSTEM_MEMORY -> GradleMCConfig.OVERLAY_SHOW_SYSTEM_MEMORY;
            case CPU -> GradleMCConfig.OVERLAY_SHOW_CPU; case GPU_NAME -> GradleMCConfig.OVERLAY_SHOW_GPU_NAME;
            case GPU_USAGE -> GradleMCConfig.OVERLAY_SHOW_GPU_USAGE; case INTEGRATED_SERVER -> GradleMCConfig.OVERLAY_SHOW_INTEGRATED_SERVER;
            case TEST_STATUS -> GradleMCConfig.OVERLAY_SHOW_TEST_STATUS; case PROFILER_STATUS -> GradleMCConfig.OVERLAY_SHOW_PROFILER_STATUS;
            case STABILITY -> GradleMCConfig.OVERLAY_SHOW_STABILITY; case BACKGROUND -> GradleMCConfig.OVERLAY_BACKGROUND_ENABLED;
        };
    }
    private static double clamp(double value, double min, double max) { return Math.max(min, Math.min(max, value)); }
}
