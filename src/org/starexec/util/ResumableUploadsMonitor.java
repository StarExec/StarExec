package org.starexec.util;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import org.starexec.util.Timer;
import org.starexec.logger.StarLogger;
import org.starexec.data.database.Benchmarks;
import org.starexec.data.database.Uploads;
import org.starexec.data.to.BenchmarkUploadStatus;
import org.starexec.servlets.UploadBenchmark;
import org.starexec.util.Util;

public class ResumableUploadsMonitor extends RobustRunnable { // rename resumable uploads monitor
  public ResumableUploadsMonitor(String name) {
    super(name);
  }

  public static HashMap<Integer, Integer> uploadIds = new HashMap<Integer, Integer>();

  @Override
  protected void dorun() {
    processResumableBenchmarks();
  }

  public static void processResumableBenchmarks() {
    List<BenchmarkUploadStatus> benchmarksToProcess = Uploads.getResumableBenchmarkUploads();
    Iterator<BenchmarkUploadStatus> b = benchmarksToProcess.iterator();
    HashMap<Integer, Integer> runningIds = getUploadIds();
    log.debug("running Ids: " + runningIds.toString());

    while (b.hasNext()) {
      BenchmarkUploadStatus benchmarkUpload = b.next();
      if (!uploadIdRunning(benchmarkUpload.getId())) {
        Util.threadPoolExecute(() -> {
          try {
            addThread(benchmarkUpload.getId());
            log.info("New thread created to process resumable benchmark upload with ID: " + benchmarkUpload.getId());
            File directory = new File(benchmarkUpload.getPath());
            if (!benchmarkUpload.isFileExtractionComplete()) {
              UploadBenchmark.extractAndProcess(benchmarkUpload.getUserId(), benchmarkUpload.getSpaceId(),
                  benchmarkUpload.getTypeId(),
                  benchmarkUpload.getDownloadable(), benchmarkUpload.getPermission(), benchmarkUpload.getUploadMethod(),
                  benchmarkUpload.getId(),
                  benchmarkUpload.getHasDependencies(), benchmarkUpload.getLinked(), benchmarkUpload.getSpaceId(),
                  benchmarkUpload.getResumable(), new File(benchmarkUpload.getPath()));
            } else {
              UploadBenchmark.process(benchmarkUpload.getUserId(),
                  benchmarkUpload.getSpaceId(), benchmarkUpload.getTypeId(),
                  benchmarkUpload.getDownloadable(), benchmarkUpload.getPermission(),
                  benchmarkUpload.getUploadMethod(), benchmarkUpload.getId(),
                  benchmarkUpload.getHasDependencies(), benchmarkUpload.getLinked(),
                  benchmarkUpload.getSpaceId(), benchmarkUpload.getResumable(), directory,
                  Benchmarks.buildCompleteSpace(directory, benchmarkUpload.getTypeId(), benchmarkUpload.getUserId(),
                      benchmarkUpload.getDownloadable(), benchmarkUpload.getPermission(), benchmarkUpload.getId()), false);
            }
            Uploads.benchmarkEverythingComplete(benchmarkUpload.getId());
            log.info("Processed resumable benchmark with id: " + benchmarkUpload.getId());
            uploadIdFinished(benchmarkUpload.getId());
          } catch (Exception e) {
            log.error("Could not process a resumable benchmark upload", e);
            // remove from list
            uploadIdFinished(benchmarkUpload.getId());
          }
        });
      } else {
        log.info(
            "Encountered a resumable benchmark upload that is already processing with ID: " + benchmarkUpload.getId());
      }
    }
  }

  public static HashMap<Integer, Integer> getUploadIds() {
    return uploadIds;
  }

  public static void addThread(Integer uploadId) {
    // code to see how many to skip is put here
    Integer benchmarksToSkip = Uploads.getBenchmarksToSkip(uploadId);
    uploadIds.put(uploadId, benchmarksToSkip);
    log.debug("Thread running to process job with id: " + uploadId);
  }

  public static Boolean uploadIdRunning(Integer uploadId) {
    return uploadIds.containsKey(uploadId);
  }

  public static void uploadIdFinished(Integer uploadId) {
    uploadIds.remove(Integer.valueOf(uploadId));
    log.debug("Thread finished processing job with id: " + uploadId);
  }

  public static Boolean doneSkippingBenchmarks(Integer uploadId) {
    if(!uploadIds.containsKey(uploadId)){
      return true;
    }
    if (uploadIds.get(uploadId) == 0) {
      return true;
    } else {
      uploadIds.put(uploadId, uploadIds.get(uploadId) - 1);
      return false;
    }
  }
}
