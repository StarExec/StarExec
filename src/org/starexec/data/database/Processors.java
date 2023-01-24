package org.starexec.data.database;

import org.apache.commons.io.FileUtils;
import org.starexec.constants.R;
import org.starexec.data.to.Processor;
import org.starexec.data.to.enums.ProcessorType;
import org.starexec.logger.StarLogger;
import org.starexec.util.Util;

import java.io.File;
import java.lang.NumberFormatException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedList;
import java.util.List;

/**
 * Handles all database interaction for bench, pre and post processors
 */
public class Processors {
	private static final StarLogger log = StarLogger.getLogger(Processors.class);

	/**
	 * Given a result set where the current row points to a  processor, return the processor
	 *
	 * @param results
	 * @param prefix The table alias given to the processor table in this query. Empty means no prefix.
	 * @return The processor if it exists
	 * @throws SQLException If the ResultSet does not contain a required processor attribute
	 */
	public static Processor resultSetToProcessor(ResultSet results, String prefix) throws SQLException {
		if (Util.isNullOrEmpty(prefix)) {
			prefix = "";
		} else {
			prefix = prefix + ".";
		}

		Processor t = new Processor();
		//if the ID is null, 0 is returned here
		t.setId(results.getInt(prefix + "id"));
		t.setCommunityId(results.getInt(prefix + "community"));
		t.setDescription(results.getString(prefix + "description"));
		t.setName(results.getString(prefix + "name"));
		t.setFilePath(results.getString(prefix + "path"));
		t.setDiskSize(results.getLong(prefix + "disk_size"));
		t.setType(ProcessorType.valueOf(results.getInt("processor_type")));
		t.setTimeLimit(results.getInt("time_limit"));
		t.setSyntax(results.getInt("syntax_id"));

		return t;
	}

	private static Processor resultSetToProcessor(ResultSet results) throws SQLException {
		results.next();
		return resultSetToProcessor(results, null);
	}

	/**
	 * @param results
	 * @return a List of Processors from results
	 */
	private static List<Processor> resultSetToProcessors(ResultSet results) throws SQLException {
		List<Processor> processors = new LinkedList<>();
		while (results.next()) {
			processors.add(resultSetToProcessor(results, null));
		}
		return processors;
	}

	/**
	 * Inserts a processor into the database
	 *
	 * @param processor The processor to add to the database
	 * @return The positive integer ID of the new processor if successful, -1 otherwise
	 * @author Tyler Jensen
	 */
	public static int add(Processor processor) {
		try {
			int procId = Common.updateWithOutput("{CALL AddProcessor(?,?,?,?,?,?,?,?)}", procedure -> {
				procedure.setString(1, processor.getName());
				procedure.setString(2, processor.getDescription());
				procedure.setString(3, processor.getFilePath());
				procedure.setInt(4, processor.getCommunityId());
				procedure.setInt(5, processor.getType().getVal());
				procedure.setLong(6, FileUtils.sizeOf(new File(processor.getFilePath())));
				procedure.setInt(7, processor.getTimeLimit());
				procedure.registerOutParameter(8, java.sql.Types.INTEGER);
			}, procedure -> procedure.getInt(8));
			log.debug("the new processor has the ID = " + procId + " and community id = " + processor.getCommunityId
					());
			return procId;
		} catch (SQLException e) {
			log.error(e.getMessage(), e);
		}
		return -1;
	}

	/**
	 * Deletes a given processor from a space
	 *
	 * @param processorId the id of the processor to delete
	 * @return True if the operation was a success, false otherwise
	 * @author Todd Elvers
	 */
	public static boolean delete(int processorId) {
		final String method = "delete";
		String message;

		if (processorId == R.NO_TYPE_PROC_ID) {
			log.debug(method, "Cannot delete 'no type' processor");
			return false; // the no type processor is required for the system
		}
		if (!processorExists(processorId)) {
			log.debug(method, "Cannot find processor id: " + processorId);
			return true;
		}
		try {
			// Get processor_path of processor
			final File processorFile = Common.updateWithOutput("{CALL DeleteProcessor(?,?)}", procedure -> {
				procedure.setInt(1, processorId);
				procedure.registerOutParameter(2, java.sql.Types.LONGNVARCHAR);
			}, procedure -> new File(procedure.getString(2)));
			message = String.format("Removal of processor [id=%d] was successful.", processorId);
			log.debug(method, message);

			// Try and delete file referenced by processor_path and its parent directory
			if (processorFile.exists()) {
				if (processorFile.delete()) {
					message =
							String.format("File [%s] was deleted at [%s] because it was not inter referenced " +
									              "anywhere.", processorFile.getName(), processorFile.getAbsolutePath()
							);
					log.debug(method, message);
				}
				if (processorFile.getParentFile() != null) {
					if (processorFile.getParentFile().delete()) {
						message = String.format("Directory [%s] was deleted because it was empty.",
						                        processorFile.getParentFile().getAbsolutePath()
						);
						log.debug(method, message);
					}
				}
			}
			return true;
		} catch (SQLException e) {
			log.debug(method, String.format("Removal of processor [id=%d] failed.", processorId), e);
		}
		return false;
	}

	/**
	 * @param processorId The id of the bench processor to retrieve
	 * @return The corresponding processor
	 * @author Tyler Jensen
	 */
	public static Processor get(int processorId) {
		Connection con = null;
		try {
			con = Common.getConnection();
			return get(processorId, con);
		} catch (Exception e) {
			log.error("get", e.getMessage(), e);
			return null;
		} finally {
			Common.safeClose(con);
		}
	}

	/**
	 * @param processorId The id of the bench processor to retrieve
	 * @return The corresponding processor
	 * @author Tyler Jensen
	 */
	public static Processor get(int processorId, Connection con) throws NumberFormatException, SQLException {
		if (processorId == 0) {
			return null;
		}
		return Common.queryUsingConnection(con, "{CALL GetProcessorById(?)}", procedure -> procedure.setInt(1, processorId), Processors::resultSetToProcessor);
	}

	/**
	 * Gets the list of processors
	 *
	 * @param type The type of processors to filter by
	 * @return the list of processors
	 * @author Todd Elvers
	 */
	public static List<Processor> getAll(ProcessorType type) {
		try {
			return Common.query("{CALL GetAllProcessors(?)}", procedure -> procedure.setInt(1, type.getVal()), Processors::resultSetToProcessors);
		} catch (SQLException e) {
			log.error("getAll", e.getMessage(), e);
		}
		return null;
	}

	/**
	 * @return the system NoType benchmark processor, which is applied when the user has no processor.
	 */
	public static Processor getNoTypeProcessor() {
		return Processors.get(R.NO_TYPE_PROC_ID);
	}

	/**
	 * @param communityId The id of the community to retrieve all processors for
	 * @param type The type of processors to get for the community
	 * @return A list of all processors of the given type that the community owns
	 * @author Tyler Jensen
	 */
	public static List<Processor> getByCommunity(int communityId, ProcessorType type) {
		try {
			return Common.query("{CALL GetProcessorsByCommunity(?,?)}", procedure -> {
				procedure.setInt(1, communityId);
				procedure.setInt(2, type.getVal());
			}, Processors::resultSetToProcessors);
		} catch (SQLException e) {
			log.error("getByCommunity", e.getMessage(), e);
		}
		return null;
	}

	/**
	 * Gets all processors that a user can see because they share a community
	 *
	 * @param userId the user to retrieve post processors for
	 * @param type The type of processors to get
	 * @return A list of all unique processors of the given type that the user can see
	 * @author Eric Burns
	 */
	public static List<Processor> getByUser(int userId, ProcessorType type) {
		try {
			return Common.query("{CALL GetProcessorsByUser(?,?)}", procedure -> {
				procedure.setInt(1, userId);
				procedure.setInt(2, type.getVal());
			}, Processors::resultSetToProcessors);
		} catch (SQLException e) {
			log.error("getByUser", e.getMessage(), e);
		}
		return null;
	}

	/**
	 * Updates the description of a processor with the given processor id
	 *
	 * @param processorId the id of the processor to update
	 * @param newDesc the new description to update the processor with
	 * @return True if the operation was a success, false otherwise
	 * @author Tyler Jensen
	 */
	public static boolean updateDescription(int processorId, String newDesc) {
		try {
			Common.update("{CALL UpdateProcessorDescription(?,?)}", procedure -> {
				procedure.setInt(1, processorId);
				procedure.setString(2, newDesc);
			});
			return true;
		} catch (SQLException e) {
			log.error("updateDescription", e.getMessage(), e);
		}
		return false;
	}

	/**
	 * Makes sure that a processor with the given id exists.
	 *
	 * @param processorId The id of a processor.
	 * @return true if the the processor exists, otherwise false.
	 * @author Albert Giegerich
	 */
	public static boolean processorExists(int processorId) {
		Processor processor = Processors.get(processorId);
		return (processor != null);
	}

	/**
	 * Updates the file path of a processor with the given processor id
	 *
	 * @param processorId the id of the processor to update
	 * @param newPath the new path to the directory containing this processor
	 * @return True if the operation was a success, false otherwise
	 * @author Eric Burns
	 */
	public static boolean updateFilePath(int processorId, String newPath) {
		try {
			Common.update("{CALL UpdateProcessorFilePath(?,?)}", procedure -> {
				procedure.setInt(1, processorId);
				procedure.setString(2, newPath);
			});
			return true;
		} catch (SQLException e) {
			log.error("updateFilePath", e.getMessage(), e);
		}
		return false;
	}

	/**
	 * Updates the name of a processor with the given processor id
	 *
	 * @param processorId the id of the processor to update
	 * @param newName the new name to update the processor with
	 * @return True if the operation was a success, false otherwise
	 * @author Tyler Jensen
	 */
	public static boolean updateName(int processorId, String newName) {
		try {
			Common.update("{CALL UpdateProcessorName(?,?)}", procedure -> {
				procedure.setInt(1, processorId);
				procedure.setString(2, newName);
			});
			return true;
		} catch (SQLException e) {
			log.error("updateName", e.getMessage(), e);
		}
		return false;
	}

	public static boolean updateTimeLimit(int processorId, int timeLimit) {
		try {
			Common.update("{CALL UpdateProcessorTimeLimit(?,?)}", procedure -> {
				procedure.setInt(1, processorId);
				procedure.setInt(2, timeLimit);
			});
			return true;
		} catch (SQLException e) {
			log.error("updateTimeLimit", e.getMessage(), e);
		}
		return false;
	}

	public static void updateSyntax(int processorId, int syntaxId) throws SQLException {
		Common.update(
			"{CALL UpdateProcessorSyntax(?,?)}",
			procedure -> {
				procedure.setInt(1, processorId);
				procedure.setInt(2, syntaxId);
			}
		);
	}
}
