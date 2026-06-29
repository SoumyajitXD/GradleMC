# Contributing To GradleMC

Thanks for wanting to improve GradleMC. Focused contributions are welcome.

GradleMC is currently a Minecraft Java Edition `1.20.1` Forge mod. The actual mod project lives in [`SOURCE CODE/`](SOURCE%20CODE/). The repository root holds public-facing docs, GitHub configuration, license, and project assets.

---

## Before You Change Anything

Read these first:

1. [`README.md`](README.md) for the public project promise.
2. [`AGENTS.md`](AGENTS.md) for technical repository rules.
3. `SOURCE CODE/config/gradlemc-variants.json` before touching loader/version support claims.

Unsupported ports are roadmap entries until the code, build, runtime checks, docs, and artifact names all agree.

---

## Non-Negotiable Rules

- Keep the mod id as `gradlemc`.
- Minecraft command literals and examples must be lowercase.
- Correct GUI command: `/gradlemc gui`.
- Current supported release target: Forge `1.20.1`, Java `17`.
- Do not imply Fabric, NeoForge, Quilt, or non-1.20.1 support unless fully implemented and verified.
- Do not add telemetry, analytics, remote services, generative AI, embeddings, or online inference.
- Keep client-only code isolated from dedicated-server-safe code.
- Do not commit generated build output, local run folders, logs, private files, or exported reports.
- Prefer small, focused changes over broad rewrites.

---

## Local Setup

From the repository root:

```sh
cd "SOURCE CODE"
./gradlew build
```

On Windows:

```bat
cd "SOURCE CODE"
gradlew.bat build
```

Use Java `17` for the current Forge `1.20.1` build.

---

## Verification Checklist

For docs-only changes, run the relevant lightweight checks from `SOURCE CODE/`:

```sh
./gradlew checkProjectIdentity checkCommandCasing checkFalseSupportClaims checkReleaseMetadata
```

For source/resource changes, run:

```sh
./gradlew build
```

For automation or variant matrix changes, run:

```sh
./gradlew checkAutomationTools validateVariantMatrix checkVariantMatrix
python -m unittest discover -s tools/python/tests
```

For PowerShell wrapper validation on Windows:

```powershell
pwsh ./tools/pwsh/validate.ps1
```

For Node/TypeScript web-facing asset checks when Node tooling is touched:

```sh
npm ci
npm run check
```

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
- Test coverage for small pure logic components.
- Safer validation messages.
- Better issue reproduction docs.

Avoid starting with loader migrations, profiler rewrites, networking rewrites, or large feature expansions unless there is a clear plan and verification path.
