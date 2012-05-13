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
import org.starexec.data.to.BenchmarkDependency;
import org.starexec.data.to.Processor;
import org.starexec.data.to.Space;
import org.starexec.data.to.Solver;
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
	 * The benchmark types are also processed based on the type of the first benchmark only.  This method assumes
	 * we are not introducing benchmark dependencies.
	 * @param benchmarks The list of benchmarks to add
	 * @param spaceId The space the benchmarks will belong to
	 * @return True if the operation was a success, false otherwise
	 * @author Tyler Jensen
	 */
	public static boolean add(List<Benchmark> benchmarks, int spaceId) {
		Connection con = null;			
		if (benchmarks.size()>0)
		{
			try {			
				con = Common.getConnection();

				Common.beginTransaction(con);

				log.info(benchmarks.size() + " benchmarks being added to space " + spaceId);
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
		}
		else
		{
			log.info("No benchmarks to add here for space " + spaceId);
			return true;
		}
		return false;
	}	

	/**
	 * Adds the list of benchmarks to the database and associates them with the given spaceId.
	 * The benchmark types are also processed based on the type of the first benchmark only.
	 * This method will also introduced dependencies if the benchmark processor produces the right attributes.
	 * @param benchmarks The list of benchmarks to add
	 * @param spaceId The space the benchmarks will belong to
	 * @param con database connection
	 * @param depRootSpaceId the id of the space where the axiom benchmarks lie
	 * @param linked true if the depRootSpace is the same as the first directory in the include statement
	 * @param userId the user's Id
	 * @return True if the operation was a success, false otherwise
	 * @author Benton McCune
	 */
	public static boolean addWithDeps(List<Benchmark> benchmarks, int spaceId, Connection con, Integer depRootSpaceId, Boolean linked, Integer userId) {
		//Connection con = null;			
		if (benchmarks.size()>0){
			try {			
				//con = Common.getConnection();

				//Common.beginTransaction(con);
				log.info("Adding (with deps) " + benchmarks.size() + " to Space " + spaceId);
				// Get the processor of the first benchmark (they should all have the same processor)
				Processor p = Processors.get(con, benchmarks.get(0).getType().getId());
				log.info("About to attach attributes to " + benchmarks.size());
				// Process the benchmark for attributes (this must happen BEFORE they are added to the database)
				Benchmarks.attachBenchAttrs(benchmarks, p);
				log.info("About to add " + benchmarks.size() + " benchmarks to space " + spaceId);
				// Next add them to the database (must happen AFTER they are processed);
				Benchmarks.add(con, benchmarks, spaceId);		

				// Process the benchmark for dependencies (must happen after they are added so that dependencies will have bench ids)
				log.info("About to introduce dependencies to " + benchmarks.size() + " benchmarks to space " + spaceId);
				Benchmarks.introduceDependencies(benchmarks, depRootSpaceId, linked, userId, con);

				//Common.endTransaction(con);

				return true;
			} catch (Exception e){			
				log.error("Need to roll back - addWithDeps says" + e.getMessage(), e);
				Common.doRollback(con);
			} finally {
				//Common.safeClose(con);
			}
		}
		else
		{
			log.info("No benches to add with this call to addWithDeps from space " + spaceId);
			return true;
		}
		return false;
	}	

	/**
	 * Adds the list of benchmarks to the database and associates them with the given spaceId.
	 * The benchmark types are also processed based on the type of the first benchmark only.
	 * This method will also introduced dependencies if the benchmark processor produces the right attributes.
	 * @param benchmarks The list of benchmarks to add
	 * @param spaceId The space the benchmarks will belong to
	 * @param depRootSpaceId the id of the space where the axiom benchmarks lie
	 * @param linked true if the depRootSpace is the same as the first directory in the include statement
	 * @param userId the user's Id
	 * @return True if the operation was a success, false otherwise
	 * @author Benton McCune
	 */
	public static boolean addWithDeps(List<Benchmark> benchmarks, int spaceId, Integer depRootSpaceId, Boolean linked, Integer userId) {
		Connection con = null;			
		log.info("Going to add " + benchmarks.size() + "benchmarks (with dependencies) to space " + spaceId);
		try {			
			con = Common.getConnection();

			Common.beginTransaction(con);

			Boolean value = addWithDeps(benchmarks, spaceId, con, depRootSpaceId, linked, userId);

			Common.endTransaction(con);

			return value;
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
		log.info("Beginning processing for " + benchmarks.size() + " benchmarks");			

		// For each benchmark in the list to process...
		for(Benchmark b : benchmarks) {
			BufferedReader reader = null;

			try {
				// Run the processor on the benchmark file
				reader = Util.executeCommand(p.getFilePath() + " " + b.getPath());
				log.debug("reader is null = " + (reader == null));
				if (reader == null){
					log.error("Reader is null!");
				}
				// Load results into a properties file
				Properties prop = new Properties();
				if (reader != null){
				prop.load(reader);							
				reader.close();
				}
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
		log.info("adding benchmark " + benchmark.getName() + "to space " + spaceId);
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
			log.info("bench is valid.  Adding " + attrs.entrySet().size() + " attributes");
			// For each attribute (key, value)...
			int count = 0;
			for(Entry<Object, Object> keyVal : attrs.entrySet()) {
				// Add the attribute to the database
				count++;
				log.info("Adding att number " + count + " " + (String)keyVal.getKey() +", " + (String)keyVal.getValue() + " to bench " + benchmark.getId());
				Benchmarks.addBenchAttr(con, benchmark.getId(), (String)keyVal.getKey(), (String)keyVal.getValue());
			}							

		}				
		log.info("(within internal add method) Added Benchmark " + benchmark.getName());
		return true;
	}

	/**
	 * Internal method which adds a single benchmark with dependencies to the database under the given spaceId
	 * @param con The connection the operation will take place on
	 * @param benchmark The benchmark to add to the database
	 * @param spaceId The space the benchmark will belong to
	 * @param con database connection
	 * @param depRootSpaceId the id of the space where the axiom benchmarks lie
	 * @param linked true if the depRootSpace is the same as the first directory in the include statement
	 * @param userId the user's Id
	 * @return True if the operation was a success, false otherwise
	 * @author Benton McCune
	 */
	protected static boolean addWithDependencies(Connection con, Benchmark benchmark, int spaceId, int depRootSpaceId, Boolean linked, int userId) throws Exception {				
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
			if (Benchmarks.hasDependencies(attrs)){
				//add dependencies
				introduceDependencies(benchmark, depRootSpaceId, linked, userId, con);
			}
		}				
		log.info("Adding (with dependencies) Benchmark " + benchmark.getName());
		return true;
	}

	private static boolean hasDependencies(Properties attrs) {
		// TODO Auto-generated method stub
		return false;
	}
	/*	
	//need method to call this for a whole space
	private static boolean introduceDependenciesSpace(Integer spaceId, Integer depRootSpaceId, Boolean linked, Integer userId){
		List<Benchmark> benchmarks = Benchmarks.getBySpace(spaceId);
		for (Benchmark bench:benchmarks){
			introduceDependencies(bench, depRootSpaceId, linked, userId);
		}
		List<Space> subSpaces = Spaces.getSubSpaces(spaceId, userId);
		//Call for entire space hierarchy
		for (Space subSpace:subSpaces){
			introduceDependenciesSpace(subSpace.getId(), depRootSpaceId, linked, userId);
		}
		return false;
	}
	 */	
	/**
	 * Introduces the dependencies for a list of benchmarks
	 * @param benchmarks The list of benchmarks that might have dependencies
	 * @param con database connection
	 * @param depRootSpaceId the id of the space where the axiom benchmarks lie
	 * @param linked true if the depRootSpace is the same as the first directory in the include statement
	 * @param userId the user's Id
	 * @return True if the operation was a success, false otherwise
	 * @author Benton McCune
	 */
	private static boolean introduceDependencies(List<Benchmark> benchmarks, Integer depRootSpaceId, Boolean linked, Integer userId, Connection con){
		log.info("Introducing dependencies for " + benchmarks.size() + " benchmarks" );
		for (Benchmark bench:benchmarks){
			introduceDependencies(bench, depRootSpaceId, linked, userId, con);
		}

		return false;
	}

	/**
	 * Introduces the dependencies for a list of benchmarks
	 * @param benchmarks The list of benchmarks that might have dependencies
	 * @param con database connection
	 * @param spaceId the id of the space where the axiom benchmarks lie
	 * @param linked true if the depRootSpace is the same as the first directory in the include statement
	 * @param userId the user's Id
	 * @return True if the operation was a success, false otherwise
	 * @author Benton McCune
	 */
	private static boolean introduceDependencies(Benchmark bench, Integer spaceId, Boolean linked, Integer userId, Connection con){

		Properties atts = bench.getAttributes();
		// A benchmark is valid if it has attributes and it has the special starexec-valid attribute
		//		return (attrs != null && Boolean.parseBoolean(attrs.getProperty("starexec-valid", "false")));
		Integer numberDependencies = 0;
		String includePath = "";
		if (!Permissions.canUserSeeBench(bench.getId(), userId)){
			log.debug("User " + userId + " cannot see bench " +bench.getId());
			return false;
		}			
		try {
			numberDependencies = Integer.valueOf(atts.getProperty("starexec-dependencies", "0"));
			log.debug("# of dependencies = " + numberDependencies);
			for (int i = 1; i <= numberDependencies; i++){
				includePath = atts.getProperty("starexec-dependency-"+i, "");
				log.debug("Dependency Path of Dependency " + i + " is " + includePath);
				Integer depBenchId = -1;
				if (includePath.length()>0){
					depBenchId = Benchmarks.findDependentBench(spaceId,includePath, linked, userId);
					log.debug("Dependent Bench = " + depBenchId);
				}
				if (depBenchId>-1)
				{
					if (Permissions.canUserSeeBench(depBenchId, userId)){
						log.info("Adding dependency, Bench " + bench.getId() + " dependent on " + depBenchId);
						Benchmarks.addBenchDependency(bench.getId(),depBenchId, includePath, con);
					}
				}
			}		
		}
		catch (Exception e){		
			log.error(e.getMessage(), e);	
			Common.doRollback(con);
		}		
		return true;
	}

	// just for testing locally
	public static boolean testDepCode(Benchmark bench, int userId){
		Connection con = null;
		log.debug("Testing Dep Code on " + bench.getName());
		try {	
			con = Common.getConnection();
			Common.beginTransaction(con);		
			Boolean success = introduceDependencies(bench, 6, true, userId, con);
			log.debug("Dependencies introduced = " +success);
			Common.endTransaction(con);
		}catch (Exception e){			
			log.error(e.getMessage(), e);
			Common.doRollback(con);
		} finally {
			log.debug("safe closing connection.");
			Common.safeClose(con);
		}
		return true;
	}

	/**
	 * Adds the benchmark dependency to starexec db
	 * @param primaryBenchId  the bench that is dependent on another bench
	 * @param secondaryBenchId  e.g. the axiom
	 * @param includePath  the path that will be used locally at execution time
	 * @param con db connection
	 * @author Benton McCune
	 * @return
	 */
	private static Boolean addBenchDependency(int primaryBenchId, Integer secondaryBenchId,
			String includePath, Connection con) {

		//Connection con = null;
		try {	
			//con = Common.getConnection();
			//Common.beginTransaction(con);
			CallableStatement procedure = null;			

			log.debug("Adding dependency");
			log.debug("primaryBenchId = " + primaryBenchId);
			log.debug("secondaryBenchId = " + secondaryBenchId);
			log.debug("includePath = " + includePath);
			// Setup normal information for the benchmark dependency
			procedure = con.prepareCall("{CALL AddBenchDependency(?, ?, ?)}");
			procedure.setInt(1, primaryBenchId);		
			procedure.setInt(2, secondaryBenchId);
			procedure.setString(3, includePath);

			// Execute procedure and get back the benchmark's id
			procedure.executeUpdate();		

			return true;
		}catch (Exception e){			
			log.error(e.getMessage(), e);
			Common.doRollback(con);
		} finally {
			//Common.safeClose(con);
		}
		return false;

	}

	/**
	 * Returns the bench id of the axiom file that is being looked for, -1 if not found or too many by that name
	 * @param spaceId the dependent bench root space
	 * @param includePath  the path that will be used to drill down and find bench
	 * @param linked true if the depRootSpace is the same as the first directory in the include statement
	 * @return benchId
	 * @author Benton McCune	
	 */
	private static Integer findDependentBench(Integer spaceId,
			String includePath, Boolean linked, Integer userId) {
		// TODO Auto-generated method stub
		String[] spaces = includePath.split("/");//splitting up path
		log.debug("Length of spaces string array = " +spaces.length);
		if (spaces.length == 0)
		{
			return -1;
		}
		int startIndex = (linked) ? 1:0;//if linked, skip the first directory in path 
		int index = startIndex;
		log.debug("First Space(or Bench) to look for = " + spaces[index]);
		//List<Space> subSpaces;
		Integer currentSpaceId = spaceId;
		log.debug("Current Space Id = " + currentSpaceId);
		//dig through subspaces while you have to
		while ((index < (spaces.length-1)) && (currentSpaceId > -1))
		{
			log.info("Looking for SubSpace " + spaces[index] +" in Space " + currentSpaceId);
			currentSpaceId = Spaces.getSubSpaceIDbyName(currentSpaceId, userId, spaces[index]); 
			log.info("Returned with subspace " + currentSpaceId);
			index++;
		}
		//now find bench in the subspace you've found
		if (currentSpaceId > 1)
		{
			log.info("Looking for Benchmark " + spaces[index] +" in Space " + currentSpaceId);
			Integer benchId = Benchmarks.getBenchIdByName(currentSpaceId, spaces[index]);
			log.info("Returned with bench " + benchId);
			return benchId;
		}
		return -1;
	}
	/**
	 * returns the benchId of a benchmark with a specific name in a given space (-1 if not found or more than one)
	 * @param spaceId space that bench should be in
	 * @param benchName name of bench
	 * @return benchId
	 * @author Benton McCune
	 */
	private static Integer getBenchIdByName(Integer spaceId, String benchName) {
		// TODO Auto-generated method stub
		Connection con = null;			
		log.debug("(Within Method) Looking for Benchmark " + benchName +" in Space " + spaceId);
		try {
			con = Common.getConnection();		
			CallableStatement procedure = con.prepareCall("{CALL GetBenchByName(?,?)}");
			procedure.setInt(1, spaceId);
			procedure.setString(2, benchName);

			ResultSet results = procedure.executeQuery();
			Integer benchId = -1;

			if(results.next()){
				Benchmark b = new Benchmark();
				benchId = (results.getInt("bench.id"));
				log.debug("Bench Id = " + benchId);
			}		
			results.last();
			Integer numResults = results.getRow();
			log.debug("# of Benchmarks with this name = " + numResults);

			return benchId;
		} catch (Exception e){			
			log.error(e.getMessage(), e);		
		} finally {
			Common.safeClose(con);
		}
		return -1;
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
		log.info("in add method - adding " + benchmarks.size()  + " benchmarks to space " + spaceId);
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
	 * Returns a list of benchmark dependencies that have the input benchmark as the primary benchmark
	 * 
	 * @param benchmarkId the id of the primary benchmark
	 * @return a list of benchmark dependencies for a given benchmark, may be empty
	 * @author Benton McCune
	 */
	public static List<BenchmarkDependency> getBenchDependencies(int benchmarkId) {
		Connection con = null;			

		try {
			con = Common.getConnection();		
			CallableStatement procedure = con.prepareCall("{CALL getBenchmarkDependencies(?)}");
			procedure.setInt(1, benchmarkId);					
			ResultSet results = procedure.executeQuery();
			List<BenchmarkDependency> dependencies = new LinkedList<BenchmarkDependency>();

			while(results.next()){
				// Build benchmark dependency object

				BenchmarkDependency benchD = new BenchmarkDependency();
				benchD.setPrimaryBench(Benchmarks.get(results.getInt("primary_bench_id")));
				benchD.setSecondaryBench(Benchmarks.get(results.getInt("secondary_bench_id")));
				benchD.setDependencyPath(results.getString("include_path"));

				// Add benchmark dependency object to list of dependencies
				dependencies.add(benchD);
			}			

			log.debug(String.format("%d benchmarks were returned as being needed by benchmark %d.", dependencies.size(), benchmarkId));

			return dependencies;
		} catch (Exception e){			
			log.error(e.getMessage(), e);		
		} finally {
			Common.safeClose(con);
		}

		log.debug(String.format("Getting the dependencies of benchmark %d failed.", benchmarkId));
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


	/**
	 * Gets the minimal number of Benchmarks necessary in order to service the client's
	 * request for the next page of Benchmarks in their DataTables object
	 * 
	 * @param startingRecord the record to start getting the next page of Benchmarks from
	 * @param recordsPerPage how many records to return (i.e. 10, 25, 50, or 100 records)
	 * @param isSortedASC whether or not the selected column is sorted in ascending or descending order 
	 * @param indexOfColumnSortedBy the index representing the column that the client has sorted on
	 * @param searchQuery the search query provided by the client (this is the empty string if no search query was inputed)
	 * @param spaceId the id of the space to get the Benchmarks from
	 * @return a list of 10, 25, 50, or 100 Benchmarks containing the minimal amount of data necessary
	 * @author Todd Elvers
	 */
	public static List<Benchmark> getBenchmarksForNextPage(int startingRecord, int recordsPerPage, boolean isSortedASC, int indexOfColumnSortedBy,  String searchQuery, int spaceId) {
		Connection con = null;			

		try {
			con = Common.getConnection();
			CallableStatement procedure;			

			procedure = con.prepareCall("{CALL GetNextPageOfBenchmarks(?, ?, ?, ?, ?, ?)}");
			procedure.setInt(1, startingRecord);
			procedure.setInt(2,	recordsPerPage);
			procedure.setInt(3, indexOfColumnSortedBy);
			procedure.setBoolean(4, isSortedASC);
			procedure.setInt(5, spaceId);
			procedure.setString(6, searchQuery);

			ResultSet results = procedure.executeQuery();
			List<Benchmark> benchmarks = new LinkedList<Benchmark>();

			while(results.next()){
				Benchmark b = new Benchmark();
				b.setId(results.getInt("id"));
				b.setName(results.getString("name"));
				b.setDescription(results.getString("description"));

				Processor t = new Processor();
				t.setDescription(results.getString("benchTypeDescription"));
				t.setName(results.getString("benchTypeName"));

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
	 * Gets the number of Benchmarks in a given space
	 * 
	 * @param spaceId the id of the space to count the Benchmarks in
	 * @return the number of Benchmarks
	 * @author Todd Elvers
	 */
	public static int getCountInSpace(int spaceId) {
		Connection con = null;

		try {
			con = Common.getConnection();
			CallableStatement procedure = con.prepareCall("{CALL GetBenchmarkCountInSpace(?)}");
			procedure.setInt(1, spaceId);
			ResultSet results = procedure.executeQuery();

			if (results.next()) {
				return results.getInt("benchCount");
			}
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		} finally {
			Common.safeClose(con);
		}

		return 0;
	}
}
