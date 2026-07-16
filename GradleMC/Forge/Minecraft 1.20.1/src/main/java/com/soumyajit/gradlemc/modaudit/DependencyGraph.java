package com.soumyajit.gradlemc.modaudit;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public final class DependencyGraph {
    private final Map<String, List<String>> reverse;
    private DependencyGraph(Map<String, List<String>> reverse) { this.reverse = reverse; }
    public static DependencyGraph of(InstalledModSnapshot snapshot) {
        Map<String, List<String>> values = new TreeMap<>();
        snapshot.mods().forEach(mod -> mod.dependencies().forEach(dep -> values.computeIfAbsent(dep.modId(), ignored -> new ArrayList<>()).add(mod.modId())));
        values.replaceAll((key, value) -> value.stream().sorted().toList()); return new DependencyGraph(Map.copyOf(values));
    }
    public List<String> dependantsOf(String modId) { return reverse.getOrDefault(ModDescriptor.normalize(modId), List.of()); }
    public Map<String, List<String>> reverse() { return reverse; }
}
