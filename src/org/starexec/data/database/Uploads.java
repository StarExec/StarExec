package org.starexec.data.database;

import java.sql.CallableStatement;
import java.sql.Connection;

import org.apache.log4j.Logger;
/**
 * Handles all database interaction for the uploading Benchmarks Status Page.
 */
public class Uploads {
	private static final Logger log = Logger.getLogger(Uploads.class);

	public static Integer createUploadStatus(Integer spaceId, Integer userId){
		Connection con = null;			
		
		try {
			con = Common.getConnection();	
			Common.beginTransaction(con);
				
			CallableStatement procedure = con.prepareCall("{CALL CreateUploadStatus(?, ?, ?)}");
		
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
		}
	}
	
	public static Boolean fileUploadComplete(Integer statusId){
		Connection con = null;			
		
		try {
			con = Common.getConnection();	
			Common.beginTransaction(con);
				
			CallableStatement procedure = con.prepareCall("{CALL FileUploadComplete(?)}");
		
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
		}
	}

	public static Boolean fileExtractComplete(Integer statusId){
		Connection con = null;			
		
		try {
			con = Common.getConnection();	
			Common.beginTransaction(con);
				
			CallableStatement procedure = con.prepareCall("{CALL FileExtractComplete(?)}");
		
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
		}
	}
	
	public static Boolean processingBegun(Integer statusId){
		Connection con = null;			
		
		try {
			con = Common.getConnection();	
			Common.beginTransaction(con);
				
			CallableStatement procedure = con.prepareCall("{CALL processingBegun(?)}");
		
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
		}
	}
	
	public static Boolean incrementTotalSpaces(Integer statusId){
		Connection con = null;			
		
		try {
			con = Common.getConnection();	
			Common.beginTransaction(con);
				
			CallableStatement procedure = con.prepareCall("{CALL IncrementTotalSpaces(?)}");
		
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
		}
	}
	
	public static Boolean incrementTotalBenchmarks(Integer statusId){
		Connection con = null;			
		
		try {
			con = Common.getConnection();	
			Common.beginTransaction(con);
				
			CallableStatement procedure = con.prepareCall("{CALL IncrementTotalBenchmarks(?)}");
		
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
		}
	}
	
	public static Boolean incrementCompletedSpaces(Integer statusId){
		Connection con = null;			
		
		try {
			con = Common.getConnection();	
			Common.beginTransaction(con);
				
			CallableStatement procedure = con.prepareCall("{CALL IncrementCompletedSpaces(?)}");
		
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
		}
	}
	
	public static Boolean incrementCompletedBenchmarks(Integer statusId){
		Connection con = null;			
		
		try {
			con = Common.getConnection();	
			Common.beginTransaction(con);
				
			CallableStatement procedure = con.prepareCall("{CALL IncrementCompletedBenchmarks(?)}");
		
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
		}
	}

}
