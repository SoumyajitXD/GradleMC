package com.soumyajit.gradlemc.metrics;

/** Consumers resolve to the highest active demand; this never changes Minecraft settings. */
public enum MeasurementDemand {
    SNAPSHOT_ONLY,
    LOW_FREQUENCY,
    NORMAL,
    DETAILED_FOREGROUND;

    public static MeasurementDemand highest(MeasurementDemand left, MeasurementDemand right) {
        return left.ordinal() >= right.ordinal() ? left : right;
    }
}
