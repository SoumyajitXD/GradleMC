# GradleMC task model

`DiagnosticTask` declares namespaced ID, metadata, dependencies, side, permission, capabilities, cost, timeout, cancellation, cache, concurrency, inputs and outputs. `TaskEngine` rejects unsafe/duplicate IDs, missing required dependencies, cycles and graphs deeper than 128. Dependencies are sorted for deterministic dependency-first ordering. Dynamic observations use `NEVER_CACHE`; only explicit static input tasks can reuse results.
