# GradleMC Fabric 26.1.2

Target Minecraft version: 26.1.2

Loader: Fabric

Required Java: 25

Build command:

```powershell
.\gradlew.bat clean build
```

Output jar:

```text
build/libs/gradlemc-fabric-26.1.2-1.0.0.jar
```

Tooling notes:

- Uses `net.fabricmc.fabric-loom` with the 26.1.x unobfuscated setup.
- Uses standard Gradle `implementation` dependencies for Fabric Loader and Fabric API.
- Uses Java 25 source/target compatibility and Gradle 9.5.1.
- Fabric Loader 0.19.3 was selected because Fabric Meta reports it as the current stable loader compatible with Minecraft 26.1.2.
- Fabric API 0.154.0+26.1.2 was selected because it is a current 26.1.2-compatible artifact already present in the 26.1.2 starter metadata and resolves from Fabric Maven.

Known limitations:

- This port preserves the GradleMC Fabric 1.20.1 diagnostics, GUI, keybinds, reports, profiling, and mod inspection behavior as far as the 26.1.2 APIs allow.
- Client launch should be run with Java 25.
