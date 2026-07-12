# Contributing To GradleMC

Thanks for wanting to improve GradleMC. Focused contributions are welcome.

GradleMC currently supports Minecraft Java Edition `1.21.11` on Forge, Fabric, and NeoForge; `1.20.1` on Forge, Fabric, and Quilt; and `26.1.2` on Forge and Fabric.

---

## Before You Change Anything

Read these first:

1. [`README.md`](README.md) for the public project promise.
2. [`AGENTS.md`](AGENTS.md) for technical repository rules.
3. [`docs/RELEASE_CHECKLIST.md`](docs/RELEASE_CHECKLIST.md) before release-facing changes.
4. [`docs/SCREENSHOTS.md`](docs/SCREENSHOTS.md) and [`docs/SCREENSHOT_PLAN.md`](docs/SCREENSHOT_PLAN.md) before changing visual assets.

Unsupported ports remain candidates until source metadata, builds, runtime checks, docs, and artifact names all agree.

---

## Non-Negotiable Rules

- Keep the mod ID as `gradlemc`.
- Minecraft command literals and examples must be lowercase.
- Correct GUI command: `/gradlemc gui`.
- Current supported targets are exactly those listed in the README release matrix.
- Use Java `17` for `1.20.1`, Java `21` for `1.21.11`, and Java `25` for the released `26.1.2` builds.
- Do not imply Bedrock or unlisted loader/version support.
- Keep client-only code isolated from dedicated-server-safe code.
- Do not commit generated build output, run folders, logs, private files, or exported reports.
- Prefer small, focused changes over broad rewrites.
- Do not add telemetry, analytics, cloud AI, LLM integrations, or phone-home behavior.

---

## Current Public Artifacts

```text
gradlemc-forge-1.21.11-1.0.0.jar
gradlemc-fabric-1.21.11-1.0.0.jar
gradlemc-neoforge-1.21.11-1.0.0.jar
gradlemc-forge-26.1.2-1.0.0.jar
gradlemc-fabric-26.1.2-1.0.0.jar
gradlemc-1.0.2-forge-1.20.1.jar
gradlemc-fabric-1.20.1-1.0.0.jar
gradlemc-quilt-1.20.1-1.0.0.jar
```

Do not “adapt” one artifact by renaming it. That is not porting. That is putting a fake moustache on a jar.

---

## Local Setup

Current standalone source paths:

```text
GradleMC/Forge/Minecraft 1.21.11/
GradleMC/Fabric/Minecraft 1.21.11/
GradleMC/NeoForge/Minecraft 1.21.11/
GradleMC/Forge/Minecraft 26.1.2/
GradleMC/Fabric/Minecraft 26.1.2/
GradleMC/Forge/Minecraft 1.20.1/
GradleMC/Fabric/Minecraft 1.20.1/
GradleMC/Quilt/Minecraft 1.20.1/
```

Run builds from the matching project folder:

```sh
./gradlew clean build
```

On Windows, use `gradlew.bat`. Run `gradlemcSelfTest` where the target defines it.

---

## Verification Checklist

For docs-only changes:

- manually check Markdown links;
- verify all release and Java claims against source metadata;
- verify artifact names character-for-character;
- search for stale “unsupported NeoForge” wording;
- confirm commands remain lowercase.

For source/resource changes:

- build the exact target that changed;
- run available self-tests;
- test the client when client behavior changed;
- test a dedicated server when common/server behavior changed;
- never claim runtime testing that did not happen.

Java requirements:

- `1.20.1`: Java `17`.
- `1.21.11`: Java `21`.
- released `26.1.2` targets: Java `25`.

---

## Pull Request Expectations

A useful PR includes:

- a clear problem statement;
- a focused solution;
- screenshots or short clips for GUI changes;
- exact commands and tests run;
- target loader, Minecraft version, and Java version;
- notes about known limitations.

Avoid unrelated rewrites. They make review harder and usually hide bugs.

---

## Screenshot Contributions

Current screenshots live in [`Screenshots/`](Screenshots/) and are documented in [`docs/SCREENSHOTS.md`](docs/SCREENSHOTS.md).

When adding or replacing screenshots:

- use real screenshots from a supported build;
- state which loader/version produced them;
- avoid exposing local paths or sensitive values;
- keep the README preview compact;
- update the screenshot gallery and guide together;
- do not use one loader's screenshot as proof of another loader's runtime behavior.

---

## Issue Reports

Include:

- Minecraft version;
- loader and loader version;
- GradleMC version;
- exact GradleMC jar filename;
- Java version;
- client/server environment;
- reproduction steps;
- expected behavior;
- actual behavior;
- relevant report and log snippets.

Review logs and exported reports before posting. They may include local paths, mod names, Java details, and runtime context.

---

## Good First Contributions

Good first issues usually involve:

- README and documentation clarity;
- command-help text;
- GUI copy polish;
- screenshot captions;
- tests for small pure-logic components;
- safer validation messages;
- better issue-reproduction guidance.

Avoid starting with loader migrations, profiler rewrites, networking rewrites, or large feature expansions unless there is a clear plan and verification path.
