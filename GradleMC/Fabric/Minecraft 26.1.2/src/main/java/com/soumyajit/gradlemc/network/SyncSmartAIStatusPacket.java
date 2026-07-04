package com.soumyajit.gradlemc.network;

import com.soumyajit.gradlemc.ai.SmartAIStatus;
import com.soumyajit.gradlemc.ai.ThreatLevel;
import com.soumyajit.gradlemc.metrics.DiagnosticTestProgress;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record SyncSmartAIStatusPacket(SmartAIStatus status, GuiStatusSnapshot guiStatus) implements CustomPacketPayload {
    public static final Type<SyncSmartAIStatusPacket> TYPE = new Type<>(Identifier.fromNamespaceAndPath("gradlemc", "sync_smart_ai_status"));
    public static final StreamCodec<FriendlyByteBuf, SyncSmartAIStatusPacket> STREAM_CODEC =
            CustomPacketPayload.codec(SyncSmartAIStatusPacket::encode, SyncSmartAIStatusPacket::decode);
    private static final int MAX_RECENT_ADAPTATION_LENGTH = 128;
    private static final int MAX_TOP_FACTORS_LENGTH = 192;
    private static final int MAX_PATH_LENGTH = 192;
    private static final int MAX_SUMMARY_LENGTH = 192;

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void encode(SyncSmartAIStatusPacket packet, FriendlyByteBuf buffer) {
        SmartAIStatus status = packet.status() == null ? SmartAIStatus.disabled() : packet.status();
        GuiStatusSnapshot guiStatus = packet.guiStatus() == null ? GuiStatusSnapshot.empty() : packet.guiStatus();
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
    }

    public static SyncSmartAIStatusPacket decode(FriendlyByteBuf buffer) {
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
        );
        return new SyncSmartAIStatusPacket(status, guiStatus);
    }

    public static void handle(SyncSmartAIStatusPacket packet) {
        GradleMCGuiBridge.updateStatus(packet.status(), packet.guiStatus());
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
