# GradleMC Forge 26.1.2 Parity Checklist

Baseline references:

- `C:\MinecraftJavaMods\GradleMC\GradleMCFabric26.1.2`
- `C:\MinecraftJavaMods\GradleMC\GradleMCForge1.20.1`
- `C:\MinecraftJavaMods\GradleMC\GradleMCFabric1.20.1`

Status:

- `/gradlemc`: present through Forge `RegisterCommandsEvent`.
- `/gradlemc help`: lists GUI, status, checks, config/rules, adaptive diagnostics, reports, performance, worldgen, mods, entities, files, and profiler commands.
- `/gradlemc version`: reports GradleMC mod version plus Forge 26.1.2 metadata and uses `GradleMC.CURRENT_MINECRAFT_VERSION` for Minecraft version output.
- `/gradlemc gui`: server-safe command path sends a Forge networking packet to the player client.
- Diagnostics GUI: present with overview, quick actions, tests, profiler, reports, adaptive diagnostics, settings, commands, and about sections.
- GUI keybind: present through Forge `RegisterKeyMappingsEvent`; key translations are included.
- Quick actions tab: present, routed through existing command/client paths, and uses a responsive widget grid so narrow GUI widths do not cut off the controls.
- FPS/performance display: present through client FPS rolling stats and server performance sampling.
- Memory diagnostics: present in command, GUI state, checks, reports, and profiler memory-lite output.
- Loaded mod inspection: present with Forge `ModList`/`IModInfo` implementation.
- Runtime/system information: present in report environment, GUI state, checks, and profiler reports.
- Stability checks: present through `BasicCheckRegistry` and check implementations.
- Report/export functionality: present for diagnostics exports, FPS/performance/worldgen reports, issue bundles, and profiler TXT/JSON output.
- Worldgen/performance sampling: present through server tick managers and command/GUI entry points.
- Smart/adaptive diagnostics: present through adaptive status, risk scoring, smart score/advice/explain, baselines, and thresholds.
- Config/options: present through Forge common config and local overlay preferences.
- Resources/assets/lang/icon/logo: present with Forge `mods.toml`, `pack.mcmeta`, `GradleMC_logo.png`, and `assets/gradlemc/lang/en_us.json`.
- Metadata/version reporting: Gradle properties, `mods.toml` expansion, manifest attributes, commands, GUI, reports, issue bundles, and profiler summaries align on Forge 26.1.2.
- Logs/crash helper paths: report paths, issue bundle exports, and recent report discovery are present.

Forge-specific adaptations:

- Fabric initializers, Fabric command registration, Fabric client command registration, Fabric keybind registration, Fabric metadata, and Fabric API imports are intentionally not copied.
- Forge uses `mods.toml`, `FMLJavaModLoadingContext`, Forge event buses, Forge networking, Forge key mapping registration, and a reflective client bootstrap guarded by `FMLEnvironment.dist`.
- Client GUI/keybind/overlay classes are only registered on physical client to avoid dedicated-server class loading.

Automatic verification performed:

- Official Forge files page checked: MC 26.1.2 latest is Forge `26.1.2-64.0.11`, matching this port's pinned coordinate.
- `./gradlew.bat -version` confirmed Gradle 9.5.0 on Java 25.0.3.
- `clean processResources compileJava`
- `clean build`
- `runData`
- Extracted jar metadata and searched for stale active `1.20.1` strings.
- Dedicated server smoke reached `Done (...)`, loaded Forge `64.0.11` for MC `26.1.2`, and logged `GradleMC command scaffold loaded`.
- Client smoke logged `GradleMC client hooks registered`, `GradleMC command scaffold loaded`, initialized rendering/audio assets, and only showed Mojang Realms auth noise unrelated to GradleMC.
- Final jar inspected as a valid zip with Forge `mods.toml`, Forge manifest attributes, Minecraft `26.1.2`, mod version `1.0.0`, and no Fabric/NeoForge/Quilt metadata resources.

Manual in-game verification still requires an interactive Minecraft session for typing `/gradlemc`, opening `/gradlemc gui`, pressing the configured keybind, exercising report export from a player context, and visually checking every GUI tab.
