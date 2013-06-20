package org.starexec.data.database;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.ResultSet;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.TreeMap;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import org.starexec.constants.R;
import org.starexec.data.to.Benchmark;
import org.starexec.data.to.BenchmarkDependency;
import org.starexec.data.to.Permission;
import org.starexec.data.to.Processor;
import org.starexec.data.to.Space;
import org.starexec.util.DependValidator;
import org.starexec.util.Util;
import org.starexec.util.Validator;

/**
 * Handles all database interaction for benchmarks.
 */
public class Benchmarks {
	private static final Logger log = Logger.getLogger(Benchmarks.class);
	public static final int NO_TYPE = 1;
	private static DateFormat shortDate = new SimpleDateFormat(R.PATH_DATE_FORMAT); 
	
	/**
	 * Associates the benchmarks with the given ids to the given space
	 * @param benchIds The list of benchmark ids to associate with the space
	 * @param spaceId The id of the space the benchmarks will be associated with
	 * @return True if the operation was a success, false otherwise
	 * @author Tyler Jensen
	 */
	public static boolean associate(List<Integer> benchIds, int spaceId) {
		Connection con = null;			
		CallableStatement procedure = null;

		try {
			con = Common.getConnection();	
			Common.beginTransaction(con);

			procedure = con.prepareCall("{CALL AssociateBench(?, ?)}");

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
			Common.safeClose(procedure);
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
	    CallableStatement procedure = null;
	    try {
		procedure = con.prepareCall("{CALL AddBenchAttr(?, ?, ?)}");
		procedure.setInt(1, benchId);
		procedure.setString(2, key);
		procedure.setString(3, val);
		procedure.executeUpdate();
	    }
	    finally {
		Common.safeClose(procedure);
	    }
	    return true;
	}

	protected static boolean addBenchAttrTen(Connection con, int benchId, Entry<Object,Object>[] entryArray , int index) throws Exception {
	    CallableStatement procedure = null;
	    try {
		procedure = con.prepareCall("{CALL AddBenchAttrTen(?, ?, ?,?, ?, ?,?, ?, ?,?, ?, ?,?, ?, ?,?, ?, ?,?, ?, ?,?, ?, ?,?, ?, ?,?, ?, ?)}");

		for (int i = index; i < index+10; i++)
		{
			procedure.setInt((i-index)*3+1, benchId);
			procedure.setString((i-index)*3+2, (String)entryArray[i].getKey());
			procedure.setString((i-index)*3+3, (String)entryArray[i].getValue());
		}
		procedure.executeUpdate();
	    } 
	    finally {
		Common.safeClose(procedure);
	    }
	    return true;
	}

	/**
	 * Adds a single benchmark to the database under the given spaceId
	 * @param benchmark The benchmark to add to the database
	 * @param spaceId The id of the space the benchmark will belong to
	 * @return The new benchmark ID on success, -1 otherwise
	 * @author Tyler Jensen
	 */
	public static int add(Benchmark benchmark, int spaceId) throws Exception{
		if (Benchmarks.isBenchValid(benchmark.getAttributes())){
			Connection con = null;			
			try {
				con = Common.getConnection();
				Common.beginTransaction(con);

				// Add benchmark to database
				int benchId = Benchmarks.add(con, benchmark, spaceId);

				if(benchId>=0){
					//Common.endTransaction(con);
					log.debug("bench successfully added");
					return benchId;
				} else {
					//will throw exception in calling method
					return -1;
				}
			} catch (Exception e){
				Common.doRollback(con);
				log.error(e.getMessage(), e);	
				throw e;
				
			} finally {
				Common.safeClose(con);
			}
		} else {
			log.debug("Add called on invalid benchmark, no additions will be made to the database");
		}
		return -1;
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
	public static boolean add(List<Benchmark> benchmarks, int spaceId, int statusId) {
		log.info("adding list of benchmarks to space " + spaceId);
		Connection con = null;			
		if (benchmarks.size()>0)
		{
			try {			
				con = Common.getConnection();

				Common.beginTransaction(con);

				log.info(benchmarks.size() + " benchmarks being added to space " + spaceId);
				// Get the processor of the first benchmark (they should all have the same processor)
				Processor p = Processors.get(con, benchmarks.get(0).getType().getId());
				Common.endTransaction(con);
				// Process the benchmark for attributes (this must happen BEFORE they are added to the database)
				Benchmarks.attachBenchAttrs(benchmarks, p, statusId);

				// Next add them to the database (must happen AFTER they are processed);
				//Benchmarks.add(con, benchmarks, spaceId);		
				Benchmarks.addNoCon(benchmarks, spaceId, statusId);
				//Common.endTransaction(con);

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
	@SuppressWarnings("unused")
	public static boolean addWithDeps(List<Benchmark> benchmarks, int spaceId, Connection conParam, Integer depRootSpaceId, Boolean linked, Integer userId, Integer statusId) {
		Connection con = null;			
		if (benchmarks.size()>0){
			try {			
				con = Common.getConnection();
				Common.beginTransaction(con);
				log.info("Adding (with deps) " + benchmarks.size() + " to Space " + spaceId);
				// Get the processor of the first benchmark (they should all have the same processor)
				Processor p = Processors.get(con, benchmarks.get(0).getType().getId());
				Common.endTransaction(con);
				
				log.info("About to attach attributes to " + benchmarks.size());
				// Process the benchmark for attributes (this must happen BEFORE they are added to the database)
				Benchmarks.attachBenchAttrs(benchmarks, p, statusId);

				//Datastructure to make sure dependencies are all valid before benchmarks are uploaded.
				DependValidator dataStruct = new DependValidator();
				HashMap<Integer, ArrayList<String>> pathMap = new HashMap<Integer, ArrayList<String>>();//Map from primary bench Id  to the array list of dependency paths 
				HashMap<Integer, ArrayList<Integer>> axiomMap = new HashMap<Integer, ArrayList<Integer>>();//Map from primary bench Id to the array list of dependent axiom id.  same order as other arraylist
				dataStruct.setAxiomMap(axiomMap);				
				dataStruct.setPathMap(pathMap);
				dataStruct = Benchmarks.validateDependencies(benchmarks, depRootSpaceId, linked, userId);
				dataStruct.getAxiomMap().size();
				log.info("Size of Axiom Map = " +dataStruct.getAxiomMap().size() + ", Path Map = " + dataStruct.getPathMap().size());
				log.info("Dependencies Validated.  About to add (with dependencies)" + benchmarks.size() + " benchmarks to space " + spaceId);
				// Next add them to the database (must happen AFTER they are processed and have dependencies validated);
				if (dataStruct != null){

					Benchmarks.addReturnList(benchmarks, spaceId, dataStruct, statusId);

				} else{
					log.warn("Problem validating benchmark depedencies for space " + spaceId);
				}

				return true;
			} catch (Exception e){			
				log.error("Need to roll back - addWithDeps says" + e.getMessage(), e);
				Common.doRollback(con);
			} finally {
				Common.safeClose(con);
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
	 *   introduces the (presumably validated)dependencies for a list of benchmarks
	 * @param benchmarks list of benchmarks that (potentially) have dependencies 
	 * @param dataStruct  datastructure that holds information about the benchmarks' dependencies
	 * @author Benton McCune
	 */
	private static void introduceDependencies(List<Benchmark> benchmarks,
			DependValidator dataStruct) {
		HashMap<Integer, ArrayList<Integer>> axiomMap = dataStruct.getAxiomMap();
		HashMap<Integer, ArrayList<String>> pathMap = dataStruct.getPathMap();
		Integer benchId;
		log.info("entering dependency loop for benchmark list of size = " + benchmarks.size());
		for (int i=0; i< benchmarks.size(); i++){
			benchId = benchmarks.get(i).getId();
			log.info("Benchmark " + i +" has id " + benchId + " with " + axiomMap.get(benchId).size() + " axiom ids and " + pathMap.get(benchId).size() + " paths");
			introduceDependencies(benchId, axiomMap.get(benchId), pathMap.get(benchId));
		}

	}
	/**
	 * introduces the dependencies for a single benchmark
	 * @param benchId  id of the benchmark
	 * @param axiomIdList   list of benchmark ids for dependent benchmarks
	 * @param pathList   list of paths for dependent benchmarks (indices correspond to those in axiomIDList
	 * @author Benton McCune
	 */
	private static void introduceDependencies(Integer benchId,
			ArrayList<Integer> axiomIdList, ArrayList<String> pathList) {	
		for (int i = 0; i < axiomIdList.size(); i++){
			log.info("(Primary Bench, Secondary Bench, Path) = (" + benchId + "," + axiomIdList.get(i) + "," + pathList.get(i)+ ")");
			Benchmarks.addBenchDependency(benchId, axiomIdList.get(i), pathList.get(i));
		}

	}

	//returns data structure with two maps
	/**
	 * Validates the dependencies for a list of benchmarks (usually all benches of a single space)
	 * @param benchmarks The list of benchmarks that might have dependencies
	 * @param con database connection
	 * @param spaceId the id of the space where the axiom benchmarks lie
	 * @param linked true if the depRootSpace is the same as the first directory in the include statement
	 * @param userId the user's Id
	 * @return the data structure that has information about depedencies
	 * @author Benton McCune
	 */
	private static DependValidator validateDependencies(
			List<Benchmark> benchmarks, Integer spaceId, Boolean linked, Integer userId) {

		HashMap<String, Integer> foundDependencies = new HashMap<String,Integer>();//keys are include paths, values are the benchmarks ids of secondary benchmarks

		DependValidator dataStruct = new DependValidator();
		HashMap<Integer, ArrayList<String>> pathMap = new HashMap<Integer, ArrayList<String>>();//Map from primary bench Id  to the array list of dependency paths 
		HashMap<Integer, ArrayList<Integer>> axiomMap = new HashMap<Integer, ArrayList<Integer>>();//Map from primary bench Id to the array list of dependent axiom id.  same order as other arraylist
		dataStruct.setAxiomMap(axiomMap);
		dataStruct.setPathMap(pathMap);
		dataStruct.setFoundDependencies(foundDependencies);
		Benchmark benchmark = new Benchmark();
		for (int i = 0; i< benchmarks.size(); i++){
			benchmark = benchmarks.get(i);
			DependValidator benchDepLists = new DependValidator();
			benchDepLists = validateIndBenchDependencies(benchmark, spaceId, linked, userId, foundDependencies);
			if (benchDepLists == null)
			{
				log.warn("Dependent benchs not found for Bench " + benchmark.getName());
				return null;
			}
			pathMap.put(i, benchDepLists.getPaths());//doesn't have Ids yet! - need to use index
			axiomMap.put(i, benchDepLists.getAxiomIds());
			foundDependencies = benchDepLists.getFoundDependencies();// get update 
		}
		return dataStruct;
	}

	/**
	 * Validates the dependencies for a benchmark
	 * @param benchmark The benchmark that might have dependencies
	 * @param con database connection
	 * @param spaceId the id of the space where the axiom benchmarks lie
	 * @param linked true if the depRootSpace is the same as the first directory in the include statement
	 * @param userId the user's Id
	 * @return the data structure that has information about depedencies
	 * @author Benton McCune
	 * @param foundDependencies 
	 */
	private static DependValidator validateIndBenchDependencies(Benchmark bench, Integer spaceId, Boolean linked, Integer userId, HashMap<String, Integer> foundDependencies){

		Properties atts = bench.getAttributes();

		DependValidator benchDepLists = new DependValidator();
		ArrayList<Integer> axiomIdList = new ArrayList<Integer>();
		ArrayList<String> pathList = new ArrayList<String>();

		Integer numberDependencies = 0;
		String includePath = "";		
		try {
			numberDependencies = Integer.valueOf(atts.getProperty("starexec-dependencies", "0"));
			log.info("# of dependencies = " + numberDependencies);
			for (int i = 1; i <= numberDependencies; i++){
				includePath = atts.getProperty("starexec-dependency-"+i, "");//TODO: test when given bad atts
				log.debug("Dependency Path of Dependency " + i + " is " + includePath);
				Integer depBenchId = -1;				
				if (includePath.length()>0){
					//checkMap first
					if (foundDependencies.get(includePath)!= null){
						depBenchId = foundDependencies.get(includePath);
						log.info("Already found this one before, its id is " + depBenchId);
					}
					else{
						log.info("This include path (" + includePath +") is new so we must search the database.");
						depBenchId = Benchmarks.findDependentBench(spaceId,includePath, linked, userId);
						foundDependencies.put(includePath, depBenchId);
						log.info("Dependent Bench = " + depBenchId);
					}
					pathList.add(includePath);
					axiomIdList.add(depBenchId);
				}

				if (depBenchId==-1)
				{
					log.warn("Dependent Bench not found for " + bench.getName() +  ". Rolling back since dependencies not validated.");
					return null;
				}
			}	

		}
		catch (Exception e){			
			log.error("validate dependency failed on bench " +bench.getName() + ": " + e.getMessage(), e);
			return null;

		} finally {

		}	
		benchDepLists.setAxiomIds(axiomIdList);
		benchDepLists.setPaths(pathList);
		benchDepLists.setFoundDependencies(foundDependencies);
		return benchDepLists;
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
	public static boolean addWithDeps(List<Benchmark> benchmarks, int spaceId, Integer depRootSpaceId, Boolean linked, Integer userId, Integer statusId) {
		Connection con = null;			
		log.info("Going to add " + benchmarks.size() + "benchmarks (with dependencies) to space " + spaceId);
		try {			
			con = Common.getConnection();

			Common.beginTransaction(con);

			Boolean value = addWithDeps(benchmarks, spaceId, con, depRootSpaceId, linked, userId, statusId);

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
	protected static Boolean attachBenchAttrs(List<Benchmark> benchmarks, Processor p, Integer statusId) {
		log.info("Beginning processing for " + benchmarks.size() + " benchmarks");			
		int count = benchmarks.size();
		// For each benchmark in the list to process...
		for(Benchmark b : benchmarks) {
			BufferedReader reader = null;

			try {
				// Run the processor on the benchmark file
				log.info("executing - " + p.getFilePath() + " \"" + b.getPath() + "\"");
				reader = Util.executeCommand(p.getFilePath() + " \"" + b.getPath() + "\"");
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
				count--;
				if (Benchmarks.isBenchValid(prop)){
					Uploads.incrementValidatedBenchmarks(statusId);
				}
				else{
					Uploads.incrementFailedBenchmarks(statusId);
					Integer numFailedBenches = Uploads.get(statusId).getFailedBenchmarks();
					if (numFailedBenches < R.MAX_FAILED_VALIDATIONS){
						Uploads.addFailedBenchmark(statusId,b.getName());
						String message = b.getName() + " failed validation";
						log.warn(message);
						Uploads.setErrorMessage(statusId, message);	
					}
					else{
						String message = "Major Benchmark Validation Errors - examine your validator";
						log.warn(message + ", status id = " + statusId);
						Uploads.setErrorMessage(statusId, message);	
					}
				}
				log.info(b.getName() + " processed. " + count + " more benchmarks to go.");
			} catch (Exception e) {
				log.warn(e.getMessage(), e);
				return false;
				//TODO handle 
			} finally {
				if(reader != null) {
					try { reader.close(); } catch(Exception e) {log.error(e);}
				}

			}
		}
		return true;
	}	

	/**
	 * Internal method which adds a single benchmark to the database under the given spaceId
	 * @param con The connection the operation will take place on
	 * @param benchmark The benchmark to add to the database
	 * @param spaceId The id of the space the benchmark will belong to
	 * @return The new benchmark ID on success, -1 otherwise
	 * @author Tyler Jensen
	 */
	protected static int add(Connection conParam, Benchmark benchmark, int spaceId) throws Exception {				
		Connection con = null;
		CallableStatement procedure = null;			
		try{
			con = Common.getConnection();
			Common.beginTransaction(con);

			log.debug("Driver Name = " + con.getMetaData().getDriverName());
			log.debug("Driver Version = " + con.getMetaData().getDriverVersion());
			Properties attrs = benchmark.getAttributes();
			log.info("adding benchmark " + benchmark.getName() + " to space " + spaceId);
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
			log.debug("new bench id is " + benchmark.getId());
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
					log.debug("Adding att number " + count + " " + (String)keyVal.getKey() +", " + (String)keyVal.getValue() + " to bench " + benchmark.getId());
					Benchmarks.addBenchAttr(con, benchmark.getId(), (String)keyVal.getKey(), (String)keyVal.getValue());
				}							

			}				
			Common.endTransaction(con);
			log.info("(within internal add method) Added Benchmark " + benchmark.getName());	
			return benchmark.getId();
		}
		catch (Exception e){			
			log.error("add says " + e.getMessage(), e);
			Common.doRollback(con);
			return -1;
		} finally {
		    Common.safeClose(procedure);
		    Common.safeClose(con);
		}
	}


	/**
	 *   Add a benchmark with dependencies.  The dependencies are already validated.
	 * 
	 * @param benchmark  the benchmark to be added
	 * @param spaceId   the space the bench is being added to
	 * @param dataStruct  the datastructure that holds the validated dependency information
	 * @param benchIndex  the location of this benchmarks dependency info in the datastruct
	 * @return benchmark returns the benchmark added (not typically needed)
	 * @throws Exception
	 * @author Benton McCune
	 */
	protected static Benchmark addBenchWDepend(Benchmark benchmark, int spaceId, DependValidator dataStruct, Integer benchIndex) throws Exception {				
		Connection con = null;
		CallableStatement procedure = null;			
		try{
			con = Common.getConnection();
			Common.beginTransaction(con);

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
				//int bigSetAtts = attrs.entrySet().size()/10;

				for(Entry<Object, Object> keyVal : attrs.entrySet()) {
					// Add the attribute to the database
					count++;
					log.debug("Adding att number " + count + " " + (String)keyVal.getKey() +", " + (String)keyVal.getValue() + " to bench " + benchmark.getId());
					Benchmarks.addBenchAttr(con, benchmark.getId(), (String)keyVal.getKey(), (String)keyVal.getValue());
				}							
				//do previously validated dependencies here
				Common.endTransaction(con);// benchmarks should be in db now
				ArrayList<Integer> axiomIdList = dataStruct.getAxiomMap().get(benchIndex);
				ArrayList<String> pathList = dataStruct.getPathMap().get(benchIndex);			
				Benchmarks.introduceDependencies(benchmark.getId(), axiomIdList, pathList);
			}				
			log.info("(within internal add method) Added Benchmark " + benchmark.getName());
			return benchmark;
		}
		catch (Exception e){			
			log.error("addBenchWDepend says " + e.getMessage(), e);
			Common.doRollback(con);
			return null;
		} finally {
		    Common.safeClose(procedure);
		    Common.safeClose(con);
		}
	}

	// just for testing locally
	@SuppressWarnings("unused")
	public static boolean testDepCode(Benchmark bench, int userId){
		//Connection con = null;
		log.debug("Testing Dep Code on " + bench.getName());
		try {	
			//con = Common.getConnection();
			//Common.beginTransaction(con);
			ArrayList<Benchmark> benches = new ArrayList<Benchmark>();
			benches.add(bench);
			DependValidator benValidator = Benchmarks.validateDependencies(benches, 3, true, 1);
			log.info("Size of axiom Map = " + benValidator.getAxiomMap().size());
			if (benValidator!= null){
				log.debug("Validated - now introducing depedencies");
				Benchmarks.introduceDependencies(benches, benValidator);
			}
			else{
				log.debug("Null validator - nothing else to do");
			}
			//Benchmarks.validateIndBenchDependencies(bench, 3, true, 1);
			//Boolean success = introduceDependencies(bench, 6, true, userId, con);
			//log.debug("Dependencies introduced = " +success);
			//	Common.endTransaction(con);
		}catch (Exception e){			
			log.error(e.getMessage(), e);
			//Common.doRollback(con);
		} finally {
			//	log.debug("safe closing connection.");
			//	Common.safeClose(con);
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
			String includePath) {

		Connection con = null;
		CallableStatement procedure = null;			
		try {	
			con = Common.getConnection();
			Common.beginTransaction(con);

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
			Common.endTransaction(con);
			return true;
		}catch (Exception e){			
			log.error(e.getMessage(), e);
			Common.doRollback(con);
		} finally {
			Common.safeClose(procedure);
			Common.safeClose(con);
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
		
		Connection con = null;			
		CallableStatement procedure = null;
		log.debug("(Within Method) Looking for Benchmark " + benchName +" in Space " + spaceId);
		try {
			con = Common.getConnection();		
			procedure = con.prepareCall("{CALL GetBenchByName(?,?)}");
			procedure.setInt(1, spaceId);
			procedure.setString(2, benchName);

			ResultSet results = procedure.executeQuery();
			Integer benchId = -1;

			if(results.next()){
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
		    Common.safeClose(procedure);
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
	protected static void add(Connection conParam, List<Benchmark> benchmarks, int spaceId) throws Exception {		
		log.info("in add (list) method - adding " + benchmarks.size()  + " benchmarks to space " + spaceId);
		for(Benchmark b : benchmarks) {
			if (Benchmarks.isBenchValid(b.getAttributes())){
				if(Benchmarks.add(conParam, b, spaceId)<0) {
					throw new Exception(String.format("Failed to add benchmark [%s] to space [%d]", b.getName(), spaceId));
				}
			}
		}		
		log.info(String.format("[%d] new benchmarks added to space [%d]", benchmarks.size(), spaceId));
	}

	protected static void addNoCon(List<Benchmark> benchmarks, int spaceId, int statusId) throws Exception {		
		log.info("in add (list) method (no con paramter )- adding " + benchmarks.size()  + " benchmarks to space " + spaceId);
		for(Benchmark b : benchmarks) {
			if(Benchmarks.add(b, spaceId)<0) {
				String message = ("failed to add bench " + b.getName());
				Uploads.setErrorMessage(statusId, message);
				//Note - this does not occur when Benchmark fails validation even though those benchmarks not added
				throw new Exception(String.format("Failed to add benchmark [%s] to space [%d]", b.getName(), spaceId));
			}
			else{
				Uploads.incrementCompletedBenchmarks(statusId);
			}
		}		
		log.info(String.format("[%d] new benchmarks added to space [%d]", benchmarks.size(), spaceId));
	}

	//
	protected static List<Benchmark> addReturnList(List<Benchmark> benchmarks, int spaceId, DependValidator dataStruct, Integer statusId) throws Exception {		
		log.info("in addReturnList method - adding " + benchmarks.size()  + " benchmarks to space " + spaceId);

		Benchmark b = new Benchmark();
		for(int i = 0; i < benchmarks.size(); i++) {
			b = benchmarks.get(i);
			b = Benchmarks.addBenchWDepend(b, spaceId, dataStruct, i);
			if(b == null) {
				throw new Exception(String.format("Failed to add benchmark to space [%d]", spaceId));
			}
			Uploads.incrementCompletedBenchmarks(statusId);
		}
		log.info(String.format("[%d] new benchmarks added to space [%d]", benchmarks.size(), spaceId));
		return benchmarks;	
	}
	
	/**
	 * Makes a deep copy of an existing benchmark, gives it a new user, and places it
	 * into a space
	 * @param s The existing benchmark to copy
	 * @param userId The userID that the new benchmark will be given
	 * @param spaceId The space ID of the space to place the new benchmark in to
	 * @return The ID of the new benchmark, or -1 on failure
	 * @author Eric Burns
	 */
	
	public static int copyBenchmark(Benchmark b, int userId, int spaceId) {
		try {
			log.debug("Copying benchmark "+b.getName()+" to new user id= "+String.valueOf(userId));
			Benchmark newBenchmark=new Benchmark();
			newBenchmark.setAttributes(b.getAttributes());
			newBenchmark.setType(b.getType());
		
			newBenchmark.setDescription(b.getDescription());
			newBenchmark.setName(b.getName());
			newBenchmark.setUserId(userId);
			newBenchmark.setUploadDate(b.getUploadDate());
			newBenchmark.setDiskSize(b.getDiskSize());
			newBenchmark.setDownloadable(b.isDownloadable());
			
			if (newBenchmark.getAttributes()==null) {
				newBenchmark.setAttributes(new Properties());
			}
			
			//this benchmark must be valid, since it is just a copy of 
			//an old benchmark that already passed validation
			newBenchmark.getAttributes().put("starexec-valid", "true");
			File benchmarkFile=new File(b.getPath());
		
			File uniqueDir = new File(R.BENCHMARK_PATH, "" + userId);
			uniqueDir = new File(uniqueDir, newBenchmark.getName());
			uniqueDir = new File(uniqueDir, "" + shortDate.format(new Date()));
			uniqueDir.mkdirs();
			newBenchmark.setPath(uniqueDir.getAbsolutePath()+File.separator+benchmarkFile.getName());
		
			FileUtils.copyFileToDirectory(benchmarkFile, uniqueDir);
			int benchId= Benchmarks.add(newBenchmark, spaceId);
			if (benchId<0) {
				log.error("Benchmark being copied could not be successfully added to the database");
				return benchId;
			}
			log.debug("Benchmark added successfully to the database, now adding dependency associations");
			List<BenchmarkDependency> deps=Benchmarks.getBenchDependencies(b.getId());
			
			for (BenchmarkDependency dep : deps) {
				Benchmarks.addBenchDependency(benchId, dep.getSecondaryBench().getId(), dep.getDependencyPath());
			}
			
			log.debug("Benchmark copied successfully, return new benchmark ID = "+benchId);
			return benchId;
			
		} catch (Exception e) {
			log.error("copyBenchmark says "+e.getMessage());
			return -1;
		}
		
		
	}

	/**
	 * Deletes a benchmark from the database (cascading deletes handle all dependencies)
	 * @param id the id of the benchmark to delete
	 * @return True if the operation was a success, false otherwise
	 * @author Todd Elvers
	 */
	public static boolean delete(int id){
		Connection con = null;			
		CallableStatement procedure = null;
		File benchToDelete = null;
		try {
			con = Common.getConnection();

			procedure = con.prepareCall("{CALL DeleteBenchmarkById(?, ?)}");
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
		    Common.safeClose(procedure);
		    Common.safeClose(con);
		}
		log.debug(String.format("Deletion of benchmark [id=%d] failed.", id));
		return false;
	}	
	
	public static boolean isBenchmarkDeleted(int benchId) {
		Connection con=null;
		try {
			con=Common.getConnection();
			return isBenchmarkDeleted(con,benchId);
		} catch (Exception e) {
			log.error("Is benchmark deleted says " +e.getMessage(),e );
		} finally {
			Common.safeClose(con);
		}
		return false;
	}
	
	protected static boolean isBenchmarkDeleted(Connection con, int benchId) throws Exception {
	    CallableStatement procedure = null;
	    ResultSet results = null;
	    boolean deleted=false;
	    try {
		procedure = con.prepareCall("{CALL IsBenchmarkDeleted(?)}");
		procedure.setInt(1, benchId);					
		results = procedure.executeQuery();
		
		if (results.next()) {
		    deleted=results.getBoolean("benchDeleted");
		}
	    }
	    finally {
		Common.safeClose(results);
		Common.safeClose(procedure);
	    }
	    return deleted;
	    
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
			if (b==null) {
				return null;
			}

			if(true == includeAttrs){
				b.setAttributes(Benchmarks.getAttributes(con, benchId));
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
	    CallableStatement procedure = null;
	    ResultSet results = null;
	    Properties prop = new Properties();
	    try {
		procedure = con.prepareCall("{CALL GetBenchAttrs(?)}");
		procedure.setInt(1, benchId);					
		results = procedure.executeQuery();
		
		HashMap<String,String> attrMap = new HashMap<String, String>();
		while(results.next()){
			attrMap.put(results.getString("attr_key"), results.getString("attr_value"));
		}
		
		if(prop.size() <= 0) {
			prop = null;
		}
	    }
	    finally {
		Common.safeClose(results);
		Common.safeClose(procedure);
	    }
	    return prop;
	}
	
	

	/**
	 * Retrieves all attributes (key/value of the given benchmark in alphabetic order
	 * @param benchId the id of the benchmark to get the attributes of
	 * @return The properties object which holds all the benchmark's attributes
	 * @author Wyatt Kaiser
	 */
	public static TreeMap<String,String> getSortedAttributes(int benchId) {
		Connection con = null;			

		try {
			con = Common.getConnection();		
			return Benchmarks.getSortedAttributes(con, benchId);
		} catch (Exception e){			
			log.error(e.getMessage(), e);		
		} finally {
			Common.safeClose(con);
		}

		return null;
	}

	/** Retrieves all attributes (key/value) of the given benchmark in order
	 * @param con The connection to make the query on
	 * @param benchId The id of the benchmark to get the attributes of
	 * @return The properties object which holds all the benchmark's attributes
	 * @author Wyatt Kaiser
	 */
	protected static TreeMap<String,String> getSortedAttributes (Connection con, int benchId) throws Exception {
	    CallableStatement procedure = null;  
	    ResultSet results = null;  
	    TreeMap<String,String> sortedMap2 = new TreeMap<String,String>();
	    try {
		procedure = con.prepareCall("{CALL GetBenchAttrs(?)}");
		procedure.setInt(1, benchId);					
		results = procedure.executeQuery();

		while (results.next()){
			sortedMap2.put(results.getString("attr_key"), results.getString("attr_value"));
		}
	    }
	    finally {
		Common.safeClose(results);
		Common.safeClose(procedure);
	    }
	    return sortedMap2;
	}

	/**
	 * @param con The connection to query with
	 * @param benchId The id of the benchmark to retrieve
	 * @return A benchmark object representing the benchmark with the given ID
	 * @author Tyler Jensen
	 */
	protected static Benchmark get(Connection con, int benchId) throws Exception {					
	    CallableStatement procedure = null;  
	    ResultSet results = null;  
	    Benchmark b = null;
	    try {
		procedure = con.prepareCall("{CALL GetBenchmarkById(?)}");
		procedure.setInt(1, benchId);					
		results = procedure.executeQuery();

		if(results.next()){
			b = new Benchmark();
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
	    }
	    finally {
		Common.safeClose(results);
		Common.safeClose(procedure);
	    }

	    return null;
	}

	/**
	 * @param benchIds A list of ids to get benchmarks for
	 * @return A list of benchmark object representing the benchmarks with the given IDs
	 * @author Tyler Jensen
	 */
	public static List<Benchmark> get(List<Integer> benchIds, boolean includeAttrs) {
		Connection con = null;			

		try {
			con = Common.getConnection();					
			List<Benchmark> benchList = new ArrayList<Benchmark>();

			for(int id : benchIds) {				
				benchList.add(Benchmarks.get(con, id));
				if (includeAttrs) {
					benchList.get(benchList.size()-1).setAttributes(Benchmarks.getAttributes(con,id));
				}
			}
			
			

			return benchList;
		} catch (Exception e){			
			log.error(e.getMessage(), e);		
		} finally {
			Common.safeClose(con);
		}

		return null;
	}
	
	public static List<Benchmark> get(List<Integer> benchIds) {
		return get(benchIds,false);
	}


	/**
	 * @param spaceId The id of the space to get benchmarks for
	 * @return A list of all benchmarks belonging to the space
	 * @author Tyler Jensen
	 */
	public static List<Benchmark> getBySpace(int spaceId) {
		Connection con = null;			
		CallableStatement procedure = null;  
		ResultSet results = null;  

		try {
			con = Common.getConnection();		
			procedure = con.prepareCall("{CALL GetSpaceBenchmarksById(?)}");
			procedure.setInt(1, spaceId);					
			results = procedure.executeQuery();
			List<Benchmark> benchmarks = new LinkedList<Benchmark>();

			while(results.next()){
				Benchmark b = new Benchmark();
				b.setId(results.getInt("bench.id"));
				b.setName(results.getString("bench.name"));
				b.setUploadDate(results.getTimestamp("bench.uploaded"));
				b.setDescription(results.getString("bench.description"));
				b.setDownloadable(results.getBoolean("bench.downloadable"));	
				b.setDiskSize(results.getLong("bench.disk_size"));
				b.setPath(results.getString("bench.path"));

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
			Common.safeClose(results);
			Common.safeClose(procedure);
			Common.safeClose(con);
		}

		return null;
	}
	
	/**
	 * Might not use this method anymore
	 * 
	 * @param spaceId The id of the space to get benchmarks with minimal info
	 * @return A list of all benchmarks belonging to the space
	 * @author Benton McCune
	 */
	public static List<Benchmark> getMinBySpace(int spaceId) {
		Connection con = null;			
		CallableStatement procedure = null;  
		ResultSet results = null;  
		List<Benchmark> benchmarks = new LinkedList<Benchmark>();
		try {
			con = Common.getConnection();	
			//Test performance before writing new SQL method
			procedure = con.prepareCall("{CALL GetSpaceBenchmarksById(?)}");
			procedure.setInt(1, spaceId);					
			results = procedure.executeQuery();

			while(results.next()){
				Benchmark b = new Benchmark();
				b.setId(results.getInt("bench.id"));
				b.setName(results.getString("bench.name"));

				benchmarks.add(b);
			}			

			return benchmarks;
		} catch (Exception e){			
			log.error(e.getMessage(), e);		
		} finally {
			Common.safeClose(results);
			Common.safeClose(procedure);
			Common.safeClose(con);
		}

		return null;
	}
	
	/**
	 * @param spaceId The id of the root of the space hierarchy
	 *  to get benchmarks with minimal info
	 * @return A list of all benchmarks belonging to the space
	 * @author Benton McCune
	 */
	public static List<Benchmark> getMinForHierarchy(int spaceId, int userId) {
		Connection con = null;			
		CallableStatement procedure = null;
		ResultSet results = null;
		try {
			con = Common.getConnection();	
			//Test performance before writing new SQL method
			procedure = con.prepareCall("{CALL GetHierBenchmarksById(?,?,?)}");
			procedure.setInt(1, spaceId);	
			procedure.setInt(2, userId);	
			procedure.setInt(3, R.PUBLIC_USER_ID);	
			results = procedure.executeQuery();
			List<Benchmark> benchmarks = new LinkedList<Benchmark>();

			while(results.next()){
				Benchmark b = new Benchmark();
				b.setId(results.getInt("bench.id"));
				b.setName(results.getString("bench.name"));

				benchmarks.add(b);
			}			

			return benchmarks;
		} catch (Exception e){			
			log.error(e.getMessage(), e);		
		} finally {
			Common.safeClose(results);
			Common.safeClose(procedure);
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
		CallableStatement procedure = null;
		ResultSet results = null;
		try {
			con = Common.getConnection();		
			procedure = con.prepareCall("{CALL GetBenchmarksByOwner(?)}");
			procedure.setInt(1, userId);					
			results = procedure.executeQuery();
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
			Common.safeClose(results);
			Common.safeClose(procedure);
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
		CallableStatement procedure = null;
		ResultSet results = null;
		try {
			con = Common.getConnection();		
			procedure = con.prepareCall("{CALL getBenchmarkDependencies(?)}");
			procedure.setInt(1, benchmarkId);					
			results = procedure.executeQuery();
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

			log.debug(String.format("%d dependencies were returned as being needed by benchmark %d.", dependencies.size(), benchmarkId));

			return dependencies;
		} catch (Exception e){			
			log.error(e.getMessage(), e);		
		} finally {
			Common.safeClose(results);
			Common.safeClose(procedure);
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
		CallableStatement procedure = null;
		try {
			con = Common.getConnection();
			procedure = con.prepareCall("{CALL UpdateBenchmarkDetails(?, ?, ?, ?, ?)}");
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
			Common.safeClose(procedure);
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
	public static List<Benchmark> getBenchmarksForNextPage(int startingRecord, int recordsPerPage, boolean isSortedASC, int indexOfColumnSortedBy,  String searchQuery, int spaceId) {
		return getBenchmarksForNextPage(startingRecord,recordsPerPage,isSortedASC,indexOfColumnSortedBy,searchQuery,spaceId,true,"GetNextPageOfBenchmarks");
	}
	/**
	 * Get next page of the benchmarks belong to a specific user
	 * @param startingRecord specifies the number of the entry where should the query start
	 * @param recordsPerPage specifies how many records are going to be on one page
	 * @param isSortedASC specifies whether the sorting is in ascending order
	 * @param indexOfColumnSortedBy specifies which column the sorting is applied
	 * @param searchQuery the search query provided by the client
	 * @param userId Id of the user we are looking for
	 * @return a list of benchmarks belong to the user
	 * @author Wyatt Kaiser
	 */
	public static List<Benchmark> getBenchmarksByUserForNextPage(int startingRecord, int recordsPerPage, boolean isSortedASC, int indexOfColumnSortedBy, String searchQuery, int userId) {
		return getBenchmarksForNextPage(startingRecord,recordsPerPage,isSortedASC,indexOfColumnSortedBy,searchQuery,userId,false,"GetNextPageOfUserBenchmarks");
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
	 * @param id the id of the space to get the Benchmarks from
	 * @return a list of 10, 25, 50, or 100 Benchmarks containing the minimal amount of data necessary
	 * @author Todd Elvers
	 */
	private static List<Benchmark> getBenchmarksForNextPage(int startingRecord, int recordsPerPage, boolean isSortedASC, int indexOfColumnSortedBy,  String searchQuery, int id, boolean getDeleted, String procedureName) {
		Connection con = null;			
		CallableStatement procedure = null;			
		ResultSet results = null;
		try {
			con = Common.getConnection();

			procedure = con.prepareCall("{CALL "+procedureName+"(?, ?, ?, ?, ?, ?)}");
			procedure.setInt(1, startingRecord);
			procedure.setInt(2,	recordsPerPage);
			procedure.setInt(3, indexOfColumnSortedBy);
			procedure.setBoolean(4, isSortedASC);
			procedure.setInt(5, id);
			procedure.setString(6, searchQuery);
			
			results = procedure.executeQuery();
			List<Benchmark> benchmarks = new LinkedList<Benchmark>();

			while(results.next()){
				//don't include deleted benchmarks in the results if getDeleted is false
				if (!getDeleted && results.getBoolean("deleted")) {
					continue;
				}
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
			Common.safeClose(results);
			Common.safeClose(procedure);
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
		CallableStatement procedure = null;
		ResultSet results = null;

		try {
			con = Common.getConnection();
			procedure = con.prepareCall("{CALL GetBenchmarkCountInSpace(?)}");
			procedure.setInt(1, spaceId);
			results = procedure.executeQuery();

			if (results.next()) {
				return results.getInt("benchCount");
			}
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		} finally {
			Common.safeClose(results);
			Common.safeClose(procedure);
			Common.safeClose(con);
		}

		return 0;
	}

	public static boolean isPublic(int benchId) {
		Connection con = null;
		CallableStatement procedure = null;
		ResultSet results = null;
		try {
			con = Common.getConnection();
			procedure = con.prepareCall("{CALL IsBenchPublic(?,?)}");
			procedure.setInt(1, benchId);
			procedure.setInt(2, R.PUBLIC_USER_ID);
			results = procedure.executeQuery();

			if (results.next()) {
				return (results.getInt("benchPublic") > 0);
			}
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		} finally {
			Common.safeClose(results);
			Common.safeClose(procedure);
			Common.safeClose(con);
		}

		return false;
	}
	
	/**
	 * Determines whether the benchmark identified by the given benchmark ID has an
	 * editable name
	 * @param benchId The ID of the benchmark in question
	 * @return -1 if the name is not editable, 0 if it is editable and the benchmark is
	 * associated with no spaces, and a positive integer if the name is editable and the
	 * benchmark is associated only with the space with the returned ID.
	 */
	public static int isNameEditable(int benchId) {
		Connection con =null;
		CallableStatement procedure = null;
		ResultSet results=null;
		try {
			con=Common.getConnection();
			procedure = con.prepareCall("{CALL GetBenchAssoc(?)}");
			procedure.setInt(1, benchId);
			results = procedure.executeQuery();
			int id=-1;
			if (results.next()) {
				id=results.getInt("space_id");
			} else {
				log.debug("Benchmark associated with no spaces, so its name is editable");
				return 0;
			}
			
			if (results.next()) {
				log.debug("Benchmark is found in multiple spaces, so its name is not editable");
				return -1;
			}
			log.debug("Benchmark associated with one space id = "+id +" , so its name is editable");
			return id;
			
		} catch (Exception e) {
			log.error(e.getMessage(),e);
		} finally {
			Common.safeClose(results);
			Common.safeClose(procedure);
			Common.safeClose(con);
		}
		return -1;
	}

	/**
	 * Creates a space named after the directory and finds any benchmarks within the directory.
	 * Then the process recursively adds any subspaces found (other directories) until all directories
	 * under the original one are traversed. Also extracts the description file(if there is one) and
	 * sets it as the description for the space.
	 * @param directory The directory to extract data from
	 * @param typeId The bench type id to set for all the found benchmarks
	 * @param userId The user is of the owner of all the benchmarks found
	 * @param downloadable Whether or now to mark any found benchmarks as downloadable
	 * @param perm The default permissions to set for this space
	 * @return A single space containing all subspaces and benchmarks based on the file structure of the given directory.
	 * 
	 * @author Wyatt Kaiser
	 */
	@SuppressWarnings("deprecation")
	public static Space extractSpacesAndBenchmarks(File directory, int typeId, int userId, boolean downloadable, Permission perm, int statusId) throws Exception {
		// Create a space for the current directory and set it's name		
		log.info("Extracting Spaces and Benchmarks for " + userId);
		Space space = new Space();
		space.setName(directory.getName());
		space.setPermission(perm);
		String strUnzipped = "";
		
		// Search for description file within the directory...
		for (File f : directory.listFiles()) {
			if(f.getName().equals(R.BENCHMARK_DESC_PATH)){
				strUnzipped = "";
				try {
					FileInputStream fis = new FileInputStream(f.getAbsolutePath());
					BufferedInputStream bis = new BufferedInputStream(fis);
					DataInputStream dis = new DataInputStream(bis);
					String text;
					//dis.available() returns 0 if the file does not have more lines
					while (dis.available() != 0) {
						text= dis.readLine().toString();
						strUnzipped = strUnzipped + text;
					}
					fis.close();
					bis.close();
					dis.close();
				} catch (FileNotFoundException e) {
					e.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		space.setDescription(strUnzipped);
		
		for(File f : directory.listFiles()) {
			// If it's a sub-directory			
			if(f.isDirectory()) {
				// Recursively extract spaces/benchmarks from that directory
			    space.getSubspaces().add(Benchmarks.extractSpacesAndBenchmarks(f, typeId, 
											   userId, downloadable, perm, statusId));
			    Uploads.incrementTotalSpaces(statusId);//for upload status page
			} else if (!f.getName().equals(R.BENCHMARK_DESC_PATH)) { //Not a description file
				
				if (Validator.isValidPrimName(f.getName())) {
					Processor t = new Processor();
					t.setId(typeId);

					Benchmark b = new Benchmark();
					b.setPath(f.getAbsolutePath());
					b.setName(f.getName());
					b.setType(t);
					b.setUserId(userId);
					b.setDownloadable(downloadable);
													
					Uploads.incrementTotalBenchmarks(statusId);//for upload status page
					// Make sure that the benchmark has a unique name in the space.
					if(Spaces.notUniquePrimitiveName(b.getName(), space.getId(), 2)) {
					    throw new Exception("\""+b.getName() + "\" is not a unique name in the space.");
					}

					space.addBenchmark(b);
				} else {
				    throw new Exception("\""+f.getName() + "\" is not accepted as a legal benchmark name.");
				}
				
			}
		}

		return space;
	}

	/**
	 * Recursively walks through the given directory and subdirectory to find all benchmark files within them
	 * @param directory The directory to extract benchmark files from
	 * @param typeId The bench type id to set for all the found benchmarks
	 * @param userId The user id of the owner of all the benchmarks found
	 * @param downloadable Whether or now to mark any found benchmarks as downloadable
	 * @return A flat list of benchmarks containing all the benchmarks found under the given directory and it's subdirectories and so on
	 */
	public static List<Benchmark> extractBenchmarks(File directory, int typeId, int userId, boolean downloadable) {
		// Initialize the list we will return at the end...
		List<Benchmark> benchmarks = new LinkedList<Benchmark>();
		
		// For each file in the directory
		for(File f : directory.listFiles()) {
			if(f.isDirectory()) {
				// If it's a directory, recursively extract all benchmarks from it and add them to our list
				benchmarks.addAll(Benchmarks.extractBenchmarks(f, typeId, userId, downloadable));
			} else if (!f.getName().equals(R.BENCHMARK_DESC_PATH)) { //Not a description file
				
				//make sure the name is valid
				if (Validator.isValidPrimName(f.getName())) {
					Processor t = new Processor();
					t.setId(typeId);

					Benchmark b = new Benchmark();
					b.setPath(f.getAbsolutePath());
					b.setName(f.getName());
					b.setType(t);
					b.setUserId(userId);
					b.setDownloadable(downloadable);
					benchmarks.add(b);
				} else {
					//TODO: Determine behavior if a name is invalid
					return null;
				}
			}
		}

		return benchmarks;
	}

	/**
	 * Get the total count of the benchmarks belong to a specific user
	 * @param userId Id of the user we are looking for
	 * @return The count of the benchmarks
	 * @author Wyatt Kaiser
	 */
	public static int getBenchmarkCountByUser(int userId) {
		Connection con = null;
		CallableStatement procedure = null;
		ResultSet results = null;

		try {
			con = Common.getConnection();
			procedure = con.prepareCall("{CALL GetBenchmarkCountByUser(?)}");
			procedure.setInt(1, userId);
			results = procedure.executeQuery();

			if (results.next()) {
				return results.getInt("benchmarkCount");
			}
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		} finally {
			Common.safeClose(results);
			Common.safeClose(procedure);
			Common.safeClose(con);
		}

		return 0;		
	}

}


