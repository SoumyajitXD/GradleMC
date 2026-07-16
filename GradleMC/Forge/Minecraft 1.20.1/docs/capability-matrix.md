# GradleMC 1.0.3 capability matrix

This matrix describes the Forge 1.20.1 source tree. “Self-tested” means the deterministic `gradlemcSelfTest` suite ran; it does not mean a physical Minecraft client or dedicated server was exercised.

| Capability | Support | Self-tested | Client runtime | Dedicated-server runtime | Notes |
|---|---|---:|---:|---:|---|
| Task graph, cycle detection, cache decisions, explanations and history | Supported | Yes | No | No | Runtime evidence is never treated as up-to-date. |
| Immutable instance and installed-mod inventory | Supported | Yes | No | No | Bounded pack/archive inspection and normalized identities. |
| Resource packs, shader state, datapacks, configs and world evidence | Partial | Model tests | No | No | Availability remains side- and world-dependent. |
| TPS/MSPT, FPS, worldgen and sampling profiler | Supported | Aggregation tests | No | No | No native-profiler parity claim. |
| GradleMC execution overhead and budgets | Supported | Yes | No | No | Best-effort wall/thread attribution, not CPU accounting. |
| Health gates | Supported | Yes | No | No | Missing evidence is `INCONCLUSIVE`, never a pass. |
| Controlled experiments | Supported | Yes | No | No | Manual, reversible changes only; bounded local store. |
| Triggered incidents | Supported | Yes | No | No | Cheap bounded context; no packet payloads or coordinates. |
| Startup/resource-reload observation | Partial | No | No | No | Uses observable lifecycle markers; does not trigger normal reloads. |
| Instance lock and diff | Supported | Yes | No | No | Inventory lock only; no downloading or supply-chain claim. |
| Network diagnostics | Partial | No | No | No | GradleMC-channel metadata and aggregates only. |
| Storage diagnostics and cleanup preview | Partial | No | No | No | GradleMC-owned directories only; confirmed cleanup deletes stale `.tmp` files only. |
| Adaptive Diagnostics | Supported | Deterministic rules tested | No | No | Local, explainable, telemetry-free; weak recommendations are suppressed/downgraded. |
| GUI shell and required pages | Partial | View-model tests | No | N/A | Client-only; responsive/runtime behavior remains unverified. |
| Versioned TXT/JSON GradleMC Scans | Supported | Task integration tests | No | No | Atomic, bounded, deterministic ordering with centralized redaction. |
| Issue bundles | Supported | No | No | No | Local, bounded inputs, redacted text, no automatic upload. |
| JFR provider | Planned | No | No | No | Deferred until mandatory systems receive runtime verification. |
| Heap dump command | Unavailable | No | No | No | Deliberately omitted rather than exposing an insufficiently safeguarded artifact. |

## Privacy and safety boundaries

GradleMC does not provide telemetry, remote uploads, cloud AI, packet-payload capture, mod downloading, automatic mod/config/pack changes, or automatic deletion outside validated GradleMC-owned temporary files. Reports redact managed absolute paths, user-home paths and common secret assignments. Symlinked managed output and pack components are rejected where they could cross a filesystem boundary.
