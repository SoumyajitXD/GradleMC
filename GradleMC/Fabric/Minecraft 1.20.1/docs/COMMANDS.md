# Commands

Commands are registered under `/gradlemc`. Permission level 2 is required for checks, configuration/rules operations, workflow execution/report writing, server sampling/profiling, filesystem/bundle exports, entity scans, smart diagnostics, adaptive reset, and mod-audit export. Read-only status, help, version, memory, report listing, mod metadata inspection, GUI opening, and client FPS requests do not add that requirement. Server policy may impose additional restrictions.

## General and workflows

- `/gradlemc [help]`, `status`, `version`, `memory`, `gui`
- `/gradlemc tasks`; `/gradlemc task graph <task>`
- `/gradlemc workflows`
- `/gradlemc workflow describe|dryrun|run <workflow>`
- `/gradlemc workflow status|cancel|latest|report`

Workflow IDs are `quick`, `lag`, `fps`, `worldgen`, `memory`, and `modpack_release`. Only one workflow runs at a time. Environment-specific evidence becomes unavailable when its owning client/world/player snapshot is absent. Cancellation preserves completed evidence; report writing requires a completed inactive workflow.

## Diagnostics

- `/gradlemc check`
- `/gradlemc perf start <seconds>` or compatibility alias `/gradlemc perf <seconds>`; `/gradlemc perf stop`
- `/gradlemc worldgen start <seconds>`; `stop`, `status`, or `latest`
- `/gradlemc testfps start <seconds>`; `stop`
- `/gradlemc profiler start [--timeout <seconds> --interval <milliseconds> --thread <name|pattern|*> --only-ticks-over <milliseconds> --include-sleeping <true|false> --mode <tick|cpu-lite|memory-lite|combined>]`
- `/gradlemc profiler stop|cancel|status|latest|summary|open|export`
- `/gradlemc entities [radius]`; `/gradlemc blockentities [radius]`

Performance and FPS durations are 5 seconds through their configured maximum; world-generation duration starts at 10 seconds. All configured maxima are clamped to 1,800 seconds. Entity radii are accepted from 8 through 512 and are further constrained by configuration. Profiler timeout is 5–1,800 seconds, interval is 4–1,000 ms, and slow-tick thresholds are bounded to 50–5,000 ms when enabled.

FPS commands require a physical client and an active client handler. Server sampling requires a server command context. World-generation observation is passive: it does not teleport players, force chunks, or scan save files.

## Metadata, reports, and local rules

- `/gradlemc mods list|count|search <modid>|inspect <modid>|audit|export`
- `/gradlemc reports list|latest`; `/gradlemc export`
- `/gradlemc issuebundle create`; `/gradlemc files`
- `/gradlemc config path|files|check`
- `/gradlemc rules path|example|reload|check`
- `/gradlemc ai status|reset`
- `/gradlemc smart score|advice|explain|baseline|thresholds`; `/gradlemc smart baseline reset confirm`

Exports can fail because reports are disabled, input is unavailable, limits are exceeded, or the filesystem denies a safe local write. Issue bundles contain only allowlisted GradleMC-generated material and are never uploaded automatically.
