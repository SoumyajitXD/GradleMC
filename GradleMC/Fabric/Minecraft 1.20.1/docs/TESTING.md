# Testing

Use the checked-in wrapper from PowerShell. Do not refresh dependencies for normal validation.

```powershell
.\gradlew.bat compileJava compileClientJava processResources --offline --no-daemon
.\gradlew.bat test --offline --no-daemon
.\gradlew.bat gradlemcSelfTest --offline --no-daemon
.\gradlew.bat verifyCommonEnvironmentBoundary --offline --no-daemon
.\gradlew.bat build --offline --no-daemon
```

JUnit covers processed Fabric metadata, FPS/overlay behavior, config migration, duration policy, deterministic workflows/reports/history, request correlation, Fabric mod-audit models, and filesystem/privacy controls. The legacy `gradlemcSelfTest` harness exercises additional dependency-free behavior and intentionally emits one malformed-config fallback warning.

There is no Loader-JUnit or GameTest source set. `runServer` and `runClient` are Loom development runs, not production-artifact launchers. Dedicated-server development smoke testing should use a disposable directory under `build`; stop it gracefully and retain only validation evidence. Client validation must manually cover title screen, world join/leave, keybinds, GUI scale/resize/scroll, all overlay toggle combinations, FPS start/stop/cancel, and shutdown. Multiplayer validation must cover absent, compatible, incompatible, timed-out, and reconnecting servers.

After building, inspect the remapped JAR for metadata expansion, Java 17 bytecode, entrypoints, license/icon/language resources, and absence of tests, Forge metadata, local outputs, absolute paths, secrets, and placeholders. The artifact remains non-final until production-like client/server runtime gates pass.
