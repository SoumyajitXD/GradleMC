package com.soumyajit.gradlemc.foundation;
import java.time.*; import java.util.*;
public final class FoundationSelfTest {
 private FoundationSelfTest() { }
 public static void run() {
  check(TaskId.of("instance:snapshot").equals(TaskId.of("instance:snapshot"))); bad(() -> TaskId.of("Instance:snapshot")); bad(() -> TaskId.of("missing")); bad(() -> TaskId.of("bad:space here"));
  StaticFingerprint a=StaticFingerprint.of(1,Map.of("b","2","a","1")); check(a.equals(StaticFingerprint.of(1,Map.of("a","1","b","2")))); check(!a.equals(StaticFingerprint.of(2,Map.of("a","1","b","2"))));
  Clock clock=Clock.fixed(Instant.parse("2026-01-01T00:00:00Z"),ZoneOffset.UTC); check(Freshness.evaluate(Instant.parse("2025-12-31T20:00:00Z"),Optional.of(Instant.parse("2025-12-31T21:00:00Z")),true,true,clock,"test").state()==Freshness.State.STALE);
  TaskRegistry r=new TaskRegistry(); r.register(task("d:leaf",List.of())); r.register(task("b:left",List.of(TaskId.of("d:leaf")))); r.register(task("c:right",List.of(TaskId.of("d:leaf")))); r.register(task("a:root",List.of(TaskId.of("b:left"),TaskId.of("c:right")))); check(r.plan(List.of(TaskId.of("a:root")),context(a),false).nodes().stream().map(n->n.id().toString()).toList().equals(List.of("d:leaf","b:left","c:right","a:root"))); bad(()->r.register(task("a:root",List.of())));
  TaskHistory h=new TaskHistory(2); h.add(result("a:one",a));h.add(result("a:two",a));h.add(result("a:three",a));check(h.recent().size()==2&&h.recent().get(1).id().equals(TaskId.of("a:two")));
 }
 private static TaskCore.Definition task(String id,List<TaskId> deps){return new TaskCore.Definition(TaskId.of(id),id,"test",deps,TaskCore.Affinity.CALLER_THREAD,Duration.ofSeconds(1),TaskCore.CachePolicy.PROCESS_LIFETIME,c->TaskCore.Availability.present(),c->new TaskCore.Output(List.of(),"ok"));}
 private static TaskCore.Context context(StaticFingerprint s){return new TaskCore.Context(Clock.systemUTC(),new TaskCore.CancellationToken(),s,RuntimeContextFingerprint.of(1,s,Map.of("world","none")),Map.of());}
 private static TaskCore.TaskResult result(String id,StaticFingerprint s){TaskCore.Context c=context(s);return new TaskCore.TaskResult(TaskId.of(id),TaskCore.State.SUCCEEDED,Instant.EPOCH,Instant.EPOCH,Instant.EPOCH,"",List.of(),List.of(),List.of(),new Freshness(Freshness.State.FRESH,Instant.EPOCH,Optional.empty(),"test","",List.of()),Optional.of(s),Optional.of(c.runtimeFingerprint()),Optional.empty());}
 private static void check(boolean value){if(!value)throw new AssertionError("Foundation self-test failed");} private static void bad(Runnable action){try{action.run();throw new AssertionError("Expected failure");}catch(IllegalArgumentException expected){}}
}
