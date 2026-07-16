package com.soumyajit.gradlemc.health;

import com.soumyajit.gradlemc.task.TaskState;
import java.util.*;

public final class HealthGateSelfTest {
    private HealthGateSelfTest() { }
    public static void run() {
        List<HealthGate> gates=List.of(new HealthGate("fps","fps",HealthGateKind.MINIMUM,"medianFps",60,true),new HealthGate("scan","scan",HealthGateKind.REQUIRED_TASK,"gradlemc:export_scan",0,true),new HealthGate("off","off",HealthGateKind.MAXIMUM,"x",1,false));
        List<HealthGateResult> missing=HealthGateEvaluator.evaluate(gates,new HealthGateEvidence(Map.of(),Map.of(),Map.of()));
        assertState(missing,"fps",HealthGateState.INCONCLUSIVE); assertState(missing,"scan",HealthGateState.INCONCLUSIVE); assertState(missing,"off",HealthGateState.NOT_EVALUATED);
        List<HealthGateResult> pass=HealthGateEvaluator.evaluate(gates,new HealthGateEvidence(Map.of("medianFps",75D),Map.of("gradlemc:export_scan",TaskState.SUCCESS),Map.of("medianFps","fps:1")));
        assertState(pass,"fps",HealthGateState.PASS); assertState(pass,"scan",HealthGateState.PASS);
        List<HealthGateResult> fail=HealthGateEvaluator.evaluate(gates,new HealthGateEvidence(Map.of("medianFps",30D),Map.of("gradlemc:export_scan",TaskState.FAILED),Map.of()));
        assertState(fail,"fps",HealthGateState.FAIL); assertState(fail,"scan",HealthGateState.FAIL);
    }
    private static void assertState(List<HealthGateResult> values,String id,HealthGateState expected){HealthGateState actual=values.stream().filter(v->v.gateId().equals(id)).findFirst().orElseThrow().state();if(actual!=expected)throw new AssertionError(id+" expected="+expected+" actual="+actual);}
}
