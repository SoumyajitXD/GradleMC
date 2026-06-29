# GradleMC Release Checklist

Use this checklist before publishing or exporting a GradleMC release. Releases are where sloppy projects go to embarrass themselves in public. Do not freestyle it.

---

## Release Identity

Confirm the intended release identity:

| Field | Expected for current release |
| --- | --- |
| Mod ID | `gradlemc` |
| Product name | GradleMC |
| Version | `1.0.0` |
| Minecraft | `1.20.1` |
| Loader | Forge |
| Forge target | `47.4.20` |
| Java | `17` |
| Expected artifact | `gradlemc-1.0.0-forge-1.20.1.jar` |
| CurseForge Project ID | `1585182` |

Do not ship if the identity disagrees across `gradle.properties`, `mods.toml`, README, CurseForge copy, variant matrix, artifact name, or release automation.

---

## Pre-Release Checks

From `SOURCE CODE/`:

```sh
./gradlew checkAutomationTools validateVariantMatrix checkProjectIdentity checkCommandCasing checkFalseSupportClaims checkReleaseMetadata
```

Run the full build:

```sh
./gradlew build
```

On Windows:

```bat
gradlew.bat build
```

PowerShell wrapper validation:

```powershell
pwsh ./tools/pwsh/validate.ps1
```

Python tests after Python automation changes:

```sh
python -m unittest discover -s tools/python/tests
```

Node checks after Node/TypeScript web-facing tooling changes:

```sh
npm ci
npm run check
```

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
- [ ] Reports do not include broad private-file dumps.

---

## Export

Build first, then export.

```sh
./gradlew exportReleaseJar
```

Or export to a handoff directory:

```sh
./gradlew exportReleaseJar -PgradlemcExportDir=/path/to/output
```

Windows PowerShell:

```powershell
pwsh ./tools/pwsh/export-release.ps1 -OutputDir "C:\path\to\output"
```

Verify:

- [ ] artifact exists before export;
- [ ] artifact exists after export;
- [ ] exported filename exactly matches expected artifact name;
- [ ] no stale old-folder paths appear in docs or scripts;
- [ ] no generated local runtime reports are accidentally committed.

---

## Public Text Check

Before publishing, check every public surface:

- [ ] README.
- [ ] CHANGELOG.
- [ ] ROADMAP.
- [ ] SUPPORT.
- [ ] SECURITY.
- [ ] CurseForge description.
- [ ] Release notes.
- [ ] Issue templates.
- [ ] PR template.

Confirm these claims are still true:

- [ ] Current release target is Forge `1.20.1`.
- [ ] Java `17` is stated where needed.
- [ ] Commands are lowercase.
- [ ] `/gradlemc gui` is lowercase.
- [ ] Adaptive diagnostics are not described as LLMs or generative AI.
- [ ] Smart Diagnostics are local rule-based diagnostics.
- [ ] Profiler language does not imply Spark parity.
- [ ] Future ports are roadmap entries, not release claims.

---

## After Release

- [ ] Tag the release if appropriate.
- [ ] Attach or publish the correct jar through the intended platform.
- [ ] Update `CHANGELOG.md`.
- [ ] Update README if artifact/version changes.
- [ ] Update CurseForge description if public behavior changes.
- [ ] Add screenshots only after the `V1.0.1` visual state is final.
- [ ] Watch GitHub issues and CurseForge comments for regressions.

---

## Release Killer Conditions

Do not release if any of these are true:

- build fails;
- CI fails;
- artifact name is wrong;
- command casing regressed;
- dedicated server loads client-only code;
- docs claim unsupported loaders or versions;
- reports expose broad private data;
- release jar was built from a dirty mystery state;
- old folder names or stale paths still appear;
- the release depends on “probably fine.”

“Probably fine” is not quality control. It is a bug report incubator.
