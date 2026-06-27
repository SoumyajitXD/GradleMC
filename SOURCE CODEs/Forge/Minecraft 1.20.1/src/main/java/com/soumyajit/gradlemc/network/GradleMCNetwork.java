package com.soumyajit.gradlemc.network;

import com.soumyajit.gradlemc.GradleMC;
import com.soumyajit.gradlemc.ai.SmartAIStatus;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.Objects;

public final class GradleMCNetwork {
    private static final String PROTOCOL_VERSION = "2";
    private static int packetId;
    private static boolean registered;

    private static final SimpleChannel CHANNEL = NetworkRegistry.ChannelBuilder
            .named(Objects.requireNonNull(ResourceLocation.tryBuild(GradleMC.MOD_ID, "main")))
            .networkProtocolVersion(() -> PROTOCOL_VERSION)
            .clientAcceptedVersions(PROTOCOL_VERSION::equals)
            .serverAcceptedVersions(PROTOCOL_VERSION::equals)
            .simpleChannel();

    private GradleMCNetwork() {
    }

    public static void register() {
        if (registered) {
            return;
        }
        CHANNEL.messageBuilder(OpenGradleMCGuiPacket.class, nextPacketId(), NetworkDirection.PLAY_TO_CLIENT)
                .encoder(OpenGradleMCGuiPacket::encode)
                .decoder(OpenGradleMCGuiPacket::decode)
                .consumerMainThread(OpenGradleMCGuiPacket::handle)
                .add();
        CHANNEL.messageBuilder(SyncSmartAIStatusPacket.class, nextPacketId(), NetworkDirection.PLAY_TO_CLIENT)
                .encoder(SyncSmartAIStatusPacket::encode)
                .decoder(SyncSmartAIStatusPacket::decode)
                .consumerMainThread(SyncSmartAIStatusPacket::handle)
                .add();
        CHANNEL.messageBuilder(RequestSmartAIStatusPacket.class, nextPacketId(), NetworkDirection.PLAY_TO_SERVER)
                .encoder(RequestSmartAIStatusPacket::encode)
                .decoder(RequestSmartAIStatusPacket::decode)
                .consumerMainThread(RequestSmartAIStatusPacket::handle)
                .add();
        registered = true;
    }

    public static void openGui(ServerPlayer player) {
        if (!registered || player == null) {
            return;
        }
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), OpenGradleMCGuiPacket.INSTANCE);
    }

    public static void syncSmartAIStatus(ServerPlayer player, SmartAIStatus status) {
        if (!registered || player == null) {
            return;
        }
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new SyncSmartAIStatusPacket(status));
    }

    public static void requestSmartAIStatus() {
        if (!registered) {
            return;
        }
        CHANNEL.sendToServer(RequestSmartAIStatusPacket.INSTANCE);
    }

    private static int nextPacketId() {
        return packetId++;
    }
}
