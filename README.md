# GradleMC

<p align="center">
  <img src="GradleMC_logo.png" width="180" alt="GradleMC logo">
</p>

<p align="center">
  <strong>In-game diagnostics, stability checks, Smart Diagnostics, and exportable troubleshooting reports for Minecraft modpacks.</strong>
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
  <a href="#what-it-does"><strong>Overview</strong></a>
  · <a href="#quick-start">Quick Start</a>
  · <a href="#features">Features</a>
  · <a href="#server-hosting">Server Hosting</a>
  · <a href="#screenshots">Screenshots</a>
  · <a href="#build-from-source">Build</a>
  · <a href="CHANGELOG.md">Changelog</a>
  · <a href="SUPPORT.md">Support</a>
</p>

---

## What It Does

GradleMC is a practical diagnostics mod for modded Minecraft. It helps players, modpack makers, server owners, and testers stop guessing blindly when a pack starts lagging, crashing, or behaving like a cursed toaster.

It gives you in-game tools for checking the local modded environment, memory pressure, loaded mods, paths, reports, profiler summaries, performance samples, worldgen pressure, Smart Diagnostics, and exportable support evidence.

GradleMC does **not** pretend to magically fix broken packs. It gives you evidence so troubleshooting can move from “delete random mods until it works” to “look at the actual problem.”

---

## Current Public Targets

| Loader | Latest public release | Public artifact | Minecraft | Java | Notes |
| --- | --- | --- | --- | --- | --- |
| Forge | `1.0.1` | `gradlemc-1.0.1-forge-1.20.1.jar` | `1.20.1` | `17` | Forge target `47.4.20` |
| Fabric | `1.0.0` | `gradlemc-fabric-1.20.1-1.0.0.jar` | `1.20.1` | `17` | Fabric `1.20.1` release |

| Field | Value |
| --- | --- |
| CurseForge project ID | `1585182` |
| License | Apache-2.0 |
| Telemetry | None |

> GradleMC currently claims public support for Minecraft Java Edition `1.20.1` on Forge and Fabric only. NeoForge, Quilt, Bedrock, and future Minecraft versions stay unsupported until the code, build, runtime checks, docs, and artifact names all agree. Fake support claims are not a roadmap; they are clown makeup with a version number. 🗿

---

## Quick Start

### Install

1. Install Minecraft Java Edition `1.20.1`.
2. Use Java `17`.
3. Pick the jar that matches your loader:
   - Forge: `gradlemc-1.0.1-forge-1.20.1.jar`
   - Fabric: `gradlemc-fabric-1.20.1-1.0.0.jar`
4. Drop the matching jar into your instance or server `mods` folder.
5. Install GradleMC on the client for the GUI, keybind, overlay, and client-side FPS sampling.
6. Install GradleMC on the server for server commands, reports, TPS/MSPT sampling, passive worldgen observation, issue bundles, Smart Diagnostics, and adaptive diagnostics state.

### Open the GUI

```text
/gradlemc gui
```

The default GUI keybind is `G`. Change it in Minecraft controls under the `GradleMC` category.

### First Commands To Try

```text
/gradlemc status
/gradlemc version
/gradlemc memory
/gradlemc check
/gradlemc export
/gradlemc smart score
/gradlemc smart advice
```

Minecraft commands are lowercase. `/GradleMC` is not “branding”; it is just wrong.

---

## Features

| Feature | What it gives you |
| --- | --- |
| Diagnostics GUI | A central in-game panel for checks, reports, settings, and diagnostic status. |
| Lowercase command tree | `/gradlemc` commands for status, memory, checks, exports, and Smart Diagnostics. |
| Exportable reports | Local text reports that are easier to share than vague “it lagged” poetry. |
| Mod and environment inspection | Minecraft, loader, Java, GradleMC, loaded-mod, config, and path details. |
| Memory diagnostics | JVM heap visibility and memory-pressure context. |
| Bounded performance checks | TPS/MSPT, FPS, entity density, block entity density, and passive worldgen observations. |
| Local profiler foundation | Bounded tick, CPU-lite, memory-lite, and combined summaries with TXT/JSON output. |
| Smart Diagnostics | Local rule-based scoring, advice, evidence, confidence, trends, and missing-data notes. |
| Adaptive diagnostics | Lightweight local gameplay-state diagnostics. Not cloud AI. Not generative AI. Not telemetry. |
| Issue-bundle exports | Safer support bundles designed for review before sharing. |

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

## Server Hosting

GradleMC is useful on servers because support without evidence is just multiplayer astrology.

<p align="center">
  <a href="https://url-shortener.curseforge.com/kZ5IK">
    <img src="bisecthosting-banner.png" alt="Create a Minecraft server with BisectHosting" width="900">
  </a>
</p>

<p align="center">
  <a href="https://url-shortener.curseforge.com/kZ5IK"><strong>Create a GradleMC-ready Minecraft server</strong></a>
</p>

Use GradleMC server-side for:

- `/gradlemc` server commands;
- TPS/MSPT sampling;
- memory and environment reports;
- loaded-mod inspection;
- passive worldgen observation;
- issue-bundle export;
- Smart Diagnostics summaries.

For best results, install the same GradleMC loader target on both the client and server when troubleshooting multiplayer modpacks.

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

## What GradleMC Is Not

GradleMC is deliberately honest about its limits:

- Not a Gradle replacement.
- Not a crash-fixing bot.
- Not Spark, VisualVM, or a deep-profiler replacement.
- Not an LLM, generative AI system, neural network, cloud AI service, online inference engine, telemetry feature, or analytics feature.
- Not a public support claim for NeoForge, Quilt, Bedrock, or non-`1.20.1` Minecraft versions.

Unsupported ports belong on the roadmap until they are real. Fake support claims age like milk in a furnace. 🔥

---

## Build From Source

The currently documented standalone source projects are:

| Loader | Source path |
| --- | --- |
| Forge `1.20.1` | [`GradleMC/Forge/Minecraft 1.20.1/`](GradleMC/Forge/Minecraft%201.20.1/) |
| Fabric `1.20.1` | [`GradleMC/Fabric/Minecraft 1.20.1/`](GradleMC/Fabric/Minecraft%201.20.1/) |

### Forge Build

Linux/macOS:

```sh
cd "GradleMC/Forge/Minecraft 1.20.1"
./gradlew build
```

Windows:

```bat
cd "GradleMC\Forge\Minecraft 1.20.1"
gradlew.bat build
```

Run the Forge self-test task:

```sh
cd "GradleMC/Forge/Minecraft 1.20.1"
./gradlew gradlemcSelfTest
```

### Fabric Build

Linux/macOS:

```sh
cd "GradleMC/Fabric/Minecraft 1.20.1"
./gradlew build
```

Windows:

```bat
cd "GradleMC\Fabric\Minecraft 1.20.1"
gradlew.bat build
```

Before publishing anything, make sure source metadata, public release version, docs, changelog, screenshots, and artifact names agree. Version drift is not a workflow; it is a banana peel with CI badges.

For release/export checks, use [`docs/RELEASE_CHECKLIST.md`](docs/RELEASE_CHECKLIST.md).

---

## Repository Layout

| Path | Purpose |
| --- | --- |
| [`GradleMC/Forge/Minecraft 1.20.1/`](GradleMC/Forge/Minecraft%201.20.1/) | Standalone Forge `1.20.1` mod source project. |
| [`GradleMC/Fabric/Minecraft 1.20.1/`](GradleMC/Fabric/Minecraft%201.20.1/) | Standalone Fabric `1.20.1` mod source project. |
| [`Screenshots/`](Screenshots/) | README and docs screenshot assets. |
| [`bisecthosting-banner.png`](bisecthosting-banner.png) | Server hosting banner asset used in this README. |
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
