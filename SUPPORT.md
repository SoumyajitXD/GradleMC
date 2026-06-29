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

Use discussions, community posts, or modpack support channels for general Minecraft or Forge troubleshooting that is not clearly caused by GradleMC.

---

## Before Opening An Issue

Check these first:

1. You are using Minecraft Java Edition `1.20.1`.
2. You are using Forge `47.4.20` or a compatible Forge `47.x` setup.
3. You are using Java `17`.
4. You are using a GradleMC jar named like `gradlemc-1.0.0-forge-1.20.1.jar`.
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

Also mention whether the GUI keybind opens the screen.

---

## What To Include

A strong report includes:

- Minecraft version.
- Forge version.
- Java version.
- GradleMC version.
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

These are not support issues for the current release:

- Fabric support.
- NeoForge support.
- Quilt support.
- Minecraft versions other than `1.20.1`.
- Replacing Spark or other deep profilers.
- Adding telemetry or cloud features.
- Debugging an entire modpack with no reproduction steps.

Those may be roadmap discussions, but they are not current-release bug reports.
