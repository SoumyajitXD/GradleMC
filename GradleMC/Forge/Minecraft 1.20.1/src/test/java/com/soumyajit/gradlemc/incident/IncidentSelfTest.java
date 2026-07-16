package com.soumyajit.gradlemc.incident;

import java.time.Instant;
import java.util.*;

public final class IncidentSelfTest {
    private IncidentSelfTest() { }
    public static void run(){IncidentRecorder r=IncidentRecorder.instance();r.resetForTests();r.start();for(int i=0;i<IncidentRecorder.MAX_SIGNALS+20;i++)r.signal(new IncidentSignal(Instant.now(),"tick",Map.of("mspt",(double)i)));for(int i=0;i<IncidentRecorder.MAX_INCIDENTS+5;i++)r.trigger("manual",Map.of("i",Integer.toString(i)),List.of("e:"+i));if(r.incidents().size()!=IncidentRecorder.MAX_INCIDENTS)throw new AssertionError("incident limit");Incident latest=r.latest().orElseThrow();if(latest.preWindow().size()>IncidentRecorder.MAX_WINDOW_SIGNALS)throw new AssertionError("window limit");if(!latest.truncated())throw new AssertionError("truncation flag");r.stop();if(r.trigger("ignored",Map.of(),List.of()).isPresent())throw new AssertionError("stopped recorder");r.resetForTests();}
}
