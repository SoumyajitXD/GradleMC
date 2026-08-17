# GradleMC V1.1.0 for Fabric 1.20.1

GradleMC is a local-first, privacy-first diagnostics tool for Minecraft 1.20.1 on Fabric. It provides bounded diagnostics, local reports and issue bundles, configuration and mod inspection helpers, deterministic Smart guidance, FPS/performance sampling, and a client diagnostics screen with an optional overlay.

This implementation is written in Kotlin. Fabric Language Kotlin is a required runtime dependency: [Fabric Language Kotlin](https://www.curseforge.com/minecraft/mc-mods/fabric-language-kotlin/preview).

## Build

Use Java 17 and the checked-in Gradle wrapper:

```powershell
.\gradlew.bat clean test build --no-daemon
```

The remapped production artifact is:

```text
build/libs/gradlemc-1.1.0-fabric-1.20.1.jar
```

The project declares Minecraft 1.20.1, Fabric Loader, Fabric API, and Fabric Language Kotlin through `gradle.properties` and `build.gradle.kts`.

## Privacy and scope

Diagnostics, reports, configuration, and issue bundles stay on the local machine. The implementation does not upload telemetry or diagnostic data. Generated reports can contain local Minecraft/mod metadata, so review them before sharing.

The source is split between server-safe common Kotlin code and client-only Kotlin code. The porting notes in [PORTING_STATUS.md](PORTING_STATUS.md) record current coverage and remaining runtime-test limitations.
