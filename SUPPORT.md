# GradleMC Support

Need help with GradleMC? Use this guide so the report is useful instead of becoming another vague modpack mystery.

---

## Where To Ask

Use GitHub issues for:

- reproducible GradleMC bugs;
- crashes or command failures related to GradleMC;
- GUI, overlay, report, profiler, Smart Diagnostics, or adaptive diagnostics problems;
- documentation mistakes;
- focused feature requests.

Use community or modpack support channels for general Minecraft or loader troubleshooting that is not clearly caused by GradleMC.

---

## Supported Release Matrix

| Loader | Minecraft | GradleMC | Java | Artifact | Loader target / notes |
| --- | --- | --- | --- | --- | --- |
| Forge | `1.21.11` | `1.0.0` | `21` | `gradlemc-forge-1.21.11-1.0.0.jar` | Forge `61.1.8` |
| Fabric | `1.21.11` | `1.0.0` | `21` | `gradlemc-fabric-1.21.11-1.0.0.jar` | Fabric Loader `0.19.3`; Fabric API `0.141.4+1.21.11` |
| NeoForge | `1.21.11` | `1.0.0` | `21` | `gradlemc-neoforge-1.21.11-1.0.0.jar` | NeoForge `21.11.42` |
| Forge | `26.1.2` | `1.0.0` | `25` | `gradlemc-forge-26.1.2-1.0.0.jar` | Forge `26.1.2-64.0.11` |
| Fabric | `26.1.2` | `1.0.0` | `25` | `gradlemc-fabric-26.1.2-1.0.0.jar` | Fabric `26.1.2` release |
| NeoForge | `26.1.2` | `1.0.0` | `25` | `gradlemc-neoforge-26.1.2-1.0.0.jar` | NeoForge `26.1.2.78` |
| Forge | `1.20.1` | `1.0.2` | `17` | `gradlemc-1.0.2-forge-1.20.1.jar` | Forge `47.4.20`; Quick Actions overlay hotfix |
| Fabric | `1.20.1` | `1.0.0` | `17` | `gradlemc-fabric-1.20.1-1.0.0.jar` | Fabric `1.20.1` release |
| Quilt | `1.20.1` | `1.0.0` | `17` | `gradlemc-quilt-1.20.1-1.0.0.jar` | Quilt `1.20.1` release |

Use the exact jar matching both the loader and Minecraft version. Renaming a different jar does not create compatibility; it creates a crash with branding.

---

## Before Opening An Issue

Check these first:

1. Your Minecraft, loader, GradleMC, Java, and artifact combination appears in the supported matrix above.
2. `/gradlemc version` reports the expected Minecraft version, loader, GradleMC version, and Java context.
3. You restarted the client or server and reproduced the issue again.
4. You tested with only GradleMC and required loader dependencies when practical.
5. You reviewed generated reports and logs before sharing them.

For the Minecraft `1.21.11` releases, explicitly mention whether you use Forge `61.1.8`, Fabric Loader `0.19.3` with Fabric API `0.141.4+1.21.11`, or NeoForge `21.11.42`.

For the Minecraft `26.1.2` releases, explicitly mention whether you use Forge `26.1.2-64.0.11`, Fabric, or NeoForge `26.1.2.78`.

---

## Useful Commands For Support

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

Minecraft commands are lowercase. Use `/gradlemc`, not `/GradleMC`.

---

## What To Include

A strong report includes:

- Minecraft version;
- loader and exact loader version;
- Java version;
- GradleMC version;
- exact GradleMC jar filename;
- client, dedicated server, or integrated server;
- whether it happens with only GradleMC installed;
- reproduction steps;
- expected behavior;
- actual behavior;
- relevant GradleMC report snippets;
- relevant latest-log snippets if safe to share;
- screenshots or short clips for GUI and overlay problems.

Do not paste huge logs unless requested. Start with the relevant part.

---

## Privacy Warning

GradleMC avoids broad private-file scans and full-folder dumps by default, but exported reports and logs can still include local paths, mod names, Java details, loader details, and runtime context.

Review anything before posting it publicly.

---

## Unsupported Requests

These are not support issues for current releases:

- Bedrock support;
- Minecraft, loader, Java, or jar combinations absent from the supported matrix;
- replacing Spark or other deep profilers;
- adding telemetry or cloud features;
- debugging an entire modpack with no reproduction steps.

Unreleased loader/version targets remain unsupported. NeoForge `1.21.11` and NeoForge `26.1.2` are supported public GradleMC releases.
