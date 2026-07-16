package com.soumyajit.gradlemc.foundation;

import com.soumyajit.gradlemc.instance.MinecraftInstanceService;
import com.soumyajit.gradlemc.instance.MinecraftInstanceSnapshot;
import com.soumyajit.gradlemc.scan.GradleMcScanV1Writer;
import com.soumyajit.gradlemc.util.GradleMcPaths;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.*;

/** Internal bridge; only the real, lightweight instance:snapshot proof task is registered. */
public final class FoundationService {
    private static final TaskRegistry REGISTRY = new TaskRegistry();
    private static FoundationEngine engine = new FoundationEngine(REGISTRY);
    static { registerScanV1Tasks(); }
    private FoundationService() { }
    public static List<TaskCore.Definition> tasks() { return REGISTRY.all(); }
    public static synchronized TaskCore.Plan dryRun(TaskId id, Clock clock) { return engine.dryRun(id, context(clock), false); }
    public static List<TaskCore.TaskResult> run(TaskId id, Clock clock, boolean force) { FoundationEngine current; synchronized(FoundationService.class){current=engine;} return current.execute(id, context(clock), force); }
    public static synchronized boolean cancel() { return engine.cancelActive(); }
    public static synchronized List<TaskCore.TaskResult> history() { return engine.history(); }
    public static synchronized void resetExecutor() { engine.close(); engine = new FoundationEngine(REGISTRY); }
    private static TaskCore.Context context(Clock clock) {
        InstanceSnapshot snapshot = capture();
        return new TaskCore.Context(clock, new TaskCore.CancellationToken(), snapshot.staticFingerprint(), snapshot.runtimeFingerprint(), Map.of());
    }
    private static void registerScanV1Tasks() {
        register("instance:identity", List.of(), TaskCore.CachePolicy.STATIC_FINGERPRINT, "identity", s -> Map.of("minecraft",s.minecraftVersion(),"loader",s.loaderName(),"gradlemc",s.gradleMcVersion(),"java",s.javaVersion()));
        register("mods:inventory", List.of(), TaskCore.CachePolicy.STATIC_FINGERPRINT, "mod inventory", s -> Map.of("count",s.installedModSummary().getOrDefault("count","0")));
        register("mods:dependencies", List.of(TaskId.of("mods:inventory")), TaskCore.CachePolicy.STATIC_FINGERPRINT, "declared dependency metadata", s -> Map.of("status","available through Forge loaded-mod metadata"));
        register("packs:resource_inventory", List.of(), TaskCore.CachePolicy.STATIC_FINGERPRINT, "resource-pack inventory", s -> Map.of("count",Integer.toString(s.resourcePacks().size())));
        register("packs:datapack_inventory", List.of(), TaskCore.CachePolicy.STATIC_FINGERPRINT, "datapack inventory", s -> Map.of("count",Integer.toString(s.dataPacks().size())));
        register("configs:inventory", List.of(), TaskCore.CachePolicy.STATIC_FINGERPRINT, "bounded config inventory", s -> Map.of("count",s.configSummary().getOrDefault("count","0")));
        register("jvm:environment", List.of(TaskId.of("instance:identity")), TaskCore.CachePolicy.STATIC_FINGERPRINT, "JVM allowlist environment", s -> Map.of("java",s.javaVersion(),"vendor",s.javaVendor()));
        register("memory:snapshot", List.of(TaskId.of("jvm:environment")), TaskCore.CachePolicy.NEVER_CACHE, "instantaneous memory snapshot", s -> Map.of("note","instantaneous; not a trend or leak detector"));
        REGISTRY.register(new TaskCore.Definition(TaskId.of("instance:snapshot"), "Immutable instance snapshot", "instance", List.of(TaskId.of("instance:identity"),TaskId.of("mods:inventory"),TaskId.of("mods:dependencies"),TaskId.of("packs:resource_inventory"),TaskId.of("packs:datapack_inventory"),TaskId.of("configs:inventory"),TaskId.of("jvm:environment"),TaskId.of("memory:snapshot")), TaskCore.Affinity.CALLER_THREAD, Duration.ofSeconds(3), TaskCore.CachePolicy.STATIC_FINGERPRINT, c->TaskCore.Availability.present(), c->output("instance:snapshot","Minecraft instance snapshot","Captured immutable bounded summaries; unavailable components remain explicit.",capture(),Map.of())));
        REGISTRY.register(new TaskCore.Definition(TaskId.of("scan:standard"), "Write GradleMC Scan v1", "scan", List.of(TaskId.of("instance:snapshot")), TaskCore.Affinity.BACKGROUND_SAFE, Duration.ofSeconds(15), TaskCore.CachePolicy.NEVER_CACHE, c->TaskCore.Availability.present(), c->{ MinecraftInstanceSnapshot live=MinecraftInstanceService.current(false); GradleMcScanV1Writer.Result result=new GradleMcScanV1Writer().write(GradleMcPaths.gameDirectory(),live,new ArrayList<>(c.dependencies().values())); return output("scan:standard","GradleMC Scan v1","Wrote local bounded Scan v1 files.",capture(),Map.of("scanId",result.scanId(),"location","gradlemc/scans/"+result.scanId(),"atomicMoveFallback",Boolean.toString(result.atomicMoveFallback()))); }));
    }
    private static void register(String raw, List<TaskId> dependencies, TaskCore.CachePolicy policy, String title, java.util.function.Function<InstanceSnapshot,Map<String,String>> fields) {
        TaskId id=TaskId.of(raw); REGISTRY.register(new TaskCore.Definition(id,title,"scan",dependencies,TaskCore.Affinity.CALLER_THREAD,Duration.ofSeconds(3),policy,c->TaskCore.Availability.present(),c->output(raw,title,"Collected from GradleMC's existing bounded local collector.",capture(),fields.apply(capture()))));
    }
    private static TaskCore.Output output(String raw,String title,String description,InstanceSnapshot snapshot,Map<String,String> fields) {
        Evidence evidence=new Evidence(raw+".observed",Evidence.Classification.OBSERVED_FACT,title,description,TaskId.of(raw),Instant.now(),new Freshness(Freshness.State.FRESH,Instant.now(),Optional.empty(),"GradleMC local collector","Collected on demand",List.of()),Optional.of(snapshot.staticFingerprint()),Optional.of(snapshot.runtimeFingerprint()),fields,Evidence.Confidence.HIGH,List.of("Unavailable collector components are reported separately; no causal conclusion is implied."),Optional.empty(),List.of(),1);return new TaskCore.Output(List.of(evidence),description);
    }
    private static InstanceSnapshot capture() {
        MinecraftInstanceSnapshot old = MinecraftInstanceService.current(false);
        Map<String, String> env = old.environment().value(); Map<String, String> runtime = old.runtime().value();
        Map<String, String> inputs = new TreeMap<>(); inputs.putAll(env); inputs.putAll(runtime);
        inputs.put("mods", Integer.toString(old.mods().value().mods().size()));
        old.mods().value().mods().stream().map(m -> m.normalizedIdentifier()).sorted().forEach(m -> inputs.put("mod:" + m, m));
        old.resourcePacks().value().forEach(p -> inputs.put("resource:" + p.id(), p.fileName()));
        old.configs().value().forEach(c -> inputs.put("config:" + c.fileName(), c.fileName() + ":" + c.sizeBytes()));
        StaticFingerprint stat = StaticFingerprint.of(1, inputs);
        InstanceSnapshot.RuntimeContext runtimeContext = new InstanceSnapshot.RuntimeContext(false, false, Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), InstanceSnapshot.ShaderState.UNKNOWN, InstanceSnapshot.Activity.MAIN_MENU);
        RuntimeContextFingerprint run = RuntimeContextFingerprint.of(1, stat, Map.of("world", "none", "activity", runtimeContext.activity().name(), "shader", runtimeContext.shaderState().name()));
        return new InstanceSnapshot(env.getOrDefault("gradlemc", "1.0.3"), env.getOrDefault("minecraft", "unknown"), env.getOrDefault("loader", "Forge"), "unknown", runtime.getOrDefault("java", "unknown"), runtime.getOrDefault("vendor", "unknown"), "common", old.collectedAt(), 1, Map.of("count", Integer.toString(old.mods().value().mods().size())), old.resourcePacks().value().stream().map(p -> p.id()).toList(), List.of(), Map.of("count", Integer.toString(old.configs().value().size())), Map.of(), runtimeContext, stat, run);
    }
}
