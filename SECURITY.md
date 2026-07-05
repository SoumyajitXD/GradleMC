# Security Policy

GradleMC is a local Minecraft diagnostics mod. Most reports are normal bugs, crashes, bad configs, mod conflicts, or documentation issues. Security reports are for risks affecting users, maintainers, project distribution, or release integrity.

---

## Supported Versions

| Version / target | Security support |
| --- | --- |
| GradleMC `1.0.2` for Minecraft `1.20.1` Forge | Current public support target |
| GradleMC `1.0.0` for Minecraft `26.1.2` Fabric | Current public support target |
| GradleMC `1.0.0` for Minecraft `1.20.1` Fabric | Current public support target |
| GradleMC `1.0.0` for Minecraft `1.20.1` Quilt | Current public support target |
| Future `1.0.x` releases for listed supported targets | Supported after release |
| NeoForge, Bedrock, or unlisted loader/version combinations | Not supported until actually implemented and released |
| Unofficial mirrors, modified jars, or random ZIPs | Not supported |

Current expected release artifacts:

```text
gradlemc-1.0.2-forge-1.20.1.jar
gradlemc-fabric-26.1.2-1.0.0.jar
gradlemc-fabric-1.20.1-1.0.0.jar
gradlemc-quilt-1.20.1-1.0.0.jar
```

---

## What Counts As A Security Concern

Open a GitHub issue if you find or suspect:

- fake GradleMC downloads, mirrors, installers, or jar files;
- tampered files pretending to be official GradleMC releases;
- project links or documentation that could mislead users;
- accidental exposure of sensitive project or user information;
- supply-chain concerns around release references, build artifacts, attribution, dependency metadata, or license scope.

Bring evidence: links, filenames, hashes if available, screenshots if useful, where you found the problem, and what made it suspicious.

---

## What Is Usually Not A Security Concern

These belong in normal support or bug channels:

- crashes;
- command failures;
- GUI problems;
- bad config behavior;
- mod conflicts;
- low FPS, lag, or memory pressure;
- server startup failures;
- missing reports;
- unsupported loader or Minecraft-version requests;
- issues caused by modified jars, unofficial packs, extra mods, or local experiments.

If the issue only affects gameplay, diagnostics behavior, stability, or documentation, file it as a normal bug with logs and reproduction steps.

---

## Reporting Security Issues

For reports that are safe to discuss publicly, use GitHub Issues:

- https://github.com/SoumyajitXD/GradleMC/issues

Avoid posting sensitive details publicly. Redact anything private before sharing logs, reports, or screenshots.

---

## Logs, Reports, And Privacy

Logs and exported reports can include local paths, mod names, Java details, loader details, runtime context, and server or modpack information. Review files before posting them publicly.

---

## License Scope

GradleMC's original repository files are licensed under **Apache-2.0** through [`LICENSE`](LICENSE). That does not relicense Minecraft, Forge, Fabric, Quilt, third-party mods, third-party assets, mod names, logos, screenshots containing third-party content, libraries, tools, or external project content.
