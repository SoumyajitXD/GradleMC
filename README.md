<p align="center">
  <img src="GradleMC_logo.png" width="180" alt="GradleMC logo">
</p>

<h1 align="center">GradleMC</h1>

<p align="center">
  <strong>In-game diagnostics, stability checks, Smart Diagnostics, and exportable troubleshooting reports for modded Minecraft.</strong>
</p>

<p align="center">
  Stop guessing. GradleMC gives players, pack makers, server owners, and testers cleaner local evidence before troubleshooting turns into ritual mod deletion.
</p>

<p align="center">
  <a href="https://github.com/SoumyajitXD/GradleMC/actions/workflows/ci.yml"><img alt="CI" src="https://github.com/SoumyajitXD/GradleMC/actions/workflows/ci.yml/badge.svg"></a>
  <img alt="Minecraft 1.20.1" src="https://img.shields.io/badge/Minecraft-1.20.1-brightgreen">
  <img alt="Forge 47.4.20" src="https://img.shields.io/badge/Forge-47.4.20-orange">
  <img alt="Fabric 1.20.1" src="https://img.shields.io/badge/Fabric-1.20.1-blueviolet">
  <img alt="Java 17" src="https://img.shields.io/badge/Java-17-blue">
  <img alt="License Apache--2.0" src="https://img.shields.io/badge/License-Apache--2.0-lightgrey">
  <img alt="Telemetry none" src="https://img.shields.io/badge/Telemetry-none-success">
</p>

<p align="center">
  <a href="#quick-start"><strong>Quick Start</strong></a>
  · <a href="#current-public-releases">Releases</a>
  · <a href="#server-setup">Server Setup</a>
  · <a href="#features">Features</a>
  · <a href="#commands">Commands</a>
  · <a href="#screenshots">Screenshots</a>
  · <a href="#build-from-source">Build</a>
  · <a href="CHANGELOG.md">Changelog</a>
  · <a href="SUPPORT.md">Support</a>
</p>

---

## What GradleMC Is

**GradleMC is a practical diagnostics control center for modded Minecraft.**

It helps inspect the local modded environment, memory pressure, loaded mods, paths, reports, profiler summaries, performance samples, worldgen pressure, Smart Diagnostics, issue bundles, and exportable support evidence.

It does not magically repair broken packs. It gives you better information so you can debug with evidence instead of vibes.

---

## Current Public Releases

| Loader | Current public version | Public artifact | Minecraft | Java | Notes |
| --- | --- | --- | --- | --- | --- |
| Forge | `1.0.1` | `gradlemc-1.0.1-forge-1.20.1.jar` | `1.20.1` | `17` | Forge target `47.4.20` |
| Fabric | `1.0.0` | `gradlemc-fabric-1.20.1-1.0.0.jar` | `1.20.1` | `17` | Fabric `1.20.1` release |

| Field | Value |
| --- | --- |
| CurseForge project ID | `1585182` |
| License | Apache-2.0 |
| Telemetry | None |
| Cloud AI / LLM / generative AI | None |

> GradleMC currently claims public support for Minecraft Java Edition `1.20.1` on Forge and Fabric only. NeoForge, Quilt, Bedrock, and future Minecraft versions stay on the roadmap until code, builds, runtime checks, docs, screenshots, and artifact names all agree. Fake support claims rot faster than milk in a furnace. 🔥

---

## Quick Start

### Install

1. Install **Minecraft Java Edition `1.20.1`**.
2. Use **Java `17`**.
3. Pick the GradleMC jar that matches your loader:
   - **Forge:** `gradlemc-1.0.1-forge-1.20.1.jar`
   - **Fabric:** `gradlemc-fabric-1.20.1-1.0.0.jar`
4. Put the jar in the instance or server `mods` folder.
5. Launch the game or server.
6. Run `/gradlemc status` or open the GUI with `/gradlemc gui`.

### Client vs Server

| Install location | Use it for |
| --- | --- |
| Client | GUI, configurable keybind, overlay, local FPS sampling, and client-side troubleshooting context. |
| Server | Commands, reports, TPS/MSPT sampling, memory/environment checks, passive worldgen observation, issue bundles, Smart Diagnostics, and adaptive diagnostics state. |

---

## Server Setup

GradleMC is useful on servers because support without evidence is just multiplayer astrology.

<p align="center">
  <a href="https://url-shortener.curseforge.com/kZ5IK" rel="nofollow">
    <img src="bisecthosting-banner.png" alt="Create a Minecraft server with BisectHosting" width="900">
  </a>
</p>

<p align="center">
  <a href="https://url-shortener.curseforge.com/kZ5IK" rel="nofollow"><strong>Create a Minecraft server with BisectHosting</strong></a>
</p>

Use GradleMC server-side for:

- `/gradlemc` server commands.
- TPS/MSPT sampling.
- Memory and environment reports.
- Loaded-mod inspection.
- Passive worldgen observation.
- Issue-bundle export.
- Smart Diagnostics summaries.

---

## Features

| Feature | What it gives you |
| --- | --- |
| Diagnostics GUI | A central in-game panel for checks, reports, settings, and diagnostic status. |
| Lowercase command tree | `/gradlemc` commands for status, memory, checks, exports, reports, and Smart Diagnostics. |
| Exportable reports | Local text reports that are easier to share than vague “it lagged” reports. |
| Mod and environment inspection | Minecraft, loader, Java, GradleMC, loaded-mod, config, and path details. |
| Memory diagnostics | JVM heap visibility and memory-pressure context. |
| Bounded performance checks | TPS/MSPT, FPS, entity density, block entity density, and passive worldgen observations. |
| Local profiler foundation | Bounded tick, CPU-lite, memory-lite, and combined summaries with TXT/JSON output. |
| Smart Diagnostics | Local rule-based scoring, advice, evidence, confidence, trends, and missing-data notes. |
| Adaptive diagnostics | Lightweight local gameplay-state diagnostics. Not cloud AI. Not generative AI. Not telemetry. |
| Issue-bundle exports | Safer support bundles designed for review before sharing. |

---

## Commands

Open the GUI:

```text
/gradlemc gui
```

The default GUI keybind is `G`. Change it in Minecraft controls under the `GradleMC` category.

Useful commands:

```text
/gradlemc status
/gradlemc version
/gradlemc memory
/gradlemc check
/gradlemc export
/gradlemc smart score
/gradlemc smart advice
```

Minecraft commands are lowercase. `/GradleMC` is not stylish. It is just wrong.

---

## What GradleMC Checks

GradleMC focuses on evidence that helps when a pack is slow, unstable, overloaded, or painful to support:

- Minecraft, loader, Java, GradleMC, and loaded-mod environment details.
- JVM heap pressure and memory status.
- Loaded mod count, previews, and mod ID search.
- Config and report directory access.
- Top-level config-file sanity checks.
- Optional local risk rules from `<gameDir>/gradlemc/rules/gradlemc-rules.json`.
- Nearby entity and block entity density.
- Bounded server TPS/MSPT samples.
- Bounded profiler sessions with tick timelines, slow tick snapshots, Java CPU-lite stack sampling, memory/GC pressure signals, and TXT/JSON output.
- Passive chunk/worldgen pressure observations.
- Client-only FPS samples.
- Smart Diagnostics scoring, evidence, confidence, missing-data notes, and local aggregate baselines.

Generated output is written under:

```text
<gameDir>/gradlemc/
```

Default subfolders include:

```text
reports/
exports/
issue-bundles/
profiles/
rules/
```

---

## Screenshots

<p align="center">
  <img src="Screenshots/0.png" alt="GradleMC in-game diagnostics screenshot" width="900">
</p>

| Screenshot 1 | Screenshot 2 | Screenshot 3 |
| --- | --- | --- |
| ![GradleMC screenshot 1](Screenshots/1.png) | ![GradleMC screenshot 2](Screenshots/2.png) | ![GradleMC screenshot 3](Screenshots/3.png) |

More screenshots live in [`docs/SCREENSHOTS.md`](docs/SCREENSHOTS.md). Screenshot maintenance rules live in [`docs/SCREENSHOT_PLAN.md`](docs/SCREENSHOT_PLAN.md).

---

## Privacy And Honesty

GradleMC is deliberately local-first:

- No telemetry.
- No analytics.
- No hidden cloud calls.
- No LLM integration.
- No generative AI.
- No online inference.

Smart Diagnostics and adaptive diagnostics are **local rule-based systems**.

Reports can still include local paths, mod names, Java details, loader details, and runtime context. Review exports before posting them publicly.

---

## What GradleMC Is Not

- Not a Gradle replacement.
- Not a crash-fixing bot.
- Not Spark, VisualVM, or a deep profiler replacement.
- Not an LLM, generative AI system, neural network, cloud AI service, online inference engine, telemetry feature, or analytics feature.
- Not a public support claim for NeoForge, Quilt, Bedrock, or non-`1.20.1` Minecraft versions.

Unsupported ports belong on the roadmap until they are real.

---

## Build From Source

Source projects live under loader/version folders:

| Loader | Source path |
| --- | --- |
| Forge `1.20.1` | [`GradleMC/Forge/Minecraft 1.20.1/`](GradleMC/Forge/Minecraft%201.20.1/) |
| Fabric `1.20.1` | [`GradleMC/Fabric/Minecraft 1.20.1/`](GradleMC/Fabric/Minecraft%201.20.1/) |

Build Forge on Linux/macOS:

```sh
cd "GradleMC/Forge/Minecraft 1.20.1"
./gradlew build
```

Build Forge on Windows:

```bat
cd "GradleMC\Forge\Minecraft 1.20.1"
gradlew.bat build
```

Build Fabric on Linux/macOS:

```sh
cd "GradleMC/Fabric/Minecraft 1.20.1"
./gradlew build
```

Build Fabric on Windows:

```bat
cd "GradleMC\Fabric\Minecraft 1.20.1"
gradlew.bat build
```

Run the Forge self-test task:

```sh
cd "GradleMC/Forge/Minecraft 1.20.1"
./gradlew gradlemcSelfTest
```

Before publishing anything, make sure source metadata, public release version, docs, changelog, screenshots, and artifact names agree. Version drift is not a workflow; it is a banana peel with CI badges.

For release/export checks, use [`docs/RELEASE_CHECKLIST.md`](docs/RELEASE_CHECKLIST.md).

---

## Repository Layout

| Path | Purpose |
| --- | --- |
| [`GradleMC/Forge/Minecraft 1.20.1/`](GradleMC/Forge/Minecraft%201.20.1/) | Standalone Forge `1.20.1` mod source project. |
| [`GradleMC/Fabric/Minecraft 1.20.1/`](GradleMC/Fabric/Minecraft%201.20.1/) | Standalone Fabric `1.20.1` mod source project. |
| [`GradleMC_logo.png`](GradleMC_logo.png) | README and mod branding asset. |
| [`bisecthosting-banner.png`](bisecthosting-banner.png) | Server creation banner used in the README. |
| [`Screenshots/`](Screenshots/) | README and docs screenshot assets. |
| [`docs/SCREENSHOTS.md`](docs/SCREENSHOTS.md) | Full screenshot gallery. |
| [`docs/SCREENSHOT_PLAN.md`](docs/SCREENSHOT_PLAN.md) | Screenshot maintenance guide. |
| [`.github/workflows/ci.yml`](.github/workflows/ci.yml) | Repository CI workflow. |
| [`CHANGELOG.md`](CHANGELOG.md) | Release and repository-surface history. |
| [`ROADMAP.md`](ROADMAP.md) | Public planning and support gates. |
| [`SUPPORT.md`](SUPPORT.md) | Support guide. |
| [`SECURITY.md`](SECURITY.md) | Security and distribution-chain reporting policy. |
| [`CONTRIBUTING.md`](CONTRIBUTING.md) | Contribution rules and local verification flow. |
| [`AGENTS.md`](AGENTS.md) | Technical operating manual for coding agents and maintainers. |
| [`curseforge-description.html`](curseforge-description.html) | CurseForge project description source for the current public releases. |

---

## Contributing

Good contributions are welcome. Sloppy ones get eaten by the checklist.

Before opening a PR:

- Read [`CONTRIBUTING.md`](CONTRIBUTING.md).
- Read [`AGENTS.md`](AGENTS.md) before code or automation changes.
- Keep Minecraft command examples lowercase.
- Do not claim loader/version support until implementation, builds, runtime checks, docs, and artifact naming prove it.
- Do not add telemetry, analytics, cloud calls, LLMs, generative AI, or fake AI marketing.
- Run the relevant Gradle checks before opening a PR.

Useful local checks for the currently documented Forge source project:

```sh
cd "GradleMC/Forge/Minecraft 1.20.1"
./gradlew check gradlemcSelfTest assemble
```

---

## License

GradleMC is licensed under the [Apache License 2.0](LICENSE).
