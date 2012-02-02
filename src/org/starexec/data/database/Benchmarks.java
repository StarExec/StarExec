package org.starexec.data.database;

import java.io.File;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedList;
import java.util.List;

import org.apache.log4j.Logger;
import org.starexec.data.to.Benchmark;
import org.starexec.data.to.BenchmarkType;

/**
 * Handles all database interaction for benchmarks.
 */
public class Benchmarks {
	private static final Logger log = Logger.getLogger(Benchmarks.class);
	
	/**
	 * Associates the benchmarks with the given ids to the given space
	 * @param benchIds The list of benchmark ids to associate with the space
	 * @param spaceId The id of the space the benchmarks will be associated with
	 * @return True if the operation was a success, false otherwise
	 * @author Tyler Jensen
	 */
	public static boolean associate(List<Long> benchIds, long spaceId) {
		Connection con = null;			
		
		try {
			con = Common.getConnection();	
			Common.beginTransaction(con);
			
			CallableStatement procedure = null;						
			procedure = con.prepareCall("{CALL AssociateBench(?, ?)}");
			
			for(long bid : benchIds) {
				procedure.setLong(1, bid);
				procedure.setLong(2, spaceId);			
				procedure.executeUpdate();			
			}			
			
			Common.endTransaction(con);
			return true;
		} catch (Exception e){			
			log.error(e.getMessage(), e);	
			Common.doRollback(con);
		} finally {
			Common.safeClose(con);
		}
		
		return false;
	}
	
	/**
	 * Adds a single benchmark to the database under the given spaceId
	 * @param benchmark The benchmark to add to the database
	 * @param spaceId The id of the space the benchmark will belong to
	 * @return True if the operation was a success, false otherwise
	 * @author Tyler Jensen
	 */
	public static boolean add(Benchmark benchmark, long spaceId) {
		Connection con = null;			
		
		try {
			con = Common.getConnection();		
			return Benchmarks.add(con, benchmark, spaceId);
		} catch (Exception e){			
			log.error(e.getMessage(), e);		
		} finally {
			Common.safeClose(con);
		}
		
		return false;
	}
	
	/**
	 * Adds the list of benchmarks to the database and associates them with the given spaceId
	 * @param benchmarks The list of benchmarks to add
	 * @param spaceId The space the benchmarks will belong to
	 * @return True if the operation was a success, false otherwise
	 * @author Tyler Jensen
	 */
	public static boolean add(List<Benchmark> benchmarks, long spaceId) {
		Connection con = null;			
		
		try {			
			con = Common.getConnection();
			
			Common.beginTransaction(con);
			Benchmarks.add(con, benchmarks, spaceId);
			Common.endTransaction(con);
			
			return true;
		} catch (Exception e){			
			log.error(e.getMessage(), e);
			Common.doRollback(con);
		} finally {
			Common.safeClose(con);
		}
		
		return false;
	}	
	
	/**
	 * Internal method which adds a single benchmark to the database under the given spaceId
	 * @param con The connection the operation will take place on
	 * @param benchmark The benchmark to add to the database
	 * @param spaceId The id of the space the benchmark will belong to
	 * @return True if the operation was a success, false otherwise
	 * @author Tyler Jensen
	 */
	protected static boolean add(Connection con, Benchmark benchmark, long spaceId) throws SQLException {				
		CallableStatement procedure = null;			
		procedure = con.prepareCall("{CALL AddBenchmark(?, ?, ?, ?, ?, ?)}");
		procedure.setString(1, benchmark.getName());		
		procedure.setString(2, benchmark.getPath());
		procedure.setBoolean(3, benchmark.isDownloadable());
		procedure.setLong(4, benchmark.getUserId());
		procedure.setLong(5, benchmark.getType().getId());
		procedure.setLong(6, spaceId);
		
		procedure.executeUpdate();		
		return true;
	}
	
	/**
	 * Internal method which adds the list of benchmarks to the database and associates them with the given spaceId
	 * @param con The connection the operation will take place on
	 * @param benchmarks The list of benchmarks to add
	 * @param spaceId The space the benchmarks will belong to
	 * @return True if the operation was a success, false otherwise
	 * @author Tyler Jensen
	 */
	protected static void add(Connection con, List<Benchmark> benchmarks, long spaceId) throws Exception {		
		for(Benchmark b : benchmarks) {
			if(!Benchmarks.add(con, b, spaceId)) {
				throw new Exception(String.format("Failed to add benchmark [%s] to space [%d]", b.getName(), spaceId));
			}
		}
		
		log.info(String.format("[%d] new benchmarks added to space [%d]", benchmarks.size(), spaceId));
	}
	
	/**
	 * Deletes a benchmark from the database (cascading deletes handle all dependencies)
	 * @param id the id of the benchmark to delete
	 * @return True if the operation was a success, false otherwise
	 * @author Todd Elvers
	 */
	public static boolean delete(long id){
		Connection con = null;			
		File benchToDelete = null;
		try {
			con = Common.getConnection();
			CallableStatement procedure = con.prepareCall("{CALL DeleteBenchmarkById(?, ?)}");
			procedure.setLong(1, id);
			procedure.registerOutParameter(2, java.sql.Types.LONGNVARCHAR);
			procedure.executeUpdate();
			
			// Delete benchmark file from disk, and the parent directory if it's empty
			benchToDelete = new File(procedure.getString(2));
			if(benchToDelete.delete()){
				log.debug(String.format("Benchmark file [%s] was successfully deleted from disk at [%s].", benchToDelete.getName(), benchToDelete.getAbsolutePath()));
			}
			if(benchToDelete.getParentFile().delete()){
				log.debug(String.format("Directory [%s] was deleted because it was empty.", benchToDelete.getParentFile().getAbsolutePath()));
			}
			
			log.debug(String.format("Deletion of benchmark [id=%d] in directory [%s] was successful.", id, benchToDelete.getAbsolutePath()));					
			return true;
		} catch (Exception e){			
			log.error(e.getMessage(), e);		
		} finally {
			Common.safeClose(con);
		}
		log.debug(String.format("Deletion of benchmark [id=%d] failed.", id));
		return false;
	}	
	
	/**
	 * 
	 * @param benchId The id of the benchmark to retrieve
	 * @return A benchmark object representing the benchmark with the given ID
	 * @author Tyler Jensen
	 */
	public static Benchmark get(long benchId) {
		Connection con = null;			
		
		try {
			con = Common.getConnection();		
			
			CallableStatement procedure = con.prepareCall("{CALL GetBenchmarkById(?)}");
			procedure.setLong(1, benchId);					
			ResultSet results = procedure.executeQuery();
			
			if(results.next()){
				Benchmark b = new Benchmark();
				b.setId(results.getLong("bench.id"));
				b.setUserId(results.getLong("bench.user_id"));
				b.setName(results.getString("bench.name"));
				b.setUploadDate(results.getTimestamp("bench.uploaded"));
				b.setPath(results.getString("bench.path"));
				b.setDescription(results.getString("bench.description"));
				b.setDownloadable(results.getBoolean("bench.downloadable"));
				
				BenchmarkType t = new BenchmarkType();
				t.setId(results.getLong("types.id"));
				t.setCommunityId(results.getLong("types.community"));
				t.setDescription(results.getString("types.description"));
				t.setName(results.getString("types.name"));
				t.setProcessorPath(results.getString("types.processor_path"));
				
				b.setType(t);
				return b;				
			}											
		} catch (Exception e){			
			log.error(e.getMessage(), e);		
		} finally {
			Common.safeClose(con);
		}
		
		return null;
	}
	
	/**
	 * @param spaceId The id of the space to get benchmarks for
	 * @return A list of all benchmarks belonging to the space
	 * @author Tyler Jensen
	 */
	public static List<Benchmark> getBySpace(long spaceId) {
		Connection con = null;			
		
		try {
			con = Common.getConnection();		
			CallableStatement procedure = con.prepareCall("{CALL GetSpaceBenchmarksById(?)}");
			procedure.setLong(1, spaceId);					
			ResultSet results = procedure.executeQuery();
			List<Benchmark> benchmarks = new LinkedList<Benchmark>();
			
			while(results.next()){
				Benchmark b = new Benchmark();
				b.setId(results.getLong("bench.id"));
				b.setName(results.getString("bench.name"));
				b.setUploadDate(results.getTimestamp("bench.uploaded"));
				b.setDescription(results.getString("bench.description"));
				b.setDownloadable(results.getBoolean("bench.downloadable"));	
				
				BenchmarkType t = new BenchmarkType();
				t.setId(results.getLong("types.id"));
				t.setCommunityId(results.getLong("types.community"));
				t.setDescription(results.getString("types.description"));
				t.setName(results.getString("types.name"));
				t.setProcessorPath(results.getString("types.processor_path"));
				
				b.setType(t);
				benchmarks.add(b);
			}			
						
			return benchmarks;
		} catch (Exception e){			
			log.error(e.getMessage(), e);		
		} finally {
			Common.safeClose(con);
		}
		
		return null;
	}
	
	/**
	 * Updates the details of a benchmark
	 * @param id the id of the benchmark to update
	 * @param name the new name to apply to the benchmark
	 * @param description the new description to apply to the benchmark
	 * @param isDownloadable boolean indicating whether or not this benchmark is downloadable
	 * @param benchTypeId the new benchmark type to apply to the benchmark 
	 * @return True if the operation was a success, false otherwise
	 * @author Todd Elvers
	 */
	public static boolean updateDetails(long id, String name, String description, boolean isDownloadable, long benchTypeId){
		Connection con = null;			
		
		try {
			con = Common.getConnection();
			CallableStatement procedure = con.prepareCall("{CALL UpdateBenchmarkDetails(?, ?, ?, ?, ?)}");
			procedure.setLong(1, id);
			procedure.setString(2, name);
			procedure.setString(3, description);
			procedure.setBoolean(4, isDownloadable);
			procedure.setLong(5, benchTypeId);
			
			procedure.executeUpdate();					
			log.debug(String.format("Benchmark [id=%d] was successfully updated.", id));
			return true;
		} catch (Exception e){			
			log.error(e.getMessage(), e);		
		} finally {
			Common.safeClose(con);
		}
		
		log.debug(String.format("Benchmark [id=%d] failed to be updated.", id));
		return false;
	}	
}
