package com.soumyajit.gradlemc.task;
import java.util.*;
public final class DiagnosticWorkflows {
    private static final Set<String> FOUNDATION_WORKFLOWS = Set.of("standard");
    private static final Map<String, List<String>> WORKFLOWS = Map.of(
            "quick", List.of("gradlemc:inspect_instance", "gradlemc:mod_audit", "gradlemc:config_check", "gradlemc:memory_snapshot", "gradlemc:correlate", "gradlemc:export_scan"),
            "inspect", List.of("gradlemc:inspect_instance", "gradlemc:mod_audit"),
            "check", List.of("gradlemc:inspect_instance", "gradlemc:mod_audit", "gradlemc:config_check", "gradlemc:correlate", "gradlemc:evaluate_health_gates", "gradlemc:export_scan"),
            "verify", List.of("gradlemc:inspect_instance", "gradlemc:mod_audit", "gradlemc:config_check", "gradlemc:memory_snapshot", "gradlemc:correlate", "gradlemc:evaluate_health_gates", "gradlemc:export_scan"),
            "releasecheck", List.of("gradlemc:inspect_instance", "gradlemc:check_instance_lock", "gradlemc:mod_audit", "gradlemc:config_check", "gradlemc:memory_snapshot", "gradlemc:correlate", "gradlemc:evaluate_health_gates", "gradlemc:export_scan"),
            "scan", List.of("gradlemc:inspect_instance", "gradlemc:mod_audit", "gradlemc:config_check", "gradlemc:memory_snapshot", "gradlemc:correlate", "adaptive:evaluate", "gradlemc:export_scan"),
            "lag", List.of("gradlemc:inspect_instance", "gradlemc:mod_audit", "gradlemc:memory_snapshot", "gradlemc:server_performance", "gradlemc:profile", "gradlemc:correlate", "gradlemc:recommend", "gradlemc:export_scan"));
    private DiagnosticWorkflows() { }
    public static Set<String> ids() { TreeSet<String> ids=new TreeSet<>(WORKFLOWS.keySet());ids.addAll(FOUNDATION_WORKFLOWS);return ids; }
    public static boolean isFoundationWorkflow(String id) { return FOUNDATION_WORKFLOWS.contains(id); }
    public static TaskPlan plan(TaskEngine engine, String id) { List<String> roots = WORKFLOWS.get(id); if (roots == null) return engine.plan(id); LinkedHashMap<String, DiagnosticTask> ordered = new LinkedHashMap<>(); for (String root : roots) for (DiagnosticTask task : engine.plan(root).orderedTasks()) ordered.put(task.id(), task); return new TaskPlan(id, new ArrayList<>(ordered.values())); }
}
