# GradleMC Roadmap

This roadmap records the public support plan. A target is considered current only when release identity, dependencies, documentation, and actual artifacts agree. Source presence alone is not proof of a current release.

---

## Current Focus

### Forge `1.20.1` — GradleMC `v1.1.0`

The current GradleMC focus is the Kotlin-based Forge `1.20.1` release:

- GradleMC `1.1.0`;
- Minecraft `1.20.1`;
- Forge;
- Java `17` runtime;
- **Kotlin implementation**;
- **Kotlin for Forge required**;
- artifact: `gradlemc-1.1.0-forge-1.20.1.jar`.

Priority work for this line:

- keep diagnostics accurate and bounded;
- preserve lowercase `/gradlemc` commands;
- keep the GUI, keybind, overlays, reports, and Smart Diagnostics stable;
- improve correctness before feature count;
- keep generated evidence local and reviewable;
- keep client/server boundaries safe;
- synchronize the Kotlin source tree to GitHub;
- keep CurseForge, GitHub docs, metadata, and artifact naming aligned.

The checked-in Forge `1.20.1` source on `main` is still the legacy Java `v1.0.4` project. Updating that source is a separate required synchronization task; documentation must not pretend the old Java tree is the Kotlin release.

---

## Published Release Lines

| Status | Loader | GradleMC | Minecraft | Java | Notes |
| --- | --- | --- | --- | --- | --- |
| **Latest / active focus** | Forge | `1.1.0` | `1.20.1` | `17` | Kotlin; Kotlin for Forge required |
| Published | Fabric | `1.0.0` | `1.20.1` | `17` | Existing Fabric release |
| Published | Forge | `1.0.0` | `1.21.11` | `21` | Forge `61.1.8` |
| Published | Fabric | `1.0.0` | `1.21.11` | `21` | Fabric Loader `0.19.3`; Fabric API `0.141.4+1.21.11` |
| Published | NeoForge | `1.0.0` | `1.21.11` | `21` | NeoForge `21.11.42` |
| Published | Forge | `1.0.0` | `26.1.2` | `25` | Forge `26.1.2-64.0.11` |
| Published | Fabric | `1.0.0` | `26.1.2` | `25` | Existing Fabric release |
| Published | NeoForge | `1.0.0` | `26.1.2` | `25` | NeoForge `26.1.2.78` |
| **Discontinued** | Quilt | `1.0.0` | `1.20.1` | `17` | Legacy only; no new GradleMC Quilt updates |

---

## Quilt: Development Ended

No new GradleMC versions are planned for Quilt.

The Quilt release line produced too few downloads to justify the ongoing loader-specific development, regression testing, compatibility work, documentation, and release maintenance. Existing Quilt artifacts may remain available as legacy downloads, but future features and fixes will not be ported to Quilt.

Roadmap rules:

- do not list Quilt as an active loader;
- do not promise future Quilt versions;
- do not create new Quilt ports unless the project owner explicitly reverses this decision;
- keep existing Quilt source/artifacts clearly marked legacy or discontinued where they remain in the repository.

---

## Kotlin Migration Direction

The Forge `1.20.1` `v1.1.0` rewrite establishes Kotlin as the implementation language for that release line.

For the Kotlin Forge line:

- do not reintroduce Java source merely to recreate old architecture;
- keep Java `17` as the required JVM/runtime for Minecraft `1.20.1`;
- keep Kotlin for Forge explicit in installation and support documentation;
- prefer direct, maintainable Kotlin over abstraction-heavy rewrites;
- preserve working diagnostics behavior while migrating internals;
- verify dependency, loader, client/server, and packaging behavior before release claims.

Kotlin replacing GradleMC's Java source does not change the fact that Minecraft Forge runs on the JVM.

---

## Quality Priorities

Future work should be quality-first rather than feature stuffing:

1. Fix confirmed bugs from real reports.
2. Keep FPS and performance evidence trustworthy.
3. Keep runtime overhead bounded.
4. Keep the diagnostics GUI readable and functional.
5. Keep command behavior consistent and lowercase.
6. Improve report clarity and evidence quality.
7. Test dedicated-server safety for common/server code.
8. Keep Smart Diagnostics local, rule-based, and transparent.
9. Keep release metadata and public documentation synchronized.
10. Avoid branch sprawl and abandoned partial ports.

A feature that exists only in documentation is not a feature. It is fan fiction with a version number.

---

## Future Ports

Possible future targets may include additional Forge, Fabric, or NeoForge releases and later Minecraft versions.

A new port becomes supported only after all of the following are true:

- source metadata identifies the correct Minecraft version and loader;
- the project builds successfully;
- required dependencies are documented;
- client and/or dedicated-server behavior is verified as applicable;
- the artifact name is correct;
- runtime identity reports the intended target;
- public documentation is updated;
- release packaging is verified.

Quilt is excluded from this future-port list unless support is explicitly reinstated by the project owner.

---

## Non-Goals

- No telemetry or analytics.
- No cloud AI, LLM, or hidden remote inference.
- No fake profiler-parity claims.
- No renamed artifacts pretending to be ports.
- No unsupported loader/version claims.
- No Quilt maintenance by default.
- No branch sprawl as a substitute for planning.
- No Java reintroduction into the Kotlin Forge `1.20.1` implementation without a concrete technical reason.
