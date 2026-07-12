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
- [ ] I checked the current public release matrix in `README.md`.
- [ ] I treated Forge, Fabric, and NeoForge `1.21.11` as released targets.
- [ ] I did not imply Bedrock or unlisted loader/version support.
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

Run the matching standalone project build:

```sh
./gradlew clean build
```

Use Java `17` for Minecraft `1.20.1`, Java `21` for Minecraft `1.21.11`, and Java `25` for released Minecraft `26.1.2` targets. Run `gradlemcSelfTest` where available.

For docs-only changes, list the files reviewed, release metadata checked, and screenshot links verified.

## Screenshots / Clips

Add screenshots or short clips for GUI, overlay, report, or user-facing behavior changes.

Current committed screenshots live in [`Screenshots/`](../Screenshots/). The full gallery lives in [`docs/SCREENSHOTS.md`](../docs/SCREENSHOTS.md), and maintenance rules live in [`docs/SCREENSHOT_PLAN.md`](../docs/SCREENSHOT_PLAN.md).

## Review Checklist

- [ ] I reviewed release-facing claims, screenshots, reports, and generated outputs before committing.
- [ ] I did not commit generated build output, run folders, logs, or private reports.
- [ ] Artifact names, loader versions, Minecraft versions, and Java versions agree.

## Notes For Reviewers

Mention known limitations, risk areas, follow-up work, or anything that still needs manual verification.
