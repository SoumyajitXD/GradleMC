package com.soumyajit.gradlemc.client;

import com.soumyajit.gradlemc.GradleMC;
import com.soumyajit.gradlemc.client.gui.GradleMCGuiOpener;
import com.soumyajit.gradlemc.client.input.GradleMCKeyMappings;
import com.soumyajit.gradlemc.command.FpsTestCommandBridge;
import com.soumyajit.gradlemc.network.GradleMCGuiBridge;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = GradleMC.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
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

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            FpsTestManager.onClientTick();
            handleOpenGuiKey();
        }
    }

    private static void handleOpenGuiKey() {
        Minecraft minecraft = Minecraft.getInstance();
        while (GradleMCKeyMappings.OPEN_GUI.consumeClick()) {
            if (minecraft.player != null && minecraft.screen == null) {
                GradleMCGuiOpener.open();
            }
        }
    }
}
