package com.soumyajit.gradlemc.incident;

import java.time.Duration;
import java.time.Instant;
import java.util.*;

/** Cheap bounded signal capture. Expensive snapshot enrichment belongs outside trigger callbacks. */
public final class IncidentRecorder {
    public static final int MAX_INCIDENTS = 32;
    public static final int MAX_SIGNALS = 256;
    public static final int MAX_WINDOW_SIGNALS = 64;
    private static final Duration PRE_WINDOW = Duration.ofSeconds(10);
    private static final IncidentRecorder INSTANCE = new IncidentRecorder();
    private final ArrayDeque<IncidentSignal> signals = new ArrayDeque<>();
    private final ArrayDeque<Incident> incidents = new ArrayDeque<>();
    private boolean recording;
    private long sequence;
    private IncidentRecorder() { }
    public static IncidentRecorder instance() { return INSTANCE; }

    public synchronized void start() { recording = true; }
    public synchronized void stop() { recording = false; }
    public synchronized boolean recording() { return recording; }

    public synchronized void signal(IncidentSignal signal) {
        if (!recording) return;
        signals.addLast(signal);
        while (signals.size() > MAX_SIGNALS) signals.removeFirst();
    }

    public synchronized Optional<Incident> trigger(String trigger, Map<String, String> cheapContext, List<String> evidenceIds) {
        if (!recording) return Optional.empty();
        Instant now = Instant.now();
        List<IncidentSignal> before = signals.stream().filter(signal -> !signal.timestamp().isBefore(now.minus(PRE_WINDOW)))
                .skip(Math.max(0, signals.size() - MAX_WINDOW_SIGNALS)).toList();
        boolean truncated = before.size() >= MAX_WINDOW_SIGNALS || signals.size() >= MAX_SIGNALS;
        String id = "incident-" + now.toEpochMilli() + "-" + (++sequence);
        Incident incident = new Incident(id, bounded(trigger, 96), now, before, List.of(), bounded(cheapContext),
                evidenceIds.stream().limit(32).map(value -> bounded(value, 128)).toList(), truncated);
        incidents.addFirst(incident);
        while (incidents.size() > MAX_INCIDENTS) incidents.removeLast();
        return Optional.of(incident);
    }

    public synchronized List<Incident> incidents() { return List.copyOf(incidents); }
    public synchronized Optional<Incident> latest() { return Optional.ofNullable(incidents.peekFirst()); }
    public synchronized Optional<Incident> find(String id) { return incidents.stream().filter(value -> value.id().equals(id)).findFirst(); }
    public synchronized void resetForTests() { recording=false; signals.clear(); incidents.clear(); sequence=0; }
    private static Map<String,String> bounded(Map<String,String> input) { Map<String,String> out=new TreeMap<>(); input.entrySet().stream().limit(64).forEach(e->out.put(bounded(e.getKey(),64),bounded(e.getValue(),256))); return Map.copyOf(out); }
    private static String bounded(String value,int max){if(value==null)return "";return value.length()<=max?value:value.substring(0,max);}
}
