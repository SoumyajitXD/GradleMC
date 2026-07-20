package com.soumyajit.gradlemc.task;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/** Registry and deterministic graph planner. It deliberately has no loader or GUI references. */
public final class TaskEngine {
    private final Map<String, DiagnosticTask> tasks = new TreeMap<>();

    public TaskEngine(Collection<DiagnosticTask> definitions) {
        for (DiagnosticTask definition : definitions) register(definition);
        tasks.keySet().forEach(this::plan);
    }

    public synchronized void register(DiagnosticTask task) {
        if (task == null) throw new IllegalArgumentException("task is required");
        if (tasks.putIfAbsent(task.id(), task) != null) throw new IllegalArgumentException("Duplicate task id: " + task.id());
    }

    public List<String> taskIds() { return List.copyOf(tasks.keySet()); }
    public DiagnosticTask task(String id) { return tasks.get(id); }
    public List<DiagnosticTask> definitions() { return List.copyOf(tasks.values()); }

    public List<String> plan(String target) { return planAll(List.of(target)).stream().map(DiagnosticTask::id).toList(); }

    public List<DiagnosticTask> planAll(Collection<String> targets) {
        List<DiagnosticTask> ordered = new ArrayList<>();
        LinkedHashSet<String> visiting = new LinkedHashSet<>();
        LinkedHashSet<String> visited = new LinkedHashSet<>();
        targets.stream().sorted().forEach(target -> visit(target, visiting, visited, ordered));
        return List.copyOf(ordered);
    }

    private void visit(String id, LinkedHashSet<String> visiting, LinkedHashSet<String> visited, List<DiagnosticTask> ordered) {
        DiagnosticTask task = tasks.get(id);
        if (task == null) throw new IllegalArgumentException("Missing task dependency: " + id);
        if (visited.contains(id)) return;
        if (!visiting.add(id)) throw new IllegalArgumentException("Task dependency cycle: " + String.join(" -> ", visiting) + " -> " + id);
        task.dependencies().stream().sorted(Comparator.comparing(TaskDependency::id)).forEach(dependency -> {
            if (tasks.containsKey(dependency.id())) visit(dependency.id(), visiting, visited, ordered);
            else if (dependency.required()) throw new IllegalArgumentException("Missing required task dependency: " + dependency.id() + " for " + id);
        });
        visiting.remove(id);
        visited.add(id);
        ordered.add(task);
    }
}
