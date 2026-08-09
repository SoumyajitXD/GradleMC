# GradleMC Forge 1.20.1

This module is GradleMC 1.1.0 for Minecraft 1.20.1 on Forge, implemented in
Kotlin. Keep production code in `src/main/kotlin` and tests in
`src/test/kotlin`; the small Java helper under `src/test/java` is test-only.

Use Java 17 and the included Gradle wrapper. Kotlin for Forge 4.12.0 is a
required runtime dependency and must remain Maven-resolved rather than
vendored. Keep `/gradlemc` commands lowercase and preserve client/server
separation. Never commit generated `build/`, `run/`, `.gradle/`, logs, caches,
or extracted dependency/reference directories.
