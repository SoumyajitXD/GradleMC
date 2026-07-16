package com.soumyajit.gradlemc.lock;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public final class InstanceLockComparator {
    private InstanceLockComparator() { }
    public static InstanceLockDiff compare(InstanceLockSnapshot before,InstanceLockSnapshot after){List<String> added=new ArrayList<>(),removed=new ArrayList<>(),changed=new ArrayList<>(),reordered=new ArrayList<>(),ambiguous=new ArrayList<>();
        compareValue("minecraft",before.minecraft(),after.minecraft(),changed);compareValue("forge",before.forge(),after.forge(),changed);compareValue("javaMajor",before.javaMajor(),after.javaMajor(),changed);compareValue("javaRuntime",before.javaRuntime(),after.javaRuntime(),changed);compareValue("gradlemc",before.gradleMc(),after.gradleMc(),changed);compareValue("configFingerprint",before.configFingerprint(),after.configFingerprint(),changed);
        Map<String,InstanceLockSnapshot.LockedMod> a=unique(before.mods(),InstanceLockSnapshot.LockedMod::id,ambiguous,"baseline mod"),b=unique(after.mods(),InstanceLockSnapshot.LockedMod::id,ambiguous,"current mod");
        for(String id:new TreeSet<>(b.keySet()))if(!a.containsKey(id))added.add("mod:"+id);for(String id:new TreeSet<>(a.keySet()))if(!b.containsKey(id))removed.add("mod:"+id);for(String id:new TreeSet<>(a.keySet()))if(b.containsKey(id)&&!a.get(id).equals(b.get(id)))changed.add("mod:"+id+" "+a.get(id).version()+" -> "+b.get(id).version());
        compareList("resourcePack",before.resourcePacks(),after.resourcePacks(),added,removed,reordered);compareList("shaderPack",before.shaderPacks(),after.shaderPacks(),added,removed,reordered);compareList("dataPack",before.dataPacks(),after.dataPacks(),added,removed,reordered);
        List<String> unavailable=new TreeSet<String>(){{addAll(before.unavailable());addAll(after.unavailable());}}.stream().toList();return new InstanceLockDiff(added,removed,changed,reordered,unavailable,ambiguous);}
    private static <T> Map<String,T> unique(List<T> values,Function<T,String> key,List<String> ambiguous,String label){Map<String,T> out=new TreeMap<>();for(T value:values){String id=key.apply(value);if(out.putIfAbsent(id,value)!=null)ambiguous.add(label+":"+id);}return out;}
    private static void compareValue(String key,String a,String b,List<String> changed){if(!Objects.equals(a,b))changed.add(key+":"+a+" -> "+b);}
    private static void compareList(String kind,List<String> a,List<String> b,List<String> added,List<String> removed,List<String> reordered){Set<String> as=new LinkedHashSet<>(a),bs=new LinkedHashSet<>(b);for(String v:bs)if(!as.contains(v))added.add(kind+":"+v);for(String v:as)if(!bs.contains(v))removed.add(kind+":"+v);if(as.equals(bs)&&!a.equals(b))reordered.add(kind+":"+a+" -> "+b);}
}
