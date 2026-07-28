package com.soumyajit.gradlemc.client.overlay;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class FpsRollingStatsCalculatorTest {
    @Test void emptyHistoryWarmsUp() { assertEquals(0,new FpsRollingStatsCalculator(60).snapshot().sampleCount()); }
    @Test void steadySixtyFps() { var c=samples(1_000_000_000L/60,120); assertEquals(60,c.snapshot().averageFps(),.1); }
    @Test void steadyOneFortyFourFps() { var c=samples(1_000_000_000L/144,120); assertEquals(144,c.snapshot().averageFps(),.2); }
    @Test void invalidZeroDeltaIgnored() { var c=samples(10,1); c.recordFrameTimeNanos(0); assertEquals(1,c.snapshot().sampleCount()); }
    @Test void negativeDeltaIgnored() { var c=samples(10,1); c.recordFrameTimeNanos(-1); assertEquals(1,c.snapshot().sampleCount()); }
    @Test void longPauseIgnored() { var c=samples(10,1); c.recordFrameTimeNanos(2_000_000_000L); assertEquals(1,c.snapshot().sampleCount()); }
    @Test void percentilesWarmAtCorrectThresholds() { var c=samples(10_000_000L,99); assertNull(c.snapshot().onePercentLowFps()); c.recordFrameTimeNanos(10_000_000L); assertNotNull(c.snapshot().onePercentLowFps()); }
    @Test void percentileLowReflectsHitch() { var c=samples(10_000_000L,100); c.recordFrameTimeNanos(500_000_000L); assertTrue(c.snapshot().onePercentLowFps() < 10.0); }
    private static FpsRollingStatsCalculator samples(long nanos,int count) { var c=new FpsRollingStatsCalculator(60); for(int i=0;i<count;i++) c.recordFrameTimeNanos(nanos); return c; }
}
