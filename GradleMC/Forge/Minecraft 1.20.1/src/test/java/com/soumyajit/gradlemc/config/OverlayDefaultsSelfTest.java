package com.soumyajit.gradlemc.config;

public final class OverlayDefaultsSelfTest {
    private OverlayDefaultsSelfTest() {
    }

    public static void run() {
        assertTrue(!OverlayDefaults.ENABLED, "overlay should be disabled by default");
        assertEquals("COMPACT", OverlayDefaults.MODE, "overlay default mode");
        assertEquals("TOP_LEFT", OverlayDefaults.POSITION, "overlay default position");
        assertEquals(60, OverlayDefaults.SAMPLING_WINDOW_SECONDS, "overlay default sampling window");
        assertEquals(500, OverlayDefaults.UPDATE_INTERVAL_MS, "overlay default update interval");
        assertTrue(!OverlayDefaults.SHOW_TITLE, "overlay title should be disabled by default");
        assertTrue(!OverlayDefaults.SHOW_AVERAGE_FPS, "average FPS should be disabled by default");
        assertTrue(!OverlayDefaults.SHOW_GPU_USAGE, "GPU usage should not be enabled without an accurate provider");
    }

    private static void assertEquals(Object expected, Object actual, String message) {
        if (!expected.equals(actual)) {
            throw new AssertionError(message + " expected=" + expected + " actual=" + actual);
        }
    }

    private static void assertTrue(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
