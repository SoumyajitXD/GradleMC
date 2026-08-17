# GradleMC Release Checklist

Use this checklist before publishing or exporting a GradleMC release. Releases are where sloppy projects go to embarrass themselves in public. Do not freestyle it.

---

## Release Identity — Minecraft 1.20.1 v1.1.0

### Forge

| Field | Expected |
| --- | --- |
| GradleMC | `1.1.0` |
| Minecraft | `1.20.1` |
| Loader | Forge |
| Java runtime | `17` |
| GradleMC implementation | **Kotlin** |
| Required dependency | **Kotlin for Forge** |
| Artifact | `gradlemc-1.1.0-forge-1.20.1.jar` |
| Mod ID | `gradlemc` |

### Fabric

| Field | Expected |
| --- | --- |
| GradleMC | `1.1.0` |
| Minecraft | `1.20.1` |
| Loader | Fabric |
| Java runtime | `17` |
| GradleMC implementation | **Kotlin** |
| Required dependencies | **Fabric API + Fabric Language Kotlin** |
| Artifact | `gradlemc-1.1.0-fabric-1.20.1.jar` |
| Mod ID | `gradlemc` |

CurseForge Project ID: `1585182`.

The Kotlin implementation replaces GradleMC's previous Java application code; it does not replace Minecraft's Java `17` runtime requirement.

### Other published/legacy lines

| Status | Loader | GradleMC | Minecraft | Java | Artifact / notes |
| --- | --- | --- | --- | --- | --- |
| Published | Forge | `1.0.0` | `1.21.11` | `21` | `gradlemc-forge-1.21.11-1.0.0.jar` |
| Published | Fabric | `1.0.0` | `1.21.11` | `21` | `gradlemc-fabric-1.21.11-1.0.0.jar` |
| Published | NeoForge | `1.0.0` | `1.21.11` | `21` | `gradlemc-neoforge-1.21.11-1.0.0.jar` |
| Published | Forge | `1.0.0` | `26.1.2` | `25` | `gradlemc-forge-26.1.2-1.0.0.jar` |
| Published | Fabric | `1.0.0` | `26.1.2` | `25` | `gradlemc-fabric-26.1.2-1.0.0.jar` |
| Published | NeoForge | `1.0.0` | `26.1.2` | `25` | `gradlemc-neoforge-26.1.2-1.0.0.jar` |
| **Discontinued** | Quilt | `1.0.0` | `1.20.1` | `17` | Legacy artifact only; no new GradleMC Quilt updates |

If version, loader, Minecraft target, Java target, dependency list, source language, release notes, and jar filename disagree, stop. That is not a release; it is a bug with a ZIP extension.

---

## Source-Sync Gate

Before claiming GitHub reproduces a release, all of these must be true:

- [ ] The relevant source tree is synchronized to the intended version.
- [ ] GradleMC version metadata matches the release.
- [ ] Artifact naming is exact.
- [ ] Kotlin/JVM compilation targets the required Java level.
- [ ] Loader-specific dependency metadata is present and correct.
- [ ] Build and runtime verification use the synchronized project.

For current Minecraft `1.20.1` `v1.1.0`, both Forge and Fabric source trees are synchronized in `main`; future changes must preserve that alignment.

A release is code plus behavior, not a version sticker.

---

## Pre-Release Checks

For every target:

- [ ] Confirm Minecraft version.
- [ ] Confirm loader and loader version.
- [ ] Confirm GradleMC version.
- [ ] Confirm Java/JVM target.
- [ ] Confirm required mod dependencies.
- [ ] Confirm exact artifact name.
- [ ] Confirm source metadata and public docs agree.
- [ ] Confirm command literals remain lowercase.
- [ ] Confirm no telemetry/cloud behavior was introduced.

For Forge `1.20.1` `v1.1.0`:

- [ ] Kotlin for Forge is present in the test instance/server.
- [ ] Client launches with the actual release jar.
- [ ] Dedicated server launches where server use is supported.
- [ ] `/gradlemc version` reports `1.1.0` and expected Forge/Minecraft/JVM context.

For Fabric `1.20.1` `v1.1.0`:

- [ ] Fabric API is present.
- [ ] Fabric Language Kotlin is present.
- [ ] Client launches with the actual release jar.
- [ ] Dedicated server launches where server use is supported.
- [ ] `/gradlemc version` reports `1.1.0` and expected Fabric/Minecraft/JVM context.

---

## Build Verification

Run from the exact source project representing the intended release:

```sh
./gradlew clean build
```

On Windows:

```text
gradlew.bat clean build
```

Run target-specific self-tests where defined.

Rules:

- do not claim a build passed unless it ran and passed;
- do not build one loader and rename the output for another loader;
- do not casually use `--refresh-dependencies` or delete caches;
- keep dependency downloads controlled and intentional;
- use Java `17` for Minecraft `1.20.1`, Java `21` for `1.21.11`, and Java `25` for published `26.1.2` releases.

---

## Manual Smoke Test

Test the actual release jar, not merely development classes:

- [ ] Client launches.
- [ ] Dedicated server launches where applicable.
- [ ] `/gradlemc` help works.
- [ ] `/gradlemc gui` opens where supported.
- [ ] GUI keybind works where supported.
- [ ] `/gradlemc status` works.
- [ ] `/gradlemc version` reports expected identity.
- [ ] `/gradlemc memory` works.
- [ ] `/gradlemc check` works with correct permissions.
- [ ] `/gradlemc export` creates output.
- [ ] `/gradlemc reports latest` finds the newest report.
- [ ] Smart Diagnostics commands work where included.
- [ ] Performance/FPS tools behave only on supported sides.
- [ ] Client-only classes do not load on a dedicated server.
- [ ] Overlay defaults and settings match release intent.
- [ ] Reports remain local unless the user manually shares them.

For Forge `1.20.1` `v1.1.0`:

- [ ] Kotlin for Forge is installed.
- [ ] Exported jar is exactly `gradlemc-1.1.0-forge-1.20.1.jar`.

For Fabric `1.20.1` `v1.1.0`:

- [ ] Fabric API is installed.
- [ ] Fabric Language Kotlin is installed.
- [ ] Exported jar is exactly `gradlemc-1.1.0-fabric-1.20.1.jar`.

---

## Export

Build first, then copy only the intended release jar.

Verify:

- [ ] artifact exists before export;
- [ ] filename exactly matches release identity;
- [ ] jar metadata reports intended GradleMC version;
- [ ] jar metadata reports intended loader/Minecraft target;
- [ ] JVM compatibility is correct;
- [ ] required dependencies are documented;
- [ ] no logs, run folders, generated reports, or private files are included;
- [ ] no renamed jar is being passed off as another loader port;
- [ ] release path/location is correct.

---

## Quilt Gate

Quilt development is discontinued.

Before any new Quilt work is accepted, the project owner must explicitly reverse that policy. Otherwise:

- [ ] do not create new Quilt release artifacts;
- [ ] do not add Quilt to active-loader badges;
- [ ] do not promise Quilt fixes/features;
- [ ] keep existing Quilt source/artifacts marked legacy or discontinued.

---

## Public Text Check

Check at least:

- [ ] `README.md`;
- [ ] `CHANGELOG.md`;
- [ ] `ROADMAP.md`;
- [ ] `SUPPORT.md`;
- [ ] `SECURITY.md`;
- [ ] `CONTRIBUTING.md`;
- [ ] `AGENTS.md`;
- [ ] `docs/RELEASE_CHECKLIST.md`;
- [ ] screenshot documentation where relevant;
- [ ] release notes and artifact listings.

For Minecraft `1.20.1` `v1.1.0`, public surfaces must agree on:

- [ ] GradleMC `1.1.0`;
- [ ] Minecraft `1.20.1`;
- [ ] Java `17` runtime;
- [ ] Kotlin implementation;
- [ ] Forge + Kotlin for Forge + `gradlemc-1.1.0-forge-1.20.1.jar`;
- [ ] Fabric + Fabric API + Fabric Language Kotlin + `gradlemc-1.1.0-fabric-1.20.1.jar`;
- [ ] no new Quilt releases.

---

## After Release

- [ ] Publish the exact intended jar.
- [ ] Update changelog and README.
- [ ] Update support/security docs when support status changes.
- [ ] Synchronize source to GitHub when the public release source is meant to be available there.
- [ ] Watch issues/comments for regressions and dependency confusion.

---

## Release Killer Conditions

Do not release if any of these are true:

- build or CI fails;
- artifact name is wrong;
- source metadata and public docs disagree;
- required dependency is omitted;
- command casing regressed;
- dedicated server loads client-only code;
- docs claim unsupported loaders/versions;
- Quilt is accidentally presented as actively maintained;
- source-language claims do not match the actual release;
- the release depends on “probably fine.”

“Probably fine” is not quality control. It is a bug-report incubator.
