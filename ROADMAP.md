# GradleMC Roadmap

This roadmap is a planning document, not a promise factory. If the code, build, runtime behavior, docs, and artifact naming do not prove support, the support claim does not exist.

---

## Current Supported Target

| Field | Value |
| --- | --- |
| Version | `1.0.1` |
| Minecraft | `1.20.1` |
| Loader | Forge |
| Forge target | `47.4.20` |
| Java | `17` |
| Artifact | `gradlemc-1.0.1-forge-1.20.1.jar` |

Current public focus:

- stable Forge `1.20.1` diagnostics;
- trustworthy reports;
- clean GUI and command UX;
- bounded local performance/profiler evidence;
- Smart Diagnostics clarity;
- adaptive diagnostics correctness;
- release automation and CI discipline;
- no telemetry, no fake AI, no unsupported support claims.

---

## Released: `1.0.1` Stabilization

`1.0.1` is the current public Forge `1.20.1` release.

Release-surface goals:

- keep support claims locked to Forge `1.20.1`;
- keep command examples lowercase;
- keep Smart Diagnostics and adaptive diagnostics described as local rule-based systems;
- keep profiler wording bounded and honest;
- keep CurseForge and GitHub docs synced;
- use only real screenshots captured from the released jar.

---

## Next: `1.0.x` Hardening

Future `1.0.x` work should be quality-first, not feature stuffing.

Priority work:

- fix confirmed bugs from real reports;
- improve GUI copy and error handling;
- polish command help and report summaries;
- tighten export/report privacy wording;
- improve diagnostics confidence explanations;
- verify dedicated-server behavior;
- keep CI strict;
- update README and CurseForge copy only after behavior is verified.

Do not add screenshots to the README until they are captured from the actual released build. Screenshots are proof, not decoration.

---

## Screenshot And Demo Pass

After `1.0.1`, add a focused visual proof section using real assets:

1. GUI overview screen.
2. Diagnostics/status screen.
3. Smart Diagnostics advice screen.
4. Reports/export confirmation.
5. Optional overlay screenshot.
6. Optional 10–20 second GIF showing `/gradlemc gui` to export flow.

Rules:

- use real screenshots from the released build;
- hide private file paths, usernames, server addresses, and tokens;
- do not use mockups unless clearly labeled;
- keep screenshots compressed enough for GitHub;
- prefer `docs/assets/screenshots/` or `media/screenshots/` with clear names;
- update README and CurseForge description together when screenshots are added.

---

## Mid-Term Quality Work

Potential `1.0.x` improvements:

- stronger report sections with clearer headings;
- better issue-bundle summaries;
- more actionable Smart Diagnostics recommendations;
- more GUI affordances for latest reports and profile summaries;
- stricter validation for public wording drift;
- clearer separation between diagnostics, profiler, and adaptive gameplay-state logic;
- better docs for server owners and support helpers.

Each item needs testing. A cool idea is not a release criterion.

---

## Future Ports

Possible future targets belong behind gates:

- newer Forge versions;
- Fabric candidates;
- NeoForge candidates;
- later Minecraft versions.

A port is not real until all of this is true:

- variant matrix says it is enabled;
- build works;
- runtime behavior is tested;
- client/server boundaries are correct;
- docs mention it accurately;
- artifact naming is correct;
- release automation exports the expected jar;
- CI covers the target;
- unsupported-feature checks stop false claims.

Until then, future targets are roadmap entries. Not downloads. Not support claims. Not vibes.

---

## Explicit Non-Goals

- No telemetry or analytics.
- No cloud AI.
- No LLM integration.
- No generative AI features.
- No private-file scanning outside the modpack/report scope.
- No fake profiler parity claims.
- No fake Fabric, NeoForge, Quilt, or future-version jars.
- No branch sprawl as a substitute for planning.
