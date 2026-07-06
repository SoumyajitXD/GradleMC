package com.soumyajit.gradlemc.client.gui;

import com.soumyajit.gradlemc.GradleMC;
import com.soumyajit.gradlemc.ai.SmartAIStatus;
import com.soumyajit.gradlemc.client.FpsTestManager;
import com.soumyajit.gradlemc.client.gui.model.GradleMCGuiState;
import com.soumyajit.gradlemc.client.overlay.OverlayConfigActions;
import com.soumyajit.gradlemc.client.overlay.OverlayPosition;
import com.soumyajit.gradlemc.config.GradleMCConfig;
import com.soumyajit.gradlemc.metrics.DiagnosticTestProgress;
import com.soumyajit.gradlemc.network.GradleMCGuiBridge;
import com.soumyajit.gradlemc.network.GuiStatusSnapshot;
import com.soumyajit.gradlemc.util.GradleMcPaths;
import com.soumyajit.gradlemc.network.GradleMCNetwork;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Util;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class GradleMCScreen extends Screen {
    private static final int MIN_CONTENT_WIDTH = 380;
    private static final int MAX_CONTENT_WIDTH = 840;
    private static final int HEADER_HEIGHT = 58;
    private static final int FOOTER_HEIGHT = 34;
    private static final int SIDEBAR_WIDTH = 128;
    private static final int TAB_HEIGHT = 22;
    private static final int BUTTON_HEIGHT = 20;
    private static final int MARGIN = 14;
    private static final int GAP = 8;
    private static final int CONTENT_AFTER_WIDGET_GAP = GAP + 4;
    private static final int LINE_HEIGHT = 11;
    private static final int SCROLL_STEP = 18;
    private static final long REFRESH_COOLDOWN_MS = 750L;
    private static final int BACKGROUND = 0xE010141C;
    private static final int PANEL = 0xD0182230;
    private static final int PANEL_DARK = 0xE00C1118;
    private static final int BORDER = 0x70465D78;
    private static final int TEXT = 0xFFEAF0F7;
    private static final int MUTED = 0xFFB8C3D1;
    private static final int DIM = 0xFF8793A3;
    private static final int GOOD = 0xFF77D38A;
    private static final int WARN = 0xFFE6C15A;
    private static final int BAD = 0xFFFF7777;
    private static final int[] DURATIONS = {30, 60, 300, 600};
    private static final int[] WORLDGEN_DURATIONS = {60, 300, 600};
    private static final int[] PROFILER_DURATIONS = {30, 60, 300, 600};
    private static final int[] PROFILER_INTERVALS = {4, 10, 20, 50, 100};
    private static final int[] PROFILER_THRESHOLDS = {50, 75, 100, 150};
    private static final String[] PROFILER_MODES = {"tick", "cpu-lite", "memory-lite", "combined"};
    private static final String[] PROFILER_THREADS = {"server", "render", "all", "custom"};

    private final List<ContentLine> contentLines = new ArrayList<>();
    private GradleMCGuiSection selectedSection = GradleMCGuiSection.OVERVIEW;
    private int scrollOffset;
    private int contentHeight;
    private int currentContentWidth;
    private int widgetContentBottom;
    private int ticksUntilStatusRefresh;
    private long lastRefreshMillis;
    private String modSearchText = "";
    private int entityRadius = 64;
    private int blockEntityRadius = 64;
    private int selectedFpsDuration = 60;
    private int selectedPerfDuration = 60;
    private int selectedWorldgenDuration = 300;
    private int selectedProfilerDuration = 60;
    private int selectedProfilerInterval = 20;
    private int selectedProfilerThreshold = 50;
    private int selectedProfilerModeIndex = 3;
    private int selectedProfilerThreadIndex = 0;
    private boolean profilerOnlySlowTicks = true;
    private boolean profilerIncludeSleeping;
    private String profilerThreadPattern = "server";
    private Component statusLine = Component.translatable("screen.gradlemc.status.ready");

    public GradleMCScreen() {
        super(Component.translatable("screen.gradlemc.title"));
    }

    @Override
    protected void init() {
        rebuildGuiWidgets();
        requestStatusRefresh(true);
        ticksUntilStatusRefresh = refreshIntervalTicks();
    }

    private void rebuildGuiWidgets() {
        clearWidgets();
        Layout layout = layout();
        int tabX = layout.left() + MARGIN;
        int tabY = layout.mainTop();
        for (GradleMCGuiSection section : GradleMCGuiSection.values()) {
            Button tab = Button.builder(section.label(), button -> {
                selectedSection = section;
                scrollOffset = 0;
                rebuildGuiWidgets();
            }).bounds(tabX, tabY, SIDEBAR_WIDTH - MARGIN, TAB_HEIGHT).build();
            tab.active = section != selectedSection;
            addRenderableWidget(tab);
            tabY += TAB_HEIGHT + 4;
        }
        buildSectionWidgets(layout);
        int buttonY = layout.footerTop() + 7;
        addRenderableWidget(Button.builder(Component.translatable("screen.gradlemc.button.refresh"), button -> requestStatusRefresh(true))
                .bounds(layout.right() - MARGIN - 188, buttonY, 88, BUTTON_HEIGHT)
                .tooltip(Tooltip.create(Component.translatable("screen.gradlemc.tooltip.refresh")))
                .build());
        addRenderableWidget(Button.builder(Component.translatable("screen.gradlemc.button.close"), button -> onClose())
                .bounds(layout.right() - MARGIN - 92, buttonY, 92, BUTTON_HEIGHT)
                .build());
    }

    private void buildSectionWidgets(Layout layout) {
        int x = layout.contentLeft();
        int y = layout.mainTop();
        int width = layout.contentWidth();
        widgetContentBottom = y;
        switch (selectedSection) {
            case QUICK_ACTIONS -> buildQuickActionWidgets(x, y, width);
            case TESTS -> buildTestWidgets(x, y, width);
            case PROFILER -> buildProfilerWidgets(x, y, width);
            case REPORTS -> buildReportWidgets(x, y, width);
            case SETTINGS -> buildSettingWidgets(x, y, width);
            default -> {
            }
        }
    }

    private void buildQuickActionWidgets(int x, int y, int width) {
        GradleMCGuiState state = GradleMCGuiState.capture(GradleMCGuiBridge.latestSmartAIStatus());
        GridSpec grid = grid(width, 3, 88);
        boolean compact = grid.columnWidth() < 112;
        int item = 0;
        addQuickCommandButton("screen.gradlemc.action.status", "Status", compact, "gradlemc status", gridX(x, grid, item), gridY(y, grid, item), grid.columnWidth(), "screen.gradlemc.tooltip.status");
        item++;
        addQuickCommandButton("screen.gradlemc.action.memory", "Memory", compact, "gradlemc memory", gridX(x, grid, item), gridY(y, grid, item), grid.columnWidth(), "screen.gradlemc.tooltip.memory");
        item++;
        addQuickCommandButton("screen.gradlemc.action.smart_score", "Score", compact, "gradlemc smart score", gridX(x, grid, item), gridY(y, grid, item), grid.columnWidth(), "screen.gradlemc.tooltip.smart_score");
        item++;
        addQuickCommandButton("screen.gradlemc.action.smart_advice", "Advice", compact, "gradlemc smart advice", gridX(x, grid, item), gridY(y, grid, item), grid.columnWidth(), "screen.gradlemc.tooltip.smart_advice");
        item++;
        addQuickCommandButton("screen.gradlemc.action.smart_explain", "Explain", compact, "gradlemc smart explain", gridX(x, grid, item), gridY(y, grid, item), grid.columnWidth(), "screen.gradlemc.tooltip.smart_explain");
        item++;
        addQuickCommandButton("screen.gradlemc.action.ai_status", "AI State", compact, "gradlemc ai status", gridX(x, grid, item), gridY(y, grid, item), grid.columnWidth(), "screen.gradlemc.tooltip.ai_status");
        item++;
        addQuickCommandButton("screen.gradlemc.action.mods_count", "Mods", compact, "gradlemc mods count", gridX(x, grid, item), gridY(y, grid, item), grid.columnWidth(), "screen.gradlemc.tooltip.mods_count");
        item++;
        addQuickCommandButton("screen.gradlemc.action.export", "Export", compact, "gradlemc export", gridX(x, grid, item), gridY(y, grid, item), grid.columnWidth(), "screen.gradlemc.tooltip.export");
        item++;
        addQuickCommandButton("screen.gradlemc.action.issue_bundle", "Bundle", compact, "gradlemc issuebundle create", gridX(x, grid, item), gridY(y, grid, item), grid.columnWidth(), "screen.gradlemc.tooltip.issue_bundle");
        item++;

        EditBox modSearch = new EditBox(font, gridX(x, grid, item), gridY(y, grid, item), grid.columnWidth(), BUTTON_HEIGHT, Component.translatable("screen.gradlemc.input.mod_search"));
        modSearch.setValue(modSearchText);
        modSearch.setResponder(value -> modSearchText = value);
        modSearch.setHint(Component.translatable("screen.gradlemc.input.mod_search"));
        addRenderableWidget(modSearch);
        item++;
        addRenderableWidget(Button.builder(Component.translatable("screen.gradlemc.action.search"), button -> {
                    String query = modSearchText.trim();
                    if (query.isEmpty()) {
                        setStatus(Component.translatable("screen.gradlemc.status.enter_modid"));
                    } else {
                        runServerCommand("gradlemc mods search " + query);
                    }
                })
                .bounds(gridX(x, grid, item), gridY(y, grid, item), grid.columnWidth(), BUTTON_HEIGHT)
                .tooltip(Tooltip.create(Component.translatable("screen.gradlemc.tooltip.mod_search")))
                .build());
        item++;
        addQuickCommandButton("screen.gradlemc.action.latest_report", "Latest", compact, "gradlemc reports latest", gridX(x, grid, item), gridY(y, grid, item), grid.columnWidth(), "screen.gradlemc.tooltip.latest_report");
        item++;

        addRenderableWidget(Button.builder(Component.literal((compact ? "Ent: " : "Entities: ") + entityRadius), button -> {
                    entityRadius = nextRadius(entityRadius);
                    rebuildGuiWidgets();
                }).bounds(gridX(x, grid, item), gridY(y, grid, item), grid.columnWidth(), BUTTON_HEIGHT)
                .tooltip(Tooltip.create(Component.translatable("screen.gradlemc.tooltip.radius")))
                .build());
        item++;
        addRenderableWidget(Button.builder(quickLabel("screen.gradlemc.action.scan_entities", "Scan Ent", compact), button -> runServerCommand("gradlemc entities " + entityRadius))
                .bounds(gridX(x, grid, item), gridY(y, grid, item), grid.columnWidth(), BUTTON_HEIGHT)
                .tooltip(Tooltip.create(Component.translatable("screen.gradlemc.tooltip.entities")))
                .build());
        item++;
        addRenderableWidget(Button.builder(Component.literal((compact ? "Block: " : "Block entities: ") + blockEntityRadius), button -> {
                    blockEntityRadius = nextRadius(blockEntityRadius);
                    rebuildGuiWidgets();
                }).bounds(gridX(x, grid, item), gridY(y, grid, item), grid.columnWidth(), BUTTON_HEIGHT)
                .tooltip(Tooltip.create(Component.translatable("screen.gradlemc.tooltip.radius")))
                .build());
        item++;
        addRenderableWidget(Button.builder(quickLabel("screen.gradlemc.action.scan_block_entities", "Scan Block", compact), button -> runServerCommand("gradlemc blockentities " + blockEntityRadius))
                .bounds(gridX(x, grid, item), gridY(y, grid, item), grid.columnWidth(), BUTTON_HEIGHT)
                .tooltip(Tooltip.create(Component.translatable("screen.gradlemc.tooltip.block_entities")))
                .build());
        item++;
        addDurationButton("FPS", gridX(x, grid, item), gridY(y, grid, item), grid.columnWidth(), true);
        item++;
        Button startFps = Button.builder(quickLabel("screen.gradlemc.action.start_selected_fps", "Start FPS", compact), button -> startFps(selectedFpsDuration))
                .bounds(gridX(x, grid, item), gridY(y, grid, item), grid.columnWidth(), BUTTON_HEIGHT)
                .tooltip(Tooltip.create(Component.translatable("screen.gradlemc.tooltip.fps_start")))
                .build();
        startFps.active = !state.fpsTestRunning();
        addRenderableWidget(startFps);
        item++;
        addDurationButton("Perf", gridX(x, grid, item), gridY(y, grid, item), grid.columnWidth(), false);
        item++;
        Button startPerf = commandButton(quickLabel("screen.gradlemc.action.start_selected_perf", "Start Perf", compact),
                "gradlemc perf start " + selectedPerfDuration, gridX(x, grid, item), gridY(y, grid, item), grid.columnWidth(), "screen.gradlemc.tooltip.perf_start");
        startPerf.active = !state.performanceTestRunning();
        addRenderableWidget(startPerf);
        item++;
        addDurationButton("Worldgen", gridX(x, grid, item), gridY(y, grid, item), grid.columnWidth(), false);
        item++;
        Button startWorldgen = commandButton(quickLabel("screen.gradlemc.action.start_selected_worldgen", "Start WG", compact),
                "gradlemc worldgen start " + selectedWorldgenDuration, gridX(x, grid, item), gridY(y, grid, item), grid.columnWidth(), "screen.gradlemc.tooltip.worldgen_start");
        startWorldgen.active = !state.worldgenObservationRunning();
        addRenderableWidget(startWorldgen);
        item++;
        addRenderableWidget(Button.builder(quickLabel("screen.gradlemc.action.open_folder", "Folder", compact), button -> openOutputFolder())
                .bounds(gridX(x, grid, item), gridY(y, grid, item), grid.columnWidth(), BUTTON_HEIGHT)
                .tooltip(Tooltip.create(Component.translatable("screen.gradlemc.tooltip.open_folder")))
                .build());
        item++;
        addRenderableWidget(Button.builder(quickLabel("screen.gradlemc.action.copy_latest_path", "Copy Path", compact), button -> copyLatestPath())
                .bounds(gridX(x, grid, item), gridY(y, grid, item), grid.columnWidth(), BUTTON_HEIGHT)
                .tooltip(Tooltip.create(Component.translatable("screen.gradlemc.tooltip.copy_path")))
                .build());
        item++;
        markWidgetContentBottom(gridLastRowY(y, grid, item));
    }

    private void buildTestWidgets(int x, int y, int width) {
        GradleMCGuiState state = GradleMCGuiState.capture(GradleMCGuiBridge.latestSmartAIStatus());
        GridSpec grid = grid(width, 4, 106);
        int item = 0;
        for (int duration : DURATIONS) {
            Button button = Button.builder(Component.literal(duration + "s FPS"), ignored -> startFps(duration))
                    .bounds(gridX(x, grid, item), gridY(y, grid, item), grid.columnWidth(), BUTTON_HEIGHT)
                    .tooltip(Tooltip.create(Component.translatable("screen.gradlemc.tooltip.fps_start")))
                    .build();
            button.active = !state.fpsTestRunning();
            addRenderableWidget(button);
            item++;
        }
        for (int duration : DURATIONS) {
            Button button = commandButton(Component.literal(duration + "s perf"), "gradlemc perf start " + duration,
                    gridX(x, grid, item), gridY(y, grid, item), grid.columnWidth(), "screen.gradlemc.tooltip.perf_start");
            button.active = !state.performanceTestRunning();
            addRenderableWidget(button);
            item++;
        }
        for (int duration : WORLDGEN_DURATIONS) {
            Button button = commandButton(Component.literal(duration + "s worldgen"), "gradlemc worldgen start " + duration,
                    gridX(x, grid, item), gridY(y, grid, item), grid.columnWidth(), "screen.gradlemc.tooltip.worldgen_start");
            button.active = !state.worldgenObservationRunning();
            addRenderableWidget(button);
            item++;
        }
        Button stopFps = Button.builder(Component.translatable("screen.gradlemc.action.stop_fps"), ignored -> {
            FpsTestManager.stopFromClient();
            rebuildGuiWidgets();
        }).bounds(gridX(x, grid, item), gridY(y, grid, item), grid.columnWidth(), BUTTON_HEIGHT).build();
        stopFps.active = state.fpsTestRunning();
        addRenderableWidget(stopFps);
        item++;
        Button stopPerf = commandButton(Component.translatable("screen.gradlemc.action.stop_perf"), "gradlemc perf stop", gridX(x, grid, item), gridY(y, grid, item), grid.columnWidth(), "screen.gradlemc.tooltip.stop");
        stopPerf.active = state.performanceTestRunning();
        addRenderableWidget(stopPerf);
        item++;
        Button stopWorldgen = commandButton(Component.translatable("screen.gradlemc.action.stop_worldgen"), "gradlemc worldgen stop", gridX(x, grid, item), gridY(y, grid, item), grid.columnWidth(), "screen.gradlemc.tooltip.stop");
        stopWorldgen.active = state.worldgenObservationRunning();
        addRenderableWidget(stopWorldgen);
        item++;
        markWidgetContentBottom(gridLastRowY(y, grid, item));
    }

    private void buildProfilerWidgets(int x, int y, int width) {
        GridSpec grid = grid(width, 3, 112);
        int item = 0;
        addRenderableWidget(Button.builder(Component.literal("Mode: " + profilerMode()), button -> {
                    selectedProfilerModeIndex = (selectedProfilerModeIndex + 1) % PROFILER_MODES.length;
                    rebuildGuiWidgets();
                })
                .bounds(gridX(x, grid, item), gridY(y, grid, item), grid.columnWidth(), BUTTON_HEIGHT)
                .tooltip(Tooltip.create(Component.translatable("screen.gradlemc.profiler.tooltip.mode")))
                .build());
        item++;
        addRenderableWidget(Button.builder(Component.literal("Duration: " + selectedProfilerDuration + "s"), button -> {
                    selectedProfilerDuration = nextOf(selectedProfilerDuration, PROFILER_DURATIONS);
                    rebuildGuiWidgets();
                })
                .bounds(gridX(x, grid, item), gridY(y, grid, item), grid.columnWidth(), BUTTON_HEIGHT)
                .tooltip(Tooltip.create(Component.translatable("screen.gradlemc.profiler.tooltip.duration")))
                .build());
        item++;
        addRenderableWidget(Button.builder(Component.literal("Interval: " + selectedProfilerInterval + "ms"), button -> {
                    selectedProfilerInterval = nextOf(selectedProfilerInterval, PROFILER_INTERVALS);
                    rebuildGuiWidgets();
                })
                .bounds(gridX(x, grid, item), gridY(y, grid, item), grid.columnWidth(), BUTTON_HEIGHT)
                .tooltip(Tooltip.create(Component.translatable("screen.gradlemc.profiler.tooltip.interval")))
                .build());
        item++;
        addRenderableWidget(Button.builder(Component.literal("Thread: " + profilerThreadLabel()), button -> {
                    selectedProfilerThreadIndex = (selectedProfilerThreadIndex + 1) % PROFILER_THREADS.length;
                    profilerThreadPattern = switch (PROFILER_THREADS[selectedProfilerThreadIndex]) {
                        case "all" -> "*";
                        case "render" -> "render";
                        case "custom" -> profilerThreadPattern.isBlank() ? "server" : profilerThreadPattern;
                        default -> "server";
                    };
                    rebuildGuiWidgets();
                })
                .bounds(gridX(x, grid, item), gridY(y, grid, item), grid.columnWidth(), BUTTON_HEIGHT)
                .tooltip(Tooltip.create(Component.translatable("screen.gradlemc.profiler.tooltip.thread")))
                .build());
        item++;
        addRenderableWidget(Button.builder(Component.literal("Slow tick: " + selectedProfilerThreshold + "ms"), button -> {
                    selectedProfilerThreshold = nextOf(selectedProfilerThreshold, PROFILER_THRESHOLDS);
                    rebuildGuiWidgets();
                })
                .bounds(gridX(x, grid, item), gridY(y, grid, item), grid.columnWidth(), BUTTON_HEIGHT)
                .tooltip(Tooltip.create(Component.translatable("screen.gradlemc.profiler.tooltip.threshold")))
                .build());
        item++;
        addRenderableWidget(Button.builder(Component.literal("Slow-only: " + onOff(profilerOnlySlowTicks)), button -> {
                    profilerOnlySlowTicks = !profilerOnlySlowTicks;
                    rebuildGuiWidgets();
                })
                .bounds(gridX(x, grid, item), gridY(y, grid, item), grid.columnWidth(), BUTTON_HEIGHT)
                .tooltip(Tooltip.create(Component.translatable("screen.gradlemc.profiler.tooltip.slow_only")))
                .build());
        item++;
        if ("custom".equals(PROFILER_THREADS[selectedProfilerThreadIndex])) {
            EditBox threadPattern = new EditBox(font, gridX(x, grid, item), gridY(y, grid, item), grid.columnWidth(), BUTTON_HEIGHT, Component.literal("Thread pattern"));
            threadPattern.setValue(profilerThreadPattern);
            threadPattern.setResponder(value -> profilerThreadPattern = value);
            threadPattern.setHint(Component.literal("thread pattern"));
            addRenderableWidget(threadPattern);
        } else {
            addRenderableWidget(Button.builder(Component.literal("Include sleeping: " + onOff(profilerIncludeSleeping)), button -> {
                        profilerIncludeSleeping = !profilerIncludeSleeping;
                        rebuildGuiWidgets();
                    })
                    .bounds(gridX(x, grid, item), gridY(y, grid, item), grid.columnWidth(), BUTTON_HEIGHT)
                    .tooltip(Tooltip.create(Component.translatable("screen.gradlemc.profiler.tooltip.sleeping")))
                    .build());
        }
        item++;
        addRenderableWidget(Button.builder(Component.translatable("screen.gradlemc.profiler.start"), button -> runServerCommand(profilerStartCommand()))
                .bounds(gridX(x, grid, item), gridY(y, grid, item), grid.columnWidth(), BUTTON_HEIGHT)
                .tooltip(Tooltip.create(Component.translatable("screen.gradlemc.profiler.tooltip.start")))
                .build());
        item++;
        addCommandButton("screen.gradlemc.profiler.status", "gradlemc profiler status", gridX(x, grid, item), gridY(y, grid, item), grid.columnWidth(), "screen.gradlemc.profiler.tooltip.status");
        item++;
        addCommandButton("screen.gradlemc.profiler.stop", "gradlemc profiler stop", gridX(x, grid, item), gridY(y, grid, item), grid.columnWidth(), "screen.gradlemc.profiler.tooltip.stop");
        item++;
        addCommandButton("screen.gradlemc.profiler.cancel", "gradlemc profiler cancel", gridX(x, grid, item), gridY(y, grid, item), grid.columnWidth(), "screen.gradlemc.profiler.tooltip.cancel");
        item++;
        addCommandButton("screen.gradlemc.profiler.summary", "gradlemc profiler summary", gridX(x, grid, item), gridY(y, grid, item), grid.columnWidth(), "screen.gradlemc.profiler.tooltip.summary");
        item++;
        addCommandButton("screen.gradlemc.profiler.latest", "gradlemc profiler latest", gridX(x, grid, item), gridY(y, grid, item), grid.columnWidth(), "screen.gradlemc.profiler.tooltip.latest");
        item++;
        addRenderableWidget(Button.builder(Component.translatable("screen.gradlemc.profiler.open_folder"), button -> openProfilesFolder())
                .bounds(gridX(x, grid, item), gridY(y, grid, item), grid.columnWidth(), BUTTON_HEIGHT)
                .tooltip(Tooltip.create(Component.translatable("screen.gradlemc.profiler.tooltip.open")))
                .build());
        item++;
        addRenderableWidget(Button.builder(Component.translatable("screen.gradlemc.profiler.copy_path"), button -> copyLatestProfilePath())
                .bounds(gridX(x, grid, item), gridY(y, grid, item), grid.columnWidth(), BUTTON_HEIGHT)
                .tooltip(Tooltip.create(Component.translatable("screen.gradlemc.tooltip.copy_path")))
                .build());
        item++;
        markWidgetContentBottom(gridLastRowY(y, grid, item));
    }

    private void buildReportWidgets(int x, int y, int width) {
        GridSpec grid = grid(width, 3, 120);
        int item = 0;
        addCommandButton("screen.gradlemc.action.view_summary", "gradlemc reports latest", gridX(x, grid, item), gridY(y, grid, item), grid.columnWidth(), "screen.gradlemc.tooltip.latest_report");
        item++;
        addRenderableWidget(Button.builder(Component.translatable("screen.gradlemc.action.copy_path"), button -> copyLatestPath())
                .bounds(gridX(x, grid, item), gridY(y, grid, item), grid.columnWidth(), BUTTON_HEIGHT)
                .tooltip(Tooltip.create(Component.translatable("screen.gradlemc.tooltip.copy_path")))
                .build());
        item++;
        addRenderableWidget(Button.builder(Component.translatable("screen.gradlemc.action.open_folder"), button -> openOutputFolder())
                .bounds(gridX(x, grid, item), gridY(y, grid, item), grid.columnWidth(), BUTTON_HEIGHT)
                .tooltip(Tooltip.create(Component.translatable("screen.gradlemc.tooltip.open_folder")))
                .build());
        item++;
        markWidgetContentBottom(gridLastRowY(y, grid, item));
    }

    private void buildSettingWidgets(int x, int y, int width) {
        GridSpec grid = grid(width, 3, 120);
        int item = 0;
        addSettingButton(toggleLabel("screen.gradlemc.setting.overlay_enabled", GradleMCConfig.OVERLAY_ENABLED.get()),
                () -> OverlayConfigActions.toggleEnabled(), gridX(x, grid, item), gridY(y, grid, item), grid.columnWidth());
        item++;
        addSettingButton(Component.literal("Mode: " + modeLabel()), () -> OverlayConfigActions.toggleMode(), gridX(x, grid, item), gridY(y, grid, item), grid.columnWidth());
        item++;
        addSettingButton(Component.literal("Position: " + positionLabel()), () -> OverlayConfigActions.cyclePosition(), gridX(x, grid, item), gridY(y, grid, item), grid.columnWidth());
        item++;
        addSettingButton(toggleLabel("screen.gradlemc.setting.fps", GradleMCConfig.OVERLAY_SHOW_FPS.get()),
                () -> OverlayConfigActions.setBoolean(GradleMCConfig.OVERLAY_SHOW_FPS, !GradleMCConfig.OVERLAY_SHOW_FPS.get()), gridX(x, grid, item), gridY(y, grid, item), grid.columnWidth());
        item++;
        addSettingButton(toggleLabel("screen.gradlemc.setting.one_percent_low", GradleMCConfig.OVERLAY_SHOW_ONE_PERCENT_LOW.get()),
                () -> OverlayConfigActions.setBoolean(GradleMCConfig.OVERLAY_SHOW_ONE_PERCENT_LOW, !GradleMCConfig.OVERLAY_SHOW_ONE_PERCENT_LOW.get()), gridX(x, grid, item), gridY(y, grid, item), grid.columnWidth());
        item++;
        addSettingButton(toggleLabel("screen.gradlemc.setting.point_one_low", GradleMCConfig.OVERLAY_SHOW_POINT_ONE_PERCENT_LOW.get()),
                () -> OverlayConfigActions.setBoolean(GradleMCConfig.OVERLAY_SHOW_POINT_ONE_PERCENT_LOW, !GradleMCConfig.OVERLAY_SHOW_POINT_ONE_PERCENT_LOW.get()), gridX(x, grid, item), gridY(y, grid, item), grid.columnWidth());
        item++;
        addSettingButton(toggleLabel("screen.gradlemc.setting.jvm_memory", GradleMCConfig.OVERLAY_SHOW_JVM_MEMORY.get()),
                () -> OverlayConfigActions.setBoolean(GradleMCConfig.OVERLAY_SHOW_JVM_MEMORY, !GradleMCConfig.OVERLAY_SHOW_JVM_MEMORY.get()), gridX(x, grid, item), gridY(y, grid, item), grid.columnWidth());
        item++;
        addSettingButton(toggleLabel("screen.gradlemc.setting.system_memory", GradleMCConfig.OVERLAY_SHOW_SYSTEM_MEMORY.get()),
                () -> OverlayConfigActions.setBoolean(GradleMCConfig.OVERLAY_SHOW_SYSTEM_MEMORY, !GradleMCConfig.OVERLAY_SHOW_SYSTEM_MEMORY.get()), gridX(x, grid, item), gridY(y, grid, item), grid.columnWidth());
        item++;
        addSettingButton(toggleLabel("screen.gradlemc.setting.cpu", GradleMCConfig.OVERLAY_SHOW_CPU.get()),
                () -> OverlayConfigActions.setBoolean(GradleMCConfig.OVERLAY_SHOW_CPU, !GradleMCConfig.OVERLAY_SHOW_CPU.get()), gridX(x, grid, item), gridY(y, grid, item), grid.columnWidth());
        item++;
        addSettingButton(toggleLabel("screen.gradlemc.setting.gpu_name", GradleMCConfig.OVERLAY_SHOW_GPU_NAME.get()),
                () -> OverlayConfigActions.setBoolean(GradleMCConfig.OVERLAY_SHOW_GPU_NAME, !GradleMCConfig.OVERLAY_SHOW_GPU_NAME.get()), gridX(x, grid, item), gridY(y, grid, item), grid.columnWidth());
        item++;
        Button gpuUsage = Button.builder(Component.literal("GPU usage: unavailable"), button -> {
                })
                .bounds(gridX(x, grid, item), gridY(y, grid, item), grid.columnWidth(), BUTTON_HEIGHT)
                .tooltip(Tooltip.create(Component.translatable("screen.gradlemc.settings.gpu_note")))
                .build();
        gpuUsage.active = false;
        addRenderableWidget(gpuUsage);
        item++;
        addSettingButton(toggleLabel("screen.gradlemc.setting.background", GradleMCConfig.OVERLAY_BACKGROUND_ENABLED.get()),
                () -> OverlayConfigActions.setBoolean(GradleMCConfig.OVERLAY_BACKGROUND_ENABLED, !GradleMCConfig.OVERLAY_BACKGROUND_ENABLED.get()), gridX(x, grid, item), gridY(y, grid, item), grid.columnWidth());
        item++;
        addSettingButton(Component.literal("Scale: " + String.format(Locale.ROOT, "%.2f", GradleMCConfig.OVERLAY_SCALE.get())),
                () -> OverlayConfigActions.setScale(nextScale()), gridX(x, grid, item), gridY(y, grid, item), grid.columnWidth());
        item++;
        addSettingButton(Component.literal("Window: " + GradleMCConfig.OVERLAY_SAMPLING_WINDOW_SECONDS.get() + "s"),
                () -> OverlayConfigActions.setSamplingWindow(nextOf(GradleMCConfig.OVERLAY_SAMPLING_WINDOW_SECONDS.get(), new int[]{30, 60, 120})), gridX(x, grid, item), gridY(y, grid, item), grid.columnWidth());
        item++;
        addSettingButton(Component.literal("Update: " + GradleMCConfig.OVERLAY_UPDATE_INTERVAL_MS.get() + "ms"),
                () -> OverlayConfigActions.setUpdateInterval(nextOf(GradleMCConfig.OVERLAY_UPDATE_INTERVAL_MS.get(), new int[]{250, 500, 1000})), gridX(x, grid, item), gridY(y, grid, item), grid.columnWidth());
        item++;
        markWidgetContentBottom(gridLastRowY(y, grid, item));
    }

    private void markWidgetContentBottom(int rowY) {
        widgetContentBottom = Math.max(widgetContentBottom, rowY + BUTTON_HEIGHT);
    }

    private void addSettingButton(Component label, Runnable action, int x, int y, int width) {
        addRenderableWidget(Button.builder(label, button -> {
                    action.run();
                    rebuildGuiWidgets();
                })
                .bounds(x, y, width, BUTTON_HEIGHT)
                .tooltip(Tooltip.create(Component.translatable("screen.gradlemc.tooltip.overlay_setting")))
                .build());
    }

    private void addCommandButton(String labelKey, String command, int x, int y, int width, String tooltipKey) {
        addRenderableWidget(commandButton(Component.translatable(labelKey), command, x, y, width, tooltipKey));
    }

    private void addQuickCommandButton(String labelKey, String compactLabel, boolean compact, String command, int x, int y, int width, String tooltipKey) {
        addRenderableWidget(commandButton(quickLabel(labelKey, compactLabel, compact), command, x, y, width, tooltipKey));
    }

    private Component quickLabel(String labelKey, String compactLabel, boolean compact) {
        return compact ? Component.literal(compactLabel) : Component.translatable(labelKey);
    }

    private Button commandButton(Component label, String command, int x, int y, int width, String tooltipKey) {
        return Button.builder(label, button -> runServerCommand(command))
                .bounds(x, y, width, BUTTON_HEIGHT)
                .tooltip(Tooltip.create(Component.translatable(tooltipKey)))
                .build();
    }

    private void addDurationButton(String label, int x, int y, int width, boolean fps) {
        int duration = fps ? selectedFpsDuration : "Perf".equals(label) ? selectedPerfDuration : selectedWorldgenDuration;
        addRenderableWidget(Button.builder(Component.literal(label + ": " + duration + "s"), button -> {
                    if (fps) {
                        selectedFpsDuration = nextOf(selectedFpsDuration, DURATIONS);
                    } else if ("Perf".equals(label)) {
                        selectedPerfDuration = nextOf(selectedPerfDuration, DURATIONS);
                    } else {
                        selectedWorldgenDuration = nextOf(selectedWorldgenDuration, WORLDGEN_DURATIONS);
                    }
                    rebuildGuiWidgets();
                })
                .bounds(x, y, width, BUTTON_HEIGHT)
                .tooltip(Tooltip.create(Component.translatable("screen.gradlemc.tooltip.duration")))
                .build());
    }

    private GridSpec grid(int width, int preferredColumns, int minColumnWidth) {
        int columns = Math.max(1, Math.min(preferredColumns, (width + GAP) / (minColumnWidth + GAP)));
        int columnWidth = Math.max(64, (width - GAP * (columns - 1)) / columns);
        return new GridSpec(columns, columnWidth);
    }

    private int gridX(int x, GridSpec grid, int itemIndex) {
        return x + (itemIndex % grid.columns()) * (grid.columnWidth() + GAP);
    }

    private int gridY(int y, GridSpec grid, int itemIndex) {
        return y + (itemIndex / grid.columns()) * (BUTTON_HEIGHT + GAP);
    }

    private int gridLastRowY(int y, GridSpec grid, int itemCount) {
        if (itemCount <= 0) {
            return y;
        }
        return gridY(y, grid, itemCount - 1);
    }

    @Override
    public void tick() {
        if (ticksUntilStatusRefresh > 0) {
            ticksUntilStatusRefresh--;
            return;
        }
        requestStatusRefresh(false);
        ticksUntilStatusRefresh = refreshIntervalTicks();
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        Layout layout = layout();
        GradleMCGuiState state = GradleMCGuiState.capture(GradleMCGuiBridge.latestSmartAIStatus());
        renderShell(graphics, layout);
        renderHeader(graphics, layout, state);
        renderSidebar(graphics, layout);
        renderMainPanel(graphics, layout, state);
        renderFooter(graphics, layout);
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
    }

    private void renderShell(GuiGraphicsExtractor graphics, Layout layout) {
        graphics.fill(layout.left(), layout.top(), layout.right(), layout.bottom(), BACKGROUND);
        graphics.fill(layout.left(), layout.headerBottom(), layout.right(), layout.headerBottom() + 1, BORDER);
        graphics.fill(layout.left() + SIDEBAR_WIDTH + MARGIN, layout.mainTop(), layout.left() + SIDEBAR_WIDTH + MARGIN + 1, layout.footerTop() - GAP, BORDER);
        graphics.fill(layout.left(), layout.footerTop(), layout.right(), layout.footerTop() + 1, BORDER);
    }

    private void renderHeader(GuiGraphicsExtractor graphics, Layout layout, GradleMCGuiState state) {
        int x = layout.left() + MARGIN;
        int y = layout.top() + 10;
        graphics.text(font, title, x, y, TEXT);
        graphics.text(font, Component.translatable("screen.gradlemc.subtitle"), x, y + 13, MUTED);
        graphics.text(font, Component.literal(GradleMC.PRODUCT_NAME + " " + fallback(state.modVersion())
                + " for " + GradleMC.CURRENT_DISPLAY_VARIANT), x, y + 27, DIM);
        if (layout.width() >= 500) {
            renderBadge(graphics, layout.right() - MARGIN - 130, y + 4,
                    Component.literal("Stability: ").append(technicalScore(state.guiStatus())),
                    state.guiStatus().technicalStabilityScore() >= 80 ? GOOD : state.guiStatus().technicalStabilityScore() >= 55 ? WARN : BAD);
            renderBadge(graphics, layout.right() - MARGIN - 130, y + 28,
                    Component.literal("Risk: ").append(Component.translatable("screen.gradlemc.threat.with_value", threatLabel(state.smartAIStatus()), state.smartAIStatus().threatScore())),
                    threatColor(state.smartAIStatus()));
        }
    }

    private void renderSidebar(GuiGraphicsExtractor graphics, Layout layout) {
        graphics.text(font, Component.translatable("screen.gradlemc.nav.title"), layout.left() + MARGIN, layout.mainTop() - 12, DIM);
    }

    private void renderMainPanel(GuiGraphicsExtractor graphics, Layout layout, GradleMCGuiState state) {
        int x = layout.contentLeft();
        int y = layout.mainTop();
        int width = layout.contentWidth();
        int height = layout.mainHeight();
        currentContentWidth = width;
        graphics.fill(x - GAP, y - GAP, x + width + GAP, y + height + GAP, PANEL);
        graphics.fill(x - GAP, y - GAP, x + width + GAP, y - GAP + 1, BORDER);
        graphics.fill(x - GAP, y + height + GAP - 1, x + width + GAP, y + height + GAP, BORDER);

        contentLines.clear();
        switch (selectedSection) {
            case OVERVIEW -> renderOverviewSection(state, width);
            case QUICK_ACTIONS -> renderQuickActionsSection(state, width);
            case TESTS -> renderTestsSection(state, width);
            case PROFILER -> renderProfilerSection(state, width);
            case REPORTS -> renderReportsSection(state, width);
            case SETTINGS -> renderSettingsSection(state, width);
            case ABOUT -> renderAboutSection(state, width);
        }
        contentHeight = contentLines.stream().mapToInt(ContentLine::height).sum();
        int contentTopOffset = contentTopOffset(layout);
        int textTop = y + contentTopOffset;
        int viewportHeight = Math.max(0, height - contentTopOffset);
        int maxScroll = maxScroll(viewportHeight);
        scrollOffset = Math.max(0, Math.min(scrollOffset, maxScroll));

        if (viewportHeight > 0) {
            graphics.enableScissor(x, textTop, x + width, y + height);
            int drawY = textTop - scrollOffset;
            for (ContentLine line : contentLines) {
                if (drawY + line.height() >= textTop && drawY <= y + height) {
                    line.draw(graphics, x, drawY);
                }
                drawY += line.height();
            }
            graphics.disableScissor();
        }

        if (viewportHeight > 0 && maxScroll > 0) {
            int barX = x + width + 3;
            int thumbHeight = Math.max(18, viewportHeight * viewportHeight / contentHeight);
            int thumbY = textTop + (viewportHeight - thumbHeight) * scrollOffset / maxScroll;
            graphics.fill(barX, textTop, barX + 2, textTop + viewportHeight, 0x60304050);
            graphics.fill(barX, thumbY, barX + 2, thumbY + thumbHeight, 0xFF8EA3BA);
        }
    }

    private void renderFooter(GuiGraphicsExtractor graphics, Layout layout) {
        int width = Math.max(20, layout.right() - layout.left() - MARGIN * 2 - 196);
        graphics.text(font, font.plainSubstrByWidth(statusLine.getString(), width), layout.left() + MARGIN, layout.footerTop() + 13, DIM);
    }

    private void renderOverviewSection(GradleMCGuiState state, int width) {
        addHeading(Component.translatable("screen.gradlemc.nav.overview"));
        addKeyValue("screen.gradlemc.label.technical_stability", technicalScore(state.guiStatus()));
        addKeyValue("screen.gradlemc.label.adaptive_risk", Component.translatable("screen.gradlemc.threat.with_value",
                threatLabel(state.smartAIStatus()), state.smartAIStatus().threatScore()));
        addKeyValue("screen.gradlemc.label.memory", Component.literal(state.memory().usedMiB() + "/" + state.memory().maxMiB() + " MiB JVM"));
        addKeyValue("screen.gradlemc.label.current_fps", Component.literal(state.currentFps() + " FPS"));
        addKeyValue("screen.gradlemc.label.rolling_fps", Component.literal("avg " + whole(state.rollingAverageFps())
                + ", 1% " + lowLabel(state.rollingOnePercentLowFps())
                + ", 0.1% " + lowLabel(state.rollingPointOnePercentLowFps())));
        addGap();
        addHeading(Component.translatable("screen.gradlemc.active_tests"));
        addKeyValue("screen.gradlemc.label.fps_test", progressComponent(state.fpsProgress()));
        addKeyValue("screen.gradlemc.label.performance_test", progressComponent(state.performanceProgress()));
        addKeyValue("screen.gradlemc.label.worldgen_observation", progressComponent(state.worldgenProgress()));
        addGap();
        addHeading(Component.translatable("screen.gradlemc.reports.latest"));
        addKeyValue("screen.gradlemc.label.latest_report", pathOrUnavailable(state.guiStatus().latestReportPath()));
        addLabelWrapped("screen.gradlemc.label.summary", fallback(state.guiStatus().latestReportSummary()), width);
    }

    private void renderQuickActionsSection(GradleMCGuiState state, int width) {
        addHeading(Component.translatable("screen.gradlemc.nav.quick_actions"));
        addWrapped(Component.translatable("screen.gradlemc.quick_actions.description"), width, MUTED);
        addKeyValue("screen.gradlemc.label.last_action", statusLine);
        addKeyValue("screen.gradlemc.label.status_age", statusAgeComponent(state.smartAIStatusAgeMillis()));
    }

    private void renderTestsSection(GradleMCGuiState state, int width) {
        addHeading(Component.translatable("screen.gradlemc.nav.tests"));
        addKeyValue("screen.gradlemc.label.fps_test", progressComponent(state.fpsProgress()));
        addKeyValue("screen.gradlemc.label.performance_test", progressComponent(state.performanceProgress()));
        addKeyValue("screen.gradlemc.label.worldgen_observation", progressComponent(state.worldgenProgress()));
        addWrapped(Component.translatable("screen.gradlemc.tests.note"), width, DIM);
    }

    private void renderProfilerSection(GradleMCGuiState state, int width) {
        addHeading(Component.translatable("screen.gradlemc.nav.profiler"));
        addWrapped(Component.translatable("screen.gradlemc.profiler.description"), width, MUTED);
        addGap();
        addKeyValue("screen.gradlemc.profiler.mode", Component.literal(profilerMode()));
        addKeyValue("screen.gradlemc.profiler.duration", Component.literal(selectedProfilerDuration + "s"));
        addKeyValue("screen.gradlemc.profiler.interval", Component.literal(selectedProfilerInterval + "ms"));
        addKeyValue("screen.gradlemc.profiler.thread", Component.literal(profilerThreadPattern));
        addKeyValue("screen.gradlemc.profiler.threshold", Component.literal(selectedProfilerThreshold + "ms"));
        addKeyValue("screen.gradlemc.profiler.slow_only", Component.literal(onOff(profilerOnlySlowTicks)));
        addKeyValue("screen.gradlemc.profiler.latest_profile", pathOrUnavailable(state.guiStatus().latestProfilePath()));
        addLabelWrapped("screen.gradlemc.label.summary", fallback(state.guiStatus().latestProfileSummary()), width);
        addGap();
        addWrapped(Component.translatable("screen.gradlemc.profiler.warning"), width, DIM);
    }

    private void renderReportsSection(GradleMCGuiState state, int width) {
        GuiStatusSnapshot status = state.guiStatus();
        addHeading(Component.translatable("screen.gradlemc.nav.reports"));
        addKeyValue("screen.gradlemc.label.latest_fps_report", pathOrUnavailable(state.latestFpsReportPath()));
        addKeyValue("screen.gradlemc.label.latest_perf_report", pathOrUnavailable(status.latestPerformanceReportPath()));
        addKeyValue("screen.gradlemc.label.latest_worldgen_report", pathOrUnavailable(status.latestWorldgenReportPath()));
        addKeyValue("screen.gradlemc.label.latest_export", pathOrUnavailable(status.latestExportPath()));
        addKeyValue("screen.gradlemc.label.latest_issue_bundle", pathOrUnavailable(status.latestIssueBundlePath()));
        addLabelWrapped("screen.gradlemc.label.summary", fallback(status.latestReportSummary()), width);
    }

    private void renderSettingsSection(GradleMCGuiState state, int width) {
        addHeading(Component.translatable("screen.gradlemc.nav.settings"));
        addWrapped(Component.translatable("screen.gradlemc.settings.overlay_note"), width, MUTED);
        addGap();
        addKeyValue("screen.gradlemc.setting.overlay_enabled", enabledComponent(GradleMCConfig.OVERLAY_ENABLED.get()));
        addKeyValue("screen.gradlemc.setting.mode", Component.literal(modeLabel()));
        addKeyValue("screen.gradlemc.setting.position", Component.literal(positionLabel()));
        addKeyValue("screen.gradlemc.setting.scale", Component.literal(String.format(Locale.ROOT, "%.2f", GradleMCConfig.OVERLAY_SCALE.get())));
        addKeyValue("screen.gradlemc.setting.window", Component.literal(GradleMCConfig.OVERLAY_SAMPLING_WINDOW_SECONDS.get() + "s"));
        addKeyValue("screen.gradlemc.setting.update_rate", Component.literal(GradleMCConfig.OVERLAY_UPDATE_INTERVAL_MS.get() + "ms"));
        addWrapped(Component.translatable("screen.gradlemc.settings.gpu_note"), width, DIM);
    }

    private void renderAboutSection(GradleMCGuiState state, int width) {
        addHeading(Component.translatable("screen.gradlemc.nav.about"));
        addKeyValue("screen.gradlemc.label.mod", Component.translatable("screen.gradlemc.title"));
        addKeyValue("screen.gradlemc.label.version", fallback(state.modVersion()));
        addKeyValue("screen.gradlemc.label.minecraft", Component.literal(state.minecraftVersion()));
        addKeyValue("screen.gradlemc.label.neoforge", Component.literal(state.loaderVersion()));
        addGap();
        addWrapped(Component.translatable("screen.gradlemc.about.description"), width, MUTED);
        addGap();
        addWrapped(Component.translatable("screen.gradlemc.about.ai_note"), width, DIM);
    }

    private void runServerCommand(String command) {
        if (minecraft == null || minecraft.player == null || minecraft.player.connection == null) {
            setStatus(Component.translatable("screen.gradlemc.status.no_connection"));
            return;
        }
        minecraft.player.connection.sendCommand(command);
        setStatus(Component.literal("/" + command));
        requestStatusRefresh(true);
    }

    private void startFps(int seconds) {
        if (FpsTestManager.startFromClient(seconds)) {
            setStatus(Component.literal("/gradlemc testfps start " + seconds));
            rebuildGuiWidgets();
        }
    }

    private void openOutputFolder() {
        try {
            Util.getPlatform().openFile(GradleMcPaths.gradleMcDirectory().toFile());
            setStatus(Component.translatable("screen.gradlemc.status.opened_folder"));
        } catch (RuntimeException exception) {
            setStatus(Component.translatable("screen.gradlemc.status.open_folder_failed"));
        }
    }

    private void openProfilesFolder() {
        try {
            Util.getPlatform().openFile(GradleMcPaths.profileDirectory().toFile());
            setStatus(Component.translatable("screen.gradlemc.status.opened_folder"));
        } catch (RuntimeException exception) {
            setStatus(Component.translatable("screen.gradlemc.status.open_folder_failed"));
        }
    }

    private void copyLatestPath() {
        String path = latestPath(GradleMCGuiState.capture(GradleMCGuiBridge.latestSmartAIStatus()));
        if (path.isBlank()) {
            setStatus(Component.translatable("screen.gradlemc.status.no_latest_path"));
            return;
        }
        if (minecraft != null) {
            minecraft.keyboardHandler.setClipboard(path);
        }
        setStatus(Component.translatable("screen.gradlemc.status.copied_path"));
    }

    private void copyLatestProfilePath() {
        String path = GradleMCGuiState.capture(GradleMCGuiBridge.latestSmartAIStatus()).guiStatus().latestProfilePath();
        if (path == null || path.isBlank()) {
            setStatus(Component.translatable("screen.gradlemc.status.no_latest_path"));
            return;
        }
        if (minecraft != null) {
            minecraft.keyboardHandler.setClipboard(path);
        }
        setStatus(Component.translatable("screen.gradlemc.status.copied_path"));
    }

    private String latestPath(GradleMCGuiState state) {
        if (!state.guiStatus().latestReportPath().isBlank()) {
            return state.guiStatus().latestReportPath();
        }
        if (!state.latestFpsReportPath().isBlank()) {
            return state.latestFpsReportPath();
        }
        if (!state.guiStatus().latestExportPath().isBlank()) {
            return state.guiStatus().latestExportPath();
        }
        if (!state.guiStatus().latestPerformanceReportPath().isBlank()) {
            return state.guiStatus().latestPerformanceReportPath();
        }
        if (!state.guiStatus().latestWorldgenReportPath().isBlank()) {
            return state.guiStatus().latestWorldgenReportPath();
        }
        if (!state.guiStatus().latestIssueBundlePath().isBlank()) {
            return state.guiStatus().latestIssueBundlePath();
        }
        if (!state.guiStatus().latestProfilePath().isBlank()) {
            return state.guiStatus().latestProfilePath();
        }
        return "";
    }

    private void setStatus(Component status) {
        statusLine = status;
    }

    private int contentTopOffset(Layout layout) {
        if (widgetContentBottom <= layout.mainTop()) {
            return 0;
        }
        return widgetContentBottom - layout.mainTop() + CONTENT_AFTER_WIDGET_GAP;
    }

    private int refreshIntervalTicks() {
        return Math.max(20, GradleMCConfig.GUI_STATUS_REFRESH_TICKS.get());
    }

    private void requestStatusRefresh(boolean explicit) {
        long now = System.currentTimeMillis();
        if (now - lastRefreshMillis < REFRESH_COOLDOWN_MS) {
            return;
        }
        lastRefreshMillis = now;
        if (minecraft != null && minecraft.player != null && minecraft.getConnection() != null) {
                GradleMCNetwork.requestSmartAIStatus();
            if (explicit) {
                setStatus(Component.translatable("screen.gradlemc.status.refreshed"));
            }
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        Layout layout = layout();
        int contentTopOffset = contentTopOffset(layout);
        int textTop = layout.mainTop() + contentTopOffset;
        int viewportHeight = Math.max(0, layout.mainHeight() - contentTopOffset);
        if (mouseX >= layout.contentLeft() && mouseX <= layout.contentLeft() + layout.contentWidth()
                && mouseY >= textTop && mouseY <= layout.mainTop() + layout.mainHeight()) {
            scrollOffset = Math.max(0, Math.min(scrollOffset - (int) (scrollY * SCROLL_STEP), maxScroll(viewportHeight)));
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return true;
    }

    private int maxScroll(int viewportHeight) {
        return Math.max(0, contentHeight - viewportHeight);
    }

    private Layout layout() {
        int width = Math.max(300, Math.min(MAX_CONTENT_WIDTH, this.width - 24));
        if (this.width >= MIN_CONTENT_WIDTH + 24) {
            width = Math.max(MIN_CONTENT_WIDTH, width);
        }
        int height = Math.max(240, this.height - 32);
        int left = (this.width - width) / 2;
        int top = (this.height - height) / 2;
        int right = left + width;
        int bottom = top + height;
        int headerBottom = top + HEADER_HEIGHT;
        int footerTop = bottom - FOOTER_HEIGHT;
        int mainTop = headerBottom + MARGIN;
        int mainHeight = Math.max(60, footerTop - mainTop - MARGIN);
        int contentLeft = left + SIDEBAR_WIDTH + MARGIN * 2;
        int contentWidth = Math.max(140, right - contentLeft - MARGIN);
        return new Layout(left, top, right, bottom, headerBottom, footerTop, mainTop, mainHeight, contentLeft, contentWidth);
    }

    private void addHeading(Component label) {
        contentLines.add(new TextLine(label, TEXT, 14, true));
    }

    private void addKeyValue(String labelKey, Component value) {
        contentLines.add(new KeyValueLine(Component.translatable(labelKey), value, currentContentWidth));
    }

    private void addLabelWrapped(String labelKey, Component value, int width) {
        contentLines.add(new TextLine(Component.translatable(labelKey), DIM, 12, false));
        addWrapped(value, Math.max(80, width - 14), MUTED);
    }

    private void addWrapped(Component text, int width, int color) {
        List<FormattedCharSequence> lines = font.split(text, width);
        for (FormattedCharSequence line : lines) {
            contentLines.add(new WrappedLine(line, color));
        }
    }

    private void addGap() {
        contentLines.add(new GapLine(7));
    }

    private void renderBadge(GuiGraphicsExtractor graphics, int x, int y, Component label, int color) {
        graphics.fill(x, y, x + 130, y + 17, PANEL_DARK);
        graphics.fill(x, y, x + 3, y + 17, color);
        graphics.text(font, label, x + 8, y + 5, TEXT);
    }

    private Component enabledComponent(boolean enabled) {
        return Component.translatable(enabled ? "screen.gradlemc.status.enabled" : "screen.gradlemc.status.disabled");
    }

    private Component technicalScore(GuiStatusSnapshot status) {
        if (status == null || status.technicalStabilityScore() < 0) {
            return Component.translatable("screen.gradlemc.placeholder.unavailable");
        }
        return Component.literal(status.technicalStabilityScore() + "/100 " + status.technicalRiskLevel());
    }

    private Component progressComponent(DiagnosticTestProgress progress) {
        if (progress == null || !progress.running()) {
            return Component.translatable("screen.gradlemc.status.idle");
        }
        return Component.translatable("screen.gradlemc.value.progress",
                progress.clampedElapsedSeconds(), progress.requestedSeconds(), progress.percent());
    }

    private Component threatLabel(SmartAIStatus status) {
        return Component.translatable("screen.gradlemc.threat." + status.threatLevel().name().toLowerCase(Locale.ROOT));
    }

    private int threatColor(SmartAIStatus status) {
        return switch (status.threatLevel()) {
            case LOW -> GOOD;
            case MEDIUM -> WARN;
            case HIGH, EXTREME -> BAD;
        };
    }

    private Component fallback(String value) {
        if (value == null || value.isBlank()) {
            return Component.translatable("screen.gradlemc.placeholder.unavailable");
        }
        return Component.literal(value);
    }

    private Component pathOrUnavailable(String value) {
        if (value == null || value.isBlank()) {
            return Component.translatable("screen.gradlemc.placeholder.unavailable");
        }
        return Component.literal(value);
    }

    private Component statusAgeComponent(long ageMillis) {
        if (ageMillis < 0L) {
            return Component.translatable("screen.gradlemc.placeholder.unavailable");
        }
        int seconds = (int) Math.max(0L, ageMillis / 1000L);
        return seconds <= 1
                ? Component.translatable("screen.gradlemc.value.fresh")
                : Component.translatable("screen.gradlemc.value.cached_seconds", seconds);
    }

    private Component toggleLabel(String key, boolean enabled) {
        return Component.translatable(key).append(Component.literal(": ")).append(enabledComponent(enabled));
    }

    private String modeLabel() {
        return GradleMCConfig.OVERLAY_MODE.get().toLowerCase(Locale.ROOT);
    }

    private String positionLabel() {
        return GradleMCConfig.OVERLAY_POSITION.get().toLowerCase(Locale.ROOT).replace('_', '-');
    }

    private String lowLabel(Double value) {
        return value == null ? "warming up" : whole(value);
    }

    private String whole(double value) {
        return String.format(Locale.ROOT, "%.0f", Double.isFinite(value) ? Math.max(0.0D, value) : 0.0D);
    }

    private int nextRadius(int value) {
        return value >= 256 ? 32 : value * 2;
    }

    private int nextOf(int current, int[] values) {
        for (int index = 0; index < values.length; index++) {
            if (values[index] == current) {
                return values[(index + 1) % values.length];
            }
        }
        return values[0];
    }

    private int durationIndex(int duration, int[] values) {
        for (int index = 0; index < values.length; index++) {
            if (values[index] == duration) {
                return index;
            }
        }
        return 0;
    }

    private String profilerStartCommand() {
        int slowThreshold = profilerOnlySlowTicks ? selectedProfilerThreshold : 0;
        return "gradlemc profiler start --mode " + profilerMode()
                + " --timeout " + selectedProfilerDuration
                + " --interval " + selectedProfilerInterval
                + " --thread " + profilerThreadPattern
                + " --only-ticks-over " + slowThreshold
                + " --include-sleeping " + profilerIncludeSleeping;
    }

    private String profilerMode() {
        return PROFILER_MODES[selectedProfilerModeIndex];
    }

    private String profilerThreadLabel() {
        return PROFILER_THREADS[selectedProfilerThreadIndex];
    }

    private String onOff(boolean value) {
        return value ? "on" : "off";
    }

    private double nextScale() {
        double current = GradleMCConfig.OVERLAY_SCALE.get();
        if (current < 1.0D) {
            return 1.0D;
        }
        if (current < 1.25D) {
            return 1.25D;
        }
        if (current < 1.5D) {
            return 1.5D;
        }
        return 0.75D;
    }

    private record Layout(int left, int top, int right, int bottom, int headerBottom, int footerTop,
                          int mainTop, int mainHeight, int contentLeft, int contentWidth) {
        private int width() {
            return right - left;
        }
    }

    private record GridSpec(int columns, int columnWidth) {
    }

    private interface ContentLine {
        int height();

        void draw(GuiGraphicsExtractor graphics, int x, int y);
    }

    private class TextLine implements ContentLine {
        private final Component text;
        private final int color;
        private final int height;
        private final boolean underline;

        private TextLine(Component text, int color, int height, boolean underline) {
            this.text = text;
            this.color = color;
            this.height = height;
            this.underline = underline;
        }

        @Override
        public int height() {
            return height;
        }

        @Override
        public void draw(GuiGraphicsExtractor graphics, int x, int y) {
            graphics.text(font, text, x, y, color);
            if (underline) {
                graphics.fill(x, y + 10, x + Math.min(180, font.width(text)), y + 11, BORDER);
            }
        }
    }

    private class WrappedLine implements ContentLine {
        private final FormattedCharSequence text;
        private final int color;

        private WrappedLine(FormattedCharSequence text, int color) {
            this.text = text;
            this.color = color;
        }

        @Override
        public int height() {
            return LINE_HEIGHT;
        }

        @Override
        public void draw(GuiGraphicsExtractor graphics, int x, int y) {
            graphics.text(font, text, x, y, color);
        }
    }

    private class KeyValueLine implements ContentLine {
        private final Component label;
        private final Component value;
        private final int width;
        private final boolean stacked;

        private KeyValueLine(Component label, Component value, int width) {
            this.label = label;
            this.value = value;
            this.width = Math.max(80, width);
            this.stacked = this.width < 260;
        }

        @Override
        public int height() {
            return stacked ? 24 : 14;
        }

        @Override
        public void draw(GuiGraphicsExtractor graphics, int x, int y) {
            graphics.text(font, label, x, y, DIM);
            if (stacked) {
                graphics.text(font, font.plainSubstrByWidth(value.getString(), Math.max(20, width - 8)), x + 8, y + 11, TEXT);
                return;
            }
            int labelWidth = Math.min(160, Math.max(110, width / 2));
            graphics.text(font, font.plainSubstrByWidth(value.getString(), Math.max(20, width - labelWidth)), x + labelWidth, y, TEXT);
        }
    }

    private record GapLine(int height) implements ContentLine {
        @Override
        public void draw(GuiGraphicsExtractor graphics, int x, int y) {
        }
    }
}
