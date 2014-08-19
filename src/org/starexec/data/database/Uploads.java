package org.starexec.data.database;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.ResultSet;
import java.util.LinkedList;
import java.util.List;

import org.apache.log4j.Logger;
import org.starexec.data.to.UploadStatus;
/**
 * Handles database interaction for the uploading Benchmarks Status Page.
 */
public class Uploads {
	private static final Logger log = Logger.getLogger(Uploads.class);

	/**
	 * Adds failed benchmark name to db
	 * @param statusId - id of status object being changed
	 * @return true if successful, false if not
	 */
	public static Boolean addFailedBenchmark(Integer statusId, String name){
		Connection con = null;	
		CallableStatement procedure = null;
		if (name.length() > 512){
			throw new IllegalArgumentException("set Error Message too long, must be less than 512 chars.  This message has " + name.length());
		}
		try {
			con = Common.getConnection();	
			Common.beginTransaction(con);
				
			 procedure = con.prepareCall("{CALL AddUnvalidatedBenchmark(?,?)}");
		
			procedure.setInt(1, statusId);
			procedure.setString(2,name);
			procedure.executeUpdate();			
			Common.endTransaction(con);
			return true;
		} catch (Exception e){			
			log.error("addFailedBenchmark says " + e.getMessage(), e);	
			Common.doRollback(con);
			return false;
		} finally {
			Common.safeClose(con);
			Common.safeClose(procedure);
		}
	}
	
	/**
	 * Creates object representing the current status of a user's upload of benchmarks
	 * @param spaceId - the id of the parent space that benchmarks are being uploaded to
	 * @param userId - id of the user uploading benchmarks
	 * @return the id of the UploadStatus object
	 * @author Benton McCune
	 */
	public static Integer createUploadStatus(Integer spaceId, Integer userId){
		Connection con = null;			
		CallableStatement procedure = null;
		try {
			con = Common.getConnection();	
			Common.beginTransaction(con);
				
			 procedure = con.prepareCall("{CALL CreateUploadStatus(?, ?, ?)}");
		
				procedure.setInt(1, spaceId);
				procedure.setInt(2, userId);	
				procedure.registerOutParameter(3, java.sql.Types.INTEGER);	
				procedure.executeUpdate();			
				Integer newStatusId = procedure.getInt(3);
			Common.endTransaction(con);
			return newStatusId;
		} catch (Exception e){			
			log.error(e.getMessage(), e);	
			Common.doRollback(con);
			return -1;
		} finally {
			Common.safeClose(con);
			Common.safeClose(procedure);
		}
	}
	/**
	 * Indicates that the entire benchmark upload process is complete.  This is triggered
	 * even if the upload failed for some reason.
	 * @param statusId
	 * @return true if successful, false if not
	 */
	public static Boolean everythingComplete(Integer statusId){
		Connection con = null;			
		CallableStatement procedure = null;
		try {
			con = Common.getConnection();	
			Common.beginTransaction(con);
				
			 procedure = con.prepareCall("{CALL EverythingComplete(?)}");
		
			procedure.setInt(1, statusId);
			procedure.executeUpdate();			
			Common.endTransaction(con);
			return true;
		} catch (Exception e){			
			log.error(e.getMessage(), e);	
			Common.doRollback(con);
			return false;
		} finally {
			Common.safeClose(con);
			Common.safeClose(procedure);
		}
	}
	/**
	 * The archived file has been successfully extracted.
	 * @param statusId
	 * @return true if successful, false if not
	 */
	public static Boolean fileExtractComplete(Integer statusId){
		Connection con = null;			
		CallableStatement procedure = null;
		try {
			con = Common.getConnection();	
			Common.beginTransaction(con);
				
			 procedure = con.prepareCall("{CALL FileExtractComplete(?)}");
		
			procedure.setInt(1, statusId);
			procedure.executeUpdate();			
			Common.endTransaction(con);
			return true;
		} catch (Exception e){			
			log.error(e.getMessage(), e);	
			Common.doRollback(con);
			return false;
		} finally {
			Common.safeClose(con);
			Common.safeClose(procedure);
		}
	}
	
	/**
	 * Informs the database that the archive file has been uploaded and is in the file system.
	 * @param statusId - id of uploadStatus object
	 * @return true if successful, false if not
	 */
	public static Boolean fileUploadComplete(Integer statusId){
		Connection con = null;			
		CallableStatement procedure = null;
		try {
			con = Common.getConnection();	
			Common.beginTransaction(con);
				
			procedure = con.prepareCall("{CALL FileUploadComplete(?)}");
		
			procedure.setInt(1, statusId);
			procedure.executeUpdate();			
			Common.endTransaction(con);
			return true;
		} catch (Exception e){			
			log.error(e.getMessage(), e);	
			Common.doRollback(con);
			return false;
		} finally {
			Common.safeClose(con);
			Common.safeClose(procedure);
		}
	}
	
	/**
	 * Gets a string summary of an upload status, suitable for printing out and displaying to users
	 * (used in StarexeCommand)
	 * @param statusId
	 * @return The string summary, or null on error
	 */
	public static String getUploadStatusSummary(int statusId) {
		UploadStatus status=get(statusId);
		StringBuilder sb=new StringBuilder();
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
		if(status.isEverythingComplete()) {
			sb.append("\n");
			sb.append("upload complete");
		}
		return sb.toString();
	}
	
	/**
	 * Gets the upload status object when given its id
	 * @param statusId The id of the status to get information for
	 * @return An upload status object
	 * @author Benton McCune
	 */
	public static UploadStatus get(int statusId) {
		Connection con = null;			
		CallableStatement procedure = null;
		ResultSet results = null;
		try {			
			con = Common.getConnection();		
			 procedure = con.prepareCall("{CALL GetUploadStatusById(?)}");
			procedure.setInt(1, statusId);					
			 results = procedure.executeQuery();		
			
			if(results.next()){
				UploadStatus s = new UploadStatus();
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
		} catch (Exception e){			
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
	 * @param statusId The id of the status to get information for
	 * @return An upload status object
	 * @author Benton McCune
	 */
	public static List<String> getFailedBenches(int statusId) {
		Connection con = null;			
		CallableStatement procedure = null;
		ResultSet results = null;
		try {			
			con = Common.getConnection();		
			 procedure = con.prepareCall("{CALL GetUnvalidatedBenchmarks(?)}");
			procedure.setInt(1, statusId);					
			 results = procedure.executeQuery();		
			List<String> badBenches = new LinkedList<String>();
			while(results.next()){
				badBenches.add(results.getString("bench_name"));
			}
			return badBenches;
		} catch (Exception e){			
			log.error(e.getMessage(), e);		
		} finally {
			Common.safeClose(con);
			Common.safeClose(results);
			Common.safeClose(procedure);
		}
		
		return null;
	}
	
	/**
	 * Adds 1 to the count of completed benchmarks when a benchmark is finished and added to the db.
	 * @param statusId - id of status object being incremented
	 * @return true if successful, false if not
	 */
	public static Boolean incrementCompletedBenchmarks(Integer statusId){
		Connection con = null;			
		CallableStatement procedure = null;
		try {
			con = Common.getConnection();	
			Common.beginTransaction(con);
				
			 procedure = con.prepareCall("{CALL IncrementCompletedBenchmarks(?)}");
		
			procedure.setInt(1, statusId);
			procedure.executeUpdate();			
			Common.endTransaction(con);
			return true;
		} catch (Exception e){			
			log.error(e.getMessage(), e);	
			Common.doRollback(con);
			return false;
		} finally {
			Common.safeClose(con);
			Common.safeClose(procedure);
		}
	}
	/**
	 * Adds 1 to the count of completed spaces when a space is finished and added to the db.
	 * @param statusId - id of status object being incremented
	 * @return true if successful, false if not
	 */
	public static Boolean incrementCompletedSpaces(Integer statusId){
		Connection con = null;			
		CallableStatement procedure = null;
		try {
			con = Common.getConnection();	
			Common.beginTransaction(con);
				
			 procedure = con.prepareCall("{CALL IncrementCompletedSpaces(?)}");
		
			procedure.setInt(1, statusId);
			procedure.executeUpdate();			
			Common.endTransaction(con);
			return true;
		} catch (Exception e){			
			log.error(e.getMessage(), e);	
			Common.doRollback(con);
			return false;
		} finally {
			Common.safeClose(con);
			Common.safeClose(procedure);
		}
	}
	
	/**
	 * Adds 1 to the count of failed benchmarks when a benchmark fails validation.
	 * @param statusId - id of status object being incremented
	 * @return true if successful, false if not
	 */
	public static Boolean incrementFailedBenchmarks(Integer statusId){
		Connection con = null;			
		CallableStatement procedure = null;
		try {
			con = Common.getConnection();	
			Common.beginTransaction(con);
				
			 procedure = con.prepareCall("{CALL IncrementFailedBenchmarks(?)}");
		
			procedure.setInt(1, statusId);
			procedure.executeUpdate();			
			Common.endTransaction(con);
			return true;
		} catch (Exception e){			
			log.error(e.getMessage(), e);	
			Common.doRollback(con);
			return false;
		} finally {
			Common.safeClose(con);
			Common.safeClose(procedure);
		}
	}
	
	/**
	 *  Adds 1 to the count of total benchmarks when a file is encountered in the creation of space
	 * java objects.
	 * @param statusId - id of status object being incremented
	 * @return true if successful, false if not
	 */
	public static Boolean incrementTotalBenchmarks(Integer statusId){
		Connection con = null;			
		CallableStatement procedure = null;
		try {
			con = Common.getConnection();	
			Common.beginTransaction(con);
				
			procedure = con.prepareCall("{CALL IncrementTotalBenchmarks(?)}");
		
			procedure.setInt(1, statusId);
			procedure.executeUpdate();			
			Common.endTransaction(con);
			return true;
		} catch (Exception e){			
			log.error(e.getMessage(), e);	
			Common.doRollback(con);
			return false;
		} finally {
			Common.safeClose(con);
			Common.safeClose(procedure);
		}
	}
	
	/**
	 * Adds 1 to the count of total spaces when a directory is encountered in the creation of space
	 * java objects.
	 * @param statusId - id of status object being incremented
	 * @return true if successful, false if not
	 */
	public static Boolean incrementTotalSpaces(Integer statusId){
		Connection con = null;			
		CallableStatement procedure = null;
		try {
			con = Common.getConnection();	
			Common.beginTransaction(con);
				
			 procedure = con.prepareCall("{CALL IncrementTotalSpaces(?)}");
		
			procedure.setInt(1, statusId);
			procedure.executeUpdate();			
			Common.endTransaction(con);
			return true;
		} catch (Exception e){			
			log.error(e.getMessage(), e);	
			Common.doRollback(con);
			return false;
		} finally {
			Common.safeClose(con);
			Common.safeClose(procedure);
		}
	}
	
	/**
	 *  Adds 1 to the count of validated benchmarks when a benchmark is processed and validated.  Benchmark must still be 
	 *  added to the db at this point.
	 * @param statusId - id of status object being incremented
	 * @return true if successful, false if not
	 */
	public static Boolean incrementValidatedBenchmarks(Integer statusId){
		Connection con = null;			
		CallableStatement procedure = null;
		try {
			con = Common.getConnection();	
			Common.beginTransaction(con);
				
			 procedure = con.prepareCall("{CALL IncrementValidatedBenchmarks(?)}");
		
			procedure.setInt(1, statusId);
			procedure.executeUpdate();			
			Common.endTransaction(con);
			return true;
		} catch (Exception e){			
			log.error(e.getMessage(), e);	
			Common.doRollback(con);
			return false;
		} finally {
			Common.safeClose(con);
			Common.safeClose(procedure);
		}
	}
	
	/**
	 * Indicates that benchmark and space java objects have been created for the entire space hierarchy.  
	 * This is called when the benchmarks are being validated and entered into the database.
	 * @param statusId
	 * @return true if successful, false if not
	 */
	public static Boolean processingBegun(Integer statusId){
		Connection con = null;			
		CallableStatement procedure = null;
		try {
			con = Common.getConnection();	
			Common.beginTransaction(con);
				
			 procedure = con.prepareCall("{CALL processingBegun(?)}");
		
			procedure.setInt(1, statusId);
			procedure.executeUpdate();			
			Common.endTransaction(con);
			return true;
		} catch (Exception e){			
			log.error(e.getMessage(), e);	
			Common.doRollback(con);
			return false;
		} finally {
			Common.safeClose(con);
			Common.safeClose(procedure);
		}
	}
	
	/**
	 * Sets error message
	 * @param statusId - id of status object being changed
	 * @return true if successful, false if not
	 */
	public static Boolean setErrorMessage(Integer statusId, String message){
	    if (statusId == null)
		return false;
	    Connection con = null;	
	    CallableStatement procedure = null;
	    if (message.length() > 512){
		throw new IllegalArgumentException("set Error Message too long, must be less than 512 chars.  This message has " + message.length());
	    }
	    try {
		con = Common.getConnection();	
		Common.beginTransaction(con);
				
		procedure = con.prepareCall("{CALL SetErrorMessage(?,?)}");
		
		procedure.setInt(1, statusId);
		procedure.setString(2,message);
		procedure.executeUpdate();			
		Common.endTransaction(con);
		return true;
	    } catch (Exception e){			
		log.error(e.getMessage(), e);	
		Common.doRollback(con);
		return false;
	    } finally {
		Common.safeClose(con);
		Common.safeClose(procedure);
	    }
	}
    
}
