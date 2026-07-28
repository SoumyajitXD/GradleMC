package com.soumyajit.gradlemc.investigation.storage;

import com.soumyajit.gradlemc.investigation.*;
import java.nio.file.*;
import java.time.*;
import java.util.*;

/** Dependency-free hostile-boundary tests; all fixtures stay beneath one temporary game directory. */
public final class InvestigationStorageSelfTest {
    private InvestigationStorageSelfTest() { }
    public static void run() throws Exception {
        Path game = Files.createTempDirectory("gradlemc-investigation-");
        try { codec(); storeAndRecovery(game); retention(); }
        finally { try (var paths = Files.walk(game)) { for (Path path : paths.sorted(Comparator.reverseOrder()).toList()) Files.deleteIfExists(path); } }
    }
    private static InvestigationManifest manifest(String id, InvestigationState state, long rev) {
        Instant created = Instant.parse("2026-01-01T00:00:00Z"); Instant ended = state.terminal() ? created.plusSeconds(1) : null;
        return new InvestigationManifest(1, rev, new InvestigationId(id), new InvestigationProfileId("placeholder"), "1", state, created, state == InvestigationState.CREATED ? null : created, ended, new InvestigationRuntimeIdentity("1.0.3", "1.20.1", "47.4.20", "COMMON"), "abc", List.of(), List.of(), state == InvestigationState.COMPLETED_WITH_LIMITATIONS ? List.of(new InvestigationLimitation("limited", "x")) : List.of(), state == InvestigationState.FAILED ? Optional.of(new InvestigationFailure("failed", "x", true)) : Optional.empty(), false, List.of(), 1, state == InvestigationState.COMPLETED);
    }
    private static void codec() throws Exception {
        InvestigationManifestCodec codec = new InvestigationManifestCodec(); byte[] bytes = codec.encode(manifest("inv-1", InvestigationState.CREATED, 0));
        check(Arrays.equals(bytes, codec.encode(manifest("inv-1", InvestigationState.CREATED, 0))), "canonical manifest bytes"); check(codec.decode(bytes).equals(manifest("inv-1", InvestigationState.CREATED, 0)), "manifest round trip");
        bad(() -> codec.decode("{".getBytes())); bad(() -> codec.decode(new byte[InvestigationManifestCodec.MAX_BYTES + 1]));
        InvestigationIndexCodec index = new InvestigationIndexCodec(); bad(() -> index.decode(new byte[InvestigationIndexCodec.MAX_BYTES + 1]));
    }
    private static void storeAndRecovery(Path game) throws Exception {
        InvestigationStore store = new InvestigationStore(game); InvestigationManifest created = manifest("inv-1", InvestigationState.CREATED, 0); store.create(created); check(store.read(created.id()).equals(created), "initial read");
        InvestigationManifest running = manifest("inv-1", InvestigationState.RUNNING, 1); store.update(running, 0, InvestigationState.CREATED); bad(() -> store.update(running, 0, InvestigationState.CREATED));
        InvestigationManifest complete = manifest("inv-1", InvestigationState.COMPLETED, 2); store.update(complete, 1, InvestigationState.RUNNING); bad(() -> store.update(complete, 2, InvestigationState.COMPLETED));
        InvestigationManifest interrupted = manifest("inv-recover", InvestigationState.CREATED, 0); store.create(interrupted);
        InvestigationStore.RecoverySummary recovery = store.recoverInterrupted(4);
        check(recovery.recovered() == 1 && store.read(interrupted.id()).state() == InvestigationState.FAILED, "interrupted sessions are terminally reconciled without rerun");
        Files.writeString(InvestigationPaths.index(game), "bad"); check(store.loadIndex().rebuilt(), "malformed index rebuild");
        Path root = InvestigationPaths.root(game); Path mismatch = root.resolve("inv-mismatch"); Files.createDirectories(mismatch); Files.write(mismatch.resolve("manifest.json"), new InvestigationManifestCodec().encode(manifest("inv-other", InvestigationState.CREATED, 0)));
        check(store.rebuildIndex().diagnostics().stream().anyMatch(x -> x.startsWith("skipped-inv-mismatch:")), "directory/manifest mismatch diagnostic");
        Files.write(InvestigationPaths.index(game), new byte[InvestigationIndexCodec.MAX_BYTES + 1]); check(store.loadIndex().rebuilt(), "oversized index rebuild");
        Files.write(root.resolve("unrelated.txt"), new byte[] {1}); check(store.rebuildIndex().index().entries().size() == 2, "unrelated child ignored");
        Path manifestPath = InvestigationPaths.manifest(game, created.id());
        Files.write(manifestPath, new byte[InvestigationManifestCodec.MAX_BYTES + 1]);
        bad(() -> store.read(created.id()));
        Files.write(manifestPath, new InvestigationManifestCodec().encode(complete));
    }
    private static void retention() {
        Instant now = Instant.parse("2026-02-01T00:00:00Z"); List<InvestigationIndexEntry> entries = List.of(new InvestigationIndexEntry(new InvestigationId("inv-1"),new InvestigationProfileId("p"),InvestigationState.COMPLETED,Instant.parse("2025-01-01T00:00:00Z"),Instant.parse("2025-01-02T00:00:00Z"),1),new InvestigationIndexEntry(new InvestigationId("inv-2"),new InvestigationProfileId("p"),InvestigationState.RUNNING,now,null,1),new InvestigationIndexEntry(new InvestigationId("inv-3"),new InvestigationProfileId("p"),InvestigationState.COMPLETED,now,now,1));
        var plan = InvestigationRetentionPlan.plan(entries, Map.of(new InvestigationId("inv-1"),100L,new InvestigationId("inv-3"),100L), new InvestigationRetentionPolicy(1,100,Duration.ofDays(30),0),now); check(plan.candidates().size()==1 && plan.candidates().get(0).id().value().equals("inv-1"), "retention terminal candidate");
    }
    private interface Throwing { void run() throws Exception; }
    private static void bad(Throwing r) { try { r.run(); throw new AssertionError("expected rejection"); } catch (Exception expected) { } }
    private static void check(boolean value, String name) { if (!value) throw new AssertionError("Investigation storage self-test failed: " + name); }
}
