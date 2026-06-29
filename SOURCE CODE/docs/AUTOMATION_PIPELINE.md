# GradleMC Automation Pipeline

GradleMC uses several languages, but each one has a narrow job. Gradle remains the main entrypoint; helpers exist only where they make validation easier to read or easier to run on Windows.

## Language Roles

- Java owns the Minecraft mod, Forge 1.20.1 integration, common-core candidates, and JVM tests.
- Gradle owns build orchestration, variant task entrypoints, Java toolchain selection, and CI parity.
- Python 3.12+ owns variant manifest validation, matrix generation, docs table generation, claim checks, command casing checks, and readable diagnostics.
- PowerShell 7 owns Windows-first local wrappers around Gradle and Python checks.
- Node.js and TypeScript are optional. They are not used in the current pipeline because Python handles the JSON and Markdown work without extra dependencies.

Python 3.12 is the baseline because it is modern, widely available in CI, and includes the standard-library features this tooling needs. Python 3.13 can be used locally and in CI, but Python 3.14-only features are not allowed.

## Minimum Local Tools

- Java 17 for Forge 1.20.1 and other 1.19.2/1.20.x candidates.
- Java 21 for 1.21.x candidates.
- Java 25 for 26.x candidates that declare it in `config/gradlemc-variants.json`.
- Python 3.12 or newer.
- PowerShell 7 for `tools/pwsh/*.ps1`.
- Node.js only if a future `package.json` is added for justified tooling.

## Local Commands

Windows PowerShell:

```powershell
pwsh ./tools/pwsh/check-env.ps1
pwsh ./tools/pwsh/validate.ps1
pwsh ./tools/pwsh/build-variant.ps1 -Variant forge-1.20.1
pwsh ./tools/pwsh/generate-matrix.ps1
pwsh ./tools/pwsh/export-release.ps1
```

Gradle:

```sh
./gradlew checkToolchains
./gradlew validateVariantMatrix
./gradlew printVariantMatrix
./gradlew checkAutomationTools
./gradlew checkProjectIdentity
./gradlew checkCommandCasing
./gradlew checkFalseSupportClaims
./gradlew checkReleaseMetadata
./gradlew generateGithubMatrix
./gradlew buildVariant -PgradlemcVariant=forge-1.20.1
./gradlew buildEnabledVariants
./gradlew exportReleaseJar
```

Python:

```sh
python -m gradlemc_automation.validate_variants
python -m gradlemc_automation.generate_github_matrix
python -m gradlemc_automation.generate_docs_tables
python -m gradlemc_automation.check_claims
python -m gradlemc_automation.check_command_casing
python -m gradlemc_automation.validate_release
```

## Release Export

The intended release artifact is `gradlemc-1.0.0-forge-1.20.1.jar`.

`./gradlew exportReleaseJar` builds the current supported Forge artifact, validates release metadata, copies the jar to `build/exports/`, verifies the exported file exists, and prints the final path. Use `-PgradlemcExportDir=<path>` to export to a specific local handoff folder.

`pwsh ./tools/pwsh/export-release.ps1 -OutputDir <path>` is the Windows wrapper for the same Gradle task. The wrapper also validates the exported jar contents with `python -m gradlemc_automation.validate_release --artifact <jar>`.

## Generated Outputs

Validation and generation tasks write only under `build/` by default:

- `build/generated/gradlemc/variant-matrix.json`
- `build/generated/gradlemc/github-matrix.json`
- `build/generated/gradlemc/variant-table.md`
- `build/reports/gradlemc/automation-report.txt`

These files are build outputs and should not be committed unless the project explicitly changes that convention. Checked-in docs are updated only by `./gradlew updateVariantDocs`.

## Adding A Variant Safely

1. Add a disabled entry to `config/gradlemc-variants.json`.
2. Set `status`, `enabled`, `buildable`, `javaVersion`, `gradlePluginKind`, `artifactName`, and `reasonDisabled` honestly.
3. Run `./gradlew validateVariantMatrix printVariantMatrix`.
4. Add build adapter work only after common-core and bridge boundaries are ready.
5. Promote to `supported` only after build, client launch, server launch, command smoke, GUI smoke, report path, docs, and CI gates pass.

## Avoiding Fake Support Claims

Only `enabled: true`, `buildable: true`, and `status: "supported"` counts as support. Planned, experimental, unsupported, and needs-verification entries are roadmap data, not downloads. Docs may say Fabric or NeoForge are planned, but must not say they are supported until the manifest and build prove it.

## CI Parity

GitHub Actions mirrors the local flow:

- `automation-validation` checks tools, manifest, identity, command casing, false support claims, and GitHub matrix generation.
- `build-enabled-variants` builds enabled/buildable variants only.
- `python-tooling-test` runs Python automation tests on Python 3.12 and 3.13 where available.
- `node-tooling-test` runs only if `package.json` exists.
- `powershell-wrapper-test` runs the Windows PowerShell wrappers.

No CI job publishes, exports release jars, or creates placeholder variant jars.
