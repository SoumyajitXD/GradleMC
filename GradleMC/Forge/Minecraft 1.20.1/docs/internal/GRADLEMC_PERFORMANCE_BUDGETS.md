# GradleMC performance budgets

Idle GradleMC performs no filesystem scan, JSON parse, hash, directory poll, report refresh, hardware probe, network heartbeat, task-graph reconstruction, or worker busy loop without a demanding feature. Hot callbacks must be bounded, non-blocking, and I/O-free.

Hard bounds include FPS samples (288,000 maximum), server-health samples (6,240), task history (32 per task), Foundation queue (16), profiler queue (2), and profiler retained snapshots (128). Configuration selects only within declared Forge ranges and cannot raise these safety ceilings.
