package com.soumyajit.gradlemc.client;

import com.soumyajit.gradlemc.GradleMC;
import net.minecraftforge.client.event.AddGuiOverlayLayersEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.event.TickEvent;

public final class ClientBootstrap {
    private static boolean registered;

    private ClientBootstrap() {
    }

    public static void register() {
        if (registered) {
            return;
        }
        registered = true;
        RegisterKeyMappingsEvent.BUS.addListener(ClientModEventHandler::registerKeyMappings);
        AddGuiOverlayLayersEvent.BUS.addListener(ClientModEventHandler::registerGuiOverlays);
        TickEvent.ClientTickEvent.Post.BUS.addListener(ClientEventHandler::onClientTick);
        GradleMC.LOGGER.info("GradleMC client hooks registered");
    }
}
