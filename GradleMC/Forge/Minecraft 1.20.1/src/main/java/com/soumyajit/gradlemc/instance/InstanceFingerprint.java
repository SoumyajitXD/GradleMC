package com.soumyajit.gradlemc.instance;

import com.soumyajit.gradlemc.task.TaskEngine;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

public final class InstanceFingerprint {
    private InstanceFingerprint() { }
    public static String calculate(Map<String, String> environment, Map<String, String> runtime,
                                   java.util.List<PackDescriptor> resources, java.util.List<PackDescriptor> shaders,
                                   java.util.List<PackDescriptor> dataPacks, java.util.List<String> modIds) {
        Map<String, String> values = new LinkedHashMap<>();
        copy(values, "env", environment); copy(values, "runtime", runtime);
        values.put("mods", modIds.stream().sorted().collect(Collectors.joining(",")));
        values.put("resources", packs(resources)); values.put("shaders", packs(shaders)); values.put("datapacks", packs(dataPacks));
        return TaskEngine.fingerprint(values);
    }
    private static void copy(Map<String, String> to, String prefix, Map<String, String> from) {
        if (from != null) from.forEach((key, value) -> to.put(prefix + "." + key, value == null ? "" : value));
    }
    private static String packs(java.util.List<PackDescriptor> packs) {
        if (packs == null) return "";
        return packs.stream().map(p -> p.id() + "@" + p.lastModifiedMillis() + ":" + p.sizeBytes()).sorted().collect(Collectors.joining(","));
    }
}
