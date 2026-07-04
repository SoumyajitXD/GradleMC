# GradleMC Roadmap

This roadmap records the public GradleMC support plan. A target is supported only when the code, build, runtime behavior, documentation, screenshots, and artifact naming all agree.

---

## Current Supported Public Targets

| Loader | Version | Minecraft | Java | Artifact | Notes |
| --- | --- | --- | --- | --- | --- |
| Forge | `1.0.2` | `1.20.1` | `17` | `gradlemc-1.0.2-forge-1.20.1.jar` | Forge target `47.4.20`; Quick Actions overlay hotfix |
| Fabric | `1.0.0` | `1.20.1` | `17` | `gradlemc-fabric-1.20.1-1.0.0.jar` | Fabric `1.20.1` release |
| Quilt | `1.0.0` | `1.20.1` | `17` | `gradlemc-quilt-1.20.1-1.0.0.jar` | Quilt `1.20.1` release |

Current public focus:

- stable Forge, Fabric, and Quilt `1.20.1` diagnostics;
- trustworthy reports;
- clean GUI and command UX;
- bounded local performance and profiler evidence;
- Smart Diagnostics clarity;
- adaptive diagnostics correctness;
- screenshot-backed README and docs;
- release and CI discipline.

---

## Released: Quilt `1.20.1` `1.0.0`

`1.0.0` is the current public Quilt `1.20.1` release.

Release-surface goals:

- treat Quilt `1.20.1` as a real public target, not a roadmap hallucination with a jar filename taped to it;
- keep Quilt artifact naming exact: `gradlemc-quilt-1.20.1-1.0.0.jar`;
- keep command examples lowercase;
- keep Smart Diagnostics and adaptive diagnostics described as local rule-based systems;
- keep profiler wording bounded and honest;
- keep CurseForge and GitHub docs synced;
- use only real screenshots captured from supported builds.

---

## Released: Forge `1.0.2` Hotfix

`1.0.2` is the current public Forge `1.20.1` release.

Release-surface goals:

- keep Forge support claims locked to Forge `1.20.1`;
- keep Forge artifact naming exact: `gradlemc-1.0.2-forge-1.20.1.jar`;
- document the real hotfix scope: Quick Actions tab overlay fix;
- keep command examples lowercase;
- keep Smart Diagnostics and adaptive diagnostics described as local rule-based systems;
- keep profiler wording bounded and honest;
- keep CurseForge and GitHub docs synced;
- use only real screenshots captured from supported builds.

---

## Released: Fabric `1.20.1` `1.0.0`

`1.0.0` is the current public Fabric `1.20.1` release.

Release-surface goals:

- treat Fabric `1.20.1` as a real public target, not a roadmap maybe-baby;
- keep Fabric artifact naming exact: `gradlemc-fabric-1.20.1-1.0.0.jar`;
- keep command examples lowercase;
- keep Smart Diagnostics and adaptive diagnostics described as local rule-based systems;
- keep profiler wording bounded and honest;
- keep CurseForge and GitHub docs synced;
- use only real screenshots captured from supported builds.

---

## Released: Forge `1.0.1` Stabilization

`1.0.1` was the previous public Forge `1.20.1` release.

Release-surface goals:

- keep Forge support claims locked to Forge `1.20.1`;
- keep Forge artifact naming exact: `gradlemc-1.0.1-forge-1.20.1.jar`;
- keep command examples lowercase;
- keep Smart Diagnostics and adaptive diagnostics described as local rule-based systems;
- keep profiler wording bounded and honest;
- keep CurseForge and GitHub docs synced;
- use only real screenshots captured from supported builds.

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
- verify dedicated-server behavior on supported loaders;
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
- newer Fabric versions;
- newer Quilt versions;
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
- No fake NeoForge, Bedrock, or future-version jars.
- No branch sprawl as a substitute for planning.
