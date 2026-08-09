# GradleMC Forge 1.20.1

GradleMC 1.1.0 for Minecraft Forge 1.20.1 is implemented in Kotlin and targets Java 17.

Kotlin for Forge is a required runtime dependency, declared as
`thedarkcolour:kotlinforforge:4.12.0` in `build.gradle`; it is resolved from its
Maven repository and is not vendored in this project.

Use the Gradle wrapper with Java 17:

```powershell
.\gradlew.bat compileKotlin
.\gradlew.bat test
.\gradlew.bat build
```

The release artifact is `gradlemc-1.1.0-forge-1.20.1.jar`. GradleMC is
local-first and privacy-first: it does not use telemetry or cloud AI. Its root
command is lowercase: `/gradlemc`.
