package com.soumyajit.gradlemc.investigation;
public record InvestigationProfileId(String value) {
    public static final int MAX_LENGTH = 48;
    public InvestigationProfileId { if(value==null||!value.matches("[a-z][a-z0-9-]{0,47}")) throw new IllegalArgumentException("Invalid investigation profile ID"); }
    @Override public String toString(){return value;}
}
