# GradleMC 1.0.1 Release Notes

GradleMC `1.0.1` is the current public release for Minecraft Java Edition `1.20.1` on Forge.

Release artifact:

```text
gradlemc-1.0.1-forge-1.20.1.jar
```

## Highlights

- Forge `1.20.1` support, targeting Forge `47.4.20`.
- Java `17` target.
- Read-only client GUI via `/gradlemc gui`.
- Configurable `Open GradleMC GUI` keybind.
- Local Adaptive Diagnostics rule-based system.
- Smart Diagnostics stability score, evidence, confidence, trend notes, missing-data notes, and advice.
- Lowercase `/gradlemc` command suite for checks, config, rules, reports, scans, performance, FPS, worldgen observation, profiler summaries, and issue bundles.
- FPS test reports with interpretation notes.
- Passive worldgen observation reports with interpretation notes.
- Safe report and issue-bundle export support.
- Loader-neutral public wording for Minecraft modpacks while this artifact remains Forge `1.20.1`.
- No telemetry, cloud services, generative AI, online inference, embeddings, external ML calls, or hidden network behavior.

## Known Limitations

- This release artifact targets Forge for Minecraft Java Edition `1.20.1`.
- Fabric, NeoForge, Quilt, Bedrock, and other Minecraft versions are not included in this artifact.
- Diagnostics are bounded support context, not benchmark certification or a full profiler replacement.
- Profiler output is useful local evidence, not Spark parity.
