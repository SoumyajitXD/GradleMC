package com.soumyajit.gradlemc.startup;
import java.time.Instant;
public record StartupPhase(String id,Instant startedAt,Instant endedAt,long durationMillis,TimingSource source,String limitation){ }
