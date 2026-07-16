package com.soumyajit.gradlemc.foundation;
import java.time.*;
import java.util.Optional;
public record Freshness(State state, Instant capturedAt, Optional<Instant> validUntil, String source, String explanation, java.util.List<String> limitations) {
 public enum State { FRESH, AGING, STALE, CONTEXT_MISMATCH, MISSING, UNAVAILABLE }
 public Freshness { if(state==null) throw new IllegalArgumentException("state"); validUntil=validUntil==null?Optional.empty():validUntil; source=source==null?"unknown":source; explanation=explanation==null?"":explanation; limitations=Evidence.boundedList(limitations, Evidence.MAX_LIMITATIONS); }
 public static Freshness evaluate(Instant captured, Optional<Instant> until, boolean staticMatches, boolean runtimeMatches, Clock clock, String source) { if(captured==null)return new Freshness(State.MISSING,null,Optional.empty(),source,"No capture exists",java.util.List.of()); Instant now=clock.instant(); if(!staticMatches||!runtimeMatches)return new Freshness(State.CONTEXT_MISMATCH,captured,until,source,"Fingerprint context changed",java.util.List.of()); if(until.isPresent() && !now.isBefore(until.get()))return new Freshness(State.STALE,captured,until,source,"Validity period expired",java.util.List.of()); if(until.isPresent() && !now.isBefore(captured.plus(Duration.between(captured,until.get()).dividedBy(2))))return new Freshness(State.AGING,captured,until,source,"Approaching validity limit",java.util.List.of()); return new Freshness(State.FRESH,captured,until,source,"Capture remains comparable",java.util.List.of()); }
}
