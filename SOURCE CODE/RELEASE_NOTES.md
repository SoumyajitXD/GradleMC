# GradleMC 1.0.0 Release Notes

Initial stable v1.0.0 release for Minecraft Java Edition 1.20.1 on Forge.

## Highlights

- Forge 1.20.1 support, tested against Forge 47.4.20.
- Read-only client GUI via `/gradlemc gui`.
- Configurable `Open GradleMC GUI` keybind.
- Local Adaptive Diagnostics rule-based system.
- Smart Diagnostics stability score, evidence, confidence, trend notes, missing-data notes, and advice.
- Lowercase `/gradlemc` command suite for checks, config, rules, reports, scans, performance, FPS, worldgen observation, and issue bundles.
- FPS test reports with interpretation notes.
- Passive worldgen observation reports with interpretation notes.
- Safe report and issue-bundle export support.
- Loader-neutral public wording for Minecraft modpacks while this artifact remains Forge 1.20.1.
- No telemetry, cloud services, generative AI, online inference, embeddings, external ML calls, or hidden network behavior.

## Known Limitations

- This release artifact targets Forge for Minecraft Java Edition 1.20.1.
- Fabric, NeoForge, Bedrock, and other Minecraft versions are not included in this artifact.
- Diagnostics are bounded support context, not benchmark certification or a full profiler replacement.
