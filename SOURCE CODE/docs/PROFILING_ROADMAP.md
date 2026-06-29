# GradleMC Profiling Roadmap

## Competitive Context

Spark does several things well: quick profiler start/stop/open/cancel workflows, timeout support, thread filters, sampling interval controls, laggy-tick-only profiling, Java and async-profiler-backed sampling, allocation profiling where supported, health reports, TPS/MSPT views, tick and GC monitoring, heap inspection, viewer output, and flame graph/call-tree workflows.

GradleMC should not clone Spark. The target is a Minecraft/modpack-aware local workflow with better in-game controls, beginner-friendly interpretation, support-ready offline reports, and honest confidence labels.

## What GradleMC Currently Does

- Lowercase `/gradlemc` commands.
- GUI-driven diagnostics control center.
- Optional disabled-by-default stats overlay.
- Bounded TPS/MSPT, FPS, worldgen, entity, and block entity diagnostics.
- Local reports, issue bundles, Smart Diagnostics, and adaptive diagnostics.
- No telemetry, no cloud AI, no external inference.

## What This Pass Adds

- `/gradlemc profiler` command group with start, stop, cancel, status, latest, open, summary, and export.
- Bounded profiler session service shared by commands and GUI.
- Tick timeline recorder with bounded retention and slow tick thresholds.
- Slow tick snapshots with memory/GC context and cautious category labels.
- CPU-lite Java stack sampling with interval, thread filter, all-thread option, and sleeping/waiting filtering.
- Memory-lite heap and GC pressure tracking.
- Possible package/mod attribution with confidence levels.
- TXT and JSON profile reports under `<gameDir>/gradlemc/profiles/`.
- Profiler GUI tab with mode, duration, interval, thread, threshold, slow-only, start, stop, cancel, status, latest, folder, path, and summary controls.
- Small overlay status line in detailed overlay mode.

## What GradleMC Still Lacks

- Native async-profiler integration.
- True allocation profiling.
- Lock contention, native stack, safepoint, and kernel-level profiler insight.
- A local interactive flame graph or full call-tree viewer.
- Strong source/mod attribution for arbitrary classes.
- Real benchmark data proving low overhead on large modpacks.
- Runtime-tested reports from real modded client/server cases.

## Claim Gates

### Gate 1: Basic Profiler Foundation

- Profiler start/stop/cancel/status.
- Bounded tick timeline.
- Slow tick snapshots.
- JSON/TXT profiles.
- GUI profiler controls.

### Gate 2: Useful Profiler

- CPU-lite stack sampler.
- Thread filtering.
- Slow-tick-only sampling.
- Top stack report.
- Memory/GC correlation.
- Profile summaries are actionable.

### Gate 3: Spark Competitor

- Reliable source/mod attribution.
- Local HTML or strong in-game viewer.
- Flame graph or call tree export.
- Low overhead proven by tests.
- Users can solve real lag cases with GradleMC alone.

### Gate 4: Spark Challenger

- Optional deeper profiler backend such as JFR or async-profiler-style integration.
- Serious allocation, lock, and native insights where possible.
- Cross-platform strategy.
- Documented limitations.
- Benchmarked against Spark on real modpacks.

### Gate 5: Claim Upgrade

- Only after real evidence.
- Update CurseForge and README wording carefully.
- Never claim GradleMC beats Spark, replaces Spark, or is a Spark alternative before the implementation and evidence justify it.

## Public Wording Rules

- Safe now: "profiler foundation", "local profiler tools", "bounded Java CPU-lite sampling", "memory/GC pressure tracking".
- Unsafe now: "beats Spark", "Spark replacement", "Spark alternative", "deep profiler", "allocation profiler".
- "Deep profiler" requires meaningful stack sampling, attribution, report/viewer usability, and evidence from real lag cases.
