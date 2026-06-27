package com.soumyajit.gradlemc.network;

import com.soumyajit.gradlemc.ai.SmartAIStatus;
import com.soumyajit.gradlemc.ai.ThreatLevel;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record SyncSmartAIStatusPacket(SmartAIStatus status) {
    private static final int MAX_RECENT_ADAPTATION_LENGTH = 128;

    public static void encode(SyncSmartAIStatusPacket packet, FriendlyByteBuf buffer) {
        SmartAIStatus status = packet.status() == null ? SmartAIStatus.disabled() : packet.status();
        buffer.writeBoolean(status.adaptiveSmartAIEnabled());
        buffer.writeBoolean(status.adaptiveAmbienceEnabled());
        buffer.writeBoolean(status.adaptiveEventsEnabled());
        buffer.writeInt(status.threatScore());
        buffer.writeEnum(status.threatLevel());
        buffer.writeUtf(trim(status.recentAdaptation()), MAX_RECENT_ADAPTATION_LENGTH);
        buffer.writeInt(status.ticksUntilNextEvent());
        buffer.writeInt(status.ticksUntilNextAmbience());
        buffer.writeInt(status.darknessTicks());
        buffer.writeInt(status.undergroundTicks());
        buffer.writeInt(status.ticksSinceSleep());
        buffer.writeInt(status.movementPressure());
        buffer.writeInt(status.recentDamageTaken());
        buffer.writeInt(status.recentMobKills());
        buffer.writeInt(status.recentDeaths());
    }

    public static SyncSmartAIStatusPacket decode(FriendlyByteBuf buffer) {
        return new SyncSmartAIStatusPacket(new SmartAIStatus(
                buffer.readBoolean(),
                buffer.readBoolean(),
                buffer.readBoolean(),
                buffer.readInt(),
                buffer.readEnum(ThreatLevel.class),
                buffer.readUtf(MAX_RECENT_ADAPTATION_LENGTH),
                buffer.readInt(),
                buffer.readInt(),
                buffer.readInt(),
                buffer.readInt(),
                buffer.readInt(),
                buffer.readInt(),
                buffer.readInt(),
                buffer.readInt(),
                buffer.readInt()
        ));
    }

    public static void handle(SyncSmartAIStatusPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        if (context.getDirection() == NetworkDirection.PLAY_TO_CLIENT) {
            context.enqueueWork(() -> GradleMCGuiBridge.updateSmartAIStatus(packet.status()));
        }
        context.setPacketHandled(true);
    }

    private static String trim(String value) {
        if (value == null) {
            return "";
        }
        return value.length() <= MAX_RECENT_ADAPTATION_LENGTH
                ? value
                : value.substring(0, MAX_RECENT_ADAPTATION_LENGTH);
    }
}
