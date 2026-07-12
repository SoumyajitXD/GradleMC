# GradleMC — NeoForge 1.21.11

This standalone project builds the public GradleMC `1.0.0` release for Minecraft Java Edition `1.21.11` on NeoForge.

## Release Identity

| Field | Value |
| --- | --- |
| Minecraft | `1.21.11` |
| NeoForge | `21.11.42` |
| GradleMC | `1.0.0` |
| Java | `21` |
| Public artifact | `gradlemc-neoforge-1.21.11-1.0.0.jar` |

## Build

```powershell
.\gradlew.bat clean build
```

Linux/macOS:

```sh
./gradlew clean build
```

Expected output:

```text
build/libs/gradlemc-neoforge-1.21.11-1.0.0.jar
```

## Verification

- Confirm `/gradlemc version` reports Minecraft `1.21.11`, NeoForge, GradleMC `1.0.0`, and Java `21` context.
- Verify the diagnostics GUI, keybind, commands, reports, performance tests, Smart Diagnostics, mod inspection, and worldgen observation.
- Test client-only behavior on the client and common/server behavior on a dedicated server.
- NeoForge `1.21.11` is released; other NeoForge source candidates remain unsupported until separately verified and published.
