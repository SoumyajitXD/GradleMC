# GradleMC

<p align="center">
  <img src="GradleMC_logo.png" width="180" alt="GradleMC logo">
</p>

<p align="center">
  <strong>In-game diagnostics, stability checks, Smart Diagnostics, and exportable troubleshooting reports for Minecraft Forge modpacks.</strong>
</p>

<p align="center">
  <a href="https://github.com/SoumyajitXD/GradleMC/actions/workflows/ci.yml"><img alt="CI" src="https://github.com/SoumyajitXD/GradleMC/actions/workflows/ci.yml/badge.svg"></a>
  <img alt="Minecraft 1.20.1" src="https://img.shields.io/badge/Minecraft-1.20.1-brightgreen">
  <img alt="Forge 47.4.20" src="https://img.shields.io/badge/Forge-47.4.20-orange">
  <img alt="Java 17" src="https://img.shields.io/badge/Java-17-blue">
  <img alt="License Apache--2.0" src="https://img.shields.io/badge/License-Apache--2.0-lightgrey">
  <img alt="Telemetry none" src="https://img.shields.io/badge/Telemetry-none-success">
</p>

<p align="center">
  <a href="#what-gradlemc-does"><strong>Overview</strong></a>
  · <a href="#quick-start">Quick Start</a>
  · <a href="#features">Features</a>
  · <a href="#server-setup">Server Setup</a>
  · <a href="#screenshots">Screenshots</a>
  · <a href="#build-from-source">Build</a>
  · <a href="CHANGELOG.md">Changelog</a>
  · <a href="SUPPORT.md">Support</a>
</p>

<p align="center">
  <a href="https://url-shortener.curseforge.com/kZ5IK">
    <img src="bisecthosting-banner.png" alt="Create a Minecraft server with BisectHosting" width="900">
  </a>
</p>

<p align="center">
  <sub>Want to test GradleMC on a server? Use the banner above or the <a href="https://url-shortener.curseforge.com/kZ5IK">server setup link</a>.</sub>
</p>

---

## What GradleMC Does

GradleMC is a practical diagnostics mod for modded Minecraft. It helps players, modpack makers, server owners, and testers answer the annoying questions that usually turn support threads into smoke, shouting, and ritual mod deletion.

It gives you in-game tools for checking the local modded environment, memory pressure, loaded mods, paths, reports, profiler summaries, performance samples, worldgen pressure, Smart Diagnostics, and exportable support evidence.

**Current public target:** Minecraft Java Edition `1.20.1` on Forge `47.4.20` with Java `17`.

| Field | Value |
| --- | --- |
| Latest public release | `1.0.1` |
| Public artifact | `gradlemc-1.0.1-forge-1.20.1.jar` |
| Supported loader | Forge |
| Supported Minecraft version | `1.20.1` |
| Forge target | `47.4.20` |
| Java target | `17` |
| CurseForge project ID | `1585182` |
| License | Apache-2.0 |

> GradleMC currently claims public support for Forge `1.20.1` only. Fabric, NeoForge, Quilt, and future Minecraft versions are not supported until the code, build, runtime checks, docs, and artifact names all agree. No placeholder-port clownery. 🗿

---

## Quick Start

### Install

1. Install Minecraft Java Edition `1.20.1`.
2. Install Forge `47.4.20`.
3. Use Java `17`.
4. Put `gradlemc-1.0.1-forge-1.20.1.jar` into the instance or server `mods` folder.
5. Install it on the client for the GUI, keybind, overlay, and client-side FPS sampling.
6. Install it on the server for server commands, reports, TPS/MSPT sampling, passive worldgen observation, issue bundles, Smart Diagnostics, and adaptive diagnostics state.

### Open the GUI

```text
/gradlemc gui
```

The default GUI keybind is `G`. Change it in Minecraft controls under the `GradleMC` category.

### First commands to try

```text
/gradlemc status
/gradlemc version
/gradlemc memory
/gradlemc check
/gradlemc export
/gradlemc smart score
/gradlemc smart advice
```

Minecraft commands are lowercase. `/GradleMC` is not cute; it is just wrong.

---

## Features

| Feature | What it gives you |
| --- | --- |
| Diagnostics GUI | A central in-game panel for checks, reports, settings, and diagnostic status. |
| Lowercase command tree | `/gradlemc` commands for status, memory, checks, exports, and Smart Diagnostics. |
| Exportable reports | Local text reports that are easier to share than vague “it lagged” poetry. |
| Mod and environment inspection | Minecraft, Forge, Java, GradleMC, loaded-mod, config, and path details. |
| Memory diagnostics | JVM heap visibility and memory-pressure context. |
| Bounded performance checks | TPS/MSPT, FPS, entity density, block entity density, and passive worldgen observations. |
| Local profiler foundation | Bounded tick, CPU-lite, memory-lite, and combined summaries with TXT/JSON output. |
| Smart Diagnostics | Local rule-based scoring, advice, evidence, confidence, trends, and missing-data notes. |
| Adaptive diagnostics | Lightweight local gameplay-state diagnostics. Not cloud AI. Not generative AI. Not telemetry. |
| Issue-bundle exports | Safer support bundles designed for review before sharing. |

---

## What GradleMC Checks

GradleMC focuses on evidence that helps when a pack is slow, unstable, overloaded, or painful to support:

- Minecraft, Forge, Java, GradleMC, and loaded-mod environment details.
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

## Server Setup

GradleMC is useful on servers because support without evidence is just multiplayer astrology.

Use it server-side for:

- `/gradlemc` server commands;
- TPS/MSPT sampling;
- memory and environment reports;
- loaded-mod inspection;
- passive worldgen observation;
- issue-bundle export;
- Smart Diagnostics summaries.

<p align="center">
  <a href="https://url-shortener.curseforge.com/kZ5IK">
    <img src="bisecthosting-banner.png" alt="Create a Minecraft server with BisectHosting" width="900">
  </a>
</p>

<p align="center">
  <strong><a href="https://url-shortener.curseforge.com/kZ5IK">Create a Minecraft server for GradleMC testing</a></strong>
</p>

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
- Not Spark, VisualVM, or a deep profiler replacement.
- Not an LLM, generative AI system, neural network, cloud AI service, online inference engine, telemetry feature, or analytics feature.
- Not a public support claim for Fabric, NeoForge, Quilt, or non-`1.20.1` Minecraft versions.

Unsupported ports belong on the roadmap until they are real. Fake support claims age like milk in a furnace. 🔥

---

## Build From Source

The current standalone Forge source project lives here:

[`GradleMC/Forge/Minecraft 1.20.1/`](GradleMC/Forge/Minecraft%201.20.1/)

Build on Linux/macOS:

```sh
cd "GradleMC/Forge/Minecraft 1.20.1"
./gradlew build
```

Build on Windows:

```bat
cd "GradleMC\Forge\Minecraft 1.20.1"
gradlew.bat build
```

Run the local self-test task:

```sh
cd "GradleMC/Forge/Minecraft 1.20.1"
./gradlew gradlemcSelfTest
```

Build output is written under:

```text
GradleMC/Forge/Minecraft 1.20.1/build/libs/
```

The exact source artifact name is controlled by `artifact_name` in [`gradle.properties`](GradleMC/Forge/Minecraft%201.20.1/gradle.properties). Before publishing anything, make sure source metadata, public release version, docs, changelog, screenshots, and artifact names agree. Version drift is not a workflow; it is a banana peel with CI badges.

For release/export checks, use [`docs/RELEASE_CHECKLIST.md`](docs/RELEASE_CHECKLIST.md).

---

## Repository Layout

| Path | Purpose |
| --- | --- |
| [`GradleMC/Forge/Minecraft 1.20.1/`](GradleMC/Forge/Minecraft%201.20.1/) | Current standalone Forge `1.20.1` mod source project. |
| [`Screenshots/`](Screenshots/) | README and docs screenshot assets. |
| [`bisecthosting-banner.png`](bisecthosting-banner.png) | Server setup banner used by this README. |
| [`docs/SCREENSHOTS.md`](docs/SCREENSHOTS.md) | Full screenshot gallery. |
| [`docs/SCREENSHOT_PLAN.md`](docs/SCREENSHOT_PLAN.md) | Screenshot maintenance guide. |
| [`.github/workflows/ci.yml`](.github/workflows/ci.yml) | Repository CI workflow. |
| [`CHANGELOG.md`](CHANGELOG.md) | Release and repository-surface history. |
| [`ROADMAP.md`](ROADMAP.md) | Public planning and support gates. |
| [`SUPPORT.md`](SUPPORT.md) | Support guide. |
| [`SECURITY.md`](SECURITY.md) | Security and distribution-chain reporting policy. |
| [`CONTRIBUTING.md`](CONTRIBUTING.md) | Contribution rules and local verification flow. |
| [`AGENTS.md`](AGENTS.md) | Technical operating manual for coding agents and maintainers. |
| [`curseforge-description.html`](curseforge-description.html) | CurseForge project description source for the current public release. |

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

Useful local checks:

```sh
cd "GradleMC/Forge/Minecraft 1.20.1"
./gradlew check gradlemcSelfTest assemble
```

---

## License

GradleMC is licensed under the [Apache License 2.0](LICENSE).
