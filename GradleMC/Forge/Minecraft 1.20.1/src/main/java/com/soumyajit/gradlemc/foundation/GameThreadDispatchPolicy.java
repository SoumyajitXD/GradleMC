package com.soumyajit.gradlemc.foundation;

import java.time.Duration;

/** Conservative bounds; they limit work started by the bridge, not Minecraft's tick time. */
public record GameThreadDispatchPolicy(int maxQueuedRequests, int maxRequestsPerTick,
                                       Duration captureBudgetPerTick, Duration maxRequestAge) {
    public static final GameThreadDispatchPolicy SAFE_DEFAULT = new GameThreadDispatchPolicy(32, 2,
            Duration.ofMillis(2), Duration.ofSeconds(10));
    public GameThreadDispatchPolicy {
        if (maxQueuedRequests < 1 || maxQueuedRequests > 256 || maxRequestsPerTick < 1 || maxRequestsPerTick > 32
                || captureBudgetPerTick == null || captureBudgetPerTick.isNegative() || captureBudgetPerTick.isZero()
                || maxRequestAge == null || maxRequestAge.isNegative() || maxRequestAge.isZero()) {
            throw new IllegalArgumentException("Invalid game-thread dispatch policy");
        }
    }
}
