package com.soumyajit.gradlemc.profiler;

import com.soumyajit.gradlemc.capability.*;
import org.junit.jupiter.api.Test;
import java.util.Set;
import static org.junit.jupiter.api.Assertions.*;

class ProfilerAvailabilityTest {
    private CapabilitySnapshot ready(boolean admin) { return CapabilityResolver.resolve(new CapabilityInput(true,true,true,true,CapabilityInput.ConnectionState.CHANNEL_AVAILABLE,true,false,false,admin,Set.of(ServerOperation.STATUS,ServerOperation.SERVER_PROFILER,ServerOperation.TICK_PROFILER))); }
    private ProfilerSessionConfig config(ProfilerMode mode,String thread,int interval) { return new ProfilerSessionConfig(60,interval,thread,50,false,mode); }
    @Test void tickWorksForReadyIntegratedServer() { assertTrue(ProfilerAvailability.decide(ready(true),config(ProfilerMode.TICK,"server",20)).runnable()); }
    @Test void tickNeedsLogicalServer() { assertFalse(ProfilerAvailability.decide(CapabilityResolver.resolve(null),config(ProfilerMode.TICK,"server",20)).runnable()); }
    @Test void renderCpuIsLocal() { assertTrue(ProfilerAvailability.decide(ready(false),config(ProfilerMode.CPU_LITE,"render",20)).runnable()); }
    @Test void serverCpuNeedsPermission() { assertEquals(ProfilerAvailability.Reason.SERVER_PERMISSION_REQUIRED,ProfilerAvailability.decide(ready(false),config(ProfilerMode.CPU_LITE,"server",20)).reason()); }
    @Test void remoteServerPlanIsAvailableOnlyThroughFreshServerEvidence() { var remote=CapabilityResolver.resolve(new CapabilityInput(true,true,false,false,CapabilityInput.ConnectionState.CHANNEL_AVAILABLE,true,false,false,true,Set.of(ServerOperation.STATUS,ServerOperation.SERVER_PROFILER))); assertTrue(ProfilerAvailability.decide(remote,config(ProfilerMode.CPU_LITE,"server",20)).runnable()); }
    @Test void memoryLiteDescribesClientProcess() { var plan=ProfilerAvailability.decide(ready(false),config(ProfilerMode.MEMORY_LITE,"render",20)); assertTrue(plan.collectors().contains("memory-lite-client-process")); }
    @Test void combinedIsFullWhenEveryCollectorExists() { assertTrue(ProfilerAvailability.decide(ready(true),config(ProfilerMode.COMBINED,"server",20)).full()); }
    @Test void combinedDoesNotDegrade() { var unavailable=CapabilityResolver.resolve(new CapabilityInput(true,true,true,true,CapabilityInput.ConnectionState.CHANNEL_AVAILABLE,true,false,false,true,Set.of(ServerOperation.STATUS,ServerOperation.TICK_PROFILER))); assertFalse(ProfilerAvailability.decide(unavailable,config(ProfilerMode.COMBINED,"server",20)).runnable()); }
    @Test void allThreadsHasIntervalFloor() { assertEquals(ProfilerAvailability.Reason.ALL_THREADS_INTERVAL_TOO_LOW,ProfilerAvailability.decide(ready(true),config(ProfilerMode.CPU_LITE,"all",4)).reason()); }
    @Test void allThreadsWarns() { assertFalse(ProfilerAvailability.decide(ready(true),config(ProfilerMode.CPU_LITE,"all",20)).warning().isBlank()); }
    @Test void validCustomFilterRuns() { assertTrue(ProfilerAvailability.decide(ready(true),config(ProfilerMode.CPU_LITE,"worker",20)).runnable()); }
    @Test void blankCustomFilterIsRejected() { assertEquals(ProfilerAvailability.Reason.EMPTY_CUSTOM_FILTER,ProfilerAvailability.decide(ready(true),config(ProfilerMode.CPU_LITE,"",20)).reason()); }
    @Test void oversizedCustomFilterIsRejected() { assertEquals(ProfilerAvailability.Reason.CUSTOM_FILTER_TOO_LONG,ProfilerAvailability.decide(ready(true),config(ProfilerMode.CPU_LITE,"x".repeat(65),20)).reason()); }
    @Test void broadCustomFilterIsRejected() { assertEquals(ProfilerAvailability.Reason.CUSTOM_FILTER_TOO_BROAD,ProfilerAvailability.decide(ready(true),config(ProfilerMode.CPU_LITE,"xx",20)).reason()); }
    @Test void staleCapabilityDisablesTick() { var stale=CapabilityResolver.resolve(new CapabilityInput(true,true,false,false,CapabilityInput.ConnectionState.CHANNEL_AVAILABLE,true,true,false,true,Set.of(ServerOperation.STATUS,ServerOperation.TICK_PROFILER))); assertEquals(ProfilerAvailability.Reason.STALE_SERVER_CAPABILITY,ProfilerAvailability.decide(stale,config(ProfilerMode.TICK,"server",20)).reason()); }
}
