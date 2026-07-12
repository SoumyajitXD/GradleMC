# GradleMC — NeoForge 26.1.2

This standalone project builds the public GradleMC `1.0.0` release for Minecraft Java Edition `26.1.2` on NeoForge.

## Release Identity

| Field | Value |
| --- | --- |
| Minecraft | `26.1.2` |
| NeoForge | `26.1.2.78` |
| GradleMC | `1.0.0` |
| Java | `25` |
| Public artifact | `gradlemc-neoforge-26.1.2-1.0.0.jar` |

## Build

Use Java `25`, then run from this folder:

```powershell
.\gradlew.bat clean build
```

Linux/macOS:

```sh
./gradlew clean build
```

Expected output:

```text
build/libs/gradlemc-neoforge-26.1.2-1.0.0.jar
```

## Verification

- Confirm `/gradlemc version` reports Minecraft `26.1.2`, NeoForge, GradleMC `1.0.0`, and Java `25` context.
- Verify the diagnostics GUI, configurable keybind, lowercase commands, memory and environment inspection, reports, FPS and performance tests, Smart Diagnostics, adaptive diagnostics, mod inspection, stability-risk checks, and worldgen observation.
- Test client-only behavior on the client and common/server behavior on a dedicated server.
- Verify the built jar is named exactly `gradlemc-neoforge-26.1.2-1.0.0.jar` before publishing.

## Tooling Notes

- Uses NeoForge ModDevGradle `2.0.141`.
- Uses NeoForge `26.1.2.78`.
- Uses the Java `25` toolchain and Java compile release `25`.
- Uses `META-INF/neoforge.mods.toml`, Gradle property expansion, generated release identity, and jar manifest attributes.

## Support Boundary

NeoForge `26.1.2` is a released GradleMC target. Other NeoForge loader/version pairs remain unsupported until separately built, verified, documented, and published.
