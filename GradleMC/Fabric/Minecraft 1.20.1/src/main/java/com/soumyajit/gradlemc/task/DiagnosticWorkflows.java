package com.soumyajit.gradlemc.task;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/** The sole workflow catalog. IDs and root tasks are stable across commands, GUI, history and reports. */
public final class DiagnosticWorkflows {
    private static final Map<String, WorkflowDefinition> WORKFLOWS;
    static {
        Map<String, WorkflowDefinition> workflows = new TreeMap<>();
        register(workflows, new WorkflowDefinition("quick", "Quick diagnostics", "Low-overhead environment, memory and mod metadata snapshot.", List.of("runtime", "memory", "mods"), Duration.ofSeconds(20)));
        register(workflows, new WorkflowDefinition("lag", "Lag diagnostics", "Correlates available server, entity, memory and worldgen observations.", List.of("runtime", "memory", "entity_observation", "worldgen_observation"), Duration.ofSeconds(45)));
        register(workflows, new WorkflowDefinition("fps", "FPS diagnostics", "Uses the authoritative client frame sampler when client evidence is available.", List.of("runtime", "fps_client"), Duration.ofSeconds(20)));
        register(workflows, new WorkflowDefinition("worldgen", "World-generation diagnostics", "Reads only an already completed bounded observation.", List.of("runtime", "worldgen_observation"), Duration.ofSeconds(20)));
        register(workflows, new WorkflowDefinition("memory", "Memory diagnostics", "Captures a bounded JVM memory snapshot.", List.of("runtime", "memory"), Duration.ofSeconds(15)));
        register(workflows, new WorkflowDefinition("modpack_release", "Modpack release readiness", "Checks identity, configuration, installed-mod metadata and available diagnostic evidence.", List.of("runtime", "mods", "memory", "worldgen_observation"), Duration.ofSeconds(30)));
        WORKFLOWS = Map.copyOf(workflows);
    }
    private DiagnosticWorkflows() { }
    private static void register(Map<String, WorkflowDefinition> values, WorkflowDefinition workflow) {
        if (values.putIfAbsent(workflow.id(), workflow) != null) throw new IllegalArgumentException("Duplicate workflow id: " + workflow.id());
    }
    public static List<String> names() { return List.copyOf(WORKFLOWS.keySet()); }
    public static WorkflowDefinition workflow(String id) { return WORKFLOWS.get(id); }
}
