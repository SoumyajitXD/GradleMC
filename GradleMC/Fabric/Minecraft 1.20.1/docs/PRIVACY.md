# Privacy

GradleMC operates locally. It contains no telemetry, analytics, cloud AI, generative AI, hidden uploads, webhooks, remote inference, listening ports, or automatic support submission. “Adaptive diagnostics” means deterministic local rules and thresholds.

Local reports can include GradleMC/Fabric/Minecraft/Java versions, operating-system and architecture labels, bounded memory/timing observations, loaded mod metadata, normalized package/frame summaries, command/check results, and GradleMC-owned relative filenames. Mod audit reads Fabric Loader metadata and origins; it does not scan arbitrary JAR contents. Profiling uses Java stack samples and does not prove mod causation.

Issue bundles use an allowlist of GradleMC-generated summaries and reports. They exclude saves, world seeds, mod JARs, screenshots, crash reports, profile files, and external logs. Bundle entry count and size are bounded. Inputs are revalidated as regular files and redacted before inclusion.

Redaction is best-effort, not anonymity. It targets common credentials, webhook/token patterns, game/home paths, and oversized content; unusual secrets or identifying metadata can remain. Local GUI copy actions may intentionally expose the user's own full path. Review every report and ZIP before sharing it, and remove anything inappropriate for the intended recipient.
