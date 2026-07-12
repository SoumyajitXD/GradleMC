# AGENTS.md

## Purpose

This is the technical operating manual for coding agents and maintainers working on GradleMC. `README.md` is user-facing; this file protects the repository from stale paths, inaccurate support claims, destructive edits, branch mess, and command-casing mistakes.

If instructions conflict, follow the active user task unless it would cause data loss, fabricated features, broken release identity, telemetry, or destructive repository changes.

---

## Project Summary

- Project and product name: GradleMC.
- Purpose: in-game diagnostics and stability checking for Minecraft modpacks.
- Mod ID: `gradlemc`.
- Main package: `com.soumyajit.gradlemc`.
- License: Apache-2.0.
- CurseForge Project ID: `1585182`.
- No telemetry, analytics, cloud AI, LLM integration, or phone-home behavior.

GradleMC includes lowercase `/gradlemc` commands, a diagnostics GUI, configurable keybind, optional disabled-by-default overlay, local reports, local risk rules, Smart Diagnostics, bounded performance/FPS/worldgen diagnostics, profiler foundations, issue bundles, and adaptive diagnostics.

Smart Diagnostics and adaptive diagnostics are local rule-based/adaptive systems. They are not LLMs, generative AI, cloud AI, online inference, telemetry, or analytics.

---

## Current Public Releases

| Loader | GradleMC | Minecraft | Java | Artifact | Loader target / notes |
| --- | --- | --- | --- | --- | --- |
| Forge | `1.0.0` | `1.21.11` | `21` | `gradlemc-forge-1.21.11-1.0.0.jar` | Forge `61.1.8` |
| Fabric | `1.0.0` | `1.21.11` | `21` | `gradlemc-fabric-1.21.11-1.0.0.jar` | Fabric Loader `0.19.3`; Fabric API `0.141.4+1.21.11` |
| NeoForge | `1.0.0` | `1.21.11` | `21` | `gradlemc-neoforge-1.21.11-1.0.0.jar` | NeoForge `21.11.42` |
| Forge | `1.0.0` | `26.1.2` | `25` | `gradlemc-forge-26.1.2-1.0.0.jar` | Forge `26.1.2-64.0.11` |
| Fabric | `1.0.0` | `26.1.2` | `25` | `gradlemc-fabric-26.1.2-1.0.0.jar` | Fabric `26.1.2` release |
| Forge | `1.0.2` | `1.20.1` | `17` | `gradlemc-1.0.2-forge-1.20.1.jar` | Quick Actions overlay hotfix |
| Fabric | `1.0.0` | `1.20.1` | `17` | `gradlemc-fabric-1.20.1-1.0.0.jar` | Fabric `1.20.1` release |
| Quilt | `1.0.0` | `1.20.1` | `17` | `gradlemc-quilt-1.20.1-1.0.0.jar` | Quilt `1.20.1` release |

Treat NeoForge `1.21.11` as a real public target. Do not convert that fact into a broad claim that every NeoForge source folder or Minecraft version is released.

---

## Current Repository Layout

```text
GradleMC/Forge/Minecraft 1.21.11/
GradleMC/Fabric/Minecraft 1.21.11/
GradleMC/NeoForge/Minecraft 1.21.11/
GradleMC/Forge/Minecraft 26.1.2/
GradleMC/Fabric/Minecraft 26.1.2/
GradleMC/Forge/Minecraft 1.20.1/
GradleMC/Fabric/Minecraft 1.20.1/
GradleMC/Quilt/Minecraft 1.20.1/
Releases/
Screenshots/
docs/
```

Other source candidates may exist, including unreleased loader/version pairs. Source presence is not release status.

Do not resurrect stale `SOURCE CODE/` paths. Use the standalone loader/version projects unless the user explicitly changes the layout.

---

## Non-Negotiable Rules

- Command root: `gradlemc`.
- GUI command: `/gradlemc gui`.
- All Minecraft command literals and examples must be lowercase.
- Preserve the `gradlemc` mod ID across source, metadata, resources, and docs.
- Public support claims must exactly match the current release table.
- Do not create fake jars, renamed-loader jars, placeholder releases, or unsupported claims.
- Use Java `17` for `1.20.1`, Java `21` for `1.21.11`, and Java `25` for released `26.1.2` work.
- Keep client-only code isolated from common/server-safe code.
- Do not add LLMs, external ML systems, telemetry, analytics, or cloud APIs.
- Do not add gameplay features during docs, cleanup, release, or verification tasks.
- Do not use internet-heavy Gradle tasks unless explicitly allowed.
- Do not create branch sprawl. One active branch is normally enough.
- Small correct changes beat broad rewrites.

---

## Source Metadata Discipline

Before building or publishing, read the selected target's metadata files, especially:

- `gradle.properties`;
- `build.gradle`;
- loader metadata such as `mods.toml`, `neoforge.mods.toml`, or `fabric.mod.json`;
- jar manifest configuration.

Check at least:

- Minecraft version;
- loader name and version;
- GradleMC version;
- Java toolchain/release;
- variant identity;
- exact artifact name.

If source metadata, public docs, release notes, and artifact names disagree, stop and fix release identity first. Version drift is a rake on the floor.

---

## Build And Verification

Run commands from the relevant source project:

```sh
./gradlew clean build
```

On Windows, use `gradlew.bat`. Run `gradlemcSelfTest` where defined.

Rules:

- Run the build after Java/resource changes unless the task is docs-only or the user forbids it.
- Run available self-tests after diagnostics, scoring, path, report, or utility changes.
- Test the client after client-facing changes.
- Test a dedicated server after common/server changes.
- Never claim a build or runtime test passed unless it actually did.
- Do not casually run `--refresh-dependencies`, wrapper upgrades, dependency upgrades, or generated-data tasks.
- Do not delete Gradle caches as a lazy fix.
- Do not suppress or hand-wave build errors.

---

## Screenshot Rules

- Keep README preview links short and high-signal.
- Keep the full gallery in `docs/SCREENSHOTS.md`.
- Keep capture rules in `docs/SCREENSHOT_PLAN.md`.
- Use relative links.
- Do not expose sensitive paths, usernames, private server details, or secrets.
- Do not use one loader's screenshot as proof of another loader's runtime behavior.
- If files are renamed, update every reference in the same change.

---

## Command Rules

- Brigadier literals must be lowercase.
- Root literal must be `Commands.literal("gradlemc")`.
- GUI literal must produce `/gradlemc gui`.
- Keep command output short and actionable; put long details in reports.
- Validate numeric arguments with safe bounds.
- Handle console execution, missing player context, unsupported side, permission failures, and internal failures clearly.
- Heavy diagnostics must be opt-in and bounded.

---

## GUI And Client Rules

- GUI, overlay, keybind, and FPS sampling code are client-only.
- Do not import `net.minecraft.client.*` from common/server classes.
- Use loader-safe client boundaries.
- Server-triggered GUI opening must use safe server-to-client flow.
- GUI-triggered server actions must respect command permissions.
- Escape must close custom screens.
- Provide an explicit Close button.
- Do not add fake buttons or toggles without real behavior, sync, persistence, validation, and permission handling.
- Localize visible strings in `assets/gradlemc/lang/en_us.json`.

---

## Branch And Git Discipline

Before local edits, inspect:

```sh
git status --short
git branch -a
git log --oneline --decorate --graph --all -n 20
```

Rules:

- Prefer the current branch, usually `main`, unless the user requests another.
- Do not create multiple branches for a simple task.
- Do not force-push.
- Do not run destructive reset or cleanup commands without explicit authorization.
- Never delete user files or old source folders until the replacement is verified complete.
- Stage and commit only intended files.

---

## Release And Documentation Discipline

Before release-facing changes, update and cross-check:

- `README.md`;
- `CHANGELOG.md`;
- `ROADMAP.md`;
- `SUPPORT.md`;
- `SECURITY.md`;
- `CONTRIBUTING.md`;
- `docs/RELEASE_CHECKLIST.md`;
- screenshot docs;
- `curseforge-description.html`;
- release notes and artifact paths.

Do not let one stale sentence contradict an entire release matrix. Documentation drift is still a bug, just one wearing punctuation.
