package org.starexec.data.database;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.ResultSet;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;

import org.starexec.data.to.Report;


/**
 * Handles database interaction for the weekly reports. 
 * @author Albert Giegerich
 */ 
public class Reports {
	private static final Logger log = Logger.getLogger(Reports.class);


	/**
	 * Set the number of occurrences for an event not related to a queue.
	 * @param eventName the name of the event.
	 * @param occurrences the number of times the event occurred.
	 * @author Albert Giegerich
	 */
	public static void setEventOccurrencesNotRelatedToQueue(String eventName, int occurrences) {
		setEventOccurrences(eventName, occurrences, null);
	}

	/**
	 * Set the number of occurrences for an event related to a queue.
	 * @param eventName the name of the event.
	 * @param occurrences the number of times the event occurred.
	 * @param queueName the name of the queue related to the event.
	 * @author Albert Giegerich
	 */
	public static void setEventOccurrencesForQueue(String eventName, int occurrences, String queueName) {
		setEventOccurrences(eventName, occurrences, queueName);
	}

	/**
	 * Add occurrences to an event not related to a queue.
	 * @param eventName the name of the event.
	 * @param occurrences the number of times the event occurred.
	 * @author Albert Giegerich
	 */
	public static void addToEventOccurrencesNotRelatedToQueue(String eventName, int occurrences) {
		addToEventOccurrences(eventName, occurrences, null);
	}

	/**
	 * Add occurrences to an event related to a queue
	 * @param eventName the name of the event.
	 * @param occurrences the number of times the event occurred.
	 * @param queueName the name of the queue related to the event.
	 */
	public static void addToEventOccurrencesForQueue(String eventName, int occurrences, String queueName) {
		addToEventOccurrences(eventName, occurrences, queueName);
	}


	/**
	 * Get the number of times an event has occurred.
	 * @param eventName the name of the event.
	 * @return the number of times the event has occurred.
	 * @author Albert Giegerihc
	 */
	public static Integer getEventOccurrencesNotRelatedToQueue(String eventName) {
		return getEventOccurrences(eventName, null);
	}

	/**
	 * Get the number of times an event has occurred not related to a queue.
	 * @param eventName the name of the event.
	 * @param queueName the name of the queue the event is related to.
	 * @return the number of times the event occurred
	 * @author Albert Giegerich
	 */
	public static Integer getEventOccurrencesForQueue(String eventName, String queueName) {
		return getEventOccurrences(eventName, queueName);
	}



	/**
	 * Gets every event and the number of times it occurred for events that are not related to a queue.
	 * @return a list of ImmutablePairs representing and event and the number of times it occurred.
	 * @author Albert Giegerich
	 */
	public static List<Report> getAllReportsNotRelatedToQueues() {
		List<Report> reports = new LinkedList<Report>();
		Connection con = null;
		CallableStatement procedure = null;
		ResultSet results = null;

		try {
			con = Common.getConnection();
			procedure = con.prepareCall("{CALL GetAllEventsAndOccurrencesNotRelatedToQueues()}");
			results = procedure.executeQuery();

			while (results.next()) {
				String event = results.getString("event_name");
				Integer occurrences = results.getInt("occurrences");
				Report report = new Report(event, occurrences);
				reports.add(report);
			}
			return reports;
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		} finally {
			Common.safeClose(con);
			Common.safeClose(procedure);
			Common.safeClose(results);
		}
		return null;
	}

	/**
	 * Get every event, the number of times it occurred, and the queue it occurred on.
	 * @return  a list of Reports representing the event, the number of times it occurred, and which queue it occurred on.
	 * @author Albert Giegerich
	 */
	public static List<List<Report>> getAllReportsForAllQueues() {
		LinkedList<Report> reportsForAllQueues = new LinkedList<Report>();
		Connection con = null;
		CallableStatement procedure = null;
		ResultSet results = null;

		try {
			con = Common.getConnection();
			procedure = con.prepareCall("{CALL GetAllEventsAndOccurrencesForAllQueues()}");
			results = procedure.executeQuery();

			while (results.next()) {
				String event = results.getString("event_name");
				Integer occurrences = results.getInt("occurrences");
				Integer queueId = results.getInt("queue_id");
				String queueName = results.getString("name");

				Report report = new Report(event, occurrences, queueId, queueName); 

				reportsForAllQueues.add(report);
			}

			List<List<Report>> reportsByQueue = seperateReportsByQueue(reportsForAllQueues);

			return reportsByQueue;
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		} finally {
			Common.safeClose(con);
			Common.safeClose(procedure);
			Common.safeClose(results);
		}
		return null;
	}

	/**
	 * Resets all report data by setting all occurrences to 0 and deleting any rows related to queues.
	 * @author Albert Giegerich
	 */
	public static void resetReports() {
		Connection con = null;
		CallableStatement procedure = null;
		try {
		    con = Common.getConnection();
		    procedure = con.prepareCall("{CALL ResetReports()}");
		    procedure.executeQuery();
		} catch (Exception e) {
		    log.error(e.getMessage(), e);
		} finally {
		    Common.safeClose(con);
		    Common.safeClose(procedure);
		}
	}

	/**
	 * Inner method that sets the number of event occurrences not related to a queue if queueName is null
	 * otherwise sets the number of event occurrences related to the specified queue.
	 * @param eventName the name of the event to set the number of occurrences for
	 * @param occurrences the number of times the event occurred
	 * @param queueName the name of the queue for which the event occurred. Null the event is unrelated to a queue.
	 * @author Albert Giegerich
	 */
	private static void setEventOccurrences(String eventName, int occurrences, String queueName) {
		Connection con = null;
		CallableStatement procedure = null;

		try {
			con = Common.getConnection();
			if (queueName == null) {
				procedure = con.prepareCall("{CALL SetEventOccurrencesNotRelatedToQueue(?, ?)}");
			} else {
				procedure = con.prepareCall("{CALL SetEventOccurrencesForQueue(?, ?, ?)}");
				procedure.setString(3, queueName);
			}
			procedure.setString(1, eventName);
			procedure.setInt(2, occurrences);
			procedure.executeQuery();
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		} finally {
			Common.safeClose(con);
			Common.safeClose(procedure);
		}
	}

	/**
	 * Inner method that adds to the number of event occurrences not related to a queue
	 * if queueName is null, otherwise adds to the number of event occurrences related to
	 * the specified queue.
	 * @param eventName the name of the event
	 * @param occurrences the number of times the event occurred
	 * @param queueName the name of the queue the event is related to. Null if not related to a queue.
	 * @author Albert Giegerich
	 */
	private static void addToEventOccurrences(String eventName, int occurrences, String queueName) {
		Connection con = null;
		CallableStatement procedure = null;
		try {
			con = Common.getConnection();
			
			if (queueName == null) {
				procedure = con.prepareCall("{CALL AddToEventOccurrencesNotRelatedToQueue(?, ?)}");
			} else {
				procedure = con.prepareCall("{CALL AddToEventOccurrencesForQueue(?, ?, ?)}");
				procedure.setString(3, queueName);
			}
			procedure.setString(1, eventName);
			procedure.setInt(2, occurrences);

			procedure.executeQuery();
			log.debug("Added " + occurrences + " occurrences to " + eventName + (queueName == null ? "" : " for queue " + queueName) + ".");
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		} finally {
			Common.safeClose(con);
			Common.safeClose(procedure);
		}
	}

	/**
	 * Inner method that gets the number of event occurrences not related to a queue
	 * if queueName is null, otherwise gets the number of event occurrences related to
	 * the specified queue.
	 * @param eventName the name of the event.
	 * @param queueName the name of the queue the event is related to. Null if not related to a queue.
	 * @return the number of times the event occurred.
	 * @author Albert Giegerich
	 */
	private static Integer getEventOccurrences(String eventName, String queueName) {
		Connection con = null;
		CallableStatement procedure = null;
		ResultSet results = null;
		try {
			con = Common.getConnection();

			if (queueName == null) {
				procedure = con.prepareCall("{CALL GetEventOccurrencesNotRelatedToQueue(?)}");
			} else {
				procedure = con.prepareCall("{CALL GetEventOccurrencesForQueue(?, ?)}");
				procedure.setString(2, queueName);
			}
			procedure.setString(1, eventName);

			results = procedure.executeQuery();
			results.first();

			int occurrences = results.getInt("occurrences");

			return occurrences;
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		} finally {
			Common.safeClose(con);
			Common.safeClose(procedure);
			Common.safeClose(results);
		}
		return null;
	}

	/**
	 * Turns a list of reports related to queues into a list of lists related to queues where 
	 * each inner list is made up of reports related to a different queue.
	 * @param reports a list of reports related to queues
	 * @author Albert Giegerich
	 */
	private static List<List<Report>> seperateReportsByQueue(List<Report> reports) {
		// Build a map that seperates all the reports into lists based on which queue they're related to.
		Map<String,List<Report>> reportMap = new HashMap<String,List<Report>>();
		for (Report report : reports) {
			String queueName = report.getQueueName();
			if (reportMap.containsKey(queueName)) {
				reportMap.get(queueName).add(report);	
			} else {
				List<Report> reportsRelatedToQueue = new LinkedList<Report>(); 
				reportsRelatedToQueue.add(report);
				reportMap.put(queueName, reportsRelatedToQueue);
			}
		}

		// Use the map to build a list of lists where each inner list contains all the reports related to a single queue.
		List<List<Report>> reportsSeperatedByQueue = new LinkedList<List<Report>>();
		Set<String> keys = reportMap.keySet();
		for (String key : keys) {
			reportsSeperatedByQueue.add(reportMap.get(key));
		}

		return reportsSeperatedByQueue;
	}
}
