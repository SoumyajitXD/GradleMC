# Contributing To GradleMC

Thanks for wanting to improve GradleMC. Focused, verifiable contributions are welcome.

The current Minecraft `1.20.1` line is GradleMC `v1.1.0` for both Forge and Fabric. Both active 1.20.1 implementations use **Kotlin** and Java `17` as the Minecraft runtime.

- Forge `1.20.1` `v1.1.0` requires **Kotlin for Forge**.
- Fabric `1.20.1` `v1.1.0` requires **Fabric API** and **Fabric Language Kotlin**.
- Quilt development is discontinued.

---

## Before You Change Anything

Read these first:

1. [`README.md`](README.md) for the public project promise.
2. [`AGENTS.md`](AGENTS.md) for technical repository rules.
3. [`docs/RELEASE_CHECKLIST.md`](docs/RELEASE_CHECKLIST.md) before release-facing changes.
4. [`CHANGELOG.md`](CHANGELOG.md) and [`ROADMAP.md`](ROADMAP.md) for release identity and loader policy.

Unsupported ports remain candidates until metadata, dependencies, builds, runtime checks, docs, and artifacts all agree.

---

## Current 1.20.1 Source

The checked-in Forge and Fabric `1.20.1` source trees are synchronized to GradleMC `v1.1.0`.

- Forge source lives under `GradleMC/Forge/Minecraft 1.20.1/` and uses Kotlin application source.
- Fabric source lives under `GradleMC/Fabric/Minecraft 1.20.1/` and uses Kotlin entrypoints.

Do not reintroduce the previous Java GradleMC implementation merely to recreate old architecture. A migration that preserves stale architecture for nostalgia is just archaeology with a build script.

---

## Non-Negotiable Rules

- Mod ID remains `gradlemc`.
- Minecraft command literals and examples remain lowercase.
- Correct GUI command: `/gradlemc gui`.
- Use Java `17` for Minecraft `1.20.1`, Java `21` for `1.21.11`, and Java `25` for the published `26.1.2` line.
- Forge `1.20.1` `v1.1.0` requires **Kotlin for Forge**.
- Fabric `1.20.1` `v1.1.0` requires **Fabric API** and **Fabric Language Kotlin**.
- Keep client-only code isolated from dedicated-server-safe code.
- Do not commit generated build output, run folders, logs, private files, or exported reports.
- Do not add telemetry, analytics, cloud AI, LLM integrations, or phone-home behavior.
- Do not create new Quilt releases by default.
- Prefer small, focused changes over architecture theatre.

---

## Current Public Artifacts

```text
gradlemc-1.1.0-forge-1.20.1.jar
gradlemc-1.1.0-fabric-1.20.1.jar
gradlemc-forge-1.21.11-1.0.0.jar
gradlemc-fabric-1.21.11-1.0.0.jar
gradlemc-neoforge-1.21.11-1.0.0.jar
gradlemc-forge-26.1.2-1.0.0.jar
gradlemc-fabric-26.1.2-1.0.0.jar
gradlemc-neoforge-26.1.2-1.0.0.jar
```

Legacy/discontinued Quilt artifact:

```text
gradlemc-quilt-1.20.1-1.0.0.jar
```

Do not “adapt” one artifact by renaming it. That is not porting.

---

## Local Setup

Checked-in source projects include:

```text
GradleMC/Forge/Minecraft 1.20.1/    # Kotlin v1.1.0
GradleMC/Fabric/Minecraft 1.20.1/   # Kotlin v1.1.0
GradleMC/Forge/Minecraft 1.21.11/
GradleMC/Fabric/Minecraft 1.21.11/
GradleMC/NeoForge/Minecraft 1.21.11/
GradleMC/Forge/Minecraft 26.1.2/
GradleMC/Fabric/Minecraft 26.1.2/
GradleMC/NeoForge/Minecraft 26.1.2/
GradleMC/Quilt/Minecraft 1.20.1/    # legacy/discontinued
```

Run builds from the matching project folder only after confirming the source metadata represents the release you intend to build.

On Windows, use `gradlew.bat`; on Unix-like environments, use `./gradlew`.

---

## Verification Checklist

For docs-only changes:

- verify release versions and artifact names;
- verify Forge `1.20.1` `v1.1.0` says Kotlin + Kotlin for Forge;
- verify Fabric `1.20.1` `v1.1.0` says Kotlin + Fabric API + Fabric Language Kotlin;
- verify Java `17` is still described as the Minecraft `1.20.1` runtime;
- verify Quilt is marked discontinued rather than active;
- verify commands remain lowercase.

For source/resource changes:

- build the exact target that changed;
- run available tests/self-tests;
- test client behavior when client code changed;
- test a dedicated server when common/server behavior changed;
- verify dependencies and packaging;
- never claim runtime testing that did not happen.

---

## Pull Request Expectations

A useful PR includes:

- a clear problem statement;
- a focused solution;
- the exact loader/Minecraft/release target;
- dependency changes, if any;
- commands/tests actually run;
- screenshots or clips for GUI changes;
- known limitations.

Avoid unrelated rewrites. Large diff size is not a quality metric.

---

## Screenshot Contributions

Current screenshots live in [`Screenshots/`](Screenshots/) and are documented in [`docs/SCREENSHOTS.md`](docs/SCREENSHOTS.md).

When adding or replacing screenshots:

- use a real supported build;
- identify the loader/version that produced them;
- avoid exposing local paths or sensitive information;
- do not use one loader's screenshot as proof of another loader's behavior;
- do not present legacy Quilt screenshots as evidence of current Quilt support.

---

## Issue Reports

Include:

- Minecraft version;
- loader and loader version;
- GradleMC version;
- exact GradleMC jar filename;
- Java version;
- required dependency versions;
- client/server environment;
- reproduction steps;
- expected and actual behavior;
- relevant reviewed report/log snippets.

Review logs and reports before posting. They can contain local paths, mod names, JVM details, and runtime context.
