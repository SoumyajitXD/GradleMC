# GradleMC

<p align="center">
  <img src="GradleMC_logo.png" width="180" alt="GradleMC logo">
</p>

<p align="center">
  <strong>In-game diagnostics, stability checks, Smart Diagnostics, and exportable troubleshooting reports for Minecraft modpacks.</strong>
</p>

<p align="center">
  <a href="https://github.com/SoumyajitXD/GradleMC/actions/workflows/ci.yml"><img alt="CI" src="https://github.com/SoumyajitXD/GradleMC/actions/workflows/ci.yml/badge.svg"></a>
  <img alt="Minecraft versions" src="https://img.shields.io/badge/Minecraft-1.20.1%20%7C%201.21.11%20%7C%2026.1.2-brightgreen">
  <img alt="Loaders" src="https://img.shields.io/badge/Loaders-Forge%20%7C%20Fabric%20%7C%20NeoForge%20%7C%20Quilt-blueviolet">
  <img alt="Java versions" src="https://img.shields.io/badge/Java-17%20%7C%2021%20%7C%2025-blue">
  <img alt="License Apache--2.0" src="https://img.shields.io/badge/License-Apache--2.0-lightgrey">
  <img alt="Telemetry none" src="https://img.shields.io/badge/Telemetry-none-success">
</p>

<p align="center">
  <a href="#overview"><strong>Overview</strong></a>
  · <a href="#supported-releases">Releases</a>
  · <a href="#quick-start">Quick Start</a>
  · <a href="#features">Features</a>
  · <a href="#server-hosting">Server Hosting</a>
  · <a href="#screenshots">Screenshots</a>
  · <a href="#build-from-source">Build</a>
</p>

---

## Overview

GradleMC is a diagnostics mod for modded Minecraft. It helps players, modpack makers, server owners, and testers inspect a modded instance before troubleshooting turns into blind guessing.

It provides an in-game control center, readable `/gradlemc` commands, stability checks, memory diagnostics, mod and environment inspection, bounded performance sampling, Smart Diagnostics, adaptive local diagnostics, and exportable reports.

**Current public support:**

- Minecraft `1.21.11` on Forge, Fabric, and NeoForge.
- Minecraft `1.20.1` on Forge, Fabric, and Quilt.
- Minecraft `26.1.2` on Forge and Fabric.

Other loader/version pairs are not supported until implementation, builds, runtime behavior, documentation, and artifact naming all agree.

---

## Supported Releases

| Loader | GradleMC | Minecraft | Java | Artifact | Loader target / notes |
| --- | --- | --- | --- | --- | --- |
| Forge | `1.0.0` | `1.21.11` | `21` | `gradlemc-forge-1.21.11-1.0.0.jar` | Forge `61.1.8` |
| Fabric | `1.0.0` | `1.21.11` | `21` | `gradlemc-fabric-1.21.11-1.0.0.jar` | Fabric Loader `0.19.3`; Fabric API `0.141.4+1.21.11` |
| NeoForge | `1.0.0` | `1.21.11` | `21` | `gradlemc-neoforge-1.21.11-1.0.0.jar` | NeoForge `21.11.42` |
| Forge | `1.0.0` | `26.1.2` | `25` | `gradlemc-forge-26.1.2-1.0.0.jar` | Forge `26.1.2-64.0.11` |
| Fabric | `1.0.0` | `26.1.2` | `25` | `gradlemc-fabric-26.1.2-1.0.0.jar` | Fabric `26.1.2` release |
| Forge | `1.0.2` | `1.20.1` | `17` | `gradlemc-1.0.2-forge-1.20.1.jar` | Forge `47.4.20`; Quick Actions overlay hotfix |
| Fabric | `1.0.0` | `1.20.1` | `17` | `gradlemc-fabric-1.20.1-1.0.0.jar` | Fabric `1.20.1` release |
| Quilt | `1.0.0` | `1.20.1` | `17` | `gradlemc-quilt-1.20.1-1.0.0.jar` | Quilt `1.20.1` release |

| Field | Value |
| --- | --- |
| CurseForge project ID | `1585182` |
| License | Apache-2.0 |
| Telemetry | None |
| Cloud AI or LLM usage | None |

The `1.21.11` release line brings the GradleMC diagnostics surface to Forge, Fabric, and NeoForge with Java `21`. Use the jar that exactly matches the loader and Minecraft version; jar roulette is not troubleshooting.

---

## Quick Start

1. Pick a supported Minecraft and loader target from the table above.
2. Use Java `17` for `1.20.1`, Java `21` for `1.21.11`, or Java `25` for the released `26.1.2` builds.
3. Download the jar matching both your loader and Minecraft version.
4. Put the jar in the instance or server `mods` folder.
5. Install it on the client for the GUI, keybind, overlay, and client FPS sampling.
6. Install it on the server for commands, TPS/MSPT sampling, reports, issue bundles, passive worldgen observation, and Smart Diagnostics summaries.

Open the GUI:

```text
/gradlemc gui
```

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

Minecraft commands are lowercase. Use `/gradlemc`, not `/GradleMC`.

---

## Features

| Feature | What it does |
| --- | --- |
| Diagnostics GUI | In-game control center for checks, status, reports, and diagnostics. |
| Command tree | Lowercase `/gradlemc` commands for status, memory, checks, exports, and Smart Diagnostics. |
| Exportable reports | Local support evidence for cleaner troubleshooting. |
| Environment inspection | Minecraft, loader, Java, GradleMC, loaded-mod, config, and path information. |
| Memory diagnostics | JVM heap pressure and memory context. |
| Performance sampling | Bounded TPS/MSPT, FPS, entity density, block entity density, and worldgen pressure signals. |
| Local profiler foundation | Bounded tick, CPU-lite, memory-lite, and combined TXT/JSON summaries. |
| Smart Diagnostics | Local rule-based scoring, advice, evidence, confidence, trends, and missing-data notes. |
| Adaptive diagnostics | Lightweight local gameplay-state diagnostics without telemetry or cloud inference. |
| Issue-bundle exports | Reviewable support bundles for bug reports and pack support. |

Generated output is written under `<gameDir>/gradlemc/`, including `reports/`, `exports/`, `issue-bundles/`, `profiles/`, and `rules/`.

---

## Server Hosting

GradleMC is useful on servers because server support needs evidence, not guesses.

<p align="center">
  <a href="https://url-shortener.curseforge.com/kZ5IK">
    <img src="bisecthosting-banner.png" alt="Create a Minecraft server with BisectHosting" width="900">
  </a>
</p>

<p align="center">
  <a href="https://url-shortener.curseforge.com/kZ5IK"><strong>Create a Minecraft server</strong></a>
</p>

Use GradleMC server-side for `/gradlemc` commands, TPS/MSPT sampling, memory and environment reports, loaded-mod inspection, passive worldgen observation, issue-bundle exports, and Smart Diagnostics summaries.

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

## What GradleMC Is Not

- Not a Gradle replacement.
- Not a crash-fixing bot.
- Not Spark, VisualVM, or a deep-profiler replacement.
- Not an LLM, generative AI system, cloud AI service, telemetry feature, or analytics feature.
- Not a support claim for Bedrock or any loader/version pair absent from the supported-release table.

---

## Build From Source

Use the matching standalone project folder:

```text
GradleMC/Forge/Minecraft 1.21.11/
GradleMC/Fabric/Minecraft 1.21.11/
GradleMC/NeoForge/Minecraft 1.21.11/
GradleMC/Forge/Minecraft 26.1.2/
GradleMC/Fabric/Minecraft 26.1.2/
GradleMC/Forge/Minecraft 1.20.1/
GradleMC/Fabric/Minecraft 1.20.1/
GradleMC/Quilt/Minecraft 1.20.1/
```

Example:

```sh
cd "GradleMC/Forge/Minecraft 1.21.11"
./gradlew build
```

Windows users can run `gradlew.bat` from the same project folder. Use [`docs/RELEASE_CHECKLIST.md`](docs/RELEASE_CHECKLIST.md) before release/export work.

---

## Repository Layout

| Path | Purpose |
| --- | --- |
| [`GradleMC/`](GradleMC/) | Standalone source projects organized by loader and Minecraft version. |
| [`Releases/`](Releases/) | Published release artifacts organized by loader and Minecraft version. |
| [`Screenshots/`](Screenshots/) | README and documentation screenshots. |
| [`CHANGELOG.md`](CHANGELOG.md) | Release history. |
| [`ROADMAP.md`](ROADMAP.md) | Public planning and support gates. |
| [`SUPPORT.md`](SUPPORT.md) | Support guide. |
| [`SECURITY.md`](SECURITY.md) | Security policy. |
| [`CONTRIBUTING.md`](CONTRIBUTING.md) | Contribution rules. |
| [`AGENTS.md`](AGENTS.md) | Technical operating manual. |
| [`curseforge-description.html`](curseforge-description.html) | CurseForge description source. |

---

## Contributing

Before opening a PR, read [`CONTRIBUTING.md`](CONTRIBUTING.md) and [`AGENTS.md`](AGENTS.md). Keep Minecraft command examples lowercase. Do not claim loader or version support until implementation, builds, runtime checks, documentation, and artifact naming prove it.

---

## License

GradleMC is licensed under the [Apache License 2.0](LICENSE).
