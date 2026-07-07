package com.soumyajit.gradlemc.profiler.tick;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class TickTimelineRecorder {
    private final TickRecord[] ring;
    private final SlowTickDetector slowTickDetector;
    private int nextIndex;
    private int size;
    private long totalRecorded;
    private int slowTickCount;

    public TickTimelineRecorder(int capacity, SlowTickDetector slowTickDetector) {
        this.ring = new TickRecord[Math.max(1, capacity)];
        this.slowTickDetector = slowTickDetector;
    }

    public void record(TickRecord record) {
        if (record == null) {
            return;
        }
        TickRecord replaced = ring[nextIndex];
        if (replaced != null && slowTickDetector.isSlow(replaced.durationMillis())) {
            slowTickCount--;
        }
        ring[nextIndex] = record;
        nextIndex = (nextIndex + 1) % ring.length;
        if (size < ring.length) {
            size++;
        }
        totalRecorded++;
        if (slowTickDetector.isSlow(record.durationMillis())) {
            slowTickCount++;
        }
    }

    public List<TickRecord> records() {
        List<TickRecord> records = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            int index = (nextIndex - size + i + ring.length) % ring.length;
            TickRecord record = ring[index];
            if (record != null) {
                records.add(record);
            }
        }
        return records;
    }

    public TickSummary summary() {
        List<Double> values = records().stream()
                .map(TickRecord::durationMillis)
                .sorted()
                .toList();
        if (values.isEmpty()) {
            return TickSummary.empty();
        }
        double total = values.stream().mapToDouble(Double::doubleValue).sum();
        return new TickSummary(
                values.size(),
                total / values.size(),
                percentile(values, 0.50D),
                percentile(values, 0.95D),
                percentile(values, 0.99D),
                values.get(0),
                values.get(values.size() - 1),
                slowTickCount
        );
    }

    public List<TickRecord> slowest(int limit) {
        return records().stream()
                .sorted(Comparator.comparingDouble(TickRecord::durationMillis).reversed())
                .limit(Math.max(0, limit))
                .toList();
    }

    public long totalRecorded() {
        return totalRecorded;
    }

    private static double percentile(List<Double> sorted, double percentile) {
        if (sorted.isEmpty()) {
            return 0.0D;
        }
        int index = (int) Math.ceil(percentile * sorted.size()) - 1;
        index = Math.max(0, Math.min(sorted.size() - 1, index));
        return sorted.get(index);
    }
}
