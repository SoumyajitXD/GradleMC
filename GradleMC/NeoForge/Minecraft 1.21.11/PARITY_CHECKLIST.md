# GradleMC NeoForge 1.21.11 Parity Checklist

Baseline references:

- `C:\MinecraftJavaMods\GradleMC\GradleMCNeoForge26.1.2`
- `C:\MinecraftJavaMods\GradleMC\GradleMCForge1.21.11`
- `C:\MinecraftJavaMods\GradleMC\GradleMCFabric1.21.11`

Implemented feature checklist:

- `/gradlemc`: present through NeoForge `RegisterCommandsEvent` on `NeoForge.EVENT_BUS`.
- `/gradlemc help`: lists GUI, status, checks, config/rules, adaptive diagnostics, reports, performance, worldgen, mods, entities, files, and profiler commands.
- `/gradlemc version`: reports GradleMC `1.0.0`, loader `NeoForge`, Minecraft `1.21.11`, and NeoForge `21.11.42`.
- `/gradlemc gui`: server-safe command path sends a NeoForge client-bound payload to the player client.
- Diagnostics GUI: present with overview, quick actions, tests, profiler, reports, adaptive diagnostics, settings, commands, and about sections.
- GUI keybind: present through NeoForge `RegisterKeyMappingsEvent`; key translations are included.
- Quick actions tab: present and routed through existing command/client paths.
- FPS/performance display: present through client FPS rolling stats and server performance sampling.
- Memory diagnostics: present in command, GUI state, checks, reports, and profiler memory-lite output.
- Loaded mod inspection: present with NeoForge `ModList.get()` and `IModInfo`.
- Runtime/system information: present in report environment, GUI state, checks, and profiler reports.
- Stability checks: present through `BasicCheckRegistry` and check implementations.
- Report/export functionality: present for diagnostics exports, FPS/performance/worldgen reports, issue bundles, and profiler TXT/JSON output.
- Crash/log helper paths: issue bundles include safe path/report discovery and optional redacted latest.log snippets.
- Worldgen/performance sampling: present through server tick managers and command/GUI entry points.
- Smart/adaptive diagnostics: present through adaptive status, risk scoring, smart score/advice/explain, baselines, and thresholds.
- Config/options: present through NeoForge common config and local overlay preferences.
- Translations/lang files: present under `assets/gradlemc/lang/en_us.json`.
- Assets/logo/icon: `GradleMC_logo.png` is included and referenced by `neoforge.mods.toml`.
- Metadata/version output: Gradle properties, `neoforge.mods.toml` expansion, manifest attributes, commands, GUI, reports, issue bundles, and profiler summaries align on NeoForge 1.21.11.

NeoForge-specific adaptations:

- Fabric initializers, Fabric command callbacks, Fabric key binding APIs, and `fabric.mod.json` are intentionally not copied.
- Forge `mods.toml`, `FMLJavaModLoadingContext`, Forge static event buses, Forge `SimpleChannel`, and Forge GUI overlay wiring are intentionally not copied.
- Client GUI/keybind/overlay classes are registered only through the physical-client bootstrap guarded by `FMLEnvironment.getDist()`.
- Client-bound payload handlers are registered through NeoForge `RegisterClientPayloadHandlersEvent`; server/common payload registration uses `RegisterPayloadHandlersEvent`.
- Common/server commands do not directly reference GUI screen classes.

Verification targets:

- `clean`, `compileJava`, `processResources`, `build`, and available `check`/test tasks must pass under Java 21 or newer.
- Client smoke should launch far enough to load GradleMC and register client hooks.
- Dedicated server smoke should launch without client-only class loading.
- Exported jar must contain `META-INF/neoforge.mods.toml`, `assets/gradlemc/lang/en_us.json`, `GradleMC_logo.png`, command classes, GUI classes, diagnostics/report classes, and no active stale 1.20.1 metadata.
