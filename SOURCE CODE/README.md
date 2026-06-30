# GradleMC Source

![GradleMC logo](src/main/resources/GradleMC_logo.png)

**In-game diagnostics, stability, and troubleshooting toolkit for modded Minecraft.**

GradleMC is a Minecraft Java Edition mod that helps players, pack makers, server owners, and testers inspect a running modpack without leaving the game. It provides the lowercase `/gradlemc` command tree, an in-game diagnostics control center, an optional stats overlay, bounded performance checks, local profiler foundation tools, local Smart Diagnostics, adaptive diagnostics status, and shareable text reports.

GradleMC is not a Gradle replacement, not a crash-fixing bot, and not a replacement for Spark or deeper profiling tools. The profiler tools are useful local evidence, not a claim of Spark parity.

---

## Project Facts

| Field | Current value |
| --- | --- |
| Mod ID | `gradlemc` |
| Display name | GradleMC |
| Current public release | `1.0.1` |
| Project ID | `1585182` |
| Minecraft target | Java Edition `1.20.1` |
| Loader | Forge |
| Forge target | `47.4.20` |
| Java | `17` |
| Build system | Gradle with ForgeGradle 6.x |
| License | Apache-2.0 |

Current supported build: Minecraft `1.20.1` on Forge. Future architecture may support more loaders and Minecraft versions, but this release does not claim Fabric, NeoForge, Quilt, or other Minecraft-version support.

Release artifact for v1.0.1:

```text
gradlemc-1.0.1-forge-1.20.1.jar
```

---

## Variant Matrix

GradleMC uses [`config/gradlemc-variants.json`](config/gradlemc-variants.json) as the source of truth for loader/version targets, status, buildability, Java version, Gradle plugin kind, and artifact names.

Currently supported:

| Minecraft | Loader | Java | Variant ID |
| --- | --- | --- | --- |
| `1.20.1` | Forge | 17 | `forge-1.20.1` |

Planned or experimental candidates are roadmap entries, not downloads:

| Group | Examples |
| --- | --- |
| `1.19.2` | Forge/Fabric candidates, NeoForge unsupported unless official evidence changes |
| `1.20.x` | Forge verification candidates, Fabric planned candidates, NeoForge verification candidates |
| `1.21.x` | Forge/Fabric/NeoForge planned or verification candidates |
| `26.x` | Experimental or needs-verification candidates only |

The broader plan is documented in [`docs/PORTING_MATRIX.md`](docs/PORTING_MATRIX.md), and the extraction audit is in [`docs/COMMON_CORE_EXTRACTION.md`](docs/COMMON_CORE_EXTRACTION.md). Disabled variants must not produce placeholder jars or public support claims.

Automation details are documented in [`docs/AUTOMATION_PIPELINE.md`](docs/AUTOMATION_PIPELINE.md).

---

## What It Checks

GradleMC focuses on stability context that is useful when a pack is slow, unstable, overloaded, or hard to support:

- Java, Forge, Minecraft, GradleMC, and loaded-mod environment details.
- JVM memory pressure.
- Loaded mod count, list previews, and mod ID search.
- Report directory and config directory access.
- Top-level config-file sanity checks.
- Optional local risk rules from `<gameDir>/gradlemc/rules/gradlemc-rules.json`.
- Nearby entity and block entity density from an in-game player location.
- Bounded server TPS/MSPT samples.
- Bounded local profiler sessions with tick timelines, slow tick snapshots, Java CPU-lite stack sampling, memory/GC pressure signals, and TXT/JSON output.
- Passive chunk/worldgen pressure observations.
- Client-only FPS samples.
- Smart Diagnostics scoring, recommendations, evidence, confidence, missing data notes, and local aggregate baselines.

Most commands show a short chat summary. Longer output is written to local report files under:

```text
<gameDir>/gradlemc/reports/
```

---

## GUI, Overlay, And Keybinds

The client GUI is the main GradleMC control center. It shows overview status and can run common diagnostics from buttons without typing commands manually.

- Open it with `/gradlemc gui`.
- Open it with the configurable `Open GradleMC GUI` keybind.
- Default key: `G`.
- Controls category: `GradleMC`.
- Close it with Escape or the Close button.

The optional stats overlay is disabled by default. Overlay keybinds are unbound by default. GUI rendering, overlay rendering, keybind handling, and FPS sampling are client-only and must not be loaded by dedicated servers.

---

## Common Commands

All command examples are lowercase. Use `/gradlemc gui`, not an uppercase command root.

| Command | Purpose |
| --- | --- |
| `/gradlemc` | Show command help. |
| `/gradlemc help` | Show command groups. |
| `/gradlemc status` | Show report, rules, Smart Diagnostics, adaptive diagnostics, GUI, and active-test status. |
| `/gradlemc gui` | Open the client diagnostics control center for an in-game player. |
| `/gradlemc version` | Show GradleMC, Minecraft, Forge, Java, and loaded-mod count. |
| `/gradlemc memory` | Show a JVM heap snapshot. |
| `/gradlemc check` | Run the default stability checks. Requires permission level 2. |
| `/gradlemc export` | Write a full text diagnostics report. Requires permission level 2. |
| `/gradlemc reports latest` | Show a short summary from the newest report. |
| `/gradlemc smart score` | Compute a local stability score. |
| `/gradlemc smart advice` | Show prioritized recommendations. |
| `/gradlemc smart explain` | Show evidence, confidence, trends, and missing-data notes. |
| `/gradlemc perf start <seconds>` | Start bounded server TPS/MSPT sampling. |
| `/gradlemc perf stop` | Stop the active performance sample and export a report. |
| `/gradlemc profiler start` | Start a conservative combined local profile. |
| `/gradlemc profiler stop` | Stop the active profile and write TXT/JSON reports. |
| `/gradlemc worldgen start <seconds>` | Start passive chunk/worldgen pressure observation. |
| `/gradlemc worldgen stop` | Stop the active worldgen observation and export a report. |
| `/gradlemc testfps start <seconds>` | Start a bounded client-only FPS test. |
| `/gradlemc testfps stop` | Stop the active FPS test. |
| `/gradlemc issuebundle create` | Create a safe support ZIP bundle. |

Config, rules, file, scan, performance, export, and issue-bundle commands are intentionally permission-gated where appropriate.

---

## Smart Diagnostics

Smart Diagnostics are local rule-based scoring and recommendations built from the data GradleMC can see.

Baselines are stored locally at:

```text
<gameDir>/gradlemc/adaptive-baseline.properties
```

Missing data lowers confidence. GradleMC should not invent evidence when a performance sample, FPS sample, worldgen observation, or scan has not been collected.

---

## Adaptive Diagnostics

Adaptive diagnostics are local, lightweight, rule-based gameplay-state logic. They are separate from Smart Diagnostics.

Adaptive diagnostics are not an LLM, generative AI system, cloud AI service, online inference engine, neural network, embedding system, ChatGPT integration, telemetry feature, or analytics feature.

---

## Configuration And Output

Forge creates the common config at:

```text
config/gradlemc-common.toml
```

Generated GradleMC output is written under:

```text
<gameDir>/gradlemc/
```

Default subfolders include `reports/`, `exports/`, `issue-bundles/`, `profiles/`, and `rules/`. Forge still owns `config/gradlemc-common.toml`.

Review exported reports before sharing them. GradleMC avoids broad private-file scans, full logs, crash reports, full config folders, mod jars, and full mods folders by default, but reports can still include local paths, loaded mod names, Java details, and runtime context.

---

## Installation

1. Install Forge for Minecraft Java Edition `1.20.1`.
2. Use Java `17`.
3. Put `gradlemc-1.0.1-forge-1.20.1.jar` in the instance or server `mods` folder.
4. Install on the client for the GUI, keybind, overlay, and FPS testing.
5. Install on the server for server commands, reports, adaptive diagnostics state, TPS/MSPT sampling, passive worldgen observation, scans, and issue bundles.

Client-only features are not available from a dedicated server console. Commands that require an in-game player should fail clearly when run from console.

---

## Build / Verify / Export

Use Java `17` for the current Forge `1.20.1` build. Gradle wrapper commands are the supported entrypoint; offline mode works only when ForgeGradle, Minecraft, and plugins are already cached.

Verify metadata, variant rules, command casing, stale old-folder paths, and release guardrails:

```sh
./gradlew checkAutomationTools validateVariantMatrix checkProjectIdentity checkCommandCasing checkFalseSupportClaims checkReleaseMetadata
```

On Windows:

```powershell
pwsh ./tools/pwsh/validate.ps1
```

Build the mod jar:

```sh
./gradlew build
```

On Windows:

```bat
gradlew.bat build
```

The expected release jar is:

```text
build/libs/gradlemc-1.0.1-forge-1.20.1.jar
```

Export the verified release jar to `build/exports/`:

```sh
./gradlew exportReleaseJar
```

Or choose a local handoff folder:

```sh
./gradlew exportReleaseJar -PgradlemcExportDir=/path/to/output
```

On Windows PowerShell:

```powershell
pwsh ./tools/pwsh/export-release.ps1 -OutputDir "C:\path\to\output"
```

The export command fails if the expected artifact is missing and prints the final jar path on success.

Variant automation:

```sh
./gradlew checkToolchains
./gradlew checkAutomationTools
./gradlew printVariantMatrix
./gradlew validateVariantMatrix
./gradlew checkProjectIdentity
./gradlew checkCommandCasing
./gradlew checkFalseSupportClaims
./gradlew checkReleaseMetadata
./gradlew buildVariant -PgradlemcVariant=forge-1.20.1
./gradlew buildEnabledVariants
./gradlew assembleVariantMatrix
./gradlew checkVariantMatrix
```

For now, only `forge-1.20.1` is enabled, buildable, and wired to the existing ForgeGradle build. Planned Fabric, NeoForge, later Forge, and 26.x entries are metadata and release gates, not buildable ports.

Python remains the source of truth for variant and release validation. PowerShell remains thin Windows wrapper glue. Node.js and TypeScript validate docs and optional CurseForge/web-facing assets when `package.json` exists. Kotlin is confined to Gradle build logic in `buildSrc/` and is not part of the mod runtime.

Offline builds can work only when ForgeGradle, Minecraft, and plugin dependencies are already cached:

```sh
./gradlew build --offline
```

---

## Development Notes

Before changing code, read [AGENTS.md](AGENTS.md).

Keep contributions focused:

- Preserve the `gradlemc` mod id.
- Keep Minecraft command literals and examples lowercase.
- Keep `/gradlemc gui` as the GUI command.
- Stay on Forge `1.20.1` and Java `17` unless a version migration is explicitly chosen.
- Keep client-only classes out of common/server code.
- Run `./gradlew build` after Java or resource changes.
- Avoid unrelated rewrites, dependency churn, telemetry, and unsupported feature claims.

---

## License

GradleMC is licensed under the Apache License 2.0. See [LICENSE](LICENSE).
