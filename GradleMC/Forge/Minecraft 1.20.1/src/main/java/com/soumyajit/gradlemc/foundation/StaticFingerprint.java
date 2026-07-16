package com.soumyajit.gradlemc.foundation;
import java.util.*;
public record StaticFingerprint(int schemaVersion,String digest) { public StaticFingerprint {if(schemaVersion<1||digest==null||!digest.matches("[0-9a-f]{64}"))throw new IllegalArgumentException("Invalid static fingerprint");} public String shortDisplay(){return digest.substring(0,12);} public static StaticFingerprint of(int schema,Map<String,String> input){return new StaticFingerprint(schema,Fingerprints.digest("static/"+schema,input));} }
