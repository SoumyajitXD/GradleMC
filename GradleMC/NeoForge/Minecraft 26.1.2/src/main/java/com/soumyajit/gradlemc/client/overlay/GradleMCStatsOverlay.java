package com.soumyajit.gradlemc.client.overlay;

import com.soumyajit.gradlemc.client.FpsTestManager;
import com.soumyajit.gradlemc.config.GradleMCConfig;
import com.soumyajit.gradlemc.metrics.DiagnosticTestProgress;
import com.soumyajit.gradlemc.network.GradleMCGuiBridge;
import com.soumyajit.gradlemc.network.GuiStatusSnapshot;
import com.soumyajit.gradlemc.profiler.GradleMcProfilerService;
import com.soumyajit.gradlemc.util.RuntimeSnapshots;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.server.MinecraftServer;
import org.joml.Matrix3x2fStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class GradleMCStatsOverlay {
    private static final int PADDING = 4;
    private static final int MARGIN = 6;
    private static final int TEXT_COLOR = 0xFFEAF0F7;
    private static final int MUTED_COLOR = 0xFFC3CBD5;
    private static final FpsRollingStatsCalculator FPS_STATS =
            new FpsRollingStatsCalculator(GradleMCConfig.OVERLAY_SAMPLING_WINDOW_SECONDS.get());

    private static long lastRenderNanos;
    private static long lastDisplayUpdateMillis;
    private static FpsRollingStatsCalculator.Snapshot cachedFps = FpsRollingStatsCalculator.Snapshot.empty();
    private static List<String> cachedLines = List.of();

    private GradleMCStatsOverlay() {
    }

    public static void render(GuiGraphicsExtractor graphics, float partialTick) {
        recordFrame();
        Minecraft minecraft = Minecraft.getInstance();
        if (!shouldRender(minecraft)) {
            return;
        }

        long now = System.currentTimeMillis();
        int updateInterval = Math.max(250, GradleMCConfig.OVERLAY_UPDATE_INTERVAL_MS.get());
        if (now - lastDisplayUpdateMillis >= updateInterval || cachedLines.isEmpty()) {
            FPS_STATS.setWindowSeconds(GradleMCConfig.OVERLAY_SAMPLING_WINDOW_SECONDS.get());
            cachedFps = FPS_STATS.snapshot().withCurrentFps(minecraft.getFps());
            cachedLines = buildLines(minecraft, cachedFps);
            lastDisplayUpdateMillis = now;
        }
        int screenWidth = minecraft.getWindow().getGuiScaledWidth();
        int screenHeight = minecraft.getWindow().getGuiScaledHeight();
        draw(graphics, minecraft.font, cachedLines, screenWidth, screenHeight);
    }

    public static FpsRollingStatsCalculator.Snapshot latestFpsSnapshot() {
        return cachedFps.withCurrentFps(Minecraft.getInstance().getFps());
    }

    private static void recordFrame() {
        long now = System.nanoTime();
        if (lastRenderNanos > 0L) {
            FPS_STATS.recordFrameTimeNanos(now - lastRenderNanos);
        }
        lastRenderNanos = now;
    }

    public static boolean shouldRender() {
        return shouldRender(Minecraft.getInstance());
    }

    private static boolean shouldRender(Minecraft minecraft) {
        return GradleMCConfig.OVERLAY_ENABLED.get()
                && minecraft.level != null
                && minecraft.player != null
                && minecraft.screen == null
                && !minecraft.options.hideGui;
    }

    private static List<String> buildLines(Minecraft minecraft, FpsRollingStatsCalculator.Snapshot fps) {
        boolean compact = !"DETAILED".equalsIgnoreCase(GradleMCConfig.OVERLAY_MODE.get());
        RuntimeSnapshots.MemorySnapshot memory = RuntimeSnapshots.memory();
        GuiStatusSnapshot serverStatus = GradleMCGuiBridge.latestGuiStatus();
        if (compact) {
            return List.of(compactLine(memory, fps));
        }

        List<String> lines = new ArrayList<>();
        if (GradleMCConfig.OVERLAY_SHOW_FPS.get()) {
            lines.add("FPS: " + whole(fps.currentFps()) + " current, " + whole(fps.averageFps()) + " avg");
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
        integratedServerLine(minecraft).forEach(lines::add);
        lines.add("Tests: FPS " + progressLabel(FpsTestManager.progress())
                + ", perf " + progressLabel(serverStatus.performanceProgress())
                + ", worldgen " + progressLabel(serverStatus.worldgenProgress()));
        lines.add(GradleMcProfilerService.overlayStatusLine());
        lines.add("Stability: " + stabilityLabel(serverStatus));
        return List.copyOf(lines);
    }

    private static String compactLine(RuntimeSnapshots.MemorySnapshot memory, FpsRollingStatsCalculator.Snapshot fps) {
        List<String> parts = new ArrayList<>();
        parts.add("GradleMC");
        if (GradleMCConfig.OVERLAY_SHOW_FPS.get()) {
            parts.add(whole(fps.currentFps()) + " FPS");
            parts.add("avg " + whole(fps.averageFps()));
        }
        if (GradleMCConfig.OVERLAY_SHOW_ONE_PERCENT_LOW.get()) {
            parts.add("1% " + lowLabel(fps.onePercentLowFps()));
        }
        if (GradleMCConfig.OVERLAY_SHOW_POINT_ONE_PERCENT_LOW.get()) {
            parts.add("0.1% " + lowLabel(fps.pointOnePercentLowFps()));
        }
        if (GradleMCConfig.OVERLAY_SHOW_JVM_MEMORY.get()) {
            parts.add("JVM " + formatGiB(memory.usedMiB()) + "/" + formatGiB(memory.maxMiB()) + "G");
        }
        return String.join(" | ", parts);
    }

    private static List<String> integratedServerLine(Minecraft minecraft) {
        MinecraftServer server = minecraft.getSingleplayerServer();
        if (server == null) {
            return List.of();
        }
        double mspt = Math.max(0.0D, server.getAverageTickTimeNanos() / 1_000_000.0D);
        double tps = mspt <= 0.0D ? 20.0D : Math.min(20.0D, 1000.0D / mspt);
        return List.of("Integrated server: " + format(tps) + " TPS, " + format(mspt) + " MSPT");
    }

    private static void draw(GuiGraphicsExtractor graphics, Font font, List<String> lines, int screenWidth, int screenHeight) {
        if (lines.isEmpty()) {
            return;
        }
        double scale = Math.max(0.75D, Math.min(2.0D, GradleMCConfig.OVERLAY_SCALE.get()));
        int maxWidth = 0;
        for (String line : lines) {
            maxWidth = Math.max(maxWidth, font.width(line));
        }
        int lineHeight = font.lineHeight + 2;
        int boxWidth = maxWidth + PADDING * 2;
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

        Matrix3x2fStack pose = graphics.pose();
        pose.pushMatrix();
        pose.translate(x, y);
        pose.scale((float) scale);
        if (GradleMCConfig.OVERLAY_BACKGROUND_ENABLED.get()) {
            int alpha = (int) Math.round(Math.max(0.0D, Math.min(1.0D, GradleMCConfig.OVERLAY_BACKGROUND_OPACITY.get())) * 255.0D);
            graphics.fill(0, 0, boxWidth, boxHeight, (alpha << 24) | 0x10141C);
        }
        int textY = PADDING;
        for (int index = 0; index < lines.size(); index++) {
            graphics.text(font, lines.get(index), PADDING, textY, index == 0 ? TEXT_COLOR : MUTED_COLOR, false);
            textY += lineHeight;
        }
        pose.popMatrix();
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
}
