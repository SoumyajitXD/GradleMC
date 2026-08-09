# Changelog

This changelog tracks public GradleMC releases and release-facing repository changes. Public claims must match the actual artifacts and runtime requirements; checked-in source must not be silently treated as newer than it is.

---

## Current Release Matrix

| Status | Loader | GradleMC | Minecraft | Java | Dependency / notes | Artifact |
| --- | --- | --- | --- | --- | --- | --- |
| **Latest** | Forge | `1.1.0` | `1.20.1` | `17` | Kotlin implementation; **Kotlin for Forge required** | `gradlemc-1.1.0-forge-1.20.1.jar` |
| Published | Fabric | `1.0.0` | `1.20.1` | `17` | Fabric release | `gradlemc-fabric-1.20.1-1.0.0.jar` |
| Published | Forge | `1.0.0` | `1.21.11` | `21` | Forge `61.1.8` | `gradlemc-forge-1.21.11-1.0.0.jar` |
| Published | Fabric | `1.0.0` | `1.21.11` | `21` | Fabric Loader `0.19.3`; Fabric API `0.141.4+1.21.11` | `gradlemc-fabric-1.21.11-1.0.0.jar` |
| Published | NeoForge | `1.0.0` | `1.21.11` | `21` | NeoForge `21.11.42` | `gradlemc-neoforge-1.21.11-1.0.0.jar` |
| Published | Forge | `1.0.0` | `26.1.2` | `25` | Forge `26.1.2-64.0.11` | `gradlemc-forge-26.1.2-1.0.0.jar` |
| Published | Fabric | `1.0.0` | `26.1.2` | `25` | Fabric release | `gradlemc-fabric-26.1.2-1.0.0.jar` |
| Published | NeoForge | `1.0.0` | `26.1.2` | `25` | NeoForge `26.1.2.78` | `gradlemc-neoforge-26.1.2-1.0.0.jar` |
| **Discontinued** | Quilt | `1.0.0` | `1.20.1` | `17` | Legacy release; no future GradleMC Quilt updates | `gradlemc-quilt-1.20.1-1.0.0.jar` |

| Field | Value |
| --- | --- |
| Project | GradleMC |
| CurseForge Project ID | `1585182` |
| License | Apache-2.0 |
| Telemetry | None |
| Cloud AI / LLM usage | None |

---

## `1.1.0` — Forge `1.20.1`

Artifact: `gradlemc-1.1.0-forge-1.20.1.jar`

### Major internal change: Kotlin rebuild

- Rebuilt the Forge `1.20.1` GradleMC implementation in **Kotlin**.
- Removed the previous Java GradleMC implementation from the `v1.1.0` release codebase.
- Added **Kotlin for Forge** as a required runtime mod dependency.
- Kept Minecraft `1.20.1` on Java `17`; the Kotlin rewrite does not remove Minecraft/Forge's Java runtime requirement.
- Preserved GradleMC's local-only privacy model: no telemetry, analytics, cloud AI, LLM integration, or automatic report upload.

### Release identity

- GradleMC: `1.1.0`
- Minecraft: `1.20.1`
- Loader: Forge
- Java runtime: `17`
- Required dependency: Kotlin for Forge
- Public artifact: `gradlemc-1.1.0-forge-1.20.1.jar`

### Repository synchronization status

The Forge `1.20.1` source currently checked into `main` is still the legacy Java `v1.0.4` tree. Documentation has been updated to describe the published `v1.1.0` release, but the Kotlin source must be pushed separately before the repository can be considered source-synchronized for this release.

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
