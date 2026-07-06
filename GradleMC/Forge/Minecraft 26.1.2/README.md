# GradleMC Forge 26.1.2

This is the GradleMC Forge port for Minecraft Java Edition `26.1.2`.

## Release identity

| Field | Value |
| --- | --- |
| Loader | Forge |
| Minecraft version | `26.1.2` |
| Forge coordinate | `26.1.2-64.0.11` |
| GradleMC version | `1.0.0` |
| Required Java | `25` |
| Public artifact | `gradlemc-forge-26.1.2-1.0.0.jar` |

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
build/libs/gradlemc-forge-26.1.2-1.0.0.jar
```

## Runtime checks

Before publishing or replacing the jar, verify with the actual built artifact:

- client launch works with Java `25`;
- dedicated server launch works where applicable;
- `/gradlemc version` reports Minecraft `26.1.2`, Forge, Java `25`, and GradleMC `1.0.0`;
- `/gradlemc gui` opens from an in-game player;
- `/gradlemc status`, `/gradlemc memory`, `/gradlemc check`, and `/gradlemc export` work;
- GUI keybind, reports, profiling, Smart Diagnostics, and mod inspection behavior match the intended Forge port behavior as far as the `26.1.2` APIs allow.

## Tooling notes

- Uses `net.minecraftforge.gradle` with the Forge `26.1.2` / `64.x` setup.
- Uses Java `25` toolchain and Java compile release `25`.
- Uses Forge metadata through `META-INF/mods.toml`, Gradle property expansion, and jar manifest attributes.
- The jar filename is controlled by `artifact_name=gradlemc-forge-26.1.2-1.0.0.jar` in `gradle.properties`.

## Known limits

- This port preserves the GradleMC diagnostics, GUI, keybinds, reports, profiling, Smart Diagnostics, and mod inspection behavior as far as the `26.1.2` Forge APIs allow.
- Client launch should be run with Java `25`.
- This README does not claim NeoForge, Bedrock, Quilt `26.1.2`, or any unlisted target.
