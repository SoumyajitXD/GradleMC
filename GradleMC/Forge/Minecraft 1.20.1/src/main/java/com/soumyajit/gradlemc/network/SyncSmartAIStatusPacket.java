package com.soumyajit.gradlemc.network;

import com.soumyajit.gradlemc.ai.SmartAIStatus;
import com.soumyajit.gradlemc.ai.ThreatLevel;
import com.soumyajit.gradlemc.metrics.DiagnosticTestProgress;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record SyncSmartAIStatusPacket(SmartAIStatus status, GuiStatusSnapshot guiStatus, long epoch, long requestId, long generation) {
    private static final int MAX_RECENT_ADAPTATION_LENGTH = 128;
    private static final int MAX_TOP_FACTORS_LENGTH = 192;
    private static final int MAX_PATH_LENGTH = 192;
    private static final int MAX_SUMMARY_LENGTH = 192;

    public static void encode(SyncSmartAIStatusPacket packet, FriendlyByteBuf buffer) {
        SmartAIStatus status = packet.status() == null ? SmartAIStatus.disabled() : packet.status();
        GuiStatusSnapshot guiStatus = packet.guiStatus() == null ? GuiStatusSnapshot.empty() : packet.guiStatus();
        buffer.writeLong(packet.epoch());
        buffer.writeLong(packet.requestId());
        buffer.writeLong(packet.generation());
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
        buffer.writeInt(status.nearbyHostileMobs());
        buffer.writeInt(status.healthPercent());
        buffer.writeInt(status.foodLevel());
        buffer.writeUtf(trim(status.topRiskFactors(), MAX_TOP_FACTORS_LENGTH), MAX_TOP_FACTORS_LENGTH);
        buffer.writeInt(guiStatus.technicalStabilityScore());
        buffer.writeUtf(trim(guiStatus.technicalRiskLevel(), 32), 32);
        buffer.writeUtf(trim(guiStatus.technicalConfidence(), 32), 32);
        writeProgress(buffer, guiStatus.performanceProgress());
        writeProgress(buffer, guiStatus.worldgenProgress());
        buffer.writeUtf(trim(guiStatus.latestReportPath(), MAX_PATH_LENGTH), MAX_PATH_LENGTH);
        buffer.writeUtf(trim(guiStatus.latestReportSummary(), MAX_SUMMARY_LENGTH), MAX_SUMMARY_LENGTH);
        buffer.writeUtf(trim(guiStatus.latestPerformanceReportPath(), MAX_PATH_LENGTH), MAX_PATH_LENGTH);
        buffer.writeUtf(trim(guiStatus.latestWorldgenReportPath(), MAX_PATH_LENGTH), MAX_PATH_LENGTH);
        buffer.writeUtf(trim(guiStatus.latestExportPath(), MAX_PATH_LENGTH), MAX_PATH_LENGTH);
        buffer.writeUtf(trim(guiStatus.latestIssueBundlePath(), MAX_PATH_LENGTH), MAX_PATH_LENGTH);
        buffer.writeUtf(trim(guiStatus.latestProfilePath(), MAX_PATH_LENGTH), MAX_PATH_LENGTH);
        buffer.writeUtf(trim(guiStatus.latestProfileSummary(), MAX_SUMMARY_LENGTH), MAX_SUMMARY_LENGTH);
        buffer.writeBoolean(guiStatus.administrativeAllowed());
        buffer.writeVarInt(guiStatus.supportedOperations().size());
        for (com.soumyajit.gradlemc.capability.ServerOperation operation : guiStatus.supportedOperations()) buffer.writeEnum(operation);
    }

    public static SyncSmartAIStatusPacket decode(FriendlyByteBuf buffer) {
        try {
        long epoch = buffer.readLong();
        long requestId = buffer.readLong();
        long generation = buffer.readLong();
        SmartAIStatus status = new SmartAIStatus(
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
                buffer.readInt(),
                buffer.readInt(),
                buffer.readInt(),
                buffer.readInt(),
                buffer.readUtf(MAX_TOP_FACTORS_LENGTH)
        );
        GuiStatusSnapshot guiStatus = new GuiStatusSnapshot(
                buffer.readInt(),
                buffer.readUtf(32),
                buffer.readUtf(32),
                readProgress(buffer),
                readProgress(buffer),
                buffer.readUtf(MAX_PATH_LENGTH),
                buffer.readUtf(MAX_SUMMARY_LENGTH),
                buffer.readUtf(MAX_PATH_LENGTH),
                buffer.readUtf(MAX_PATH_LENGTH),
                buffer.readUtf(MAX_PATH_LENGTH),
                buffer.readUtf(MAX_PATH_LENGTH),
                buffer.readUtf(MAX_PATH_LENGTH),
                buffer.readUtf(MAX_SUMMARY_LENGTH)
                ,buffer.readBoolean(), readOperations(buffer)
        );
        return new SyncSmartAIStatusPacket(status, guiStatus, epoch, requestId, generation);
        } catch (RuntimeException malformed) {
            // A remote endpoint must not turn malformed status metadata into client state or GUI work.
            return new SyncSmartAIStatusPacket(SmartAIStatus.disabled(), GuiStatusSnapshot.empty(), 0L, 0L, 0L);
        }
    }

    public static void handle(SyncSmartAIStatusPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        if (context.getDirection() == NetworkDirection.PLAY_TO_CLIENT) {
            NetworkDiagnostics.record("sync_status","server-to-client",-1);
            context.enqueueWork(() -> GradleMCGuiBridge.acceptResponse(packet.epoch(), packet.requestId(), packet.generation(), packet.status(), packet.guiStatus()));
        }
        context.setPacketHandled(true);
    }

    private static void writeProgress(FriendlyByteBuf buffer, DiagnosticTestProgress progress) {
        DiagnosticTestProgress safeProgress = progress == null ? DiagnosticTestProgress.idle() : progress;
        buffer.writeBoolean(safeProgress.running());
        buffer.writeInt(safeProgress.requestedSeconds());
        buffer.writeInt(safeProgress.elapsedSeconds());
    }

    private static DiagnosticTestProgress readProgress(FriendlyByteBuf buffer) {
        return new DiagnosticTestProgress(buffer.readBoolean(), buffer.readInt(), buffer.readInt());
    }

    private static java.util.Set<com.soumyajit.gradlemc.capability.ServerOperation> readOperations(FriendlyByteBuf buffer) {
        int count = buffer.readVarInt();
        if (count < 0 || count > com.soumyajit.gradlemc.capability.ServerOperation.values().length) throw new IllegalArgumentException("Invalid operation count");
        java.util.EnumSet<com.soumyajit.gradlemc.capability.ServerOperation> operations = java.util.EnumSet.noneOf(com.soumyajit.gradlemc.capability.ServerOperation.class);
        for (int index = 0; index < count; index++) operations.add(buffer.readEnum(com.soumyajit.gradlemc.capability.ServerOperation.class));
        return operations;
    }

    private static String trim(String value) {
        return trim(value, MAX_RECENT_ADAPTATION_LENGTH);
    }

    private static String trim(String value, int maxLength) {
        if (value == null) {
            return "";
        }
        return value.length() <= maxLength
                ? value
                : value.substring(0, maxLength);
    }
}
