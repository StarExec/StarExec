package org.starexec.util;

import java.util.ArrayList;
import org.starexec.util.Timer;
import org.starexec.logger.StarLogger;

public abstract class ResumableRobustRunnable implements Runnable {
  private static final StarLogger log = StarLogger.getLogger(ResumableRobustRunnable.class);

  protected final String name;
  private ArrayList <Integer> uploadIds = new ArrayList<Integer>();
  
  abstract protected void dorun();
  
  public ResumableRobustRunnable(String _name) {
		name = _name;
	}

  public ArrayList getUploadIds(){
    return uploadIds;
  }

  public void addThread(Integer uploadId){
    uploadIds.add(uploadId);
    log.debug("DANNY: Thread running to process job with id: " + uploadId);
  }

  public Boolean uploadIdRunning(Integer uploadId){
    return uploadIds.contains(uploadId);
  }

  public void uploadIdFinished(Integer uploadId){
    uploadIds.remove(Integer.valueOf(uploadId));
    log.debug("DANNY: Thread finished processing job with id: " + uploadId);
  }
  
  @Override
	public void run() {
		Timer timer = new Timer();
		try {
			log.info(name + " (periodic)");
			dorun();
		} catch (Throwable e) {
			log.warn(name + " caught throwable: " + e, e);
		} finally {
			log.info(name + " completed one periodic execution in " + timer.getTime() + " milliseconds.");
		}
	}
}
