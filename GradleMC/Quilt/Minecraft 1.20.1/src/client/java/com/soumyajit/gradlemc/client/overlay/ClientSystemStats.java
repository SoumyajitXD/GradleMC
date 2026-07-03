package com.soumyajit.gradlemc.client.overlay;

import org.lwjgl.opengl.GL11;

import java.lang.management.ManagementFactory;
import java.util.Locale;

public final class ClientSystemStats {
    private static final long MIB = 1024L * 1024L;
    private static final java.lang.management.OperatingSystemMXBean BASE_OS_BEAN = ManagementFactory.getOperatingSystemMXBean();
    private static final com.sun.management.OperatingSystemMXBean EXT_OS_BEAN =
            BASE_OS_BEAN instanceof com.sun.management.OperatingSystemMXBean bean ? bean : null;
    private static String gpuRenderer;
    private static String cpuName;

    private ClientSystemStats() {
    }

    public static SystemMemory systemMemory() {
        if (EXT_OS_BEAN == null) {
            return SystemMemory.unavailable();
        }
        long total = EXT_OS_BEAN.getTotalMemorySize();
        long free = EXT_OS_BEAN.getFreeMemorySize();
        if (total <= 0L || free < 0L) {
            return SystemMemory.unavailable();
        }
        return new SystemMemory((total - free) / MIB, total / MIB, true);
    }

    public static double processCpuLoadPercent() {
        if (EXT_OS_BEAN == null) {
            return -1.0D;
        }
        double load = EXT_OS_BEAN.getProcessCpuLoad();
        if (!Double.isFinite(load) || load < 0.0D) {
            return -1.0D;
        }
        return Math.max(0.0D, Math.min(100.0D, load * 100.0D));
    }

    public static String cpuName() {
        if (cpuName == null) {
            String processor = System.getenv("PROCESSOR_IDENTIFIER");
            if (processor == null || processor.isBlank()) {
                processor = BASE_OS_BEAN.getArch();
            }
            cpuName = processor == null || processor.isBlank() ? "Unavailable" : processor;
        }
        return cpuName;
    }

    public static String gpuRenderer() {
        if (gpuRenderer == null) {
            try {
                String renderer = GL11.glGetString(GL11.GL_RENDERER);
                String vendor = GL11.glGetString(GL11.GL_VENDOR);
                if (renderer == null || renderer.isBlank()) {
                    gpuRenderer = "Unavailable";
                } else if (vendor == null || vendor.isBlank() || renderer.toLowerCase(Locale.ROOT).contains(vendor.toLowerCase(Locale.ROOT))) {
                    gpuRenderer = renderer;
                } else {
                    gpuRenderer = vendor + " " + renderer;
                }
            } catch (RuntimeException exception) {
                gpuRenderer = "Unavailable";
            }
        }
        return gpuRenderer;
    }

    public record SystemMemory(long usedMiB, long totalMiB, boolean available) {
        public static SystemMemory unavailable() {
            return new SystemMemory(0L, 0L, false);
        }
    }
}
