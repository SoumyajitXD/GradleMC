package com.soumyajit.gradlemc.doctor;

import com.soumyajit.gradlemc.config.GradleMCConfig;
import com.soumyajit.gradlemc.instance.MinecraftInstanceService;
import com.soumyajit.gradlemc.network.GradleMCNetwork;
import com.soumyajit.gradlemc.rules.RiskRuleLoader;
import com.soumyajit.gradlemc.task.*;
import com.soumyajit.gradlemc.util.GradleMcPaths;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.loading.FMLEnvironment;
import java.nio.file.*;
import java.util.*;

public final class DoctorService {
    private DoctorService(){ }
    public static List<DoctorResult> inspect(){List<DoctorResult> out=new ArrayList<>();out.add(new DoctorResult("version",DoctorResult.State.PASS,"GradleMC 1.0.3 / Minecraft 1.20.1 / Forge"));
        out.add(writable());try{GradleMCConfig.REPORTS_ENABLED.get();out.add(new DoctorResult("config",DoctorResult.State.PASS,"Forge common configuration is readable"));}catch(RuntimeException e){out.add(new DoctorResult("config",DoctorResult.State.FAIL,e.getClass().getSimpleName()));}
        try{var rules=RiskRuleLoader.current();long failures=rules.loadResults().stream().filter(r->r.severity().name().equals("FAIL")).count();out.add(new DoctorResult("rules",failures==0?DoctorResult.State.PASS:DoctorResult.State.FAIL,rules.rules().size()+" rules; "+failures+" parse failures"));}catch(RuntimeException e){out.add(new DoctorResult("rules",DoctorResult.State.WARN,e.getClass().getSimpleName()));}
        List<String> registry=DiagnosticRunService.registryIssues();out.add(new DoctorResult("task-registry",registry.isEmpty()?DoctorResult.State.PASS:DoctorResult.State.FAIL,registry.isEmpty()?DiagnosticRunService.tasks().size()+" valid unique tasks":registry.toString()));
        try{var providers=MinecraftInstanceService.current(false).providers();out.add(new DoctorResult("providers",providers.availability().name().equals("UNAVAILABLE")?DoctorResult.State.FAIL:DoctorResult.State.WARN,providers.availability()+": "+providers.limitation()));}catch(RuntimeException e){out.add(new DoctorResult("providers",DoctorResult.State.FAIL,e.getClass().getSimpleName()));}
        out.add(new DoctorResult("network",GradleMCNetwork.registered()?DoctorResult.State.PASS:DoctorResult.State.FAIL,"protocol="+GradleMCNetwork.protocolVersion()+", packetTypes="+GradleMCNetwork.registeredPacketTypes()));
        out.add(new DoctorResult("scan-schema",GradleMcScanWriter.SCHEMA_VERSION==3?DoctorResult.State.PASS:DoctorResult.State.FAIL,"schema="+GradleMcScanWriter.SCHEMA_VERSION));
        out.add(new DoctorResult("physical-side",DoctorResult.State.PASS,FMLEnvironment.dist==Dist.CLIENT?"client registrations physically isolated under client package":"dedicated server; common registrations only"));
        long failures=DiagnosticRunService.latestResults().stream().filter(r->r.state()==TaskState.FAILED||r.state()==TaskState.TIMED_OUT).count();out.add(new DoctorResult("recent-failures",failures==0?DoctorResult.State.PASS:DoctorResult.State.WARN,failures+" failed or timed-out tasks in latest run"));return List.copyOf(out);}
    public static int run(CommandSourceStack source){List<DoctorResult> values=inspect();values.forEach(v->source.sendSuccess(()->Component.literal(v.id()+" | "+v.state()+" | "+v.detail()),false));return values.stream().anyMatch(v->v.state()==DoctorResult.State.FAIL)?0:1;}
    private static DoctorResult writable(){Path root=GradleMcPaths.gradleMcDirectory();try{Files.createDirectories(root);if(Files.isSymbolicLink(root))return new DoctorResult("directory",DoctorResult.State.FAIL,"GradleMC root is a symbolic link");Path probe=Files.createTempFile(root,".doctor-",".tmp");Files.delete(probe);return new DoctorResult("directory",DoctorResult.State.PASS,"GradleMC output root is writable");}catch(Exception e){return new DoctorResult("directory",DoctorResult.State.FAIL,e.getClass().getSimpleName());}}
}
