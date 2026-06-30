# GradleMC

<p align="center">
  <img src="GradleMC_logo.png" width="180" alt="GradleMC logo">
</p>

<p align="center">
  <strong>In-game diagnostics, stability checks, and exportable troubleshooting reports for Minecraft Forge modpacks.</strong>
</p>

<p align="center">
  <a href="https://github.com/SoumyajitXD/GradleMC/actions/workflows/ci.yml"><img alt="CI" src="https://github.com/SoumyajitXD/GradleMC/actions/workflows/ci.yml/badge.svg"></a>
  <img alt="Minecraft 1.20.1" src="https://img.shields.io/badge/Minecraft-1.20.1-brightgreen">
  <img alt="Forge 47.4.20" src="https://img.shields.io/badge/Forge-47.4.20-orange">
  <img alt="Java 17" src="https://img.shields.io/badge/Java-17-blue">
  <img alt="License Apache--2.0" src="https://img.shields.io/badge/License-Apache--2.0-lightgrey">
  <img alt="Telemetry-none" src="https://img.shields.io/badge/Telemetry-none-success">
</p>

<p align="center">
  <a href="#quick-start"><strong>Quick Start</strong></a>
  · <a href="#features">Features</a>
  · <a href="#build-from-source">Build</a>
  · <a href="CHANGELOG.md">Changelog</a>
  · <a href="ROADMAP.md">Roadmap</a>
  · <a href="SUPPORT.md">Support</a>
</p>

GradleMC helps players, pack makers, server owners, and testers inspect a running modded Minecraft instance without leaving the game. It provides a diagnostics GUI, lowercase `/gradlemc` commands, bounded performance checks, local profiler foundations, Smart Diagnostics, adaptive diagnostics status, and shareable text reports.

> **Latest public release:** `1.0.1`  
> **Current supported build:** Minecraft Java Edition `1.20.1` on Forge `47.4.20`  
> **Release artifact:** `gradlemc-1.0.1-forge-1.20.1.jar`  
> **Java:** `17`  
> **Project ID:** `1585182`

---

## Preview

Screenshots should be captured from the real `1.0.1` release jar before they are added here. The screenshot plan lives in [`docs/SCREENSHOT_PLAN.md`](docs/SCREENSHOT_PLAN.md).

---

## Why GradleMC Exists

Troubleshooting modded Minecraft often turns into guesswork. GradleMC exists to produce local evidence that is easier to read, export, and share:

- version and environment details;
- memory pressure;
- loaded mod information;
- config/report path checks;
- entity and block entity density;
- TPS/MSPT, FPS, profiler, and worldgen observations;
- Smart Diagnostics recommendations and evidence;
- exportable reports for support conversations.

---

## Features

| Feature | What it does |
| --- | --- |
| Diagnostics GUI | Opens an in-game GradleMC control center with quick actions, tests, reports, settings, and status panels. |
| Lowercase commands | Uses the `/gradlemc` command tree. Minecraft command examples must stay lowercase. |
| Report export | Writes readable local troubleshooting reports under `<gameDir>/gradlemc/reports/`. |
| Mod/config inspection | Shows loaded mods, config paths, report paths, local rule checks, and environment details. |
| Bounded performance tests | Samples TPS/MSPT, FPS, entity density, block entity density, and passive worldgen pressure within configured limits. |
| Profiler foundation | Produces bounded local tick, CPU-lite, and memory-lite summaries. |
| Smart Diagnostics | Provides local rule-based scoring, advice, evidence, confidence, trends, and missing-data notes. |
| Adaptive diagnostics | Provides lightweight local gameplay-state diagnostics. It is not cloud AI or telemetry. |
| Privacy-aware exports | Avoids broad private-file scans and full-folder dumps by default. Review exports before sharing. |

---

## Quick Start

### Install

1. Install Minecraft Java Edition `1.20.1` with Forge `47.4.20`.
2. Use Java `17`.
3. Put `gradlemc-1.0.1-forge-1.20.1.jar` in the instance or server `mods` folder.
4. Install on the client for the GUI, keybind, overlay, and FPS testing.
5. Install on the server for server commands, reports, TPS/MSPT sampling, passive worldgen observation, scans, issue bundles, Smart Diagnostics, and adaptive diagnostics state.

### Open the GUI

```text
/gradlemc gui
```

The default GUI keybind is `G` and can be changed in Minecraft controls under the `GradleMC` category.

### Useful first commands

```text
/gradlemc status
/gradlemc version
/gradlemc memory
/gradlemc check
/gradlemc export
/gradlemc smart score
/gradlemc smart advice
```

Commands that inspect or export heavier information are permission-gated where appropriate.

---

## What GradleMC Checks

GradleMC focuses on stability context that is useful when a pack is slow, unstable, overloaded, or hard to support:

- Java, Forge, Minecraft, GradleMC, and loaded-mod environment details.
- JVM heap pressure.
- Loaded mod count, previews, and mod ID search.
- Report/config directory access.
- Top-level config-file sanity checks.
- Optional local risk rules from `<gameDir>/gradlemc/rules/gradlemc-rules.json`.
- Nearby entity and block entity density.
- Bounded server TPS/MSPT samples.
- Bounded profiler sessions with tick timelines, slow tick snapshots, Java CPU-lite stack sampling, memory/GC pressure signals, and TXT/JSON output.
- Passive chunk/worldgen pressure observations.
- Client-only FPS samples.
- Smart Diagnostics scoring, evidence, confidence, missing data notes, and local aggregate baselines.

Generated output is written under:

```text
<gameDir>/gradlemc/
```

Default subfolders include `reports/`, `exports/`, `issue-bundles/`, `profiles/`, and `rules/`.

---

## What It Is Not

GradleMC is deliberately honest about its limits:

- Not a Gradle replacement.
- Not a crash-fixing bot.
- Not a replacement for Spark or deeper profilers.
- Not an LLM, generative AI system, neural network, cloud AI service, online inference engine, telemetry feature, or analytics feature.
- Not a claim of Fabric, NeoForge, Quilt, or non-1.20.1 support in this release.

Unsupported ports belong on the roadmap until the code, build, runtime checks, docs, and artifact naming all agree.

---

## Build From Source

The actual mod project lives in [`SOURCE CODE/`](SOURCE%20CODE/). The repository root contains public-facing docs, license, GitHub configuration, and the logo.

```sh
cd "SOURCE CODE"
./gradlew build
```

On Windows:

```bat
cd "SOURCE CODE"
gradlew.bat build
```

The expected build output for the current public release is:

```text
SOURCE CODE/build/libs/gradlemc-1.0.1-forge-1.20.1.jar
```

Run the main verification gate:

```sh
cd "SOURCE CODE"
./gradlew checkAutomationTools validateVariantMatrix checkProjectIdentity checkCommandCasing checkFalseSupportClaims checkReleaseMetadata
```

Run CI-equivalent validation from PowerShell:

```powershell
cd "SOURCE CODE"
pwsh ./tools/pwsh/validate.ps1
```

For release/export flow, use [`docs/RELEASE_CHECKLIST.md`](docs/RELEASE_CHECKLIST.md).

---

## Repository Layout

| Path | Purpose |
| --- | --- |
| [`SOURCE CODE/`](SOURCE%20CODE/) | Main Forge mod source, Gradle build, automation, docs, and tests. |
| [`.github/workflows/ci.yml`](.github/workflows/ci.yml) | CI for repository sanity, Forge build, Python automation, Node tooling, and PowerShell wrappers. |
| [`README.md`](README.md) | Project landing page. |
| [`CHANGELOG.md`](CHANGELOG.md) | Release and repository-surface history. |
| [`ROADMAP.md`](ROADMAP.md) | Public planning and support gates. |
| [`SUPPORT.md`](SUPPORT.md) | Support guide. |
| [`SECURITY.md`](SECURITY.md) | Security and distribution-chain reporting policy. |
| [`CONTRIBUTING.md`](CONTRIBUTING.md) | Contribution rules and local verification flow. |
| [`CODE_OF_CONDUCT.md`](CODE_OF_CONDUCT.md) | Community behavior rules. |
| [`docs/RELEASE_CHECKLIST.md`](docs/RELEASE_CHECKLIST.md) | Release/export validation checklist. |
| [`docs/SCREENSHOT_PLAN.md`](docs/SCREENSHOT_PLAN.md) | Screenshot capture plan. |
| [`curseforge-description.html`](curseforge-description.html) | CurseForge project description source for the current public release. |
| [`AGENTS.md`](AGENTS.md) | Technical operating manual for coding agents and maintainers. |
| [`.github/PULL_REQUEST_TEMPLATE.md`](.github/PULL_REQUEST_TEMPLATE.md) | Pull request checklist. |
| [`.github/ISSUE_TEMPLATE/`](.github/ISSUE_TEMPLATE/) | Issue routing. |

Inside `SOURCE CODE/`, the variant matrix lives at:

```text
SOURCE CODE/config/gradlemc-variants.json
```

Current enabled variant:

| Minecraft | Loader | Java | Variant ID |
| --- | --- | --- | --- |
| `1.20.1` | Forge | 17 | `forge-1.20.1` |

Planned or experimental entries are roadmap metadata only. They must not produce placeholder jars or public support claims.

---

## Contributing

Good contributions are welcome. Start here:

- Read [`CONTRIBUTING.md`](CONTRIBUTING.md).
- Read [`AGENTS.md`](AGENTS.md) before code or automation changes.
- Keep Minecraft command examples lowercase.
- Do not edit loader/version support claims unless the implementation and artifact gates prove it.
- Do not add telemetry, cloud calls, LLMs, generative AI, or fake AI marketing.
- Run the relevant Gradle/Python/Node/PowerShell checks before opening a PR.

---

## License

GradleMC is licensed under the [Apache License 2.0](LICENSE).
