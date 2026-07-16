# Changelog

## 1.0.3 — Forge 1.20.1

- Hardened the diagnostic task engine with declared capabilities, side/cost/concurrency metadata, dependency explanations, input-change reasons, bounded history, timeouts, cancellation and best-effort execution-overhead accounting.
- Added configurable performance budgets and explicit truncation evidence instead of silent omission.
- Added local Health Gates, controlled before/after experiments, bounded incident capture, startup/reload observations, instance lock snapshots, GradleMC-channel network summaries, storage diagnostics and `/gradlemc doctor`.
- Expanded deterministic GradleMC Scan TXT/JSON output with task outputs, changed inputs, overhead, gates, limitations, atomic writes and centralized path/secret redaction.
- Improved sampler drift/missed-sample accounting and top-down, bottom-up, per-state and truncated-stack aggregation.
- Reorganized the GUI into the required diagnostic pages and added cached, bounded mod-list filtering.
- Hardened pack, report, issue-bundle, experiment, incident and storage filesystem boundaries against symlink escape and oversized inputs; privileged filesystem actions now require operator permission.
- JFR and heap-dump support remain deferred; physical client and dedicated-server runtime verification must not be inferred from build/self-test results.
- Added local Installed Mod Intelligence: immutable Forge metadata snapshots, normalized mod identifiers, safe JAR filenames, bundled-mod-file counts, dependency records, reverse dependency graph, and metadata-quality observations.
- Added `/gradlemc mods inspect`, `audit`, `audit refresh`, and `export`, plus improved mod summary output.
- Added dedicated deterministic TXT and JSON mod-audit reports. Reports are local-only, omit absolute JAR paths, and make no network requests.
- Added cautious confidence labels and local declarative rules for all/any mod presence, version ranges, and declared dependency relationships. Rules that cannot be parsed are skipped safely.
- Added a Mods & Audit GUI section with local searchable inventory and working audit/export/inspect controls.
- Added configurable mod-audit enablement, safe JAR-name inclusion, and chat finding limits.
- GradleMC connects evidence to guide controlled testing; it does not automatically identify every lag-causing mod.
