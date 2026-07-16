package com.soumyajit.gradlemc.modaudit;

import java.time.Instant;
import java.util.List;

public final class ModAuditModelSelfTest {
    private ModAuditModelSelfTest() { }
    public static void run() {
        ModDescriptor library = new ModDescriptor("Library", "library", "Library", "2.0", "C:/private/alice/mods/library.jar", 1, List.of(), false, false, List.of());
        ModDescriptor addon = new ModDescriptor("Addon", "addon", "Addon", "1.0", "addon.jar", 1, List.of(new ModDependencyDescriptor("LIBRARY", "[2.0,)", true, "BOTH", "NONE")), false, false, List.of());
        InstalledModSnapshot snapshot = new InstalledModSnapshot(Instant.EPOCH, List.of(addon, library), true, "");
        assertEquals("addon", snapshot.mods().get(0).modId(), "snapshot must use deterministic mod-id ordering");
        assertEquals("library.jar", library.jarFileName(), "reports must retain only a JAR filename");
        assertEquals(List.of("addon"), DependencyGraph.of(snapshot).dependantsOf("library"), "reverse dependencies must be exact");
        assertTrue(snapshot.find("LIBRARY").isPresent(), "mod lookup must normalize case");
    }
    private static void assertEquals(Object expected, Object actual, String message) { if (!expected.equals(actual)) throw new AssertionError(message); }
    private static void assertTrue(boolean value, String message) { if (!value) throw new AssertionError(message); }
}
