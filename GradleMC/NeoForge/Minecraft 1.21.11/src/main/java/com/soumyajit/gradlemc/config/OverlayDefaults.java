package com.soumyajit.gradlemc.config;

public final class OverlayDefaults {
    public static final boolean ENABLED = false;
    public static final String MODE = "COMPACT";
    public static final String POSITION = "TOP_LEFT";
    public static final double SCALE = 1.0D;
    public static final boolean BACKGROUND = true;
    public static final double OPACITY = 0.55D;
    public static final int SAMPLING_WINDOW_SECONDS = 60;
    public static final int UPDATE_INTERVAL_MS = 500;
    public static final boolean SHOW_FPS = true;
    public static final boolean SHOW_ONE_PERCENT_LOW = true;
    public static final boolean SHOW_POINT_ONE_PERCENT_LOW = true;
    public static final boolean SHOW_JVM_MEMORY = true;
    public static final boolean SHOW_SYSTEM_MEMORY = false;
    public static final boolean SHOW_CPU = false;
    public static final boolean SHOW_GPU_NAME = true;
    public static final boolean SHOW_GPU_USAGE = false;

    private OverlayDefaults() {
    }
}
