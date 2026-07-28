# GradleMC measurement architecture

`MeasurementHub` is the authoritative owner of the rendered-frame channel and
the ordinary JVM heap-memory channel. It is deliberately a typed, bounded
facade rather than a reflection-based registry.
`ClientEventHandler` is its only frame producer. Overlay presentation and an
active FPS test acquire demand handles; the hub does no frame work after the
last handle closes. The FPS test is a bounded subscriber and preserves its own
test-window aggregation without becoming a second Forge render listener.

Demand handles are idempotently closeable, reference-counted per owner/channel,
and retain no world, player, GUI or screen object. The strongest live demand is
resolved deterministically. `releaseOwner` and `releaseAll` are lifecycle hooks
for disconnect/world unload/shutdown integration.

Snapshots from the frame channel are immutable `FpsRollingStatsCalculator.Snapshot`
values. Zero samples and warming-up percentile values remain distinct from a
numeric zero. The next migration targets are server tick, heap and context
inspection channels; they must feed this hub rather than introduce replacement
samplers.

## JVM heap channel (Phase 3E increment)

`JVM_MEMORY` has one producer: `Runtime.getRuntime()` through
`MeasurementHub.memorySnapshot`. It measures heap only; no native/JVM-wide
memory claim is made. `RuntimeSnapshots.memory()` is now a compatibility facade
over that same snapshot, so existing report, stability, reload-observation,
performance-test and GUI consumers no longer create a second ordinary heap
collector. `/gradlemc memory` requests one explicit foreground refresh instead
of retaining a subscription.

`MemorySnapshot` is immutable and includes used, committed, maximum and
headroom bytes; percent and pressure; wall and monotonic collection times;
availability/freshness; source/provenance; and a 32-sample bounded trend. A
missing maximum, zero usage, warming-up, stale, unavailable and collection
failure are separate states. It never calls `System.gc()` and never recommends
a RAM cleaner or more RAM from percentage alone.

Memory demand resolves to the highest live request: low frequency (5 s),
normal (1 s), or detailed foreground (250 ms). Snapshot-only owners do not
cause background collection. The overlay acquires low-frequency demand only
while its JVM-memory component is visible and releases it when hidden, disabled
or the client leaves the world. The legacy GUI's compatibility read is bounded
by the current snapshot; its dedicated page lifecycle remains pending.

## Frame-channel verification corrections

`ClientEventHandler.onRenderGui` remains the sole Forge rendered-frame producer.
The FPS test and overlay acquire hub demand and do not register independent
render listeners. The hub now isolates a failing subscriber, bounds listener
owners to four, coalesces a duplicate owner subscription into one callback, and
removes callbacks as part of `releaseOwner`. Close is idempotent. No subscriber
stores a player, world or screen reference.

Server tick/TPS/MSPT is still owned by the pre-existing `ServerHealthTelemetry`
path at this point; it has not yet been represented as a MeasurementHub channel.
That migration must precede any claim that Phase 3E is complete.

## Server-performance channel (in progress)

`ServerPerformanceChannel` is now the authoritative producer registered by
`GradleMC::onServerTickHealth`: it records a monotonic timestamp at `START` and
exactly one elapsed duration at the matching `END`. Its fixed primitive ring is
240 completed ticks. `currentMspt` is the most recently completed duration;
`averageMspt` is the arithmetic mean of the retained durations; `averageTps`
is `min(20, 1000 / averageMspt)`. No value is emitted as `20 TPS` before the
first valid completed sample. Invalid monotonic durations yield failure rather
than a fabricated zero.

The immutable snapshot carries availability, warming/fresh/stale state,
provenance, bounded context identity (`integrated-N` or `dedicated-N`), logical
server side, dedicated/integrated flag and bounded pressure class. The channel
is reset on server start and stop, so evidence cannot cross a logical-server
replacement. A client without an integrated server has no local server evidence
and must render unavailable rather than infer remote timings.
