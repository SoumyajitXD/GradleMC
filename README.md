# GradleMC

<p align="center">
  <img src="GradleMC_logo.png" width="180" alt="GradleMC logo">
</p>

<p align="center">
  <strong>Local diagnostics, performance evidence, Smart Diagnostics, and exportable troubleshooting reports for modded Minecraft.</strong>
</p>

<p align="center">
  <a href="https://github.com/SoumyajitXD/GradleMC/actions/workflows/ci.yml"><img alt="CI" src="https://github.com/SoumyajitXD/GradleMC/actions/workflows/ci.yml/badge.svg"></a>
  <img alt="Minecraft versions" src="https://img.shields.io/badge/Minecraft-1.20.1%20%7C%201.21.11%20%7C%2026.1.2-brightgreen">
  <img alt="Active loaders" src="https://img.shields.io/badge/Active%20loaders-Forge%20%7C%20Fabric%20%7C%20NeoForge-blueviolet">
  <img alt="Language" src="https://img.shields.io/badge/1.20.1%20v1.1.0-Kotlin-7f52ff">
  <img alt="License Apache--2.0" src="https://img.shields.io/badge/License-Apache--2.0-lightgrey">
  <img alt="Telemetry none" src="https://img.shields.io/badge/Telemetry-none-success">
</p>

---

## Latest 1.20.1 Release

GradleMC `v1.1.0` is available for both **Forge** and **Fabric** on Minecraft `1.20.1`. Both current 1.20.1 implementations are Kotlin-based and still run on Java `17`.

| Loader | Required dependencies | Artifact |
| --- | --- | --- |
| Forge | **Kotlin for Forge** | `gradlemc-1.1.0-forge-1.20.1.jar` |
| Fabric | **Fabric API** + **Fabric Language Kotlin** | `gradlemc-1.1.0-fabric-1.20.1.jar` |

The previous Java GradleMC implementation has been replaced in the current 1.20.1 `v1.1.0` line. Kotlin does **not** replace Minecraft's Java runtime; Minecraft `1.20.1` still requires Java `17`.

The checked-in Forge and Fabric `1.20.1` source trees are synchronized to `v1.1.0`.

---

## Overview

GradleMC is a local, privacy-first diagnostics and troubleshooting mod for modded Minecraft. It helps players, modpack creators, server owners, testers, and developers inspect a modded instance before troubleshooting turns into blind guessing.

It provides an in-game diagnostics interface, lowercase `/gradlemc` commands, memory and environment inspection, bounded performance sampling, local Smart Diagnostics, and exportable reports.

GradleMC is **not** an FPS booster, crash-fixing bot, cloud AI service, or replacement for specialist profilers.

---

## Release Matrix

| Status | Loader | GradleMC | Minecraft | Java | Dependency / notes | Artifact |
| --- | --- | --- | --- | --- | --- | --- |
| **Latest** | Forge | `1.1.0` | `1.20.1` | `17` | Kotlin; **Kotlin for Forge required** | `gradlemc-1.1.0-forge-1.20.1.jar` |
| **Latest** | Fabric | `1.1.0` | `1.20.1` | `17` | Kotlin; **Fabric API + Fabric Language Kotlin required** | `gradlemc-1.1.0-fabric-1.20.1.jar` |
| Published | Forge | `1.0.0` | `1.21.11` | `21` | Forge `61.1.8` | `gradlemc-forge-1.21.11-1.0.0.jar` |
| Published | Fabric | `1.0.0` | `1.21.11` | `21` | Fabric Loader `0.19.3`; Fabric API `0.141.4+1.21.11` | `gradlemc-fabric-1.21.11-1.0.0.jar` |
| Published | NeoForge | `1.0.0` | `1.21.11` | `21` | NeoForge `21.11.42` | `gradlemc-neoforge-1.21.11-1.0.0.jar` |
| Published | Forge | `1.0.0` | `26.1.2` | `25` | Forge `26.1.2-64.0.11` | `gradlemc-forge-26.1.2-1.0.0.jar` |
| Published | Fabric | `1.0.0` | `26.1.2` | `25` | Fabric release | `gradlemc-fabric-26.1.2-1.0.0.jar` |
| Published | NeoForge | `1.0.0` | `26.1.2` | `25` | NeoForge `26.1.2.78` | `gradlemc-neoforge-26.1.2-1.0.0.jar` |
| **Discontinued** | Quilt | `1.0.0` | `1.20.1` | `17` | Legacy only; no new GradleMC Quilt updates | `gradlemc-quilt-1.20.1-1.0.0.jar` |

Use the exact jar matching your Minecraft version and loader. Renaming a jar changes its filename, not its loader compatibility.

### Quilt support ended

No new GradleMC versions will be released for Quilt. Existing Quilt files may remain available as legacy downloads, but the line is discontinued and will not receive new GradleMC releases.

---

## Quick Start — 1.20.1 v1.1.0

### Forge

1. Install Minecraft Java Edition `1.20.1` with Forge.
2. Use Java `17`.
3. Install **Kotlin for Forge**.
4. Install `gradlemc-1.1.0-forge-1.20.1.jar`.
5. Launch Minecraft and run `/gradlemc`.

### Fabric

1. Install Minecraft Java Edition `1.20.1` with Fabric Loader.
2. Use Java `17`.
3. Install **Fabric API**.
4. Install **Fabric Language Kotlin**.
5. Install `gradlemc-1.1.0-fabric-1.20.1.jar`.
6. Launch Minecraft and run `/gradlemc`.

Open the GUI where supported with:

```text
/gradlemc gui
```

Useful commands include:

```text
/gradlemc status
/gradlemc version
/gradlemc memory
/gradlemc check
/gradlemc export
/gradlemc reports latest
/gradlemc smart score
/gradlemc smart advice
```

Minecraft commands are lowercase. Use `/gradlemc`, not `/GradleMC`.

---

## Features

| Feature | What it does |
| --- | --- |
| Diagnostics GUI | In-game control center for checks, status, reports, settings, and supported actions. |
| Command tree | Lowercase `/gradlemc` commands for diagnostics, status, memory, reports, and Smart Diagnostics. |
| Environment inspection | Minecraft, loader, JVM, GradleMC, loaded-mod, config, OS, and path context where supported. |
| Memory diagnostics | JVM heap and memory-pressure information. |
| Performance evidence | Bounded FPS, TPS/MSPT, entity, block-entity, worldgen, and other runtime signals where supported. |
| Smart Diagnostics | Local rule-based scoring and prioritised advice; no cloud inference. |
| Exportable reports | Local troubleshooting evidence for bug reports and controlled comparisons. |

Generated output is stored beneath `<gameDir>/gradlemc/` where supported. Review reports before sharing because diagnostic files can contain local paths, mod names, loader/JVM details, and system context.

---

## Privacy

GradleMC is local by design:

- no behavioural telemetry;
- no analytics tracking;
- no cloud AI or LLM integration;
- no hidden remote diagnostic inference;
- no automatic report upload;
- no broad scanning of unrelated private files.

Smart Diagnostics is local and rule-based. Generated evidence remains local until you choose to share it.

---

## Screenshots

<p align="center">
  <img src="Screenshots/0.png" alt="GradleMC in-game diagnostics screenshot" width="900">
</p>

| Screenshot 1 | Screenshot 2 | Screenshot 3 |
| --- | --- | --- |
| ![GradleMC screenshot 1](Screenshots/1.png) | ![GradleMC screenshot 2](Screenshots/2.png) | ![GradleMC screenshot 3](Screenshots/3.png) |

More screenshots live in [`docs/SCREENSHOTS.md`](docs/SCREENSHOTS.md).

---

## Build From Source

Use the source project matching the loader and Minecraft version.

Current 1.20.1 Kotlin projects:

```text
GradleMC/Forge/Minecraft 1.20.1/
GradleMC/Fabric/Minecraft 1.20.1/
```

Other checked-in source projects include Forge, Fabric, and NeoForge targets for newer published Minecraft versions. Quilt `1.20.1` remains legacy/discontinued.

Use [`docs/RELEASE_CHECKLIST.md`](docs/RELEASE_CHECKLIST.md) before release/export work.

---

## Repository Layout

| Path | Purpose |
| --- | --- |
| [`GradleMC/`](GradleMC/) | Loader/version source projects. |
| [`Releases/`](Releases/) | Committed release artifacts where present. |
| [`Screenshots/`](Screenshots/) | README and documentation screenshots. |
| [`CHANGELOG.md`](CHANGELOG.md) | Release history. |
| [`ROADMAP.md`](ROADMAP.md) | Public planning and loader support policy. |
| [`SUPPORT.md`](SUPPORT.md) | Support guide. |
| [`SECURITY.md`](SECURITY.md) | Security policy. |
| [`CONTRIBUTING.md`](CONTRIBUTING.md) | Contribution rules. |
| [`AGENTS.md`](AGENTS.md) | Maintainer and coding-agent operating rules. |

---

## Project Information

- CurseForge Project ID: `1585182`
- License: Apache-2.0
- Telemetry: none
- Cloud AI / LLM usage: none

GradleMC is an independent Minecraft project. It is not affiliated with, endorsed by, or part of Gradle, Inc. or the Gradle Build Tool, and it does not replace Gradle.