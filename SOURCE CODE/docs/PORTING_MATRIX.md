# GradleMC Porting Matrix

GradleMC should be one repository with one shared brain and thin loader/version adapters. It should not become dozens of copied projects that drift apart.

The source of truth is [`config/gradlemc-variants.json`](../config/gradlemc-variants.json). README, CurseForge copy, CI, artifact names, and porting work must follow that file.

## Support Definitions

- `supported`: implemented, buildable, CI-built, and verified through the release gates.
- `planned`: a roadmap target that may be worth implementing after common-core and adapter work.
- `experimental`: a moving or risky target kept for investigation, especially 26.x lines.
- `needs-verification`: official loader/tooling/version facts must be refreshed before planning.
- `unsupported`: not a target unless official evidence changes.
- `deprecated`: retained only for historical context.

Only variants with `enabled: true`, `buildable: true`, and `status: "supported"` are supported. Planned, experimental, unsupported, and needs-verification entries are not downloads.

## Current Support

| Variant | Minecraft | Loader | Java | Status | Enabled | Buildable | Publish |
| --- | --- | --- | --- | --- | --- | --- | --- |
| `forge-1.20.1` | `1.20.1` | Forge | 17 | supported | true | true | false |

This pass does not add Fabric, NeoForge, or extra Minecraft-version support. It only adds guardrails and planning automation.

## Roadmap Coverage

The manifest includes candidates from Minecraft `1.19.2` through `26.x` without pretending every loader exists for every version.

| Minecraft line | Forge | Fabric | NeoForge |
| --- | --- | --- | --- |
| `1.19.2` | needs-verification | planned | unsupported |
| `1.20.1` | supported | planned | needs-verification |
| `1.20.4` | needs-verification | planned | needs-verification |
| `1.20.6` | needs-verification | planned | needs-verification |
| `1.21.1` | planned | planned | planned |
| `1.21.4` | needs-verification | planned | planned |
| `1.21.8` | needs-verification | planned | planned |
| `1.21.11` | needs-verification | planned | planned |
| `26.1` | needs-verification | experimental | experimental |
| `26.1.2` | needs-verification | experimental | needs-verification |
| `26.2` | needs-verification | needs-verification | needs-verification |

## Why Not Build Everything Manually

Thirty-plus copied Gradle projects would make every command, report writer, GUI action, profiler fix, and safety rule need repeated edits. That is exactly how fake compatibility claims and broken side-safety appear.

The intended model is:

1. `common-core`: pure Java scoring, profiler math, FPS low calculations, report formatting, issue bundle models, and serialization helpers.
2. `minecraft-common` or `mc-bridge`: abstractions around game directory, loaded mods, command feedback, server tick data, client FPS data, entity/block-entity scans, and report paths.
3. Loader/version adapters: Forge, Fabric, and NeoForge entrypoints, events, config, command registration, networking, and client-only integration.

## Loader And Java Notes

- Forge 1.19.x, 1.20.1, and 1.20.x candidates are treated as Java 17 until verified per exact line.
- Minecraft 1.21.x candidates are treated as Java 21 until verified per exact loader line.
- NeoForge/current 26.x docs currently require Java 25. Keep 26.x as refreshed data, not a permanent guessed target.
- Forge uses ForgeGradle for the current 1.20.1 build. Later Forge lines may need setup changes.
- Fabric Loom now has a plugin split: use `fabric-loom-remap` for 1.21.11-or-older candidates, and `fabric-loom` for non-obfuscated 26.1+ candidates unless refreshed research proves otherwise.
- NeoForge should use current ModDevGradle/NeoGradle guidance per line. Do not assume NeoForge exists for old lines such as 1.19.2.
- Architectury and multiloader templates are useful architecture references, not code to copy blindly.
- Stonecutter-style multi-version tooling may help once common-core and bridge boundaries exist.

## Release Gates

A variant can become `supported` only after all of these are true:

- It builds successfully.
- Client launch is verified.
- Dedicated server launch is verified where applicable.
- `/gradlemc` works.
- `/gradlemc gui` works on client.
- Overlay/keybind/client GUI code does not load on dedicated server.
- Reports write to `<gameDir>/gradlemc`.
- Profiler and diagnostics commands work or are clearly disabled.
- Artifact name follows `gradlemc-<modVersion>-<loader>-<minecraftVersion>.jar`.
- CI builds the exact variant.
- README and CurseForge claims match reality.
- No fake Fabric, Forge, or NeoForge support is claimed.

## Automation

Use these commands:

```sh
./gradlew checkToolchains
./gradlew checkAutomationTools
./gradlew validateVariantMatrix
./gradlew checkProjectIdentity
./gradlew checkCommandCasing
./gradlew checkFalseSupportClaims
./gradlew printVariantMatrix
./gradlew listSupportedVariants
./gradlew listPlannedVariants
./gradlew listExperimentalVariants
./gradlew suggestNextPort
./gradlew printPortingPlan
./gradlew printVariantGaps
./gradlew generateGithubMatrix
./gradlew generateGithubMatrix -PincludeExperimental=true
./gradlew generateGithubMatrix -PincludePlanned=true
./gradlew generateVariantReadmeTable
./gradlew buildVariant -PgradlemcVariant=forge-1.20.1
./gradlew buildEnabledVariants
./gradlew assembleVariantMatrix
./gradlew checkVariantMatrix
```

`generateGithubMatrix` defaults to enabled variants only. Planned and experimental entries require explicit properties.

`buildVariant` must fail clearly for listed but not buildable variants. It must not generate placeholder jars.

Generated automation outputs are written under `build/generated/gradlemc/` and `build/reports/gradlemc/`. They are build outputs, not checked-in release artifacts.

<!-- gradlemc:variant-table:start -->
| Variant | Minecraft | Loader | Java | Status | Enabled | Buildable |
| --- | --- | --- | --- | --- | --- | --- |
| `forge-1.20.1` | `1.20.1` | forge | 17 | supported | true | true |
| `forge-1.19.2` | `1.19.2` | forge | 17 | needs-verification | false | false |
| `fabric-1.19.2` | `1.19.2` | fabric | 17 | planned | false | false |
| `neoforge-1.19.2` | `1.19.2` | neoforge | 17 | unsupported | false | false |
| `fabric-1.20.1` | `1.20.1` | fabric | 17 | planned | false | false |
| `neoforge-1.20.1` | `1.20.1` | neoforge | 17 | needs-verification | false | false |
| `forge-1.20.4` | `1.20.4` | forge | 17 | needs-verification | false | false |
| `fabric-1.20.4` | `1.20.4` | fabric | 17 | planned | false | false |
| `neoforge-1.20.4` | `1.20.4` | neoforge | 17 | needs-verification | false | false |
| `forge-1.20.6` | `1.20.6` | forge | 17 | needs-verification | false | false |
| `fabric-1.20.6` | `1.20.6` | fabric | 17 | planned | false | false |
| `neoforge-1.20.6` | `1.20.6` | neoforge | 17 | needs-verification | false | false |
| `forge-1.21.1` | `1.21.1` | forge | 21 | planned | false | false |
| `fabric-1.21.1` | `1.21.1` | fabric | 21 | planned | false | false |
| `neoforge-1.21.1` | `1.21.1` | neoforge | 21 | planned | false | false |
| `forge-1.21.4` | `1.21.4` | forge | 21 | needs-verification | false | false |
| `fabric-1.21.4` | `1.21.4` | fabric | 21 | planned | false | false |
| `neoforge-1.21.4` | `1.21.4` | neoforge | 21 | planned | false | false |
| `forge-1.21.8` | `1.21.8` | forge | 21 | needs-verification | false | false |
| `fabric-1.21.8` | `1.21.8` | fabric | 21 | planned | false | false |
| `neoforge-1.21.8` | `1.21.8` | neoforge | 21 | planned | false | false |
| `forge-1.21.11` | `1.21.11` | forge | 21 | needs-verification | false | false |
| `fabric-1.21.11` | `1.21.11` | fabric | 21 | planned | false | false |
| `neoforge-1.21.11` | `1.21.11` | neoforge | 21 | planned | false | false |
| `forge-26.1` | `26.1` | forge | 25 | needs-verification | false | false |
| `fabric-26.1` | `26.1` | fabric | 25 | experimental | false | false |
| `neoforge-26.1` | `26.1` | neoforge | 25 | experimental | false | false |
| `forge-26.1.2` | `26.1.2` | forge | 25 | needs-verification | false | false |
| `fabric-26.1.2` | `26.1.2` | fabric | 25 | experimental | false | false |
| `neoforge-26.1.2` | `26.1.2` | neoforge | 25 | needs-verification | false | false |
| `forge-26.2` | `26.2` | forge | 25 | needs-verification | false | false |
| `fabric-26.2` | `26.2` | fabric | 25 | needs-verification | false | false |
| `neoforge-26.2` | `26.2` | neoforge | 25 | needs-verification | false | false |
<!-- gradlemc:variant-table:end -->

## Next Actual Port Target

Recommended next implementation target: `fabric-1.20.1`.

Reasoning: it stays on Java 17 and close Minecraft APIs while forcing the project to prove common-core and loader-adapter boundaries. After that, `fabric-1.21.1` and `neoforge-1.21.1` are more meaningful than jumping straight into 26.x.
