package com.soumyajit.gradlemc.incident;

import com.soumyajit.gradlemc.util.ManagedPathSafety;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

public final class IncidentExporter {
    public static final int MAX_EXPORT_BYTES = 1024 * 1024;
    private final Path root;
    private final Path trustedBase;
    public IncidentExporter(Path root) { this(root.getParent(), root); }
    public IncidentExporter(Path trustedBase, Path root) { this.trustedBase = trustedBase.toAbsolutePath().normalize(); this.root = root.toAbsolutePath().normalize(); }

    public Path export(Incident incident) throws IOException {
        if (!incident.id().matches("incident-[0-9]+-[0-9]+")) throw new IllegalArgumentException("Invalid incident ID");
        ManagedPathSafety.ensureDirectory(trustedBase, root);
        Path target=root.resolve(incident.id()+".json").normalize(); if(!target.getParent().equals(root))throw new IOException("Incident export escaped its managed root");
        String value=json(incident); if(value.getBytes(StandardCharsets.UTF_8).length>MAX_EXPORT_BYTES)throw new IOException("Incident export exceeds its safe size limit");
        Path temporary=Files.createTempFile(root,".incident-",".tmp");
        try{Files.writeString(temporary,value,StandardCharsets.UTF_8,StandardOpenOption.TRUNCATE_EXISTING);try{Files.move(temporary,target,StandardCopyOption.ATOMIC_MOVE);}catch(AtomicMoveNotSupportedException e){Files.move(temporary,target);}}
        finally{Files.deleteIfExists(temporary);} return target;
    }
    private static String json(Incident i){StringBuilder out=new StringBuilder("{\n  \"schemaVersion\":1,\n  \"id\":\"").append(escape(i.id())).append("\",\n  \"trigger\":\"").append(escape(i.trigger())).append("\",\n  \"timestamp\":\"").append(i.timestamp()).append("\",\n  \"truncated\":").append(i.truncated()).append(",\n  \"context\":{");int n=0;for(var e:new TreeMap<>(i.context()).entrySet()){if(n++>0)out.append(',');out.append("\n    \"").append(escape(e.getKey())).append("\":\"").append(escape(e.getValue())).append('"');}out.append("\n  },\n  \"evidenceIds\":[");for(int x=0;x<i.evidenceIds().size();x++){if(x>0)out.append(',');out.append('"').append(escape(i.evidenceIds().get(x))).append('"');}out.append("],\n  \"preWindow\":");signals(out,i.preWindow());out.append(",\n  \"postWindow\":");signals(out,i.postWindow());return out.append("\n}\n").toString();}
    private static void signals(StringBuilder out,List<IncidentSignal> values){out.append('[');for(int x=0;x<values.size();x++){if(x>0)out.append(',');IncidentSignal s=values.get(x);out.append("{\"timestamp\":\"").append(s.timestamp()).append("\",\"kind\":\"").append(escape(s.kind())).append("\",\"metrics\":{");int n=0;for(var e:new TreeMap<>(s.metrics()).entrySet()){if(!Double.isFinite(e.getValue()))continue;if(n++>0)out.append(',');out.append('"').append(escape(e.getKey())).append("\":").append(e.getValue());}out.append("}}");}out.append(']');}
    private static String escape(String v){return v.replace("\\","\\\\").replace("\"","\\\"").replace("\n","\\n").replace("\r","\\r");}
}
