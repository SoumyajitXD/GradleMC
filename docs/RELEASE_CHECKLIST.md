# GradleMC Release Checklist

Use this checklist before publishing or exporting a GradleMC release. Releases are where sloppy projects go to embarrass themselves in public. Do not freestyle it.

---

## Release Identity

| Loader | GradleMC | Minecraft | Java | Expected public artifact | Loader target / notes |
| --- | --- | --- | --- | --- | --- |
| Forge | `1.0.0` | `1.21.11` | `21` | `gradlemc-forge-1.21.11-1.0.0.jar` | Forge `61.1.8` |
| Fabric | `1.0.0` | `1.21.11` | `21` | `gradlemc-fabric-1.21.11-1.0.0.jar` | Fabric Loader `0.19.3`; Fabric API `0.141.4+1.21.11` |
| NeoForge | `1.0.0` | `1.21.11` | `21` | `gradlemc-neoforge-1.21.11-1.0.0.jar` | NeoForge `21.11.42` |
| Forge | `1.0.0` | `26.1.2` | `25` | `gradlemc-forge-26.1.2-1.0.0.jar` | Forge `26.1.2-64.0.11` |
| Fabric | `1.0.0` | `26.1.2` | `25` | `gradlemc-fabric-26.1.2-1.0.0.jar` | Fabric `26.1.2` release |
| Forge | `1.0.2` | `1.20.1` | `17` | `gradlemc-1.0.2-forge-1.20.1.jar` | Forge `47.4.20`; Quick Actions overlay hotfix |
| Fabric | `1.0.0` | `1.20.1` | `17` | `gradlemc-fabric-1.20.1-1.0.0.jar` | Fabric `1.20.1` release |
| Quilt | `1.0.0` | `1.20.1` | `17` | `gradlemc-quilt-1.20.1-1.0.0.jar` | Quilt `1.20.1` release |

| Field | Expected |
| --- | --- |
| Mod ID | `gradlemc` |
| Product name | GradleMC |
| CurseForge Project ID | `1585182` |

If the version, loader, Minecraft target, Java target, metadata, release notes, and jar filename disagree, stop. That is not a release; it is a bug with a ZIP extension.

---

## Pre-Release Checks

Run from the matching standalone source project:

```text
GradleMC/Forge/Minecraft 1.21.11/
GradleMC/Fabric/Minecraft 1.21.11/
GradleMC/NeoForge/Minecraft 1.21.11/
GradleMC/Forge/Minecraft 26.1.2/
GradleMC/Fabric/Minecraft 26.1.2/
GradleMC/Forge/Minecraft 1.20.1/
GradleMC/Fabric/Minecraft 1.20.1/
GradleMC/Quilt/Minecraft 1.20.1/
```

Standard verification:

```sh
./gradlew clean build
```

Run `gradlemcSelfTest` where the target defines it. On Windows, use `gradlew.bat`.

Java rules:

- Java `17` for Minecraft `1.20.1`.
- Java `21` for Minecraft `1.21.11`.
- Java `25` for the released Minecraft `26.1.2` builds.

Do not claim a build passed unless it actually ran and passed.

---

## Manual Smoke Test

Test the actual release jar:

- [ ] Client launches.
- [ ] Dedicated server launches where applicable.
- [ ] `/gradlemc` help works.
- [ ] `/gradlemc gui` opens for an in-game player.
- [ ] GUI keybind opens the GUI.
- [ ] `/gradlemc status` works.
- [ ] `/gradlemc version` reports the expected Minecraft, loader, GradleMC, and Java identity.
- [ ] `/gradlemc memory` works.
- [ ] `/gradlemc check` is permission-gated correctly.
- [ ] `/gradlemc export` writes a report.
- [ ] `/gradlemc reports latest` locates the newest report.
- [ ] `/gradlemc smart score` works.
- [ ] `/gradlemc smart advice` works.
- [ ] `/gradlemc perf start 30` and `/gradlemc perf stop` work.
- [ ] Client-only FPS tools do not load on dedicated servers.
- [ ] Overlay remains disabled by default.

Target-specific identity checks:

- [ ] Forge `1.21.11`: Forge `61.1.8`, GradleMC `1.0.0`, Java `21`.
- [ ] Fabric `1.21.11`: Fabric Loader `0.19.3`, Fabric API `0.141.4+1.21.11`, GradleMC `1.0.0`, Java `21`.
- [ ] NeoForge `1.21.11`: NeoForge `21.11.42`, GradleMC `1.0.0`, Java `21`.
- [ ] Forge `26.1.2`: Forge `26.1.2-64.0.11`, GradleMC `1.0.0`, Java `25`.
- [ ] Fabric `26.1.2`: GradleMC `1.0.0`, Java `25`.
- [ ] Forge `1.20.1` `1.0.2`: the Quick Actions tab no longer overlays lower controls or text.

---

## Export

Build first, then copy the intended jar from the target's `build/libs/` folder.

Verify:

- [ ] the artifact exists before export;
- [ ] the filename exactly matches the supported-release table;
- [ ] jar metadata reports the intended GradleMC version;
- [ ] jar metadata reports the intended loader;
- [ ] jar metadata reports the intended Minecraft target;
- [ ] Java compatibility matches the release line;
- [ ] no generated local reports, logs, run folders, or private files are committed;
- [ ] no renamed jar is being passed off as another loader port;
- [ ] the artifact is placed under the correct `Releases/<Loader>/Minecraft <version>/` path when committed to the repository.

---

## Screenshot And Visual Check

The committed screenshot set lives in [`../Screenshots/`](../Screenshots/), and the full gallery is documented in [`SCREENSHOTS.md`](SCREENSHOTS.md).

Before publishing or replacing screenshots:

- [ ] screenshots come from a supported release jar;
- [ ] screenshots do not imply an untested loader/version pair;
- [ ] README preview images render on GitHub;
- [ ] `docs/SCREENSHOTS.md` includes every committed screenshot;
- [ ] `docs/SCREENSHOT_PLAN.md` matches current paths and support claims;
- [ ] CurseForge copy is synchronized when relevant.

---

## Public Text Check

Check every public surface:

- [ ] `README.md`;
- [ ] `CHANGELOG.md`;
- [ ] `ROADMAP.md`;
- [ ] `SUPPORT.md`;
- [ ] `SECURITY.md`;
- [ ] `CONTRIBUTING.md`;
- [ ] `AGENTS.md`;
- [ ] `docs/SCREENSHOTS.md`;
- [ ] `docs/SCREENSHOT_PLAN.md`;
- [ ] `curseforge-description.html`;
- [ ] release notes, issue templates, and PR template.

Confirm:

- [ ] all eight current public artifacts are listed accurately;
- [ ] Java `17`, `21`, and `25` are attached to the correct release lines;
- [ ] Forge `1.21.11`, Fabric `1.21.11`, and NeoForge `1.21.11` are treated as released targets;
- [ ] commands are lowercase;
- [ ] Smart Diagnostics and adaptive diagnostics are not described as LLMs, generative AI, telemetry, analytics, or cloud inference;
- [ ] profiler language does not imply Spark parity;
- [ ] Bedrock and unlisted loader/version pairs remain unsupported.

---

## After Release

- [ ] Publish or attach the correct jar through the intended platform.
- [ ] Update `CHANGELOG.md`.
- [ ] Update README artifact/version tables.
- [ ] Update support and security docs when supported targets change.
- [ ] Update screenshot docs if visuals change.
- [ ] Update CurseForge description when public releases change.
- [ ] Watch GitHub issues and CurseForge comments for regressions.

---

## Release Killer Conditions

Do not release if any of these are true:

- build or CI fails;
- artifact name is wrong;
- source metadata and public docs disagree;
- command casing regressed;
- dedicated server loads client-only code;
- docs claim unsupported loaders or versions;
- screenshots imply unsupported behavior;
- stale paths remain;
- the release depends on “probably fine.”

“Probably fine” is not quality control. It is a bug-report incubator.
