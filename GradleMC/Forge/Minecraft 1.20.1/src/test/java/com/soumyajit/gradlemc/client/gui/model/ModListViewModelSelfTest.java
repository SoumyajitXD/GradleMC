package com.soumyajit.gradlemc.client.gui.model;

import com.soumyajit.gradlemc.check.Severity;
import com.soumyajit.gradlemc.modaudit.*;
import java.time.Instant;
import java.util.*;

public final class ModListViewModelSelfTest {
    private ModListViewModelSelfTest(){ }
    public static void run(){List<ModDescriptor> mods=new ArrayList<>();for(int i=0;i<250;i++)mods.add(new ModDescriptor("mod"+i,"mod"+i,"Display "+i,"1","mod"+i+".jar",1,List.of(),false,false,List.of()));InstalledModSnapshot snapshot=new InstalledModSnapshot(Instant.now(),mods,true,"");ModAuditFinding finding=new ModAuditFinding(Severity.WARN,"category","f","title","detail",List.of("mod2"),List.of("evidence"),AuditConfidence.STRONG_EVIDENCE,"action","basis");ModAuditResult audit=new ModAuditResult(snapshot,DependencyGraph.of(snapshot),List.of(finding),Instant.now(),false);ModListViewModel model=new ModListViewModel();var all=model.project(audit,"");if(all.mods().size()!=ModListViewModel.PAGE_SIZE||!all.truncated())throw new AssertionError("page bound");var filtered=model.project(audit,"Display 2");if(filtered.totalMatches()==0||filtered.mods().stream().anyMatch(m->!m.displayName().contains("2")))throw new AssertionError("filter");if(filtered.findingCounts().getOrDefault("mod2",0)!=1)throw new AssertionError("finding count");if(model.project(audit,"Display 2")!=filtered)throw new AssertionError("cached projection");}
}
