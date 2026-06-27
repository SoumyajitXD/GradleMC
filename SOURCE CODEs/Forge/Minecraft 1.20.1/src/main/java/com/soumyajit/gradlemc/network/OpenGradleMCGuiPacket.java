package com.soumyajit.gradlemc.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public final class OpenGradleMCGuiPacket {
    public static final OpenGradleMCGuiPacket INSTANCE = new OpenGradleMCGuiPacket();

    private OpenGradleMCGuiPacket() {
    }

    public static void encode(OpenGradleMCGuiPacket packet, FriendlyByteBuf buffer) {
    }

    public static OpenGradleMCGuiPacket decode(FriendlyByteBuf buffer) {
        return INSTANCE;
    }

    public static void handle(OpenGradleMCGuiPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        if (context.getDirection() == NetworkDirection.PLAY_TO_CLIENT) {
            context.enqueueWork(GradleMCGuiBridge::open);
        }
        context.setPacketHandled(true);
    }
}
