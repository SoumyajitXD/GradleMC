# Security Policy

GradleMC is a local Minecraft Forge diagnostics mod. Most problems will be bugs, crashes, bad configs, mod conflicts, or support workflow issues. Annoying, yes. Security issues, usually no.

Security reports are for risks that could harm players, server owners, maintainers, or the project distribution chain.

---

## Supported Versions

| Version / target | Security support |
| --- | --- |
| GradleMC `1.0.1` for Minecraft `1.20.1` Forge | Current public support target |
| Future `1.0.x` Forge `1.20.1` releases | Supported after release |
| Fabric, NeoForge, Quilt, or other Minecraft versions | Not supported until actually implemented and released |
| Unofficial mirrors, modified jars, or random ZIPs | Not supported |

Current expected release artifact:

```text
gradlemc-1.0.1-forge-1.20.1.jar
```

---

## What Counts As A Security Concern

Open a GitHub issue if you find or suspect:

- fake GradleMC downloads, mirrors, installers, or jar files;
- tampered files pretending to be official GradleMC releases;
- malicious links in issues, comments, documentation, release notes, or project pages;
- accidental exposure of tokens, passwords, private server addresses, credentials, or private files;
- project files that could mislead users into unsafe installation behavior;
- unsafe instructions that encourage broad private-file scanning, full-folder dumping, or unsupported redistribution;
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

Do **not** publicly post secrets, tokens, passwords, private server IPs, private logs, credentials, or anything that would make the situation worse by being indexed forever.

If a report contains sensitive details, redact the sensitive parts before posting.

---

## Logs, Reports, And Privacy

GradleMC tries to avoid broad private-file scans, full logs, crash reports, full config folders, mod jars, and full mods-folder dumps by default. Even so, logs and exported reports can still contain:

- usernames;
- local file paths;
- mod names;
- Java details;
- server addresses;
- runtime context;
- private modpack or server information.

Review files before posting them publicly. Evidence is useful. Accidentally exposing yourself is not a feature.

---

## License Scope

GradleMC's original repository files are licensed under **Apache-2.0** through [`LICENSE`](LICENSE). That does not relicense Minecraft, Forge, third-party mods, third-party assets, mod names, logos, screenshots containing third-party content, libraries, tools, or external project content.
