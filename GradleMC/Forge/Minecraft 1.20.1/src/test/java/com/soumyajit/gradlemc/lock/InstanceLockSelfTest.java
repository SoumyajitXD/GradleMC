package com.soumyajit.gradlemc.lock;

import java.nio.file.*;
import java.util.*;

public final class InstanceLockSelfTest {
    private InstanceLockSelfTest(){ }
    public static void run()throws Exception{InstanceLockSnapshot a=snapshot("1",List.of(new InstanceLockSnapshot.LockedMod("a","1","a.jar",List.of())),List.of("one","two"));InstanceLockSnapshot b=snapshot("1",List.of(new InstanceLockSnapshot.LockedMod("a","2","a.jar",List.of()),new InstanceLockSnapshot.LockedMod("b","1","b.jar",List.of())),List.of("two","one"));InstanceLockDiff diff=InstanceLockComparator.compare(a,b);if(!diff.added().contains("mod:b")||diff.changed().stream().noneMatch(v->v.startsWith("mod:a"))||diff.reordered().isEmpty())throw new AssertionError("lock diff categories: "+diff);
        Path root=Files.createTempDirectory("gradlemc-lock-test-"),file=root.resolve("gradlemc-instance-lock.json");try{InstanceLockIO.write(file,a);InstanceLockSnapshot read=InstanceLockIO.read(file);if(!read.equals(a))throw new AssertionError("lock serialization round trip");Files.writeString(file,"{broken");try{InstanceLockIO.read(file);throw new AssertionError("malformed lock accepted");}catch(java.io.IOException expected){}}finally{Files.deleteIfExists(file);Files.deleteIfExists(root);}}
    private static InstanceLockSnapshot snapshot(String mc,List<InstanceLockSnapshot.LockedMod> mods,List<String> packs){return new InstanceLockSnapshot(1,mc,"47","17","vendor/vm","1.0.3",mods,packs,List.of(),List.of(),"cfg",Map.of("packs","partial"),List.of());}
}
