package com.soumyajit.gradlemc.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public final class OpenGradleMCGuiPacket implements CustomPacketPayload {
    public static final Type<OpenGradleMCGuiPacket> TYPE = new Type<>(Identifier.fromNamespaceAndPath("gradlemc", "open_gui"));
    public static final OpenGradleMCGuiPacket INSTANCE = new OpenGradleMCGuiPacket();
    public static final StreamCodec<FriendlyByteBuf, OpenGradleMCGuiPacket> STREAM_CODEC = StreamCodec.unit(INSTANCE);

    private OpenGradleMCGuiPacket() {
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
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
