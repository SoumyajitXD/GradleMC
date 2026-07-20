package com.soumyajit.gradlemc.network;

import com.soumyajit.gradlemc.util.GradleMcLimits;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;

/** Bounded request correlation independent of Fabric networking callbacks. */
public final class ClientRequestTracker {
    public static final int MAX_PENDING = GradleMcLimits.MAX_PENDING_REQUESTS;
    private static final long DEFAULT_TIMEOUT_MILLIS = 5_000L;

    public record Request(int id, String type, long generation, long createdAtMillis, long timeoutAtMillis) {
        public Request {
            type = type == null ? "unknown" : type;
        }
    }

    private final Map<Integer, Request> pending = new HashMap<>();
    private int nextId = 1;
    private long generation;

    public synchronized long beginConnection() {
        pending.clear();
        generation = generation == Long.MAX_VALUE ? 1L : generation + 1L;
        return generation;
    }

    public synchronized long generation() {
        return generation;
    }

    public synchronized Optional<Request> begin(String type, long nowMillis) {
        expire(nowMillis);
        if (pending.size() >= MAX_PENDING) return Optional.empty();
        int id = nextRequestId();
        Request request = new Request(id, type, generation, nowMillis, nowMillis + DEFAULT_TIMEOUT_MILLIS);
        pending.put(id, request);
        return Optional.of(request);
    }

    public synchronized Optional<Request> complete(int id, long responseGeneration, long nowMillis) {
        expire(nowMillis);
        Request request = pending.get(id);
        if (request == null || request.generation() != responseGeneration) return Optional.empty();
        pending.remove(id);
        return Optional.of(request);
    }

    public synchronized Optional<Request> cancel(int id) {
        return Optional.ofNullable(pending.remove(id));
    }

    public synchronized int expire(long nowMillis) {
        int expired = 0;
        Iterator<Request> iterator = pending.values().iterator();
        while (iterator.hasNext()) {
            if (iterator.next().timeoutAtMillis() <= nowMillis) {
                iterator.remove();
                expired++;
            }
        }
        return expired;
    }

    public synchronized void clear() {
        pending.clear();
    }

    public synchronized int pendingCount() {
        return pending.size();
    }

    private int nextRequestId() {
        for (int attempts = 0; attempts < Integer.MAX_VALUE; attempts++) {
            int candidate = nextId++;
            if (nextId <= 0) nextId = 1;
            if (candidate > 0 && !pending.containsKey(candidate)) return candidate;
        }
        throw new IllegalStateException("GradleMC request ID space exhausted");
    }
}
