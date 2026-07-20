# GradleMC v1.0.1 for Fabric 1.20.1

GradleMC provides bounded, local diagnostics for Minecraft 1.20.1 modpacks running Fabric. It includes stability checks, FPS and server-tick sampling, passive world-generation observations, a Java sampling profiler, Fabric-native mod metadata inspection, deterministic workflows, and local reports.

## Build and development

Java 17 is the production target and runtime requirement. The checked-in Gradle wrapper is required; dependency versions are pinned for this Minecraft target.

```powershell
.\gradlew.bat build --offline
.\gradlew.bat runClient --offline
.\gradlew.bat runServer --offline
```

Use `test` for the JUnit suite, `gradlemcSelfTest` for the legacy deterministic harness, and `verifyCommonEnvironmentBoundary` for the common/client source check. The remapped development artifact is written to `build/libs/gradlemc-fabric-1.20.1-1.0.1.jar`. It is not a final release export.

## Scope and privacy

Diagnostics and reports are generated locally. GradleMC has no telemetry, analytics, cloud AI, hidden upload, or remote diagnostic service. Reports can still contain mod metadata and machine/runtime context; redaction is best-effort, so review every artifact before sharing it. See [docs/PRIVACY.md](docs/PRIVACY.md).

The Forge 1.20.1 v1.0.3 project is a behavioral reference for this port, not a package-layout template or runtime dependency. Current equivalence and intentional gaps are recorded in [docs/forge-1.0.3-parity.md](docs/forge-1.0.3-parity.md).

This project does not declare a stable external Java API. Internal packages and report schemas may change only with documented compatibility handling. GradleMC is not affiliated with or endorsed by Gradle, Mojang Studios, Microsoft, or the Fabric project. Minecraft is a trademark of Microsoft; Fabric is maintained by its respective project.
