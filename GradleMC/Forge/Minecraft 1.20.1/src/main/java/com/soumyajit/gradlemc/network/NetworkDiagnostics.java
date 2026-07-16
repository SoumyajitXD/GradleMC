package com.soumyajit.gradlemc.network;

import java.time.*;
import java.util.*;

/** GradleMC-channel metadata only. Payloads, addresses, chat, commands, and player identity are never retained. */
public final class NetworkDiagnostics {
    private static final Map<Key,Counter> counters=new HashMap<>();private static Sample active,latest;private static long ignoredResponses;
    private NetworkDiagnostics(){ }
    public static synchronized void record(String packet,String direction,long approximateBytes){Key key=new Key(safe(packet),safe(direction));Counter old=counters.getOrDefault(key,new Counter(0,0,0));long bytes=approximateBytes<0?old.bytes:saturating(old.bytes,approximateBytes);counters.put(key,new Counter(old.count+1,bytes,Math.max(old.peakBytes,Math.max(0,approximateBytes))));finishIfDue();}
    public static synchronized void ignoredResponse(){ignoredResponses++;}
    public static synchronized boolean start(int seconds){finishIfDue();if(active!=null)return false;Instant now=Instant.now();active=new Sample(now,now.plusSeconds(seconds),copy(),Map.of(),ignoredResponses,false);return true;}
    public static synchronized Snapshot snapshot(){finishIfDue();Map<String,PacketSummary> summaries=new TreeMap<>();for(var e:counters.entrySet())summaries.put(e.getKey().direction+":"+e.getKey().packet,new PacketSummary(e.getValue().count,e.getValue().bytes,e.getValue().peakBytes));return new Snapshot(Instant.now(),Map.copyOf(summaries),ignoredResponses,"Only GradleMC channel metadata is available; third-party payloads and addresses are not intercepted.");}
    public static synchronized Optional<SampleResult> latest(){finishIfDue();if(latest==null||!latest.complete)return Optional.empty();return Optional.of(result(latest));}
    public static synchronized boolean sampling(){finishIfDue();return active!=null;}
    private static void finishIfDue(){if(active!=null&&!Instant.now().isBefore(active.end)){latest=new Sample(active.start,active.end,active.before,copy(),ignoredResponses,true);active=null;}}
    private static SampleResult result(Sample sample){Map<String,PacketSummary> delta=new TreeMap<>();Set<Key> keys=new HashSet<>(sample.after.keySet());keys.addAll(sample.before.keySet());double seconds=Math.max(0.001,Duration.between(sample.start,sample.end).toMillis()/1000D);for(Key key:keys){Counter a=sample.after.getOrDefault(key,new Counter(0,0,0)),b=sample.before.getOrDefault(key,new Counter(0,0,0));long count=Math.max(0,a.count-b.count),bytes=Math.max(0,a.bytes-b.bytes);delta.put(key.direction+":"+key.packet,new PacketSummary(count,bytes,a.peakBytes));}return new SampleResult(sample.start,sample.end,Map.copyOf(delta),seconds,sample.ignoredResponses);}
    private static Map<Key,Counter> copy(){return Map.copyOf(counters);}private static String safe(String v){if(v==null)return "unknown";return v.replaceAll("[^a-zA-Z0-9_.:-]","_").substring(0,Math.min(64,v.length()));}private static long saturating(long a,long b){return a>Long.MAX_VALUE-b?Long.MAX_VALUE:a+b;}
    private record Key(String packet,String direction){}private record Counter(long count,long bytes,long peakBytes){}private record Sample(Instant start,Instant end,Map<Key,Counter> before,Map<Key,Counter> after,long ignoredResponses,boolean complete){}
    public record PacketSummary(long count,long approximateBytes,long peakApproximateBytes){}public record Snapshot(Instant at,Map<String,PacketSummary> packets,long ignoredResponses,String limitation){}public record SampleResult(Instant startedAt,Instant endedAt,Map<String,PacketSummary> packets,double seconds,long ignoredResponses){public double packetsPerSecond(){return packets.values().stream().mapToLong(PacketSummary::count).sum()/seconds;}}
}
