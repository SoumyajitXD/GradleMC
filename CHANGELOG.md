# Changelog

This changelog tracks public GradleMC release and repository-surface changes.

GradleMC uses semantic versioning where practical. Public support claims must match the code, variant matrix, build, runtime behavior, docs, and artifact names. No fake ports. No placeholder jars. No marketing fog.

---

## Current Public Release Target

| Field | Value |
| --- | --- |
| Project | GradleMC |
| Current public version | `1.0.0` |
| Minecraft | `1.20.1` |
| Loader | Forge |
| Forge target | `47.4.20` |
| Java | `17` |
| CurseForge Project ID | `1585182` |
| Expected artifact | `gradlemc-1.0.0-forge-1.20.1.jar` |
| Repository license | Apache-2.0 |

---

## Unreleased

### Repository and community surface

- Added a security policy for fake downloads, sensitive logs, unsafe distribution, and report privacy.
- Added a roadmap that separates current support, next-release work, and future ports.
- Added a release checklist for repeatable release/export validation.
- Added a screenshot plan for the post-`V1.0.1` visual pass without faking screenshots early.
- Improved GitHub issue routing through contact links.
- Expanded the pull request template with stricter release, privacy, and validation checks.
- Improved the README repository map and public trust signals.

### Guardrails

- Current supported target remains Minecraft `1.20.1` on Forge.
- Fabric, NeoForge, Quilt, and other Minecraft versions remain future work until fully implemented and verified.
- Adaptive diagnostics remain local rule-based systems, not LLMs, generative AI, telemetry, or cloud inference.

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
