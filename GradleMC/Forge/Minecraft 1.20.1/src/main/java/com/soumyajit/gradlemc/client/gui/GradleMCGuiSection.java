package com.soumyajit.gradlemc.client.gui;

public enum GradleMCGuiSection {
    OVERVIEW("screen.gradlemc.nav.overview"),
    QUICK_ACTIONS("screen.gradlemc.nav.quick_actions"),
    TESTS("screen.gradlemc.nav.tests"),
    PROFILER("screen.gradlemc.nav.profiler"),
    REPORTS("screen.gradlemc.nav.reports"),
    SETTINGS("screen.gradlemc.nav.settings"),
    ABOUT("screen.gradlemc.nav.about");

    private final String labelKey;
    GradleMCGuiSection(String labelKey) { this.labelKey = labelKey; }
    public net.minecraft.network.chat.Component label() { return net.minecraft.network.chat.Component.translatable(labelKey); }
}
