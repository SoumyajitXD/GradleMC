# Diagnostic architecture

GradleMC 1.0.3 treats diagnosis as a local build: providers collect bounded snapshots, tasks declare inputs and outputs, workflows plan dependency graphs, evidence feeds analysis, and scans preserve the result and the reasons behind it.

## Boundaries

- Instance and component records are immutable snapshots. Background work may process those snapshots; live Minecraft objects remain on their owning game thread.
- `TaskEngine` owns planning, execution state, cache decisions, timeouts, cancellation, history and overhead. Commands invoke shared services instead of implementing diagnostics.
- Static tasks may reuse a matching input fingerprint. Runtime tests and nondeterministic observations never become `UP_TO_DATE`.
- Health gates consume completed task evidence. Missing evidence produces `INCONCLUSIVE`.
- Adaptive Diagnostics consumes collected evidence and validates recommendation freshness, attribution and verifiability. It does not rescan the instance.
- Experiments persist a bounded baseline and compare the same workflow only after checking context compatibility. GradleMC instructs the user to make and roll back changes manually.
- Incident recording retains bounded cheap signals and enriches incidents from already available context. It does not perform filesystem or registry scans in a bad tick/frame.

## Client FPS and overlay

GradleMC observes completed active-gameplay frames once from Forge's post-GUI render callback using `System.nanoTime()`. It does not derive FPS from client or server ticks. The recent FPS display uses approximately one second of the latest valid frame intervals so it remains readable while showing temporary stutters. The rolling average is the count of completed frame intervals divided by their total elapsed time in the configured bounded window; it is not an average of averages. Menu, pause, unfocused-window, world-transition, non-positive, and over-one-second intervals reset the pending timestamp and are not reported as rendered frames.

`showOverlayTitle` defaults to `false`; when enabled it displays the `GradleMC` heading only. `showAverageFps` also defaults to `false` and independently controls the rolling-average line. `overlayShowFps` controls only the recent FPS line. Therefore either FPS value can be enabled without the other, and an overlay with every component disabled produces no lines or background.
- Scan and bundle writers serialize already-collected state. Writes are bounded and atomic, and managed output directories reject symlink components below the game directory.
- Client GUI code is physically isolated under the client package. Unsupported data is presented as unavailable rather than synthesized.

## Task states and explanations

Tasks can finish as successful, failed, skipped, up-to-date, unavailable, cancelled or timed out. Planning records the requested root, dependency chain, changed declared inputs, side/capability failures and cache decision. `/gradlemc task why`, `inputs`, `outputs`, `history`, and `graph` expose those real decisions.

Execution budgets cap task duration, files, archive entries, bytes, retained samples, output size and concurrent/background work where supported. A reached budget produces a partial/truncated result with an explicit reason.

## Local artifacts

- GradleMC Scans: schema-versioned TXT and JSON with tasks, input changes, outputs, overhead, gates and limitations.
- Experiments: bounded local records with baseline/candidate fingerprints and normalized comparisons.
- Incidents: bounded JSON exports with trigger, timestamps, metrics and evidence references.
- Instance lock: normalized mod/pack/provider inventory; never a downloader or cryptographic attestation.

All functionality remains local and telemetry-free.
