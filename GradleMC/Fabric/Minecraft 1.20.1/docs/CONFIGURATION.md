# Configuration

GradleMC stores Fabric configuration at `<game>/config/gradlemc/gradlemc.properties`. The current configuration schema is `2`; it is separate from the mod, report, workflow, and network protocol versions.

On first load, defaults are written atomically. Existing v1.0.0-era files without a schema marker retain recognized values, receive safe v1.0.1 overlay defaults, and are rewritten once with `schemaVersion=2`. Unknown valid properties are preserved. Malformed recognized values fall back to that field's default; an unreadable file is preserved as a `.corrupt` sibling where possible. Save failures are logged and leave runtime defaults/values active.

The overlay itself defaults to disabled. GradleMC branding and Average FPS also default to disabled, while Current FPS is independently enabled for users who turn the overlay on. Each overlay component has its own flag; when every component is disabled, the overlay renders no rows. Position, compact/full mode, scale, background/opacity, rolling window, update interval, JVM/system memory, CPU, GPU name/availability, integrated-server, active-test, profiler, and stability components are separately persisted.

Diagnostic maximum durations default to 600 seconds for performance and FPS and 900 seconds for world-generation observation. Runtime policy clamps malformed configured maxima to the command minimum and clamps excessive values to 1,800 seconds. Profiler bounds are defined independently by its session configuration.

Configuration writes do not edit Minecraft, Fabric Loader, or other mods' configuration. Rule files live under `<game>/gradlemc/rules`; the example command writes only a missing GradleMC-owned template.
