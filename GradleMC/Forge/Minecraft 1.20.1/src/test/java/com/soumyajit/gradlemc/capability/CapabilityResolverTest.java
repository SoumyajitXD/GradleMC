package com.soumyajit.gradlemc.capability;

import org.junit.jupiter.api.Test;
import java.util.Set;
import static org.junit.jupiter.api.Assertions.*;

class CapabilityResolverTest {
    private CapabilitySnapshot resolve(boolean world, boolean integrated, boolean ready, CapabilityInput.ConnectionState connection,
                                       boolean accepted, boolean stale, boolean admin) {
        return CapabilityResolver.resolve(new CapabilityInput(true, world, integrated, ready, connection, accepted, stale, false, admin,
                Set.of(ServerOperation.STATUS, ServerOperation.SERVER_PROFILER, ServerOperation.TICK_PROFILER, ServerOperation.ADMIN_ACTIONS)));
    }
    @Test void noWorldKeepsLocalFps() { var value = resolve(false,false,false,CapabilityInput.ConnectionState.DISCONNECTED,false,false,false); assertEquals(RuntimeLocation.NO_ACTIVE_WORLD,value.location()); assertTrue(value.available(RuntimeCapability.FPS_SAMPLING)); }
    @Test void noWorldIsDisconnected() { assertEquals(NetworkCompatibility.DISCONNECTED, resolve(false,false,false,CapabilityInput.ConnectionState.DISCONNECTED,false,false,false).network()); }
    @Test void localClientWithoutServerIsDistinct() { assertEquals(RuntimeLocation.LOCAL_CLIENT_NO_LOGICAL_SERVER, resolve(true,false,false,CapabilityInput.ConnectionState.DISCONNECTED,false,false,false).location()); }
    @Test void integratedStartingIsDistinct() { assertEquals(RuntimeLocation.INTEGRATED_SERVER_STARTING, resolve(true,true,false,CapabilityInput.ConnectionState.CONNECTING,false,false,false).location()); }
    @Test void integratedReadyIsDistinct() { assertEquals(RuntimeLocation.INTEGRATED_SERVER_READY, resolve(true,true,true,CapabilityInput.ConnectionState.CHANNEL_AVAILABLE,true,false,false).location()); }
    @Test void remoteIsDistinct() { assertEquals(RuntimeLocation.REMOTE_MULTIPLAYER, resolve(true,false,false,CapabilityInput.ConnectionState.CHANNEL_AVAILABLE,false,false,false).location()); }
    @Test void remoteWithoutModIsDistinct() { assertEquals(NetworkCompatibility.REMOTE_SERVER_WITHOUT_GRADLEMC, resolve(true,false,false,CapabilityInput.ConnectionState.REMOTE_WITHOUT_GRADLEMC,false,false,false).network()); }
    @Test void incompatibleIsDistinct() { assertEquals(NetworkCompatibility.INCOMPATIBLE_PROTOCOL, resolve(true,false,false,CapabilityInput.ConnectionState.INCOMPATIBLE,false,false,false).network()); }
    @Test void pendingSnapshotIsDistinct() { assertEquals(NetworkCompatibility.CAPABILITY_SNAPSHOT_PENDING, resolve(true,false,false,CapabilityInput.ConnectionState.CHANNEL_AVAILABLE,false,false,false).network()); }
    @Test void freshSnapshotIsDistinct() { assertEquals(NetworkCompatibility.CAPABILITY_SNAPSHOT_FRESH, resolve(true,false,false,CapabilityInput.ConnectionState.CHANNEL_AVAILABLE,true,false,false).network()); }
    @Test void staleSnapshotIsDistinct() { assertEquals(NetworkCompatibility.CAPABILITY_SNAPSHOT_STALE, resolve(true,false,false,CapabilityInput.ConnectionState.CHANNEL_AVAILABLE,true,true,false).network()); }
    @Test void readonlyPermissionIsNotAdministrative() { var value=resolve(true,false,false,CapabilityInput.ConnectionState.CHANNEL_AVAILABLE,true,false,false); assertEquals(PermissionState.READ_ONLY,value.permission()); assertFalse(value.serverActionAllowed(RuntimeCapability.SERVER_THREAD_PROFILING)); }
    @Test void operatorPermissionEnablesAdvertisedAction() { var value=resolve(true,false,false,CapabilityInput.ConnectionState.CHANNEL_AVAILABLE,true,false,true); assertTrue(value.serverActionAllowed(RuntimeCapability.SERVER_THREAD_PROFILING)); }
    @Test void unknownPermissionDoesNotEnableAction() { assertEquals(PermissionState.UNKNOWN, resolve(true,false,false,CapabilityInput.ConnectionState.CONNECTING,false,false,true).permission()); }
    @Test void disconnectDoesNotRemoveLocalMemory() { var value=resolve(false,false,false,CapabilityInput.ConnectionState.DISCONNECTED,false,false,false); assertTrue(value.available(RuntimeCapability.LOCAL_JVM_MEMORY)); }
    @Test void disconnectRemovesServerAction() { assertFalse(resolve(false,false,false,CapabilityInput.ConnectionState.DISCONNECTED,false,false,true).serverActionAllowed(RuntimeCapability.ADMINISTRATIVE_ACTIONS)); }
    @Test void remoteOutputIsNeverLocal() { assertEquals(OutputLocality.REMOTE_SERVER_ONLY, resolve(true,false,false,CapabilityInput.ConnectionState.CHANNEL_AVAILABLE,true,false,true).outputLocality()); }
    @Test void integratedOutputIsProcessLocal() { assertEquals(OutputLocality.INTEGRATED_PROCESS_OUTPUT, resolve(true,true,true,CapabilityInput.ConnectionState.CHANNEL_AVAILABLE,true,false,true).outputLocality()); }
    @Test void snapshotCollectionsAreImmutable() { var value=resolve(true,false,false,CapabilityInput.ConnectionState.CHANNEL_AVAILABLE,true,false,true); assertThrows(UnsupportedOperationException.class, () -> value.available().add(RuntimeCapability.FPS_SAMPLING)); }
    @Test void replacementDoesNotRetainServerActions() { var first=resolve(true,false,false,CapabilityInput.ConnectionState.CHANNEL_AVAILABLE,true,false,true); var second=resolve(false,false,false,CapabilityInput.ConnectionState.DISCONNECTED,false,false,false); assertTrue(first.serverActionAllowed(RuntimeCapability.SERVER_THREAD_PROFILING)); assertFalse(second.serverActionAllowed(RuntimeCapability.SERVER_THREAD_PROFILING)); }
}
