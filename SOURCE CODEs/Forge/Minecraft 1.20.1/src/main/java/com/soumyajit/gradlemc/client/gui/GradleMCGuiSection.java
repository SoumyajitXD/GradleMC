package com.soumyajit.gradlemc.client.gui;

import net.minecraft.network.chat.Component;

public enum GradleMCGuiSection {
    OVERVIEW("screen.gradlemc.nav.overview"),
    SMART_AI("screen.gradlemc.nav.smart_ai"),
    SETTINGS("screen.gradlemc.nav.settings"),
    COMMANDS("screen.gradlemc.nav.commands"),
    ABOUT("screen.gradlemc.nav.about");

    private final Component label;

    GradleMCGuiSection(String translationKey) {
        this.label = Component.translatable(translationKey);
    }

    public Component label() {
        return label;
    }
}
