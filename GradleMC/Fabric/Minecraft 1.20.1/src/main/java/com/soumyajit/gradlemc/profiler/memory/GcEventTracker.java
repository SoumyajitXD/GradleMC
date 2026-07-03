package com.soumyajit.gradlemc.profiler.memory;

import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;

public final class GcEventTracker {
    public Snapshot snapshot() {
        long count = 0L;
        long time = 0L;
        for (GarbageCollectorMXBean bean : ManagementFactory.getGarbageCollectorMXBeans()) {
            long beanCount = bean.getCollectionCount();
            long beanTime = bean.getCollectionTime();
            if (beanCount > 0L) {
                count += beanCount;
            }
            if (beanTime > 0L) {
                time += beanTime;
            }
        }
        return new Snapshot(count, time);
    }

    public record Snapshot(long collectionCount, long collectionTimeMillis) {
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

    public record Delta(long collectionCount, long collectionTimeMillis) {
    }
}
