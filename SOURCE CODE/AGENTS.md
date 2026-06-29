# AGENTS.md

## Purpose

This file is the technical operating manual for coding agents and maintainers working on GradleMC. `README.md` is user/project-facing. `AGENTS.md` is implementation-facing and should protect the repository from unsafe edits, inaccurate documentation, branch churn, side-safety regressions, and command casing mistakes.

If instructions conflict, follow the active user task unless it would cause data loss, unsafe repository changes, unsupported version migration, telemetry, or fabricated feature claims. If the conflict cannot be resolved safely, stop and ask.

## Project Summary

- Project: GradleMC.
- Product name: GradleMC.
- Purpose: in-game diagnostics and stability checking for Minecraft modpacks.
- Target game: Minecraft Java Edition 1.20.1.
- Loader: Forge.
- Current supported target: Forge 1.20.1.
- Current variant ID: `forge-1.20.1`.
- Forge target in `gradle.properties`: `47.4.20`.
- Java: 17.
- Build system: Gradle with ForgeGradle 6.x.
- Variant source of truth: `config/gradlemc-variants.json`.
- Mod ID: `gradlemc`.
- Display name: `GradleMC`.
- Main package: `com.soumyajit.gradlemc`.
- Main mod class: `com.soumyajit.gradlemc.GradleMC`.
- License: Apache-2.0.

GradleMC currently includes lowercase `/gradlemc` commands, a client diagnostics control-center GUI, a configurable client GUI keybind, optional disabled-by-default stats overlay, Forge common config, local reports, local JSON risk rules, Smart Diagnostics, bounded performance/FPS/worldgen diagnostics, and adaptive diagnostics.

Adaptive diagnostics and Smart Diagnostics are local rule-based/adaptive systems. They are not LLMs, generative AI, cloud AI, online inference, neural networks, embeddings, telemetry, or ChatGPT integrations.

## Non-negotiable Rules

- Do not use uppercase Minecraft command literals or command examples.
- Loader/version belongs in variant metadata, not the product name.
- Do not write `GradleMC Forge 1.20.1` as the project name.
- Correct command root: `gradlemc`.
- Correct GUI command: `/gradlemc gui`.
- Do not write uppercase Minecraft command-root examples.
- Use lowercase `gradlemc` for the Brigadier root literal.
- Preserve the `gradlemc` mod id across `gradle.properties`, `mods.toml`, and `@Mod`.
- Do not migrate Minecraft, Forge, ForgeGradle, Gradle wrapper, Java, mappings, or mod-loader targets without explicit user instruction and a clear reason.
- Do not imply Fabric, NeoForge, or non-1.20.1 support unless it is actually implemented.
- Future ports must use variant naming like `forge-1.20.1`, `fabric-1.21.1`, and `neoforge-1.21.1`.
- Do not create a 30-variant matrix or enable the full loader/version cartesian product.
- Do not create fake Fabric, NeoForge, Quilt, or future-version jars.
- Do not enable or publish a variant unless the matrix, build, runtime checks, docs, and artifact naming all agree.
- Keep `config/gradlemc-variants.json` as the loader/version source of truth.
- Gradle is the main automation orchestrator. Do not bypass Gradle for normal build validation.
- Do not create language sprawl. Java, Gradle, Python, PowerShell, Node/TypeScript, and Kotlin build logic must each have a clear role.
- Do not duplicate validators across languages unless the duplication is an intentional cross-check and documented.
- Python owns manifest validation, matrix generation, docs table generation, command casing checks, and support-claim checks.
- Node.js/TypeScript owns docs and web-facing asset validation only; do not port the Python variant validator to TypeScript.
- Kotlin may be used only in Gradle/build automation helpers; do not add Kotlin to Minecraft runtime sources or the mod jar.
- PowerShell wrappers must stay thin and call Gradle or Python instead of reimplementing validation logic.
- Automation scripts must fail loudly and return nonzero on failed Gradle, Python, missing-file, missing-artifact, stale-path, metadata, or command-casing checks.
- Release/export automation must verify the expected jar exists before and after copy/export, then print the final path.
- Node.js or TypeScript may be added only with a concrete justification, minimal dependencies, and package scripts.
- Do not manually duplicate whole projects per loader/version. Extract common-core first, then add thin loader/version adapters.
- Follow the common-core-first rule before adding loader adapters.
- Use the Java toolchain required by the selected variant. Treat Java requirements as matrix data that must be refreshed per loader line: current Forge `1.20.1` uses Java 17; `1.21.x` candidates are Java 21 until verified; `26.x` candidates are Java 25 only where current loader docs require it.
- Keep client-only code isolated behind client adapters and `Dist.CLIENT` boundaries.
- Do not export release jars unless explicitly requested.
- Do not add gameplay features during documentation, bug-fix, cleanup, or verification tasks.
- Do not add LLMs, generative AI, cloud APIs, online inference, embeddings, neural networks, external ML libraries, telemetry, analytics, or phone-home behavior.
- Do not scan private files outside the game/modpack/config/report scope.
- Do not use internet-heavy Gradle tasks unless the user explicitly allows them.
- Do not create branch sprawl. One active local branch is enough.
- Focus on quality more than quantity. Small correct changes beat broad rewrites.
- Do not document nonexistent or planned features as finished.

## Repository Layout

- `src/main/java` - Java source.
- `src/main/resources` - Forge resources.
- `src/main/resources/META-INF/mods.toml` - Forge mod metadata template.
- `src/main/resources/pack.mcmeta` - resource pack metadata.
- `src/main/resources/GradleMC_logo.png` - mod-list logo referenced by `mods.toml`.
- `src/main/resources/assets/gradlemc/lang/en_us.json` - English localization.
- `build.gradle` - ForgeGradle project build.
- `gradle.properties` - Minecraft, Forge, mod, mapping, and publishing properties.
- `settings.gradle` - Gradle plugin repositories and toolchain resolver.
- `README.md` - user/project-facing page.
- `AGENTS.md` - this technical guide.
- `config/gradlemc-variants.json` - machine-readable variant matrix and artifact naming source of truth.
- `tools/python/gradlemc_automation/` - Python automation package for matrix, docs, claims, casing, and tool checks.
- `gradlemc_automation/__init__.py` - bootstrap so `python -m gradlemc_automation...` works from the repo root.
- `tools/python/gradlemc_automation/validate_release.py` - release metadata, stale path, command casing, required resource, and optional jar-content validator.
- `tools/pwsh/` - thin PowerShell 7 wrappers for local Windows-first automation commands, including release export.
- `package.json` and `tsconfig.json` - minimal Node/TypeScript tooling for docs and web-facing asset checks.
- `tools/node/` - TypeScript source and tiny JavaScript bootstrap for Node checks.
- `buildSrc/` - Kotlin build-logic helpers for Gradle-side variant artifact sanity checks only.
- `scripts/validate-variant-matrix.py` - compatibility wrapper for the Python package.
- `scripts/variant_matrix_tests.py` - compatibility wrapper for Python automation tests.
- `docs/AUTOMATION_PIPELINE.md` - local and CI automation design.
- `docs/PORTING_MATRIX.md` - multi-loader/multi-version release strategy and gates.
- `docs/COMMON_CORE_EXTRACTION.md` - extraction audit for common-core, bridge, adapter, client-only, and version-sensitive code.
- `.github/workflows/ci.yml` - CI skeleton for matrix validation, current release builds, Python tests, conditional Node checks, and PowerShell wrappers.
- `LICENSE` - Apache-2.0 license text.
- `curseforge-description` or `curseforge-description.html` - optional concise user-facing description; do not recreate it unless explicitly requested.
- `gradlemc/reports/` under the Minecraft game directory - runtime report output; do not commit.
- `gradlemc/exports/` under the Minecraft game directory - diagnostics exports; do not commit.
- `gradlemc/issue-bundles/` under the Minecraft game directory - generated support bundles; do not commit.
- `gradlemc/profiles/` under the Minecraft game directory - profiler TXT/JSON/JFR output; do not commit.
- `gradlemc/rules/gradlemc-rules.json` under the Minecraft game directory - optional local runtime rule file; do not assume it exists.
- `gradlemc/adaptive-baseline.properties` under the Minecraft game directory - local aggregate baseline data; do not commit.
- `config/gradlemc-common.toml` - Forge common config generated at runtime.
- `run/`, `run-data/`, `build/`, `.gradle/`, `logs/`, `.idea/` - local/generated folders; do not commit casually.
- `src/generated/resources/` - only for intentional data generation; do not commit casually.

## Current Architecture

- `GradleMC` registers the common config, networking, commands, adaptive diagnostics events, and bounded metric tick listeners.
- `command/GradleMcCommands` builds the `/gradlemc` Brigadier tree from `RegisterCommandsEvent`.
- `command/FpsTestCommandBridge` keeps common command code from directly loading client-only FPS classes.
- `client/ClientModEventHandler` registers client key mappings on the MOD bus with `Dist.CLIENT`.
- `client/ClientEventHandler` handles client ticks, FPS sampling, GUI keybind opens, overlay keybinds, and client bridge registration.
- `client/input/GradleMCKeyMappings` defines the `Open GradleMC GUI` keybind under the GradleMC category, defaulting to `G`; overlay keybinds must stay unbound by default with `InputConstants.UNKNOWN`.
- `client/gui/GradleMCScreen` renders the diagnostics control-center GUI, launches supported diagnostics through shared command/action paths, and closes through Escape or Close.
- `client/overlay/GradleMCStatsOverlay` renders the optional in-game stats overlay only on the physical client.
- `client/overlay/FpsRollingStatsCalculator` computes rolling frame-time FPS statistics for current/average/1% low/0.1% low.
- `network/GradleMCNetwork` owns the Forge `SimpleChannel` and packet registration.
- `network/OpenGradleMCGuiPacket` opens the GUI on the client through the bridge.
- `network/RequestSmartAIStatusPacket` asks the server to sync current player adaptive diagnostics state.
- `network/SyncSmartAIStatusPacket` sends bounded adaptive diagnostics status fields to the client.
- `ai/AdaptiveSmartAIManager` performs local server-side player-state scoring with cooldown-gated messages.
- `ai/AdaptiveRiskCalculator` keeps adaptive player pressure/risk scoring testable and separate from technical stability scoring.
- `config/GradleMCConfig` defines Forge common config using `ForgeConfigSpec`.
- `metrics/PerformanceTestManager` samples server TPS/MSPT only during active bounded sessions.
- `metrics/WorldgenObservationManager` passively observes loaded chunk/worldgen pressure only during active bounded sessions.
- `client/FpsTestManager` samples client FPS only during active bounded client sessions.
- `profiler/GradleMcProfilerService` owns profiler lifecycle, command-facing actions, bounded tick recording, CPU-lite sampling, memory/GC tracking, and report writing.
- `profiler/tick`, `profiler/sampling`, `profiler/memory`, and `profiler/report` contain profiler internals. Keep them common/server-safe unless explicitly under a client package.
- `check/`, `check/impl/`, `report/`, `rules/`, `smart/`, and `util/` contain reusable diagnostics, report writing, local rules, Smart Diagnostics, paths, and snapshots.

## Branch And Git Discipline

Always inspect state before edits:

```sh
git status --short
git branch -a
git log --oneline --decorate --graph --all -n 20
```

Rules:

- Prefer the current branch, usually `main`, unless the user asks for a new branch.
- Work on one local branch. Do not create multiple branches for simple tasks.
- `origin/main` is normally a remote-tracking ref, not a duplicate local branch.
- Do not panic-delete `origin/main`.
- Do not force-push.
- Do not run destructive reset/cleanup commands unless explicitly requested and understood.
- Never use `git reset --hard`, `git clean -fdx`, `git checkout .`, `git push --force`, or `git branch -D` as a casual fix.
- Preserve uncommitted user work. If files are dirty, read them and edit only what the task requires.
- Commit only when explicitly asked.
- Push only when explicitly asked.

## Build And Verification Commands

Allowed when appropriate:

```sh
./gradlew build
./gradlew checkToolchains
./gradlew checkAutomationTools
./gradlew printVariantMatrix
./gradlew validateVariantMatrix
./gradlew checkVariantMatrix
./gradlew checkProjectIdentity
./gradlew checkCommandCasing
./gradlew checkFalseSupportClaims
./gradlew checkReleaseMetadata
./gradlew checkNodeTooling
./gradlew checkKotlinBuildLogic
./gradlew printVariantSummaryKotlin
./gradlew buildVariant -PgradlemcVariant=forge-1.20.1
./gradlew buildEnabledVariants
./gradlew assembleVariantMatrix
./gradlew exportReleaseJar
./gradlew suggestNextPort
./gradlew printPortingPlan
./gradlew printVariantGaps
./gradlew generateGithubMatrix
./gradlew genIntellijRuns
```

Windows equivalents:

```bat
gradlew.bat build
gradlew.bat checkToolchains
gradlew.bat checkAutomationTools
gradlew.bat printVariantMatrix
gradlew.bat validateVariantMatrix
gradlew.bat checkVariantMatrix
gradlew.bat checkProjectIdentity
gradlew.bat checkCommandCasing
gradlew.bat checkFalseSupportClaims
gradlew.bat checkReleaseMetadata
gradlew.bat checkNodeTooling
gradlew.bat checkKotlinBuildLogic
gradlew.bat printVariantSummaryKotlin
gradlew.bat buildVariant "-PgradlemcVariant=forge-1.20.1"
gradlew.bat buildEnabledVariants
gradlew.bat assembleVariantMatrix
gradlew.bat exportReleaseJar
gradlew.bat suggestNextPort
gradlew.bat printPortingPlan
gradlew.bat printVariantGaps
gradlew.bat generateGithubMatrix
gradlew.bat genIntellijRuns
```

Rules:

- Run `./gradlew build` after Java/resource changes unless the user asks not to or the task is clearly docs-only.
- Run `./gradlew validateVariantMatrix` after changing `config/gradlemc-variants.json`, artifact naming, CI matrix logic, or support documentation.
- Run `./gradlew checkProjectIdentity checkCommandCasing checkFalseSupportClaims checkReleaseMetadata` after changing docs, commands, metadata, paths, release automation, or support claims.
- Run `python -m unittest discover -s tools/python/tests` after changing Python automation.
- `checkAutomationTools` requires Node.js and npm when `package.json` exists; do not make Node part of mod runtime or duplicate Python validation in Node.
- Do not require Node.js for Java/mod runtime logic.
- Kotlin build logic is allowed under `buildSrc/` only; do not migrate `build.gradle` to Kotlin DSL unless explicitly requested.
- Do not require Python 3.14-only features. Python 3.12 is the automation baseline.
- Run `./gradlew genIntellijRuns` only after setup/run-configuration changes or when the user requests IDE run generation.
- Do not run internet-heavy Gradle tasks unless the user explicitly allows them.
- Do not run `--refresh-dependencies`, wrapper upgrades, dependency upgrades, or generated data tasks casually.
- Do not delete Gradle caches.
- Do not suppress, hide, or hand-wave build errors.
- Do not claim a build passed unless it was actually run and passed.
- Do not claim runtime testing happened unless `runClient`, `runServer`, or equivalent runtime testing was actually performed.

## Command Rules

- All Brigadier command literals must be lowercase.
- Root literal must be `Commands.literal("gradlemc")`.
- GUI literal must be `Commands.literal("gui")`, producing `/gradlemc gui`.
- Player-facing examples in README, AGENTS, lang files, chat output, comments, and support text must use lowercase `/gradlemc`.
- Display name `GradleMC` is fine for titles, labels, logs, and prose. It is not fine as a Minecraft command root.
- Keep command output short and actionable; write long details to reports.
- Validate numeric arguments with safe bounds.
- Handle console execution, missing player context, unsupported side, permission failures, and internal failures clearly.
- Heavy diagnostics must be opt-in and bounded.
- GUI buttons and typed Brigadier commands should share the same underlying command/action logic where practical. Do not copy server diagnostics logic into client screens.
- Profiler GUI controls must call shared profiler commands/services. Do not duplicate profiler lifecycle or sampling logic inside GUI classes.
- Search for uppercase command mistakes before finishing command-related work.

## GUI And Client Rules

- GUI code is client-only.
- Overlay code is client-only.
- Keybind code is client-only.
- FPS sampling is client-only.
- Do not import `net.minecraft.client.*` in common/server classes.
- Do not reference `Screen`, `GuiGraphics`, `Minecraft`, or `KeyMapping` from common/server command code.
- Use `Dist.CLIENT` event subscribers or client bridge boundaries for client-only classloading.
- Opening the GUI from a server-side command must use a safe server-to-client packet flow.
- GUI-triggered server actions should use a safe client-to-server command/action path and respect the same permissions as typed commands. Do not inject fake visible chat messages to run commands.
- Escape must close custom screens.
- Provide an explicit Close button for custom screens.
- Do not add fake buttons, fake toggles, or controls that visually change settings without real sync, persistence, validation, and permission handling.
- Localize visible GUI strings in `assets/gradlemc/lang/en_us.json`.
- Keep the GUI scalable with wrapping, scroll support, and stable layout constants.
- Do not spam packets from render loops. Status refreshes must be explicit or throttled.
- Overlay rendering must stay lightweight: no heavy allocation every frame, no report scans every frame, no background training, and no render-loop packets.
- Overlay defaults must be conservative: disabled by default, compact mode by default, and all new overlay/stat keybinds unbound by default with `InputConstants.UNKNOWN`.
- Do not fake GPU usage. Showing OpenGL renderer/vendor/name metadata is acceptable; usage must be omitted or marked unavailable unless measured accurately and safely.
- Run `./gradlew build` after GUI/client changes.

## Networking Rules

- Use Forge 1.20.1-compatible networking.
- Keep packet IDs unique and stable within `GradleMCNetwork.register()`.
- Register packets in a deterministic order.
- Packet handlers must enqueue work correctly.
- Server-to-client packets may open GUI or sync bounded display state.
- Client-to-server packets must be validated. Do not trust client input for server state.
- Handle null sender for client-to-server packets.
- Bound string lengths and numeric fields during packet encode/decode.
- Do not send packets every render frame or every tick unless explicitly needed and throttled.
- Dedicated servers must not classload client-only GUI classes through packet handlers.

## Adaptive Diagnostics Rules

- Adaptive diagnostics are local rule-based adaptive gameplay logic.
- No LLMs.
- No generative AI.
- No cloud APIs.
- No online inference.
- No embeddings.
- No external ML libraries.
- No telemetry.
- Keep logic lightweight and explainable.
- Throttle tick handlers. Current sampling is bounded by `SAMPLE_INTERVAL_TICKS`.
- Clamp scores and counters.
- Respect cooldowns.
- Debug logging must be off by default.
- Avoid unfair event spam or repeated pressure after death.
- Keep configs safe and bounded.
- Do not store private data, chat, IPs, server addresses, access tokens, full logs, full configs, mod jars, or crash reports in adaptive baselines.
- Player-facing adaptive behavior must be optional/configurable and low impact unless explicitly designed otherwise.

## Smart Diagnostics Rules

- Smart Diagnostics are local scoring, thresholds, anomaly notes, trend notes, recommendations, and confidence explanations.
- Missing data lowers confidence; do not fabricate missing data.
- Store only small aggregate metrics, sample counts, and timestamps under `<gameDir>/gradlemc/adaptive-baseline.properties`.
- Only update adaptive baselines after explicit bounded diagnostics or scan commands.
- Do not perform background training.
- Use simple math and visible thresholds.
- Recommendations should include reason, action, confidence, priority, and evidence when evidence exists.
- `/gradlemc smart baseline reset confirm` may delete only the adaptive baseline file.

## Profiling Rules

- Profiling must be opt-in, bounded, and local/offline-first.
- Do not run a deep profiler in the background by default.
- Use bounded data structures such as ring buffers for tick timelines, slow tick snapshots, and stack aggregation.
- Do not add unbounded maps/lists to long-running profiler sessions.
- Do not fake metrics. If allocation profiling is not implemented with real allocation data, call the feature memory pressure, not allocation profiling.
- CPU-lite means Java-level stack sampling through safe Java APIs. It is not async-profiler and must not be described as native profiling.
- Do not integrate async-profiler, native agents, JVMTI binaries, or platform-specific profiler payloads without an explicit packaging, platform, signing/security, Java 17, Forge distribution, and license audit.
- Optional JFR must stay opt-in and must not be required for normal GradleMC usage.
- Thread sampling interval, target thread, all-thread sampling, and sleeping/waiting filtering must be configurable and bounded.
- Default sampling must be conservative. Do not default to high-frequency all-thread sampling.
- Slow tick attribution must use confidence language. Say possible contributor when evidence is weak; do not blame a mod from weak package similarity.
- Separate Minecraft/Forge internals, JVM/GC, GradleMC, third-party mods, and unknown sources in reports where possible.
- Profiler reports belong under `<gameDir>/gradlemc/profiles/`.
- Do not upload profiles, open external viewers, call cloud services, or phone home automatically.
- Do not copy Spark code, assets, viewer implementation, report format, names, or licensed material. Competitive research is allowed only to understand feature expectations.
- Do not inflate public claims. GradleMC must not claim to beat Spark, replace Spark, or be a Spark alternative until objective evidence and implemented features justify the wording.

## Config And Rule System Rules

- Use Forge's config system for GradleMC settings.
- Keep defaults safe and bounded.
- Do not auto-edit user configs unless a command explicitly asks, confirms, and provides backup behavior.
- Rule files should fail gracefully.
- Bad rule files should produce warnings, not crashes.
- Gson is already available in the Minecraft/Forge environment and is currently used; do not add JSON libraries casually.
- Rule checks should not run huge scans by default.
- Keep rule file paths inside `<gameDir>/gradlemc/rules/` as appropriate.

## Report And Check Rules

- Use existing `Severity`, `CheckCategory`, `CheckResult`, `CheckContext`, `StabilityCheck`, `BasicCheckRegistry`, `Report`, `ReportWriter`, and `GradleMcPaths` types.
- Checks should return warnings or failures instead of crashing.
- Report filenames must be timestamp-safe and collision-safe.
- Reports should avoid private data and must not scan arbitrary user files.
- Smart report sections must show stability score, risk level, confidence, evidence, recommendations, baseline summary, trend notes, and missing data notes without overstating certainty.
- Keep report writers independent of Minecraft client classes.
- Issue bundles must exclude full logs, crash reports, full config folders, mods folders, and private files by default.

## Resource And Localization Rules

- `en_us.json` must stay valid JSON.
- Use lowercase/safe resource paths.
- Localize player-facing text.
- Do not break `logoFile="GradleMC_logo.png"` in `mods.toml` unless the resource is moved and references are updated.
- Do not invent translations for nonexistent features.
- Keep command examples in localization lowercase.
- Preserve display name `GradleMC` where it is a title, label, or brand string.

## Documentation Rules

- `README.md` is for players, modpack makers, server owners, curious developers, and contributors.
- `AGENTS.md` is for technical implementation rules and agent safety.
- README should be polished and scannable, not an implementation dump.
- AGENTS should be practical, specific, and enforceable.
- Do not document nonexistent features as finished.
- Mark partial or planned features honestly.
- Keep command examples lowercase.
- Keep docs synced with actual code, configs, resources, and metadata.
- Keep documented project folder paths current after workspace renames.
- Do not add fake badges, fake release claims, fake compatibility claims, or unsupported platform claims.
- Matrix status controls public claims: only `enabled: true` plus `buildable: true` plus `status: "supported"` is supported.
- Planned, experimental, unsupported, needs-verification, and deprecated variants must be described as not supported unless they are promoted through the release gates in `docs/PORTING_MATRIX.md`.
- For docs-only changes, a Gradle build is usually unnecessary.

## Release Checklist

- Stay on the current branch unless the user explicitly asks for a branch.
- Do not create multiple local branches for automation cleanup. Treat `origin/main` as a remote-tracking ref, not a duplicate local branch.
- Confirm `gradle.properties`, `mods.toml`, and `@Mod` still use mod id `gradlemc`.
- Confirm `mod_version=1.0.0` for the v1.0.0 release.
- Confirm the release jar name is `gradlemc-1.0.0-forge-1.20.1.jar`.
- Run `./gradlew checkReleaseMetadata` before exporting a release jar.
- Run `gradlew.bat clean build` on Windows, or `./gradlew clean build` on Unix-like shells, after Java/resource release changes.
- Inspect the built jar for `META-INF/mods.toml`, `pack.mcmeta`, `assets/gradlemc/`, `GradleMC_logo.png`, and compiled GradleMC classes.
- Verify packaged metadata: mod id, version, license, authors, description, dependencies, and logo file.
- Export the verified jar to the requested publish handoff path only when asked. Prefer `./gradlew exportReleaseJar -PgradlemcExportDir=<path>` or `pwsh ./tools/pwsh/export-release.ps1 -OutputDir <path>`.
- Prepare commit, tag, and push commands separately; do not commit, tag, push, or upload without explicit user instruction.

## Search Checklist Before Finishing

Use targeted searches that match the task. For command or docs work, run:

```sh
rg -n "/[G]radleMC" README.md AGENTS.md src/main
rg -n "literal\\(\"GradleMC\"\\)" src/main/java
rg -n "[G]radleMC gui|[G]radleMC status|[G]radleMC ai|[G]radleMC help" README.md AGENTS.md src/main
```

For side-safety work, run:

```sh
rg -n "net\\.minecraft\\.client|Minecraft\\.getInstance|Screen|GuiGraphics|KeyMapping" src/main/java/com/soumyajit/gradlemc
```

Then verify every match is isolated to `client/` packages or a deliberate bridge boundary.

For localization or GUI work, also check:

```sh
rg -n "TODO|FIXME" src/main/java src/main/resources
rg -n "Component\\.literal|/gradlemc|/[G]radleMC" src/main/java src/main/resources/assets/gradlemc/lang/en_us.json
```

Legitimate uppercase `GradleMC` uses are allowed for display names, titles, log labels, class names, and file names. Uppercase command syntax is not allowed.

## Final Report Expectations

When finishing a task, report:

- Files changed.
- Commands and searches run.
- Build result, or why no build was run.
- Command casing audit result.
- Client/server side-safety notes when relevant.
- Project facts verified when docs were changed.
- Remaining issues, unverified claims, or user decisions needed.

Do not claim verification that was not performed.

## What Not To Do

- Do not turn GradleMC into a Gradle replacement.
- Do not clone a profiler badly.
- Do not clone a crash assistant badly.
- Do not pretend server TPS equals client FPS.
- Do not force-load chunks for diagnostics.
- Do not scan private files.
- Do not add telemetry.
- Do not spam logs, chat, packets, or tick handlers.
- Do not hide errors with empty catch blocks.
- Do not break dedicated servers for client features.
- Do not add dependencies casually.
- Do not use mixins unless the project is explicitly configured for them and there is a strong reason.
