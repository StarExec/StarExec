package org.starexec.data.to;

import java.sql.Timestamp;

/**
 * Object storing the status of a benchmark upload.
 *
 * @author Benton McCune
 */
public class BenchmarkUploadStatus extends Identifiable {
	private int spaceId;
	private int userId;
	private Timestamp uploadDate;
	private boolean fileUploadComplete;
	private boolean fileExtractionComplete;
	private boolean processingBegun;
	private boolean everythingComplete;
	private int totalSpaces;
	private int completedSpaces;
	private int totalBenchmarks;
	private int validatedBenchmarks;
	private int completedBenchmarks;
	private int failedBenchmarks;
	private String errorMessage;
    

        public BenchmarkUploadStatus() {
        }

	/**
	 * @return the spaceId
	 */
	public int getSpaceId() {
		return spaceId;
	}

	/**
	 * @param spaceId the spaceId to set
	 */
	public void setSpaceId(int spaceId) {
		this.spaceId = spaceId;
	}

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
	 * @return the fileExtractionComplete
	 */
	public boolean isFileExtractionComplete() {
		return fileExtractionComplete;
	}

	/**
	 * @param fileExtractionComplete the fileExtractionComplete to set
	 */
	public void setFileExtractionComplete(boolean fileExtractionComplete) {
		this.fileExtractionComplete = fileExtractionComplete;
	}

	/**
	 * @return the processingBegun
	 */
	public boolean isProcessingBegun() {
		return processingBegun;
	}

	/**
	 * @param processingBegun the processingBegun to set
	 */
	public void setProcessingBegun(boolean processingBegun) {
		this.processingBegun = processingBegun;
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
	 * @return the validatedBenchmarks
	 */
	public int getValidatedBenchmarks() {
		return validatedBenchmarks;
	}

	/**
	 * @param validatedBenchmarks the validatedBenchmarks to set
	 */
	public void setValidatedBenchmarks(int validatedBenchmarks) {
		this.validatedBenchmarks = validatedBenchmarks;
	}

	/**
	 * @return the failedBenchmarks
	 */
	public int getFailedBenchmarks() {
		return failedBenchmarks;
	}

	/**
	 * @param failedBenchmarks the failedBenchmarks to set
	 */
	public void setFailedBenchmarks(int failedBenchmarks) {
		this.failedBenchmarks = failedBenchmarks;
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
}



