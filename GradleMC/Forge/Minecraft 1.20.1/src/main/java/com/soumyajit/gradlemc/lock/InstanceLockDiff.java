package com.soumyajit.gradlemc.lock;

import java.util.List;

public record InstanceLockDiff(List<String> added,List<String> removed,List<String> changed,List<String> reordered,
                               List<String> unavailable,List<String> ambiguous){
    public InstanceLockDiff{added=List.copyOf(added);removed=List.copyOf(removed);changed=List.copyOf(changed);reordered=List.copyOf(reordered);unavailable=List.copyOf(unavailable);ambiguous=List.copyOf(ambiguous);}
    public boolean matches(){return added.isEmpty()&&removed.isEmpty()&&changed.isEmpty()&&reordered.isEmpty()&&ambiguous.isEmpty();}
}
