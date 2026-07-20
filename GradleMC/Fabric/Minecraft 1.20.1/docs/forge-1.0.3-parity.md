# Forge 1.20.1 v1.0.3 to Fabric 1.20.1 v1.0.1 parity matrix

This is an internal engineering comparison, not a claim that every Forge class or command is ported. Forge is a read-only behavioral reference; Fabric architecture and APIs remain loader-native.

| Area | Classification | Fabric implementation and evidence |
|---|---|---|
| Product/loader/Minecraft identity | Equivalent | Processed `fabric.mod.json`, runtime identity lookup, manifest, metadata JUnit, and JAR inspection target GradleMC 1.0.1 / Fabric / Minecraft 1.20.1 / Java 17. |
| FPS measurement and bounded FPS test | Equivalent, Fabric-specific | One rendered-frame sampler feeds HUD, GUI, and test sessions; invalid gaps and no-sample results are explicit. JVM regression coverage exists. |
| Overlay defaults and controls | Equivalent, Fabric-specific | Overlay/branding/Average FPS default off; Current FPS is independent; empty component selection produces no rows. Visual layout remains a manual gate. |
| Configuration and migration | Equivalent outcome, Fabric-specific storage | Schema-v2 Fabric properties, atomic write, unknown-key preservation, malformed fallback, and v1.0.0 migration tests. Forge config API is not copied. |
| Basic checks, local rules, smart diagnostics | Equivalent core | Deterministic checks/rules, local thresholds/baseline, stability score/advice/explanation. No cloud or generative AI. |
| Performance/world-generation sampling | Equivalent core | Bounded server tick sampling and passive world-generation observation with local reports and explicit duration policy. |
| Java profiler | Equivalent core | Tick timeline, CPU-lite Java stack sampling, memory/GC pressure, bounded reports, and cautious attribution language. It is not async-profiler. |
| Mod inspection/audit | Fabric-specific equivalent | Fabric Loader metadata, origins, authors/contacts, contained mods, dependency state, and deterministic TXT/JSON export; no Forge-only fields are invented. |
| Tasks/workflows/evidence/history | Partial | Fabric has stable catalogs, deterministic planning/execution, availability, cancellation, deadlines, immutable TXT/JSON reports, and bounded history. Forge cache/explanation/history breadth is larger. |
| Commands | Partial | Core status/check/config/rules/mods/perf/worldgen/profiler/smart/report/workflow/FPS/GUI commands exist. Forge experiment, incident, instance, startup, storage, health-gate, scan, and broad network-diagnostic commands are absent. |
| GUI workspace | Partial | Fabric control/status screen, quick actions, tests, profiler, reports, settings, and about sections exist. Forge audit/scan/task-center breadth is not fully ported; manual GUI validation remains. |
| Reports and issue bundles | Equivalent core / partial breadth | Local TXT/JSON diagnostics, mod audit, profiler/workflow reports, allowlisted bounded issue bundles, best-effort redaction, legacy report discovery. Forge unified scan/index breadth is absent. |
| Dedicated-server safety | Fabric-specific equivalent | Split source sets, common-boundary verification, server-safe main entrypoint, owned worker shutdown, and prior development-server smoke cycles. |
| Multiplayer/capability lifecycle | Fabric-specific partial | Protocol-v1 channels, request limits/timeouts, generation correlation, stale-response rejection, reconnect and capability models have JVM coverage; live client/server matrix remains blocked. |
| Forge event/config/AT/metadata integration | Not applicable | Fabric lifecycle/events/networking/Loader metadata are used; there are no Mixins or access wideners. |
| Scan correlation, experiments, incidents, instance lock, startup/reload, storage and health gates | Blocked / absent | These Forge v1.0.3 subsystems are not exposed as fake Fabric commands. A separate scoped port and evidence model would be required. |

## Compatibility invariants

- `/gradlemc` and existing Fabric command literals remain stable.
- Config schema `2`, protocol `1`, and existing report/history schema identifiers remain independent of product version.
- Legacy Fabric reports remain readable; valid older config properties are preserved and new writes use canonical schema-v2 names.
- Common source remains client-free; client-only behavior stays in the client source set.

## Post-cleanup conclusion

The maintainability cleanup preserves the verified supported core: FPS correction, overlay independence/defaults, config migration, checks/rules, bounded diagnostics, Fabric mod audit, profiler, workflows/evidence, local reports/bundles, privacy controls, dedicated-server boundaries, and protocol correlation. It does not convert the documented Forge-only gaps into parity claims. Full Forge v1.0.3 feature parity is therefore **not achieved**; core behavioral parity is achieved only for the rows classified equivalent above.
