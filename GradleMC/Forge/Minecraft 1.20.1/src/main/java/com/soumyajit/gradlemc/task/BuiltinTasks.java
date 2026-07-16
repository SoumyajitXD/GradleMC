package com.soumyajit.gradlemc.task;

import com.soumyajit.gradlemc.GradleMC;
import com.soumyajit.gradlemc.check.BasicCheckRegistry;
import com.soumyajit.gradlemc.check.CheckContext;
import com.soumyajit.gradlemc.check.CheckResult;
import com.soumyajit.gradlemc.modaudit.ModAuditResult;
import com.soumyajit.gradlemc.modaudit.ModAuditService;
import com.soumyajit.gradlemc.util.RuntimeSnapshots;
import com.soumyajit.gradlemc.instance.MinecraftInstanceService;
import com.soumyajit.gradlemc.instance.MinecraftInstanceSnapshot;
import com.soumyajit.gradlemc.health.HealthGateResult;
import com.soumyajit.gradlemc.health.HealthGateService;
import com.soumyajit.gradlemc.health.HealthGateState;
import com.soumyajit.gradlemc.lock.InstanceLockDiff;
import com.soumyajit.gradlemc.lock.InstanceLockService;
import com.soumyajit.gradlemc.network.NetworkDiagnostics;
import com.soumyajit.gradlemc.storage.*;
import net.minecraftforge.fml.ModList;
import java.time.Instant;
import java.util.*;

public final class BuiltinTasks {
    private BuiltinTasks() { }
    public static void register(TaskEngine engine) {
        engine.register(simple("gradlemc:inspect_environment", "Environment inventory", "Discovery", List.of(), CachePolicy.STATIC_INPUTS, false, context -> { MinecraftInstanceSnapshot snapshot = MinecraftInstanceService.current(context.rerun()); context.accountRecords(snapshot.environment().value().size()+snapshot.runtime().value().size()); return TaskOutcome.success(Map.of("minecraft", snapshot.environment().value().get("minecraft"), "loader", snapshot.environment().value().get("loader"), "fingerprint", snapshot.fingerprint())); }));
        engine.register(simple("gradlemc:inspect_resource_packs", "Resource-pack inventory", "Discovery", List.of(TaskDependency.required("gradlemc:inspect_environment")), CachePolicy.STATIC_INPUTS, false, context -> { MinecraftInstanceSnapshot snapshot = MinecraftInstanceService.current(context.rerun()); accountPacks(context,snapshot.resourcePacks().value()); return snapshot.resourcePacks().availability() == com.soumyajit.gradlemc.instance.ComponentAvailability.UNAVAILABLE ? TaskOutcome.unavailable("resource-packs-unavailable", snapshot.resourcePacks().limitation()) : TaskOutcome.success(Map.of("packs", Integer.toString(snapshot.resourcePacks().value().size()), "capability", snapshot.resourcePacks().limitation())); }));
        engine.register(simple("gradlemc:inspect_shader_packs", "Shader-pack inventory", "Discovery", List.of(TaskDependency.required("gradlemc:inspect_environment")), CachePolicy.STATIC_INPUTS, false, context -> { MinecraftInstanceSnapshot snapshot = MinecraftInstanceService.current(context.rerun()); accountPacks(context,snapshot.shaderPacks().value()); return snapshot.shaderPacks().availability() == com.soumyajit.gradlemc.instance.ComponentAvailability.UNAVAILABLE ? TaskOutcome.unavailable("shader-packs-unavailable", snapshot.shaderPacks().limitation()) : TaskOutcome.success(Map.of("packs", Integer.toString(snapshot.shaderPacks().value().size()), "capability", "DIRECTORY_INVENTORY_ONLY")); }));
        engine.register(simple("gradlemc:inspect_configs", "Configuration inventory", "Discovery", List.of(TaskDependency.required("gradlemc:inspect_environment")), CachePolicy.STATIC_INPUTS, false, context -> { MinecraftInstanceSnapshot snapshot = MinecraftInstanceService.current(context.rerun()); context.accountFiles(snapshot.configs().value().size());context.accountRecords(snapshot.configs().value().size()); return TaskOutcome.success(Map.of("configs", Integer.toString(snapshot.configs().value().size()), "limitation", snapshot.configs().limitation())); }));
        engine.register(simple("gradlemc:inspect_data_packs", "Datapack inventory", "Discovery", List.of(TaskDependency.required("gradlemc:inspect_environment")), CachePolicy.NEVER_CACHE, true, context -> TaskOutcome.unavailable("world-adapter-required", "Enabled datapacks are unavailable until a world/server provider is registered.")));
        engine.register(simple("gradlemc:inspect_instance", "Complete lightweight instance inventory", "Discovery", List.of(TaskDependency.required("gradlemc:inspect_environment"), TaskDependency.optional("gradlemc:inspect_resource_packs"), TaskDependency.optional("gradlemc:inspect_shader_packs"), TaskDependency.optional("gradlemc:inspect_configs"), TaskDependency.optional("gradlemc:inspect_data_packs")), CachePolicy.STATIC_INPUTS, false, context -> { MinecraftInstanceSnapshot snapshot = MinecraftInstanceService.current(context.rerun()); return TaskOutcome.success(Map.of("fingerprint", snapshot.fingerprint(), "providers", Integer.toString(snapshot.providers().value().size()))); }));
        engine.register(simple("gradlemc:environment", "Environment inventory (compatibility alias)", "Discovery", List.of(TaskDependency.required("gradlemc:inspect_environment")), CachePolicy.STATIC_INPUTS, false, context -> TaskOutcome.success(Map.of("alias", "gradlemc:inspect_environment"))));
        engine.register(simple("gradlemc:mod_audit", "Mod audit", "Discovery", List.of(TaskDependency.required("gradlemc:environment")), CachePolicy.STATIC_INPUTS, false, context -> { ModAuditResult audit = context.rerun() ? ModAuditService.refresh() : ModAuditService.current(); return TaskOutcome.success(Map.of("mods", Integer.toString(audit.snapshot().mods().size()), "findings", Integer.toString(audit.findings().size()), "cached", Boolean.toString(audit.fromCache()))); }));
        engine.register(simple("gradlemc:config_check", "Configuration sanity", "Discovery", List.of(TaskDependency.required("gradlemc:environment")), CachePolicy.STATIC_INPUTS, false, context -> { List<CheckResult> checks = BasicCheckRegistry.runDefaultChecks(new CheckContext(context.server(), com.soumyajit.gradlemc.util.GradleMcPaths.reportDirectory(), Instant.now())); long problems = checks.stream().filter(c -> !"PASS".equals(c.severity().name()) && !"INFO".equals(c.severity().name())).count(); return TaskOutcome.success(Map.of("checks", Integer.toString(checks.size()), "problems", Long.toString(problems))); }));
        engine.register(simple("gradlemc:memory_snapshot", "Memory snapshot", "Runtime", List.of(TaskDependency.required("gradlemc:environment")), CachePolicy.NEVER_CACHE, false, context -> { RuntimeSnapshots.MemorySnapshot m = RuntimeSnapshots.memory(); return TaskOutcome.success(Map.of("usedMiB", Long.toString(m.usedMiB()), "maxMiB", Long.toString(m.maxMiB()), "pressure", m.pressureLabel())); }));
        engine.register(simple("gradlemc:server_performance", "Server performance evidence", "Runtime", List.of(TaskDependency.required("gradlemc:environment")), CachePolicy.NEVER_CACHE, true, context -> TaskOutcome.unavailable("bounded-sample-required", "Run /gradlemc perf start <seconds> first; this workflow will reference its local report.")));
        engine.register(simple("gradlemc:profile", "Profiler evidence", "Runtime", List.of(TaskDependency.required("gradlemc:environment")), CachePolicy.NEVER_CACHE, true, context -> TaskOutcome.unavailable("bounded-profile-required", "Run /gradlemc profiler start first; native profiling is not implied.")));
        engine.register(simple("gradlemc:correlate", "Evidence correlation", "Analysis", List.of(TaskDependency.required("gradlemc:environment"), TaskDependency.required("gradlemc:memory_snapshot"), TaskDependency.optional("gradlemc:mod_audit"), TaskDependency.optional("gradlemc:server_performance"), TaskDependency.optional("gradlemc:profile")), CachePolicy.NEVER_CACHE, false, context -> TaskOutcome.success(Map.of("conclusion", "Correlation records associations only; unavailable runtime evidence is a limitation."))));
        engine.register(simple("adaptive:evaluate", "Adaptive evidence evaluation", "Analysis", List.of(TaskDependency.required("gradlemc:correlate")), CachePolicy.NEVER_CACHE, false, context -> TaskOutcome.success(Map.of("mode", "typed-evidence-only", "limitations", "No timed benchmark is started; unavailable evidence remains explicit."))));
        engine.register(simple("gradlemc:recommend", "Recommendations", "Analysis", List.of(TaskDependency.required("gradlemc:correlate")), CachePolicy.NEVER_CACHE, false, context -> TaskOutcome.success(Map.of("recommendations", "Review only reversible, evidence-backed actions."))));
        engine.register(simple("gradlemc:evaluate_health_gates", "Evaluate local health gates", "Verification", List.of(TaskDependency.required("gradlemc:correlate")), CachePolicy.NEVER_CACHE, false, context -> {
            List<HealthGateResult> gates = HealthGateService.evaluateOutcomes(context.outcomes());
            long passed = gates.stream().filter(g -> g.state() == HealthGateState.PASS).count();
            long failed = gates.stream().filter(g -> g.state() == HealthGateState.FAIL).count();
            long inconclusive = gates.stream().filter(g -> g.state() == HealthGateState.INCONCLUSIVE).count();
            return failed > 0 ? TaskOutcome.failed("health-gate-failed", failed + " health gate(s) failed")
                    : TaskOutcome.success(Map.of("gates", Integer.toString(gates.size()), "passed", Long.toString(passed),
                    "failed", Long.toString(failed), "inconclusive", Long.toString(inconclusive)));
        }));
        engine.register(simple("gradlemc:check_instance_lock", "Check normalized instance lock", "Verification", List.of(TaskDependency.required("gradlemc:inspect_instance")), CachePolicy.NEVER_CACHE, false, context -> {
            try { InstanceLockDiff diff=InstanceLockService.diffCurrent(); return diff.matches()?TaskOutcome.success(Map.of("matches","true","unavailable",Integer.toString(diff.unavailable().size()))):TaskOutcome.failed("instance-lock-drift","Instance lock differs: added="+diff.added().size()+", removed="+diff.removed().size()+", changed="+diff.changed().size()+", reordered="+diff.reordered().size()); }
            catch (java.io.IOException exception) { return TaskOutcome.unavailable("instance-lock-unavailable", exception.getMessage()); }
        }));
        engine.register(simple("gradlemc:inspect_network", "Inspect safe GradleMC network metadata", "Runtime", List.of(TaskDependency.required("gradlemc:inspect_environment")), CachePolicy.NEVER_CACHE, false, context -> {NetworkDiagnostics.Snapshot snapshot=NetworkDiagnostics.snapshot();long packets=snapshot.packets().values().stream().mapToLong(NetworkDiagnostics.PacketSummary::count).sum();long bytes=snapshot.packets().values().stream().mapToLong(NetworkDiagnostics.PacketSummary::approximateBytes).sum();context.accountRecords(snapshot.packets().size());return TaskOutcome.success(Map.of("packetGroups",Integer.toString(snapshot.packets().size()),"packets",Long.toString(packets),"approximateBytes",Long.toString(bytes),"scope","gradlemc-channel-only"));}));
        engine.register(simple("gradlemc:inspect_storage", "Inspect GradleMC-owned storage", "Discovery", List.of(TaskDependency.required("gradlemc:inspect_environment")), CachePolicy.NEVER_CACHE, false, context -> {StorageSummary s=StorageDiagnostics.inspect();context.accountFiles(s.reportFiles());context.accountRecords(s.reportFiles()+s.staleTemporaryFiles().size());return TaskOutcome.success(Map.of("gameUsableBytes",Long.toString(s.gameUsableBytes()),"outputUsableBytes",Long.toString(s.outputUsableBytes()),"reportsWritable",Boolean.toString(s.reportsWritable()),"reportBytes",Long.toString(s.reportBytes()),"reportFiles",Integer.toString(s.reportFiles()),"staleTemporaryFiles",Integer.toString(s.staleTemporaryFiles().size())));}));
        engine.register(simple("gradlemc:export_scan", "Export GradleMC Scan", "Reporting", List.of(TaskDependency.required("gradlemc:correlate"), TaskDependency.optional("gradlemc:evaluate_health_gates")), CachePolicy.NEVER_CACHE, false, context -> {
            GradleMcScanWriter.ScanFiles files = new GradleMcScanWriter().writeOutcomes(context.requestedId(), context.outcomes());
            return TaskOutcome.success(Map.of("format", "txt,json", "text", files.text().getFileName().toString(), "json", files.json().getFileName().toString()));
        }));
    }
    private static DiagnosticTask simple(String id, String name, String group, List<TaskDependency> deps, CachePolicy policy, boolean server, Runner runner) { return new DiagnosticTask() {
        public String id(){return id;} public String displayName(){return name;} public String description(){return name + ".";} public String group(){return group;} public String version(){return "1";} public List<TaskDependency> dependencies(){return deps;} public CachePolicy cachePolicy(){return policy;} public boolean requiresServer(){return server;} public long timeoutMillis(){return 30_000L;} public Map<String,String> inputs(TaskRunContext c){return Map.of("taskVersion", version(), "minecraft", GradleMC.CURRENT_MINECRAFT_VERSION, "side", c.server()==null?"client-or-console":"server");} public TaskOutcome execute(TaskRunContext c) throws Exception{return runner.run(c);}
        public List<String> declaredOutputs(){return BuiltinTasks.declaredOutputs(id);} public List<String> capabilities(){return server ? List.of("active-server") : List.of();}
        public TaskCost cost(){return group.equals("Runtime") ? TaskCost.MODERATE : TaskCost.CHEAP;}
    }; }
    private static List<String> declaredOutputs(String id) {
        return switch (id) {
            case "gradlemc:inspect_environment" -> List.of("minecraft", "loader", "fingerprint");
            case "gradlemc:inspect_resource_packs", "gradlemc:inspect_shader_packs" -> List.of("packs", "capability");
            case "gradlemc:inspect_configs" -> List.of("configs", "limitation");
            case "gradlemc:inspect_instance" -> List.of("fingerprint", "providers");
            case "gradlemc:mod_audit" -> List.of("mods", "findings", "cached");
            case "gradlemc:config_check" -> List.of("checks", "problems");
            case "gradlemc:memory_snapshot" -> List.of("usedMiB", "maxMiB", "pressure");
            case "adaptive:evaluate" -> List.of("mode", "limitations");
            case "gradlemc:evaluate_health_gates" -> List.of("gates", "passed", "failed", "inconclusive");
            case "gradlemc:export_scan" -> List.of("format", "text", "json");
            case "gradlemc:check_instance_lock" -> List.of("matches", "unavailable");
            case "gradlemc:inspect_network" -> List.of("packetGroups","packets","approximateBytes","scope");
            case "gradlemc:inspect_storage" -> List.of("gameUsableBytes","outputUsableBytes","reportsWritable","reportBytes","reportFiles","staleTemporaryFiles");
            default -> List.of();
        };
    }
    private static void accountPacks(TaskRunContext context,List<com.soumyajit.gradlemc.instance.PackDescriptor> packs){context.accountFiles(packs.size());context.accountRecords(packs.size());}
    @FunctionalInterface private interface Runner { TaskOutcome run(TaskRunContext context) throws Exception; }
}
