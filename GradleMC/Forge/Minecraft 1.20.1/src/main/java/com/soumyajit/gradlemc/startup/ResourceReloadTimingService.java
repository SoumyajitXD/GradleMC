package com.soumyajit.gradlemc.startup;

import com.soumyajit.gradlemc.util.RuntimeSnapshots;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.event.OnDatapackSyncEvent;
import java.lang.management.*;
import java.time.*;

public final class ResourceReloadTimingService {
    private static Instant requested,started;private static long heapBefore,gcBefore;private static ReloadObservation latest;
    private ResourceReloadTimingService(){ }
    public static synchronized int observe(CommandSourceStack source){requested=Instant.now();started=null;heapBefore=RuntimeSnapshots.memory().usedMiB();gcBefore=gcCount();source.sendSuccess(()->Component.literal("Reload observation armed. Run the normal /reload command explicitly; GradleMC will not trigger it."),false);return 1;}
    public static synchronized void onReloadListeners(AddReloadListenerEvent event){if(requested!=null&&started==null)started=Instant.now();}
    public static synchronized void onDatapackSync(OnDatapackSyncEvent event){if(requested==null)return;Instant end=Instant.now(),begin=started==null?requested:started;latest=new ReloadObservation(requested,begin,end,Math.max(0,Duration.between(begin,end).toMillis()),TimingSource.TIMESTAMP_DERIVED,heapBefore,RuntimeSnapshots.memory().usedMiB(),gcBefore,gcCount(),"completed","Derived from Forge reload-listener and datapack-sync boundaries; per-stage timing and client shader state are unavailable.");requested=null;started=null;}
    public static synchronized int show(CommandSourceStack source){if(latest==null){source.sendSuccess(()->Component.literal(requested==null?"No reload observation is available.":"Reload observation is armed and awaiting supported lifecycle events."),false);return 1;}ReloadObservation v=latest;source.sendSuccess(()->Component.literal("Reload | "+v.durationMillis()+" ms | "+v.source()+" | heap "+v.heapBeforeMiB()+" -> "+v.heapAfterMiB()+" MiB | GC "+v.gcBefore()+" -> "+v.gcAfter()),false);source.sendSuccess(()->Component.literal(v.limitation()),false);return 1;}
    public static synchronized ReloadObservation latest(){return latest;}
    private static long gcCount(){long count=0;for(GarbageCollectorMXBean bean:ManagementFactory.getGarbageCollectorMXBeans())if(bean.getCollectionCount()>0)count+=bean.getCollectionCount();return count;}
}
