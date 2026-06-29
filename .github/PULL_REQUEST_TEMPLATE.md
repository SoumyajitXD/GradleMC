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
- [ ] I did not broaden private-file scanning or report exports beyond the intended modpack/report scope.
- [ ] I kept the change focused and avoided unrelated rewrites.
- [ ] I checked user-facing text for accuracy.

## Release / Metadata Impact

- [ ] No release-facing files changed.
- [ ] README changed.
- [ ] CHANGELOG changed.
- [ ] ROADMAP changed.
- [ ] SUPPORT / SECURITY / CONTRIBUTING changed.
- [ ] CurseForge description changed.
- [ ] Variant matrix or artifact naming changed.
- [ ] CI or release automation changed.

## Testing

Paste the commands you ran:

```text

```

Suggested checks from `SOURCE CODE/`:

```sh
./gradlew checkAutomationTools validateVariantMatrix checkProjectIdentity checkCommandCasing checkFalseSupportClaims checkReleaseMetadata
./gradlew build
python -m unittest discover -s tools/python/tests
```

PowerShell wrapper check:

```powershell
pwsh ./tools/pwsh/validate.ps1
```

## Screenshots / Clips

Add screenshots or short clips for GUI, overlay, report, or user-facing behavior changes.

Do not add README screenshots before the `V1.0.1` visual state is final. Use [`docs/SCREENSHOT_PLAN.md`](../../docs/SCREENSHOT_PLAN.md) for the planned screenshot pass.

## Privacy Review

- [ ] Logs, reports, screenshots, and issue bundles were reviewed for local paths, usernames, server addresses, tokens, and private data.
- [ ] No generated reports, local run folders, build output, or private files were committed.

## Notes For Reviewers

Mention known limitations, risk areas, follow-up work, or anything that still needs manual verification.
