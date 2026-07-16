package com.soumyajit.gradlemc.scan;

import com.soumyajit.gradlemc.GradleMC;
import com.soumyajit.gradlemc.adaptive.AdaptiveDiagnostics;
import com.soumyajit.gradlemc.foundation.*;
import com.soumyajit.gradlemc.instance.*;
import com.soumyajit.gradlemc.modaudit.*;
import com.soumyajit.gradlemc.util.ManagedPathSafety;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.Instant;
import java.util.*;

/** Local-only Scan v1 writer.  Inputs are immutable summaries; it never serializes Forge objects. */
public final class GradleMcScanV1Writer {
    public static final String SCHEMA_VERSION = "gradlemc-scan-v1";
    public static final int MAX_MODS = 512, MAX_PACKS = 256, MAX_CONFIGS = 512, MAX_EVIDENCE = 128;
    public static final int MAX_TASKS = 256, MAX_RECOMMENDATIONS = 64, MAX_LIST_VALUES = 64;
    public static final int MAX_FILE_BYTES = 2 * 1024 * 1024;
    public record Result(String scanId, Path directory, boolean atomicMoveFallback) { }

    public Result write(Path gameDirectory, MinecraftInstanceSnapshot snapshot, List<TaskCore.TaskResult> tasks) throws IOException {
        return write(gameDirectory, snapshot, tasks, null);
    }
    /** Writes an already-evaluated immutable result; evaluation itself is deliberately outside I/O. */
    public Result write(Path gameDirectory, MinecraftInstanceSnapshot snapshot, List<TaskCore.TaskResult> tasks, AdaptiveDiagnostics.Evaluation adaptive) throws IOException {
        Objects.requireNonNull(gameDirectory); Objects.requireNonNull(snapshot);
        String id = "scan-" + Instant.now().toEpochMilli() + "-" + UUID.randomUUID().toString().substring(0, 8);
        Path root = gameDirectory.toAbsolutePath().normalize().resolve("gradlemc").resolve("scans").normalize();
        ManagedPathSafety.ensureDirectory(gameDirectory, root);
        Path directory = root.resolve(id).normalize();
        if (!directory.startsWith(root) || !id.matches("[a-z0-9-]{12,80}") || Files.exists(directory, LinkOption.NOFOLLOW_LINKS)) throw new IOException("Unsafe scan directory");
        Path staging = root.resolve("." + id + ".incomplete").normalize();
        if (!staging.startsWith(root) || Files.exists(staging, LinkOption.NOFOLLOW_LINKS)) throw new IOException("Unsafe scan staging directory");
        Files.createDirectory(staging);
        List<TaskCore.TaskResult> ordered = tasks == null ? List.of() : tasks.stream().sorted(Comparator.comparing(r -> r.id().toString())).limit(MAX_TASKS).toList();
        List<String> limitations = limitations(snapshot, ordered);
        boolean fallback = false;
        try {
            fallback |= writeFile(staging.resolve("snapshot.json"), snapshotJson(snapshot, gameDirectory));
            fallback |= writeFile(staging.resolve("evidence.json"), evidenceJson(ordered));
            fallback |= writeFile(staging.resolve("tasks.json"), tasksJson(ordered));
            if (adaptive != null) fallback |= writeFile(staging.resolve("recommendations.json"), recommendationsJson(adaptive));
            fallback |= writeFile(staging.resolve("limitations.txt"), String.join("\n", limitations) + "\n");
            fallback |= writeFile(staging.resolve("summary.txt"), summary(id, snapshot, ordered, limitations));
            // A Scan becomes visible only after every required file exists; manifest is written last.
            fallback |= writeFile(staging.resolve("manifest.json"), manifest(id, snapshot, ordered, limitations, fallback, adaptive));
            try { Files.move(staging, directory, StandardCopyOption.ATOMIC_MOVE); }
            catch (AtomicMoveNotSupportedException exception) { Files.move(staging, directory); fallback = true; }
            return new Result(id, directory, fallback);
        } finally { deleteStaging(staging); }
    }

    private static List<String> limitations(MinecraftInstanceSnapshot s, List<TaskCore.TaskResult> tasks) {
        TreeSet<String> all = new TreeSet<>();
        all.add("Local-only Scan v1; no telemetry, uploads, JAR hashing, archive unpacking, or config contents.");
        all.add("Timed FPS, TPS/MSPT, profiler, entity, and worldgen diagnostics are not started by standard Scan.");
        for (TaskCore.TaskResult task : tasks) if (task.state() != TaskCore.State.SUCCEEDED) all.add(task.id()+": "+task.reason());
        for (InstanceComponent<?> c : List.of(s.resourcePacks(), s.dataPacks(), s.configs(), s.world())) if (!c.limitation().isBlank()) all.add(c.limitation());
        return all.stream().limit(32).toList();
    }
    private static String manifest(String id, MinecraftInstanceSnapshot s, List<TaskCore.TaskResult> tasks, List<String> limits, boolean fallback, AdaptiveDiagnostics.Evaluation adaptive) {
        boolean complete = tasks.stream().allMatch(t -> t.state()==TaskCore.State.SUCCEEDED)
                && List.of(s.mods(),s.resourcePacks(),s.dataPacks(),s.configs(),s.world()).stream().noneMatch(c -> c.availability()==ComponentAvailability.UNAVAILABLE);
        String adaptiveState=adaptive==null?"NOT_EVALUATED":"EVALUATED"; int rules=adaptive==null?0:adaptive.ruleResults().size();int matches=adaptive==null?0:(int)adaptive.ruleResults().stream().filter(r->r.outcome()==AdaptiveDiagnostics.RuleOutcome.MATCHED).count();int recs=adaptive==null?0:adaptive.recommendations().size();
        return "{\n\"schemaVersion\":\""+SCHEMA_VERSION+"\",\n\"scanId\":\""+id+"\",\n\"gradleMcVersion\":\""+esc(s.environment().value().getOrDefault("gradlemc", "unknown"))+"\",\n\"minecraftVersion\":\""+esc(s.environment().value().getOrDefault("minecraft", "unknown"))+"\",\n\"loader\":\"Forge\",\n\"loaderVersion\":\""+esc(s.environment().value().getOrDefault("loaderVersion", "unknown"))+"\",\n\"javaVersion\":\""+esc(s.runtime().value().getOrDefault("java", "unknown"))+"\",\n\"createdAt\":\""+s.collectedAt()+"\",\n\"completedAt\":\""+Instant.now()+"\",\n\"workflowId\":\"scan:standard\",\n\"requestedRootTask\":\"scan:standard\",\n\"staticFingerprint\":\""+esc(s.fingerprint())+"\",\n\"runtimeContextFingerprint\":\"unknown\",\n\"redactionPolicyVersion\":\"scan-path-v1\",\n\"completionState\":\""+(complete?"COMPLETE":"PARTIAL")+"\",\n\"adaptiveEvaluationState\":\""+adaptiveState+"\",\n\"adaptiveRuleCount\":"+rules+",\n\"adaptiveMatchedCount\":"+matches+",\n\"adaptiveRecommendationCount\":"+recs+",\n\"adaptiveTruncated\":"+(adaptive!=null&&adaptive.truncated())+",\n\"atomicMoveFallback\":"+fallback+",\n\"limitations\":"+array(limits)+"\n}\n";
    }
    private static String recommendationsJson(AdaptiveDiagnostics.Evaluation value) { StringBuilder b=new StringBuilder("{\n\"schemaVersion\":").append(AdaptiveDiagnostics.RECOMMENDATION_SCHEMA_VERSION).append(",\n\"ruleSetVersion\":\"adaptive-rules-v").append(AdaptiveDiagnostics.RULE_SET_VERSION).append("\",\n\"truncated\":").append(value.truncated()).append(",\n\"missingEvidence\":").append(array(value.missingEvidence())).append(",\n\"recommendations\":["); int i=0; for(AdaptiveDiagnostics.Recommendation r:value.recommendations().stream().limit(MAX_RECOMMENDATIONS).toList()){if(i++>0)b.append(',');b.append("{\"id\":\"").append(esc(r.id())).append("\",\"state\":\"").append(r.state()).append("\",\"classification\":\"").append(r.classification()).append("\",\"confidence\":\"").append(r.confidence()).append("\",\"supportingEvidenceIds\":").append(array(r.supportingEvidenceIds())).append(",\"ruleIds\":").append(array(r.ruleIds())).append(",\"limitations\":").append(array(r.limitations())).append("}");} return b.append("]\n}\n").toString(); }
    private static String snapshotJson(MinecraftInstanceSnapshot s, Path game) {
        StringBuilder out=new StringBuilder("{\n\"schemaVersion\":\""+SCHEMA_VERSION+"\",\n\"identity\":"); map(out,s.environment().value());
        out.append(",\n\"jvmEnvironment\":");map(out,s.runtime().value());
        out.append(",\n\"componentStates\":");componentStates(out,s);
        out.append(",\n\"mods\":[");int i=0;InstalledModSnapshot loaded=s.mods().value();for(ModDescriptor m:(loaded==null?List.<ModDescriptor>of():loaded.mods()).stream().sorted(Comparator.comparing(ModDescriptor::normalizedIdentifier)).limit(MAX_MODS).toList()){if(i++>0)out.append(',');out.append("{\"id\":\"").append(esc(m.normalizedIdentifier())).append("\",\"name\":\"").append(esc(bound(m.displayName(),256))).append("\",\"version\":\"").append(esc(bound(m.version(),128))).append("\",\"sourceJar\":\"").append(esc(bound(m.jarFileName(),160))).append("\",\"dependencyCount\":").append(Math.min(m.dependencies().size(),128)).append("}");}
        out.append("],\n\"resourcePacks\":");packs(out,s.resourcePacks().value());out.append(",\n\"datapacks\":");packs(out,s.dataPacks().value());out.append(",\n\"configs\":[");i=0;for(ConfigDescriptor c:(s.configs().value()==null?List.<ConfigDescriptor>of():s.configs().value()).stream().sorted(Comparator.comparing(ConfigDescriptor::fileName)).limit(MAX_CONFIGS).toList()){if(i++>0)out.append(',');out.append("{\"fileName\":\"").append(esc(bound(c.fileName(),160))).append("\",\"extension\":\"").append(esc(c.extension())).append("\",\"sizeBytes\":").append(c.sizeBytes()).append(",\"state\":\"").append(esc(c.parseStatus())).append("\"}");}out.append("]\n}\n");return out.toString();
    }
    private static void packs(StringBuilder out,List<PackDescriptor> values){out.append('[');int i=0;for(PackDescriptor p:(values==null?List.<PackDescriptor>of():values).stream().sorted(Comparator.comparing(PackDescriptor::id)).limit(MAX_PACKS).toList()){if(i++>0)out.append(',');out.append("{\"id\":\"").append(esc(bound(p.id(),160))).append("\",\"fileName\":\"").append(esc(bound(p.fileName(),160))).append("\",\"kind\":\"").append(p.kind()).append("\"}");}out.append(']');}
    private static void componentStates(StringBuilder out,MinecraftInstanceSnapshot s){out.append('{');component(out,"mods",s.mods(),false);component(out,"resourcePacks",s.resourcePacks(),true);component(out,"datapacks",s.dataPacks(),true);component(out,"configs",s.configs(),true);component(out,"world",s.world(),true);out.append('}');}
    private static void component(StringBuilder out,String name,InstanceComponent<?> value,boolean comma){if(comma)out.append(',');out.append('"').append(name).append("\":{\"availability\":\"").append(value.availability()).append("\",\"fresh\":").append(value.fresh()).append(",\"limitation\":\"").append(esc(bound(value.limitation(),1024))).append("\"}");}
    private static String evidenceJson(List<TaskCore.TaskResult> tasks){List<Evidence> evidence=tasks.stream().flatMap(t->t.output().stream()).flatMap(o->o.evidence().stream()).sorted(Comparator.comparing(Evidence::id)).limit(MAX_EVIDENCE).toList();StringBuilder out=new StringBuilder("[\n");for(int i=0;i<evidence.size();i++){Evidence e=evidence.get(i);if(i>0)out.append(',');out.append("{\"id\":\"").append(esc(e.id())).append("\",\"classification\":\"").append(e.classification()).append("\",\"sourceTask\":\"").append(e.sourceTask()).append("\",\"title\":\"").append(esc(bound(e.title(),512))).append("\",\"description\":\"").append(esc(bound(e.description(),2048))).append("\",\"limitations\":").append(array(e.limitations())).append("}");}return out.append("\n]\n").toString();}
    private static String tasksJson(List<TaskCore.TaskResult> tasks){StringBuilder out=new StringBuilder("[\n");for(int i=0;i<tasks.size();i++){TaskCore.TaskResult t=tasks.get(i);if(i>0)out.append(',');out.append("{\"id\":\"").append(t.id()).append("\",\"state\":\"").append(t.state()).append("\",\"reason\":\"").append(esc(bound(t.reason(),512))).append("\",\"whyRun\":\"requested workflow dependency\",\"warnings\":").append(array(t.warnings())).append("}");}return out.append("\n]\n").toString();}
    private static String summary(String id,MinecraftInstanceSnapshot s,List<TaskCore.TaskResult> tasks,List<String> limits){InstalledModSnapshot mods=s.mods().value();return "GradleMC Scan v1\nScan ID: "+id+"\nSchema: "+SCHEMA_VERSION+"\nMods: "+(mods==null?"unavailable":mods.mods().size())+"\nTasks: "+tasks.size()+"\nLocal-only and privacy-safe.\nLimitations:\n- "+String.join("\n- ",limits)+"\n";}
    private static boolean writeFile(Path target,String content)throws IOException{byte[] bytes=content.getBytes(StandardCharsets.UTF_8);if(bytes.length>MAX_FILE_BYTES)throw new IOException("Scan v1 file exceeds limit: "+target.getFileName());Path temp=target.resolveSibling(target.getFileName()+".tmp");Files.write(temp,bytes,StandardOpenOption.CREATE_NEW,StandardOpenOption.WRITE);try{Files.move(temp,target,StandardCopyOption.ATOMIC_MOVE);}catch(AtomicMoveNotSupportedException e){Files.move(temp,target);return true;}return false;}
    private static void map(StringBuilder out,Map<String,String> values){out.append('{');int i=0;for(var e:new TreeMap<>(values).entrySet()){if(i++>0)out.append(',');out.append("\"").append(esc(bound(e.getKey(),160))).append("\":\"").append(esc(bound(e.getValue(),1024))).append("\"");}out.append('}');}
    private static String array(List<String> values){StringBuilder b=new StringBuilder("[");int i=0;for(String v:(values==null?List.<String>of():values).stream().limit(MAX_LIST_VALUES).toList()){if(i++>0)b.append(',');b.append('"').append(esc(bound(v,1024))).append('"');}return b.append(']').toString();}
    private static String bound(String v,int max){if(v==null)return "";return v.length()<=max?v:v.substring(0,max)+"…";}
    private static String esc(String value){if(value==null)return "";StringBuilder out=new StringBuilder(value.length()+8);for(int i=0;i<value.length();i++){char c=value.charAt(i);switch(c){case '\\'->out.append("\\\\");case '"'->out.append("\\\"");case '\b'->out.append("\\b");case '\f'->out.append("\\f");case '\n'->out.append("\\n");case '\r'->out.append("\\r");case '\t'->out.append("\\t");default->{if(c<0x20)out.append(String.format(Locale.ROOT,"\\u%04x",(int)c));else out.append(c);}}}return out.toString();}
    private static void deleteStaging(Path staging) { if (!Files.exists(staging, LinkOption.NOFOLLOW_LINKS) || Files.isSymbolicLink(staging)) return; try (var paths=Files.walk(staging)) { for(Path path:paths.sorted(Comparator.reverseOrder()).toList()) Files.deleteIfExists(path); } catch(IOException ignored) { } }
}
