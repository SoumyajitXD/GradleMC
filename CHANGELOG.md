# Changelog

This changelog tracks public GradleMC release and repository-surface changes.

GradleMC uses semantic versioning where practical. Public support claims must match the code, build, runtime behavior, docs, screenshots, and artifact names.

---

## Current Public Releases

| Loader | Current public version | Minecraft | Java | Expected artifact | Notes |
| --- | --- | --- | --- | --- | --- |
| Forge | `1.0.0` | `26.1.2` | `25` | `gradlemc-forge-26.1.2-1.0.0.jar` | Forge `26.1.2` release; Forge target `26.1.2-64.0.11` |
| Forge | `1.0.2` | `1.20.1` | `17` | `gradlemc-1.0.2-forge-1.20.1.jar` | Forge target `47.4.20`; Quick Actions overlay hotfix |
| Fabric | `1.0.0` | `26.1.2` | `25` | `gradlemc-fabric-26.1.2-1.0.0.jar` | Fabric `26.1.2` release |
| Fabric | `1.0.0` | `1.20.1` | `17` | `gradlemc-fabric-1.20.1-1.0.0.jar` | Fabric `1.20.1` release |
| Quilt | `1.0.0` | `1.20.1` | `17` | `gradlemc-quilt-1.20.1-1.0.0.jar` | Quilt `1.20.1` release |

| Field | Value |
| --- | --- |
| Project | GradleMC |
| CurseForge Project ID | `1585182` |
| Repository license | Apache-2.0 |

---

## Unreleased

### Documentation

- Kept screenshot, release, and contribution docs aligned with the current public release surface.

---

## `1.0.0` - Forge `26.1.2`

Published the Forge port for Minecraft Java Edition `26.1.2`.

### Release identity

- Release artifact: `gradlemc-forge-26.1.2-1.0.0.jar`.
- Supported target: Forge `26.1.2`.
- Forge coordinate: `26.1.2-64.0.11`.
- Java target: `25`.
- CurseForge Project ID remains `1585182`.

### Documentation and publishing surface

- Updated public GitHub docs so Forge `26.1.2` is no longer treated as an unsupported future target.
- Updated `README.md`, `CHANGELOG.md`, `ROADMAP.md`, `SUPPORT.md`, `SECURITY.md`, `CONTRIBUTING.md`, `AGENTS.md`, `docs/RELEASE_CHECKLIST.md`, `docs/SCREENSHOT_PLAN.md`, `.github/PULL_REQUEST_TEMPLATE.md`, the Forge `26.1.2` source README, and the repository CurseForge description source for the Forge `26.1.2` release.
- Kept support claims limited to the explicitly listed public release targets: Forge `1.20.1`, Forge `26.1.2`, Fabric `1.20.1`, Fabric `26.1.2`, and Quilt `1.20.1`.
- Kept NeoForge, Bedrock, and all unlisted loader/version pairs out of current support claims.

### Guardrails

- The Forge `26.1.2` release is a real public target, not a roadmap placeholder with a jar name taped to its forehead.
- Smart Diagnostics and adaptive diagnostics remain local rule-based systems, not LLMs, generative AI, telemetry, analytics, or cloud inference.
- Profiler features remain bounded local evidence tools, not Spark parity.

---

## `1.0.0` - Fabric `26.1.2`

Published the Fabric port for Minecraft Java Edition `26.1.2`.

### Release identity

- Release artifact: `gradlemc-fabric-26.1.2-1.0.0.jar`.
- Supported target: Fabric `26.1.2`.
- Java target: `25`.
- CurseForge Project ID remains `1585182`.

### Documentation and publishing surface

- Updated public GitHub docs so Fabric `26.1.2` is no longer treated as an unsupported future target.
- Updated `README.md`, `CHANGELOG.md`, `ROADMAP.md`, `SUPPORT.md`, `SECURITY.md`, `CONTRIBUTING.md`, `AGENTS.md`, screenshot guidance, release checklist docs, the Fabric `26.1.2` port README, and the repository CurseForge description source for the Fabric `26.1.2` release.
- Kept support claims limited to the explicitly listed public release targets: Forge `1.20.1`, Fabric `1.20.1`, Fabric `26.1.2`, and Quilt `1.20.1`.
- Kept NeoForge, Bedrock, and all unlisted loader/version pairs out of current support claims.

### Guardrails

- The Fabric `26.1.2` release is a real public target, not a roadmap placeholder.
- Smart Diagnostics and adaptive diagnostics remain local rule-based systems, not LLMs, generative AI, telemetry, analytics, or cloud inference.
- Profiler features remain bounded local evidence tools, not Spark parity.

---

## `1.0.0` - Quilt `1.20.1`

Published the Quilt port for Minecraft Java Edition `1.20.1`.

### Release identity

- Release artifact: `gradlemc-quilt-1.20.1-1.0.0.jar`.
- Supported target: Quilt `1.20.1`.
- Java target remains `17`.
- CurseForge Project ID remains `1585182`.

### Documentation and publishing surface

- Updated public GitHub docs so Quilt is no longer described as unsupported.
- Updated `README.md`, `CHANGELOG.md`, `ROADMAP.md`, `SUPPORT.md`, `SECURITY.md`, `CONTRIBUTING.md`, `AGENTS.md`, release checklist docs, PR template checks, screenshot guidance, issue contact text, and the repository CurseForge description source for the Quilt `1.20.1` release.
- Kept support claims limited to Minecraft `1.20.1` on Forge, Fabric, and Quilt only.
- Kept NeoForge, Bedrock, and future Minecraft versions out of current support claims.

### Guardrails

- The Quilt release is a real public target, not a roadmap placeholder.
- Smart Diagnostics and adaptive diagnostics remain local rule-based systems, not LLMs, generative AI, telemetry, analytics, or cloud inference.
- Profiler features remain bounded local evidence tools, not Spark parity.

---

## `1.0.2` - Forge `1.20.1`

Published public Forge hotfix release for Minecraft Java Edition `1.20.1`.

### Release identity

- Release artifact: `gradlemc-1.0.2-forge-1.20.1.jar`.
- Supported target remains Forge `1.20.1`.
- Java target remains `17`.
- Forge target remains `47.4.20`.
- CurseForge Project ID remains `1585182`.

### Fixed

- Fixed the Quick Actions tab overlay issue where lower controls/text could visually overlap instead of laying out cleanly.

### Documentation and publishing surface

- Updated GitHub release-facing docs for the `1.0.2` Forge public release.
- Updated `README.md`, `AGENTS.md`, `docs/RELEASE_CHECKLIST.md`, `ROADMAP.md`, and `curseforge-description.html` to point at `gradlemc-1.0.2-forge-1.20.1.jar`.
- Kept public support claims limited to Minecraft `1.20.1` on Forge and Fabric at that time.

### Guardrails

- No new loader, Minecraft version, telemetry, cloud AI, LLM, or generative AI support is claimed by this hotfix.
- Smart Diagnostics and adaptive diagnostics remain local rule-based systems, not LLMs, generative AI, telemetry, analytics, or cloud inference.
- Profiler features remain bounded local evidence tools, not Spark parity.

---

## `1.0.0` - Fabric `1.20.1`

Published the Fabric port for Minecraft Java Edition `1.20.1`.

### Release identity

- Release artifact: `gradlemc-fabric-1.20.1-1.0.0.jar`.
- Supported target: Fabric `1.20.1`.
- Java target remains `17`.
- CurseForge Project ID remains `1585182`.

### Documentation and publishing surface

- Updated public GitHub docs so Fabric is no longer described as unsupported.
- Kept support claims limited to Minecraft `1.20.1` on Forge and Fabric only at that time.
- Kept NeoForge, Quilt, Bedrock, and future Minecraft versions out of current support claims at that time.

### Guardrails

- The Fabric release is a real public target, not a roadmap placeholder.
- Smart Diagnostics and adaptive diagnostics remain local rule-based systems, not LLMs, generative AI, telemetry, analytics, or cloud inference.
- Profiler features remain bounded local evidence tools, not Spark parity.

---

## `1.0.1` - Forge `1.20.1`

Published public release for Minecraft Java Edition `1.20.1` on Forge.

### Release identity

- Release artifact: `gradlemc-1.0.1-forge-1.20.1.jar`.
- Supported target remains Forge `1.20.1`.
- Java target remains `17`.
- Forge target remains `47.4.20`.
- CurseForge Project ID remains `1585182`.

### Documentation and publishing surface

- Updated GitHub release-facing docs for the `1.0.1` public release.
- Added `curseforge-description.html` as the repository source for the CurseForge project description.
- Clarified that screenshots should be captured from the real `1.0.1` release jar before being added.
- Kept public support claims limited to Forge `1.20.1` only at the time of release.

### Guardrails

- No Fabric, NeoForge, Quilt, or non-`1.20.1` support was claimed by this release.
- Smart Diagnostics and adaptive diagnostics remain local rule-based systems, not LLMs, generative AI, telemetry, analytics, or cloud inference.
- Profiler features remain bounded local evidence tools, not Spark parity.

---

## `1.0.0` - Forge `1.20.1`

Initial public release target for Minecraft Java Edition `1.20.1` on Forge.

### Highlights

- Lowercase `/gradlemc` command tree.
- In-game diagnostics GUI opened with `/gradlemc gui` and a configurable keybind.
- Optional disabled-by-default stats overlay.
- Local report export under `<gameDir>/gradlemc/reports/`.
- JVM memory diagnostics.
- Loaded mod inspection.
- Config and report path checks.
- Local risk-rule checks.
- Entity and block entity density scans.
- Bounded TPS/MSPT sampling.
- Bounded FPS sampling.
- Passive worldgen pressure observation.
- Local profiler foundation with tick, CPU-lite, memory-lite, and combined modes.
- Smart Diagnostics scoring, advice, evidence, confidence, and missing-data notes.
- Adaptive diagnostics status based on local bounded gameplay-state signals.
- Safe issue-bundle export behavior.

### Known limits

- Forge `1.20.1` was the only supported public target at initial release.
- Profiler features are local evidence tools, not Spark parity.
- Smart Diagnostics and adaptive diagnostics are rule-based and local.
- No telemetry, analytics, online inference, or cloud AI.
