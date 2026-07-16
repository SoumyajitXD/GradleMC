package com.soumyajit.gradlemc.profiler.sampling;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class StackTraceAggregator {
    private static final int MAX_KEYS = 4096;
    private final Map<String, Long> threadSamples = new HashMap<>();
    private final Map<String, Long> frameSamples = new HashMap<>();
    private final Map<String, Long> leafSamples = new HashMap<>();
    private final Map<String, Long> packageSamples = new HashMap<>();
    private final Map<String, Long> threadStateSamples = new HashMap<>();
    private final Map<String, Long> topDownSamples = new HashMap<>();
    private final Map<String, Long> bottomUpSamples = new HashMap<>();
    private long sampleCount;
    private long truncatedKeys;

    public synchronized void add(ThreadSnapshot snapshot) {
        if (snapshot == null || snapshot.frames().isEmpty()) {
            return;
        }
        sampleCount++;
        increment(threadSamples, snapshot.threadName(), 1L);
        increment(threadStateSamples,snapshot.threadName()+" ["+snapshot.state()+"]",1L);
        StackTraceElement leaf = snapshot.frames().get(0);
        increment(leafSamples, frameKey(leaf), 1L);
        for (StackTraceElement frame : snapshot.frames()) {
            String key = frameKey(frame);
            increment(frameSamples, key, 1L);
            increment(packageSamples, packageKey(frame.getClassName()), 1L);
        }
        increment(topDownSamples,path(snapshot.frames(),true),1L);increment(bottomUpSamples,path(snapshot.frames(),false),1L);
    }

    public synchronized void addAll(List<ThreadSnapshot> snapshots) {
        for (ThreadSnapshot snapshot : snapshots) {
            add(snapshot);
        }
    }

    public synchronized long sampleCount() {
        return sampleCount;
    }

    public synchronized List<FrameCount> topThreads(int limit) {
        return top(threadSamples, limit);
    }

    public synchronized List<FrameCount> topFrames(int limit) {
        return top(frameSamples, limit);
    }

    public synchronized List<FrameCount> topLeaves(int limit) {
        return top(leafSamples, limit);
    }

    public synchronized List<FrameCount> topPackages(int limit) {
        return top(packageSamples, limit);
    }
    public synchronized List<FrameCount> topThreadStates(int limit){return top(threadStateSamples,limit);}public synchronized List<FrameCount> topDown(int limit){return top(topDownSamples,limit);}public synchronized List<FrameCount> bottomUp(int limit){return top(bottomUpSamples,limit);}public synchronized long truncatedKeys(){return truncatedKeys;}

    public synchronized Map<String, Long> packageCounts() {
        return new LinkedHashMap<>(packageSamples);
    }

    private static List<FrameCount> top(Map<String, Long> source, int limit) {
        return source.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue(Comparator.reverseOrder()).thenComparing(Map.Entry.comparingByKey()))
                .limit(Math.max(0, limit))
                .map(entry -> new FrameCount(entry.getKey(), entry.getValue()))
                .toList();
    }

    private void increment(Map<String, Long> map, String key, long amount) {
        if (key == null || key.isBlank()) {
            return;
        }
        if (!map.containsKey(key) && map.size() >= MAX_KEYS) {
            map.merge("<other>", amount, Long::sum);
            truncatedKeys+=amount;
            return;
        }
        map.merge(key, amount, Long::sum);
    }
    private static String path(List<StackTraceElement> frames,boolean topDown){StringBuilder out=new StringBuilder();int limit=Math.min(64,frames.size());for(int index=0;index<limit;index++){int i=topDown?limit-1-index:index;if(index>0)out.append(" -> ");out.append(frameKey(frames.get(i)));}return out.toString();}

    private static String frameKey(StackTraceElement frame) {
        return frame.getClassName() + "#" + frame.getMethodName();
    }

    private static String packageKey(String className) {
        if (className == null || className.isBlank()) {
            return "unknown";
        }
        String[] parts = className.split("\\.");
        if (parts.length <= 2) {
            return className;
        }
        return parts[0] + "." + parts[1] + "." + parts[2];
    }

    public record ThreadSnapshot(String threadName, Thread.State state, List<StackTraceElement> frames) {
        public ThreadSnapshot {
            frames = frames == null ? List.of() : List.copyOf(frames);
        }
    }

    public record FrameCount(String name, long count) {
    }
}
