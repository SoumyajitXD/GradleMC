# GradleMC Fabric 1.21.11

Target Minecraft version: 1.21.11

Loader: Fabric

Required Java: 21 or newer

Build command:

```powershell
.\gradlew.bat clean build
```

Output jar:

```text
build/libs/gradlemc-fabric-1.21.11-1.0.0.jar
```

Tooling notes:

- Uses `net.fabricmc.fabric-loom-remap` for the obfuscated Minecraft 1.21.11 line.
- Uses official Mojang mappings, matching the existing mature GradleMC Fabric source style.
- Uses Fabric Loader `0.19.3` and Fabric API `0.141.4+1.21.11`.
- Uses Java 21 source/target compatibility.

Known limitations:

- This port preserves the mature GradleMC Fabric diagnostics, GUI, keybinds, reports, profiling, and mod inspection behavior as far as the Minecraft 1.21.11 APIs allow.
- Client launch should be run with Java 21 or newer.
