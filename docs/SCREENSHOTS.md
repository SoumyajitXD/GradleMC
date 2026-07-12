# GradleMC Screenshots

This page is the full visual inventory for the screenshots currently committed to the repository.

The screenshot assets live in [`../Screenshots/`](../Screenshots/). The folder name is capitalized, and GitHub paths are case-sensitive.

---

## README Preview Set

The README uses a compact preview so the landing page does not become an image dump.

<p align="center">
  <img src="../Screenshots/0.png" alt="GradleMC screenshot 0" width="900">
</p>

| README thumbnail 1 | README thumbnail 2 | README thumbnail 3 |
| --- | --- | --- |
| ![GradleMC screenshot 1](../Screenshots/1.png) | ![GradleMC screenshot 2](../Screenshots/2.png) | ![GradleMC screenshot 3](../Screenshots/3.png) |

---

## Full Screenshot Gallery

| # | Screenshot |
| ---: | --- |
| 0 | ![GradleMC screenshot 0](../Screenshots/0.png) |
| 1 | ![GradleMC screenshot 1](../Screenshots/1.png) |
| 2 | ![GradleMC screenshot 2](../Screenshots/2.png) |
| 3 | ![GradleMC screenshot 3](../Screenshots/3.png) |
| 4 | ![GradleMC screenshot 4](../Screenshots/4.png) |
| 5 | ![GradleMC screenshot 5](../Screenshots/5.png) |
| 6 | ![GradleMC screenshot 6](../Screenshots/6.png) |
| 7 | ![GradleMC screenshot 7](../Screenshots/7.png) |
| 8 | ![GradleMC screenshot 8](../Screenshots/8.png) |
| 9 | ![GradleMC screenshot 9](../Screenshots/9.png) |
| 10 | ![GradleMC screenshot 10](../Screenshots/10.png) |
| 11 | ![GradleMC screenshot 11](../Screenshots/11.png) |
| 12 | ![GradleMC screenshot 12](../Screenshots/12.png) |
| 13 | ![GradleMC screenshot 13](../Screenshots/13.png) |

---

## What These Screenshots Prove

These screenshots are evidence for the documented GradleMC UI and user-facing flow. A screenshot captured from one loader or Minecraft version must not be used as proof that another target was runtime-tested.

Current public release families are:

- Forge, Fabric, and NeoForge for Minecraft `1.21.11`;
- Forge, Fabric, and Quilt for Minecraft `1.20.1`;
- Forge, Fabric, and NeoForge for Minecraft `26.1.2`.

The screenshots do **not** prove support for Bedrock, cloud AI, telemetry, or any loader/version pair absent from the supported-release matrix. Screenshots are evidence, not marketing fog.

---

## Maintenance Rules

- Keep screenshot links relative so they render on forks and branches.
- Keep the README preview small; use this page for the full gallery.
- If screenshots are renamed, update `README.md`, this file, and `docs/SCREENSHOT_PLAN.md` in the same commit.
- If screenshots are recaptured, verify the release jar, Minecraft version, loader version, Java version, and command casing first.
- Prefer descriptive filenames in future cleanup work, but only rename files when every reference is updated.

---

## Future Naming Cleanup

The current screenshot set uses numbered files: `0.png` through `13.png`. That is acceptable because the links are documented and stable.

A future focused cleanup may rename them to descriptive lowercase kebab-case names such as:

```text
gui-overview.png
status-panel.png
smart-diagnostics.png
report-export.png
stats-overlay.png
```

Do not casually rename screenshot files while changing unrelated code.
