package com.soumyajit.gradlemc.profiler;

import java.util.Locale;

public enum ProfilerThreadScope {
    SERVER, RENDER, ALL, CUSTOM;

    public static ProfilerThreadScope fromPattern(String value) {
        String normalized = value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
        return switch (normalized) {
            case "server", "server thread" -> SERVER;
            case "render", "render thread" -> RENDER;
            case "*", "all" -> ALL;
            default -> CUSTOM;
        };
    }
}
