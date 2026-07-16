package com.soumyajit.gradlemc.client.gui;

import net.minecraft.network.chat.Component;

/** The complete, deliberately small, diagnostics workspace information architecture. */
public enum WorkspacePage {
    OVERVIEW("screen.gradlemc.nav.overview"),
    INSTANCE("screen.gradlemc.nav.instance"),
    MODS("screen.gradlemc.nav.mods"),
    PERFORMANCE("screen.gradlemc.nav.performance"),
    TASKS("screen.gradlemc.nav.tasks"),
    ADAPTIVE("screen.gradlemc.nav.adaptive"),
    SCANS("screen.gradlemc.nav.scans"),
    SETTINGS("screen.gradlemc.nav.settings");

    private final String key;

    WorkspacePage(String key) { this.key = key; }

    public Component label() { return Component.translatable(key); }
}
