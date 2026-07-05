# GradleMC Screenshot Guide

GradleMC now has a committed screenshot set in [`../Screenshots/`](../Screenshots/), with numbered PNG assets from `0.png` through `13.png`.

This file is no longer only a future capture plan. The screenshots exist, so the job is keeping them accurate, useful, and linked correctly.

---

## Current Screenshot Locations

| Asset | Path | Used by |
| --- | --- | --- |
| Main preview | `Screenshots/0.png` | `README.md`, `docs/SCREENSHOTS.md` |
| README thumbnail 1 | `Screenshots/1.png` | `README.md`, `docs/SCREENSHOTS.md` |
| README thumbnail 2 | `Screenshots/2.png` | `README.md`, `docs/SCREENSHOTS.md` |
| README thumbnail 3 | `Screenshots/3.png` | `README.md`, `docs/SCREENSHOTS.md` |
| Full gallery assets | `Screenshots/0.png` through `Screenshots/13.png` | `docs/SCREENSHOTS.md` |

The full gallery lives in [`SCREENSHOTS.md`](SCREENSHOTS.md).

---

## README Placement

The README should stay compact:

1. one large preview image;
2. three thumbnails;
3. a link to `docs/SCREENSHOTS.md` for the complete gallery.

Do not paste all fourteen screenshots into the README. A README is a landing page, not an image dump.

---

## Capture Rules

Use real screenshots from the actual supported build. Do not use mockups unless they are clearly labeled as mockups.

Before replacing the screenshot set, confirm:

- [ ] the release jar being shown is the intended GradleMC release;
- [ ] the Minecraft version is a currently supported GradleMC target, such as Forge/Fabric/Quilt `1.20.1` or Fabric `26.1.2`;
- [ ] the loader shown is a currently supported loader/version pair;
- [ ] the Java version matches the target: Java `17` for `1.20.1`, Java `25` for Fabric `26.1.2`;
- [ ] command examples use lowercase `/gradlemc`;
- [ ] screenshots do not imply NeoForge, Bedrock, or unlisted loader/version support;
- [ ] UI text matches the checked-in language/resources;
- [ ] images are compressed enough for GitHub without becoming unreadable;
- [ ] README, `docs/SCREENSHOTS.md`, and this file are updated together.

---

## Naming Rules

The current committed files are numbered:

```text
0.png
1.png
2.png
...
13.png
```

That is not elegant, but it is stable and documented.

Future cleanup can rename them to lowercase kebab-case names:

```text
gui-overview.png
status-panel.png
smart-diagnostics.png
report-export.png
stats-overlay.png
```

Do not leave names like:

```text
Screenshot (47).png
final_final_REAL.png
image.png
```

Also do not rename files without updating every reference. Renaming one file and leaving stale links is not cleanup.

---

## CurseForge Sync

When screenshots change on GitHub:

- update README if the visible preview changes;
- update `docs/SCREENSHOTS.md` if assets are added, removed, renamed, or reordered;
- update `curseforge-description.html` only if the public description references visuals or needs matching screenshots;
- keep claims consistent with the released jar;
- do not mention screenshots of features that are not present in the release.

Visuals are proof. Fake proof is worse than no proof.
