package com.soumyajit.gradlemc.network;

import net.minecraft.network.FriendlyByteBuf;

public final class OpenGradleMCGuiPacket {
    public static final OpenGradleMCGuiPacket INSTANCE = new OpenGradleMCGuiPacket();

    private OpenGradleMCGuiPacket() {
    }

    public static void encode(OpenGradleMCGuiPacket packet, FriendlyByteBuf buffer) {
    }

    public static OpenGradleMCGuiPacket decode(FriendlyByteBuf buffer) {
        return INSTANCE;
    }

    public static void handle(OpenGradleMCGuiPacket packet) {
        GradleMCGuiBridge.open();
    }
}
