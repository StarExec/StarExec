package org.starexec.data.database;

import java.io.BufferedReader;
import java.io.File;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;
import java.util.Properties;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import org.starexec.data.to.Benchmark;
import org.starexec.data.to.Processor;
import org.starexec.util.Util;

/**
 * Handles all database interaction for benchmarks.
 */
public class Benchmarks {
	private static final Logger log = Logger.getLogger(Benchmarks.class);
	public static final int NO_TYPE = 1;
	
	/**
	 * Associates the benchmarks with the given ids to the given space
	 * @param benchIds The list of benchmark ids to associate with the space
	 * @param spaceId The id of the space the benchmarks will be associated with
	 * @return True if the operation was a success, false otherwise
	 * @author Tyler Jensen
	 */
	public static boolean associate(List<Integer> benchIds, int spaceId) {
		Connection con = null;			
		
		try {
			con = Common.getConnection();	
			Common.beginTransaction(con);
			
			CallableStatement procedure = con.prepareCall("{CALL AssociateBench(?, ?)}");
			
			for(int bid : benchIds) {
				procedure.setInt(1, bid);
				procedure.setInt(2, spaceId);			
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
	 * Adds a new attribute to a benchmark
	 * @param con The connection to make the insertion on
	 * @param benchId The id of the benchmark the attribute is for
	 * @param key The key of the attribute
	 * @param val The value of the attribute
	 * @return True if the operation was a success, false otherwise
	 * @author Tyler Jensen
	 */
	protected static boolean addBenchAttr(Connection con, int benchId, String key, String val) throws Exception {
		CallableStatement procedure = con.prepareCall("{CALL AddBenchAttr(?, ?, ?)}");
		procedure.setInt(1, benchId);
		procedure.setString(2, key);
		procedure.setString(3, val);
		procedure.executeUpdate();
		return true;
	}
	
	/**
	 * Adds a single benchmark to the database under the given spaceId
	 * @param benchmark The benchmark to add to the database
	 * @param spaceId The id of the space the benchmark will belong to
	 * @return True if the operation was a success, false otherwise
	 * @author Tyler Jensen
	 */
	public static boolean add(Benchmark benchmark, int spaceId) {
		Connection con = null;			
		
		try {
			con = Common.getConnection();
			Common.beginTransaction(con);
			
			// Add benchmark to database
			boolean benchAdded = Benchmarks.add(con, benchmark, spaceId);
			
			if(benchAdded){
				Common.endTransaction(con);
				return true;
			} else {
				Common.doRollback(con);
				return false;
			}
		} catch (Exception e){
			Common.doRollback(con);
			log.error(e.getMessage(), e);		
		} finally {
			Common.safeClose(con);
		}
		
		return false;
	}
	
	/**
	 * Adds the list of benchmarks to the database and associates them with the given spaceId.
	 * The benchmark types are also processed based on the type of the first benchmark only.
	 * @param benchmarks The list of benchmarks to add
	 * @param spaceId The space the benchmarks will belong to
	 * @return True if the operation was a success, false otherwise
	 * @author Tyler Jensen
	 */
	public static boolean add(List<Benchmark> benchmarks, int spaceId) {
		Connection con = null;			
		
		try {			
			con = Common.getConnection();
			
			Common.beginTransaction(con);
			
			// Get the processor of the first benchmark (they should all have the same processor)
			Processor p = Processors.get(con, benchmarks.get(0).getType().getId());
			
			// Process the benchmark for attributes (this must happen BEFORE they are added to the database)
			Benchmarks.attachBenchAttrs(benchmarks, p);
			
			// Next add them to the database (must happen AFTER they are processed);
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
	 * Given a set of benchmarks and a processor, this method runs each benchmark through
	 * the processor and adds a hashmap of attributes to the benchmark that are given from
	 * the processor.
	 * @param benchmarks The set of benchmarks to get attributes for
	 * @param p The processor to run each benchmark on
	 */
	protected static void attachBenchAttrs(List<Benchmark> benchmarks, Processor p) {
		log.debug("Beginning processing for " + benchmarks.size() + " benchmarks");			
			
		// For each benchmark in the list to process...
		for(Benchmark b : benchmarks) {
			BufferedReader reader = null;
			
			try {
				// Run the processor on the benchmark file
				reader = Util.executeCommand(p.getFilePath() + " " + b.getPath());
				
				// Load results into a properties file
				Properties prop = new Properties();
				prop.load(reader);							
				
				// Attach the attributes to the benchmark
				b.setAttributes(prop);
			} catch (Exception e) {
				log.warn(e.getMessage(), e);
			} finally {
				if(reader != null) {
					try { reader.close(); } catch(Exception e) {}
				}
			}
		}
	}	
	
	/**
	 * Internal method which adds a single benchmark to the database under the given spaceId
	 * @param con The connection the operation will take place on
	 * @param benchmark The benchmark to add to the database
	 * @param spaceId The id of the space the benchmark will belong to
	 * @return True if the operation was a success, false otherwise
	 * @author Tyler Jensen
	 */
	protected static boolean add(Connection con, Benchmark benchmark, int spaceId) throws Exception {				
		CallableStatement procedure = null;			
		Properties attrs = benchmark.getAttributes();
		
		// Setup normal information for the benchmark
		procedure = con.prepareCall("{CALL AddBenchmark(?, ?, ?, ?, ?, ?, ?, ?)}");
		procedure.setString(1, benchmark.getName());		
		procedure.setString(2, benchmark.getPath());
		procedure.setBoolean(3, benchmark.isDownloadable());
		procedure.setInt(4, benchmark.getUserId());			
		procedure.setInt(5, Benchmarks.isBenchValid(attrs) ? benchmark.getType().getId() : Benchmarks.NO_TYPE);
		procedure.setInt(6, spaceId);
		procedure.setLong(7, FileUtils.sizeOf(new File(benchmark.getPath())));
		procedure.registerOutParameter(8, java.sql.Types.INTEGER);
		
		// Execute procedure and get back the benchmark's id
		procedure.executeUpdate();		
		benchmark.setId(procedure.getInt(8));
		
		// If the benchmark is valid according to its processor...
		if(Benchmarks.isBenchValid(attrs)) {
			// Discard the valid attribute, we don't need it
			attrs.remove("starexec-valid");
			
			// For each attribute (key, value)...
			for(Entry<Object, Object> keyVal : attrs.entrySet()) {
				// Add the attribute to the database
				Benchmarks.addBenchAttr(con, benchmark.getId(), (String)keyVal.getKey(), (String)keyVal.getValue());
			}							
		}				
		
		return true;
	}
	
	/**
	 * Internal helper method to determine if a benchmark is valid according to its attributes
	 */
	private static boolean isBenchValid(Properties attrs) {
		// A benchmark is valid if it has attributes and it has the special starexec-valid attribute
		return (attrs != null && Boolean.parseBoolean(attrs.getProperty("starexec-valid", "false")));
	}
	
	/**
	 * Internal method which adds the list of benchmarks to the database and associates them with the given spaceId
	 * @param con The connection the operation will take place on
	 * @param benchmarks The list of benchmarks to add
	 * @param spaceId The space the benchmarks will belong to
	 * @return True if the operation was a success, false otherwise
	 * @author Tyler Jensen
	 */
	protected static void add(Connection con, List<Benchmark> benchmarks, int spaceId) throws Exception {		
		for(Benchmark b : benchmarks) {
			if(!Benchmarks.add(con, b, spaceId)) {
				throw new Exception(String.format("Failed to add benchmark [%s] to space [%d]", b.getName(), spaceId));
			}
		}
		
		log.debug(String.format("[%d] new benchmarks added to space [%d]", benchmarks.size(), spaceId));
	}
	
	/**
	 * Deletes a benchmark from the database (cascading deletes handle all dependencies)
	 * @param id the id of the benchmark to delete
	 * @return True if the operation was a success, false otherwise
	 * @author Todd Elvers
	 */
	public static boolean delete(int id){
		Connection con = null;			
		File benchToDelete = null;
		try {
			con = Common.getConnection();
			
			CallableStatement procedure = con.prepareCall("{CALL DeleteBenchmarkById(?, ?)}");
			procedure.setInt(1, id);
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
	 * Retrieves a benchmark without attributes
	 * @param benchId The id of the benchmark to retrieve
	 * @return A benchmark object representing the benchmark with the given ID
	 * @author Tyler Jensen
	 */
	public static Benchmark get(int benchId) {
		return Benchmarks.get(benchId, false);
	}
	
	/**
	 * @param benchId The id of the benchmark to retrieve
	 * @param includeAttrs Whether or not to to get this benchmark's attributes
	 * @return A benchmark object representing the benchmark with the given ID
	 * @author Tyler Jensen
	 */
	public static Benchmark get(int benchId, boolean includeAttrs) {
		Connection con = null;			
		
		try {
			con = Common.getConnection();		
			Benchmark b = Benchmarks.get(con, benchId);
			
			if(true == includeAttrs){
				b.setAttributes(Benchmarks.getAttributes(con, b.getId()));
			}
			
			return b;
		} catch (Exception e){			
			log.error(e.getMessage(), e);		
		} finally {
			Common.safeClose(con);
		}
		
		return null;
	}
	
	/**
	 * Retrieves all attributes (key/value) of the given benchmark
	 * @param benchId The id of the benchmark to get the attributes of
	 * @return The properties object which holds all the benchmark's attributes
	 * @author Tyler Jensen
	 */
	public static Properties getAttributes(int benchId) {
		Connection con = null;			
		
		try {
			con = Common.getConnection();		
			return Benchmarks.getAttributes(con, benchId);
		} catch (Exception e){			
			log.error(e.getMessage(), e);		
		} finally {
			Common.safeClose(con);
		}
		
		return null;
	}
	
	/**
	 * Retrieves all attributes (key/value) of the given benchmark
	 * @param con The connection to make the query on
	 * @param benchId The id of the benchmark to get the attributes of
	 * @return The properties object which holds all the benchmark's attributes
	 * @author Tyler Jensen
	 */
	protected static Properties getAttributes(Connection con, int benchId) throws Exception {
		CallableStatement procedure = con.prepareCall("{CALL GetBenchAttrs(?)}");
		procedure.setInt(1, benchId);					
		ResultSet results = procedure.executeQuery();
		
		Properties prop = new Properties();
		
		while(results.next()){
			prop.put(results.getString("attr_key"), results.getString("attr_value"));				
		}			
		
		if(prop.size() <= 0) {
			prop = null;
		}
		
		return prop;
	}
	
	/**
	 * @param con The connection to query with
	 * @param benchId The id of the benchmark to retrieve
	 * @return A benchmark object representing the benchmark with the given ID
	 * @author Tyler Jensen
	 */
	protected static Benchmark get(Connection con, int benchId) throws Exception {					
		CallableStatement procedure = con.prepareCall("{CALL GetBenchmarkById(?)}");
		procedure.setInt(1, benchId);					
		ResultSet results = procedure.executeQuery();
		
		if(results.next()){
			Benchmark b = new Benchmark();
			b.setId(results.getInt("bench.id"));
			b.setUserId(results.getInt("bench.user_id"));
			b.setName(results.getString("bench.name"));
			b.setUploadDate(results.getTimestamp("bench.uploaded"));
			b.setPath(results.getString("bench.path"));
			b.setDescription(results.getString("bench.description"));
			b.setDownloadable(results.getBoolean("bench.downloadable"));
			b.setDiskSize(results.getLong("bench.disk_size"));
			
			Processor t = new Processor();
			t.setId(results.getInt("types.id"));
			t.setCommunityId(results.getInt("types.community"));
			t.setDescription(results.getString("types.description"));
			t.setName(results.getString("types.name"));
			t.setFilePath(results.getString("types.path"));
			t.setDiskSize(results.getLong("types.disk_size"));
			
			b.setType(t);
			return b;				
		}													
		
		return null;
	}
	
	/**
	 * @param benchIds A list of ids to get benchmarks for
	 * @return A list of benchmark object representing the benchmarks with the given IDs
	 * @author Tyler Jensen
	 */
	public static List<Benchmark> get(List<Integer> benchIds) {
		Connection con = null;			
		
		try {
			con = Common.getConnection();					
			List<Benchmark> benchList = new ArrayList<Benchmark>();
			
			for(int id : benchIds) {				
					benchList.add(Benchmarks.get(con, id));
			}
			
			return benchList;
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
	public static List<Benchmark> getBySpace(int spaceId) {
		Connection con = null;			
		
		try {
			con = Common.getConnection();		
			CallableStatement procedure = con.prepareCall("{CALL GetSpaceBenchmarksById(?)}");
			procedure.setInt(1, spaceId);					
			ResultSet results = procedure.executeQuery();
			List<Benchmark> benchmarks = new LinkedList<Benchmark>();
			
			while(results.next()){
				Benchmark b = new Benchmark();
				b.setId(results.getInt("bench.id"));
				b.setName(results.getString("bench.name"));
				b.setUploadDate(results.getTimestamp("bench.uploaded"));
				b.setDescription(results.getString("bench.description"));
				b.setDownloadable(results.getBoolean("bench.downloadable"));	
				b.setDiskSize(results.getLong("bench.disk_size"));
				
				Processor t = new Processor();
				t.setId(results.getInt("types.id"));
				t.setCommunityId(results.getInt("types.community"));
				t.setDescription(results.getString("types.description"));
				t.setName(results.getString("types.name"));
				t.setFilePath(results.getString("types.path"));
				t.setDiskSize(results.getLong("types.disk_size"));
				
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
	 * Returns a list of benchmarks owned by a given user
	 * 
	 * @param userId the id of the user who is the owner of the benchmarks we are to retrieve
	 * @return a list of benchmarks owned by a given user, may be empty
	 * @author Todd Elvers
	 */
	public static List<Benchmark> getByOwner(int userId) {
		Connection con = null;			
		
		try {
			con = Common.getConnection();		
			CallableStatement procedure = con.prepareCall("{CALL GetBenchmarksByOwner(?)}");
			procedure.setInt(1, userId);					
			ResultSet results = procedure.executeQuery();
			List<Benchmark> benchmarks = new LinkedList<Benchmark>();
			
			
			while(results.next()){
				// Build benchmark object
				Benchmark b = new Benchmark();
				b.setId(results.getInt("id"));
				b.setName(results.getString("name"));
				b.setPath(results.getString("path"));
				b.setUploadDate(results.getTimestamp("uploaded"));
				b.setDescription(results.getString("description"));
				b.setDownloadable(results.getBoolean("downloadable"));
				b.setDiskSize(results.getLong("disk_size"));
				
				// Add benchmark object to listOfBenchmarks
				benchmarks.add(b);
			}			
			
			log.debug(String.format("%d benchmarks were returned as being owned by user %d.", benchmarks.size(), userId));
			
			return benchmarks;
		} catch (Exception e){			
			log.error(e.getMessage(), e);		
		} finally {
			Common.safeClose(con);
		}
		
		log.debug(String.format("Getting the benchmarks owned by user %d failed.", userId));
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
	public static boolean updateDetails(int id, String name, String description, boolean isDownloadable, int benchTypeId){
		Connection con = null;			
		
		try {
			con = Common.getConnection();
			CallableStatement procedure = con.prepareCall("{CALL UpdateBenchmarkDetails(?, ?, ?, ?, ?)}");
			procedure.setInt(1, id);
			procedure.setString(2, name);
			procedure.setString(3, description);
			procedure.setBoolean(4, isDownloadable);
			procedure.setInt(5, benchTypeId);
			
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
	
	/**
	 * Retrieves the contents of a benchmark file from disk as a string
	 * @param b The benchmark to get the contents of (must have a valid path)
	 * @param limit the maximum number of lines to return
	 * @return The file contents as a string
	 */
	public static String getContents(Benchmark b, int limit) {
		File file = new File(b.getPath());
		return Util.readFileLimited(file, limit);
	}
	
	/**
	 * Retrieves the contents of a benchmark file from disk as a string
	 * @param benchId The id of the benchmark to get the contents of
	 * @param limit the maximum number of lines to return
	 * @return The file contents as a string
	 */
	public static String getContents(int benchId, int limit) {
		return Benchmarks.getContents(Benchmarks.get(benchId), limit);
	}
}
