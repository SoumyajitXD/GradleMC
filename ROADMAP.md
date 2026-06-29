# GradleMC Roadmap

This roadmap is a planning document, not a promise factory. If the code, build, runtime behavior, docs, and artifact naming do not prove support, the support claim does not exist. Simple. 🗿

---

## Current Supported Target

| Field | Value |
| --- | --- |
| Version | `1.0.0` |
| Minecraft | `1.20.1` |
| Loader | Forge |
| Forge target | `47.4.20` |
| Java | `17` |
| Artifact | `gradlemc-1.0.0-forge-1.20.1.jar` |

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

## Next: `V1.0.1` Stabilization

`V1.0.1` should be a quality release, not a feature landfill.

Priority work:

- fix confirmed bugs from real reports;
- improve GUI copy and error handling;
- polish command help and report summaries;
- tighten export/report privacy wording;
- improve diagnostics confidence explanations;
- verify dedicated-server behavior;
- keep CI strict;
- update README and CurseForge copy only after behavior is verified.

Do **not** add screenshots to the README until the `V1.0.1` visual state is stable. Screenshots of a half-finished UI age like milk in a furnace.

---

## Screenshot And Demo Pass

After `V1.0.1` finishes, add a focused visual proof section:

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

Each item needs testing. “Looks cool” is not a release criterion; that is how bloat enters through the window with a fake mustache.

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
