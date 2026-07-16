package com.soumyajit.gradlemc.experiment;

import java.nio.file.*;
import java.time.Instant;
import java.util.*;

public final class ExperimentSelfTest {
    private ExperimentSelfTest() { }
    public static void run() throws Exception {
        classificationAndComparability(); serializationAndNamesAreSafe();
    }
    private static void classificationAndComparability(){Map<String,String> context=new HashMap<>();for(String key:List.of("minecraft","forge","javaMajor","world","workflow","taskParameters","testDuration"))context.put(key,"same");context.put("mods","a");
        ExperimentSnapshot base=new ExperimentSnapshot(Instant.now(),"one",context,Map.of("medianFps",60D,"p95Mspt",50D));Map<String,String> changed=new HashMap<>(context);changed.put("mods","b");
        ExperimentComparison better=ExperimentComparator.compare(base,new ExperimentSnapshot(Instant.now(),"two",changed,Map.of("medianFps",70D,"p95Mspt",40D)));assertEquals(ExperimentVerdict.IMPROVED,better.verdict(),"improved");
        ExperimentComparison worse=ExperimentComparator.compare(base,new ExperimentSnapshot(Instant.now(),"two",changed,Map.of("medianFps",50D,"p95Mspt",60D)));assertEquals(ExperimentVerdict.REGRESSED,worse.verdict(),"regressed");
        Map<String,String> incompatible=new HashMap<>(changed);incompatible.put("minecraft","different");assertEquals(ExperimentVerdict.NOT_COMPARABLE,ExperimentComparator.compare(base,new ExperimentSnapshot(Instant.now(),"two",incompatible,Map.of("medianFps",90D))).verdict(),"hard context");
        assertEquals(ExperimentVerdict.INCONCLUSIVE,ExperimentComparator.compare(base,new ExperimentSnapshot(Instant.now(),"one",context,base.metrics())).verdict(),"unchanged fingerprint");
    }
    private static void serializationAndNamesAreSafe()throws Exception{Path root=Files.createTempDirectory("gradlemc-experiment-test-");try{ExperimentStore store=new ExperimentStore(root);ExperimentRecord created=store.create("safe_name","quick");ExperimentSnapshot snapshot=new ExperimentSnapshot(Instant.now(),"fp",Map.of("minecraft","1.20.1"),Map.of("medianFps",60D));store.write(new ExperimentRecord(created.name(),created.workflow(),created.createdAt(),false,snapshot,null,created.manualAction(),created.rollback()));assertEquals("fp",store.get("safe_name").orElseThrow().baseline().fingerprint(),"round trip");expect(()->store.create("../escape","quick"));}finally{try(var files=Files.list(root)){for(Path file:files.toList())Files.deleteIfExists(file);}Files.deleteIfExists(root);}}
    private static void expect(Throwing action){try{action.run();throw new AssertionError("Expected rejection");}catch(IllegalArgumentException expected){}catch(Exception other){throw new AssertionError(other);}}
    private static void assertEquals(Object e,Object a,String m){if(!Objects.equals(e,a))throw new AssertionError(m+" expected="+e+" actual="+a);}private interface Throwing{void run()throws Exception;}
}
