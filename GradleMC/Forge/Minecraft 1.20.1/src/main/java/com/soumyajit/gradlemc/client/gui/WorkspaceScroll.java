package com.soumyajit.gradlemc.client.gui;

/** Pure bounded scroll arithmetic; kept independent of Minecraft for deterministic tests. */
public final class WorkspaceScroll {
    private WorkspaceScroll() { }
    public static int clamp(int offset, int contentHeight, int viewportHeight) {
        return Math.max(0, Math.min(Math.max(0, contentHeight - Math.max(0, viewportHeight)), offset));
    }
}
