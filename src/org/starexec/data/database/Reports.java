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
	 * Add occurrences to an event for the report system.
	 * @param eventName The name of the event.
	 * @param occurrences The number of times the event occurred.
	 * @author Albert Giegerich
	 */
	public static void addToEventOccurrencesNotRelatedToQueue(String eventName, int occurrences) {
		Connection con = null;
		CallableStatement procedure = null;
		ResultSet results = null;
		try {
			con = Common.getConnection();
			
			procedure = con.prepareCall("{CALL AddToEventOccurrences(?, ?)}");
			procedure.setString(1, eventName);
			procedure.setInt(2, occurrences);

			procedure.executeQuery();
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		} finally {
			Common.safeClose(con);
		}
	}

	/**
	 * Get the number of times an event has occurred not related to a queue.
	 * @param eventName the name of the event.
	 * @return the number of times the event occurred
	 * @author Albert Giegerich
	 */
	public static Integer getEventOccurrencesRelatedToQueue(String eventName) {
		Connection con = null;
		CallableStatement procedure = null;
		ResultSet results = null;
		try {
			con = Common.getConnection();

			procedure = con.prepareCall("{CALL GetEventOccurrencesNotRelatedToQueue(?)}");
			procedure.setString(1, eventName);

			results = procedure.executeQuery();
			results.first();

			int occurrences = results.getInt("occurrences");

			return occurrences;
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		} finally {
			Common.safeClose(con);
		}
		return null;
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
		}
		return null;
	}

	/**
	 * 
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
