package com.soumyajit.gradlemc.investigation.storage;
import com.soumyajit.gradlemc.investigation.*;import java.time.Instant;
public record InvestigationIndexEntry(InvestigationId id,InvestigationProfileId profileId,InvestigationState state,Instant createdAt,Instant endedAt,long revision){public InvestigationIndexEntry{if(id==null||profileId==null||state==null||createdAt==null||revision<0)throw new IllegalArgumentException("Invalid index entry");}}
