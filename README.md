# GradleMC

![GradleMC logo](src/main/resources/GradleMC_logo.png)

**In-game diagnostics and stability checker for Minecraft modpacks.**

GradleMC is a Minecraft Java Edition mod that helps players, pack makers, server owners, and testers inspect a running modpack without leaving the game. It provides the lowercase `/gradlemc` command tree, a read-only client GUI, bounded performance checks, local Smart Diagnostics, adaptive diagnostics status, and shareable text reports.

GradleMC is not a Gradle replacement, not a crash-fixing bot, and not a full profiler. It is a small diagnostics layer for collecting practical evidence from a live pack.

## Project Facts

| Field | Current value |
| --- | --- |
| Mod ID | `gradlemc` |
| Display name | GradleMC |
| Version | `1.0.0` |
| Project ID | `1585182` |
| Minecraft target | Java Edition `1.20.1` |
| Loader | Forge |
| Forge target | `47.4.20` |
| Java | 17 |
| Build system | Gradle with ForgeGradle 6.x |
| License | Apache-2.0 |

GradleMC currently targets Forge 1.20.1 only. It does not claim Fabric, NeoForge, or other Minecraft-version support.

Release artifact for v1.0.0:

```text
gradlemc-1.0.0-forge-1.20.1.jar
```

## What It Checks

GradleMC focuses on stability context that is useful when a pack is slow, unstable, overloaded, or hard to support:

- Java, Forge, Minecraft, GradleMC, and loaded-mod environment details.
- JVM memory pressure.
- Loaded mod count, list previews, and mod ID search.
- Report directory and config directory access.
- Top-level config-file sanity checks.
- Optional local risk rules from `config/gradlemc/gradlemc-rules.json`.
- Nearby entity and block entity density from an in-game player location.
- Bounded server TPS/MSPT samples.
- Passive chunk/worldgen pressure observations.
- Client-only FPS samples.
- Smart Diagnostics scoring, recommendations, evidence, confidence, missing data notes, and local aggregate baselines.

Most commands show a short chat summary. Longer output is written to local report files under:

```text
config/gradlemc/reports/
```

## GUI And Keybind

The client GUI is read-only and intended for quick status checks.

- Open it with `/gradlemc gui`.
- Open it with the configurable `Open GradleMC GUI` keybind.
- Default key: `G`.
- Controls category: `GradleMC`.
- Close it with Escape or the Close button.

The GUI shows overview status, adaptive diagnostics state, read-only settings, key commands, and about information. GUI rendering, keybind handling, and FPS sampling are client-only.

## Common Commands

All command examples are lowercase. Use `/gradlemc gui`, not an uppercase command root.

| Command | Purpose |
| --- | --- |
| `/gradlemc` | Show command help. |
| `/gradlemc help` | Show command groups. |
| `/gradlemc status` | Show report, rules, Smart Diagnostics, adaptive diagnostics, GUI, and active-test status. |
| `/gradlemc gui` | Open the read-only client GUI for an in-game player. |
| `/gradlemc version` | Show GradleMC, Minecraft, Forge, Java, and loaded-mod count. |
| `/gradlemc memory` | Show a JVM heap snapshot. |
| `/gradlemc check` | Run the default stability checks. Requires permission level 2. |
| `/gradlemc export` | Write a full text diagnostics report. Requires permission level 2. |

## Mod, Config, And File Commands

| Command | Purpose |
| --- | --- |
| `/gradlemc mods` | Show loaded mod count. |
| `/gradlemc mods count` | Show loaded mod count. |
| `/gradlemc mods list` | Preview loaded mods and write a full list when long. |
| `/gradlemc mods search <modid>` | Search loaded mod IDs and display names. |
| `/gradlemc config path` | Show Minecraft and GradleMC config paths. |
| `/gradlemc config files` | Count and preview top-level config files. |
| `/gradlemc config check` | Run lightweight config directory checks. |
| `/gradlemc rules path` | Show local risk-rule paths. |
| `/gradlemc rules example` | Write an example rule file if missing. |
| `/gradlemc rules reload` | Reload local risk rules from disk. |
| `/gradlemc rules check` | Run local risk-rule checks. |
| `/gradlemc files` | Check report/config access and latest-log visibility. |

Config, rules, file, scan, performance, export, and issue-bundle commands are intentionally permission-gated where appropriate.

## Scans, Performance, And Reports

| Command | Purpose |
| --- | --- |
| `/gradlemc entities` | Count nearby entities using the configured default radius. |
| `/gradlemc entities <radius>` | Count nearby entities in a bounded radius. |
| `/gradlemc blockentities` | Count nearby block entities using the configured default radius. |
| `/gradlemc blockentities <radius>` | Count nearby block entities in loaded chunks. |
| `/gradlemc perf start <seconds>` | Start bounded server TPS/MSPT sampling. |
| `/gradlemc perf stop` | Stop the active performance sample and export a report. |
| `/gradlemc worldgen` | Show worldgen observation help. |
| `/gradlemc worldgen start <seconds>` | Start passive chunk/worldgen pressure observation. |
| `/gradlemc worldgen stop` | Stop the active worldgen observation and export a report. |
| `/gradlemc worldgen status` | Show whether worldgen observation is active. |
| `/gradlemc worldgen latest` | Show the latest worldgen observation summary. |
| `/gradlemc testfps start <seconds>` | Start a bounded client-only FPS test. |
| `/gradlemc testfps stop` | Stop the active FPS test. |
| `/gradlemc reports list` | List recent GradleMC report files. |
| `/gradlemc reports latest` | Show a short summary from the newest report. |
| `/gradlemc issuebundle create` | Create a safe support ZIP bundle. |

Worldgen observation is passive. It does not teleport players, force-load chunks, or generate chunks. TPS/MSPT, FPS, entity, and worldgen diagnostics are bounded samples, not benchmark certification.

## Smart Diagnostics

Smart Diagnostics are local rule-based scoring and recommendations built from the data GradleMC can see.

| Command | Purpose |
| --- | --- |
| `/gradlemc smart` | Show Smart Diagnostics help and baseline status. Requires permission level 2. |
| `/gradlemc smart score` | Compute a local stability score. |
| `/gradlemc smart advice` | Show prioritized recommendations. |
| `/gradlemc smart explain` | Show evidence, confidence, trends, and missing data notes. |
| `/gradlemc smart baseline` | Show local aggregate baseline metrics. |
| `/gradlemc smart baseline reset confirm` | Delete only the adaptive baseline file. |
| `/gradlemc smart thresholds` | Show fixed and adaptive threshold behavior. |

Baselines are stored locally at:

```text
config/gradlemc/adaptive-baseline.properties
```

Missing data lowers confidence. GradleMC should not invent evidence when a performance sample, FPS sample, worldgen observation, or scan has not been collected.

## Adaptive Diagnostics

Adaptive diagnostics are local, lightweight, rule-based gameplay-state logic. They are separate from Smart Diagnostics.

| Command | Purpose |
| --- | --- |
| `/gradlemc ai` | Show adaptive diagnostics help. |
| `/gradlemc ai status` | Show runtime adaptive diagnostics state for the current player. |
| `/gradlemc ai reset` | Reset runtime adaptive diagnostics data for the current player. Requires permission level 2. |

The current implementation samples bounded server-side player signals such as darkness exposure, underground time, recent damage, recent mob kills, recent deaths, sleep timing, movement pressure, armor, dimension context, and cooldowns. It can show occasional bounded ambience or warning messages when enabled.

Adaptive diagnostics are not an LLM, generative AI system, cloud AI service, online inference engine, neural network, embedding system, ChatGPT integration, telemetry feature, or analytics feature.

## Configuration

Forge creates the common config at:

```text
config/gradlemc-common.toml
```

Important config areas include:

- Report writing and report directory behavior.
- Entity and block entity scan radius bounds.
- Performance, worldgen, and FPS test duration bounds.
- Safe issue-bundle export and optional redacted latest-log snippets.
- Local risk-rule checks and rule-file name.
- Smart Diagnostics, adaptive baseline, anomaly sensitivity, and recommendation limits.
- Adaptive diagnostics toggles, stability risk bounds, cooldowns, debug logging, death intensity reduction, high-intensity event allowance, and difficulty multiplier.
- GUI adaptive diagnostics status refresh interval.

If Forge reports that `gradlemc-common.toml` is being corrected after an update, that usually means new config keys were added and Forge is filling in defaults. It should settle after the corrected file is saved. Deleting the old file regenerates defaults, but only do that after reviewing any local changes.

Local risk rules are read from:

```text
config/gradlemc/gradlemc-rules.json
```

Reports are written under:

```text
config/gradlemc/reports/
```

Review exported reports before sharing them. GradleMC avoids broad private-file scans, full logs, crash reports, full config folders, mod jars, and full mods folders by default, but reports can still include local paths, loaded mod names, Java details, and runtime context.

## Installation

1. Install Forge for Minecraft Java Edition 1.20.1.
2. Use Java 17.
3. Put the GradleMC jar in the instance or server `mods` folder.
4. Install on the client for the GUI, keybind, and FPS testing.
5. Install on the server for server commands, reports, adaptive diagnostics state, TPS/MSPT sampling, passive worldgen observation, scans, and issue bundles.

Client-only features are not available from a dedicated server console. Commands that require an in-game player should fail clearly when run from console.

## Building From Source

Build the mod jar with:

```sh
./gradlew build
```

On Windows:

```bat
gradlew.bat build
```

The built jar is normally written under `build/libs/`.

For IntelliJ IDEA run configuration setup:

```sh
./gradlew genIntellijRuns
```

Offline builds can work only when ForgeGradle, Minecraft, and plugin dependencies are already cached:

```sh
./gradlew build --offline
```

## Development Notes

Before changing code, read [AGENTS.md](AGENTS.md).

Keep contributions focused:

- Preserve the `gradlemc` mod id.
- Keep Minecraft command literals and examples lowercase.
- Keep `/gradlemc gui` as the GUI command.
- Stay on Forge 1.20.1 and Java 17 unless a version migration is explicitly chosen.
- Keep client-only classes out of common/server code.
- Run `./gradlew build` after Java or resource changes.
- Avoid unrelated rewrites, dependency churn, telemetry, and unsupported feature claims.

## License

GradleMC is licensed under the Apache License 2.0. See [LICENSE](LICENSE).
