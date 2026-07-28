# GradleMC work avoidance

Existing Phase 3B `TaskEngine` cache/provenance remains the authority. Static inputs now use full SHA-256 over sorted, length-prefixed UTF-8 key/value inputs. Static tasks reuse only matching declared inputs; runtime tasks remain `NEVER_CACHE` and must be fresh or explicitly stale. No cache lookup occurs in render or tick callbacks.

Restart-safe persistent cache/history remains disabled pending managed-root schema and atomic persistence work.
