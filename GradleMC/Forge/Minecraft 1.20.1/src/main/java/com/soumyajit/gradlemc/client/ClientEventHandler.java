package com.soumyajit.gradlemc.client;

import com.soumyajit.gradlemc.GradleMC;
import com.soumyajit.gradlemc.client.gui.GradleMCGuiOpener;
import com.soumyajit.gradlemc.client.input.GradleMCKeyMappings;
import com.soumyajit.gradlemc.client.overlay.GradleMCStatsOverlay;
import com.soumyajit.gradlemc.client.overlay.OverlayConfigActions;
import com.soumyajit.gradlemc.command.FpsTestCommandBridge;
import com.soumyajit.gradlemc.network.GradleMCGuiBridge;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = GradleMC.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class ClientEventHandler {
    private static boolean wasInWorld;
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

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            Minecraft minecraft = Minecraft.getInstance();
            boolean inWorld = minecraft.level != null && minecraft.player != null;
            if (inWorld != wasInWorld) {
                GradleMCStatsOverlay.resetFpsMeasurement();
                if (!inWorld) {
                    FpsTestManager.pause();
                }
                wasInWorld = inWorld;
            }
            boolean measuring = isActiveGameplay(minecraft);
            if (!measuring) {
                GradleMCStatsOverlay.pauseFpsMeasurement();
                FpsTestManager.pause();
            }
            FpsTestManager.onClientTick(inWorld);
            handleOpenGuiKey();
            handleOverlayKeys();
        }
    }

    @SubscribeEvent
    public static void onRenderGui(RenderGuiEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (!isActiveGameplay(minecraft)) {
            return;
        }
        long nowNanos = System.nanoTime();
        GradleMCStatsOverlay.onRenderedFrame(nowNanos);
        FpsTestManager.onRenderedFrame(nowNanos);
    }

    private static boolean isActiveGameplay(Minecraft minecraft) {
        return minecraft.level != null && minecraft.player != null && minecraft.screen == null
                && minecraft.isWindowActive() && !minecraft.isPaused();
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
