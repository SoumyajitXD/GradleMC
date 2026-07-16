package com.soumyajit.gradlemc.storage;
import java.util.List;
public record StorageSummary(long gameUsableBytes,long outputUsableBytes,boolean reportsWritable,long reportBytes,int reportFiles,List<String>oversizedReports,List<String>staleTemporaryFiles,List<String>limitations){public StorageSummary{oversizedReports=List.copyOf(oversizedReports);staleTemporaryFiles=List.copyOf(staleTemporaryFiles);limitations=List.copyOf(limitations);}}
