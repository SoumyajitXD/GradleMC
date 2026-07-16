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
    private static final String PROTOCOL_VERSION = "4";
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
        NetworkDiagnostics.record("open_gui","server-to-client",0);
    }

    public static void syncSmartAIStatus(ServerPlayer player, SmartAIStatus status) {
        syncSmartAIStatus(player, status, player == null ? GuiStatusSnapshot.empty() : GuiStatusSnapshot.capture(player.server));
    }

    public static void syncSmartAIStatus(ServerPlayer player, SmartAIStatus status, GuiStatusSnapshot guiStatus) {
        if (!registered || player == null) {
            return;
        }
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new SyncSmartAIStatusPacket(status, guiStatus));
        NetworkDiagnostics.record("sync_status","server-to-client",approximateStatusBytes(status,guiStatus));
    }

    public static void requestSmartAIStatus() {
        if (!registered) {
            return;
        }
        CHANNEL.sendToServer(RequestSmartAIStatusPacket.INSTANCE);
        NetworkDiagnostics.record("request_status","client-to-server",0);
    }

    public static String protocolVersion() { return PROTOCOL_VERSION; }
    public static boolean registered() { return registered; }
    public static int registeredPacketTypes() { return packetId; }
    private static long approximateStatusBytes(SmartAIStatus status,GuiStatusSnapshot gui){if(status==null||gui==null)return 0;return 96L+chars(status.recentAdaptation())+chars(status.topRiskFactors())+chars(gui.latestReportPath())+chars(gui.latestReportSummary())+chars(gui.latestPerformanceReportPath())+chars(gui.latestWorldgenReportPath())+chars(gui.latestExportPath())+chars(gui.latestIssueBundlePath())+chars(gui.latestProfilePath())+chars(gui.latestProfileSummary());}
    private static long chars(String value){return value==null?0:Math.min(3L*value.length(),576L);}

    private static int nextPacketId() {
        return packetId++;
    }
}
