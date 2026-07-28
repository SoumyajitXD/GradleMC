package com.soumyajit.gradlemc.investigation.planning;

import com.soumyajit.gradlemc.foundation.*;
import com.soumyajit.gradlemc.investigation.InvestigationProfileId;
import com.soumyajit.gradlemc.investigation.profile.*;
import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Supplier;

/** Thin profile-to-Foundation translation. It only asks Foundation for a plan and never executes or persists. */
public final class InvestigationPlanningAdapter {
    private final InvestigationProfileRegistry profiles;
    private final Supplier<Collection<TaskCore.Definition>> catalog;
    private final BiFunction<List<TaskId>, TaskCore.Context, TaskCore.Plan> planner;
    public InvestigationPlanningAdapter(InvestigationProfileRegistry profiles, TaskRegistry registry) {
        this(profiles, registry::all, (roots, context) -> registry.plan(roots, context, false));
    }
    private InvestigationPlanningAdapter(InvestigationProfileRegistry profiles, Supplier<Collection<TaskCore.Definition>> catalog, BiFunction<List<TaskId>, TaskCore.Context, TaskCore.Plan> planner) {
        this.profiles = Objects.requireNonNull(profiles, "profiles"); this.catalog = Objects.requireNonNull(catalog, "catalog"); this.planner = Objects.requireNonNull(planner, "planner");
    }
    public static InvestigationPlanningAdapter builtins() {
        return new InvestigationPlanningAdapter(InvestigationProfileRegistry.builtins(), FoundationService::tasks, FoundationService::plan);
    }
    public InvestigationPlan plan(InvestigationProfileId id, InvestigationPlanningContext context) { return plan(profiles.require(id), context); }
    public InvestigationPlan plan(InvestigationProfile profile, InvestigationPlanningContext context) {
        Objects.requireNonNull(context, "context");
        InvestigationProfileValidation.validate(List.of(profile), catalog.get());
        List<InvestigationPlanLimitation> limitations = new ArrayList<>();
        for (String limitation : profile.limitations()) limitations.add(new InvestigationPlanLimitation("profile-limitation", limitation));
        if (!context.allows(profile.sideEligibility())) {
            limitations.add(new InvestigationPlanLimitation("profile-side-unavailable", "Profile is not eligible on side " + context.side()));
            return new InvestigationPlan(profile.id(), profile.budget(), InvestigationPlanStatus.BLOCKED, List.of(), limitations);
        }
        Map<TaskId, InvestigationProfileTask.Requirement> roots = new HashMap<>();
        for (InvestigationProfileTask selection : profile.roots()) roots.put(selection.taskId(), selection.requirement());
        TaskCore.Plan foundation = planner.apply(profile.roots().stream().map(InvestigationProfileTask::taskId).toList(), context.foundationContext());
        Set<TaskId> unavailable = new HashSet<>();
        for (TaskCore.PlanNode node : foundation.nodes()) if (!node.availability().available()) unavailable.add(node.id());
        boolean requiredBlocked = false, optionalUnavailable = false;
        List<InvestigationPlanTask> tasks = new ArrayList<>();
        for (TaskCore.PlanNode node : foundation.nodes()) {
            InvestigationProfileTask.Requirement requirement = roots.get(node.id());
            InvestigationPlanTask.Role role = requirement == InvestigationProfileTask.Requirement.REQUIRED ? InvestigationPlanTask.Role.REQUIRED_ROOT
                    : requirement == InvestigationProfileTask.Requirement.OPTIONAL ? InvestigationPlanTask.Role.OPTIONAL_ROOT : InvestigationPlanTask.Role.DEPENDENCY;
            boolean viable = node.availability().available() && node.dependencies().stream().noneMatch(unavailable::contains);
            String foundationReason = node.availability().available() ? "" : node.availability().reason();
            String interpretation = viable ? "Foundation task can be planned." : requirement == InvestigationProfileTask.Requirement.REQUIRED ? "Required profile evidence is unavailable." : requirement == InvestigationProfileTask.Requirement.OPTIONAL ? "Optional profile evidence is unavailable." : "Dependency is unavailable; affected root evidence remains explicit.";
            tasks.add(new InvestigationPlanTask(node.id(), role, Optional.ofNullable(requirement), viable, foundationReason, interpretation, node.reasons()));
            if (requirement == InvestigationProfileTask.Requirement.REQUIRED && !viable) requiredBlocked = true;
            if (requirement == InvestigationProfileTask.Requirement.OPTIONAL && !viable) optionalUnavailable = true;
        }
        if (requiredBlocked) limitations.add(new InvestigationPlanLimitation("required-root-unavailable", "One or more required profile roots cannot be planned."));
        if (optionalUnavailable) limitations.add(new InvestigationPlanLimitation("optional-root-unavailable", "One or more optional profile roots cannot be planned."));
        InvestigationPlanStatus status = requiredBlocked ? InvestigationPlanStatus.BLOCKED : optionalUnavailable ? InvestigationPlanStatus.PARTIAL : InvestigationPlanStatus.READY;
        return new InvestigationPlan(profile.id(), profile.budget(), status, tasks, limitations);
    }
}
