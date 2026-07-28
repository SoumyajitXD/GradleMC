# GradleMC runtime execution — current audit

This is not the required final topology. Current bounded pools are Foundation
(1–2 threads / queue 16), investigations (1 / direct handoff), and profiler
writer (1 / queue 2). `ThreadSampler` still owns a single-thread scheduled
executor with an unbounded delayed-work queue. It blocks release readiness.
