package com.soumyajit.gradlemc.scan;

import com.soumyajit.gradlemc.instance.*;
import com.soumyajit.gradlemc.modaudit.*;
import com.soumyajit.gradlemc.adaptive.AdaptiveDiagnostics;
import com.soumyajit.gradlemc.foundation.*;
import java.nio.file.*;
import java.time.Instant;
import java.util.*;

/** Filesystem-free collector tests are kept separate; this verifies bounded Scan v1 serialization. */
public final class GradleMcScanV1SelfTest {
    private GradleMcScanV1SelfTest() { }
    public static void run() throws Exception {
        Path game = Files.createTempDirectory("gradlemc-scan-v1-");
        try {
            check(ScanPathRedactor.redact(game.resolve("config").resolve("a.toml"), game).equals("<gameDir>\\config\\a.toml"));
            check(ScanPathRedactor.redact(Path.of("C:\\Users\\Someone\\outside.txt"), game).equals("<redacted>\\outside.txt"));
            GradleMcScanV1Writer.Result written = new GradleMcScanV1Writer().write(game, snapshot(), List.of());
            check(Files.isRegularFile(written.directory().resolve("manifest.json")));
            check(Files.isRegularFile(written.directory().resolve("snapshot.json")));
            check(!Files.exists(written.directory().resolve("recommendations.json")));
            String manifest = Files.readString(written.directory().resolve("manifest.json"));
            check(manifest.contains("gradlemc-scan-v1") && manifest.contains("scan-path-v1") && manifest.contains("\"loaderVersion\":\"47.4.20\""));
            String snapshot = Files.readString(written.directory().resolve("snapshot.json"));
            check(snapshot.contains("control\\tvalue\\u0001"));
            check(snapshot.contains("\"datapacks\":{\"availability\":\"UNAVAILABLE\""));
            check(manifest.contains("\"completionState\":\"PARTIAL\""));
            GradleMcScanV1Writer.Result partial = new GradleMcScanV1Writer().write(game, snapshot(), List.of(taskResult(TaskCore.State.UNAVAILABLE)));
            check(Files.readString(partial.directory().resolve("manifest.json")).contains("\"completionState\":\"PARTIAL\""));
            try (var paths = Files.list(written.directory().getParent())) {
                check(paths.noneMatch(path -> path.getFileName().toString().contains(".incomplete")));
            }
        } finally { try (var paths = Files.walk(game)) { for (Path p : paths.sorted(Comparator.reverseOrder()).toList()) Files.deleteIfExists(p); } }
    }
    private static MinecraftInstanceSnapshot snapshot() {
        Instant now=Instant.EPOCH;
        InstanceComponent<Map<String,String>> env=c(Map.of("gradlemc","1.0.3","minecraft","1.20.1","loader","Forge","loaderVersion","47.4.20","control","control\tvalue\u0001"),now);
        InstanceComponent<Map<String,String>> runtime=c(Map.of("java","17","vendor","test"),now);
        ModDescriptor mod=new ModDescriptor("zmod","zmod","Z Mod","1.0","z.jar",1,List.of(),false,false,List.of());
        InstanceComponent<InstalledModSnapshot> mods=c(new InstalledModSnapshot(now,List.of(mod),true,""),now);
        InstanceComponent<List<PackDescriptor>> packs=c(List.of(new PackDescriptor("a","a.zip",PackDescriptor.PackKind.RESOURCE,true,1,0,15,"metadata",List.of(),List.of())),now);
        return new MinecraftInstanceSnapshot(now,env,runtime,mods,packs,c(List.of(),now),InstanceComponent.unavailable("test",ComponentScope.WORLD,"No active world"),c(List.of(new ConfigDescriptor("a.toml","toml",2,0,"metadata-only","","")),now),InstanceComponent.unavailable("test",ComponentScope.WORLD,"No active world"),c(Map.of(),now),"fingerprint");
    }
    private static <T> InstanceComponent<T> c(T value,Instant now){return new InstanceComponent<>(ComponentAvailability.AVAILABLE,"test",now,"common",ComponentScope.STATIC,true,"",value);}
    private static TaskCore.TaskResult taskResult(TaskCore.State state) {
        StaticFingerprint stat=StaticFingerprint.of(1,Map.of("test","scan"));
        RuntimeContextFingerprint runtime=RuntimeContextFingerprint.of(1,stat,Map.of("world","none"));
        return new TaskCore.TaskResult(TaskId.of("test:optional"),state,Instant.EPOCH,Instant.EPOCH,Instant.EPOCH,"optional unavailable",List.of(),List.of(),List.of(),new Freshness(Freshness.State.UNAVAILABLE,Instant.EPOCH,Optional.empty(),"test","",List.of()),Optional.of(stat),Optional.of(runtime),Optional.empty());
    }
    private static void check(boolean v){if(!v)throw new AssertionError("Scan v1 self-test failed");}
}
