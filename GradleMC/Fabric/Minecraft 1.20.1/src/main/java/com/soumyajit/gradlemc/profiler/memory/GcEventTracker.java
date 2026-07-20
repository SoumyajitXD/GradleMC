package com.soumyajit.gradlemc.profiler.memory;

import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.List;

public final class GcEventTracker {
    public Snapshot snapshot() {
        long count = 0L;
        long time = 0L;
        List<Collector> collectors = new ArrayList<>();
        for (GarbageCollectorMXBean bean : ManagementFactory.getGarbageCollectorMXBeans()) {
            long beanCount = bean.getCollectionCount();
            long beanTime = bean.getCollectionTime();
            collectors.add(new Collector(bean.getName(), beanCount, beanTime, beanCount >= 0L && beanTime >= 0L));
            if (beanCount > 0L) {
                count += beanCount;
            }
            if (beanTime > 0L) {
                time += beanTime;
            }
        }
        return new Snapshot(count, time, List.copyOf(collectors));
    }

    public record Snapshot(long collectionCount, long collectionTimeMillis, List<Collector> collectors) {
        public Delta delta(Snapshot before) {
            if (before == null) {
                return new Delta(0L, 0L);
            }
            return new Delta(
                    Math.max(0L, collectionCount - before.collectionCount),
                    Math.max(0L, collectionTimeMillis - before.collectionTimeMillis)
            );
        }
    }

    public record Collector(String name, long collectionCount, long collectionTimeMillis, boolean supported) { }

    public record Delta(long collectionCount, long collectionTimeMillis) {
    }
}
