package org.starexec.data.database;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.ResultSet;
import java.util.LinkedList;
import java.util.List;

import org.apache.log4j.Logger;
import org.starexec.data.to.BenchmarkType;

/**
 * Handles all database interaction for benchmark types
 */
public class BenchTypes {
	private static final Logger log = Logger.getLogger(BenchTypes.class);
	
	/**
	 * Inserts a benchmark type into the database
	 * @param type The benchmark type to add to the database
	 * @return True if the operation was a success, false otherwise
	 * @author Tyler Jensen
	 */
	public static boolean add(BenchmarkType type) {
		Connection con = null;			
		
		try {
			con = Common.getConnection();		
			CallableStatement procedure = null;			
			procedure = con.prepareCall("{CALL AddBenchmarkType(?, ?, ?, ?)}");			
			procedure.setString(1, type.getName());
			procedure.setString(2, type.getDescription());
			procedure.setString(3, type.getProcessorPath());
			procedure.setLong(4, type.getCommunityId());
			
			procedure.executeUpdate();			
			return true;			
		} catch (Exception e){			
			log.error(e.getMessage(), e);		
		} finally {
			Common.safeClose(con);
		}
		
		return false;
	}
	
	/**
	 * Deletes a given benchmark type from a space
	 * @param typeId the id of the benchmark type to delete
	 * @param spaceId the id of the space to delete the benchmark type from
	 * @return True if the operation was a success, false otherwise
	 * @author Todd Elvers
	 */
	public static boolean delete(long typeId, long spaceId){
		Connection con = null;			
		
		try {
			con = Common.getConnection();
			CallableStatement procedure = con.prepareCall("{CALL DeleteBenchmarkType(?, ?)}");
			procedure.setLong(1, typeId);
			procedure.setLong(2, spaceId);
			
			procedure.executeUpdate();						
			log.debug(String.format("Deletion of benchmark type [id=%d] for space [id=%d] was successful.", typeId, spaceId));
			return true;
		} catch (Exception e){			
			log.error(e.getMessage(), e);		
		} finally {
			Common.safeClose(con);
		}
		
		log.debug(String.format("Deletion of benchmark type [id=%d] for space [id=%d] failed.", typeId, spaceId));
		return false;
	}
	
	/**	 
	 * @param benchTypeId The id of the bentch type to retrieve
	 * @return The corresponding benchmark type
	 * @author Tyler Jensen
	 */
	public static BenchmarkType get(long benchTypeId) {
		Connection con = null;			
		
		try {
			con = Common.getConnection();					
			CallableStatement procedure = con.prepareCall("{CALL GetBenchTypeById(?)}");
			procedure.setLong(1, benchTypeId);
			ResultSet results = procedure.executeQuery();			
			
			if(results.next()){							
				BenchmarkType t = new BenchmarkType();
				t.setId(results.getLong("id"));
				t.setCommunityId(results.getLong("community"));
				t.setDescription(results.getString("description"));
				t.setName(results.getString("name"));
				t.setProcessorPath(results.getString("processor_path"));
				return t;					
			}							
		} catch (Exception e){			
			log.error(e.getMessage(), e);		
		} finally {
			Common.safeClose(con);
		}
		
		return null;
	}
	
	/**
	 * Gets the list of benchmark types
	 * @return the list of benchmark types
	 * @author Todd Elvers
	 */
	public static List<BenchmarkType> getAll(){
		Connection con = null;			
		
		try {
			con = Common.getConnection();		
			CallableStatement procedure = con.prepareCall("{CALL GetAllBenchTypes}");
			ResultSet results = procedure.executeQuery();
			List<BenchmarkType> benchTypes = new LinkedList<BenchmarkType>();
			
			while(results.next()){
				BenchmarkType bt = new BenchmarkType();
				bt.setId(results.getLong("id"));
				bt.setName(results.getString("name"));
				bt.setDescription(results.getString("description"));
				bt.setProcessorPath((results.getString("processor_path")));
				bt.setCommunityId((results.getLong("community")));
				benchTypes.add(bt);
			}
			
			return benchTypes;
		} catch (Exception e){			
			log.error(e.getMessage(), e);		
		} finally {
			Common.safeClose(con);
		}
		
		return null;
	}	
	
	/**	 
	 * @param communityId The id of the community to retrieve all types for
	 * @return A list of all benchmark types the community owns
	 * @author Tyler Jensen
	 */
	public static List<BenchmarkType> getByCommunity(long communityId) {
		Connection con = null;			
		
		try {
			con = Common.getConnection();					
			CallableStatement procedure = con.prepareCall("{CALL GetBenchTypesByCommunity(?)}");
			procedure.setLong(1, communityId);
			ResultSet results = procedure.executeQuery();
			List<BenchmarkType> types = new LinkedList<BenchmarkType>();
			
			while(results.next()){							
				BenchmarkType t = new BenchmarkType();
				t.setId(results.getLong("id"));
				t.setCommunityId(results.getLong("community"));
				t.setDescription(results.getString("description"));
				t.setName(results.getString("name"));
				t.setProcessorPath(results.getString("processor_path"));
				types.add(t);						
			}				
			
			return types;
		} catch (Exception e){			
			log.error(e.getMessage(), e);		
		} finally {
			Common.safeClose(con);
		}
		
		return null;
	}	
	
	/**
	 * Updates the description of a benchmark type with the given type id
	 * @param typeId the id of the bench type to update
	 * @param newDesc the new description to update the type with
	 * @return True if the operation was a success, false otherwise
	 * @author Tyler Jensen
	 */
	public static boolean updateDescription(long typeId, String newDesc){
		Connection con = null;			
		
		try {
			con = Common.getConnection();		
			CallableStatement procedure = con.prepareCall("{CALL UpdateBenchTypeDescription(?, ?)}");
			procedure.setLong(1, typeId);					
			procedure.setString(2, newDesc);
			
			procedure.executeUpdate();			
			return true;			
		} catch (Exception e){			
			log.error(e.getMessage(), e);		
		} finally {
			Common.safeClose(con);
		}
		
		return false;
	}
	
	/**
	 * Updates the name of a benchmark type with the given type id
	 * 
	 * @param typeId the id of the bench type to update
	 * @param newName the new name to update the type with
	 * @return True if the operation was a success, false otherwise
	 * @author Tyler Jensen
	 */
	public static boolean updateName(long typeId, String newName){
		Connection con = null;			
		
		try {
			con = Common.getConnection();		
			CallableStatement procedure = con.prepareCall("{CALL UpdateBenchTypeName(?, ?)}");
			procedure.setLong(1, typeId);					
			procedure.setString(2, newName);
			
			procedure.executeUpdate();			
			return true;			
		} catch (Exception e){			
			log.error(e.getMessage(), e);		
		} finally {
			Common.safeClose(con);
		}
		
		return false;
	}	
	
	/**
	 * Updates the processor path of a benchmark type with the given type id
	 * 
	 * @param typeId the id of the bench type to update
	 * @param newPath the new processor path to update the type with
	 * @return True if the operation was a success, false otherwise
	 * @author Tyler Jensen
	 */
	public static boolean updatePath(long typeId, String newPath){
		Connection con = null;			
		
		try {
			con = Common.getConnection();		
			CallableStatement procedure = con.prepareCall("{CALL UpdateBenchTypePath(?, ?)}");
			procedure.setLong(1, typeId);					
			procedure.setString(2, newPath);
			
			procedure.executeUpdate();			
			return true;				
		} catch (Exception e){			
			log.error(e.getMessage(), e);		
		} finally {
			Common.safeClose(con);
		}
		
		return false;
	}		
}