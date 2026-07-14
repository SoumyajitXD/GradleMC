from pathlib import Path
from textwrap import dedent

ROOT = Path(__file__).resolve().parents[2]
FORGE_ROOT = "GradleMC/Forge/Minecraft 1.20.1"
OLD_ARTIFACT = "gradlemc-1.0.2-forge-1.20.1.jar"
NEW_ARTIFACT = "gradlemc-1.0.3-forge-1.20.1.jar"


def path(relative: str) -> Path:
    return ROOT / relative


def read(relative: str) -> str:
    return path(relative).read_text(encoding="utf-8")


def write(relative: str, content: str) -> None:
    path(relative).write_text(content, encoding="utf-8", newline="\n")


def replace_exact(relative: str, old: str, new: str, expected: int = 1) -> None:
    content = read(relative)
    count = content.count(old)
    if count != expected:
        raise RuntimeError(
            f"{relative}: expected {expected} occurrence(s), found {count}: {old!r}"
        )
    write(relative, content.replace(old, new))


def replace_at_least(relative: str, old: str, new: str, minimum: int = 1) -> None:
    content = read(relative)
    count = content.count(old)
    if count < minimum:
        raise RuntimeError(
            f"{relative}: expected at least {minimum} occurrence(s), found {count}: {old!r}"
        )
    write(relative, content.replace(old, new))


def replace_region(relative: str, start_marker: str, end_marker: str, replacement: str) -> None:
    content = read(relative)
    start = content.find(start_marker)
    if start < 0:
        raise RuntimeError(f"{relative}: start marker not found: {start_marker!r}")
    end = content.find(end_marker, start)
    if end < 0:
        raise RuntimeError(f"{relative}: end marker not found: {end_marker!r}")
    write(relative, content[:start] + replacement + content[end:])


# Release identity.
replace_exact(f"{FORGE_ROOT}/gradle.properties", "mod_version=1.0.2", "mod_version=1.0.3")
replace_exact(
    f"{FORGE_ROOT}/gradle.properties",
    f"artifact_name={OLD_ARTIFACT}",
    f"artifact_name={NEW_ARTIFACT}",
)

# Independent overlay defaults.
replace_exact(
    f"{FORGE_ROOT}/src/main/java/com/soumyajit/gradlemc/config/OverlayDefaults.java",
    "    public static final boolean SHOW_FPS = true;\n",
    "    public static final boolean SHOW_BRANDING = false;\n"
    "    public static final boolean SHOW_FPS = true;\n"
    "    public static final boolean SHOW_AVERAGE_FPS = false;\n",
)

config = f"{FORGE_ROOT}/src/main/java/com/soumyajit/gradlemc/config/GradleMCConfig.java"
replace_exact(
    config,
    "    public static final ForgeConfigSpec.BooleanValue OVERLAY_SHOW_FPS;\n",
    "    public static final ForgeConfigSpec.BooleanValue OVERLAY_SHOW_BRANDING;\n"
    "    public static final ForgeConfigSpec.BooleanValue OVERLAY_SHOW_FPS;\n"
    "    public static final ForgeConfigSpec.BooleanValue OVERLAY_SHOW_AVERAGE_FPS;\n",
)
replace_exact(
    config,
    "        OVERLAY_SHOW_FPS = builder\n"
    "                .comment(\"Show current and average FPS.\")\n"
    "                .define(\"overlayShowFps\", OverlayDefaults.SHOW_FPS);\n",
    "        OVERLAY_SHOW_BRANDING = builder\n"
    "                .comment(\"Show the GradleMC label in compact overlay mode. Disabled by default.\")\n"
    "                .define(\"overlayShowBranding\", OverlayDefaults.SHOW_BRANDING);\n"
    "        OVERLAY_SHOW_FPS = builder\n"
    "                .comment(\"Show current FPS using Minecraft's current FPS value.\")\n"
    "                .define(\"overlayShowFps\", OverlayDefaults.SHOW_FPS);\n"
    "        OVERLAY_SHOW_AVERAGE_FPS = builder\n"
    "                .comment(\"Show rolling average FPS independently from current FPS. Disabled by default.\")\n"
    "                .define(\"overlayShowAverageFps\", OverlayDefaults.SHOW_AVERAGE_FPS);\n",
)

# Current FPS uses Minecraft's authoritative value; rolling stats remain frame-time based.
calculator = f"{FORGE_ROOT}/src/main/java/com/soumyajit/gradlemc/client/overlay/FpsRollingStatsCalculator.java"
replace_exact(
    calculator,
    "        public boolean hasPointOnePercentLow() {\n"
    "            return pointOnePercentLowFps != null;\n"
    "        }\n",
    "        public boolean hasPointOnePercentLow() {\n"
    "            return pointOnePercentLowFps != null;\n"
    "        }\n\n"
    "        public Snapshot withCurrentFps(int currentFps) {\n"
    "            return new Snapshot(sampleCount, Math.max(0, currentFps), averageFps, onePercentLowFps, pointOnePercentLowFps);\n"
    "        }\n",
)

overlay = f"{FORGE_ROOT}/src/main/java/com/soumyajit/gradlemc/client/overlay/GradleMCStatsOverlay.java"
replace_exact(
    overlay,
    "            cachedFps = FPS_STATS.snapshot();\n",
    "            cachedFps = FPS_STATS.snapshot().withCurrentFps(minecraft.getFps());\n",
)
replace_exact(
    overlay,
    "    public static FpsRollingStatsCalculator.Snapshot latestFpsSnapshot() {\n"
    "        return cachedFps;\n"
    "    }\n",
    "    public static FpsRollingStatsCalculator.Snapshot latestFpsSnapshot() {\n"
    "        return cachedFps.withCurrentFps(Minecraft.getInstance().getFps());\n"
    "    }\n",
)
replace_exact(
    overlay,
    "        if (compact) {\n"
    "            return List.of(compactLine(memory, fps));\n"
    "        }\n",
    "        if (compact) {\n"
    "            String line = compactLine(memory, fps);\n"
    "            return line.isBlank() ? List.of() : List.of(line);\n"
    "        }\n",
)
replace_exact(
    overlay,
    "        if (GradleMCConfig.OVERLAY_SHOW_FPS.get()) {\n"
    "            lines.add(\"FPS: \" + whole(fps.currentFps()) + \" current, \" + whole(fps.averageFps()) + \" avg\");\n"
    "        }\n",
    "        if (GradleMCConfig.OVERLAY_SHOW_FPS.get()) {\n"
    "            lines.add(\"FPS: \" + whole(fps.currentFps()));\n"
    "        }\n"
    "        if (GradleMCConfig.OVERLAY_SHOW_AVERAGE_FPS.get()) {\n"
    "            lines.add(\"Average FPS: \" + whole(fps.averageFps()));\n"
    "        }\n",
)
replace_exact(
    overlay,
    "        List<String> parts = new ArrayList<>();\n"
    "        parts.add(\"GradleMC\");\n"
    "        if (GradleMCConfig.OVERLAY_SHOW_FPS.get()) {\n"
    "            parts.add(whole(fps.currentFps()) + \" FPS\");\n"
    "            parts.add(\"avg \" + whole(fps.averageFps()));\n"
    "        }\n",
    "        List<String> parts = new ArrayList<>();\n"
    "        if (GradleMCConfig.OVERLAY_SHOW_BRANDING.get()) {\n"
    "            parts.add(\"GradleMC\");\n"
    "        }\n"
    "        if (GradleMCConfig.OVERLAY_SHOW_FPS.get()) {\n"
    "            parts.add(whole(fps.currentFps()) + \" FPS\");\n"
    "        }\n"
    "        if (GradleMCConfig.OVERLAY_SHOW_AVERAGE_FPS.get()) {\n"
    "            parts.add(\"avg \" + whole(fps.averageFps()));\n"
    "        }\n",
)

# Settings GUI exposes branding, current FPS, and average FPS independently.
screen = f"{FORGE_ROOT}/src/main/java/com/soumyajit/gradlemc/client/gui/GradleMCScreen.java"
settings_method = dedent(
    '''\
    private void buildSettingWidgets(int x, int y, int width) {
        int col = Math.max(120, (width - GAP * 2) / 3);
        int rowY = y;
        addSettingButton(toggleLabel("screen.gradlemc.setting.overlay_enabled", GradleMCConfig.OVERLAY_ENABLED.get()),
                () -> OverlayConfigActions.toggleEnabled(), x, rowY, col);
        addSettingButton(Component.literal("Mode: " + modeLabel()), () -> OverlayConfigActions.toggleMode(), x + col + GAP, rowY, col);
        addSettingButton(Component.literal("Position: " + positionLabel()), () -> OverlayConfigActions.cyclePosition(), x + (col + GAP) * 2, rowY, col);
        rowY += BUTTON_HEIGHT + GAP;
        addSettingButton(toggleLabel("screen.gradlemc.setting.branding", GradleMCConfig.OVERLAY_SHOW_BRANDING.get()),
                () -> OverlayConfigActions.setBoolean(GradleMCConfig.OVERLAY_SHOW_BRANDING, !GradleMCConfig.OVERLAY_SHOW_BRANDING.get()), x, rowY, col);
        addSettingButton(toggleLabel("screen.gradlemc.setting.fps", GradleMCConfig.OVERLAY_SHOW_FPS.get()),
                () -> OverlayConfigActions.setBoolean(GradleMCConfig.OVERLAY_SHOW_FPS, !GradleMCConfig.OVERLAY_SHOW_FPS.get()), x + col + GAP, rowY, col);
        addSettingButton(toggleLabel("screen.gradlemc.setting.average_fps", GradleMCConfig.OVERLAY_SHOW_AVERAGE_FPS.get()),
                () -> OverlayConfigActions.setBoolean(GradleMCConfig.OVERLAY_SHOW_AVERAGE_FPS, !GradleMCConfig.OVERLAY_SHOW_AVERAGE_FPS.get()), x + (col + GAP) * 2, rowY, col);
        rowY += BUTTON_HEIGHT + GAP;
        addSettingButton(toggleLabel("screen.gradlemc.setting.one_percent_low", GradleMCConfig.OVERLAY_SHOW_ONE_PERCENT_LOW.get()),
                () -> OverlayConfigActions.setBoolean(GradleMCConfig.OVERLAY_SHOW_ONE_PERCENT_LOW, !GradleMCConfig.OVERLAY_SHOW_ONE_PERCENT_LOW.get()), x, rowY, col);
        addSettingButton(toggleLabel("screen.gradlemc.setting.point_one_low", GradleMCConfig.OVERLAY_SHOW_POINT_ONE_PERCENT_LOW.get()),
                () -> OverlayConfigActions.setBoolean(GradleMCConfig.OVERLAY_SHOW_POINT_ONE_PERCENT_LOW, !GradleMCConfig.OVERLAY_SHOW_POINT_ONE_PERCENT_LOW.get()), x + col + GAP, rowY, col);
        addSettingButton(toggleLabel("screen.gradlemc.setting.jvm_memory", GradleMCConfig.OVERLAY_SHOW_JVM_MEMORY.get()),
                () -> OverlayConfigActions.setBoolean(GradleMCConfig.OVERLAY_SHOW_JVM_MEMORY, !GradleMCConfig.OVERLAY_SHOW_JVM_MEMORY.get()), x + (col + GAP) * 2, rowY, col);
        rowY += BUTTON_HEIGHT + GAP;
        addSettingButton(toggleLabel("screen.gradlemc.setting.system_memory", GradleMCConfig.OVERLAY_SHOW_SYSTEM_MEMORY.get()),
                () -> OverlayConfigActions.setBoolean(GradleMCConfig.OVERLAY_SHOW_SYSTEM_MEMORY, !GradleMCConfig.OVERLAY_SHOW_SYSTEM_MEMORY.get()), x, rowY, col);
        addSettingButton(toggleLabel("screen.gradlemc.setting.cpu", GradleMCConfig.OVERLAY_SHOW_CPU.get()),
                () -> OverlayConfigActions.setBoolean(GradleMCConfig.OVERLAY_SHOW_CPU, !GradleMCConfig.OVERLAY_SHOW_CPU.get()), x + col + GAP, rowY, col);
        addSettingButton(toggleLabel("screen.gradlemc.setting.gpu_name", GradleMCConfig.OVERLAY_SHOW_GPU_NAME.get()),
                () -> OverlayConfigActions.setBoolean(GradleMCConfig.OVERLAY_SHOW_GPU_NAME, !GradleMCConfig.OVERLAY_SHOW_GPU_NAME.get()), x + (col + GAP) * 2, rowY, col);
        rowY += BUTTON_HEIGHT + GAP;
        Button gpuUsage = Button.builder(Component.literal("GPU usage: unavailable"), button -> {
                })
                .bounds(x, rowY, col, BUTTON_HEIGHT)
                .tooltip(Tooltip.create(Component.translatable("screen.gradlemc.settings.gpu_note")))
                .build();
        gpuUsage.active = false;
        addRenderableWidget(gpuUsage);
        addSettingButton(toggleLabel("screen.gradlemc.setting.background", GradleMCConfig.OVERLAY_BACKGROUND_ENABLED.get()),
                () -> OverlayConfigActions.setBoolean(GradleMCConfig.OVERLAY_BACKGROUND_ENABLED, !GradleMCConfig.OVERLAY_BACKGROUND_ENABLED.get()), x + col + GAP, rowY, col);
        addSettingButton(Component.literal("Scale: " + String.format(Locale.ROOT, "%.2f", GradleMCConfig.OVERLAY_SCALE.get())),
                () -> OverlayConfigActions.setScale(nextScale()), x + (col + GAP) * 2, rowY, col);
        rowY += BUTTON_HEIGHT + GAP;
        addSettingButton(Component.literal("Window: " + GradleMCConfig.OVERLAY_SAMPLING_WINDOW_SECONDS.get() + "s"),
                () -> OverlayConfigActions.setSamplingWindow(nextOf(GradleMCConfig.OVERLAY_SAMPLING_WINDOW_SECONDS.get(), new int[]{30, 60, 120})), x, rowY, col);
        addSettingButton(Component.literal("Update: " + GradleMCConfig.OVERLAY_UPDATE_INTERVAL_MS.get() + "ms"),
                () -> OverlayConfigActions.setUpdateInterval(nextOf(GradleMCConfig.OVERLAY_UPDATE_INTERVAL_MS.get(), new int[]{250, 500, 1000})), x + col + GAP, rowY, col);
    }

    '''
)
settings_method = "".join(
    ("    " + line if line.strip() else line) for line in settings_method.splitlines(keepends=True)
)
replace_region(
    screen,
    "    private void buildSettingWidgets(int x, int y, int width) {",
    "    private void addSettingButton(Component label, Runnable action, int x, int y, int width) {",
    settings_method,
)

lang = f"{FORGE_ROOT}/src/main/resources/assets/gradlemc/lang/en_us.json"
replace_exact(
    lang,
    '  "screen.gradlemc.setting.fps": "FPS",\n',
    '  "screen.gradlemc.setting.branding": "GradleMC Label",\n'
    '  "screen.gradlemc.setting.fps": "Current FPS",\n'
    '  "screen.gradlemc.setting.average_fps": "Average FPS",\n',
)

# Regression tests.
fps_test = f"{FORGE_ROOT}/src/test/java/com/soumyajit/gradlemc/client/overlay/FpsRollingStatsCalculatorSelfTest.java"
replace_exact(
    fps_test,
    "        averageFpsUsesFrameTimes();\n",
    "        averageFpsUsesFrameTimes();\n        currentFpsCanUseMinecraftValue();\n",
)
replace_exact(
    fps_test,
    "    private static void lowsNeedEnoughSamples() {\n",
    "    private static void currentFpsCanUseMinecraftValue() {\n"
    "        FpsRollingStatsCalculator calculator = new FpsRollingStatsCalculator(60);\n"
    "        calculator.recordFrameTimeNanos(THIRTY_FPS_NANOS);\n"
    "        FpsRollingStatsCalculator.Snapshot snapshot = calculator.snapshot().withCurrentFps(144);\n"
    "        assertBetween(snapshot.currentFps(), 143.0D, 145.0D, \"current FPS should use Minecraft current FPS\");\n"
    "        assertBetween(snapshot.averageFps(), 29.0D, 31.0D, \"rolling average should remain frame-time based\");\n"
    "    }\n\n"
    "    private static void lowsNeedEnoughSamples() {\n",
)

defaults_test = f"{FORGE_ROOT}/src/test/java/com/soumyajit/gradlemc/config/OverlayDefaultsSelfTest.java"
replace_exact(
    defaults_test,
    '        assertEquals(500, OverlayDefaults.UPDATE_INTERVAL_MS, "overlay default update interval");\n',
    '        assertEquals(500, OverlayDefaults.UPDATE_INTERVAL_MS, "overlay default update interval");\n'
    '        assertTrue(!OverlayDefaults.SHOW_BRANDING, "GradleMC label should be disabled by default");\n'
    '        assertTrue(!OverlayDefaults.SHOW_AVERAGE_FPS, "average FPS should be disabled by default");\n',
)

# Public release matrices and release notes.
replace_exact(
    "README.md",
    f"| Forge | `1.0.2` | `1.20.1` | `17` | `{OLD_ARTIFACT}` | Forge `47.4.20`; Quick Actions overlay hotfix |",
    f"| Forge | `1.0.3` | `1.20.1` | `17` | `{NEW_ARTIFACT}` | Forge `47.4.20`; FPS accuracy and independent overlay controls |",
)
replace_exact(
    "README.md",
    "| Quilt | `1.0.0` | `1.20.1` | `17` | `gradlemc-quilt-1.20.1-1.0.0.jar` | Quilt `1.20.1` release |\n\n| Field | Value |",
    "| Quilt | `1.0.0` | `1.20.1` | `17` | `gradlemc-quilt-1.20.1-1.0.0.jar` | Quilt `1.20.1` release |\n\n"
    "**Forge 1.20.1 V1.0.3:** corrected the current FPS display and added independent controls for the GradleMC label and rolling Average FPS. Both optional elements are disabled by default.\n\n"
    "| Field | Value |",
)

replace_exact(
    "CHANGELOG.md",
    f"| Forge | `1.0.2` | `1.20.1` | `17` | `{OLD_ARTIFACT}` | Quick Actions overlay hotfix |",
    f"| Forge | `1.0.3` | `1.20.1` | `17` | `{NEW_ARTIFACT}` | FPS accuracy and independent overlay controls |",
)
release_notes = dedent(
    f'''\
    ## `1.0.3` — Forge `1.20.1`

    - Artifact: `{NEW_ARTIFACT}`.
    - Forge target: `47.4.20`.
    - Java target: `17`.

    ### Fixed

    - Corrected the live FPS counter to use Minecraft's current FPS value instead of presenting the most recent frame-time sample as the current counter.
    - Fixed the `GradleMC` compact-overlay label remaining visible when its display option was disabled.
    - Fixed Average FPS being forced on whenever the current FPS counter was enabled.
    - Prevented compact mode from drawing an empty overlay box when every compact element is disabled.

    ### Added

    - Added an independent `GradleMC Label` overlay setting, disabled by default.
    - Added an independent `Average FPS` overlay setting, disabled by default.
    - Added Settings-tab controls and regression tests for the new defaults and authoritative current-FPS path.

    ### Improved

    - Kept rolling Average FPS and low-FPS statistics frame-time based while separating them from the authoritative current-FPS display.
    - Improved overlay configuration clarity and persistence through dedicated Forge config keys.

    '''
)
replace_exact(
    "CHANGELOG.md",
    "## `1.0.2` — Forge `1.20.1`\n",
    release_notes + "## `1.0.2` — Forge `1.20.1`\n",
)

replace_exact(
    "ROADMAP.md",
    f"| Forge | `1.0.2` | `1.20.1` | `17` | `{OLD_ARTIFACT}` | Quick Actions overlay hotfix |",
    f"| Forge | `1.0.3` | `1.20.1` | `17` | `{NEW_ARTIFACT}` | FPS accuracy and independent overlay controls |",
)
replace_exact(
    "ROADMAP.md",
    f"- Forge `1.0.2`: `{OLD_ARTIFACT}`, Java `17`.",
    f"- Forge `1.0.3`: `{NEW_ARTIFACT}`, Java `17`.",
)
replace_exact(
    "SUPPORT.md",
    f"| Forge | `1.20.1` | `1.0.2` | `17` | `{OLD_ARTIFACT}` | Forge `47.4.20`; Quick Actions overlay hotfix |",
    f"| Forge | `1.20.1` | `1.0.3` | `17` | `{NEW_ARTIFACT}` | Forge `47.4.20`; FPS accuracy and independent overlay controls |",
)
replace_exact(
    "SECURITY.md",
    "| GradleMC `1.0.2` for Minecraft `1.20.1` Forge | Current public support target |",
    "| GradleMC `1.0.3` for Minecraft `1.20.1` Forge | Current public support target |",
)
replace_exact("SECURITY.md", OLD_ARTIFACT, NEW_ARTIFACT)
replace_exact("CONTRIBUTING.md", OLD_ARTIFACT, NEW_ARTIFACT)
replace_exact(
    "AGENTS.md",
    f"| Forge | `1.0.2` | `1.20.1` | `17` | `{OLD_ARTIFACT}` | Quick Actions overlay hotfix |",
    f"| Forge | `1.0.3` | `1.20.1` | `17` | `{NEW_ARTIFACT}` | FPS accuracy and independent overlay controls |",
)
replace_exact(
    "docs/RELEASE_CHECKLIST.md",
    f"| Forge | `1.0.2` | `1.20.1` | `17` | `{OLD_ARTIFACT}` | Forge `47.4.20`; Quick Actions overlay hotfix |",
    f"| Forge | `1.0.3` | `1.20.1` | `17` | `{NEW_ARTIFACT}` | Forge `47.4.20`; FPS accuracy and independent overlay controls |",
)
replace_exact(
    "docs/RELEASE_CHECKLIST.md",
    "- [ ] Forge `1.20.1` `1.0.2`: the Quick Actions tab no longer overlays lower controls or text.",
    "- [ ] Forge `1.20.1` `1.0.3`: current FPS matches Minecraft, the GradleMC label is optional and off by default, and Average FPS can be toggled independently.",
)
replace_exact(
    "docs/RELEASE_CHECKLIST.md",
    "- [ ] Overlay remains disabled by default.\n",
    "- [ ] Overlay remains disabled by default.\n"
    "- [ ] Compact overlay draws nothing when every compact element is disabled.\n"
    "- [ ] GradleMC label and Average FPS are independently configurable and disabled by default.\n",
)
replace_exact(
    "curseforge-description.html",
    f'<p><strong style="color: #fb7185;">Forge 1.0.2</strong><br><code style="color: #bae6fd;">{OLD_ARTIFACT}</code></p>',
    f'<p><strong style="color: #fb7185;">Forge 1.0.3</strong><br><code style="color: #bae6fd;">{NEW_ARTIFACT}</code></p>',
)
replace_exact(
    "curseforge-description.html",
    '<p style="color: #cbd5e1;"><strong style="color: #e0f2fe;">Compatibility rule:</strong>',
    '<h3 style="color: #93c5fd;">Latest Forge 1.20.1 update &mdash; V1.0.3</h3>\n'
    '<ul style="padding-left: 22px; color: #cbd5e1;">\n'
    '<li>Corrected the live FPS counter.</li>\n'
    '<li>Made the GradleMC overlay label optional and disabled by default.</li>\n'
    '<li>Added an independent Average FPS setting, also disabled by default.</li>\n'
    '</ul>\n'
    '<p style="color: #cbd5e1;"><strong style="color: #e0f2fe;">Compatibility rule:</strong>',
)

# CI expects and permits the new release artifact.
replace_at_least(".github/workflows/ci.yml", OLD_ARTIFACT, NEW_ARTIFACT, minimum=3)

# The uploaded V1.0.3 jar must exist before the superseded jar is removed.
new_jar = path(f"Releases/Forge/Minecraft 1.20.1/{NEW_ARTIFACT}")
old_jar = path(f"Releases/Forge/Minecraft 1.20.1/{OLD_ARTIFACT}")
if not new_jar.is_file():
    raise RuntimeError(f"New release jar is missing: {new_jar}")
if old_jar.exists():
    old_jar.unlink()

# Remove temporary automation from the verified release commit.
for temporary in (
    ".github/workflows/release-sync-1.0.3.yml",
    ".github/scripts/release_sync_1_0_3.py",
):
    temporary_path = path(temporary)
    if temporary_path.exists():
        temporary_path.unlink()
