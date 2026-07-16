package com.soumyajit.gradlemc.instance;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

public final class InstanceModelSelfTest {
    private InstanceModelSelfTest() { }
    public static void run() throws Exception {
        fingerprintIsStableAndSensitive();
        readsMetadataWithoutExposingPaths();
        rejectsUnsafeArchiveEntry();
    }
    private static void fingerprintIsStableAndSensitive() {
        List<PackDescriptor> packs = List.of(new PackDescriptor("a", "a.zip", PackDescriptor.PackKind.RESOURCE, true, 2, 4, 15, "parsed", List.of(), List.of()));
        String first = InstanceFingerprint.calculate(Map.of("minecraft", "1.20.1", "loader", "Forge"), Map.of("java", "17"), packs, List.of(), List.of(), List.of("example@1"));
        String second = InstanceFingerprint.calculate(Map.of("loader", "Forge", "minecraft", "1.20.1"), Map.of("java", "17"), packs, List.of(), List.of(), List.of("example@1"));
        String changed = InstanceFingerprint.calculate(Map.of("minecraft", "1.20.1", "loader", "Forge"), Map.of("java", "17"), List.of(new PackDescriptor("a", "a.zip", PackDescriptor.PackKind.RESOURCE, true, 3, 4, 15, "parsed", List.of(), List.of())), List.of(), List.of(), List.of("example@1"));
        check(first.equals(second), "fingerprint ordering"); check(!first.equals(changed), "fingerprint detects pack change");
    }
    private static void readsMetadataWithoutExposingPaths() throws Exception {
        Path root = Files.createTempDirectory("gradlemc-pack-test");
        try { Path pack = Files.createDirectory(root.resolve("ExamplePack")); Files.writeString(pack.resolve("pack.mcmeta"), "{\"pack\":{\"pack_format\":15}}"); Files.createDirectories(pack.resolve("assets/example")); PackDescriptor result = PackMetadataReader.read(root, pack, PackDescriptor.PackKind.RESOURCE); check(result.parseStatus().equals("parsed"), "metadata parsed"); check(result.namespaces().equals(List.of("example")), "namespace read"); check(!result.fileName().contains(root.toString()), "path redacted"); }
        finally { try (var paths = Files.walk(root)) { paths.sorted(java.util.Comparator.reverseOrder()).forEach(path -> { try { Files.deleteIfExists(path); } catch (Exception ignored) { } }); } }
    }
    private static void rejectsUnsafeArchiveEntry() throws Exception {
        Path root = Files.createTempDirectory("gradlemc-zip-test"); Path zip = root.resolve("unsafe.zip");
        try (var out = new java.util.zip.ZipOutputStream(Files.newOutputStream(zip))) { out.putNextEntry(new java.util.zip.ZipEntry("../bad")); out.write(1); out.closeEntry(); }
        try { PackDescriptor result = PackMetadataReader.read(root, zip, PackDescriptor.PackKind.RESOURCE); check(result.parseStatus().equals("rejected"), "unsafe zip rejected"); }
        finally { try (var paths = Files.walk(root)) { paths.sorted(java.util.Comparator.reverseOrder()).forEach(path -> { try { Files.deleteIfExists(path); } catch (Exception ignored) { } }); } }
    }
    private static void check(boolean condition, String name) { if (!condition) throw new AssertionError(name); }
}
