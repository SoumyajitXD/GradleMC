# Changelog

This changelog tracks public GradleMC releases and repository-facing release changes. Public support claims must match source metadata, builds, runtime behavior, documentation, and artifact names.

---

## Current Public Releases

| Loader | GradleMC | Minecraft | Java | Expected artifact | Notes |
| --- | --- | --- | --- | --- | --- |
| Forge | `1.0.0` | `1.21.11` | `21` | `gradlemc-forge-1.21.11-1.0.0.jar` | Forge `61.1.8` |
| Fabric | `1.0.0` | `1.21.11` | `21` | `gradlemc-fabric-1.21.11-1.0.0.jar` | Fabric Loader `0.19.3`; Fabric API `0.141.4+1.21.11` |
| NeoForge | `1.0.0` | `1.21.11` | `21` | `gradlemc-neoforge-1.21.11-1.0.0.jar` | NeoForge `21.11.42` |
| Forge | `1.0.0` | `26.1.2` | `25` | `gradlemc-forge-26.1.2-1.0.0.jar` | Forge `26.1.2-64.0.11` |
| Fabric | `1.0.0` | `26.1.2` | `25` | `gradlemc-fabric-26.1.2-1.0.0.jar` | Fabric `26.1.2` release |
| NeoForge | `1.0.0` | `26.1.2` | `25` | `gradlemc-neoforge-26.1.2-1.0.0.jar` | NeoForge `26.1.2.78` |
| Forge | `1.0.2` | `1.20.1` | `17` | `gradlemc-1.0.2-forge-1.20.1.jar` | Quick Actions overlay hotfix |
| Fabric | `1.0.0` | `1.20.1` | `17` | `gradlemc-fabric-1.20.1-1.0.0.jar` | Fabric `1.20.1` release |
| Quilt | `1.0.0` | `1.20.1` | `17` | `gradlemc-quilt-1.20.1-1.0.0.jar` | Quilt `1.20.1` release |

| Field | Value |
| --- | --- |
| Project | GradleMC |
| CurseForge Project ID | `1585182` |
| Repository license | Apache-2.0 |

---

## `1.0.0` — Minecraft `1.21.11` Release Line

Published GradleMC for Minecraft Java Edition `1.21.11` on Forge, Fabric, and NeoForge.

### Forge `1.21.11`

- Release artifact: `gradlemc-forge-1.21.11-1.0.0.jar`.
- Forge target: `61.1.8`.
- Java target: `21`.
- Added the full GradleMC diagnostics GUI, command surface, performance sampling, memory and environment inspection, Smart Diagnostics, worldgen observation, and exportable troubleshooting reports for this Forge target.
- `/gradlemc version` identifies Minecraft `1.21.11`, Forge, and GradleMC `1.0.0`.

### Fabric `1.21.11`

- Release artifact: `gradlemc-fabric-1.21.11-1.0.0.jar`.
- Fabric Loader target: `0.19.3`.
- Fabric API target: `0.141.4+1.21.11`.
- Java target: `21`.
- Added the full GradleMC diagnostics GUI, command surface, performance sampling, memory and environment inspection, Smart Diagnostics, worldgen observation, and exportable troubleshooting reports for this Fabric target.
- `/gradlemc version` identifies Minecraft `1.21.11`, Fabric, and GradleMC `1.0.0`.

### NeoForge `1.21.11`

- Release artifact: `gradlemc-neoforge-1.21.11-1.0.0.jar`.
- NeoForge target: `21.11.42`.
- Java target: `21`.
- Added the full GradleMC diagnostics GUI, command surface, performance sampling, memory and environment inspection, Smart Diagnostics, worldgen observation, and exportable troubleshooting reports for this NeoForge target.
- `/gradlemc version` identifies Minecraft `1.21.11`, NeoForge, and GradleMC `1.0.0`.

### Documentation and publishing surface

- Updated `README.md`, `CHANGELOG.md`, `ROADMAP.md`, `SUPPORT.md`, `SECURITY.md`, release documentation, screenshot guidance, maintainer guidance, and `curseforge-description.html`.
- Removed obsolete claims that NeoForge was categorically unsupported.
- Added Java `21` guidance and exact artifact names for all three `1.21.11` releases.
- Kept Smart Diagnostics and adaptive diagnostics described accurately as local rule-based systems, not cloud AI or LLM integrations.

---

## `1.0.0` — Forge `26.1.2`

- Artifact: `gradlemc-forge-26.1.2-1.0.0.jar`.
- Forge target: `26.1.2-64.0.11`.
- Java target: `25`.
- Published the Forge GradleMC diagnostics surface for Minecraft `26.1.2`.

## `1.0.0` — Fabric `26.1.2`

- Artifact: `gradlemc-fabric-26.1.2-1.0.0.jar`.
- Java target: `25`.
- Published the Fabric GradleMC diagnostics surface for Minecraft `26.1.2`.

## `1.0.0` — NeoForge `26.1.2`

- Artifact: `gradlemc-neoforge-26.1.2-1.0.0.jar`.
- NeoForge target: `26.1.2.78`.
- Java target: `25`.
- Published the NeoForge GradleMC diagnostics surface for Minecraft `26.1.2`.
- Includes the diagnostics GUI, configurable keybind, lowercase `/gradlemc` command surface, FPS and performance testing, memory and environment inspection, mod inspection, stability-risk checks, Smart Diagnostics, adaptive diagnostics, worldgen observation, and exportable troubleshooting reports.
- `/gradlemc version` identifies Minecraft `26.1.2`, NeoForge, and GradleMC `1.0.0`.

## `1.0.0` — Quilt `1.20.1`

- Artifact: `gradlemc-quilt-1.20.1-1.0.0.jar`.
- Java target: `17`.
- Published the Quilt GradleMC diagnostics surface for Minecraft `1.20.1`.

## `1.0.2` — Forge `1.20.1`

- Artifact: `gradlemc-1.0.2-forge-1.20.1.jar`.
- Forge target: `47.4.20`.
- Java target: `17`.
- Fixed the Quick Actions tab overlay issue where lower controls or text could overlap.

## `1.0.0` — Fabric `1.20.1`

- Artifact: `gradlemc-fabric-1.20.1-1.0.0.jar`.
- Java target: `17`.
- Published the Fabric GradleMC diagnostics surface for Minecraft `1.20.1`.

## `1.0.1` — Forge `1.20.1`

- Artifact: `gradlemc-1.0.1-forge-1.20.1.jar`.
- Stabilization release preceding the `1.0.2` Quick Actions hotfix.

## `1.0.0` — Forge `1.20.1`

Initial public GradleMC release, including lowercase `/gradlemc` commands, the diagnostics GUI, memory and mod inspection, local reports, bounded TPS/MSPT and FPS sampling, passive worldgen observation, Smart Diagnostics, adaptive diagnostics, and issue-bundle export behavior.
