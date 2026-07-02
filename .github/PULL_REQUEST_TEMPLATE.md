## Summary

Describe what this PR changes and why.

## Type Of Change

- [ ] Bug fix
- [ ] Feature
- [ ] Documentation
- [ ] Automation / CI
- [ ] Refactor
- [ ] Release / metadata
- [ ] Security / privacy
- [ ] Screenshots / presentation

## Scope Check

- [ ] I kept Minecraft command literals lowercase.
- [ ] I did not claim unsupported loader or Minecraft-version support.
- [ ] I did not imply Fabric, NeoForge, Quilt, or future-version support unless it is fully implemented and verified.
- [ ] I did not add telemetry, analytics, cloud AI, LLMs, generative AI, embeddings, or online inference.
- [ ] I kept the change focused and avoided unrelated rewrites.
- [ ] I checked user-facing text for accuracy.

## Release / Metadata Impact

- [ ] No release-facing files changed.
- [ ] README changed.
- [ ] CHANGELOG changed.
- [ ] ROADMAP changed.
- [ ] SUPPORT / SECURITY / CONTRIBUTING changed.
- [ ] Screenshot docs changed.
- [ ] CurseForge description changed.
- [ ] Source metadata or artifact naming changed.
- [ ] CI or release automation changed.

## Testing

Paste the commands you ran:

```text

```

Suggested source checks from `GradleMC/Forge/Minecraft 1.20.1/`:

```sh
./gradlew clean build gradlemcSelfTest
```

Windows:

```bat
gradlew.bat clean build gradlemcSelfTest
```

For docs-only changes, list the files reviewed and any screenshot links checked.

## Screenshots / Clips

Add screenshots or short clips for GUI, overlay, report, or user-facing behavior changes.

Current committed screenshots live in [`Screenshots/`](../Screenshots/). The full screenshot gallery lives in [`docs/SCREENSHOTS.md`](../docs/SCREENSHOTS.md), and screenshot maintenance rules live in [`docs/SCREENSHOT_PLAN.md`](../docs/SCREENSHOT_PLAN.md).

## Review Checklist

- [ ] Logs, reports, screenshots, and issue bundles were reviewed for accidental local/environment details.
- [ ] No generated reports, local run folders, build output, or private files were committed.

## Notes For Reviewers

Mention known limitations, risk areas, follow-up work, or anything that still needs manual verification.
