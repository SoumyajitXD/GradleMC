package com.soumyajit.gradlemc.client.gui;

/** Compatibility name for integrations compiled against the former section enum. */
@Deprecated(forRemoval = false)
public enum GradleMCGuiSection {
    OVERVIEW, INSTANCE, MODS, PERFORMANCE, TASKS, ADAPTIVE, SCANS, SETTINGS;
    public WorkspacePage page() { return WorkspacePage.valueOf(name()); }
}
