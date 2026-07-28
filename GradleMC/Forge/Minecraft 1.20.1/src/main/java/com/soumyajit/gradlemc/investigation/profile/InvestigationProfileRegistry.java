package com.soumyajit.gradlemc.investigation.profile;

import com.soumyajit.gradlemc.foundation.FoundationService;
import com.soumyajit.gradlemc.foundation.TaskId;
import com.soumyajit.gradlemc.investigation.InvestigationProfileId;
import java.util.*;

/** Fixed built-in catalog. Runtime registration and discovery are intentionally absent. */
public final class InvestigationProfileRegistry {
    private final NavigableMap<InvestigationProfileId, InvestigationProfile> profiles;
    public InvestigationProfileRegistry(List<InvestigationProfile> declarations) {
        InvestigationProfileValidation.validate(declarations, FoundationService.tasks());
        TreeMap<InvestigationProfileId, InvestigationProfile> sorted = new TreeMap<>(Comparator.comparing(InvestigationProfileId::value));
        declarations.forEach(profile -> sorted.put(profile.id(), profile));
        profiles = Collections.unmodifiableNavigableMap(sorted);
    }
    public static InvestigationProfileRegistry builtins() { return new InvestigationProfileRegistry(List.of(
            profile("modpack-audit", "profile.gradlemc.modpack_audit", "Local installed-mod, dependency, pack, and configuration metadata only.",
                    List.of(required("mods:dependencies"), optional("configs:inventory"), optional("packs:resource_inventory")), InvestigationProfile.BudgetPreset.STANDARD,
                    List.of("mod-metadata", "dependency-metadata", "pack-inventory", "config-inventory"),
                    List.of("No online lookup, mod download, automatic configuration change, or definitive lag-mod conclusion.")),
            profile("performance-stutter", "profile.gradlemc.performance_stutter", "Bounded static and instantaneous memory context for a stutter investigation.",
                    List.of(required("memory:snapshot"), optional("instance:snapshot")), InvestigationProfile.BudgetPreset.QUICK,
                    List.of("memory", "instance-context"),
                    List.of("Client FPS and frame evidence is included only where that side has recorded it; unavailable runtime evidence remains explicit.")),
            profile("quick-health", "profile.gradlemc.quick_health", "Cheap broad first-pass instance health evidence.",
                    List.of(required("instance:snapshot")), InvestigationProfile.BudgetPreset.QUICK,
                    List.of("environment", "mods", "packs", "configs", "jvm", "memory"),
                    List.of("Uses bounded local evidence only; unavailable dedicated-side metrics remain explicit.")),
            profile("startup-reload", "profile.gradlemc.startup_reload", "Existing resource and data-pack inventory context without triggering reload work.",
                    List.of(required("packs:resource_inventory"), optional("packs:datapack_inventory")), InvestigationProfile.BudgetPreset.QUICK,
                    List.of("resource-pack-inventory", "datapack-inventory"),
                    List.of("No startup timing or reload observation collector exists in Foundation; this does not trigger a reload or synthesize timings.")))); }
    public List<InvestigationProfile> all() { return List.copyOf(profiles.values()); }
    public Optional<InvestigationProfile> find(InvestigationProfileId id) { return Optional.ofNullable(profiles.get(Objects.requireNonNull(id, "id"))); }
    public InvestigationProfile require(InvestigationProfileId id) { return find(id).orElseThrow(() -> new NoSuchElementException("Unknown investigation profile: " + id)); }
    private static InvestigationProfile profile(String id, String key, String description, List<InvestigationProfileTask> roots, InvestigationProfile.BudgetPreset budget, List<String> evidence, List<String> limitations) {
        return new InvestigationProfile(new InvestigationProfileId(id), key, description, roots, budget, InvestigationProfile.SideEligibility.ANY, evidence, limitations, 1);
    }
    private static InvestigationProfileTask required(String id) { return new InvestigationProfileTask(TaskId.of(id), InvestigationProfileTask.Requirement.REQUIRED); }
    private static InvestigationProfileTask optional(String id) { return new InvestigationProfileTask(TaskId.of(id), InvestigationProfileTask.Requirement.OPTIONAL); }
}
