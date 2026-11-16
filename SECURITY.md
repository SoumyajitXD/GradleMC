# Security Policy

> [!IMPORTANT]
> **Project discontinued — 16 November 2026.** GradleMC is no longer developed or maintained. No further releases, features, bug fixes, compatibility updates, or security patches are planned. Existing source code and downloads are retained for historical reference and are provided as-is. Support, issue responses, and pull-request reviews are not guaranteed.

**Security support has ended for all GradleMC versions and loaders.** No fixes, patches, response times, or advisories are guaranteed. Consider this before using the mod, especially in environments with elevated security requirements.

GradleMC is a local Minecraft diagnostics mod. Most reports are ordinary bugs, crashes, bad configurations, mod conflicts, or documentation problems. Security reports are for risks affecting users, maintainers, project distribution, dependencies, privacy, or release integrity.

---

## Historical Release Targets — All Unsupported

| Version / target | Security support |
| --- | --- |
| GradleMC `1.1.0` for Minecraft `1.20.1` Forge | **Unsupported**; formerly published, Kotlin for Forge required |
| GradleMC `1.0.0` for Minecraft `1.20.1` Fabric | **Unsupported**; historical release |
| GradleMC `1.0.0` for Minecraft `1.21.11` Forge | **Unsupported**; historical release |
| GradleMC `1.0.0` for Minecraft `1.21.11` Fabric | **Unsupported**; historical release |
| GradleMC `1.0.0` for Minecraft `1.21.11` NeoForge | **Unsupported**; historical release |
| GradleMC `1.0.0` for Minecraft `26.1.2` Forge | **Unsupported**; historical release |
| GradleMC `1.0.0` for Minecraft `26.1.2` Fabric | **Unsupported**; historical release |
| GradleMC `1.0.0` for Minecraft `26.1.2` NeoForge | **Unsupported**; historical release |
| GradleMC `1.0.0` for Minecraft `1.20.1` Quilt | **Discontinued**; no new GradleMC Quilt updates planned |
| Bedrock or unlisted loader/version combinations | Not supported |
| Unofficial mirrors, modified jars, or random ZIPs | Not supported |

Current Forge `1.20.1` artifact:

```text
gradlemc-1.1.0-forge-1.20.1.jar
```

This release is implemented in Kotlin and requires **Kotlin for Forge**. Minecraft `1.20.1` still requires Java `17` at runtime.

The Quilt line is discontinued. Existing legacy Quilt artifacts may remain downloadable, but users should not expect new GradleMC Quilt maintenance or security releases.

---

## Dependency And Supply-Chain Notes

For Forge `1.20.1` `v1.1.0`, Kotlin for Forge is part of the expected runtime dependency chain. Dependency confusion, malicious mirrors, tampered Kotlin-for-Forge downloads, or documentation that directs users to an unofficial dependency source can be security concerns.

A filename alone is not proof that a jar is official. Verify release source, project identity, and expected loader/Minecraft target.

---

## What Counts As A Security Concern

The following remain security concerns, but this discontinued project does not promise investigation or fixes:

- fake GradleMC downloads, mirrors, installers, or jar files;
- tampered files pretending to be official GradleMC releases;
- malicious or misleading dependency links;
- project links or documentation that could direct users to unsafe downloads;
- accidental exposure of sensitive project or user information;
- unexpected network/telemetry behavior attributed to GradleMC;
- supply-chain concerns involving dependencies, release references, build artifacts, attribution, or license scope.

Bring evidence: links, filenames, hashes if available, screenshots if useful, where you found the problem, and why it appears suspicious.

---

## What Is Usually Not A Security Concern

These belong in normal support or bug channels:

- ordinary crashes;
- missing Kotlin for Forge causing `v1.1.0` not to load;
- command failures;
- GUI problems;
- bad configuration behavior;
- mod conflicts;
- low FPS, lag, or memory pressure;
- server startup failures unrelated to a security boundary;
- missing reports;
- unsupported loader/Minecraft-version requests;
- requests for new Quilt versions;
- issues caused by modified jars or unofficial modpacks.

---

## Reporting Security Issues

For reports that are safe to discuss publicly, GitHub Issues remains an option for historical public documentation, but review or response is not guaranteed:

https://github.com/SoumyajitXD/GradleMC/issues

Do not post secrets or sensitive personal data publicly. Redact private information before sharing logs, reports, or screenshots.

---

## Logs, Reports, And Privacy

GradleMC is designed without behavioural telemetry, analytics tracking, cloud AI, or automatic report uploads. However, locally generated logs and diagnostic reports can contain local paths, mod names, Java/JVM details, loader information, runtime context, and server/modpack information.

Review exported evidence before sharing it.

---

## Repository Source Note

The Forge `1.20.1` source currently checked into `main` is still the legacy Java `v1.0.4` tree. It is not the Kotlin `v1.1.0` source. Security reviews of the published `v1.1.0` artifact should not assume that the legacy checked-in Java tree is byte-for-byte representative of that release until the Kotlin source has been synchronized.

---

## License Scope

GradleMC's original repository files are licensed under **Apache-2.0** through [`LICENSE`](LICENSE). That does not relicense Minecraft, Forge, Fabric, NeoForge, Quilt, Kotlin for Forge, third-party mods, libraries, third-party assets, screenshots containing third-party content, tools, or external project content.
