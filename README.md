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
  <img alt="No telemetry" src="https://img.shields.io/badge/Telemetry-none-success">
</p>

<p align="center">
  <a href="#quick-start"><strong>Quick Start</strong></a>
  · <a href="#what-gradlemc-checks">Checks</a>
  · <a href="#build-from-source">Build</a>
  · <a href="CHANGELOG.md">Changelog</a>
  · <a href="ROADMAP.md">Roadmap</a>
  · <a href="SUPPORT.md">Support</a>
  · <a href="SECURITY.md">Security</a>
</p>

GradleMC helps players, pack makers, server owners, and testers inspect a running modpack **without leaving Minecraft**. It gives you a GUI, lowercase `/gradlemc` commands, bounded performance checks, local profiler foundations, Smart Diagnostics, adaptive diagnostics status, and shareable text reports.

Translation: fewer blind guesses, fewer ritual mod deletions, fewer support threads that look like a raccoon walked across a keyboard. 💀

> **Current supported build:** Minecraft Java Edition `1.20.1` on Forge.  
> **Release artifact name:** `gradlemc-1.0.0-forge-1.20.1.jar`  
> **Project ID:** `1585182`

---

## Preview

Screenshots are intentionally waiting until the `V1.0.1` visual state is final. Fake or outdated screenshots are worse than no screenshots; they are technical debt with eyeliner.

The planned screenshot pass is documented in [`docs/SCREENSHOT_PLAN.md`](docs/SCREENSHOT_PLAN.md). It will cover the GUI overview, status panel, Smart Diagnostics, report export flow, optional overlay, and possibly a short GIF.

---

## Why This Exists

Troubleshooting a modded Minecraft instance is usually chaos:

- “Is it memory pressure?”
- “Is the server tick dying?”
- “Which mods are loaded?”
- “Did worldgen just punch the TPS in the face?”
- “Can I export something readable instead of sending vague screenshots?”

GradleMC exists to turn that mess into local evidence you can actually share.

---

## Highlights

| Feature | What it does |
| --- | --- |
| **Diagnostics GUI** | Opens an in-game GradleMC control center with quick actions, tests, reports, settings, and status panels. |
| **Lowercase commands** | Uses the `/gradlemc` command tree. Minecraft commands should stay lowercase. |
| **Report export** | Writes readable local troubleshooting reports under `<gameDir>/gradlemc/reports/`. |
| **Mod/config inspection** | Shows loaded mods, config paths, report paths, local rule checks, and environment details. |
| **Bounded performance tests** | Samples TPS/MSPT, FPS, entity density, block entity density, and passive worldgen pressure within configured limits. |
| **Profiler foundation** | Produces bounded local tick/CPU-lite/memory-lite summaries. Useful evidence, not Spark parity cosplay. |
| **Smart Diagnostics** | Local rule-based scoring, advice, evidence, confidence, trends, and missing-data notes. |
| **Adaptive diagnostics** | Lightweight local gameplay-state logic for bounded adaptive status and warnings. No cloud nonsense. |
| **Privacy-aware exports** | Avoids broad private-file scans and full-folder dumps by default. Review exports before sharing anyway. |

---

## Project Quality Surface

GradleMC is set up like a real project, not a zip file with ambition.

| Surface | File |
| --- | --- |
| Release history | [`CHANGELOG.md`](CHANGELOG.md) |
| Public planning | [`ROADMAP.md`](ROADMAP.md) |
| Support policy | [`SUPPORT.md`](SUPPORT.md) |
| Security policy | [`SECURITY.md`](SECURITY.md) |
| Contribution rules | [`CONTRIBUTING.md`](CONTRIBUTING.md) |
| Code of conduct | [`CODE_OF_CONDUCT.md`](CODE_OF_CONDUCT.md) |
| Release checklist | [`docs/RELEASE_CHECKLIST.md`](docs/RELEASE_CHECKLIST.md) |
| Screenshot plan | [`docs/SCREENSHOT_PLAN.md`](docs/SCREENSHOT_PLAN.md) |
| Agent/maintainer manual | [`AGENTS.md`](AGENTS.md) |
| Pull request checklist | [`.github/PULL_REQUEST_TEMPLATE.md`](.github/PULL_REQUEST_TEMPLATE.md) |
| Issue routing | [`.github/ISSUE_TEMPLATE/`](.github/ISSUE_TEMPLATE/) |

---

## Quick Start

### Install

1. Install Minecraft Java Edition `1.20.1` with Forge `47.4.20`.
2. Use Java `17`.
3. Put `gradlemc-1.0.0-forge-1.20.1.jar` in the instance or server `mods` folder.
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

GradleMC focuses on stability context that is useful when a pack is slow, unstable, overloaded, or painful to support:

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
- Not a magic crash-fixing bot.
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

The expected build output is:

```text
SOURCE CODE/build/libs/gradlemc-1.0.0-forge-1.20.1.jar
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
| [`AGENTS.md`](AGENTS.md) | Technical operating manual for coding agents and maintainers. |
| [`CONTRIBUTING.md`](CONTRIBUTING.md) | Contribution rules and local verification flow. |
| [`SUPPORT.md`](SUPPORT.md) | How to ask for help without creating a cursed mystery novel. |
| [`SECURITY.md`](SECURITY.md) | Security and distribution-chain reporting policy. |
| [`CODE_OF_CONDUCT.md`](CODE_OF_CONDUCT.md) | Community behavior rules. |
| [`docs/RELEASE_CHECKLIST.md`](docs/RELEASE_CHECKLIST.md) | Release/export validation checklist. |
| [`docs/SCREENSHOT_PLAN.md`](docs/SCREENSHOT_PLAN.md) | Post-`V1.0.1` screenshot plan. |
| [`LICENSE`](LICENSE) | Apache License 2.0. |
| [`GradleMC_logo.png`](GradleMC_logo.png) | Repository/project logo. |

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

Good contributions are welcome. Random chaos is not. 🗿

Start here:

- Read [`CONTRIBUTING.md`](CONTRIBUTING.md).
- Read [`AGENTS.md`](AGENTS.md) before code or automation changes.
- Keep Minecraft command examples lowercase.
- Do not edit loader/version support claims unless the implementation and artifact gates prove it.
- Do not add telemetry, cloud calls, LLMs, generative AI, or fake “AI” marketing fog.
- Run the relevant Gradle/Python/Node/PowerShell checks before opening a PR.

Bug reports and feature requests have structured GitHub issue templates so reports include useful evidence instead of “it broke pls fix.”

---

## Star The Repo If It Helps

If GradleMC saves you from one doomed modpack debugging spiral, give the repo a star. ⭐

Stars help other Minecraft pack makers find the project. They are not magic, but discovery without stars is basically shouting into a cave and waiting for GitHub to develop empathy.

---

## License

GradleMC is licensed under the [Apache License 2.0](LICENSE).
