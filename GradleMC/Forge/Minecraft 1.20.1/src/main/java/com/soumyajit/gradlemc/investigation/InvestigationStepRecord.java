package com.soumyajit.gradlemc.investigation;
import java.time.Instant;
public record InvestigationStepRecord(String stepId, InvestigationStepState state, Instant startedAt, Instant endedAt, String detail) {
 public InvestigationStepRecord { if(stepId==null||!stepId.matches("[a-z][a-z0-9:-]{0,95}"))throw new IllegalArgumentException("Invalid step ID"); if(state==null)throw new IllegalArgumentException("step state required"); detail=InvestigationText.safe(detail==null?"":detail); if(startedAt!=null&&endedAt!=null&&endedAt.isBefore(startedAt))throw new IllegalArgumentException("step end precedes start"); if(state==InvestigationStepState.RUNNING&&startedAt==null)throw new IllegalArgumentException("running step requires start"); if((state==InvestigationStepState.SUCCEEDED||state==InvestigationStepState.FAILED||state==InvestigationStepState.CANCELLED||state==InvestigationStepState.TIMED_OUT)&&endedAt==null)throw new IllegalArgumentException("terminal step requires end"); }
}
