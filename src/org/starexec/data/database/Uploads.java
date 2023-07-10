package org.starexec.data.database;

import org.starexec.data.to.Benchmark;
import org.starexec.data.to.BenchmarkUploadStatus;
import org.starexec.data.to.SpaceXMLUploadStatus;
import org.starexec.logger.StarLogger;
import org.starexec.constants.PaginationQueries;
import org.starexec.constants.R;
import org.starexec.util.DataTablesQuery;
import org.starexec.util.NamedParameterStatement;
import org.starexec.util.PaginationQueryBuilder;
import org.starexec.util.Util;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedList;
import java.util.List;

/**
 * Handles database interaction for the uploading Benchmarks Status Page.
 */
public class Uploads {
	private static final StarLogger log = StarLogger.getLogger(Uploads.class);

	/**
	 * Adds failed benchmark name to db
	 *
	 * @param statusId - id of status object being changed
	 * @param name
	 * @param errorMessage The string message explaining why this benchmark could not be validated
	 * @return true if successful, false if not
	 */
	public static Boolean addFailedBenchmark(Integer statusId, String name, String errorMessage) {
		if (statusId == null) {
			return false;
		}
		try {
			Common.update("{CALL AddUnvalidatedBenchmark(?,?, ?)}",
					procedure -> {
						procedure.setInt(1, statusId);
						procedure.setString(2, name);
						procedure.setString(3, errorMessage);
					}
			);
			return true;
		} catch (Exception e) {
			log.error("addFailedBenchmark", e);
		}
		return false;
	}

	/**
	 * Creates object representing the current status of a user's upload of benchmarks
	 *
	 * @param spaceId - the id of the parent space that benchmarks are being uploaded to
	 * @param userId - id of the user uploading benchmarks
	 * @return the id of the UploadStatus object
	 * @author Benton McCune
	 */
	public static Integer createBenchmarkUploadStatus(Integer spaceId, Integer userId) {

		Connection con = null;
		CallableStatement procedure = null;
		try {
			con = Common.getConnection();

			procedure = con.prepareCall("{CALL CreateBenchmarkUploadStatus(?, ?, ?)}");

			procedure.setInt(1, spaceId);
			procedure.setInt(2, userId);
			procedure.registerOutParameter(3, java.sql.Types.INTEGER);
			procedure.executeUpdate();
			return procedure.getInt(3);
		} catch (Exception e) {
			log.error(e.getMessage(), e);
			return -1;
		} finally {
			Common.safeClose(con);
			Common.safeClose(procedure);
		}
	}

	/**
	 * Creates object representing the current status of a user's upload of benchmarks
	 *
	 * @param userId - id of the user uploading benchmarks
	 * @return the id of the UploadStatus object
	 * @author Benton McCune
	 */
	public static int createSpaceXMLUploadStatus(Integer userId) {

		Connection con = null;
		CallableStatement procedure = null;
		try {
			con = Common.getConnection();

			procedure = con.prepareCall("{CALL CreateSpaceXMLUploadStatus(?, ?)}");

			procedure.setInt(1, userId);
			procedure.registerOutParameter(2, java.sql.Types.INTEGER);
			procedure.executeUpdate();
			return procedure.getInt(2);
		} catch (Exception e) {
			log.error(e.getMessage(), e);
			return -1;
		} finally {
			Common.safeClose(con);
			Common.safeClose(procedure);
		}
	}

	/**
	 * Sets the 'everything complete' flag to true for the XML upload entry with the given ID
	 *
	 * @param statusId
	 * @return True on success and false otherwise, including if the given statusId is null
	 */
	public static boolean XMLEverythingComplete(Integer statusId) {
		if (statusId == null) {
			return false;
		}
		try {
			Common.update("{CALL XMLEverythingComplete(?)}",
					procedure -> {
						procedure.setInt(1, statusId);
					}
			);
			return true;
		} catch (Exception e) {
			log.error("XMLEverythingComplete", e);
		}
		return false;
	}

	/**
	 * Indicates that the entire benchmark upload process is complete.  This is triggered even if the upload failed for
	 * some reason.
	 *
	 * @param statusId
	 * @return true if successful, false if not
	 */
	public static Boolean benchmarkEverythingComplete(Integer statusId) {
		if (statusId == null) {
			return false;
		}
		try {
			Common.update("{CALL BenchmarkEverythingComplete(?)}",
					procedure -> {
						procedure.setInt(1, statusId);
					}
			);
			return true;
		} catch (Exception e) {
			log.error("benchmarkEverythingComplete", e);
		}
		return false;
	}

	/**
	 * The archived file has been successfully extracted.
	 *
	 * @param statusId
	 * @return true if successful, false if not
	 */
	public static Boolean fileExtractComplete(Integer statusId) {
		if (statusId == null) {
			return false;
		}
		try {
			Common.update("{CALL FileExtractComplete(?)}",
					procedure -> {
						procedure.setInt(1, statusId);
					}
			);
			return true;
		} catch (Exception e) {
			log.error("fileExtractComplete", e);
		}
		return false;
	}

	/**
	 * Informs the database that the archive file has been uploaded and is in the file system.
	 *
	 * @param statusId - id of uploadStatus object
	 * @return true if successful, false if not
	 */
	public static Boolean XMLFileUploadComplete(Integer statusId) {
		if (statusId == null) {
			return false;
		}
		try {
			Common.update("{CALL XMLFileUploadComplete(?)}",
					procedure -> {
						procedure.setInt(1, statusId);
					}
			);
			return true;
		} catch (Exception e) {
			log.error("XMLFileUploadComplete", e);
		}
		return false;
	}

	/**
	 * Informs the database that the archive file has been uploaded and is in the file system.
	 *
	 * @param statusId - id of uploadStatus object
	 * @return true if successful, false if not
	 */
	public static Boolean benchmarkFileUploadComplete(Integer statusId) {
		if (statusId == null) {
			return false;
		}
		try {
			Common.update("{CALL BenchmarkFileUploadComplete(?)}",
					procedure -> {
						procedure.setInt(1, statusId);
					}
			);
			return true;
		} catch (Exception e) {
			log.error("benchmarkFileUploadComplete", e);
		}
		return false;
	}

	/**
	 * Gets a string summary of an upload status, suitable for printing out and displaying to users (used in
	 * StarexeCommand)
	 *
	 * @param statusId
	 * @return The string summary, or null on error
	 */
	public static String getUploadStatusSummary(int statusId) {
		try {
			BenchmarkUploadStatus status = getBenchmarkStatus(statusId);
			StringBuilder sb = new StringBuilder();
			sb.append("benchmarks: ");
			sb.append(status.getValidatedBenchmarks());
			sb.append(" / ");
			sb.append(status.getFailedBenchmarks());
			sb.append(" / ");
			sb.append(status.getTotalBenchmarks());
			sb.append(" | ");
			sb.append("spaces: ");
			sb.append(status.getCompletedSpaces());
			sb.append(" / ");
			sb.append(status.getTotalSpaces());
			sb.append("\n");
			sb.append(status.getErrorMessage());
			if (status.isEverythingComplete()) {
				sb.append("\n");
				sb.append("upload complete");
			}
			return sb.toString();
		} catch (Exception e) {
			log.error(e.getMessage());
		}
		return null;
	}

	/**
	 * @param statusId
	 * @return the upload status object for a space XML upload
	 */
	public static SpaceXMLUploadStatus getSpaceXMLStatus(int statusId) {
		Connection con = null;
		CallableStatement procedure = null;
		ResultSet results = null;
		try {
			con = Common.getConnection();
			procedure = con.prepareCall("{CALL GetXMLUploadStatusById(?)}");
			procedure.setInt(1, statusId);
			results = procedure.executeQuery();

			if (results.next()) {
				SpaceXMLUploadStatus s = new SpaceXMLUploadStatus();
				s.setId(results.getInt("id"));

				s.setTotalBenchmarks(results.getInt("total_benchmarks"));
				s.setCompletedBenchmarks(results.getInt("completed_benchmarks"));

				s.setTotalSpaces(results.getInt("total_spaces"));
				s.setCompletedSpaces(results.getInt("completed_spaces"));

				s.setTotalSolvers(results.getInt("total_solvers"));
				s.setCompletedSolvers(results.getInt("completed_solvers"));

				s.setTotalUpdates(results.getInt("total_updates"));
				s.setCompletedUpdates(results.getInt("completed_updates"));

				s.setUploadDate(results.getTimestamp("upload_time"));
				s.setUserId(results.getInt("user_id"));
				s.setFileUploadComplete(results.getBoolean("file_upload_complete"));
				s.setEverythingComplete(results.getBoolean("everything_complete"));
				s.setErrorMessage(results.getString("error_message"));
				return s;
			}
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
	 * Accepts a result set currently pointing to a row containing a BenchmarkUploadStatus and returns that status.
	 *
	 * @param results The open resultset.
	 * @return The BenchmarkUploadStatus
	 * @throws SQLException
	 */
	private static BenchmarkUploadStatus resultsToBenchmarkUploadStatus(ResultSet results) throws SQLException {
		BenchmarkUploadStatus s = new BenchmarkUploadStatus();
		s.setId(results.getInt("id"));
		s.setCompletedBenchmarks(results.getInt("completed_benchmarks"));
		s.setCompletedSpaces(results.getInt("completed_spaces"));
		s.setFileExtractionComplete(results.getBoolean("file_extraction_complete"));
		s.setProcessingBegun(results.getBoolean("processing_begun"));
		s.setSpaceId(results.getInt("space_id"));
		s.setTotalBenchmarks(results.getInt("total_benchmarks"));
		s.setValidatedBenchmarks(results.getInt("validated_benchmarks"));
		s.setTotalSpaces(results.getInt("total_spaces"));
		s.setUploadDate(results.getTimestamp("upload_time"));
		s.setUserId(results.getInt("user_id"));
		s.setFileUploadComplete(results.getBoolean("file_upload_complete"));
		s.setEverythingComplete(results.getBoolean("everything_complete"));
		s.setErrorMessage(results.getString("error_message"));
		s.setFailedBenchmarks(results.getInt("failed_benchmarks"));
		return s;
	}

	/**
	 * Gets the upload status object when given its id
	 *
	 * @param statusId The id of the status to get information for
	 * @return An upload status object
	 * @author Benton McCune
	 */
	public static BenchmarkUploadStatus getBenchmarkStatus(int statusId) {
		Connection con = null;
		CallableStatement procedure = null;
		ResultSet results = null;
		try {
			con = Common.getConnection();
			procedure = con.prepareCall("{CALL GetBenchmarkUploadStatusById(?)}");
			procedure.setInt(1, statusId);
			results = procedure.executeQuery();

			if (results.next()) {
				return resultsToBenchmarkUploadStatus(results);
			}
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
	 * Gets the upload status object when given its id
	 *
	 * @param statusId The id of the status to get information for
	 * @return An upload status object
	 * @author Benton McCune
	 */
	public static List<Benchmark> getFailedBenches(int statusId) {

		Connection con = null;
		CallableStatement procedure = null;
		ResultSet results = null;
		try {
			con = Common.getConnection();
			procedure = con.prepareCall("{CALL GetUnvalidatedBenchmarks(?)}");
			procedure.setInt(1, statusId);
			results = procedure.executeQuery();
			List<Benchmark> badBenches = new LinkedList<>();
			while (results.next()) {
				Benchmark b = new Benchmark();
				b.setName(results.getString("bench_name"));
				b.setId(results.getInt("id"));
				badBenches.add(b);
			}
			return badBenches;
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		} finally {
			Common.safeClose(con);
			Common.safeClose(results);
			Common.safeClose(procedure);
		}

		return null;
	}

	/**
	 * Gets the benchmark processor output for a particular benchmark that failed validation
	 *
	 * @param id The ID of the invalid benchmark row
	 * @return The output of the processor as a string. Returns null if it does not exist.
	 */
	public static String getInvalidBenchmarkErrorMessage(int id) {
		Connection con = null;
		CallableStatement procedure = null;
		ResultSet results = null;
		try {
			con = Common.getConnection();
			procedure = con.prepareCall("{CALL GetInvalidBenchmarkMessage(?)}");
			procedure.setInt(1, id);
			results = procedure.executeQuery();
			if (results.next()) {
				return results.getString("error_message");
			}
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		} finally {
			Common.safeClose(con);
			Common.safeClose(results);
			Common.safeClose(procedure);
		}

		return null;
	}

	/**
	 * Given the ID of a row of an invalid benchmark, return the containing BenchmarkUploadStatus
	 *
	 * @param id The ID of the invalid benchmark row
	 * @return The BenchmarkUploadStatus, or null on error
	 */
	public static BenchmarkUploadStatus getUploadStatusForInvalidBenchmarkId(int id) {
		Connection con = null;
		CallableStatement procedure = null;
		ResultSet results = null;
		try {
			con = Common.getConnection();
			procedure = con.prepareCall("{CALL GetUploadStatusForInvalidBenchmarkId(?)}");
			procedure.setInt(1, id);
			results = procedure.executeQuery();
			if (results.next()) {
				return resultsToBenchmarkUploadStatus(results);
			}
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		} finally {
			Common.safeClose(con);
			Common.safeClose(results);
			Common.safeClose(procedure);
		}

		return null;
	}

	/**
	 * Adds incrementCount to the count of completed benchmarks when a benchmark is finished and added to the db.
	 *
	 * @param statusId - id of status object being incremented
	 * @param incrementCount The number to increment by
	 * @return true if successful, false if not
	 */
	public static Boolean incrementCompletedBenchmarks(Integer statusId, int incrementCount) {
		if (statusId == null || statusId <= 0) {
			return false;
		}
		Connection con = null;
		CallableStatement procedure = null;
		try {
			con = Common.getConnection();

			procedure = con.prepareCall("{CALL IncrementCompletedBenchmarks(?,?)}");

			procedure.setInt(1, statusId);
			procedure.setInt(2, incrementCount);
			procedure.executeUpdate();
			return true;
		} catch (Exception e) {
			log.error(e.getMessage(), e);
			return false;
		} finally {
			Common.safeClose(con);
			Common.safeClose(procedure);
		}
	}

	/**
	 * Adds 1 to the count of completed spaces when a space is finished and added to the db.
	 *
	 * @param statusId - id of status object being incremented
	 * @param incrementCount The number to increment by
	 * @return true if successful, false if not
	 */
	public static Boolean incrementCompletedSpaces(Integer statusId, int incrementCount) {
		if (statusId == null) {
			return false;
		}
		Connection con = null;
		CallableStatement procedure = null;
		try {
			con = Common.getConnection();

			procedure = con.prepareCall("{CALL IncrementCompletedSpaces(?,?)}");

			procedure.setInt(1, statusId);
			procedure.setInt(2, incrementCount);
			procedure.executeUpdate();
			return true;
		} catch (Exception e) {
			log.error(e.getMessage(), e);
			return false;
		} finally {
			Common.safeClose(con);
			Common.safeClose(procedure);
		}
	}

	/**
	 * Adds 1 to the count of failed benchmarks when a benchmark fails validation.
	 *
	 * @param statusId - id of status object being incremented
	 * @param incrementCounter The number to increment by
	 * @return true if successful, false if not
	 */
	public static Boolean incrementFailedBenchmarks(Integer statusId, int incrementCounter) {
		if (statusId == null) {
			return false;
		}
		Connection con = null;
		CallableStatement procedure = null;
		try {
			con = Common.getConnection();

			procedure = con.prepareCall("{CALL IncrementFailedBenchmarks(?,?)}");

			procedure.setInt(1, statusId);
			procedure.setInt(2, incrementCounter);
			procedure.executeUpdate();
			return true;
		} catch (Exception e) {
			log.error(e.getMessage(), e);
			return false;
		} finally {
			Common.safeClose(con);
			Common.safeClose(procedure);
		}
	}

	private static Boolean incrementXMLCompletedOfType(Integer statusId, int num, String type) {
		if (statusId == null) {
			return false;
		}
		Connection con = null;
		CallableStatement procedure = null;
		try {
			con = Common.getConnection();

			procedure = con.prepareCall("{CALL IncrementXMLCompleted" + type + "s(?,?)}");
			procedure.setInt(1, statusId);
			procedure.setInt(2, num);
			procedure.executeUpdate();
			return true;
		} catch (Exception e) {
			log.error(e.getMessage(), e);
			return false;
		} finally {
			Common.safeClose(con);
			Common.safeClose(procedure);
		}
	}

	public static Boolean incrementXMLCompletedBenchmarks(Integer statusId, int num) {
		return incrementXMLCompletedOfType(statusId, num, "Benchmark");
	}

	public static Boolean incrementXMLCompletedSolvers(Integer statusId, int num) {
		return incrementXMLCompletedOfType(statusId, num, "Solver");
	}

	public static Boolean incrementXMLCompletedUpdates(Integer statusId, int num) {
		return incrementXMLCompletedOfType(statusId, num, "Update");
	}

	public static Boolean incrementXMLCompletedSpaces(Integer statusId, int num) {
		return incrementXMLCompletedOfType(statusId, num, "Space");
	}

	private static Boolean setXMLTotalOfType(Integer statusId, int num, String type) {
		if (statusId == null) {
			return false;
		}
		Connection con = null;
		CallableStatement procedure = null;
		try {
			con = Common.getConnection();

			procedure = con.prepareCall("{CALL SetXMLTotal" + type + "s(?,?)}");
			procedure.setInt(1, statusId);
			procedure.setInt(2, num);
			procedure.executeUpdate();
			return true;
		} catch (Exception e) {
			log.error(e.getMessage(), e);
			return false;
		} finally {
			Common.safeClose(con);
			Common.safeClose(procedure);
		}
	}

	public static Boolean setXMLTotalBenchmarks(Integer statusId, int num) {
		return setXMLTotalOfType(statusId, num, "Benchmark");
	}

	public static Boolean setXMLTotalSolvers(Integer statusId, int num) {
		return setXMLTotalOfType(statusId, num, "Solver");
	}

	public static Boolean setXMLTotalUpdates(Integer statusId, int num) {
		return setXMLTotalOfType(statusId, num, "Update");
	}

	public static Boolean setXMLTotalSpaces(Integer statusId, int num) {
		return setXMLTotalOfType(statusId, num, "Space");
	}

	/**
	 * Adds 1 to the count of total benchmarks when a file is encountered in the creation of space java objects.
	 *
	 * @param statusId - id of status object being incremented
	 * @return true if successful, false if not
	 */
	public static Boolean incrementTotalBenchmarks(Integer statusId, int incrementCounter) {
		if (statusId == null) {
			return false;
		}
		if (incrementCounter == 0) {
			return true;
		}
		Connection con = null;
		CallableStatement procedure = null;
		try {
			con = Common.getConnection();

			procedure = con.prepareCall("{CALL IncrementTotalBenchmarks(?,?)}");
			procedure.setInt(1, statusId);
			procedure.setInt(2, incrementCounter);
			procedure.executeUpdate();
			return true;
		} catch (Exception e) {
			log.error(e.getMessage(), e);
			return false;
		} finally {
			Common.safeClose(con);
			Common.safeClose(procedure);
		}
	}

	/**
	 * Adds 1 to the count of total spaces when a directory is encountered in the creation of space java objects.
	 *
	 * @param statusId - id of status object being incremented
	 * @param incrementCounter The number to increment by
	 * @return true if successful, false if not
	 */
	public static Boolean incrementTotalSpaces(Integer statusId, int incrementCounter) {
		if (statusId == null) {
			return false;
		}
		if (incrementCounter == 0) {
			return true;
		}
		Connection con = null;
		CallableStatement procedure = null;
		try {
			con = Common.getConnection();

			procedure = con.prepareCall("{CALL IncrementTotalSpaces(?,?)}");

			procedure.setInt(1, statusId);
			procedure.setInt(2, incrementCounter);
			procedure.executeUpdate();
			return true;
		} catch (Exception e) {
			log.error(e.getMessage(), e);
			return false;
		} finally {
			Common.safeClose(con);
			Common.safeClose(procedure);
		}
	}

	/**
	 * Adds 1 to the count of validated benchmarks when a benchmark is processed and validated.  Benchmark must
	 * still be
	 * added to the db at this point.
	 *
	 * @param statusId - id of status object being incremented
	 * @param incrementCounter The number to increment by
	 * @return true if successful, false if not
	 */
	public static Boolean incrementValidatedBenchmarks(Integer statusId, int incrementCounter) {
		if (statusId == null) {
			return false;
		}
		Connection con = null;
		CallableStatement procedure = null;
		try {
			con = Common.getConnection();

			procedure = con.prepareCall("{CALL IncrementValidatedBenchmarks(?,?)}");

			procedure.setInt(1, statusId);
			procedure.setInt(2, incrementCounter);
			procedure.executeUpdate();
			return true;
		} catch (Exception e) {
			log.error(e.getMessage(), e);
			return false;
		} finally {
			Common.safeClose(con);
			Common.safeClose(procedure);
		}
	}

	/**
	 * Indicates that benchmark and space java objects have been created for the entire space hierarchy. This is called
	 * when the benchmarks are being validated and entered into the database.
	 *
	 * @param statusId
	 * @return true if successful, false if not
	 */
	public static Boolean processingBegun(Integer statusId) {
		if (statusId == null) {
			return false;
		}
		Connection con = null;
		CallableStatement procedure = null;
		try {
			con = Common.getConnection();

			procedure = con.prepareCall("{CALL processingBegun(?)}");

			procedure.setInt(1, statusId);
			procedure.executeUpdate();
			return true;
		} catch (Exception e) {
			log.error(e.getMessage(), e);
			return false;
		} finally {
			Common.safeClose(con);
			Common.safeClose(procedure);
		}
	}

	/**
	 * @param statusId
	 * @param message
	 * @return True on success and false on error
	 */
	public static Boolean setXMLErrorMessage(Integer statusId, String message) {
		if (statusId == null) {
			return false;
		}
		Connection con = null;
		CallableStatement procedure = null;

		try {
			con = Common.getConnection();

			procedure = con.prepareCall("{CALL SetXMLErrorMessage(?,?)}");

			procedure.setInt(1, statusId);
			procedure.setString(2, message);
			procedure.executeUpdate();
			return true;
		} catch (Exception e) {
			log.error(e.getMessage(), e);
			return false;
		} finally {
			Common.safeClose(con);
			Common.safeClose(procedure);
		}
	}


// Archie code start
    public static List<BenchmarkUploadStatus> getUploadsByUserForNextPage(DataTablesQuery query, int userId) {
	Connection con = null;
	NamedParameterStatement procedure = null;
	ResultSet results = null;
	try {
	    con = Common.getConnection();
	    PaginationQueryBuilder builder = new PaginationQueryBuilder(PaginationQueries.GET_UPLOADS_BY_USER_QUERY,
									getUploadOrderColumn(query.getSortColumn()),
									query
									);		
		procedure = new NamedParameterStatement(con, builder.getSQL());
	    procedure.setInt("userId", userId);
	    procedure.setString("query", query.getSearchQuery());

	    results = procedure.executeQuery();
	    List<BenchmarkUploadStatus> uploads = new LinkedList<>();

	    while(results.next()) {
		BenchmarkUploadStatus u  = new BenchmarkUploadStatus();
		u.setId(results.getInt("id"));
		u.setCompletedBenchmarks(results.getInt("completed_benchmarks"));
		u.setCompletedSpaces(results.getInt("completed_spaces"));
		u.setFileExtractionComplete(results.getBoolean("file_extraction_complete"));
		u.setProcessingBegun(results.getBoolean("processing_begun"));
		u.setSpaceId(results.getInt("space_id"));
		u.setTotalBenchmarks(results.getInt("total_benchmarks"));
		u.setValidatedBenchmarks(results.getInt("validated_benchmarks"));
		u.setTotalSpaces(results.getInt("total_spaces"));
		u.setUploadDate(results.getTimestamp("upload_time"));
		u.setUserId(results.getInt("user_id"));
		u.setFileUploadComplete(results.getBoolean("file_upload_complete"));
		u.setEverythingComplete(results.getBoolean("everything_complete"));
		u.setErrorMessage(results.getString("error_message"));
		u.setFailedBenchmarks(results.getInt("failed_benchmarks"));
		uploads.add(u);
	    }
	    return uploads;
	} catch (Exception e) {
	    log.error("getUploadsForNextPage", e);
	} finally {
	    Common.safeClose(con);
	    Common.safeClose(procedure);
	    Common.safeClose(results);
	}
	return null;
    }


	//get what column we are sorting by for the sql query
    public static String getUploadOrderColumn(int indexOfColumn) {
	switch (indexOfColumn) {
	case 0:
	    return  "upload_time";
	case 1:
	    return "total_benchmarks";
	case 2:
	    return "space_id";
	default:
	    return "space_id";
	}
    }



    public static int getUploadCountByUser(int userId) {
	Connection con = null;
	CallableStatement procedure = null;
	ResultSet results = null;
	try {
	    con = Common.getConnection();
	    procedure = con.prepareCall("{CALL GetUploadCountByUser(?)}");
	    procedure.setInt(1, userId);
	    results = procedure.executeQuery();

	    if(results.next()) {
		return results.getInt("uploadCount");
	    }
	} catch (Exception e) {
	    log.error("getUploadCountByUser", e);
	} finally {
	    Common.safeClose(con);
	    Common.safeClose(procedure);
	    Common.safeClose(results);
	}
	return 0;
    }


    public static int getUploadCountByUser(int userId, String query) {
	Connection con = null;
	CallableStatement procedure = null;
	ResultSet results = null;
	try {
	    con = Common.getConnection();
	    procedure = con.prepareCall("{CALL GetUploadCountByUserWithQuery(?, ?)}");
	    procedure.setInt(1, userId);
	    procedure.setString(2, query);
	    results = procedure.executeQuery();

	    if (results.next()) {
		return results.getInt("uploadCount");
	    }
	} catch (Exception e) {
		log.error("getUploadCountByUser", e);
	} finally {
	    Common.safeClose(con);
	    Common.safeClose(procedure);
	    Common.safeClose(results);
	}
	    return 0;

    }

// End Archie Code


	/**
	 * Sets error message
	 *
	 * @param statusId - id of status object being changed
	 * @param message The error message to set.
	 * @return true if successful, false if not
	 */
	public static Boolean setBenchmarkErrorMessage(Integer statusId, String message) {
		if (statusId == null) {
			return false;
		}
		Connection con = null;
		CallableStatement procedure = null;

		try {
			con = Common.getConnection();

			procedure = con.prepareCall("{CALL SetBenchmarkErrorMessage(?,?)}");

			procedure.setInt(1, statusId);
			procedure.setString(2, message);
			procedure.executeUpdate();
			return true;
		} catch (Exception e) {
			log.error(e.getMessage(), e);
			return false;
		} finally {
			Common.safeClose(con);
			Common.safeClose(procedure);
		}
	}
}
