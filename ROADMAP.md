# GradleMC Roadmap

This roadmap records the public GradleMC support plan. A target is supported only when source metadata, builds, runtime behavior, documentation, and artifact naming agree.

---

## Current Supported Public Targets

| Loader | GradleMC | Minecraft | Java | Artifact | Notes |
| --- | --- | --- | --- | --- | --- |
| Forge | `1.0.0` | `1.21.11` | `21` | `gradlemc-forge-1.21.11-1.0.0.jar` | Forge `61.1.8` |
| Fabric | `1.0.0` | `1.21.11` | `21` | `gradlemc-fabric-1.21.11-1.0.0.jar` | Fabric Loader `0.19.3`; Fabric API `0.141.4+1.21.11` |
| NeoForge | `1.0.0` | `1.21.11` | `21` | `gradlemc-neoforge-1.21.11-1.0.0.jar` | NeoForge `21.11.42` |
| Forge | `1.0.0` | `26.1.2` | `25` | `gradlemc-forge-26.1.2-1.0.0.jar` | Forge `26.1.2-64.0.11` |
| Fabric | `1.0.0` | `26.1.2` | `25` | `gradlemc-fabric-26.1.2-1.0.0.jar` | Fabric `26.1.2` release |
| Forge | `1.0.2` | `1.20.1` | `17` | `gradlemc-1.0.2-forge-1.20.1.jar` | Quick Actions overlay hotfix |
| Fabric | `1.0.0` | `1.20.1` | `17` | `gradlemc-fabric-1.20.1-1.0.0.jar` | Fabric `1.20.1` release |
| Quilt | `1.0.0` | `1.20.1` | `17` | `gradlemc-quilt-1.20.1-1.0.0.jar` | Quilt `1.20.1` release |

Current public focus:

- stabilize the Forge, Fabric, and NeoForge `1.21.11` release line;
- maintain the released `1.20.1` and `26.1.2` targets;
- keep reports trustworthy and reviewable;
- keep GUI and command UX clean;
- keep performance and profiler evidence bounded;
- improve Smart Diagnostics clarity without pretending it is cloud AI;
- keep CurseForge, GitHub, source metadata, and artifact names synchronized;
- use only real screenshots from supported builds.

---

## Released: Minecraft `1.21.11` `1.0.0`

GradleMC `1.0.0` is publicly released for Forge, Fabric, and NeoForge on Minecraft `1.21.11`.

Release-surface rules:

- keep Forge artifact naming exact: `gradlemc-forge-1.21.11-1.0.0.jar`;
- keep Fabric artifact naming exact: `gradlemc-fabric-1.21.11-1.0.0.jar`;
- keep NeoForge artifact naming exact: `gradlemc-neoforge-1.21.11-1.0.0.jar`;
- keep Java `21` attached to all three targets;
- document Forge `61.1.8`, Fabric Loader `0.19.3` with Fabric API `0.141.4+1.21.11`, and NeoForge `21.11.42` accurately;
- keep `/gradlemc` command examples lowercase;
- keep Smart Diagnostics and adaptive diagnostics described as local systems;
- do not imply feature parity that was not verified at runtime.

These are real releases, not roadmap entries wearing jar filenames as fake moustaches.

---

## Maintained Release Lines

### Minecraft `26.1.2`

- Forge `1.0.0`: `gradlemc-forge-26.1.2-1.0.0.jar`, Java `25`.
- Fabric `1.0.0`: `gradlemc-fabric-26.1.2-1.0.0.jar`, Java `25`.

### Minecraft `1.20.1`

- Forge `1.0.2`: `gradlemc-1.0.2-forge-1.20.1.jar`, Java `17`.
- Fabric `1.0.0`: `gradlemc-fabric-1.20.1-1.0.0.jar`, Java `17`.
- Quilt `1.0.0`: `gradlemc-quilt-1.20.1-1.0.0.jar`, Java `17`.

---

## Next: `1.0.x` Hardening

Future `1.0.x` work should be quality-first, not feature stuffing.

Priority work:

- fix confirmed bugs from real reports;
- verify client and dedicated-server behavior on every supported target;
- improve GUI copy, layout, and error handling;
- polish command help and report summaries;
- tighten export and issue-bundle wording;
- improve diagnostics confidence explanations;
- keep screenshots synchronized with real UI;
- keep CI strict and loader-aware;
- update public copy only after behavior is verified.

A cool idea is not a release criterion. It is merely a cool idea.

---

## Future Ports

Possible future targets remain behind release gates, including:

- additional Forge, Fabric, NeoForge, or Quilt versions not listed above;
- later Minecraft versions;
- any new loader family.

A port is not public support until all of this is true:

- source metadata identifies the intended loader and Minecraft version;
- the build succeeds;
- runtime behavior is tested;
- client/server boundaries are correct;
- documentation is accurate;
- screenshots do not misrepresent it;
- artifact naming is correct;
- release automation exports the expected jar;
- CI covers the target.

Until then, it is a candidate, not a release.

---

## Explicit Non-Goals

- No telemetry or analytics.
- No cloud AI or generative AI features.
- No fake profiler-parity claims.
- No fake jars or renamed artifacts pretending to be ports.
- No unlisted loader/version support claims.
- No branch sprawl as a substitute for planning.
