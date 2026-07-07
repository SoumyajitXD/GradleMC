package com.soumyajit.gradlemc.client;

import com.soumyajit.gradlemc.GradleMC;
import com.soumyajit.gradlemc.client.gui.GradleMCGuiOpener;
import com.soumyajit.gradlemc.client.input.GradleMCKeyMappings;
import com.soumyajit.gradlemc.client.overlay.OverlayConfigActions;
import com.soumyajit.gradlemc.command.FpsTestCommandBridge;
import com.soumyajit.gradlemc.network.GradleMCGuiBridge;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.client.event.ClientTickEvent;

public final class ClientEventHandler {
    static {
        FpsTestCommandBridge.registerClientHandler(new FpsTestCommandBridge.ClientCommandHandler() {
            @Override
            public int start(net.minecraft.commands.CommandSourceStack source, int seconds) {
                return FpsTestManager.start(source, seconds);
            }

            @Override
            public int stop(net.minecraft.commands.CommandSourceStack source) {
                return FpsTestManager.stop(source);
            }
        });
        GradleMCGuiBridge.registerClientOpener(GradleMCGuiOpener::open);
    }

    private ClientEventHandler() {
    }

    public static void onClientTick(ClientTickEvent.Post event) {
        FpsTestManager.onClientTick();
        handleOpenGuiKey();
        handleOverlayKeys();
    }

    private static void handleOpenGuiKey() {
        Minecraft minecraft = Minecraft.getInstance();
        while (GradleMCKeyMappings.OPEN_GUI.consumeClick()) {
            if (minecraft.player != null && minecraft.screen == null) {
                GradleMCGuiOpener.open();
            }
        }
    }

    private static void handleOverlayKeys() {
        Minecraft minecraft = Minecraft.getInstance();
        while (GradleMCKeyMappings.TOGGLE_OVERLAY.consumeClick()) {
            boolean enabled = OverlayConfigActions.toggleEnabled();
            showClientStatus(minecraft, Component.literal("GradleMC stats overlay " + (enabled ? "enabled." : "disabled.")));
        }
        while (GradleMCKeyMappings.CYCLE_OVERLAY_POSITION.consumeClick()) {
            String position = OverlayConfigActions.cyclePosition();
            showClientStatus(minecraft, Component.literal("GradleMC overlay position: " + position.toLowerCase(java.util.Locale.ROOT).replace('_', '-')));
        }
        while (GradleMCKeyMappings.TOGGLE_OVERLAY_MODE.consumeClick()) {
            String mode = OverlayConfigActions.toggleMode();
            showClientStatus(minecraft, Component.literal("GradleMC overlay mode: " + mode.toLowerCase(java.util.Locale.ROOT)));
        }
        while (GradleMCKeyMappings.START_STOP_QUICK_FPS_SAMPLE.consumeClick()) {
            if (FpsTestManager.isRunning()) {
                FpsTestManager.stopFromClient();
            } else {
                FpsTestManager.startFromClient(30);
            }
        }
    }

    private static void showClientStatus(Minecraft minecraft, Component message) {
        if (minecraft.player != null) {
            minecraft.player.displayClientMessage(message, true);
        }
    }
}
