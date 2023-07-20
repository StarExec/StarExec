package org.starexec.util;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.starexec.util.Timer;
import org.starexec.logger.StarLogger;
import org.starexec.data.database.Uploads;
import org.starexec.data.to.BenchmarkUploadStatus;
import org.starexec.servlets.UploadBenchmark;
import org.starexec.util.Util;


public class ResumableUploadsMonitor extends RobustRunnable { // rename resumable uploads monitor
  public ResumableUploadsMonitor(String name){
    super(name);
  }

  public static ArrayList<Integer> uploadIds = new ArrayList<Integer>();

  @Override
  protected void dorun() {
    log.debug("DANNY: Calling from periodic task");
    processResumableBenchmarks();
  }
  
  public static void processResumableBenchmarks(){
    log.debug("DANNY: Start of process resumable benchmarks method");
    List<BenchmarkUploadStatus> benchmarksToProcess = Uploads.getResumableBenchmarkUploads();
    Iterator<BenchmarkUploadStatus> b = benchmarksToProcess.iterator();
    ArrayList<Integer> runningIds = getUploadIds();
    log.debug("DANNY: running Ids: " + runningIds.toString());
  
    while (b.hasNext()) {
      BenchmarkUploadStatus benchmarkUpload = b.next();
      log.debug("DANNY: start processing?: " + (!runningIds.contains(benchmarkUpload.getId())));
      if (!runningIds.contains(benchmarkUpload.getId())) {
        Util.threadPoolExecute(() -> {
          try {
            addThread(benchmarkUpload.getId());
            log.info("New thread created to process resumable benchmark upload with ID: " + benchmarkUpload.getId());
            if (!benchmarkUpload.isFileExtractionComplete()) {
              UploadBenchmark.extractAndProcess(benchmarkUpload.getUserId(), benchmarkUpload.getSpaceId(),
                  benchmarkUpload.getTypeId(),
                  benchmarkUpload.getDownloadable(), benchmarkUpload.getPermission(), benchmarkUpload.getUploadMethod(),
                  benchmarkUpload.getId(),
                  benchmarkUpload.getHasDependencies(), benchmarkUpload.getLinked(), benchmarkUpload.getSpaceId(),
                  benchmarkUpload.getResumable(), new File(benchmarkUpload.getPath()));
            }
            // else if(!benchmarkUpload.isProcessingBegun()){
            // UploadBenchmark.process(benchmarkUpload.getUserId(),
            // benchmarkUpload.getSpaceId(), benchmarkUpload.getTypeId(),
            // benchmarkUpload.getDownloadable(), benchmarkUpload.getPermission(),
            // benchmarkUpload.getUploadMethod(), benchmarkUpload.getId(),
            // benchmarkUpload.getHasDependencies(), benchmarkUpload.getLinked(),
            // benchmarkUpload.getSpaceId(), benchmarkUpload.getResumable(), new
            // File(benchmarkUpload.getPath()), );
            // }
            Uploads.benchmarkEverythingComplete(benchmarkUpload.getId());
            log.info("Processed resumable benchmark with id: " + benchmarkUpload.getId());
            uploadIdFinished(benchmarkUpload.getId());
          } catch (Exception e) {
            log.error("Could not process a resumable benchmark upload", e);
          }
        });
      } else {
        log.info(
            "Encountered a resumable benchmark upload that is already processing with ID: " + benchmarkUpload.getId());
      }
    }
  }

  public static ArrayList<Integer> getUploadIds() {
    return uploadIds;
  }

  public static void addThread(Integer uploadId) {
    uploadIds.add(uploadId);
    log.debug("DANNY: Thread running to process job with id: " + uploadId);
  }

  public static Boolean uploadIdRunning(Integer uploadId) {
    return uploadIds.contains(uploadId);
  }

  public static void uploadIdFinished(Integer uploadId) {
    uploadIds.remove(Integer.valueOf(uploadId));
    log.debug("DANNY: Thread finished processing job with id: " + uploadId);
  }
}
