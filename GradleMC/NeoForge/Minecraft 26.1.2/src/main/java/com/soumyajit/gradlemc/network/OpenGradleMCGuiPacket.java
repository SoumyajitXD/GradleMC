package com.soumyajit.gradlemc.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public final class OpenGradleMCGuiPacket implements CustomPacketPayload {
    public static final OpenGradleMCGuiPacket INSTANCE = new OpenGradleMCGuiPacket();
    public static final CustomPacketPayload.Type<OpenGradleMCGuiPacket> TYPE =
            new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath("gradlemc", "open_gui"));
    public static final StreamCodec<FriendlyByteBuf, OpenGradleMCGuiPacket> STREAM_CODEC = StreamCodec.unit(INSTANCE);

    private OpenGradleMCGuiPacket() {
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(OpenGradleMCGuiPacket packet, IPayloadContext context) {
        GradleMCGuiBridge.open();
    }
}
