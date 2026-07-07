package com.soumyajit.gradlemc.client;

import com.soumyajit.gradlemc.GradleMC;
import com.soumyajit.gradlemc.client.input.GradleMCKeyMappings;
import com.soumyajit.gradlemc.client.overlay.GradleMCStatsOverlay;
import net.minecraft.resources.Identifier;
import net.minecraftforge.client.event.AddGuiOverlayLayersEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.gui.overlay.ForgeLayeredDraw;

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

    public static void registerGuiOverlays(AddGuiOverlayLayersEvent event) {
        event.getLayeredDraw().addWithCondition(
                ForgeLayeredDraw.POST_SLEEP_STACK,
                Identifier.fromNamespaceAndPath(GradleMC.MOD_ID, "stats_overlay"),
                (graphics, deltaTracker) -> GradleMCStatsOverlay.render(graphics, deltaTracker.getGameTimeDeltaPartialTick(false)),
                GradleMCStatsOverlay::shouldRender
        );
    }
}
