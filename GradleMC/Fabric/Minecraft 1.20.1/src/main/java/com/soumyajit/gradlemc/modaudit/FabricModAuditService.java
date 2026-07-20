package com.soumyajit.gradlemc.modaudit;

import com.soumyajit.gradlemc.GradleMC;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.fabricmc.loader.api.metadata.ModOrigin;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Loader-native, deterministic metadata audit. It observes only Fabric Loader's loaded metadata. */
public final class FabricModAuditService {
    private FabricModAuditService() {
    }

    public static Audit inspect() {
        List<ModDescriptor> mods = new ArrayList<>();
        List<Finding> findings = new ArrayList<>();
        FabricLoader.getInstance().getAllMods().stream()
                .sorted(Comparator.comparing(container -> container.getMetadata().getId()))
                .forEach(container -> {
                    try {
                        mods.add(descriptor(container));
                    } catch (RuntimeException exception) {
                        String id = container.getMetadata().getId();
                        GradleMC.LOGGER.warn("GradleMC could not fully inspect Fabric metadata for {}; preserving a bounded partial descriptor.", id, exception);
                        mods.add(partialDescriptor(container));
                        findings.add(new Finding("metadata_unavailable", id,
                                "Some Fabric Loader metadata could not be inspected (" + exception.getClass().getSimpleName() + ")."));
                    }
                });
        Map<String, ModDescriptor> byId = mods.stream().collect(Collectors.toMap(ModDescriptor::id,
                Function.identity(), (first, ignored) -> first));
        mods.stream().collect(Collectors.groupingBy(ModDescriptor::id)).forEach((id, values) -> {
            if (values.size() > 1) findings.add(new Finding("duplicate_id", id, "Multiple loaded containers share this mod id."));
        });
        for (ModDescriptor mod : mods) {
            if (mod.name().isBlank()) findings.add(new Finding("incomplete_metadata", mod.id(), "Display name is absent."));
            if (mod.version().isBlank()) findings.add(new Finding("incomplete_metadata", mod.id(), "Version is absent."));
            for (Dependency dependency : mod.dependencies()) {
                ModDescriptor target = byId.get(dependency.id());
                if (dependency.required() && !dependency.targetLoaded()) {
                    findings.add(new Finding("missing_dependency", mod.id(), dependency.id() + " is required but not loaded."));
                } else if (dependency.targetLoaded() && dependency.positive() && Boolean.FALSE.equals(dependency.matchesLoadedVersion())) {
                    findings.add(new Finding("incompatible_dependency", mod.id(), dependency.id() + " version " + target.version()
                            + " does not satisfy " + dependency.constraints() + "."));
                } else if (dependency.targetLoaded() && !dependency.positive() && Boolean.TRUE.equals(dependency.matchesLoadedVersion())) {
                    findings.add(new Finding("declared_conflict", mod.id(), dependency.kind() + " declaration matches loaded "
                            + dependency.id() + " " + target.version() + "."));
                }
            }
        }
        findings.sort(Comparator.comparing(Finding::kind).thenComparing(Finding::modId).thenComparing(Finding::detail));
        return new Audit(mods, List.copyOf(findings));
    }

    private static ModDescriptor partialDescriptor(ModContainer container) {
        var metadata = container.getMetadata();
        return new ModDescriptor(metadata.getId(), metadata.getName() == null ? "" : metadata.getName(),
                metadata.getVersion().getFriendlyString(), metadata.getDescription() == null ? "" : metadata.getDescription(),
                metadata.getEnvironment().name(), container.getOrigin().getKind().name(),
                originNames(container.getOrigin()), "", List.of(), List.of(), Map.of(), List.of());
    }

    private static ModDescriptor descriptor(ModContainer container) {
        var metadata = container.getMetadata();
        List<Dependency> dependencies = metadata.getDependencies().stream().map(dependency -> {
            ModContainer target = FabricLoader.getInstance().getModContainer(dependency.getModId()).orElse(null);
            Boolean matches = target == null ? null : dependency.matches(target.getMetadata().getVersion());
            return new Dependency(dependency.getModId(), dependency.getKind().getKey(), dependency.getKind().isPositive(),
                    dependency.getKind().isPositive() && !dependency.getKind().isSoft(), dependency.getVersionRequirements().toString(),
                    target != null, matches);
        }).sorted(Comparator.comparing(Dependency::id).thenComparing(Dependency::kind)).toList();
        List<String> origins = originNames(container.getOrigin());
        String nestedIn = container.getContainingMod().map(parent -> parent.getMetadata().getId()).orElse("");
        List<String> containedMods = container.getContainedMods().stream()
                .map(child -> child.getMetadata().getId()).sorted().toList();
        List<String> authors = metadata.getAuthors().stream().map(person -> person.getName()).filter(name -> name != null && !name.isBlank())
                .sorted().toList();
        Map<String, String> contacts = metadata.getContact().asMap().entrySet().stream()
                .filter(entry -> entry.getKey() != null && entry.getValue() != null && !entry.getValue().isBlank())
                .sorted(Map.Entry.comparingByKey())
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (first, ignored) -> first, java.util.LinkedHashMap::new));
        return new ModDescriptor(metadata.getId(), metadata.getName() == null ? "" : metadata.getName(),
                metadata.getVersion().getFriendlyString(), metadata.getDescription() == null ? "" : metadata.getDescription(),
                metadata.getEnvironment().name(), container.getOrigin().getKind().name(), origins, nestedIn, containedMods,
                authors, contacts, dependencies);
    }

    private static String safeFileName(Path path) {
        Path name = path.getFileName();
        return name == null ? path.toString() : name.toString();
    }

    static List<String> originNames(ModOrigin origin) {
        if (origin == null || origin.getKind() != ModOrigin.Kind.PATH) return List.of();
        return origin.getPaths().stream().map(FabricModAuditService::safeFileName).sorted().toList();
    }

    public record Audit(List<ModDescriptor> mods, List<Finding> findings) {
        public int requiredDependencyCount() { return (int) mods.stream().flatMap(mod -> mod.dependencies().stream()).filter(Dependency::required).count(); }
    }

    public record ModDescriptor(String id, String name, String version, String description, String environment, String originKind,
                                List<String> origins, String nestedIn, List<String> containedMods, List<String> authors,
                                Map<String, String> contacts, List<Dependency> dependencies) {
        public ModDescriptor {
            origins = List.copyOf(origins);
            containedMods = List.copyOf(containedMods);
            authors = List.copyOf(authors);
            contacts = java.util.Collections.unmodifiableMap(new java.util.LinkedHashMap<>(contacts));
            dependencies = List.copyOf(dependencies);
        }

        public int bundledModCount() { return containedMods.size(); }
    }

    public record Dependency(String id, String kind, boolean positive, boolean required, String constraints,
                             boolean targetLoaded, Boolean matchesLoadedVersion) { }
    public record Finding(String kind, String modId, String detail) { }
}
