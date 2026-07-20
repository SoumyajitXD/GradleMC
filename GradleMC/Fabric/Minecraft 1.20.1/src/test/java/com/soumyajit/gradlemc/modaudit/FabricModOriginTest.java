package com.soumyajit.gradlemc.modaudit;

import net.fabricmc.loader.api.metadata.ModOrigin;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class FabricModOriginTest {
    @Test
    void nestedOriginsNeverRequestUnavailableFilesystemPaths() {
        ModOrigin nested = new ModOrigin() {
            @Override public Kind getKind() { return Kind.NESTED; }
            @Override public List<Path> getPaths() { throw new UnsupportedOperationException("nested origins have no paths"); }
            @Override public String getParentModId() { return "fabric-api"; }
            @Override public String getParentSubLocation() { return "META-INF/jars/nested.jar"; }
        };
        assertEquals(List.of(), FabricModAuditService.originNames(nested));
    }

    @Test
    void pathOriginsExposeOnlyBasenames() {
        ModOrigin path = new ModOrigin() {
            @Override public Kind getKind() { return Kind.PATH; }
            @Override public List<Path> getPaths() { return List.of(Path.of("C:/synthetic/private/mod.jar")); }
            @Override public String getParentModId() { throw new UnsupportedOperationException(); }
            @Override public String getParentSubLocation() { throw new UnsupportedOperationException(); }
        };
        assertEquals(List.of("mod.jar"), FabricModAuditService.originNames(path));
    }
}
