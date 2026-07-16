package com.soumyajit.gradlemc.foundation;
import java.util.Objects;
public record TaskId(String value) implements Comparable<TaskId> { public TaskId { if(value==null||!value.matches("[a-z0-9][a-z0-9_.-]*:[a-z0-9][a-z0-9_./-]*")) throw new IllegalArgumentException("Task ID must be lowercase namespace:path using safe characters"); } public static TaskId of(String value){return new TaskId(value);} @Override public int compareTo(TaskId other){return value.compareTo(Objects.requireNonNull(other).value);} @Override public String toString(){return value;} }
