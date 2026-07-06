# GradleMC Release Checklist

Use this checklist before publishing or exporting a GradleMC release. Releases are where sloppy projects go to embarrass themselves in public. Do not freestyle it.

---

## Release Identity

| Loader | Public version | Minecraft | Java | Expected public artifact | Notes |
| --- | --- | --- | --- | --- | --- |
| Forge | `1.0.0` | `26.1.2` | `25` | `gradlemc-forge-26.1.2-1.0.0.jar` | Forge target `26.1.2-64.0.11`; Forge `26.1.2` release |
| Forge | `1.0.2` | `1.20.1` | `17` | `gradlemc-1.0.2-forge-1.20.1.jar` | Forge target `47.4.20`; Quick Actions overlay hotfix |
| Fabric | `1.0.0` | `26.1.2` | `25` | `gradlemc-fabric-26.1.2-1.0.0.jar` | Fabric `26.1.2` release |
| Fabric | `1.0.0` | `1.20.1` | `17` | `gradlemc-fabric-1.20.1-1.0.0.jar` | Fabric `1.20.1` release |
| Quilt | `1.0.0` | `1.20.1` | `17` | `gradlemc-quilt-1.20.1-1.0.0.jar` | Quilt `1.20.1` release |

| Field | Expected |
| --- | --- |
| Mod ID | `gradlemc` |
| Product name | GradleMC |
| CurseForge Project ID | `1585182` |

Before exporting or publishing any loader build, check its metadata against the intended public release. If version, loader, artifact name, README, release notes, and jar filename disagree, stop. That is not a release; it is a bug with a ZIP extension.

---

## Pre-Release Checks

From the relevant loader source folder, run the matching build and verification commands.

Forge `26.1.2`:

```sh
cd "GradleMC/Forge/Minecraft 26.1.2"
./gradlew clean build
```

Forge `1.20.1`:

```sh
cd "GradleMC/Forge/Minecraft 1.20.1"
./gradlew clean build gradlemcSelfTest
```

Fabric `26.1.2`:

```sh
cd "GradleMC/Fabric/Minecraft 26.1.2"
./gradlew clean build
```

Fabric `1.20.1`:

```sh
cd "GradleMC/Fabric/Minecraft 1.20.1"
./gradlew clean build
```

Quilt `1.20.1`:

```sh
cd "GradleMC/Quilt/Minecraft 1.20.1"
./gradlew clean build
```

On Windows, run `gradlew.bat` from the same source folder.

Use Java `17` for the `1.20.1` builds and Java `25` for the Forge/Fabric `26.1.2` builds. Do not claim the build passed unless this was actually run and passed.

---

## Manual Smoke Test

Test with the actual release jar:

- [ ] Client launches.
- [ ] Dedicated server launches when applicable.
- [ ] `/gradlemc` help works.
- [ ] `/gradlemc gui` opens from an in-game player.
- [ ] GUI keybind opens the GUI.
- [ ] `/gradlemc status` works.
- [ ] `/gradlemc version` reports expected versions.
- [ ] `/gradlemc memory` works.
- [ ] `/gradlemc check` is permission-gated correctly.
- [ ] `/gradlemc export` writes a report.
- [ ] `/gradlemc reports latest` shows the newest report.
- [ ] `/gradlemc smart score` works.
- [ ] `/gradlemc smart advice` works.
- [ ] `/gradlemc perf start 30` and `/gradlemc perf stop` work.
- [ ] Client-only FPS tools do not load on dedicated servers.
- [ ] Overlay remains disabled by default.
- [ ] For Forge `26.1.2`, `/gradlemc version` reports Minecraft `26.1.2`, Forge, GradleMC `1.0.0`, Forge coordinate `26.1.2-64.0.11`, and Java `25` context.
- [ ] For Forge `1.20.1` `1.0.2`, the Quick Actions tab no longer overlays lower controls or text.
- [ ] For Fabric `26.1.2`, `/gradlemc version` reports Minecraft `26.1.2`, Fabric, GradleMC `1.0.0`, and Java `25` context.

---

## Export

Build first, then copy the built jar from the loader-specific build output folder.

Loader source project folders:

```text
GradleMC/Forge/Minecraft 26.1.2/
GradleMC/Forge/Minecraft 1.20.1/
GradleMC/Fabric/Minecraft 26.1.2/
GradleMC/Fabric/Minecraft 1.20.1/
GradleMC/Quilt/Minecraft 1.20.1/
```

Expected build output folders:

```text
GradleMC/Forge/Minecraft 26.1.2/build/libs/
GradleMC/Forge/Minecraft 1.20.1/build/libs/
GradleMC/Fabric/Minecraft 26.1.2/build/libs/
GradleMC/Fabric/Minecraft 1.20.1/build/libs/
GradleMC/Quilt/Minecraft 1.20.1/build/libs/
```

Verify:

- [ ] artifact exists before export;
- [ ] exported filename exactly matches the intended public artifact name;
- [ ] jar metadata reports the intended GradleMC version;
- [ ] jar metadata reports the intended loader target;
- [ ] jar metadata reports the intended Minecraft target;
- [ ] no stale old-folder paths appear in docs or scripts;
- [ ] no generated local runtime reports are accidentally committed;
- [ ] no placeholder NeoForge, Bedrock, or unlisted-version jar is produced.

---

## Screenshot And Visual Check

The current committed screenshot set lives in [`../Screenshots/`](../Screenshots/), and the full gallery is documented in [`SCREENSHOTS.md`](SCREENSHOTS.md).

Before publishing or replacing screenshots:

- [ ] screenshots are captured from the intended release jar;
- [ ] screenshots do not imply an unsupported loader or future Minecraft version;
- [ ] README preview images render on GitHub;
- [ ] `docs/SCREENSHOTS.md` includes every committed screenshot;
- [ ] `docs/SCREENSHOT_PLAN.md` matches the current screenshot paths;
- [ ] CurseForge copy is updated only if it references visuals.

---

## Public Text Check

Before publishing, check every public surface:

- [ ] README.
- [ ] CHANGELOG.
- [ ] ROADMAP.
- [ ] SUPPORT.
- [ ] SECURITY.
- [ ] `docs/SCREENSHOTS.md`.
- [ ] `docs/SCREENSHOT_PLAN.md`.
- [ ] CurseForge description.
- [ ] Release notes.
- [ ] Issue templates.
- [ ] PR template.

Confirm these claims are still true:

- [ ] Current public release targets are Forge `1.20.1`, Forge `26.1.2`, Fabric `1.20.1`, Fabric `26.1.2`, and Quilt `1.20.1`.
- [ ] Forge `26.1.2` artifact name is `gradlemc-forge-26.1.2-1.0.0.jar`.
- [ ] Forge `1.20.1` artifact name is `gradlemc-1.0.2-forge-1.20.1.jar`.
- [ ] Fabric `26.1.2` artifact name is `gradlemc-fabric-26.1.2-1.0.0.jar`.
- [ ] Fabric `1.20.1` artifact name is `gradlemc-fabric-1.20.1-1.0.0.jar`.
- [ ] Quilt artifact name is `gradlemc-quilt-1.20.1-1.0.0.jar`.
- [ ] Java `17` is stated for `1.20.1` builds.
- [ ] Java `25` is stated for Forge/Fabric `26.1.2`.
- [ ] Commands are lowercase.
- [ ] `/gradlemc gui` is lowercase.
- [ ] Adaptive diagnostics are not described as LLMs or generative AI.
- [ ] Smart Diagnostics are local rule-based diagnostics.
- [ ] Profiler language does not imply Spark parity.
- [ ] NeoForge, Bedrock, and unlisted loader/version pairs are roadmap entries, not release claims.

---

## After Release

- [ ] Tag the release if appropriate.
- [ ] Attach or publish the correct jar through the intended platform.
- [ ] Update `CHANGELOG.md`.
- [ ] Update README if artifact/version changes.
- [ ] Update support/security docs if supported targets change.
- [ ] Update screenshot docs if screenshots change.
- [ ] Update CurseForge description if public behavior or visuals change.
- [ ] Watch GitHub issues and CurseForge comments for regressions.

---

## Release Killer Conditions

Do not release if any of these are true:

- build fails;
- CI fails;
- artifact name is wrong;
- source metadata and public docs disagree;
- command casing regressed;
- dedicated server loads client-only code;
- docs claim unsupported loaders or versions;
- screenshots imply unsupported features or versions;
- old folder names or stale paths still appear;
- the release depends on “probably fine.”

“Probably fine” is not quality control. It is a bug report incubator.
