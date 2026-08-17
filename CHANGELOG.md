# Changelog

This changelog tracks public GradleMC releases and release-facing repository changes. Public claims must match actual source, dependencies, and artifacts.

---

## Current Release Matrix

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
| **Discontinued** | Quilt | `1.0.0` | `1.20.1` | `17` | Legacy only; no future GradleMC Quilt updates | `gradlemc-quilt-1.20.1-1.0.0.jar` |

| Field | Value |
| --- | --- |
| Project | GradleMC |
| CurseForge Project ID | `1585182` |
| License | Apache-2.0 |
| Telemetry | None |
| Cloud AI / LLM usage | None |

---

## `1.1.0` — Minecraft `1.20.1`

GradleMC `v1.1.0` is the current Kotlin-based 1.20.1 release line for both Forge and Fabric.

### Forge

- Artifact: `gradlemc-1.1.0-forge-1.20.1.jar`.
- Loader: Forge.
- Java runtime: `17`.
- GradleMC implementation: **Kotlin**.
- Required dependency: **Kotlin for Forge**.
- Replaced the previous Java GradleMC implementation for the active Forge 1.20.1 line.

### Fabric

- Artifact: `gradlemc-1.1.0-fabric-1.20.1.jar`.
- Loader: Fabric.
- Java runtime: `17`.
- GradleMC implementation: **Kotlin**.
- Required dependencies: **Fabric API** and **Fabric Language Kotlin**.
- Fabric metadata uses Kotlin entrypoints and declares `fabric-language-kotlin` as a runtime dependency.

### Shared v1.1.0 direction

- Expanded and refined the diagnostics GUI and runtime visibility.
- Improved local Smart Diagnostics and diagnostic guidance.
- Improved environment, JVM, memory, and modded-runtime checks.
- Improved bounded FPS/performance testing and runtime summaries.
- Improved local report/export workflows and overlay/runtime information.
- Hardened error handling and general stability.
- Preserved the privacy model: no telemetry, analytics, cloud AI, LLM integration, or automatic report upload.
- Minecraft `1.20.1` still requires Java `17`; Kotlin does not replace the JVM runtime.

### Repository synchronization

The checked-in Forge and Fabric `1.20.1` source trees now identify as GradleMC `1.1.0`. The Forge source is under `src/main/kotlin`, and the Fabric project declares Kotlin entrypoints and Fabric Language Kotlin.

---

## Quilt Release Line — Discontinued

GradleMC will not receive new Quilt releases.

The existing Quilt `1.20.1` `v1.0.0` artifact may remain available as a legacy download, but the loader line is discontinued because its download volume did not justify continued implementation, testing, compatibility, and maintenance work.

No future GradleMC features, fixes, or version releases should be promised for Quilt.

---

## `1.0.4` — Forge `1.20.1`

- Artifact: `gradlemc-1.0.4-forge-1.20.1.jar`.
- Java implementation.
- Improved FPS measurement consistency.
- Added independent controls for current FPS, average FPS, and overlay branding.
- Made the GradleMC overlay label optional and disabled by default.
- Fixed Quick Actions layout problems.
- Reduced duplicated FPS/render measurement work and improved runtime sampling behavior.

## `1.0.2` — Forge `1.20.1`

- Artifact: `gradlemc-1.0.2-forge-1.20.1.jar`.
- Fixed Quick Actions content overlap.

## `1.0.1` — Forge `1.20.1`

- Artifact: `gradlemc-1.0.1-forge-1.20.1.jar`.
- Stabilization release preceding later Forge `1.20.1` fixes.

## `1.0.0` — Forge `1.20.1`

Initial public Forge `1.20.1` release with lowercase `/gradlemc` commands, diagnostics GUI, memory/mod inspection, local reports, bounded performance sampling, Smart Diagnostics, and troubleshooting exports.

## `1.0.1` — Fabric `1.20.1`

- Artifact: `gradlemc-fabric-1.20.1-1.0.1.jar`.
- Intermediate Fabric `1.20.1` release preceding the Kotlin `v1.1.0` line.

## `1.0.0` — Fabric `1.20.1`

- Artifact: `gradlemc-fabric-1.20.1-1.0.0.jar`.
- Java target: `17`.

## `1.0.0` — Quilt `1.20.1`

- Artifact: `gradlemc-quilt-1.20.1-1.0.0.jar`.
- Java target: `17`.
- **Legacy/discontinued:** no new GradleMC versions are planned for Quilt.

---

## `1.0.0` — Minecraft `1.21.11`

Published for Forge, Fabric, and NeoForge with Java `21`.

- Forge: `gradlemc-forge-1.21.11-1.0.0.jar` — Forge `61.1.8`.
- Fabric: `gradlemc-fabric-1.21.11-1.0.0.jar` — Fabric Loader `0.19.3`, Fabric API `0.141.4+1.21.11`.
- NeoForge: `gradlemc-neoforge-1.21.11-1.0.0.jar` — NeoForge `21.11.42`.

---

## `1.0.0` — Minecraft `26.1.2`

Published for Forge, Fabric, and NeoForge with Java `25`.

- Forge: `gradlemc-forge-26.1.2-1.0.0.jar` — Forge `26.1.2-64.0.11`.
- Fabric: `gradlemc-fabric-26.1.2-1.0.0.jar`.
- NeoForge: `gradlemc-neoforge-26.1.2-1.0.0.jar` — NeoForge `26.1.2.78`.
