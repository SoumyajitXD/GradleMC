# Repository Guidelines

## Project Structure & Module Organization

This is GradleMC `1.0.3` for Minecraft `1.20.1` on Forge. Production Java lives in `src/main/java/com/soumyajit/gradlemc/`, organized by capability such as `command/`, `metrics/`, `modaudit/`, `profiler/`, `report/`, and `task/`. Resources, language strings, and Forge metadata live in `src/main/resources/`; keep generated resources in `src/generated/resources/`. Dependency-free self-tests live in `src/test/java/`. Build outputs under `build/` are generated and must not be edited.

## Build, Test, and Development Commands

Use PowerShell 7 with the included wrapper:

- `./gradlew.bat compileJava` — compile production code quickly.
- `./gradlew.bat gradlemcSelfTest` — run GradleMC's deterministic self-test harness.
- `./gradlew.bat clean build` — final full build, tests, and reobfuscated Forge jar.
- `./gradlew.bat runClient` or `./gradlew.bat runServer` — launch a development smoke-test environment when runtime verification is needed.

Do not use `--refresh-dependencies`, upgrade the wrapper, or run generated-data tasks unless the change specifically requires it. The release artifact is `build/libs/gradlemc-1.0.3-forge-1.20.1.jar`.

## Coding Style & Naming Conventions

Use UTF-8, four-space Java indentation, and conventional Java naming: `PascalCase` types, `camelCase` fields/methods, and lowercase package names. Keep commands lowercase (for example, `/gradlemc task graph`). Prefer small immutable records and focused classes over manager classes that combine discovery, execution, analysis, and reporting. Do not add client-only classes to common/server code paths.

## Testing Guidelines

Add deterministic self-tests alongside the relevant package using the `*SelfTest.java` naming pattern, then invoke them from `GradleMcSelfTest`. Cover task graph ordering, failure states, caching decisions, serialization, and safe filesystem behavior. Runtime diagnostics must remain bounded and must not depend on live network services.

## Diagnostics, Privacy, and Artifacts

Preserve local-only operation: no telemetry, uploads, ports, downloaded plugins, arbitrary shell execution, or automatic config/mod changes. Runtime evidence is not reusable cache data. Keep scans and reports under GradleMC-managed output paths, redact unsafe paths, and state limitations instead of inferring causation.

## Commit & Pull Request Guidelines

This checkout has no Git history available, so use focused Conventional Commit subjects such as `feat: add scan task graph` or `fix: preserve optional task failures`. Describe behavior, verification commands, and any GUI screenshots in pull requests. Never commit credentials, local run data, or generated build output.
