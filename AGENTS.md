# AGENTS.md

## Purpose

This is the technical operating manual for coding agents and maintainers working on GradleMC. `README.md` is user-facing; this file protects the repository from stale paths, inaccurate support claims, destructive edits, branch mess, dependency omissions, and command-casing mistakes.

If instructions conflict, follow the active user task unless doing so would fabricate release state, cause data loss, introduce telemetry/cloud behavior, break release identity, or misrepresent unsupported loaders.

---

## Project Summary

- Project/product: GradleMC.
- Mod ID: `gradlemc`.
- Main package identity: `com.soumyajit.gradlemc`.
- License: Apache-2.0.
- CurseForge Project ID: `1585182`.
- Privacy model: no telemetry, analytics, cloud AI, LLM integration, automatic report upload, or phone-home diagnostics.

GradleMC provides lowercase `/gradlemc` commands, an in-game diagnostics GUI, configurable keybind, optional overlay, local reports, Smart Diagnostics, bounded performance diagnostics, and troubleshooting evidence.

Smart Diagnostics is local and rule-based. Do not describe it as generative AI, cloud inference, telemetry, or analytics.

---

## Current Release Identity

### Latest Forge `1.20.1`

| Field | Value |
| --- | --- |
| GradleMC | `1.1.0` |
| Minecraft | `1.20.1` |
| Loader | Forge |
| Java runtime | `17` |
| GradleMC implementation language | **Kotlin** |
| Required mod dependency | **Kotlin for Forge** |
| Artifact | `gradlemc-1.1.0-forge-1.20.1.jar` |

The previous Java GradleMC implementation was removed from the `v1.1.0` Forge `1.20.1` release codebase. This does **not** remove Minecraft/Forge's Java `17` runtime requirement.

### Other published lines

- Fabric `1.20.1` `v1.0.0`.
- Forge/Fabric/NeoForge `1.21.11` `v1.0.0` on Java `21`.
- Forge/Fabric/NeoForge `26.1.2` `v1.0.0` on Java `25`.
- Quilt `1.20.1` `v1.0.0` is **legacy/discontinued**.

### Quilt policy

No new GradleMC versions, fixes, or feature ports are planned for Quilt because the release line did not receive enough downloads to justify continued loader-specific maintenance.

Do not create, advertise, or roadmap new Quilt releases unless the project owner explicitly reverses this policy.

---

## Critical Repository Reality: Forge 1.20.1 Source Is Stale

The Forge `1.20.1` source currently checked into `main` is the legacy **Java `v1.0.4`** project. It is not the Kotlin `v1.1.0` source.

Until the Kotlin source is synchronized:

- never bump the old Java project to `1.1.0` merely to make metadata match docs;
- never claim `main` reproduces the published Kotlin `v1.1.0` artifact;
- never treat old Java files as the implementation source for Kotlin-release debugging;
- keep public documentation explicit about this source-sync gap;
- when the Kotlin source is eventually pushed, verify the migration as a real source replacement rather than a version-label edit.

Version drift is a rake on the floor. Source-language drift is the same rake with nails in it.

---

## Current Repository Layout

```text
GradleMC/Forge/Minecraft 1.21.11/
GradleMC/Fabric/Minecraft 1.21.11/
GradleMC/NeoForge/Minecraft 1.21.11/
GradleMC/Forge/Minecraft 26.1.2/
GradleMC/Fabric/Minecraft 26.1.2/
GradleMC/NeoForge/Minecraft 26.1.2/
GradleMC/Forge/Minecraft 1.20.1/   # legacy Java v1.0.4 until Kotlin source sync
GradleMC/Fabric/Minecraft 1.20.1/
GradleMC/Quilt/Minecraft 1.20.1/   # legacy/discontinued
Releases/
Screenshots/
docs/
```

Source presence is not current support status.

---

## Non-Negotiable Rules

- Command root: `gradlemc`.
- GUI command: `/gradlemc gui`.
- All Minecraft command literals/examples are lowercase.
- Preserve mod ID `gradlemc` across metadata/resources/code.
- Public release claims must match actual artifacts.
- Forge `1.20.1` `v1.1.0` must document Kotlin for Forge as required.
- Use Java `17` for Minecraft `1.20.1`, Java `21` for `1.21.11`, and Java `25` for published `26.1.2` work.
- Do not interpret "Java removed from GradleMC" as "Minecraft no longer needs Java".
- Do not reintroduce Java into the Kotlin Forge `1.20.1` implementation without a concrete technical requirement.
- Keep client-only code isolated from common/server-safe code.
- Do not add LLMs, telemetry, analytics, cloud APIs, or hidden remote diagnostics.
- Do not create fake jars, renamed-loader jars, placeholder releases, or unsupported claims.
- Do not start new Quilt work by default.
- Do not use internet-heavy Gradle tasks unless explicitly allowed.
- Do not create branch sprawl.
- Small correct changes beat broad rewrites.

---

## Kotlin Forge 1.20.1 Rules

When working on the synchronized Kotlin Forge `1.20.1` source:

- use Kotlin as the application implementation language;
- keep JVM bytecode/toolchain compatibility at Java `17` for Minecraft `1.20.1`;
- keep Kotlin for Forge dependency metadata explicit and valid;
- preserve safe Forge client/server boundaries;
- prefer straightforward Kotlin over unnecessary abstraction layers;
- ensure Kotlin/JVM dependency packaging does not accidentally duplicate or shadow Kotlin for Forge runtime components;
- verify dedicated-server loading after common/networking changes;
- verify client launch after GUI/keybind/overlay changes;
- never claim the dependency works merely because compilation succeeds.

---

## Source Metadata Discipline

Before building or publishing, inspect the exact target's:

- `gradle.properties`;
- build script(s);
- loader metadata (`mods.toml`, `neoforge.mods.toml`, `fabric.mod.json`, etc.);
- manifest configuration;
- Kotlin/Kotlin-for-Forge dependency declarations where applicable.

Check at least:

- Minecraft version;
- loader and loader version;
- GradleMC version;
- Java/JVM target;
- implementation language where release-facing;
- required dependencies;
- exact artifact name.

If metadata, source language, docs, release notes, and artifact identity disagree, stop and resolve the mismatch first.

---

## Build And Verification

Run commands from the exact relevant source project.

Typical build:

```sh
./gradlew clean build
```

On Windows, use `gradlew.bat`.

Rules:

- build after source/resource changes unless the task is docs-only or explicitly forbids it;
- run available self-tests after diagnostics/scoring/path/report changes;
- test the client after client-facing changes;
- test a dedicated server after common/server changes;
- verify required dependencies are actually present during runtime testing;
- never claim a test/build passed unless it ran and passed;
- do not casually run `--refresh-dependencies`, wrapper upgrades, dependency upgrades, or cache deletion.

---

## Command Rules

- Brigadier literals must be lowercase.
- Root literal must resolve to `gradlemc`.
- GUI path must produce `/gradlemc gui`.
- Keep command output short and actionable; put long detail in reports.
- Validate numeric arguments with safe bounds.
- Handle console execution, missing player context, unsupported side, permissions, and failures clearly.
- Heavy diagnostics must be opt-in and bounded.

---

## GUI And Client Rules

- GUI, overlay, keybind, and FPS sampling code are client-only.
- Do not import client-only Minecraft classes into common/server-safe code.
- Use loader-safe client boundaries.
- Server-triggered GUI opening must use a safe server-to-client path.
- GUI-triggered server actions must respect permissions.
- Escape should close custom screens.
- Provide an explicit Close button where appropriate.
- Do not add fake settings without real behavior, persistence, validation, and permission handling.

---

## Branch And Git Discipline

- Prefer the current intended branch, normally `main`, unless a different workflow is requested.
- Do not create multiple branches for a simple task.
- Do not force-push without explicit authorization.
- Do not use destructive reset/cleanup commands casually.
- Stage/commit only intended files.
- Do not delete legacy source merely to make documentation look cleaner; replace it only when the new source is actually available and verified.

---

## Release And Documentation Discipline

Before release-facing changes, cross-check:

- `README.md`;
- `CHANGELOG.md`;
- `ROADMAP.md`;
- `SUPPORT.md`;
- `SECURITY.md`;
- `CONTRIBUTING.md`;
- `AGENTS.md`;
- `docs/RELEASE_CHECKLIST.md`;
- `curseforge-description.html`;
- relevant screenshots/release notes/artifact paths.

For Forge `1.20.1` `v1.1.0`, every public surface should agree on these facts:

1. version `1.1.0`;
2. Minecraft `1.20.1`;
3. Forge;
4. Java `17` runtime;
5. Kotlin GradleMC implementation;
6. Kotlin for Forge required;
7. artifact `gradlemc-1.1.0-forge-1.20.1.jar`;
8. no new Quilt releases.

Do not let one stale sentence contradict the entire release story. Documentation drift is still a bug; it just wears punctuation.
