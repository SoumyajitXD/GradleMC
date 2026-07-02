# Contributing To GradleMC

Thanks for wanting to improve GradleMC. Focused contributions are welcome.

GradleMC is currently a Minecraft Java Edition `1.20.1` Forge mod. The current standalone mod project lives in [`GradleMC/Forge/Minecraft 1.20.1/`](GradleMC/Forge/Minecraft%201.20.1/). The repository root holds public-facing docs, GitHub configuration, license, screenshots, and project assets.

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
- Current supported public release target: Forge `1.20.1`, Java `17`.
- Do not imply Fabric, NeoForge, Quilt, or non-`1.20.1` support unless fully implemented and verified.
- Do not add telemetry, analytics, remote services, generative AI, embeddings, or online inference.
- Keep client-only code isolated from dedicated-server-safe code.
- Do not commit generated build output, local run folders, logs, private files, or exported reports.
- Prefer small, focused changes over broad rewrites.

---

## Local Setup

From the repository root:

```sh
cd "GradleMC/Forge/Minecraft 1.20.1"
./gradlew build
```

On Windows:

```bat
cd "GradleMC\Forge\Minecraft 1.20.1"
gradlew.bat build
```

Use Java `17` for the current Forge `1.20.1` build.

---

## Verification Checklist

For docs-only changes, manually check the edited Markdown links and any release-facing claims. If screenshots changed, confirm README and `docs/SCREENSHOTS.md` render the intended files from `Screenshots/`.

For source/resource changes, run:

```sh
cd "GradleMC/Forge/Minecraft 1.20.1"
./gradlew clean build gradlemcSelfTest
```

On Windows:

```bat
cd "GradleMC\Forge\Minecraft 1.20.1"
gradlew.bat clean build gradlemcSelfTest
```

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

- use real screenshots from the supported Forge `1.20.1` build;
- avoid exposing local paths, private server details, usernames that should not be public, or sensitive values;
- keep README preview compact;
- update `docs/SCREENSHOTS.md` for the full gallery;
- update `docs/SCREENSHOT_PLAN.md` if paths, naming rules, or capture rules change.

---

## Issue Reports

Use the GitHub issue templates. Include:

- Minecraft version.
- Forge version.
- GradleMC version.
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
