/**
 * 
 */
package org.starexec.data.to;

import java.sql.Timestamp;

/**
 * 
 *	Object storing the status of a space XML upload
 *
 * @author Eric Burns
 *
 */
public class SpaceXMLUploadStatus extends Identifiable {
	private int userId;
	private Timestamp uploadDate;
	private boolean fileUploadComplete;
	private boolean everythingComplete;
	private int totalSpaces;
	private int completedSpaces;
	private int totalBenchmarks;
	private int completedBenchmarks;
	private int totalSolvers;
	private int completedSolvers;
	private int totalUpdates;
	private int completedUpdates;
	private String errorMessage;
	
	/**
	 * @return the userId
	 */
	public int getUserId() {
		return userId;
	}
	/**
	 * @param userId the userId to set
	 */
	public void setUserId(int userId) {
		this.userId = userId;
	}
	/**
	 * @return the uploadDate
	 */
	public Timestamp getUploadDate() {
		return uploadDate;
	}
	/**
	 * @param uploadDate the uploadDate to set
	 */
	public void setUploadDate(Timestamp uploadDate) {
		this.uploadDate = uploadDate;
	}
	/**
	 * @return the fileUploadComplete
	 */
	public boolean isFileUploadComplete() {
		return fileUploadComplete;
	}
	/**
	 * @param fileUploadComplete the fileUploadComplete to set
	 */
	public void setFileUploadComplete(boolean fileUploadComplete) {
		this.fileUploadComplete = fileUploadComplete;
	}
	
	/**
	 * @return the totalSpaces
	 */
	public int getTotalSpaces() {
		return totalSpaces;
	}
	/**
	 * @param totalSpaces the totalSpaces to set
	 */
	public void setTotalSpaces(int totalSpaces) {
		this.totalSpaces = totalSpaces;
	}
	/**
	 * @return the completedSpaces
	 */
	public int getCompletedSpaces() {
		return completedSpaces;
	}
	/**
	 * @param completedSpaces the completedSpaces to set
	 */
	public void setCompletedSpaces(int completedSpaces) {
		this.completedSpaces = completedSpaces;
	}
	/**
	 * @return the totalBenchmarks
	 */
	public int getTotalBenchmarks() {
		return totalBenchmarks;
	}
	/**
	 * @param totalBenchmarks the totalBenchmarks to set
	 */
	public void setTotalBenchmarks(int totalBenchmarks) {
		this.totalBenchmarks = totalBenchmarks;
	}
	/**
	 * @return the completedBenchmarks
	 */
	public int getCompletedBenchmarks() {
		return completedBenchmarks;
	}
	/**
	 * @param completedBenchmarks the completedBenchmarks to set
	 */
	public void setCompletedBenchmarks(int completedBenchmarks) {
		this.completedBenchmarks = completedBenchmarks;
	}
	/**
	 * @return the everythingComplete
	 */
	public boolean isEverythingComplete() {
		return everythingComplete;
	}
	/**
	 * @param everythingComplete the everythingComplete to set
	 */
	public void setEverythingComplete(boolean everythingComplete) {
		this.everythingComplete = everythingComplete;
	}

	/**
	 * @return the errorMessage
	 */
	public String getErrorMessage() {
		return errorMessage;
	}
	/**
	 * @param errorMessage the errorMessage to set
	 */
	public void setErrorMessage(String errorMessage) {
		this.errorMessage = errorMessage;
	}
	public int getTotalSolvers() {
		return totalSolvers;
	}
	public void setTotalSolvers(int totalSolvers) {
		this.totalSolvers = totalSolvers;
	}
	public int getCompletedSolvers() {
		return completedSolvers;
	}
	public void setCompletedSolvers(int completedSolvers) {
		this.completedSolvers = completedSolvers;
	}
	public int getTotalUpdates() {
		return totalUpdates;
	}
	public void setTotalUpdates(int totalUpdates) {
		this.totalUpdates = totalUpdates;
	}
	public int getCompletedUpdates() {
		return completedUpdates;
	}
	public void setCompletedUpdates(int completedUpdates) {
		this.completedUpdates = completedUpdates;
	}
	
}



