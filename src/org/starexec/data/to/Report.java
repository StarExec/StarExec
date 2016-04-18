package org.starexec.data.to;

/**
 * Class that represents a report in the weekly reports system.
 * Author: Albert Giegerich
 */
public class Report {
	// The name of the event the report is about.
	private final String eventName;
	// The number of times the event occurred.
	private final Integer occurrences;
	/*
	// The id of the queue the report is related to, null if the report is not related to a queue.
	private final Integer queueId;
	*/
	// The name of the queue that the report is related to, null if the report is not related to a queue.
	private final String queueName;

	public Report(String eventName, int occurrences, String queueName) {
		this.eventName = eventName;
		this.occurrences = occurrences;
		//this.queueId = queueId;
		this.queueName = queueName;
	}

	public Report(String eventName, int occurrences) {
		this.eventName = eventName;
		this.occurrences = occurrences;
		//this.queueId = null;
		this.queueName = null;
	}

	/**
	 * Gets the name of the event the report is about.
	 * @return the name of the event the report is a bout
	 * @author Albert Giegerich
	 */
	public String getEventName() {
		return eventName;
	}

	/**
	 * Gets the number of time the report's event occurred.
	 * @return the number times the report's event occurred.
	 * @author Albert Giegerich
	 */
	public int getOccurrences() {
		return occurrences;
	}

	/**
	 * Gets the id of the queue related to the report.
	 * @return the id of the queue related to the report, null if the report is not related to a queue.
	 * @author Albert Giegerich
	public int getQueueId() {
		return queueId;
	}
	*/

	/**
	 * Gets the name of the queue related to the report.
	 * @return the name of the queue related to the report, null if the report is not related to a queue.
	 * @author Albert Giegerich
	 */
	public String getQueueName() {
		return queueName;
	}
}
