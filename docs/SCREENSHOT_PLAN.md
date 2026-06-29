# GradleMC Screenshot Plan

Screenshots should be added **after `V1.0.1` finishes**. Adding screenshots before the UI and report flow stabilize is how you preserve outdated evidence forever like a fossilized mistake. 🦴

This file defines the screenshot pass so the repo is ready without pretending the visuals are final today.

---

## When To Add Screenshots

Add screenshots only after:

- [ ] `V1.0.1` behavior is final.
- [ ] GUI copy is stable.
- [ ] report/export flow is stable.
- [ ] Smart Diagnostics output is stable.
- [ ] overlay defaults are confirmed.
- [ ] no private paths, usernames, server addresses, or tokens appear in screenshots.
- [ ] README and CurseForge copy are ready to update together.

---

## Recommended Assets

Use real screenshots from the released build.

| Asset | Purpose | Suggested path |
| --- | --- | --- |
| GUI overview | Show the main control center immediately. | `docs/assets/screenshots/gui-overview.png` |
| Status panel | Show environment/status clarity. | `docs/assets/screenshots/status-panel.png` |
| Smart Diagnostics | Show score, advice, evidence, and confidence. | `docs/assets/screenshots/smart-diagnostics.png` |
| Report export | Show export confirmation or report location. | `docs/assets/screenshots/report-export.png` |
| Overlay | Show optional disabled-by-default overlay only if visually clean. | `docs/assets/screenshots/stats-overlay.png` |
| Short GIF | Show `/gradlemc gui` to report export flow. | `docs/assets/demo/gradlemc-gui-export.gif` |

---

## README Placement

After assets exist, add a `Preview` section near the top of `README.md`, after the project pitch and before `Why This Exists`.

Suggested structure:

```md
## Preview

<p align="center">
  <img src="docs/assets/screenshots/gui-overview.png" alt="GradleMC diagnostics GUI overview" width="850">
</p>

| GUI overview | Smart Diagnostics | Report export |
| --- | --- | --- |
| ![GUI overview](docs/assets/screenshots/gui-overview.png) | ![Smart Diagnostics](docs/assets/screenshots/smart-diagnostics.png) | ![Report export](docs/assets/screenshots/report-export.png) |
```

Keep the section compact. The README should sell the project, not become a museum hallway.

---

## Capture Rules

- Use a clean test instance.
- Use the official release jar.
- Use Minecraft `1.20.1` on Forge.
- Use readable GUI scale.
- Avoid cluttered modpack backgrounds.
- Avoid usernames unless they are fake test users.
- Avoid local file paths unless sanitized.
- Avoid server IPs and tokens.
- Avoid misleading staged outputs.
- Do not show unsupported Fabric, NeoForge, Quilt, or future-version builds.
- Compress images before committing.

---

## Naming Rules

Use lowercase kebab-case names:

```text
gui-overview.png
status-panel.png
smart-diagnostics.png
report-export.png
stats-overlay.png
gradlemc-gui-export.gif
```

Do not use names like:

```text
Screenshot (47).png
final_final_REAL.png
image.png
```

That naming style belongs in a haunted Downloads folder, not a repository.

---

## CurseForge Sync

When screenshots are added to GitHub:

- update README;
- update `curseforge-description.html` if it references visuals;
- keep claims consistent with the released jar;
- do not mention screenshots of features not present in the release.

Visuals are proof. Fake proof is worse than no proof.
