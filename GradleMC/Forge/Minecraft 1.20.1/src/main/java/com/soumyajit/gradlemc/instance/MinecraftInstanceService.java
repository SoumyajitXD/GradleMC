package com.soumyajit.gradlemc.instance;

import com.soumyajit.gradlemc.GradleMC;
import com.soumyajit.gradlemc.modaudit.*;
import com.soumyajit.gradlemc.util.GradleMcPaths;
import java.io.IOException;
import java.nio.file.*;
import java.time.Instant;
import java.util.*;
import java.util.stream.Stream;
import net.minecraftforge.versions.forge.ForgeVersion;

/** Single collection entry point. Filesystem collection is bounded and performed only on demand. */
public final class MinecraftInstanceService {
    private static volatile MinecraftInstanceSnapshot latest;
    private MinecraftInstanceService() { }
    public static MinecraftInstanceSnapshot current(boolean refresh) { MinecraftInstanceSnapshot value = latest; return !refresh && value != null ? value : collect(); }
    public static synchronized MinecraftInstanceSnapshot collect() {
        Instant now = Instant.now();
        Map<String, String> env = Map.of("minecraft", GradleMC.CURRENT_MINECRAFT_VERSION, "loader", GradleMC.CURRENT_LOADER_NAME,
                "loaderVersion", ForgeVersion.getVersion(), "gradlemc", "1.0.3");
        Map<String, String> runtime = new TreeMap<>(); runtime.put("java", System.getProperty("java.version", "unknown")); runtime.put("vendor", System.getProperty("java.vendor", "unknown")); runtime.put("jvm", System.getProperty("java.vm.name", "unknown")); runtime.put("maxHeapMiB", Long.toString(Runtime.getRuntime().maxMemory() / (1024 * 1024))); runtime.put("processors", Integer.toString(Runtime.getRuntime().availableProcessors()));
        InstanceComponent<Map<String,String>> environment = new InstanceComponent<>(ComponentAvailability.AVAILABLE, "GradleMC constants", now, "common", ComponentScope.STATIC, true, "No private game path exported.", env);
        InstanceComponent<Map<String,String>> runtimeComponent = new InstanceComponent<>(ComponentAvailability.AVAILABLE, "JVM allowlist", now, "common", ComponentScope.RUNTIME, true, "JVM arguments and environment variables are intentionally omitted.", Map.copyOf(runtime));
        InstalledModSnapshot modSnapshot = ForgeInstalledModSnapshotProvider.current();
        InstanceComponent<InstalledModSnapshot> mods = new InstanceComponent<>(modSnapshot.available() ? ComponentAvailability.AVAILABLE : ComponentAvailability.UNAVAILABLE, "Forge ModList", now, "common", ComponentScope.SESSION, !modSnapshot.available(), modSnapshot.available() ? "Physical but not loaded JARs are outside Forge's active mod inventory." : modSnapshot.unavailableReason(), modSnapshot);
        InstanceComponent<List<PackDescriptor>> resources = packs(GradleMcPaths.gameDirectory().resolve("resourcepacks"), PackDescriptor.PackKind.RESOURCE, now, "Available filesystem packs only; selected client order requires a client adapter.");
        InstanceComponent<List<PackDescriptor>> shaders = packs(GradleMcPaths.gameDirectory().resolve("shaderpacks"), PackDescriptor.PackKind.SHADER, now, "Directory inventory only; no optional shader provider integration is active.");
        InstanceComponent<List<PackDescriptor>> data = InstanceComponent.unavailable("Minecraft server", ComponentScope.WORLD, "Enabled datapacks require an active world/server adapter.");
        InstanceComponent<List<ConfigDescriptor>> configs = configs(now);
        InstanceComponent<Map<String,String>> world = InstanceComponent.unavailable("Minecraft server", ComponentScope.WORLD, "World inspection requires an active server context.");
        InstanceComponent<Map<String,String>> providers = new InstanceComponent<>(ComponentAvailability.PARTIAL, "GradleMC provider registry", now, "common", ComponentScope.SESSION, true, "Client resource-pack selection, active shader state, and world datapacks need side-specific adapters.", Map.of("resourcePacks", "directory-inventory", "shaderPacks", "directory-inventory", "dataPacks", "unavailable"));
        List<String> modIds = modSnapshot.mods().stream().map(ModDescriptor::normalizedIdentifier).toList();
        latest = new MinecraftInstanceSnapshot(now, environment, runtimeComponent, mods, resources, shaders, data, configs, world, providers, InstanceFingerprint.calculate(env, runtime, resources.value(), shaders.value(), List.of(), modIds));
        return latest;
    }
    private static InstanceComponent<List<PackDescriptor>> packs(Path directory, PackDescriptor.PackKind kind, Instant now, String limitation) {
        if (!Files.isDirectory(directory)) return new InstanceComponent<>(ComponentAvailability.UNAVAILABLE, "managed game directory", now, "common", ComponentScope.STATIC, true, limitation + " Directory does not exist.", List.of());
        try (Stream<Path> paths = Files.list(directory)) { List<PackDescriptor> values = paths.filter(p -> Files.isDirectory(p) || p.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".zip")).filter(p -> !Files.isSymbolicLink(p)).limit(256).map(p -> PackMetadataReader.read(directory, p, kind)).sorted(Comparator.comparing(PackDescriptor::id)).toList(); return new InstanceComponent<>(ComponentAvailability.PARTIAL, "managed game directory", now, "common", ComponentScope.STATIC, true, limitation, values); }
        catch (IOException e) { return new InstanceComponent<>(ComponentAvailability.UNAVAILABLE, "managed game directory", now, "common", ComponentScope.STATIC, true, e.getClass().getSimpleName(), List.of()); }
    }
    private static InstanceComponent<List<ConfigDescriptor>> configs(Instant now) {
        Path root = GradleMcPaths.configDirectory(); if (!Files.isDirectory(root)) return new InstanceComponent<>(ComponentAvailability.UNAVAILABLE, "Forge config directory", now, "common", ComponentScope.STATIC, true, "Config directory unavailable.", List.of());
        try (Stream<Path> paths = Files.walk(root, 2)) { List<ConfigDescriptor> values = paths.filter(Files::isRegularFile).filter(p -> !Files.isSymbolicLink(p)).limit(512).map(p -> { try { String name = p.getFileName().toString(); int dot = name.lastIndexOf('.'); String ext = dot < 0 ? "" : name.substring(dot + 1).toLowerCase(Locale.ROOT); long size = Files.size(p); return new ConfigDescriptor(name, ext, size, Files.getLastModifiedTime(p).toMillis(), "metadata-only", owner(name), size > 4L * 1024L * 1024L ? "Unusually large configuration file." : ""); } catch (IOException e) { return new ConfigDescriptor(p.getFileName().toString(), "", 0, 0, "unreadable", "", e.getClass().getSimpleName()); } }).sorted(Comparator.comparing(ConfigDescriptor::fileName)).toList(); return new InstanceComponent<>(ComponentAvailability.PARTIAL, "Forge config directory", now, "common", ComponentScope.STATIC, true, "Contents and secrets are not collected; parser validation remains an opt-in rule task.", values); }
        catch (IOException e) { return new InstanceComponent<>(ComponentAvailability.UNAVAILABLE, "Forge config directory", now, "common", ComponentScope.STATIC, true, e.getClass().getSimpleName(), List.of()); }
    }
    private static String owner(String name) { int dot = name.indexOf('.'); return dot <= 0 ? "" : name.substring(0, dot).toLowerCase(Locale.ROOT); }
}
