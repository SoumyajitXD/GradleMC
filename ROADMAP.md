# GradleMC Roadmap

This roadmap records the public GradleMC support plan. A target is supported only when the code, build, runtime behavior, documentation, screenshots, and artifact naming all agree.

---

## Current Supported Public Target

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
- bounded local performance and profiler evidence;
- Smart Diagnostics clarity;
- adaptive diagnostics correctness;
- screenshot-backed README and docs;
- release and CI discipline.

---

## Released: `1.0.1` Stabilization

`1.0.1` is the current public Forge `1.20.1` release.

Release-surface goals:

- keep support claims locked to Forge `1.20.1`;
- keep command examples lowercase;
- keep Smart Diagnostics and adaptive diagnostics described as local rule-based systems;
- keep profiler wording bounded and honest;
- keep CurseForge and GitHub docs synced;
- use only real screenshots captured from the supported build.

---

## Screenshot And Demo Pass

The GitHub screenshot pass has started. Current assets live in [`Screenshots/`](Screenshots/), and the full gallery is documented in [`docs/SCREENSHOTS.md`](docs/SCREENSHOTS.md).

Rules:

1. The README gets only a compact preview.
2. The full gallery belongs in `docs/SCREENSHOTS.md`.
3. Screenshot maintenance rules belong in `docs/SCREENSHOT_PLAN.md`.
4. Screenshots must not imply unsupported loader or version support.
5. Visuals must be updated with docs when UI or report flow changes.

Future improvement: rename numbered screenshots to descriptive lowercase kebab-case names in a focused asset-cleanup change that updates every reference.

---

## Next: `1.0.x` Hardening

Future `1.0.x` work should be quality-first, not feature stuffing.

Priority work:

- fix confirmed bugs from real reports;
- improve GUI copy and error handling;
- polish command help and report summaries;
- tighten export and report wording;
- improve diagnostics confidence explanations;
- verify dedicated-server behavior;
- keep screenshot docs synced with real UI;
- keep CI strict;
- update README and CurseForge copy only after behavior is verified.

---

## Mid-Term Quality Work

Potential `1.0.x` improvements:

- stronger report sections with clearer headings;
- better issue-bundle summaries;
- more actionable Smart Diagnostics recommendations;
- more GUI affordances for latest reports and profile summaries;
- stricter validation for public wording drift;
- clearer separation between diagnostics, profiler, and adaptive gameplay-state logic;
- better docs for server owners and support helpers;
- better screenshot filenames and visual captions.

Each item needs testing. A cool idea is not a release criterion.

---

## Future Ports

Possible future targets belong behind gates:

- newer Forge versions;
- Fabric candidates;
- NeoForge candidates;
- later Minecraft versions.

A port is not real until all of this is true:

- variant or source metadata says it is enabled;
- build works;
- runtime behavior is tested;
- client/server boundaries are correct;
- docs mention it accurately;
- screenshots do not misrepresent it;
- artifact naming is correct;
- release automation exports the expected jar;
- CI covers the target;
- unsupported-feature checks stop false claims.

Until then, future targets are roadmap entries, not release claims.

---

## Explicit Non-Goals

- No telemetry or analytics.
- No cloud AI.
- No generative AI features.
- No fake profiler parity claims.
- No fake Fabric, NeoForge, Quilt, or future-version jars.
- No branch sprawl as a substitute for planning.
