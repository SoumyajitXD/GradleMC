package com.soumyajit.gradlemc.lock;

import com.soumyajit.gradlemc.config.GradleMCConfig;
import com.soumyajit.gradlemc.instance.*;
import com.soumyajit.gradlemc.modaudit.*;
import com.soumyajit.gradlemc.task.TaskEngine;
import com.soumyajit.gradlemc.util.GradleMcPaths;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraftforge.versions.forge.ForgeVersion;
import java.io.IOException;
import java.util.*;

public final class InstanceLockService {
    private InstanceLockService(){ }
    public static InstanceLockSnapshot capture(){MinecraftInstanceSnapshot instance=MinecraftInstanceService.current(true);List<InstanceLockSnapshot.LockedMod> mods=instance.mods().value().mods().stream().map(mod->new InstanceLockSnapshot.LockedMod(mod.modId(),mod.version(),mod.jarFileName(),mod.dependencies().stream().map(d->d.modId()+"@"+d.versionRange()+":"+d.side()+":"+d.ordering()).sorted().toList())).sorted(Comparator.comparing(InstanceLockSnapshot.LockedMod::id)).toList();
        List<String> unavailable=new ArrayList<>();if(instance.resourcePacks().availability()!=ComponentAvailability.AVAILABLE)unavailable.add("selected resource-pack order: "+instance.resourcePacks().limitation());if(instance.shaderPacks().availability()!=ComponentAvailability.AVAILABLE)unavailable.add("active shader pack: "+instance.shaderPacks().limitation());if(instance.dataPacks().availability()!=ComponentAvailability.AVAILABLE)unavailable.add("enabled datapack order: "+instance.dataPacks().limitation());
        Map<String,String> config=new TreeMap<>();config.put("reportsEnabled",Boolean.toString(GradleMCConfig.REPORTS_ENABLED.get()));config.put("reportDirectory",GradleMCConfig.REPORT_DIRECTORY_NAME.get());config.put("healthGates",Boolean.toString(GradleMCConfig.HEALTH_GATES_ENABLED.get()));config.put("gateMinTps",Double.toString(GradleMCConfig.GATE_MIN_TPS.get()));config.put("gateMinMedianFps",Double.toString(GradleMCConfig.GATE_MIN_MEDIAN_FPS.get()));
        return new InstanceLockSnapshot(1,"1.20.1",ForgeVersion.getVersion(),Integer.toString(Runtime.version().feature()),System.getProperty("java.vendor","unknown")+" / "+System.getProperty("java.vm.name","unknown"),"1.0.3",mods,packIds(instance.resourcePacks().value()),packIds(instance.shaderPacks().value()),packIds(instance.dataPacks().value()),TaskEngine.fingerprint(config),new TreeMap<>(instance.providers().value()),unavailable.stream().sorted().toList());}
    public static InstanceLockDiff diffCurrent()throws IOException{return InstanceLockComparator.compare(InstanceLockIO.read(GradleMcPaths.instanceLockFile()),capture());}
    public static int write(CommandSourceStack source){try{InstanceLockIO.write(GradleMcPaths.instanceLockFile(),capture());send(source,"Instance lock written: "+GradleMcPaths.instanceLockFile().getFileName());return 1;}catch(IOException|RuntimeException e){return fail(source,"Instance lock write failed: "+safe(e));}}
    public static int check(CommandSourceStack source){try{InstanceLockDiff diff=diffCurrent();send(source,diff.matches()?"Instance lock matches available normalized inventory.":"Instance lock differs; run /gradlemc instance lock diff.");if(!diff.unavailable().isEmpty())send(source,"Unavailable comparisons: "+diff.unavailable());return diff.matches()?1:0;}catch(IOException|RuntimeException e){return fail(source,"Instance lock check failed: "+safe(e));}}
    public static int diff(CommandSourceStack source){try{InstanceLockDiff d=diffCurrent();send(source,"added="+d.added()+" removed="+d.removed()+" changed="+d.changed());send(source,"reordered="+d.reordered()+" unavailable="+d.unavailable()+" ambiguous="+d.ambiguous());return d.matches()?1:0;}catch(IOException|RuntimeException e){return fail(source,"Instance lock diff failed: "+safe(e));}}
    private static List<String> packIds(List<PackDescriptor> packs){return packs.stream().map(p->p.id()+"@"+p.packFormat()).toList();}
    private static String safe(Exception e){return e.getMessage()==null?e.getClass().getSimpleName():e.getMessage();}private static int fail(CommandSourceStack s,String m){s.sendFailure(Component.literal(m));return 0;}private static void send(CommandSourceStack s,String m){s.sendSuccess(()->Component.literal(m),false);}
}
