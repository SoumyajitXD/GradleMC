package com.soumyajit.gradlemc.investigation.planning;

import com.soumyajit.gradlemc.foundation.*;
import com.soumyajit.gradlemc.investigation.InvestigationProfileId;
import com.soumyajit.gradlemc.investigation.profile.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

public final class InvestigationPlanningSelfTest {
    private InvestigationPlanningSelfTest() { }
    public static void run() {
        AtomicInteger executions = new AtomicInteger(); TaskRegistry tasks = fixture(executions, false, false);
        InvestigationPlanningAdapter adapter = new InvestigationPlanningAdapter(InvestigationProfileRegistry.builtins(), tasks);
        InvestigationPlan ready = adapter.plan(new InvestigationProfileId("quick-health"), context());
        check(ready.status() == InvestigationPlanStatus.READY, "ready"); check(executions.get() == 0, "plan does not execute");
        check(ready.tasks().stream().map(x -> x.taskId().value()).toList().equals(List.of("instance:identity", "mods:inventory", "mods:dependencies", "packs:resource_inventory", "packs:datapack_inventory", "configs:inventory", "jvm:environment", "memory:snapshot", "instance:snapshot")), "dependency order once");
        check(ready.tasks().get(0).role() == InvestigationPlanTask.Role.DEPENDENCY && ready.tasks().get(8).role() == InvestigationPlanTask.Role.REQUIRED_ROOT, "roles");
        check(adapter.plan(new InvestigationProfileId("quick-health"), context()).equals(ready), "repeat deterministic");
        bad(() -> ready.tasks().clear()); bad(() -> ready.limitations().clear()); bad(() -> adapter.plan(new InvestigationProfileId("unknown"), context()));
        InvestigationPlanningAdapter optional = new InvestigationPlanningAdapter(InvestigationProfileRegistry.builtins(), fixture(new AtomicInteger(), true, false));
        InvestigationPlan partial = optional.plan(new InvestigationProfileId("performance-stutter"), context()); check(partial.status() == InvestigationPlanStatus.PARTIAL, "optional unavailable");
        check(partial.tasks().stream().anyMatch(x -> x.taskId().value().equals("instance:snapshot") && !x.available() && x.foundationReason().equals("missing capability: instance")), "Foundation reason retained");
        InvestigationPlanningAdapter required = new InvestigationPlanningAdapter(InvestigationProfileRegistry.builtins(), fixture(new AtomicInteger(), false, true));
        check(required.plan(new InvestigationProfileId("quick-health"), context()).status() == InvestigationPlanStatus.BLOCKED, "missing capability blocks required root");
        check(required.plan(new InvestigationProfileId("quick-health"), context("identity")).status() == InvestigationPlanStatus.READY, "Foundation capability supplied");
        InvestigationProfile clientOnly = new InvestigationProfile(new InvestigationProfileId("client-only"), "key", "description", List.of(new InvestigationProfileTask(TaskId.of("instance:identity"), InvestigationProfileTask.Requirement.REQUIRED)), InvestigationProfile.BudgetPreset.QUICK, InvestigationProfile.SideEligibility.CLIENT, List.of("evidence"), List.of("side limited"), 1);
        InvestigationPlanningAdapter sides = new InvestigationPlanningAdapter(new InvestigationProfileRegistry(List.of(clientOnly)), fixture(new AtomicInteger(), false, false));
        check(sides.plan(clientOnly, new InvestigationPlanningContext(InvestigationPlanningContext.Side.SERVER, context().foundationContext())).status() == InvestigationPlanStatus.BLOCKED, "unsupported side");
    }
    private static TaskRegistry fixture(AtomicInteger executions, boolean optionalUnavailable, boolean requiredUnavailable) {
        TaskRegistry registry = new TaskRegistry();
        for (TaskCore.Definition definition : FoundationService.tasks()) {
            registry.register(new TaskCore.Definition(definition.id(), definition.displayName(), definition.category(), definition.dependencies(), definition.affinity(), definition.timeout(), definition.cachePolicy(), c -> requiredUnavailable && definition.id().value().equals("instance:identity") && !c.capabilities().contains("identity") ? TaskCore.Availability.unavailable("missing capability: identity")
                    : optionalUnavailable && definition.id().value().equals("instance:snapshot") ? TaskCore.Availability.unavailable("missing capability: instance") : TaskCore.Availability.present(), c -> { executions.incrementAndGet(); return new TaskCore.Output(List.of(), "executed"); }));
        }
        return registry;
    }
    private static InvestigationPlanningContext context() { return context(new String[0]); }
    private static InvestigationPlanningContext context(String... capabilities) { StaticFingerprint fingerprint = StaticFingerprint.of(1, Map.of("test", "true")); return new InvestigationPlanningContext(InvestigationPlanningContext.Side.COMMON, new TaskCore.Context(Clock.fixed(Instant.EPOCH, ZoneOffset.UTC), new TaskCore.CancellationToken(), fingerprint, RuntimeContextFingerprint.of(1, fingerprint, Map.of()), Map.of(), Set.of(capabilities))); }
    private static void check(boolean value, String name) { if (!value) throw new AssertionError("Planning self-test failed: " + name); }
    private static void bad(Runnable action) { try { action.run(); throw new AssertionError("Expected rejection"); } catch (UnsupportedOperationException | NoSuchElementException expected) { } }
}
