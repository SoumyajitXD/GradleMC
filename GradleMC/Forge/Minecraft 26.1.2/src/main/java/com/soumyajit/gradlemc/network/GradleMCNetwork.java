package com.soumyajit.gradlemc.network;

import com.soumyajit.gradlemc.GradleMC;
import com.soumyajit.gradlemc.ai.SmartAIStatus;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.ChannelBuilder;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.SimpleChannel;

import java.util.Objects;

public final class GradleMCNetwork {
    private static final int PROTOCOL_VERSION = 4;
    private static int packetId;
    private static boolean registered;

    private static final SimpleChannel CHANNEL = ChannelBuilder
            .named(Objects.requireNonNull(Identifier.tryBuild(GradleMC.MOD_ID, "main")))
            .networkProtocolVersion(PROTOCOL_VERSION)
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
        CHANNEL.send(OpenGradleMCGuiPacket.INSTANCE, PacketDistributor.PLAYER.with(player));
    }

    public static void syncSmartAIStatus(ServerPlayer player, SmartAIStatus status) {
        syncSmartAIStatus(player, status, player == null ? GuiStatusSnapshot.empty() : GuiStatusSnapshot.capture(player.level().getServer()));
    }

    public static void syncSmartAIStatus(ServerPlayer player, SmartAIStatus status, GuiStatusSnapshot guiStatus) {
        if (!registered || player == null) {
            return;
        }
        CHANNEL.send(new SyncSmartAIStatusPacket(status, guiStatus), PacketDistributor.PLAYER.with(player));
    }

    public static void requestSmartAIStatus() {
        if (!registered) {
            return;
        }
        CHANNEL.send(RequestSmartAIStatusPacket.INSTANCE, PacketDistributor.SERVER.noArg());
    }

    private static int nextPacketId() {
        return packetId++;
    }
}
