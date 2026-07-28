package com.soumyajit.gradlemc.metrics;

/** Idempotent demand handle. It carries no world, player, screen, or Minecraft reference. */
public interface MeasurementSubscription extends AutoCloseable {
    @Override
    void close();
}
