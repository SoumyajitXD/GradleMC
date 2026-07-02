# Changelog

This changelog tracks public GradleMC release and repository-surface changes.

GradleMC uses semantic versioning where practical. Public support claims must match the code, build, runtime behavior, docs, screenshots, and artifact names.

---

## Current Public Release

| Field | Value |
| --- | --- |
| Project | GradleMC |
| Current public version | `1.0.1` |
| Minecraft | `1.20.1` |
| Loader | Forge |
| Forge target | `47.4.20` |
| Java | `17` |
| CurseForge Project ID | `1585182` |
| Expected artifact | `gradlemc-1.0.1-forge-1.20.1.jar` |
| Repository license | Apache-2.0 |

---

## Unreleased

### Documentation

- Added real GitHub screenshot previews to `README.md` using the committed `Screenshots/` assets.
- Added `docs/SCREENSHOTS.md` as the full screenshot gallery.
- Updated `docs/SCREENSHOT_PLAN.md` from a future capture plan into a screenshot maintenance guide.
- Updated release, contribution, roadmap, PR-template, and agent-facing docs for the current standalone source path: `GradleMC/Forge/Minecraft 1.20.1/`.
- Removed stale `SOURCE CODE/` instructions from current contribution and build guidance.

---

## `1.0.1`

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
- Kept public support claims limited to Forge `1.20.1` only.

### Guardrails

- No Fabric, NeoForge, Quilt, or non-`1.20.1` support is claimed by this release.
- Smart Diagnostics and adaptive diagnostics remain local rule-based systems, not LLMs, generative AI, telemetry, analytics, or cloud inference.
- Profiler features remain bounded local evidence tools, not Spark parity.

---

## `1.0.0`

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

- Forge `1.20.1` is the only supported public target.
- Profiler features are local evidence tools, not Spark parity.
- Smart Diagnostics and adaptive diagnostics are rule-based and local.
- No telemetry, analytics, online inference, or cloud AI.
