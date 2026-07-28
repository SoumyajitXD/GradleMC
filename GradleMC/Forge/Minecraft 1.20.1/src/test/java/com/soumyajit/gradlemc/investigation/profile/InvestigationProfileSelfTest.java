package com.soumyajit.gradlemc.investigation.profile;

import com.soumyajit.gradlemc.foundation.*;
import com.soumyajit.gradlemc.investigation.InvestigationProfileId;
import java.time.Duration;
import java.util.*;

public final class InvestigationProfileSelfTest {
    private InvestigationProfileSelfTest() { }
    public static void run() {
        InvestigationProfileRegistry registry = InvestigationProfileRegistry.builtins();
        check(registry.all().stream().map(p -> p.id().value()).toList().equals(List.of("modpack-audit", "performance-stutter", "quick-health", "startup-reload")), "built-in IDs and order");
        check(registry.require(new InvestigationProfileId("quick-health")).roots().stream().map(x -> x.taskId().value()).toList().equals(List.of("instance:snapshot")), "quick roots");
        check(registry.require(new InvestigationProfileId("performance-stutter")).roots().stream().map(x -> x.taskId().value()).toList().equals(List.of("memory:snapshot", "instance:snapshot")), "performance roots");
        check(registry.require(new InvestigationProfileId("startup-reload")).roots().stream().map(x -> x.taskId().value()).toList().equals(List.of("packs:resource_inventory", "packs:datapack_inventory")), "startup roots");
        check(registry.require(new InvestigationProfileId("modpack-audit")).roots().stream().map(x -> x.taskId().value()).toList().equals(List.of("mods:dependencies", "configs:inventory", "packs:resource_inventory")), "audit roots");
        check(registry.find(new InvestigationProfileId("missing")).isEmpty(), "unknown lookup");
        bad(() -> new InvestigationProfileId("Quick")); bad(() -> new InvestigationProfileId("a/b"));
        bad(() -> registry.all().add(registry.all().get(0))); bad(() -> registry.require(new InvestigationProfileId("quick-health")).roots().clear());
        InvestigationProfile good = profile("test", List.of(required("instance:identity")));
        InvestigationProfileValidation.validate(List.of(good), FoundationService.tasks());
        bad(() -> InvestigationProfileValidation.validate(List.of(good, good), FoundationService.tasks()));
        bad(() -> InvestigationProfileValidation.validate(List.of(profile("empty", List.of())), FoundationService.tasks()));
        bad(() -> InvestigationProfileValidation.validate(List.of(profile("dupe", List.of(required("instance:identity"), optional("instance:identity")))), FoundationService.tasks()));
        bad(() -> InvestigationProfileValidation.validate(List.of(profile("missing-task", List.of(required("missing:task")))), FoundationService.tasks()));
        for (InvestigationProfile profile : registry.all()) {
            check(profile.limitations().stream().noneMatch(x -> x.matches(".*[\\\\/].*|.*(?i)(users|server|localhost).*")), "declaration privacy " + profile.id());
            check(profile.roots().stream().allMatch(x -> FoundationService.tasks().stream().anyMatch(t -> t.id().equals(x.taskId()))), "canonical task reference");
        }
    }
    private static InvestigationProfile profile(String id, List<InvestigationProfileTask> roots) { return new InvestigationProfile(new InvestigationProfileId(id), "key", "description", roots, InvestigationProfile.BudgetPreset.QUICK, InvestigationProfile.SideEligibility.ANY, List.of("evidence"), List.of("limitation"), 1); }
    private static InvestigationProfileTask required(String id) { return new InvestigationProfileTask(TaskId.of(id), InvestigationProfileTask.Requirement.REQUIRED); }
    private static InvestigationProfileTask optional(String id) { return new InvestigationProfileTask(TaskId.of(id), InvestigationProfileTask.Requirement.OPTIONAL); }
    private static void check(boolean value, String name) { if (!value) throw new AssertionError("Profile self-test failed: " + name); }
    private static void bad(Runnable action) { try { action.run(); throw new AssertionError("Expected rejection"); } catch (IllegalArgumentException | UnsupportedOperationException | NoSuchElementException expected) { } }
}
