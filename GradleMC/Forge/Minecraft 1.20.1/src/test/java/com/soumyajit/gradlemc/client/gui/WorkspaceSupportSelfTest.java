package com.soumyajit.gradlemc.client.gui;

/** Deterministic checks for the pure responsive geometry and bounded scrolling helpers. */
public final class WorkspaceSupportSelfTest {
    private WorkspaceSupportSelfTest() { }
    public static void run() {
        for (int[] size : new int[][] {{854,480},{1280,720},{1920,1080},{320,240}}) {
            WorkspaceLayout layout = WorkspaceLayout.calculate(size[0], size[1]);
            if (layout.contentWidth() <= 0 || layout.contentHeight() <= 0 || layout.footerTop() <= layout.headerBottom()) throw new AssertionError("invalid layout");
            if (layout.contentTop() + layout.contentHeight() > layout.footerTop()) throw new AssertionError("content overlaps footer");
        }
        if (WorkspaceScroll.clamp(-2, 100, 20) != 0) throw new AssertionError("negative scroll");
        if (WorkspaceScroll.clamp(100, 30, 40) != 0) throw new AssertionError("short content");
        if (WorkspaceScroll.clamp(100, 100, 20) != 80) throw new AssertionError("long content");
        if (WorkspaceScroll.clamp(70, 20, 10) != 10) throw new AssertionError("data shrink");
    }
}
