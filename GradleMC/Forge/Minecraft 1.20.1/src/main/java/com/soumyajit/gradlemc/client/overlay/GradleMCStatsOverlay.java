package com.soumyajit.gradlemc.client.overlay;

import com.mojang.blaze3d.vertex.PoseStack;
import com.soumyajit.gradlemc.client.FpsTestManager;
import com.soumyajit.gradlemc.config.GradleMCConfig;
import com.soumyajit.gradlemc.metrics.DiagnosticTestProgress;
import com.soumyajit.gradlemc.metrics.MeasurementChannel;
import com.soumyajit.gradlemc.metrics.MeasurementDemand;
import com.soumyajit.gradlemc.metrics.MeasurementHub;
import com.soumyajit.gradlemc.metrics.MeasurementSubscription;
import com.soumyajit.gradlemc.metrics.ServerPerformanceSnapshot;
import com.soumyajit.gradlemc.network.GradleMCGuiBridge;
import com.soumyajit.gradlemc.network.GuiStatusSnapshot;
import com.soumyajit.gradlemc.profiler.GradleMcProfilerService;
import com.soumyajit.gradlemc.performance.PerformanceService;
import com.soumyajit.gradlemc.performance.GradleMcOverheadMonitor;
import com.soumyajit.gradlemc.util.RuntimeSnapshots;
import com.soumyajit.gradlemc.report.SharedReleaseEvidence;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraftforge.client.gui.overlay.ForgeGui;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class GradleMCStatsOverlay {
    private static final int PADDING = 4;
    private static final int MARGIN = 6;
    private static final int TEXT_COLOR = 0xFFEAF0F7;
    private static final int MUTED_COLOR = 0xFFC3CBD5;
    private static MeasurementSubscription frameSubscription;
    private static MeasurementSubscription memorySubscription;

    private static long lastDisplayUpdateMillis;
    private static long lastPercentileUpdateMillis;
    private static FpsRollingStatsCalculator.Snapshot cachedFps = FpsRollingStatsCalculator.Snapshot.empty();
    private static List<String> cachedLines = List.of();
    private static Layout cachedLayout = Layout.empty();

    private GradleMCStatsOverlay() {
    }

    public static void render(ForgeGui gui, GuiGraphics graphics, float partialTick, int screenWidth, int screenHeight) {
        long renderStarted = System.nanoTime();
        Minecraft minecraft = Minecraft.getInstance();
        if (!shouldRender(minecraft)) {
            releaseFrameDemand();
            releaseMemoryDemand();
            return;
        }
        if (!hasEnabledContent()) {
            cachedLines = List.of();
            releaseFrameDemand();
            releaseMemoryDemand();
            return;
        }
        refreshFrameDemand();
        refreshMemoryDemand();

        long now = System.currentTimeMillis();
        int updateInterval = Math.max(GradleMCConfig.OVERLAY_UPDATE_INTERVAL_MS.get(), PerformanceService.policy().overlayRefreshMillis());
        if (PerformanceService.guard().deferOptionalWork()) {
            updateInterval = Math.min(5_000, updateInterval * 2);
            PerformanceService.overhead().coalesced();
        }
        if (now - lastDisplayUpdateMillis >= updateInterval || cachedLines.isEmpty()) {
            boolean wantsPercentiles = GradleMCConfig.OVERLAY_SHOW_ONE_PERCENT_LOW.get()
                    || GradleMCConfig.OVERLAY_SHOW_POINT_ONE_PERCENT_LOW.get();
            boolean refreshPercentiles = wantsPercentiles && now - lastPercentileUpdateMillis >= 1_000L;
            cachedFps = MeasurementHub.instance().frameSnapshot(refreshPercentiles);
            SharedReleaseEvidence.publishFps(cachedFps.sampleCount(), cachedFps.currentFps(), cachedFps.averageFps(),
                    cachedFps.onePercentLowFps(), cachedFps.pointOnePercentLowFps(), System.nanoTime());
            if (refreshPercentiles) lastPercentileUpdateMillis = now;
            List<String> refreshedLines = buildLines(minecraft, cachedFps);
            if (!refreshedLines.equals(cachedLines)) cachedLayout = Layout.of(minecraft.font, refreshedLines);
            cachedLines = refreshedLines;
            lastDisplayUpdateMillis = now;
        }
        draw(graphics, minecraft.font, cachedLines, cachedLayout, screenWidth, screenHeight);
        PerformanceService.overhead().record(GradleMcOverheadMonitor.Category.OVERLAY_PREPARATION, System.nanoTime() - renderStarted);
    }

    public static FpsRollingStatsCalculator.Snapshot latestFpsSnapshot() {
        return MeasurementHub.instance().frameSnapshot(false);
    }

    /** Invalidates presentation-only caches after an explicit overlay setting change. */
    public static void onSettingsChanged() {
        MeasurementHub.instance().setFrameWindowSeconds(GradleMCConfig.OVERLAY_SAMPLING_WINDOW_SECONDS.get());
        releaseFrameDemand();
        releaseMemoryDemand();
        cachedLines = List.of();
        cachedLayout = Layout.empty();
        lastDisplayUpdateMillis = 0L;
    }

    /** Pauses interval measurement without treating time away from gameplay as a slow frame. */
    public static void pauseFpsMeasurement() {
        MeasurementHub.instance().pauseFrames();
    }

    /** Called when joining or leaving a world so no prior world's samples leak into the next one. */
    public static void resetFpsMeasurement() {
        releaseFrameDemand();
        MeasurementHub.instance().resetFrames();
        cachedFps = FpsRollingStatsCalculator.Snapshot.empty();
        cachedLines = List.of();
        cachedLayout = Layout.empty();
        lastDisplayUpdateMillis = 0L;
        lastPercentileUpdateMillis = 0L;
    }

    private static void refreshFrameDemand() {
        boolean needsFrames = GradleMCConfig.OVERLAY_SHOW_FPS.get() || GradleMCConfig.OVERLAY_SHOW_AVERAGE_FPS.get()
                || GradleMCConfig.OVERLAY_SHOW_ONE_PERCENT_LOW.get() || GradleMCConfig.OVERLAY_SHOW_POINT_ONE_PERCENT_LOW.get();
        if (needsFrames && frameSubscription == null) frameSubscription = MeasurementHub.instance().acquire(MeasurementChannel.FRAME_TIMING, "overlay", MeasurementDemand.NORMAL);
        if (!needsFrames) releaseFrameDemand();
    }
    private static void releaseFrameDemand() { if (frameSubscription != null) { frameSubscription.close(); frameSubscription = null; } }
    private static void refreshMemoryDemand() {
        if (GradleMCConfig.OVERLAY_SHOW_JVM_MEMORY.get() && memorySubscription == null) {
            memorySubscription = MeasurementHub.instance().acquire(MeasurementChannel.JVM_MEMORY, "overlay-memory", MeasurementDemand.LOW_FREQUENCY);
        }
        if (!GradleMCConfig.OVERLAY_SHOW_JVM_MEMORY.get()) releaseMemoryDemand();
    }
    private static void releaseMemoryDemand() { if (memorySubscription != null) { memorySubscription.close(); memorySubscription = null; } }

    private static boolean shouldRender(Minecraft minecraft) {
        return GradleMCConfig.OVERLAY_ENABLED.get()
                && minecraft.level != null
                && minecraft.player != null
                && minecraft.screen == null
                && !minecraft.options.renderDebug;
    }

    private static boolean hasEnabledContent() {
        return GradleMCConfig.OVERLAY_SHOW_TITLE.get()
                || GradleMCConfig.OVERLAY_SHOW_FPS.get()
                || GradleMCConfig.OVERLAY_SHOW_AVERAGE_FPS.get()
                || GradleMCConfig.OVERLAY_SHOW_ONE_PERCENT_LOW.get()
                || GradleMCConfig.OVERLAY_SHOW_POINT_ONE_PERCENT_LOW.get()
                || GradleMCConfig.OVERLAY_SHOW_JVM_MEMORY.get()
                || GradleMCConfig.OVERLAY_SHOW_SYSTEM_MEMORY.get()
                || GradleMCConfig.OVERLAY_SHOW_CPU.get()
                || GradleMCConfig.OVERLAY_SHOW_GPU_NAME.get()
                || GradleMCConfig.OVERLAY_SHOW_GPU_USAGE.get()
                || GradleMCConfig.OVERLAY_SHOW_INTEGRATED_SERVER.get()
                || GradleMCConfig.OVERLAY_SHOW_TEST_STATUS.get()
                || GradleMCConfig.OVERLAY_SHOW_PROFILER_STATUS.get()
                || GradleMCConfig.OVERLAY_SHOW_STABILITY.get();
    }

    private static List<String> buildLines(Minecraft minecraft, FpsRollingStatsCalculator.Snapshot fps) {
        boolean compact = !"DETAILED".equalsIgnoreCase(GradleMCConfig.OVERLAY_MODE.get());
        RuntimeSnapshots.MemorySnapshot memory = RuntimeSnapshots.memory();
        GuiStatusSnapshot serverStatus = GradleMCGuiBridge.latestGuiStatus();
        List<String> lines = new ArrayList<>(OverlayLineComposer.compose(compact,
                GradleMCConfig.OVERLAY_SHOW_TITLE.get(), GradleMCConfig.OVERLAY_SHOW_FPS.get(),
                GradleMCConfig.OVERLAY_SHOW_AVERAGE_FPS.get(), fps));
        if (compact) {
            appendCompactMetrics(lines, minecraft, memory, fps);
            return List.copyOf(lines);
        }
        if (GradleMCConfig.OVERLAY_SHOW_ONE_PERCENT_LOW.get()) {
            lines.add("1% low: " + lowLabel(fps.onePercentLowFps()));
        }
        if (GradleMCConfig.OVERLAY_SHOW_POINT_ONE_PERCENT_LOW.get()) {
            lines.add("0.1% low: " + lowLabel(fps.pointOnePercentLowFps()));
        }
        if (GradleMCConfig.OVERLAY_SHOW_JVM_MEMORY.get()) {
            lines.add("JVM: " + memory.usedMiB() + "/" + memory.maxMiB() + " MiB");
        }
        if (GradleMCConfig.OVERLAY_SHOW_SYSTEM_MEMORY.get()) {
            ClientSystemStats.SystemMemory systemMemory = ClientSystemStats.systemMemory();
            lines.add(systemMemory.available()
                    ? "System memory: " + systemMemory.usedMiB() + "/" + systemMemory.totalMiB() + " MiB"
                    : "System memory: unavailable");
        }
        if (GradleMCConfig.OVERLAY_SHOW_CPU.get()) {
            double cpu = ClientSystemStats.processCpuLoadPercent();
            lines.add("CPU: " + (cpu < 0.0D ? "unavailable" : format(cpu) + "%"));
            lines.add("CPU name: " + ClientSystemStats.cpuName());
        }
        if (GradleMCConfig.OVERLAY_SHOW_GPU_NAME.get()) {
            lines.add("GPU: " + ClientSystemStats.gpuRenderer());
        }
        if (GradleMCConfig.OVERLAY_SHOW_GPU_USAGE.get()) {
            lines.add("GPU usage: unavailable");
        }
        if (GradleMCConfig.OVERLAY_SHOW_INTEGRATED_SERVER.get()) {
            integratedServerLine(minecraft).forEach(lines::add);
        }
        if (GradleMCConfig.OVERLAY_SHOW_TEST_STATUS.get()) {
            lines.add("Tests: FPS " + progressLabel(FpsTestManager.progress())
                    + ", perf " + progressLabel(serverStatus.performanceProgress())
                    + ", worldgen " + progressLabel(serverStatus.worldgenProgress()));
        }
        if (GradleMCConfig.OVERLAY_SHOW_PROFILER_STATUS.get()) {
            lines.add(GradleMcProfilerService.overlayStatusLine());
        }
        if (GradleMCConfig.OVERLAY_SHOW_STABILITY.get()) {
            lines.add("Stability: " + stabilityLabel(serverStatus));
        }
        return List.copyOf(lines);
    }

    private static void appendCompactMetrics(List<String> lines, Minecraft minecraft, RuntimeSnapshots.MemorySnapshot memory, FpsRollingStatsCalculator.Snapshot fps) {
        List<String> parts = lines.isEmpty() ? new ArrayList<>() : new ArrayList<>(List.of(lines.remove(0)));
        if (GradleMCConfig.OVERLAY_SHOW_ONE_PERCENT_LOW.get()) {
            parts.add("1% " + lowLabel(fps.onePercentLowFps()));
        }
        if (GradleMCConfig.OVERLAY_SHOW_POINT_ONE_PERCENT_LOW.get()) {
            parts.add("0.1% " + lowLabel(fps.pointOnePercentLowFps()));
        }
        if (GradleMCConfig.OVERLAY_SHOW_JVM_MEMORY.get()) {
            parts.add("JVM " + formatGiB(memory.usedMiB()) + "/" + formatGiB(memory.maxMiB()) + "G");
        }
        if (GradleMCConfig.OVERLAY_SHOW_SYSTEM_MEMORY.get()) {
            ClientSystemStats.SystemMemory systemMemory = ClientSystemStats.systemMemory();
            parts.add(systemMemory.available() ? "System " + formatGiB(systemMemory.usedMiB()) + "/" + formatGiB(systemMemory.totalMiB()) + "G" : "System unavailable");
        }
        if (GradleMCConfig.OVERLAY_SHOW_CPU.get()) {
            double cpu = ClientSystemStats.processCpuLoadPercent();
            parts.add(cpu < 0.0D ? "CPU unavailable" : "CPU " + format(cpu) + "%");
        }
        if (GradleMCConfig.OVERLAY_SHOW_GPU_NAME.get()) {
            parts.add("GPU " + ClientSystemStats.gpuRenderer());
        }
        if (GradleMCConfig.OVERLAY_SHOW_GPU_USAGE.get()) {
            parts.add("GPU usage unavailable");
        }
        if (GradleMCConfig.OVERLAY_SHOW_INTEGRATED_SERVER.get()) {
            ServerPerformanceSnapshot server = MeasurementHub.instance().serverPerformanceSnapshot();
            if (minecraft.getSingleplayerServer() != null && server.availability() == ServerPerformanceSnapshot.Availability.AVAILABLE) parts.add("Server " + format(server.averageTps()) + " TPS");
        }
        if (!parts.isEmpty()) {
            lines.add(String.join(" | ", parts));
        }
    }

    private static List<String> integratedServerLine(Minecraft minecraft) {
        if (minecraft.getSingleplayerServer() == null) return List.of();
        ServerPerformanceSnapshot server = MeasurementHub.instance().serverPerformanceSnapshot();
        if (server.availability() != ServerPerformanceSnapshot.Availability.AVAILABLE) return List.of("Integrated server: warming up");
        return List.of("Integrated server: " + format(server.averageTps()) + " TPS, " + format(server.averageMspt()) + " MSPT");
    }

    private static void draw(GuiGraphics graphics, Font font, List<String> lines, Layout layout, int screenWidth, int screenHeight) {
        if (lines.isEmpty()) {
            return;
        }
        double scale = Math.max(0.75D, Math.min(2.0D, GradleMCConfig.OVERLAY_SCALE.get()));
        int lineHeight = font.lineHeight + 2;
        int boxWidth = layout.maxWidth + PADDING * 2;
        int boxHeight = lines.size() * lineHeight + PADDING * 2;
        int scaledWidth = (int) Math.ceil(boxWidth * scale);
        int scaledHeight = (int) Math.ceil(boxHeight * scale);
        OverlayPosition position = OverlayPosition.fromConfig(GradleMCConfig.OVERLAY_POSITION.get());
        int x = switch (position) {
            case TOP_LEFT, BOTTOM_LEFT -> MARGIN;
            case TOP_RIGHT, BOTTOM_RIGHT -> screenWidth - scaledWidth - MARGIN;
        };
        int y = switch (position) {
            case TOP_LEFT, TOP_RIGHT -> MARGIN;
            case BOTTOM_LEFT, BOTTOM_RIGHT -> screenHeight - scaledHeight - MARGIN;
        };

        PoseStack pose = graphics.pose();
        pose.pushPose();
        pose.translate(x, y, 0.0D);
        pose.scale((float) scale, (float) scale, 1.0F);
        if (GradleMCConfig.OVERLAY_BACKGROUND_ENABLED.get()) {
            int alpha = (int) Math.round(Math.max(0.0D, Math.min(1.0D, GradleMCConfig.OVERLAY_BACKGROUND_OPACITY.get())) * 255.0D);
            graphics.fill(0, 0, boxWidth, boxHeight, (alpha << 24) | 0x10141C);
        }
        int textY = PADDING;
        for (int index = 0; index < lines.size(); index++) {
            graphics.drawString(font, lines.get(index), PADDING, textY, index == 0 ? TEXT_COLOR : MUTED_COLOR, false);
            textY += lineHeight;
        }
        pose.popPose();
    }

    private static String progressLabel(DiagnosticTestProgress progress) {
        if (progress == null || !progress.running()) {
            return "idle";
        }
        return progress.clampedElapsedSeconds() + "/" + progress.requestedSeconds() + "s";
    }

    private static String stabilityLabel(GuiStatusSnapshot status) {
        if (status == null || status.technicalStabilityScore() < 0) {
            return "unavailable";
        }
        return status.technicalStabilityScore() + "/100 " + status.technicalRiskLevel().toLowerCase(Locale.ROOT);
    }

    private static String lowLabel(Double value) {
        return value == null ? "warming up" : whole(value);
    }

    private static String whole(double value) {
        return String.format(Locale.ROOT, "%.0f", safe(value));
    }

    private static String format(double value) {
        return String.format(Locale.ROOT, "%.1f", safe(value));
    }

    private static String formatGiB(long mib) {
        return String.format(Locale.ROOT, "%.1f", Math.max(0L, mib) / 1024.0D);
    }

    private static double safe(double value) {
        return Double.isFinite(value) ? Math.max(0.0D, value) : 0.0D;
    }

    /** Width work belongs to the bounded formatting refresh, never to every render frame. */
    private record Layout(int maxWidth) {
        static Layout empty() { return new Layout(0); }
        static Layout of(Font font, List<String> lines) {
            int width = 0;
            for (String line : lines) width = Math.max(width, font.width(line));
            return new Layout(width);
        }
    }
}
