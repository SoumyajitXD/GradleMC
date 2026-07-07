package com.soumyajit.gradlemc.client.gui;

import net.minecraft.network.chat.Component;

public enum GradleMCGuiSection {
    OVERVIEW("screen.gradlemc.nav.overview"),
    QUICK_ACTIONS("screen.gradlemc.nav.quick_actions"),
    TESTS("screen.gradlemc.nav.tests"),
    PROFILER("screen.gradlemc.nav.profiler"),
    REPORTS("screen.gradlemc.nav.reports"),
    SETTINGS("screen.gradlemc.nav.settings"),
    ABOUT("screen.gradlemc.nav.about");

    private final Component label;

    GradleMCGuiSection(String translationKey) {
        this.label = Component.translatable(translationKey);
    }

    public Component label() {
        return label;
    }
}
