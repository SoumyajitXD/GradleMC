package com.soumyajit.gradlemc.client;

import com.soumyajit.gradlemc.GradleMC;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.common.NeoForge;

public final class ClientBootstrap {
    private static boolean registered;

    private ClientBootstrap() {
    }

    public static void register(IEventBus modEventBus) {
        if (registered) {
            return;
        }
        registered = true;
        modEventBus.addListener(ClientModEventHandler::registerClientPayloadHandlers);
        modEventBus.addListener(ClientModEventHandler::registerKeyMappings);
        modEventBus.addListener(ClientModEventHandler::registerGuiLayers);
        NeoForge.EVENT_BUS.addListener((ClientTickEvent.Post event) -> ClientEventHandler.onClientTick(event));
        GradleMC.LOGGER.info("GradleMC client hooks registered");
    }
}
