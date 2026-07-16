package com.soumyajit.gradlemc.client.gui.model;

import com.soumyajit.gradlemc.modaudit.*;
import java.util.*;

/** Query-sensitive immutable GUI projection; avoids rebuilding and N*M finding scans every frame. */
public final class ModListViewModel {
    public static final int PAGE_SIZE=200;
    private List<ModDescriptor> source=List.of();private List<ModAuditFinding> findings=List.of();private String query="";private View cached=new View(List.of(),Map.of(),0,false);
    public synchronized View project(ModAuditResult audit,String rawQuery){String normalized=rawQuery==null?"":rawQuery.trim().toLowerCase(Locale.ROOT);if(source==audit.snapshot().mods()&&findings==audit.findings()&&query.equals(normalized))return cached;source=audit.snapshot().mods();findings=audit.findings();query=normalized;Map<String,Integer> counts=new HashMap<>();for(ModAuditFinding finding:findings)for(String id:finding.affectedModIds())counts.merge(id,1,Integer::sum);List<ModDescriptor> matches=source.stream().filter(mod->query.isEmpty()||mod.modId().contains(query)||mod.displayName().toLowerCase(Locale.ROOT).contains(query)).toList();cached=new View(matches.stream().limit(PAGE_SIZE).toList(),Map.copyOf(counts),matches.size(),matches.size()>PAGE_SIZE);return cached;}
    public record View(List<ModDescriptor> mods,Map<String,Integer> findingCounts,int totalMatches,boolean truncated){public View{mods=List.copyOf(mods);findingCounts=Map.copyOf(findingCounts);}}
}
