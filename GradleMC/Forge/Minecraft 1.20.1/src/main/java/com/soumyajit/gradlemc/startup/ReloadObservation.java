package com.soumyajit.gradlemc.startup;
import java.time.Instant;
public record ReloadObservation(Instant requestedAt,Instant startedAt,Instant endedAt,long durationMillis,TimingSource source,long heapBeforeMiB,long heapAfterMiB,long gcBefore,long gcAfter,String state,String limitation){ }
