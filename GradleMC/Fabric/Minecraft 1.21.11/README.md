# GradleMC — Fabric 1.21.11

This standalone project builds the public GradleMC `1.0.0` release for Minecraft Java Edition `1.21.11` on Fabric.

## Release Identity

| Field | Value |
| --- | --- |
| Minecraft | `1.21.11` |
| Loader | Fabric Loader `0.19.3` |
| Fabric API | `0.141.4+1.21.11` |
| GradleMC | `1.0.0` |
| Java | `21` |
| Public artifact | `gradlemc-fabric-1.21.11-1.0.0.jar` |

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
build/libs/gradlemc-fabric-1.21.11-1.0.0.jar
```

## Verification

- Confirm `/gradlemc version` reports Minecraft `1.21.11`, Fabric, GradleMC `1.0.0`, and Java `21` context.
- Verify the diagnostics GUI, keybind, commands, reports, performance tests, Smart Diagnostics, mod inspection, and worldgen observation.
- Test client-only behavior on the client and common/server behavior on a dedicated server.
- Do not rename another loader's jar and call it a Fabric build. File extensions are not magic spells.
