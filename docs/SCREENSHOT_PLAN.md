# GradleMC Screenshot Guide

GradleMC has a committed screenshot set in [`../Screenshots/`](../Screenshots/), with numbered PNG assets from `0.png` through `13.png`.

The job is keeping those screenshots accurate, useful, and linked correctly.

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

A README is a landing page, not an image landfill.

---

## Capture Rules

Use real screenshots from an actual supported build. Do not use mockups unless they are clearly labeled as mockups.

Before replacing the screenshot set, confirm:

- [ ] the jar is the intended GradleMC release;
- [ ] the target appears in the current supported-release matrix;
- [ ] Minecraft `1.21.11` screenshots use Forge, Fabric, or NeoForge and Java `21`;
- [ ] Minecraft `1.20.1` screenshots use Forge, Fabric, or Quilt and Java `17`;
- [ ] Minecraft `26.1.2` screenshots use Forge, Fabric, or NeoForge and Java `25`;
- [ ] the loader version matches the selected release;
- [ ] command examples use lowercase `/gradlemc`;
- [ ] screenshots do not imply Bedrock or unlisted loader/version support;
- [ ] UI text matches checked-in language and resources;
- [ ] images remain readable without being absurdly large;
- [ ] README, `docs/SCREENSHOTS.md`, and this file are updated together.

A screenshot from Forge does not prove Fabric or NeoForge runtime behavior. Pixels are not cross-loader certification.

---

## Naming Rules

The current files are numbered:

```text
0.png
1.png
2.png
...
13.png
```

That is not elegant, but it is stable and documented.

A future focused cleanup can rename them to lowercase kebab-case names:

```text
gui-overview.png
status-panel.png
smart-diagnostics.png
report-export.png
stats-overlay.png
```

Do not leave names such as `final_final_REAL.png`, and do not rename files without updating every reference.

---

## CurseForge Sync

When screenshots change on GitHub:

- update README if the visible preview changes;
- update `docs/SCREENSHOTS.md` if assets are added, removed, renamed, or reordered;
- update `curseforge-description.html` when public copy or referenced visuals change;
- keep claims consistent with released jars;
- do not mention screenshots of features absent from the release.

Visuals are proof. Fake proof is worse than no proof.
