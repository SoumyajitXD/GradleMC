# Minecraft Instance Model

GradleMC inspects and connects evidence from the complete Minecraft instance—not only the base game or the current world.

`MinecraftInstanceSnapshot` is the shared immutable model used by diagnostic tasks and scan reporting. Each component records availability, source, collection time, side, scope, freshness, and a limitation. An unavailable source is reported as `UNAVAILABLE`; it is never converted into an empty successful inventory.

Currently implemented providers are the JVM allowlist, Forge active-mod metadata, safe metadata-only resource-pack and shader-pack directory inventories, and configuration-file metadata inventory. Resource-pack ZIP inspection is bounded (128 MiB archive, 10,000 entries, 64 KiB metadata), does not extract files, rejects unsafe entry names, and omits absolute paths. Shader packs currently have `DIRECTORY_INVENTORY_ONLY` capability: no active-pack claim is made without a supported provider adapter.

Datapacks and world state remain explicitly unavailable from the common snapshot until a correct active world/server adapter is present. This is intentional side safety, not an empty result.

Instance fingerprints combine versioned environment data, a JVM allowlist, loaded-mod identity, and pack metadata fingerprints. They exclude usernames, absolute paths, config contents, server addresses, seeds, tokens, and complete JAR hashes. Static task cache reuse is limited to exact relevant task inputs; runtime evidence is never reused.

The `inspect`, `check`, and `scan` workflows now use the shared inventory. Reports remain local TXT and JSON files and state limitations rather than attributing causation to a mod or pack.
