package com.soumyajit.gradlemc.startup;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.server.*;
import java.time.*;
import java.util.*;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import com.soumyajit.gradlemc.util.GradleMcPaths;

/** Supported lifecycle markers only; no third-party class instrumentation. */
public final class StartupTimingService {
    private static final Instant CLASS_LOADED=Instant.now();private static final Map<String,Instant> starts=new HashMap<>();private static final ArrayDeque<StartupPhase> phases=new ArrayDeque<>();private static final int MAX_PHASES=64;
    private StartupTimingService(){ }
    public static synchronized void initializationCompleted(){finish("gradlemc-initialization",CLASS_LOADED,Instant.now(),TimingSource.MEASURED,"GradleMC-owned constructor boundary");}
    public static synchronized void onRegisterCommands(RegisterCommandsEvent event){Instant now=Instant.now();finish("command-registration",now,now,TimingSource.TIMESTAMP_DERIVED,"Forge registration callback timestamp; callback duration is below clock resolution");}
    public static synchronized void onServerAboutToStart(ServerAboutToStartEvent event){starts.put("server-startup",Instant.now());starts.put("world-load",Instant.now());}
    public static synchronized void onServerStarting(ServerStartingEvent event){Instant now=Instant.now();Instant start=starts.remove("world-load");if(start!=null)finish("world-load",start,now,TimingSource.TIMESTAMP_DERIVED,"Forge server lifecycle boundary");}
    public static synchronized void onServerStarted(ServerStartedEvent event){Instant start=starts.remove("server-startup");if(start!=null)finish("server-startup",start,Instant.now(),TimingSource.TIMESTAMP_DERIVED,"Forge about-to-start through started events");}
    public static synchronized List<StartupPhase> latest(){return List.copyOf(phases);}
    public static int show(CommandSourceStack source){List<StartupPhase> values=latest();if(values.isEmpty())source.sendSuccess(()->Component.literal("No startup timing markers are available."),false);else values.stream().limit(12).forEach(p->source.sendSuccess(()->Component.literal(p.id()+" | "+p.durationMillis()+" ms | "+p.source()+" | "+p.limitation()),false));return 1;}
    public static int export(CommandSourceStack source){try{Path root=GradleMcPaths.reportDirectory().toAbsolutePath().normalize();Files.createDirectories(root);if(Files.isSymbolicLink(root))throw new java.io.IOException("Report directory cannot be a symbolic link");Path target=root.resolve("gradlemc-startup-"+Instant.now().toEpochMilli()+".json"),temp=Files.createTempFile(root,".startup-",".tmp");StringBuilder json=new StringBuilder("{\n  \"schemaVersion\":1,\n  \"phases\":[");List<StartupPhase> values=latest();for(int i=0;i<values.size();i++){StartupPhase p=values.get(i);if(i>0)json.append(',');json.append("\n    {\"id\":\"").append(escape(p.id())).append("\",\"startedAt\":\"").append(p.startedAt()).append("\",\"endedAt\":\"").append(p.endedAt()).append("\",\"durationMillis\":").append(p.durationMillis()).append(",\"source\":\"").append(p.source()).append("\",\"limitation\":\"").append(escape(p.limitation())).append("\"}");}json.append("\n  ]\n}\n");try{Files.writeString(temp,json,StandardCharsets.UTF_8,StandardOpenOption.TRUNCATE_EXISTING);try{Files.move(temp,target,StandardCopyOption.ATOMIC_MOVE);}catch(AtomicMoveNotSupportedException e){Files.move(temp,target);}}finally{Files.deleteIfExists(temp);}source.sendSuccess(()->Component.literal("Startup timing exported: "+target.getFileName()),false);return 1;}catch(Exception e){source.sendFailure(Component.literal("Startup timing export failed: "+(e.getMessage()==null?e.getClass().getSimpleName():e.getMessage())));return 0;}}
    private static String escape(String value){return value.replace("\\","\\\\").replace("\"","\\\"").replace("\n","\\n").replace("\r","\\r");}
    private static void finish(String id,Instant start,Instant end,TimingSource source,String limitation){phases.addFirst(new StartupPhase(id,start,end,Math.max(0,Duration.between(start,end).toMillis()),source,limitation));while(phases.size()>MAX_PHASES)phases.removeLast();}
}
