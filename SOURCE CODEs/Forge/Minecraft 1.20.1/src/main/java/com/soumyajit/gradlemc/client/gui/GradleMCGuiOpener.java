package com.soumyajit.gradlemc.client.gui;

import net.minecraft.client.Minecraft;

public final class GradleMCGuiOpener {
    private GradleMCGuiOpener() {
    }

    public static void open() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) {
            return;
        }
        if (minecraft.screen instanceof GradleMCScreen) {
            return;
        }
        minecraft.setScreen(new GradleMCScreen());
    }
}
