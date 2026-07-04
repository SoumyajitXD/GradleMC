# GradleMC Release Checklist

Use this checklist before publishing or exporting a GradleMC release. Releases are where sloppy projects go to embarrass themselves in public. Do not freestyle it.

---

## Release Identity

| Loader | Public version | Minecraft | Java | Expected public artifact | Notes |
| --- | --- | --- | --- | --- | --- |
| Forge | `1.0.2` | `1.20.1` | `17` | `gradlemc-1.0.2-forge-1.20.1.jar` | Forge target `47.4.20`; Quick Actions overlay hotfix |
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

From the currently documented Forge source folder:

```sh
cd "GradleMC/Forge/Minecraft 1.20.1"
./gradlew clean build gradlemcSelfTest
```

On Windows:

```bat
cd "GradleMC\Forge\Minecraft 1.20.1"
gradlew.bat clean build gradlemcSelfTest
```

Use Java `17`. Do not claim the build passed unless this was actually run and passed.

For Fabric or Quilt release work, run the equivalent loader build and verification commands from that loader source project. Do not paste Forge commands into other loader release notes and call it done.

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
- [ ] For Forge `1.0.2`, the Quick Actions tab no longer overlays lower controls or text.

---

## Export

Build first, then copy the built jar from the loader-specific build output folder.

For the currently documented Forge source project, build output is written under:

```text
GradleMC/Forge/Minecraft 1.20.1/build/libs/
```

Loader source project folders:

```text
GradleMC/Forge/Minecraft 1.20.1/
GradleMC/Fabric/Minecraft 1.20.1/
GradleMC/Quilt/Minecraft 1.20.1/
```

Verify:

- [ ] artifact exists before export;
- [ ] exported filename exactly matches the intended public artifact name;
- [ ] jar metadata reports the intended GradleMC version;
- [ ] jar metadata reports the intended loader target;
- [ ] no stale old-folder paths appear in docs or scripts;
- [ ] no generated local runtime reports are accidentally committed;
- [ ] no placeholder NeoForge, Bedrock, or future-version jar is produced.

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

- [ ] Current public release targets are Forge `1.20.1`, Fabric `1.20.1`, and Quilt `1.20.1`.
- [ ] Forge artifact name is `gradlemc-1.0.2-forge-1.20.1.jar`.
- [ ] Fabric artifact name is `gradlemc-fabric-1.20.1-1.0.0.jar`.
- [ ] Quilt artifact name is `gradlemc-quilt-1.20.1-1.0.0.jar`.
- [ ] Java `17` is stated where needed.
- [ ] Commands are lowercase.
- [ ] `/gradlemc gui` is lowercase.
- [ ] Adaptive diagnostics are not described as LLMs or generative AI.
- [ ] Smart Diagnostics are local rule-based diagnostics.
- [ ] Profiler language does not imply Spark parity.
- [ ] NeoForge, Bedrock, and future Minecraft versions are roadmap entries, not release claims.

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
