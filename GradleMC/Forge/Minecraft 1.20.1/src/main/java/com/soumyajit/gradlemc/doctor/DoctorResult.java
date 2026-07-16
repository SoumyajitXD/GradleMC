package com.soumyajit.gradlemc.doctor;
public record DoctorResult(String id,State state,String detail){public enum State{PASS,WARN,FAIL}}
