package com.soumyajit.gradlemc.modaudit;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.util.List;
import java.util.Map;

/** Validates deterministic, typed JSON output without requiring a running Fabric Loader instance. */
public final class FabricModAuditModelSelfTest {
    private FabricModAuditModelSelfTest() {
    }

    public static void run() {
        FabricModAuditService.Dependency required = new FabricModAuditService.Dependency(
                "fabricloader", "depends", true, true, ">=0.19.3", true, true);
        FabricModAuditService.Dependency optional = new FabricModAuditService.Dependency(
                "optionalmod", "recommends", true, false, "*", false, null);
        FabricModAuditService.Dependency conflict = new FabricModAuditService.Dependency(
                "badmod", "breaks", false, false, "*", true, true);
        FabricModAuditService.ModDescriptor nested = new FabricModAuditService.ModDescriptor(
                "nested", "", "1.0", "", "CLIENT", "PATH", List.of(), "parent", List.of(), List.of(), Map.of(), List.of());
        FabricModAuditService.ModDescriptor parent = new FabricModAuditService.ModDescriptor(
                "parent", "Parent", "1.0", "quoted \"description\"", "UNIVERSAL", "PATH", List.of("parent.jar"), "",
                List.of("nested"), List.of("Example"), Map.of("homepage", "https://example.invalid"), List.of(required, optional, conflict));
        FabricModAuditService.Audit audit = new FabricModAuditService.Audit(List.of(nested, parent), List.of(
                new FabricModAuditService.Finding("declared_conflict", "parent", "breaks declaration matches")));

        String json = FabricModAuditReportWriter.jsonFor(audit);
        JsonObject root = JsonParser.parseString(json).getAsJsonObject();
        require("Fabric".equals(root.get("loader").getAsString()), "JSON must retain Fabric identity");
        require(root.getAsJsonArray("mods").size() == 2, "JSON must retain every logical mod");
        JsonObject first = root.getAsJsonArray("mods").get(0).getAsJsonObject();
        require(first.has("originKind") && first.has("containedMods") && first.has("contacts"), "normalized metadata fields must be present");
        JsonObject secondDependency = root.getAsJsonArray("mods").get(1).getAsJsonObject()
                .getAsJsonArray("dependencies").get(1).getAsJsonObject();
        require(secondDependency.get("required").isJsonPrimitive(), "dependency required must be a JSON boolean");
        require(secondDependency.get("matchesLoadedVersion").isJsonNull(), "unloaded optional dependency must be explicitly unavailable");
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
