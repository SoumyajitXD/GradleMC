# GradleMC Fabric 26.1.2

This is the GradleMC Fabric port for Minecraft Java Edition `26.1.2`.

## Release identity

| Field | Value |
| --- | --- |
| Loader | Fabric |
| Minecraft version | `26.1.2` |
| GradleMC version | `1.0.0` |
| Required Java | `25` |
| Public artifact | `gradlemc-fabric-26.1.2-1.0.0.jar` |

## Build

Use Java `25`, then run from this folder:

```powershell
.\gradlew.bat clean build
```

On Unix-like shells:

```sh
./gradlew clean build
```

Expected output jar:

```text
build/libs/gradlemc-fabric-26.1.2-1.0.0.jar
```

## Runtime checks

Before publishing or replacing the jar, verify with the actual built artifact:

- client launch works with Java `25`;
- dedicated server launch works when applicable;
- `/gradlemc version` reports Minecraft `26.1.2`, Fabric, Java `25`, and GradleMC `1.0.0`;
- `/gradlemc gui` opens from an in-game player;
- `/gradlemc status`, `/gradlemc memory`, `/gradlemc check`, and `/gradlemc export` work;
- GUI keybind, reports, profiling, Smart Diagnostics, and mod inspection behavior match the intended Fabric port behavior as far as the `26.1.2` APIs allow.

## Tooling notes

- Uses `net.fabricmc.fabric-loom` with the `26.1.x` unobfuscated setup.
- Uses standard Gradle dependencies for Fabric Loader and Fabric API.
- Uses Java `25` source/target compatibility and Gradle `9.5.1`.
- Fabric Loader `0.19.3` is the selected loader for this port.
- Fabric API `0.154.0+26.1.2` is the selected Fabric API artifact for this port.

## Known limits

- This port preserves the GradleMC Fabric `1.20.1` diagnostics, GUI, keybinds, reports, profiling, and mod inspection behavior as far as the `26.1.2` APIs allow.
- Client launch should be run with Java `25`.
- This README does not claim NeoForge, Bedrock, Quilt `26.1.2`, or any unlisted target.
