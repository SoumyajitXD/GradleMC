package com.soumyajit.gradlemc.incident;

import com.soumyajit.gradlemc.task.DiagnosticRunService;
import com.soumyajit.gradlemc.util.GradleMcPaths;
import com.soumyajit.gradlemc.util.RuntimeSnapshots;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import java.io.IOException;
import java.time.Instant;
import java.util.*;

public final class IncidentService {
    private IncidentService() { }
    public static int start(CommandSourceStack source){IncidentRecorder recorder=IncidentRecorder.instance();recorder.start();RuntimeSnapshots.MemorySnapshot m=RuntimeSnapshots.memory();recorder.signal(new IncidentSignal(Instant.now(),"recorder-start",Map.of("usedMiB",(double)m.usedMiB(),"maxMiB",(double)m.maxMiB())));send(source,"GradleMC incident capture started; bounded cheap signals only.");return 1;}
    public static int stop(CommandSourceStack source){IncidentRecorder.instance().stop();send(source,"GradleMC incident capture stopped; retained incidents remain bounded in memory.");return 1;}
    public static int list(CommandSourceStack source){List<Incident> values=IncidentRecorder.instance().incidents();if(values.isEmpty())send(source,"No captured incidents.");else values.stream().limit(12).forEach(i->send(source,i.id()+" | "+i.trigger()+" | "+i.timestamp()));return 1;}
    public static int latest(CommandSourceStack source){return IncidentRecorder.instance().latest().map(i->show(source,i)).orElseGet(()->fail(source,"No captured incidents."));}
    public static int inspect(CommandSourceStack source,String id){return IncidentRecorder.instance().find(id).map(i->show(source,i)).orElseGet(()->fail(source,"Unknown incident ID: "+id));}
    public static int export(CommandSourceStack source,String id){Optional<Incident> incident=IncidentRecorder.instance().find(id);if(incident.isEmpty())return fail(source,"Unknown incident ID: "+id);try{var path=new IncidentExporter(GradleMcPaths.gameDirectory(),GradleMcPaths.incidentDirectory()).export(incident.get());send(source,"Incident exported: "+path.getFileName());return 1;}catch(IOException|IllegalArgumentException e){return fail(source,"Incident export failed: "+(e.getMessage()==null?e.getClass().getSimpleName():e.getMessage()));}}
    public static int mark(CommandSourceStack source){Map<String,String> context=new TreeMap<>();context.put("taskRun",DiagnosticRunService.latestRequested().isBlank()?"none":DiagnosticRunService.latestRequested());context.put("dimension",source.getLevel().dimension().location().toString());RuntimeSnapshots.MemorySnapshot m=RuntimeSnapshots.memory();context.put("heapPressure",m.pressureLabel());Optional<Incident> value=IncidentRecorder.instance().trigger("manual-marker",context,List.of("manual-marker"));if(value.isEmpty())return fail(source,"Incident capture is not running.");send(source,"Captured "+value.get().id()+" without packet payloads, coordinates, or filesystem scanning.");return 1;}
    private static int show(CommandSourceStack source,Incident i){send(source,i.id()+" | "+i.trigger()+" | "+i.timestamp()+" | preSignals="+i.preWindow().size()+" | truncated="+i.truncated());send(source,"Context: "+i.context()+" Evidence: "+i.evidenceIds());return 1;}
    private static int fail(CommandSourceStack source,String value){source.sendFailure(Component.literal(value));return 0;}private static void send(CommandSourceStack source,String value){source.sendSuccess(()->Component.literal(value),false);}
}
