# Contributing To GradleMC

Thanks for wanting to improve GradleMC. Focused contributions are welcome.

GradleMC currently has public Minecraft Java Edition `1.20.1` targets for Forge, Fabric, and Quilt, plus `26.1.2` targets for Forge and Fabric. The repository root holds public-facing docs, GitHub configuration, license, screenshots, and project assets.

---

## Before You Change Anything

Read these first:

1. [`README.md`](README.md) for the public project promise.
2. [`AGENTS.md`](AGENTS.md) for technical repository rules.
3. [`docs/RELEASE_CHECKLIST.md`](docs/RELEASE_CHECKLIST.md) before release-facing changes.
4. [`docs/SCREENSHOTS.md`](docs/SCREENSHOTS.md) and [`docs/SCREENSHOT_PLAN.md`](docs/SCREENSHOT_PLAN.md) before changing visual assets.

Unsupported ports are roadmap entries until the code, build, runtime checks, docs, screenshots, and artifact names all agree.

---

## Non-Negotiable Rules

- Keep the mod ID as `gradlemc`.
- Minecraft command literals and examples must be lowercase.
- Correct GUI command: `/gradlemc gui`.
- Current supported public release targets: Forge `1.20.1`, Forge `26.1.2`, Fabric `1.20.1`, Fabric `26.1.2`, and Quilt `1.20.1`.
- Use Java `17` for current `1.20.1` builds and Java `25` for current Forge/Fabric `26.1.2` builds.
- Current Forge `26.1.2` artifact: `gradlemc-forge-26.1.2-1.0.0.jar`.
- Current Forge `1.20.1` artifact: `gradlemc-1.0.2-forge-1.20.1.jar`.
- Current Fabric `26.1.2` artifact: `gradlemc-fabric-26.1.2-1.0.0.jar`.
- Current Fabric `1.20.1` artifact: `gradlemc-fabric-1.20.1-1.0.0.jar`.
- Current Quilt artifact: `gradlemc-quilt-1.20.1-1.0.0.jar`.
- Do not imply NeoForge, Bedrock, or unlisted loader/version support unless fully implemented and verified.
- Keep client-only code isolated from dedicated-server-safe code.
- Do not commit generated build output, local run folders, logs, private files, or exported reports.
- Prefer small, focused changes over broad rewrites.

---

## Local Setup

Current loader source paths:

```text
GradleMC/Forge/Minecraft 26.1.2/
GradleMC/Forge/Minecraft 1.20.1/
GradleMC/Fabric/Minecraft 26.1.2/
GradleMC/Fabric/Minecraft 1.20.1/
GradleMC/Quilt/Minecraft 1.20.1/
```

Build Forge `26.1.2`:

```sh
cd "GradleMC/Forge/Minecraft 26.1.2"
./gradlew build
```

Build Forge `1.20.1`:

```sh
cd "GradleMC/Forge/Minecraft 1.20.1"
./gradlew build
```

Build Fabric `26.1.2`:

```sh
cd "GradleMC/Fabric/Minecraft 26.1.2"
./gradlew build
```

Build Fabric `1.20.1`:

```sh
cd "GradleMC/Fabric/Minecraft 1.20.1"
./gradlew build
```

Build Quilt `1.20.1`:

```sh
cd "GradleMC/Quilt/Minecraft 1.20.1"
./gradlew build
```

On Windows, run `gradlew.bat` from the same source folder.

---

## Verification Checklist

For docs-only changes, manually check the edited Markdown links and any release-facing claims. If screenshots changed, confirm README and `docs/SCREENSHOTS.md` render the intended files from `Screenshots/`.

For Forge `26.1.2` source/resource changes, run:

```sh
cd "GradleMC/Forge/Minecraft 26.1.2"
./gradlew clean build
```

On Windows:

```bat
cd "GradleMC\Forge\Minecraft 26.1.2"
gradlew.bat clean build
```

For Forge `1.20.1` source/resource changes, run:

```sh
cd "GradleMC/Forge/Minecraft 1.20.1"
./gradlew clean build gradlemcSelfTest
```

On Windows:

```bat
cd "GradleMC\Forge\Minecraft 1.20.1"
gradlew.bat clean build gradlemcSelfTest
```

For Fabric or Quilt source/resource changes, run the matching loader build and verification tasks from that loader source project. Forge/Fabric `26.1.2` work must be verified from the matching `GradleMC/<Loader>/Minecraft 26.1.2/` folder with Java `25`.

Old validation commands from the removed `SOURCE CODE/` layout are not valid unless they are reintroduced in the current standalone project. Do not paste dead commands into docs, PRs, or release notes.

---

## Pull Request Expectations

A useful PR includes:

- A clear problem statement.
- A focused solution.
- Screenshots or short clips for GUI changes.
- Exact commands/tests run.
- Notes about any known limitations.

Avoid unrelated rewrites. They make review harder and usually hide bugs.

---

## Screenshot Contributions

Current screenshots live in [`Screenshots/`](Screenshots/) and are documented in [`docs/SCREENSHOTS.md`](docs/SCREENSHOTS.md).

When adding or replacing screenshots:

- use real screenshots from a supported build;
- avoid exposing local paths or sensitive values;
- keep README preview compact;
- update `docs/SCREENSHOTS.md` for the full gallery;
- update `docs/SCREENSHOT_PLAN.md` if paths, naming rules, or capture rules change.

---

## Issue Reports

Use the GitHub issue templates. Include:

- Minecraft version.
- Loader and loader version.
- GradleMC version.
- Exact GradleMC jar filename.
- Java version.
- Client/server environment.
- Reproduction steps.
- Expected behavior.
- Actual behavior.
- Relevant GradleMC report snippets.
- Relevant latest-log snippets if safe to share.

Review logs and exported reports before posting. They may include local paths, mod names, Java details, and runtime context.

---

## Good First Contributions

Good first issues usually live in:

- README clarity.
- Command help text clarity.
- GUI copy polish.
- Screenshot captions and docs polish.
- Test coverage for small pure logic components.
- Safer validation messages.
- Better issue reproduction docs.

Avoid starting with loader migrations, profiler rewrites, networking rewrites, or large feature expansions unless there is a clear plan and verification path.
