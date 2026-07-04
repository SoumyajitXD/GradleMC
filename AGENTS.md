# AGENTS.md

## Purpose

This is the technical operating manual for coding agents and maintainers working on GradleMC. `README.md` is user/project-facing. This file is implementation-facing and should protect the repo from unsafe edits, stale paths, inaccurate support claims, branch mess, and command casing mistakes.

If instructions conflict, follow the active user task unless it would cause data loss, unsupported version migration, broken public docs, telemetry, fabricated features, or destructive repository changes.

---

## Project Summary

- Project: GradleMC.
- Product name: GradleMC.
- Purpose: in-game diagnostics and stability checking for Minecraft modpacks.
- Current public targets: Minecraft Java Edition `1.20.1` on Forge, Fabric, and Quilt.
- Current Forge public release line: `1.0.2`.
- Current Fabric public release line: `1.0.0`.
- Current Quilt public release line: `1.0.0`.
- Forge artifact: `gradlemc-1.0.2-forge-1.20.1.jar`.
- Fabric artifact: `gradlemc-fabric-1.20.1-1.0.0.jar`.
- Quilt artifact: `gradlemc-quilt-1.20.1-1.0.0.jar`.
- Java: `17`.
- Mod ID: `gradlemc`.
- Main package: `com.soumyajit.gradlemc`.
- Main mod class: `com.soumyajit.gradlemc.GradleMC`.
- License: Apache-2.0.

GradleMC includes lowercase `/gradlemc` commands, a client diagnostics GUI, a configurable GUI keybind, optional disabled-by-default stats overlay, local reports, local risk rules, Smart Diagnostics, bounded performance/FPS/worldgen diagnostics, profiler foundations, and adaptive diagnostics.

Forge `1.0.2` is a Forge `1.20.1` hotfix release for `gradlemc-1.0.2-forge-1.20.1.jar`. Its public release note is narrow: it fixes the Quick Actions tab overlay issue. Do not inflate it into a fake feature release. That is how docs become fan fiction with markdown.

Quilt `1.0.0` is a Quilt `1.20.1` release for `gradlemc-quilt-1.20.1-1.0.0.jar`. Treat it as a real public target, not a half-supported ghost target.

Smart Diagnostics and adaptive diagnostics are local rule-based/adaptive systems. They are not LLMs, generative AI, cloud AI, online inference, telemetry, analytics, or ChatGPT integrations.

---

## Current Repository Layout

| Path | Role |
| --- | --- |
| `GradleMC/Forge/Minecraft 1.20.1/` | Current standalone Forge `1.20.1` source project. |
| `GradleMC/Forge/Minecraft 1.20.1/src/main/java/` | Java runtime source. |
| `GradleMC/Forge/Minecraft 1.20.1/src/main/resources/` | Forge resources and mod metadata. |
| `GradleMC/Forge/Minecraft 1.20.1/gradle.properties` | Minecraft, Forge, mod, and artifact metadata for the local source build. |
| `GradleMC/Forge/Minecraft 1.20.1/build.gradle` | ForgeGradle build. |
| `GradleMC/Fabric/Minecraft 1.20.1/` | Current standalone Fabric `1.20.1` source project. |
| `GradleMC/Quilt/Minecraft 1.20.1/` | Current standalone Quilt `1.20.1` source project. |
| `Screenshots/` | Committed screenshot assets, currently `0.png` through `13.png`. |
| `docs/SCREENSHOTS.md` | Full screenshot gallery. |
| `docs/SCREENSHOT_PLAN.md` | Screenshot maintenance and capture rules. |
| `docs/RELEASE_CHECKLIST.md` | Release/export checklist. |
| `README.md` | Project landing page. |
| `ROADMAP.md` | Public support and future-work plan. |
| `curseforge-description.html` | CurseForge description source. |

Do not resurrect stale `SOURCE CODE/` paths. That folder was removed from the current repo surface. Use the standalone loader paths above unless the user explicitly moves the project again.

---

## Non-Negotiable Rules

- Do not use uppercase Minecraft command literals or command examples.
- Correct command root: `gradlemc`.
- Correct GUI command: `/gradlemc gui`.
- Display name `GradleMC` is fine for titles, labels, logs, and prose. It is not fine as a Minecraft command root.
- Preserve the `gradlemc` mod ID across source, metadata, resources, and docs.
- Forge `1.20.1`, Fabric `1.20.1`, and Quilt `1.20.1` are the current public loader targets.
- Do not imply NeoForge, Bedrock, or non-`1.20.1` support unless it is actually implemented, built, tested, documented, and named correctly.
- Do not create fake jars or placeholder support claims.
- Do not migrate Minecraft, Forge, Fabric, Quilt, build tools, Gradle wrapper, Java, mappings, or loader targets without explicit user instruction.
- Do not add LLMs, generative AI, cloud APIs, external ML systems, telemetry, analytics, or phone-home behavior.
- Do not add gameplay features during documentation, cleanup, release, or verification tasks.
- Do not use internet-heavy Gradle tasks unless the user explicitly allows them.
- Do not create branch sprawl. One active branch is enough unless the user asks otherwise.
- Focus on quality more than quantity. Small correct changes beat broad rewrites.

---

## Source Metadata Discipline

The public docs may describe the current public releases, while checked-in source projects have their own metadata files. Before building or publishing anything, read the loader-specific metadata.

For the currently documented Forge source project, read:

```text
GradleMC/Forge/Minecraft 1.20.1/gradle.properties
```

Check at least:

- `minecraft_version`
- loader version or loader metadata
- `mod_version`
- `loader_name`
- `variant_name`
- `artifact_name`

If source metadata, public docs, release notes, and artifact name disagree, stop and fix the release identity first. Version drift is not clever. It is a rake on the floor.

---

## Build And Verification Commands

Run commands from the relevant loader source folder.

Forge:

```sh
cd "GradleMC/Forge/Minecraft 1.20.1"
./gradlew clean build gradlemcSelfTest
```

Fabric:

```sh
cd "GradleMC/Fabric/Minecraft 1.20.1"
./gradlew build
```

Quilt:

```sh
cd "GradleMC/Quilt/Minecraft 1.20.1"
./gradlew build
```

Windows users can run `gradlew.bat` from the same project folders.

Rules:

- Run `./gradlew build` after Java/resource changes unless the user asks not to or the task is docs-only.
- Run `./gradlew gradlemcSelfTest` after diagnostics, scoring, path, report, or utility changes where that task exists for the loader.
- For Fabric or Quilt work, run the equivalent loader build and verification commands from that loader source project.
- Do not claim a build passed unless it was actually run and passed.
- Do not claim runtime testing happened unless `runClient`, `runServer`, or equivalent testing was actually performed.
- Do not run `--refresh-dependencies`, wrapper upgrades, dependency upgrades, or generated data tasks casually.
- Do not delete Gradle caches as a lazy fix.
- Do not suppress, hide, or hand-wave build errors.

Old automation task names from the removed `SOURCE CODE/` layout are not valid unless they are reintroduced in the current standalone project. Do not paste dead commands into docs.

---

## Screenshot Rules

Current committed screenshots live in:

```text
Screenshots/0.png
Screenshots/1.png
...
Screenshots/13.png
```

Rules:

- Keep README preview links short and high-signal.
- Keep the full gallery in `docs/SCREENSHOTS.md`.
- Keep capture and naming rules in `docs/SCREENSHOT_PLAN.md`.
- Use relative links only.
- Do not expose local paths, private server details, usernames that should not be public, or sensitive values.
- Do not let screenshots imply unsupported loaders or versions.
- If screenshots are renamed, update README and both screenshot docs in the same change.
- Prefer descriptive lowercase kebab-case names in future cleanup, but only with full reference updates.

---

## Command Rules

- All Brigadier command literals must be lowercase.
- Root literal must be `Commands.literal("gradlemc")`.
- GUI literal must be `Commands.literal("gui")`, producing `/gradlemc gui`.
- Player-facing examples in README, AGENTS, docs, lang files, chat output, comments, and support text must use lowercase `/gradlemc`.
- Keep command output short and actionable; write long details to reports.
- Validate numeric arguments with safe bounds.
- Handle console execution, missing player context, unsupported side, permission failures, and internal failures clearly.
- Heavy diagnostics must be opt-in and bounded.
- Search for uppercase command mistakes before finishing command-related work.

---

## GUI And Client Rules

- GUI code is client-only.
- Overlay code is client-only.
- Keybind code is client-only.
- FPS sampling is client-only.
- Do not import `net.minecraft.client.*` in common/server classes.
- Do not reference `Screen`, `GuiGraphics`, `Minecraft`, or `KeyMapping` from common/server command code.
- Use loader-safe client boundaries for client-only classloading.
- Opening the GUI from a server-side command must use a safe server-to-client packet flow.
- GUI-triggered server actions should respect the same permissions as typed commands.
- Escape must close custom screens.
- Provide an explicit Close button for custom screens.
- Do not add fake buttons, fake toggles, or controls that visually change settings without real sync, persistence, validation, and permission handling.
- Localize visible GUI strings in `assets/gradlemc/lang/en_us.json`.

---

## Branch And Git Discipline

Always inspect state before edits when working locally:

```sh
git status --short
git branch -a
git log --oneline --decorate --graph --all -n 20
```

Rules:

- Prefer the current branch, usually `main`, unless the user asks for a new branch.
- Work on one local branch. Do not create multiple branches for simple tasks.
- `origin/main` is normally a remote-tracking ref, not a duplicate local branch.
- Do not force-push.
- Do not run destructive reset/cleanup commands unless explicitly requested and understood.
- Never use `git reset --hard`, `git clean -fdx`, `git checkout .`, `git push --force`, or `git branch -D` as a casual fix.
- Preserve uncommitted user work. If files are dirty, read them and edit only what the task requires.
- Commit only when explicitly asked.
- Push only when explicitly asked.

---

## Release Rules

- Use `docs/RELEASE_CHECKLIST.md` before public release work.
- The jar filename must match the intended public release.
- Source metadata must agree with docs before publishing.
- Do not export release jars unless explicitly requested.
- Do not publish from a dirty mystery state.
- Do not claim support for a target until the code, build, runtime check, docs, screenshots, and artifact naming all agree.
- For Forge `1.0.2`, keep public wording honest: it is a Forge `1.20.1` hotfix for the Quick Actions tab overlay issue, not a broad compatibility or feature milestone.
- For Quilt `1.0.0`, keep public wording honest: it is a Quilt `1.20.1` release with artifact `gradlemc-quilt-1.20.1-1.0.0.jar`.

If something is uncertain, say it clearly. Guessing in release docs is how users become unpaid QA.
