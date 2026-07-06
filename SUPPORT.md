# GradleMC Support

Need help with GradleMC? Use this guide so the report is useful instead of becoming another vague modpack mystery.

---

## Where To Ask

Use GitHub issues for:

- Reproducible bugs.
- Crashes or command failures related to GradleMC.
- GUI, overlay, report, profiler, Smart Diagnostics, or adaptive diagnostics problems.
- Documentation mistakes.
- Focused feature requests.

Use discussions, community posts, or modpack support channels for general Minecraft, Forge, Fabric, or Quilt troubleshooting that is not clearly caused by GradleMC.

---

## Before Opening An Issue

Check these first:

1. You are using a supported Minecraft Java Edition target:
   - `1.20.1` for Forge, Fabric, or Quilt.
   - `26.1.2` for Forge or Fabric.
2. You are using a supported loader setup:
   - Forge `47.4.20` or a compatible Forge `47.x` setup for Minecraft `1.20.1`.
   - Forge `26.1.2-64.0.11` for Minecraft `26.1.2`.
   - Fabric for Minecraft `1.20.1` or `26.1.2`.
   - Quilt for Minecraft `1.20.1`.
3. You are using the correct Java version:
   - Java `17` for the Minecraft `1.20.1` releases.
   - Java `25` for the Forge/Fabric `26.1.2` releases.
4. You are using the correct GradleMC jar for your loader and Minecraft version:
   - Forge `26.1.2`: `gradlemc-forge-26.1.2-1.0.0.jar`.
   - Forge `1.20.1`: `gradlemc-1.0.2-forge-1.20.1.jar`.
   - Fabric `26.1.2`: `gradlemc-fabric-26.1.2-1.0.0.jar`.
   - Fabric `1.20.1`: `gradlemc-fabric-1.20.1-1.0.0.jar`.
   - Quilt `1.20.1`: `gradlemc-quilt-1.20.1-1.0.0.jar`.
5. You tried the relevant command again after restarting the instance or server.
6. You reviewed the GradleMC report before sharing it.

---

## Useful Commands For Support

Run these in game when possible:

```text
/gradlemc status
/gradlemc version
/gradlemc memory
/gradlemc check
/gradlemc export
/gradlemc reports latest
```

For performance-related issues:

```text
/gradlemc perf start 30
/gradlemc perf stop
/gradlemc smart score
/gradlemc smart advice
/gradlemc smart explain
```

For GUI issues:

```text
/gradlemc gui
```

Also mention whether the GUI keybind opens the screen. For Forge `1.20.1` `1.0.2`, include screenshots or clips if the Quick Actions tab still shows overlay or layout problems. For Forge `26.1.2` `1.0.0`, mention Java `25`, Forge `26.1.2-64.0.11`, and the exact jar filename.

---

## What To Include

A strong report includes:

- Minecraft version.
- Loader and loader version.
- Java version.
- GradleMC version.
- Exact GradleMC jar filename.
- Client, dedicated server, or integrated server.
- Whether it happens with only GradleMC installed.
- Reproduction steps.
- Expected behavior.
- Actual behavior.
- Relevant GradleMC report snippets.
- Relevant latest-log snippets if safe to share.
- Screenshots or short clips for GUI/overlay problems.

Do not paste huge logs unless requested. Start with the relevant part.

---

## Privacy Warning

GradleMC tries to avoid broad private-file scans and full-folder dumps by default, but exported reports and logs can still include local paths, mod names, Java details, and runtime context.

Review anything before posting it publicly.

---

## Unsupported Requests

These are not support issues for the current releases:

- NeoForge support.
- Bedrock support.
- Minecraft, loader, or Java combinations not listed in the supported release matrix.
- Replacing Spark or other deep profilers.
- Adding telemetry or cloud features.
- Debugging an entire modpack with no reproduction steps.

Those may be roadmap discussions, but they are not current-release bug reports.
