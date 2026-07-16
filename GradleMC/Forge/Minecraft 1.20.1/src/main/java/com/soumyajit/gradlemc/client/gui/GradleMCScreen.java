package com.soumyajit.gradlemc.client.gui;

import com.soumyajit.gradlemc.client.FpsTestManager;
import com.soumyajit.gradlemc.client.overlay.OverlayConfigActions;
import com.soumyajit.gradlemc.client.gui.model.ModListViewModel;
import com.soumyajit.gradlemc.config.GradleMCConfig;
import com.soumyajit.gradlemc.metrics.DiagnosticTestProgress;
import com.soumyajit.gradlemc.network.GradleMCNetwork;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Client-only, bounded diagnostics workspace. Rendering is deliberately presentation only. */
public final class GradleMCScreen extends Screen {
    private static final int BUTTON_HEIGHT = 20;
    private static final int ROW_HEIGHT = 12;
    private static final int MAX_LINES = 220;
    private static final int MAX_MOD_ROWS = 200;
    private static final int MAX_SEARCH = 96;
    private static final int REFRESH_TICKS = 20;
    private WorkspacePage page = WorkspacePage.OVERVIEW;
    private WorkspaceLayout layout;
    private WorkspaceViewModel viewModel;
    private final ModListViewModel modProjection = new ModListViewModel();
    private final List<Line> lines = new ArrayList<>();
    private String modQuery = "";
    private int scroll;
    private int contentHeight;
    private int refreshCountdown;
    private boolean linesDirty = true;
    private Component status = Component.translatable("screen.gradlemc.status.ready");

    public GradleMCScreen() { super(Component.translatable("screen.gradlemc.title")); }

    @Override protected void init() {
        layout = WorkspaceLayout.calculate(width, height);
        viewModel = WorkspaceViewModel.capture();
        linesDirty = true;
        buildWorkspaceWidgets();
    }

    private void buildWorkspaceWidgets() {
        clearWidgets();
        if (layout.compact()) buildCompactNavigation(); else buildRailNavigation();
        buildPageActions();
        int footerY = layout.footerTop() + 5;
        addRenderableWidget(Button.builder(Component.translatable("screen.gradlemc.button.refresh"), b -> refresh(true))
                .bounds(layout.right() - 190, footerY, 86, BUTTON_HEIGHT)
                .tooltip(Tooltip.create(Component.translatable("screen.gradlemc.tooltip.refresh"))).build());
        addRenderableWidget(Button.builder(Component.translatable("screen.gradlemc.button.close"), b -> onClose())
                .bounds(layout.right() - 98, footerY, 86, BUTTON_HEIGHT).build());
    }

    private void buildRailNavigation() {
        int y = layout.contentTop();
        for (WorkspacePage item : WorkspacePage.values()) {
            Button button = Button.builder(item.label(), b -> select(item)).bounds(layout.navLeft(), y, layout.navWidth() - 4, BUTTON_HEIGHT).build();
            button.active = item != page;
            addRenderableWidget(button); y += BUTTON_HEIGHT + 4;
        }
    }

    private void buildCompactNavigation() {
        int x = layout.left() + 7; int y = layout.top() + 22;
        for (WorkspacePage item : WorkspacePage.values()) {
            int w = Math.max(34, Math.min(82, font.width(item.label()) + 12));
            if (x + w > layout.right() - 7) { x = layout.left() + 7; y += BUTTON_HEIGHT + 2; }
            Button button = Button.builder(item.label(), b -> select(item)).bounds(x, y, w, BUTTON_HEIGHT).build();
            button.active = item != page; addRenderableWidget(button); x += w + 3;
        }
    }

    private void buildPageActions() {
        int x = layout.contentLeft(), y = layout.contentTop();
        switch (page) {
            case OVERVIEW -> { addCommand("Run Standard Scan", "gradlemc run standard", x, y); addCommand("Open Tasks", "gradlemc tasks", x + 126, y); addFps(x + 252, y); }
            case INSTANCE -> { addCommand("Instance lock check", "gradlemc instance lock check", x, y); addCommand("Config check", "gradlemc config check", x + 126, y); }
            case MODS -> {
                EditBox search = new EditBox(font, x, y, Math.min(180, layout.contentWidth() - 100), BUTTON_HEIGHT, Component.translatable("screen.gradlemc.input.mod_search"));
                search.setMaxLength(MAX_SEARCH); search.setValue(modQuery); search.setResponder(value -> { modQuery = value; linesDirty = true; }); addRenderableWidget(search);
                addCommand("Audit", "gradlemc mods audit", x + Math.min(186, layout.contentWidth() - 94), y);
            }
            case PERFORMANCE -> { addFps(x, y); addCommand("Run server performance", "gradlemc perf start 60", x + 126, y); addCommand("Observe worldgen", "gradlemc worldgen start 300", x + 252, y); }
            case TASKS -> { addCommand("List tasks", "gradlemc tasks", x, y); addCommand("Workflow plan", "gradlemc workflows", x + 126, y); }
            case ADAPTIVE -> { addCommand("Refresh evaluation", "gradlemc smart score", x, y); addCommand("View advice", "gradlemc smart advice", x + 126, y); }
            case SCANS -> { addCommand("Run Standard Scan", "gradlemc run standard", x, y); addCommand("List scans", "gradlemc scans list", x + 126, y); }
            case SETTINGS -> {
                addOverlayToggle("Show overlay title", GradleMCConfig.OVERLAY_SHOW_TITLE, "Displays the \"GradleMC\" heading above enabled overlay statistics.", x, y);
                addOverlayToggle("Show current FPS", GradleMCConfig.OVERLAY_SHOW_FPS, "Displays the recent FPS value from completed rendered frames.", x + 126, y);
                addOverlayToggle("Show average FPS", GradleMCConfig.OVERLAY_SHOW_AVERAGE_FPS, "Displays average FPS across the rolling measurement window.", x + 252, y);
            }
        }
    }

    private void addFps(int x, int y) {
        if (x < layout.contentLeft() || x + 120 > layout.right() - 7) return;
        Button button = Button.builder(Component.literal("Run FPS Test"), b -> { FpsTestManager.startFromClient(60); status = Component.literal("FPS sample requested"); })
                .bounds(x, y, 120, BUTTON_HEIGHT).tooltip(Tooltip.create(Component.translatable("screen.gradlemc.tooltip.fps_start"))).build();
        button.active = viewModel == null || !viewModel.state().fpsTestRunning(); addRenderableWidget(button);
    }
    private void addCommand(String label, String command, int x, int y) {
        // Never create an off-viewport widget: invisible controls must not accept clicks.
        if (x < layout.contentLeft() || x + 120 > layout.right() - 7) return;
        addRenderableWidget(Button.builder(Component.literal(label), b -> command(command)).bounds(x, y, 120, BUTTON_HEIGHT).build());
    }
    private void addOverlayToggle(String label, net.minecraftforge.common.ForgeConfigSpec.BooleanValue value, String tooltip, int x, int y) {
        if (x < layout.contentLeft() || x + 120 > layout.right() - 7) return;
        Button button = Button.builder(Component.literal(label + ": " + onOff(value.get())), b -> {
                    OverlayConfigActions.setBoolean(value, !value.get());
                    status = Component.literal(label + " " + onOff(value.get()).toLowerCase(Locale.ROOT));
                    linesDirty = true;
                    buildWorkspaceWidgets();
                })
                .bounds(x, y, 120, BUTTON_HEIGHT).tooltip(Tooltip.create(Component.literal(tooltip))).build();
        addRenderableWidget(button);
    }
    private void select(WorkspacePage next) { page = next; scroll = 0; linesDirty = true; buildWorkspaceWidgets(); }
    private void command(String command) {
        if (minecraft == null || minecraft.player == null || minecraft.player.connection == null) { status = Component.translatable("screen.gradlemc.status.no_connection"); return; }
        minecraft.player.connection.sendCommand(command); status = Component.literal("Requested: /" + command);
    }
    private void refresh(boolean requestServer) {
        viewModel = WorkspaceViewModel.capture(); refreshCountdown = REFRESH_TICKS;
        if (requestServer && minecraft != null && minecraft.player != null && minecraft.getConnection() != null) GradleMCNetwork.requestSmartAIStatus();
        status = Component.translatable("screen.gradlemc.status.refreshed"); linesDirty = true;
    }
    @Override public void tick() { if (--refreshCountdown <= 0) refresh(false); }

    @Override public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics); renderShell(graphics); if (linesDirty) rebuildLines(); renderContent(graphics); super.render(graphics, mouseX, mouseY, partialTick);
    }
    private void renderShell(GuiGraphics g) {
        g.fill(layout.left(), layout.top(), layout.right(), layout.bottom(), WorkspaceTheme.BACKGROUND);
        g.fill(layout.left(), layout.headerBottom(), layout.right(), layout.headerBottom() + 1, WorkspaceTheme.BORDER);
        g.fill(layout.left(), layout.footerTop(), layout.right(), layout.footerTop() + 1, WorkspaceTheme.BORDER);
        if (!layout.compact()) g.fill(layout.contentLeft() - 12, layout.headerBottom(), layout.contentLeft() - 11, layout.footerTop(), WorkspaceTheme.BORDER);
        g.drawString(font, title, layout.left() + 10, layout.top() + 9, WorkspaceTheme.TEXT);
        g.drawString(font, page.label(), layout.contentLeft(), layout.top() + 9, WorkspaceTheme.BLUE);
        g.drawString(font, font.plainSubstrByWidth(status.getString(), Math.max(30, layout.right() - layout.left() - 210)), layout.left() + 10, layout.footerTop() + 12, WorkspaceTheme.MUTED);
    }
    private void rebuildLines() {
        linesDirty = false; lines.clear(); if (viewModel == null) return;
        var s = viewModel.state();
        switch (page) {
            case OVERVIEW -> { heading("Diagnostics overview"); kv("GradleMC", s.modVersion()); kv("Minecraft / Forge", s.minecraftVersion() + " / " + s.forgeVersion()); kv("Evidence freshness", age(s.smartAIStatusAgeMillis())); kv("Memory", s.memory().usedMiB()+" / "+s.memory().maxMiB()+" MiB"); kv("FPS", s.currentFps()+" current; "+whole(s.rollingAverageFps())+" rolling average"); task("FPS sample", s.fpsProgress()); task("Server sample", s.performanceProgress()); task("Worldgen observation", s.worldgenProgress()); limitation("Health score is unavailable until a completed compatible server summary exists."); }
            case INSTANCE -> { heading("Identity"); kv("GradleMC", s.modVersion()); kv("Minecraft", s.minecraftVersion()); kv("Forge", s.forgeVersion()); kv("Player context", blank(s.playerName())); heading("Environment"); kv("JVM heap", s.memory().usedMiB()+" used / "+s.memory().maxMiB()+" max MiB"); heading("Packs / Configs / World"); limitation("Pack, config, and loaded-world inventories are only exposed by their explicit diagnostics; opening this page does not collect them."); }
            case MODS -> mods();
            case PERFORMANCE -> { heading("Runtime diagnostics"); kv("Current FPS", Integer.toString(s.currentFps())); kv("Rolling FPS", whole(s.rollingAverageFps())); task("FPS test", s.fpsProgress()); task("Server tick test", s.performanceProgress()); task("Worldgen observation", s.worldgenProgress()); kv("Latest server test", blank(s.guiStatus().latestPerformanceReportPath())); kv("Latest worldgen observation", blank(s.guiStatus().latestWorldgenReportPath())); limitation("No profiler result is synchronized into the GUI status model yet; use the existing profiler command workflow."); }
            case TASKS -> { heading("Task engine"); limitation("The registered task graph is available through the existing bounded command output. This GUI does not invent task history or progress that the current snapshot does not provide."); kv("Command", "/gradlemc tasks"); kv("Workflow plan", "/gradlemc workflows"); }
            case ADAPTIVE -> { heading("Adaptive diagnostics"); kv("Risk", s.smartAIStatus().threatLevel().name()+" ("+s.smartAIStatus().threatScore()+")"); kv("Status age", age(s.smartAIStatusAgeMillis())); kv("Technical score", s.guiStatus().technicalStabilityScore() < 0 ? "Insufficient evidence" : Integer.toString(s.guiStatus().technicalStabilityScore())); limitation("Recommendation evidence and verification plans are not present in the current GUI status packet, so they are not fabricated here."); }
            case SCANS -> { heading("GradleMC Scans"); kv("Latest report", blank(s.guiStatus().latestReportPath())); kv("Summary", blank(s.guiStatus().latestReportSummary())); limitation("The local scan manifest index is not exposed by a safe client service. Use the existing scans command; this page performs no directory parsing."); }
            case SETTINGS -> { heading("Real client settings"); kv("Overlay", onOff(GradleMCConfig.OVERLAY_ENABLED.get())); kv("Show overlay title", onOff(GradleMCConfig.OVERLAY_SHOW_TITLE.get())); kv("Show current FPS", onOff(GradleMCConfig.OVERLAY_SHOW_FPS.get())); kv("Show average FPS", onOff(GradleMCConfig.OVERLAY_SHOW_AVERAGE_FPS.get())); kv("Overlay mode", GradleMCConfig.OVERLAY_MODE.get()); kv("Overlay scale", String.format(Locale.ROOT, "%.2f", GradleMCConfig.OVERLAY_SCALE.get())); kv("Sampling window", GradleMCConfig.OVERLAY_SAMPLING_WINDOW_SECONDS.get()+" seconds"); limitation("Keybind remains configurable in Minecraft Controls. No second settings store was created."); }
        }
        contentHeight = lines.stream().mapToInt(Line::height).sum(); scroll = WorkspaceScroll.clamp(scroll, contentHeight, viewportHeight());
    }
    private void mods() {
        heading("Loaded mod inventory"); var audit = viewModel.modAudit(); var projected = modProjection.project(audit, modQuery);
        kv("Loaded", audit.snapshot().mods().size()+" mods; "+audit.findings().size()+" metadata/dependency findings");
        for (var mod : projected.mods().stream().limit(MAX_MOD_ROWS).toList()) line(mod.displayName()+" | "+mod.modId()+" | "+mod.version());
        if (projected.truncated()) limitation("Showing "+MAX_MOD_ROWS+" of "+projected.totalMatches()+" matches; refine the search.");
        if (!audit.snapshot().available()) limitation("Forge metadata unavailable: "+audit.snapshot().unavailableReason());
    }
    private void heading(String text) { lines.add(new Line(text, WorkspaceTheme.BLUE, ROW_HEIGHT + 3)); }
    private void kv(String label, String value) { line(label + ": " + value); }
    private void line(String text) { lines.add(new Line(text, WorkspaceTheme.TEXT, ROW_HEIGHT)); }
    private void limitation(String text) { lines.add(new Line("Limit: " + text, WorkspaceTheme.WARNING, ROW_HEIGHT)); }
    private void task(String label, DiagnosticTestProgress p) { kv(label, p != null && p.running() ? p.clampedElapsedSeconds()+"/"+p.requestedSeconds()+" s" : "Not running"); }
    private String blank(String value) { return value == null || value.isBlank() ? "Unavailable" : value; }
    private String age(long millis) { return millis < 0 ? "Unavailable" : millis < 5000 ? "Fresh" : (millis / 1000)+" seconds old"; }
    private String whole(double value) { return String.format(Locale.ROOT, "%.0f", value); }
    private String onOff(boolean enabled) { return enabled ? "Enabled" : "Disabled"; }
    private int viewportTop() { return layout.contentTop() + BUTTON_HEIGHT + 8; }
    private int viewportHeight() { return Math.max(20, layout.footerTop() - viewportTop() - 8); }
    private void renderContent(GuiGraphics g) {
        int x=layout.contentLeft(), top=viewportTop(), h=viewportHeight(); g.fill(x-5, top-4, x+layout.contentWidth()+5, top+h+4, WorkspaceTheme.PANEL);
        int y=top-scroll; g.enableScissor(x, top, x+layout.contentWidth(), top+h);
        try { for (Line line:lines) { if(y+line.height()>=top && y<=top+h) g.drawString(font, font.plainSubstrByWidth(line.text(), layout.contentWidth()-5), x, y, line.color()); y+=line.height(); } }
        finally { g.disableScissor(); }
        int max=WorkspaceScroll.clamp(Integer.MAX_VALUE,contentHeight,h); if(max>0){int thumb=Math.max(14,h*h/contentHeight);int thumbY=top+(h-thumb)*scroll/max;g.fill(x+layout.contentWidth()+2,top,x+layout.contentWidth()+4,top+h,WorkspaceTheme.BORDER);g.fill(x+layout.contentWidth()+2,thumbY,x+layout.contentWidth()+4,thumbY+thumb,WorkspaceTheme.CYAN);}
    }
    @Override public boolean mouseScrolled(double mouseX,double mouseY,double delta){if(mouseX>=layout.contentLeft()&&mouseX<=layout.contentLeft()+layout.contentWidth()&&mouseY>=viewportTop()&&mouseY<=viewportTop()+viewportHeight()){scroll=WorkspaceScroll.clamp(scroll-(int)(delta*18),contentHeight,viewportHeight());return true;}return super.mouseScrolled(mouseX,mouseY,delta);}
    @Override public boolean keyPressed(int key,int scan,int mods){if(key==org.lwjgl.glfw.GLFW.GLFW_KEY_HOME){scroll=0;return true;}if(key==org.lwjgl.glfw.GLFW.GLFW_KEY_END){scroll=WorkspaceScroll.clamp(Integer.MAX_VALUE,contentHeight,viewportHeight());return true;}if(key==org.lwjgl.glfw.GLFW.GLFW_KEY_PAGE_DOWN||key==org.lwjgl.glfw.GLFW.GLFW_KEY_PAGE_UP){int d=key==org.lwjgl.glfw.GLFW.GLFW_KEY_PAGE_DOWN?viewportHeight():-viewportHeight();scroll=WorkspaceScroll.clamp(scroll+d,contentHeight,viewportHeight());return true;}return super.keyPressed(key,scan,mods);}
    @Override public boolean shouldCloseOnEsc(){return true;}
    private record Line(String text,int color,int height) { }
}
