package com.soumyajit.gradlemc.profiler;

import java.util.Locale;

public enum ProfilerMode {
    TICK,
    CPU_LITE,
    MEMORY_LITE,
    COMBINED;

    public boolean recordsTicks() {
        return this == TICK || this == COMBINED;
    }

    public boolean samplesCpu() {
        return this == CPU_LITE || this == COMBINED;
    }

    public boolean recordsMemory() {
        return this == MEMORY_LITE || this == COMBINED;
    }

    public static ProfilerMode parse(String value) {
        if (value == null || value.isBlank()) {
            return COMBINED;
        }
        return switch (value.trim().toLowerCase(Locale.ROOT)) {
            case "tick" -> TICK;
            case "cpu-lite", "cpulite", "cpu" -> CPU_LITE;
            case "memory-lite", "memorylite", "memory" -> MEMORY_LITE;
            case "combined" -> COMBINED;
            default -> throw new IllegalArgumentException("Unknown profiler mode: " + value);
        };
    }

    public String id() {
        return name().toLowerCase(Locale.ROOT).replace('_', '-');
    }
}
