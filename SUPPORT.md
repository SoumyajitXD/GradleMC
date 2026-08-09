# GradleMC Support

Need help with GradleMC? Use this guide so reports contain evidence instead of becoming another vague modpack mystery.

---

## Latest Supported Forge 1.20.1 Release

GradleMC `v1.1.0` for Forge `1.20.1`:

- Artifact: `gradlemc-1.1.0-forge-1.20.1.jar`
- Java runtime: `17`
- GradleMC implementation: **Kotlin**
- Required mod dependency: **Kotlin for Forge**

The GradleMC Java implementation used by older Forge `1.20.1` releases was replaced for `v1.1.0`. Minecraft/Forge still requires Java `17`; do not remove the Java runtime because GradleMC itself is now written in Kotlin.

If `v1.1.0` fails before GradleMC initializes, verify that Kotlin for Forge is installed before treating it as a GradleMC bug.

---

## Where To Ask

Use GitHub issues for:

- reproducible GradleMC bugs;
- crashes or command failures caused by GradleMC;
- GUI, overlay, report, profiler, Smart Diagnostics, or adaptive-diagnostics problems;
- documentation mistakes;
- focused feature requests.

Use community/modpack support channels for general Minecraft, loader, Java, or modpack troubleshooting that is not clearly caused by GradleMC.

---

## Release Matrix

| Status | Loader | Minecraft | GradleMC | Java | Dependency / notes | Artifact |
| --- | --- | --- | --- | --- | --- | --- |
| **Latest** | Forge | `1.20.1` | `1.1.0` | `17` | **Kotlin for Forge required** | `gradlemc-1.1.0-forge-1.20.1.jar` |
| Published | Fabric | `1.20.1` | `1.0.0` | `17` | Fabric release | `gradlemc-fabric-1.20.1-1.0.0.jar` |
| Published | Forge | `1.21.11` | `1.0.0` | `21` | Forge `61.1.8` | `gradlemc-forge-1.21.11-1.0.0.jar` |
| Published | Fabric | `1.21.11` | `1.0.0` | `21` | Fabric Loader `0.19.3`; Fabric API `0.141.4+1.21.11` | `gradlemc-fabric-1.21.11-1.0.0.jar` |
| Published | NeoForge | `1.21.11` | `1.0.0` | `21` | NeoForge `21.11.42` | `gradlemc-neoforge-1.21.11-1.0.0.jar` |
| Published | Forge | `26.1.2` | `1.0.0` | `25` | Forge `26.1.2-64.0.11` | `gradlemc-forge-26.1.2-1.0.0.jar` |
| Published | Fabric | `26.1.2` | `1.0.0` | `25` | Fabric release | `gradlemc-fabric-26.1.2-1.0.0.jar` |
| Published | NeoForge | `26.1.2` | `1.0.0` | `25` | NeoForge `26.1.2.78` | `gradlemc-neoforge-26.1.2-1.0.0.jar` |
| **Discontinued** | Quilt | `1.20.1` | `1.0.0` | `17` | Legacy only; no new GradleMC Quilt updates | `gradlemc-quilt-1.20.1-1.0.0.jar` |

Use the exact jar matching both the loader and Minecraft version. Renaming a different jar does not create compatibility; it creates a crash with better branding.

---

## Quilt Support

Quilt development has ended. Existing Quilt `1.20.1` files may remain available as legacy downloads, but no new GradleMC versions, fixes, or feature ports are planned for Quilt.

Requests for new Quilt versions should therefore be treated as requests to reverse the loader-support decision, not as ordinary missing-version bugs.

---

## Before Opening An Issue

Check these first:

1. Your Minecraft, loader, GradleMC, and Java combination matches a published release above.
2. For Forge `1.20.1` `v1.1.0`, **Kotlin for Forge is installed**.
3. `/gradlemc version` reports the expected Minecraft version, loader, GradleMC version, and JVM context when the command is available.
4. You restarted the client or server and reproduced the issue again.
5. You tested with GradleMC plus its required loader/runtime dependencies when practical.
6. You reviewed generated reports and logs before sharing them.

For Forge `1.20.1` `v1.1.0`, include whether the failure occurs before or after GradleMC loads. Missing Kotlin for Forge is a dependency/setup failure, not evidence of a GradleMC diagnostics bug.

---

## Useful Commands

```text
/gradlemc status
/gradlemc version
/gradlemc memory
/gradlemc check
/gradlemc export
/gradlemc reports latest
```

For supported performance/Smart Diagnostics workflows:

```text
/gradlemc perf start 30
/gradlemc smart score
/gradlemc smart advice
```

For GUI issues:

```text
/gradlemc gui
```

Minecraft commands are lowercase. Use `/gradlemc`, not `/GradleMC`.

Command availability can vary by release and side. Use `/gradlemc` to inspect the command tree supplied by the exact installed build.

---

## What To Include

A strong report includes:

- Minecraft version;
- loader and exact loader version;
- Java version;
- GradleMC version;
- exact GradleMC jar filename;
- required dependency versions, especially Kotlin for Forge for Forge `1.20.1` `v1.1.0`;
- client, dedicated server, or integrated server;
- whether GradleMC reaches initialization;
- reproduction steps;
- expected behavior;
- actual behavior;
- relevant GradleMC report snippets;
- relevant log snippets if safe to share;
- screenshots or short clips for GUI/overlay problems.

Do not paste enormous logs by default. Start with the relevant evidence.

---

## Source-Code Note For v1.1.0

The Forge `1.20.1` project currently checked into `main` is the legacy Java `v1.0.4` source, not the Kotlin `v1.1.0` source. Do not use that old source tree to reproduce a `v1.1.0` build until the Kotlin rewrite has been synchronized to GitHub.

---

## Privacy Warning

GradleMC avoids telemetry and automatic report uploads, but exported reports and logs can still contain local paths, mod names, JVM details, loader information, instance details, and runtime context.

Review anything before posting it publicly.

---

## Unsupported Requests

These are not normal support targets:

- Bedrock support;
- unlisted Minecraft/loader combinations;
- new Quilt versions or Quilt feature ports;
- replacing Spark or other deep profilers;
- adding telemetry, cloud AI, or analytics;
- debugging an entire modpack with no reproduction steps.
