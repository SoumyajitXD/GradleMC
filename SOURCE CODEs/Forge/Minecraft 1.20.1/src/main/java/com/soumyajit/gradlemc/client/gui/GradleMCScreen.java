package com.soumyajit.gradlemc.client.gui;

import com.soumyajit.gradlemc.ai.SmartAIStatus;
import com.soumyajit.gradlemc.client.gui.model.GradleMCGuiState;
import com.soumyajit.gradlemc.config.GradleMCConfig;
import com.soumyajit.gradlemc.network.GradleMCGuiBridge;
import com.soumyajit.gradlemc.network.GradleMCNetwork;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class GradleMCScreen extends Screen {
    private static final int MIN_CONTENT_WIDTH = 340;
    private static final int MAX_CONTENT_WIDTH = 760;
    private static final int HEADER_HEIGHT = 58;
    private static final int FOOTER_HEIGHT = 34;
    private static final int SIDEBAR_WIDTH = 116;
    private static final int TAB_HEIGHT = 22;
    private static final int BUTTON_WIDTH = 92;
    private static final int BUTTON_HEIGHT = 20;
    private static final int MARGIN = 14;
    private static final int GAP = 8;
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

    private final List<ContentLine> contentLines = new ArrayList<>();
    private GradleMCGuiSection selectedSection = GradleMCGuiSection.OVERVIEW;
    private int scrollOffset;
    private int contentHeight;
    private int currentContentWidth;
    private int ticksUntilStatusRefresh;
    private long lastRefreshMillis;

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
        int buttonY = layout.footerTop() + 7;
        addRenderableWidget(Button.builder(Component.translatable("screen.gradlemc.button.refresh"), button -> requestStatusRefresh(true))
                .bounds(layout.right() - MARGIN - BUTTON_WIDTH * 2 - GAP, buttonY, BUTTON_WIDTH, BUTTON_HEIGHT)
                .build());
        addRenderableWidget(Button.builder(Component.translatable("screen.gradlemc.button.close"), button -> onClose())
                .bounds(layout.right() - MARGIN - BUTTON_WIDTH, buttonY, BUTTON_WIDTH, BUTTON_HEIGHT)
                .build());
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
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        Layout layout = layout();
        GradleMCGuiState state = GradleMCGuiState.capture(GradleMCGuiBridge.latestSmartAIStatus());
        renderShell(graphics, layout);
        renderHeader(graphics, layout, state);
        renderSidebar(graphics, layout);
        renderMainPanel(graphics, layout, state);
        renderFooter(graphics, layout);
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    private void renderShell(GuiGraphics graphics, Layout layout) {
        graphics.fill(layout.left(), layout.top(), layout.right(), layout.bottom(), BACKGROUND);
        graphics.fill(layout.left(), layout.headerBottom(), layout.right(), layout.headerBottom() + 1, BORDER);
        graphics.fill(layout.left() + SIDEBAR_WIDTH + MARGIN, layout.mainTop(), layout.left() + SIDEBAR_WIDTH + MARGIN + 1, layout.footerTop() - GAP, BORDER);
        graphics.fill(layout.left(), layout.footerTop(), layout.right(), layout.footerTop() + 1, BORDER);
    }

    private void renderHeader(GuiGraphics graphics, Layout layout, GradleMCGuiState state) {
        int x = layout.left() + MARGIN;
        int y = layout.top() + 10;
        graphics.drawString(font, title, x, y, TEXT);
        graphics.drawString(font, Component.translatable("screen.gradlemc.subtitle"), x, y + 13, MUTED);
        graphics.drawString(font, Component.translatable("screen.gradlemc.header.version", fallback(state.modVersion())), x, y + 27, DIM);
        if (layout.width() >= 430) {
            renderBadge(graphics, layout.right() - MARGIN - 112, y + 4,
                    Component.literal("Adaptive: ").append(enabledComponent(state.smartAIStatus().adaptiveSmartAIEnabled())),
                    state.smartAIStatus().adaptiveSmartAIEnabled() ? GOOD : DIM);
            renderBadge(graphics, layout.right() - MARGIN - 112, y + 28,
                    Component.literal("Risk: ").append(Component.translatable("screen.gradlemc.threat.with_value", threatLabel(state.smartAIStatus()), state.smartAIStatus().threatScore())),
                    threatColor(state.smartAIStatus()));
        }
    }

    private void renderSidebar(GuiGraphics graphics, Layout layout) {
        graphics.drawString(font, Component.translatable("screen.gradlemc.nav.title"), layout.left() + MARGIN, layout.mainTop() - 12, DIM);
    }

    private void renderMainPanel(GuiGraphics graphics, Layout layout, GradleMCGuiState state) {
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
            case SMART_AI -> renderSmartAISection(state, width);
            case SETTINGS -> renderSettingsSection(state, width);
            case COMMANDS -> renderCommandsSection(width);
            case ABOUT -> renderAboutSection(state, width);
        }
        contentHeight = contentLines.stream().mapToInt(ContentLine::height).sum();
        int maxScroll = maxScroll(height);
        scrollOffset = Math.max(0, Math.min(scrollOffset, maxScroll));

        graphics.enableScissor(x, y, x + width, y + height);
        int drawY = y - scrollOffset;
        for (ContentLine line : contentLines) {
            if (drawY + line.height() >= y && drawY <= y + height) {
                line.draw(graphics, x, drawY);
            }
            drawY += line.height();
        }
        graphics.disableScissor();

        if (maxScroll > 0) {
            int barX = x + width + 3;
            int thumbHeight = Math.max(18, height * height / contentHeight);
            int thumbY = y + (height - thumbHeight) * scrollOffset / maxScroll;
            graphics.fill(barX, y, barX + 2, y + height, 0x60304050);
            graphics.fill(barX, thumbY, barX + 2, thumbY + thumbHeight, 0xFF8EA3BA);
        }
    }

    private void renderFooter(GuiGraphics graphics, Layout layout) {
        if (layout.width() >= 420) {
            graphics.drawString(font, Component.translatable("screen.gradlemc.hint.escape_to_close"),
                    layout.left() + MARGIN, layout.footerTop() + 13, DIM);
        }
    }

    private void renderOverviewSection(GradleMCGuiState state, int width) {
        addHeading(Component.translatable("screen.gradlemc.nav.overview"));
        addWrapped(Component.translatable("screen.gradlemc.overview.description"), width, MUTED);
        addGap();
        addKeyValue("screen.gradlemc.label.mod", Component.translatable("screen.gradlemc.title"));
        addKeyValue("screen.gradlemc.label.player", fallback(state.playerName()));
        addKeyValue("screen.gradlemc.label.smart_ai", enabledComponent(state.smartAIStatus().adaptiveSmartAIEnabled()));
        addKeyValue("screen.gradlemc.label.threat", Component.translatable("screen.gradlemc.threat.with_value",
                threatLabel(state.smartAIStatus()), state.smartAIStatus().threatScore()));
        addKeyValue("screen.gradlemc.label.status_age", statusAgeComponent(state.smartAIStatusAgeMillis()));
        addLabelWrapped("screen.gradlemc.label.recent", recentAdaptation(state.smartAIStatus()), width);
        addGap();
        addWrapped(Component.translatable("screen.gradlemc.overview.open_note"), width, DIM);
    }

    private void renderSmartAISection(GradleMCGuiState state, int width) {
        SmartAIStatus status = state.smartAIStatus();
        addHeading(Component.translatable("screen.gradlemc.nav.smart_ai"));
        addWrapped(Component.translatable("screen.gradlemc.smart_ai.explain"), width, MUTED);
        addGap();
        addKeyValue("screen.gradlemc.label.smart_ai", enabledComponent(status.adaptiveSmartAIEnabled()));
        addKeyValue("screen.gradlemc.label.ambience", enabledComponent(status.adaptiveAmbienceEnabled()));
        addKeyValue("screen.gradlemc.label.events", enabledComponent(status.adaptiveEventsEnabled()));
        addKeyValue("screen.gradlemc.label.threat", Component.translatable("screen.gradlemc.threat.with_value",
                threatLabel(status), status.threatScore()));
        addLabelWrapped("screen.gradlemc.label.recent", recentAdaptation(status), width);
        addGap();
        addHeading(Component.translatable("screen.gradlemc.smart_ai.signals"));
        addKeyValue("screen.gradlemc.label.darkness", secondsComponent(status.darknessTicks()));
        addKeyValue("screen.gradlemc.label.underground", secondsComponent(status.undergroundTicks()));
        addKeyValue("screen.gradlemc.label.sleep", secondsComponent(status.ticksSinceSleep()));
        addKeyValue("screen.gradlemc.label.movement", Component.literal(Integer.toString(status.movementPressure())));
        addKeyValue("screen.gradlemc.label.damage", Component.literal(Integer.toString(status.recentDamageTaken())));
        addKeyValue("screen.gradlemc.label.kills", Component.literal(Integer.toString(status.recentMobKills())));
        addKeyValue("screen.gradlemc.label.deaths", Component.literal(Integer.toString(status.recentDeaths())));
        addKeyValue("screen.gradlemc.label.ambience_cooldown", secondsComponent(status.ticksUntilNextAmbience()));
        addKeyValue("screen.gradlemc.label.event_cooldown", secondsComponent(status.ticksUntilNextEvent()));
    }

    private void renderSettingsSection(GradleMCGuiState state, int width) {
        addHeading(Component.translatable("screen.gradlemc.nav.settings"));
        addWrapped(Component.translatable("screen.gradlemc.settings.read_only"), width, MUTED);
        addGap();
        addKeyValue("screen.gradlemc.label.reports", enabledComponent(state.reportsEnabled()));
        addKeyValue("screen.gradlemc.label.rule_checks", enabledComponent(state.ruleChecksEnabled()));
        addKeyValue("screen.gradlemc.label.smart_diagnostics", enabledComponent(state.smartDiagnosticsEnabled()));
        addKeyValue("screen.gradlemc.label.adaptive_baseline", enabledComponent(state.adaptiveBaselineEnabled()));
        addKeyValue("screen.gradlemc.label.debug_logging", enabledComponent(state.debugSmartAILogging()));
        addKeyValue("screen.gradlemc.label.high_intensity", enabledComponent(state.allowHighIntensityEvents()));
        addKeyValue("screen.gradlemc.label.reduce_after_death", enabledComponent(state.reduceIntensityAfterDeath()));
        addKeyValue("screen.gradlemc.label.max_threat", Component.literal(Integer.toString(state.maxThreatLevel())));
        addKeyValue("screen.gradlemc.label.event_cooldown", secondsComponent(state.eventCooldownTicks()));
        addKeyValue("screen.gradlemc.label.ambience_cooldown", secondsComponent(state.ambienceCooldownTicks()));
        addKeyValue("screen.gradlemc.label.gui_refresh", secondsComponent(GradleMCConfig.GUI_STATUS_REFRESH_TICKS.get()));
        addKeyValue("screen.gradlemc.label.multiplier", Component.literal(String.format(Locale.ROOT, "%.2f", state.adaptiveDifficultyMultiplier())));
    }

    private void renderCommandsSection(int width) {
        addHeading(Component.translatable("screen.gradlemc.nav.commands"));
        addCommand("/gradlemc gui", "screen.gradlemc.command.gui", width);
        addCommand("/gradlemc status", "screen.gradlemc.command.status", width);
        addCommand("/gradlemc ai status", "screen.gradlemc.command.ai_status", width);
        addCommand("/gradlemc ai reset", "screen.gradlemc.command.ai_reset", width);
        addCommand("/gradlemc help", "screen.gradlemc.command.help", width);
    }

    private void renderAboutSection(GradleMCGuiState state, int width) {
        addHeading(Component.translatable("screen.gradlemc.nav.about"));
        addKeyValue("screen.gradlemc.label.mod", Component.translatable("screen.gradlemc.title"));
        addKeyValue("screen.gradlemc.label.version", fallback(state.modVersion()));
        addKeyValue("screen.gradlemc.label.minecraft", Component.literal(state.minecraftVersion()));
        addKeyValue("screen.gradlemc.label.forge", Component.literal(state.forgeVersion()));
        addGap();
        addWrapped(Component.translatable("screen.gradlemc.about.description"), width, MUTED);
        addGap();
        addWrapped(Component.translatable("screen.gradlemc.about.ai_note"), width, DIM);
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

    private void addCommand(String command, String descriptionKey, int width) {
        contentLines.add(new TextLine(Component.literal(command), TEXT, 12, true));
        addWrapped(Component.translatable(descriptionKey), width, MUTED);
        addGap();
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

    private void renderBadge(GuiGraphics graphics, int x, int y, Component label, int color) {
        graphics.fill(x, y, x + 112, y + 17, PANEL_DARK);
        graphics.fill(x, y, x + 3, y + 17, color);
        graphics.drawString(font, label, x + 8, y + 5, TEXT);
    }

    private Component enabledComponent(boolean enabled) {
        return Component.translatable(enabled ? "screen.gradlemc.status.enabled" : "screen.gradlemc.status.disabled");
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

    private Component recentAdaptation(SmartAIStatus status) {
        if (status.recentAdaptation() == null || status.recentAdaptation().isBlank()) {
            return Component.translatable("screen.gradlemc.placeholder.unavailable");
        }
        return Component.literal(status.recentAdaptation());
    }

    private Component fallback(String value) {
        if (value == null || value.isBlank()) {
            return Component.translatable("screen.gradlemc.placeholder.unavailable");
        }
        return Component.literal(value);
    }

    private Component secondsComponent(int ticks) {
        return Component.translatable("screen.gradlemc.value.seconds", Math.max(0, ticks / 20));
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

    private void requestStatusRefresh(boolean explicit) {
        long now = System.currentTimeMillis();
        if (now - lastRefreshMillis < REFRESH_COOLDOWN_MS) {
            return;
        }
        lastRefreshMillis = now;
        if (minecraft != null && minecraft.player != null && minecraft.getConnection() != null) {
            GradleMCNetwork.requestSmartAIStatus();
        }
    }

    private int refreshIntervalTicks() {
        return Math.max(20, GradleMCConfig.GUI_STATUS_REFRESH_TICKS.get());
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        Layout layout = layout();
        if (mouseX >= layout.contentLeft() && mouseX <= layout.contentLeft() + layout.contentWidth()
                && mouseY >= layout.mainTop() && mouseY <= layout.mainTop() + layout.mainHeight()) {
            scrollOffset = Math.max(0, Math.min(scrollOffset - (int) (delta * SCROLL_STEP), maxScroll(layout.mainHeight())));
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return true;
    }

    private int maxScroll(int viewportHeight) {
        return Math.max(0, contentHeight - viewportHeight);
    }

    private Layout layout() {
        int width = Math.max(260, Math.min(MAX_CONTENT_WIDTH, this.width - 24));
        if (this.width >= MIN_CONTENT_WIDTH + 24) {
            width = Math.max(MIN_CONTENT_WIDTH, width);
        }
        int height = Math.max(220, this.height - 32);
        int left = (this.width - width) / 2;
        int top = (this.height - height) / 2;
        int right = left + width;
        int bottom = top + height;
        int headerBottom = top + HEADER_HEIGHT;
        int footerTop = bottom - FOOTER_HEIGHT;
        int mainTop = headerBottom + MARGIN;
        int mainHeight = Math.max(40, footerTop - mainTop - MARGIN);
        int contentLeft = left + SIDEBAR_WIDTH + MARGIN * 2;
        int contentWidth = Math.max(80, right - contentLeft - MARGIN);
        return new Layout(left, top, right, bottom, headerBottom, footerTop, mainTop, mainHeight, contentLeft, contentWidth);
    }

    private record Layout(int left, int top, int right, int bottom, int headerBottom, int footerTop,
                          int mainTop, int mainHeight, int contentLeft, int contentWidth) {
        private int width() {
            return right - left;
        }
    }

    private interface ContentLine {
        int height();

        void draw(GuiGraphics graphics, int x, int y);
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
        public void draw(GuiGraphics graphics, int x, int y) {
            graphics.drawString(font, text, x, y, color);
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
        public void draw(GuiGraphics graphics, int x, int y) {
            graphics.drawString(font, text, x, y, color);
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
            this.stacked = this.width < 220;
        }

        @Override
        public int height() {
            return stacked ? 24 : 14;
        }

        @Override
        public void draw(GuiGraphics graphics, int x, int y) {
            graphics.drawString(font, label, x, y, DIM);
            if (stacked) {
                graphics.drawString(font, value, x + 8, y + 11, TEXT);
                return;
            }
            int labelWidth = Math.min(128, Math.max(88, width / 2));
            graphics.drawString(font, value, x + labelWidth, y, TEXT);
        }
    }

    private record GapLine(int height) implements ContentLine {
        @Override
        public void draw(GuiGraphics graphics, int x, int y) {
        }
    }
}
