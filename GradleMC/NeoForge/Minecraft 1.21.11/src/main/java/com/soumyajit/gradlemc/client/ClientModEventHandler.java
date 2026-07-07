package com.soumyajit.gradlemc.client;

import com.soumyajit.gradlemc.GradleMC;
import com.soumyajit.gradlemc.client.input.GradleMCKeyMappings;
import com.soumyajit.gradlemc.client.overlay.GradleMCStatsOverlay;
import com.soumyajit.gradlemc.network.OpenGradleMCGuiPacket;
import com.soumyajit.gradlemc.network.SyncSmartAIStatusPacket;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import net.neoforged.neoforge.client.network.event.RegisterClientPayloadHandlersEvent;

public final class ClientModEventHandler {
    private ClientModEventHandler() {
    }

    public static void registerKeyMappings(RegisterKeyMappingsEvent event) {
        event.register(GradleMCKeyMappings.OPEN_GUI);
        event.register(GradleMCKeyMappings.TOGGLE_OVERLAY);
        event.register(GradleMCKeyMappings.CYCLE_OVERLAY_POSITION);
        event.register(GradleMCKeyMappings.TOGGLE_OVERLAY_MODE);
        event.register(GradleMCKeyMappings.START_STOP_QUICK_FPS_SAMPLE);
    }

    public static void registerClientPayloadHandlers(RegisterClientPayloadHandlersEvent event) {
        event.register(OpenGradleMCGuiPacket.TYPE, OpenGradleMCGuiPacket::handle);
        event.register(SyncSmartAIStatusPacket.TYPE, SyncSmartAIStatusPacket::handle);
    }

    public static void registerGuiLayers(RegisterGuiLayersEvent event) {
        event.registerAbove(
                VanillaGuiLayers.SLEEP_OVERLAY,
                Identifier.fromNamespaceAndPath(GradleMC.MOD_ID, "stats_overlay"),
                (graphics, deltaTracker) -> GradleMCStatsOverlay.render(graphics, deltaTracker.getGameTimeDeltaPartialTick(false))
        );
    }
}
