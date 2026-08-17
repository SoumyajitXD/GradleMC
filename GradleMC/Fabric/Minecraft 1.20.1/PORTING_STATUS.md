# GradleMC Fabric 1.20.1 — completion-pass status

The Kotlin Forge 1.20.1 project remains the read-only behavioural donor. This is a native Fabric implementation.

| Area | Status | Evidence / limitation |
|---|---|---|
| Current Forge command vocabulary | Complete | All donor literals plus direct `perf <seconds>` are registered; bare `performance mode` now returns the summary. |
| Help and performance family | Implemented | Concise grouped help reflects the current tree; mode persists through the shared config. |
| Overlay | Runtime-tested integrated; dedicated multiplayer pending | Visible in the disposable integrated world; G opens the GUI and the HUD showed a live FPS value. Overlay controls were found not visible in the GUI and fixed; the rebuilt visual retest is still pending. The client-only multiplayer route is `/gradlemc-overlay`, avoiding a duplicate `/gradlemc` root. |
| Overlay controls | Implemented and unit-tested | On/off, title, current/average FPS, 30/60/120-second window; atomic persistence and invalid-window recovery. |
| Reports and issue bundles | Implemented and unit-tested | Bounded owned-report listing and explicit privacy-safe two-entry issue bundle; no uploads. |
| Files/config helpers | Implemented and unit-tested | Only GradleMC-owned locations are shown; configuration validation is local and bounded. |
| Mod tools | Implemented | Fabric Loader metadata count/list/search/inspect/audit/export with bounded chat output. |
| Entity diagnostics | Implemented | Permission-2, player-centred bounded entity/block-entity scans; block entities read loaded chunks only and never force chunk loads. |
| Richer Smart commands | Implemented | Explain and thresholds are deterministic/local alongside score/advice. |
| Historical advanced systems | Audited, deferred | Passive worldgen needs Fabric hook/session integration; profiler needs sizeable bounded-session work; tasks/workflows are too coupled; rules are unsafe to rush. |
| GUI controls | Partially runtime-tested | G opened one functional diagnostics screen in the disposable integrated world. Overlay-section controls were not rendered in that session; the clipping guard was removed and requires rebuilt-client visual confirmation. |
| Dedicated-server isolation | Compile-tested; runtime pending | Main source has no client UI/render imports. Client HUD and screen remain in the client source set; dedicated server/client interaction has not yet been runtime-tested in Phase 4. |
| Tests | Expanded | Command vocabulary surface, overlay persistence/recovery, report listing, config helpers and bundle support are covered. |

No telemetry, cloud APIs, Forge references, production Java, Mixins, or dependency upgrades were introduced.
