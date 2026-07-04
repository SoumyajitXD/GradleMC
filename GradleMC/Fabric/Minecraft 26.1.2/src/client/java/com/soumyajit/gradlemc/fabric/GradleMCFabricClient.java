package com.soumyajit.gradlemc.fabric;

import com.soumyajit.gradlemc.client.FpsTestManager;
import com.soumyajit.gradlemc.client.gui.GradleMCGuiOpener;
import com.soumyajit.gradlemc.client.input.GradleMCKeyMappings;
import com.soumyajit.gradlemc.client.overlay.GradleMCStatsOverlay;
import com.soumyajit.gradlemc.client.overlay.OverlayConfigActions;
import com.soumyajit.gradlemc.command.FpsTestCommandBridge;
import com.soumyajit.gradlemc.network.GradleMCClientNetwork;
import com.soumyajit.gradlemc.network.GradleMCGuiBridge;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

import java.util.Locale;

public final class GradleMCFabricClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        KeyMappingHelper.registerKeyMapping(GradleMCKeyMappings.OPEN_GUI);
        KeyMappingHelper.registerKeyMapping(GradleMCKeyMappings.TOGGLE_OVERLAY);
        KeyMappingHelper.registerKeyMapping(GradleMCKeyMappings.CYCLE_OVERLAY_POSITION);
        KeyMappingHelper.registerKeyMapping(GradleMCKeyMappings.TOGGLE_OVERLAY_MODE);
        KeyMappingHelper.registerKeyMapping(GradleMCKeyMappings.START_STOP_QUICK_FPS_SAMPLE);

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

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            FpsTestManager.onClientTick();
            handleOpenGuiKey(client);
            handleOverlayKeys(client);
        });
        HudElementRegistry.addLast(
                Identifier.fromNamespaceAndPath("gradlemc", "stats_overlay"),
                (graphics, deltaTracker) -> GradleMCStatsOverlay.render(graphics, deltaTracker.getGameTimeDeltaPartialTick(false))
        );
        GradleMCClientNetwork.registerReceivers();
    }

    private static void handleOpenGuiKey(Minecraft minecraft) {
        while (GradleMCKeyMappings.OPEN_GUI.consumeClick()) {
            if (minecraft.player != null && minecraft.screen == null) {
                GradleMCGuiOpener.open();
            }
        }
    }

    private static void handleOverlayKeys(Minecraft minecraft) {
        while (GradleMCKeyMappings.TOGGLE_OVERLAY.consumeClick()) {
            boolean enabled = OverlayConfigActions.toggleEnabled();
            showClientStatus(minecraft, Component.literal("GradleMC stats overlay " + (enabled ? "enabled." : "disabled.")));
        }
        while (GradleMCKeyMappings.CYCLE_OVERLAY_POSITION.consumeClick()) {
            String position = OverlayConfigActions.cyclePosition();
            showClientStatus(minecraft, Component.literal("GradleMC overlay position: " + position.toLowerCase(Locale.ROOT).replace('_', '-')));
        }
        while (GradleMCKeyMappings.TOGGLE_OVERLAY_MODE.consumeClick()) {
            String mode = OverlayConfigActions.toggleMode();
            showClientStatus(minecraft, Component.literal("GradleMC overlay mode: " + mode.toLowerCase(Locale.ROOT)));
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
            minecraft.player.sendOverlayMessage(message);
        }
    }
}
