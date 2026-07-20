# Architecture

## Boundaries

`GradleMCFabric` is the common entrypoint. It owns common configuration load, commands, server networking, server tick/lifecycle callbacks, performance/world-generation sampling, profiling, and workflow shutdown. `GradleMCFabricClient` is the client entrypoint. It owns key mappings, HUD rendering, the single FPS sampler, client networking, GUI opening, and client lifecycle cleanup. Common source must not reference Minecraft client APIs; `verifyCommonEnvironmentBoundary` enforces this constraint.

The project exposes no supported external Java API. Public visibility exists where Java package boundaries, Fabric callbacks, records, or tests require it; those types remain internal implementation details unless a future document explicitly says otherwise.

## Production packages and ownership

| Package | Responsibility | Environment / owner |
|---|---|---|
| `fabric` | Fabric entrypoints and lifecycle registration | common and client entrypoints |
| `client`, `client.gui`, `client.overlay`, `client.input` | GUI/view model, key bindings, one rendered-frame sampler, HUD composition | client thread; client entrypoint |
| `command` | Brigadier registration, permission checks, user-facing dispatch | server command thread; common entrypoint |
| `config` | Schema-v2 properties config, overlay defaults, diagnostic duration policy | process lifetime; loaded once by common entrypoint |
| `metrics` | Bounded performance/world-generation sessions and immutable results | server thread; tick callbacks |
| `profiler` | One active bounded profiler session, sampler executor, immutable summaries | server thread plus owned daemon sampler |
| `task` | Stable task/workflow catalogs, execution, cancellation, evidence, reports, bounded history | `FabricDiagnosticService`; two bounded daemon executors |
| `network` | Three protocol-v1 channels, request correlation, immutable GUI snapshots/capabilities | Fabric networking callbacks; generation-scoped client state |
| `modaudit` | Fabric Loader metadata normalization and TXT/JSON export | calling thread; no JAR-content scanning |
| `check`, `rules`, `smart`, `ai` | Deterministic checks, local rules, interpretations, adaptive runtime signals | server thread; no cloud or generative AI |
| `report` | Formatting, safe naming, local report and issue-bundle writes | immutable inputs; filesystem boundary |
| `util` | Owned-path validation, atomic writes, redaction, runtime snapshots, shared storage limits | stateless except JVM/Fabric runtime lookups |

## Data flow

Commands and GUI actions request an operation; they do not contain sampling algorithms. Tick/render callbacks update the owning sampler or session. Completed sessions produce immutable results. Smart diagnostics interpret those results without controlling collection. Report writers consume completed immutable data and never query screens.

Workflow planning uses the sole task/workflow catalogs. A workflow captures an immutable normalized snapshot on an owning Minecraft thread, executes bounded tasks on GradleMC-owned workers, records explicit terminal states and evidence availability, then optionally writes TXT/JSON plus the bounded history index. Schema identifiers live in `DiagnosticSchemas` and are independent of product version.

Networking decodes packet buffers inside the callback, transfers immutable values to the owning thread, correlates bounded requests by connection generation, and never sends server filesystem paths to clients. Protocol version `1` is independent of GradleMC v1.0.1.

## Filesystem and threading invariants

Writes stay under `<game>/gradlemc` or Fabric's `<config>/gradlemc` configuration root. Owned writes reject traversal, drive/UNC escapes, reserved Windows names, and existing symbolic-link traversal. New reports use `<game>/gradlemc/reports`; legacy reports under `<config>/gradlemc/reports` remain readable.

Minecraft world/player/client objects are not stored in the global workflow container. Server managers are called on the server thread; the FPS sampler is called on the render/client thread. Worker tasks consume immutable snapshots. Shutdown cancels bounded work and terminates owned executors; integrated-server world loss cancels world-dependent work without destroying process-lifetime client workers.

Package-level dependency cycles currently exist for `check`/`rules`, `metrics`/`report`, `metrics`/`smart`, and `config`/`util`. Review found no circular initialization or client/common violation: each reflects a small model/orchestration relationship. Breaking them would require moving stable models or adding one-method abstractions without a demonstrated defect, so this pass rejects that churn.
