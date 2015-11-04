package org.starexec.data.database;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.StringReader;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.TreeMap;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import org.starexec.constants.PaginationQueries;
import org.starexec.constants.R;
import org.starexec.data.to.Benchmark;
import org.starexec.data.to.BenchmarkDependency;
import org.starexec.data.to.Permission;
import org.starexec.data.to.Processor;
import org.starexec.data.to.Space;
import org.starexec.data.to.compare.BenchmarkComparator;
import org.starexec.exceptions.StarExecDatabaseException;
import org.starexec.exceptions.StarExecException;
import org.starexec.servlets.BenchmarkUploader;
import org.starexec.util.DependValidator;
import org.starexec.util.NamedParameterStatement;
import org.starexec.util.PaginationQueryBuilder;
import org.starexec.util.Timer;
import org.starexec.util.Util;
import org.starexec.util.Validator;

/**
 * Handles all database interaction for benchmarks.
 */
public class Benchmarks {
	private static final Logger log = Logger.getLogger(Benchmarks.class);
	

	/**
	 * Adds a single benchmark to the database under the given spaceId
	 * @param benchmark The benchmark to add to the database
	 * @param spaceId The id of the space the benchmark will belong to
	 * @param statusId the id for the upload page for adding this benchmark, if there is an upload page for this action. Otherwise, null
	 * @return The new benchmark ID on success, -1 otherwise
	 * @author Tyler Jensen
	 * @throws Exception Any database error that gets thrown
	 */
    public static int add(Benchmark benchmark, Integer spaceId, Integer statusId) throws Exception{
    if (Benchmarks.isBenchValid(benchmark.getAttributes())){
	    Connection con = null;		
			

	    try {
			con = Common.getConnection();
			Common.beginTransaction(con);
	
			// Add benchmark to database
			int benchId = Benchmarks.add(con, benchmark, statusId);
	
			if(benchId>=0){
				if (spaceId!=null) {
					Benchmarks.associate(benchId, spaceId,con);
				}
			    Common.endTransaction(con);
			    log.debug("bench successfully added");
						
			    return benchId;
			} else {
			    //will throw exception in calling method
			    Common.doRollback(con);
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
		Uploads.setXMLErrorMessage(statusId, "Benchmark validation failed for benchmark " + benchmark.getName() + ".");
	}
	return -1;
    }
    
	/**
	 * Deletes a benchmark and permanently removes it from the database. This is NOT
	 * the normal procedure for deleting a benchmark. It is used for testing. Calling "delete"
	 * is typically what is desired
	 * @param id The ID of the benchmark
	 * @return True on success and false otherwise
	 */
	
	public static boolean deleteAndRemoveBenchmark(int id) {
		boolean success=Benchmarks.delete(id);
		if (!success) {
			log.warn("there was an error deleting benchmark with id = "+id);
			return false;
		}
		Connection con=null;
		try {
			con=Common.getConnection();
			return Benchmarks.removeBenchmarkFromDatabase(id, con);
		} catch (Exception e) {
			log.error(e.getMessage(),e);
		} finally {
			Common.safeClose(con);
		}
		
		return false;
	}
	
	/**
	 * Recycles all benchmarks that are owned by the given user in the given collection
	 * @param benchmarks The benchmarks to potentially recycle
	 * @param userId The user who owns the benchmarks to recycle
	 * @return True on success and false on any error
	 */
	public static boolean recycleAllOwnedByUser(Collection<Benchmark> benchmarks, int userId) {
		boolean success=true;
		for (Benchmark b : benchmarks) {
			if (b.getUserId()==userId) {
				success=success && Benchmarks.recycle(b.getId());
			}
		}
		return success;
	}

	
    /** Add the given set of benchmark attributes to the database, if
     *  isBenchValid() returns true for the attrs.
     * @param con the db Connection to use
     * @param attrs the attributes
     * @param benchmark the Benchmark 
     * @param statusId the id of the upload page, or null if there isn't one
     * @return True on success and false otherwise
     */
    protected static boolean addAttributeSetToDbIfValid(Connection con, Properties attrs, Benchmark benchmark, Integer statusId) {
		if(!Benchmarks.isBenchValid(attrs)) {
		    Uploads.setBenchmarkErrorMessage(statusId, ("The benchmark processor did not validate the benchmark "
						       +benchmark.getName()+" (starexec-valid was not true)."));
		    return false;
		}
	
		// Discard the valid attribute, we don't need it
		attrs.remove("starexec-valid");
		log.info("bench is valid.  Adding " + attrs.entrySet().size() + " attributes");
		// For each attribute (key, value)...
		int count = 0;			
		for(Entry<Object, Object> keyVal : attrs.entrySet()) {
		    // Add the attribute to the database
		    count++;
		    log.debug("Adding att number " + count + " " 
			      + (String)keyVal.getKey() +", " + (String)keyVal.getValue() + " to bench " + benchmark.getId());
		    
		    if (!Benchmarks.addBenchAttr(con, benchmark.getId(), (String)keyVal.getKey(), (String)keyVal.getValue())) {
		    	Uploads.setBenchmarkErrorMessage(statusId, "Problem adding the following attribute-value pair to the db, for benchmark "
						+benchmark.getId()+": "+(String)keyVal.getKey() + ", " + (String)keyVal.getValue());
			
		    	return false;
		    }
		}							
	
		return true;
    }	


	/**
	 * Internal method which adds a single benchmark to the database
	 * @param con The connection the operation will take place on
	 * @param benchmark The benchmark to add to the database
	 * @param statusId the id for the upload page for adding this benchmark, if there is an upload page for this action.
	 * @return The new benchmark ID on success, -1 otherwise
	 * @author Tyler Jensen
	 * @throws Exception 
	 */
    protected static int add(Connection con, Benchmark benchmark, Integer statusId) throws Exception {
		
		CallableStatement procedure=null;
		try{
	
			Properties attrs = benchmark.getAttributes();
			// Setup normal information for the benchmark
			procedure = con.prepareCall("{CALL AddBenchmark(?, ?, ?, ?, ?, ?, ?)}");
			procedure.setString(1, benchmark.getName());		
			procedure.setString(2, benchmark.getPath());
			procedure.setBoolean(3, benchmark.isDownloadable());
			procedure.setInt(4, benchmark.getUserId());			
			//an ID of 0 or less is invalid, and can come from copying
			procedure.setInt(5, (Benchmarks.isBenchValid(attrs) && benchmark.getType().getId()>0) ? benchmark.getType().getId() : R.NO_TYPE_PROC_ID);
			procedure.setLong(6, FileUtils.sizeOf(new File(benchmark.getPath())));		
			procedure.registerOutParameter(7, java.sql.Types.INTEGER);

			// Execute procedure and get back the benchmark's id
			procedure.executeUpdate();		

			benchmark.setId(procedure.getInt(7));
			log.debug("new bench id is " + benchmark.getId());
			// If the benchmark is valid according to its processor...
			
			if (!addAttributeSetToDbIfValid(con,attrs,benchmark,statusId)) {
			    return -1;
			}

			log.info("(within internal add method) Added Benchmark " + benchmark.getName());	
			return benchmark.getId();
		}
		catch (Exception e){			
			log.error("add says " + e.getMessage(), e);
			
			return -1;
		} finally {
			
			Common.safeClose(procedure);
		}
	}

	/**
	 * Adds the list of benchmarks to the database and associates them with the given spaceId.
	 * The benchmark types are also processed based on the type of the first benchmark only.  This method assumes
	 * we are not introducing benchmark dependencies.
	 * @param benchmarks The list of benchmarks to add
	 * @param spaceId The space the benchmarks will belong to. If null, they will not be added to any space.
	 * @return A list of IDs of the new benchmarks if true, and null otherwise.
	 * @param statusId the id for the upload page for adding this benchmark, if there is an upload page for this action. Otherwise, null

	 * @author Tyler Jensen
	 */
	public static List<Integer> add(List<Benchmark> benchmarks, Integer spaceId, Integer statusId) throws StarExecDatabaseException {
		final String method = "add";
		log.trace("Entering method "+method);
		log.info("add - adding list of benchmarks to space " + spaceId);
		Connection con = null;			
		if (benchmarks.size()>0)
		{
			try {			
				con = Common.getConnection();

				Common.beginTransaction(con);

				log.info(benchmarks.size() + " benchmarks being added to space " + spaceId);
				// Get the processor of the first benchmark (they should all have the same processor)
				log.trace(method+" - benchmarks="+benchmarks);
				log.trace(method+" - benchmarks.get(0)="+benchmarks.get(0));
				log.trace(method+" - benchmarks.get(0).getType()="+benchmarks.get(0).getType());
				log.trace(method+" - benchmarks.get(0).getType().getId()="+benchmarks.get(0).getType().getId());
				Processor p = Processors.get(con, benchmarks.get(0).getType().getId());
				log.trace(method+" - TEST");
				log.trace(method+" - p="+p);
				log.trace(method+" - p.getId()="+p.getId());
				log.trace("add - found the following processor ID for the new benchmark " +p.getId());
				Common.endTransaction(con);
				// Process the benchmark for attributes (this must happen BEFORE they are added to the database)
				//We do not actually do any processing if it is the no-type, as it is not necessary 
				// The no-type always validates everything
				if (p.getId()!=Processors.getNoTypeProcessor().getId()) {
					Benchmarks.attachBenchAttrs(benchmarks, p, statusId);
				} else {
					for (Benchmark b : benchmarks) {
						Properties prop = new Properties();
						prop.put("starexec-valid", "true");
						b.setAttributes(prop);
					}
				}

				// Next add them to the database (must happen AFTER they are processed);
				return Benchmarks.addNoCon(benchmarks, spaceId, statusId);

			} catch (Exception e){			
				log.error(e.getMessage(), e);
				Common.doRollback(con);
				throw new StarExecDatabaseException(e.getMessage());
			} finally {
				Common.safeClose(con);
			}
		}
		else
		{
			log.info("No benchmarks to add here for space " + spaceId);
			return new ArrayList<Integer>();
		}
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
	protected static boolean addBenchAttr(Connection con, int benchId, String key, String val) {
		CallableStatement procedure=null;
		try {
			procedure = con.prepareCall("{CALL AddBenchAttr(?, ?, ?)}");
			procedure.setInt(1, benchId);
			procedure.setString(2, key);
			procedure.setString(3, val);
			procedure.executeUpdate();
			return true;
		} catch (Exception e) {
			log.error("addBenchAttr says "+e.getMessage(),e);
		} finally {
			Common.safeClose(procedure);
			
		}
		return false;
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
		CallableStatement procedure=null;
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
			Common.safeClose(con);
			Common.safeClose(procedure);
		}
		return false;

	}	

	/**
	 *   Add a benchmark with dependencies.  The dependencies are already validated.
	 * 
	 * @param benchmark  the benchmark to be added
	 * @param dataStruct  the datastructure that holds the validated dependency information
	 * @param benchIndex  the location of this benchmarks dependency info in the datastruct
	 * @param statusId The ID of an upload status if one exists for this operation, null otherwise
	 * @return benchmark returns the benchmark added (not typically needed)
	 * @throws Exception
	 * @author Benton McCune
	 */
    protected static Benchmark addBenchWDepend(Benchmark benchmark, DependValidator dataStruct, Integer benchIndex, 
					       int statusId) throws Exception {				
		Connection con = null;
		CallableStatement procedure=null;

		try{
			con = Common.getConnection();
			Common.beginTransaction(con);

			Properties attrs = benchmark.getAttributes();
			// Setup normal information for the benchmark
			procedure = con.prepareCall("{CALL AddBenchmark(?, ?, ?, ?, ?, ?, ?)}");
			procedure.setString(1, benchmark.getName());		
			procedure.setString(2, benchmark.getPath());
			procedure.setBoolean(3, benchmark.isDownloadable());
			procedure.setInt(4, benchmark.getUserId());			
			procedure.setInt(5, Benchmarks.isBenchValid(attrs) ? benchmark.getType().getId() : R.NO_TYPE_PROC_ID);
			procedure.setLong(6, FileUtils.sizeOf(new File(benchmark.getPath())));
			procedure.registerOutParameter(7, java.sql.Types.INTEGER);

			// Execute procedure and get back the benchmark's id
			procedure.executeUpdate();		
			benchmark.setId(procedure.getInt(7));

			// If the benchmark is valid according to its processor...

			if(!addAttributeSetToDbIfValid(con,attrs,benchmark,statusId))
			    return null;

			//do previously validated dependencies here
			Common.endTransaction(con);// benchmarks should be in db now
			ArrayList<Integer> axiomIdList = dataStruct.getAxiomMap().get(benchIndex);
			ArrayList<String> pathList = dataStruct.getPathMap().get(benchIndex);			
			Benchmarks.introduceDependencies(benchmark.getId(), axiomIdList, pathList);

			log.info("(within internal add method) Added Benchmark " + benchmark.getName());
			return benchmark;
		}
		catch (Exception e){			
			log.error("addBenchWDepend says " + e.getMessage(), e);
			Common.doRollback(con);
			return null;
		} finally {
			Common.safeClose(con);
			Common.safeClose(procedure);
		}
	}	

	/**
	 * Adds a list of benchmarks to the database and associates them with the given space ID
	 * @param benchmarks
	 * @param spaceId
	 * @param statusId
	 * @return
	 * @throws Exception
	 */
	protected static List<Integer> addNoCon(List<Benchmark> benchmarks, Integer spaceId, Integer statusId) throws Exception {		
		ArrayList<Integer> benchmarkIds=new ArrayList<Integer>();
		log.info("in add (list) method (no con paramter )- adding " + benchmarks.size()  + " benchmarks to space " + spaceId);
		int incrementCounter=0;
		Timer timer=new Timer();
		for(Benchmark b : benchmarks) {
		    int id=Benchmarks.add(b, spaceId,statusId);
			if(id<0) {
				String message = ("failed to add bench " + b.getName());
				Uploads.setBenchmarkErrorMessage(statusId, message);
				//Note - this does not occur when Benchmark fails validation even though those benchmarks not added
				throw new Exception(String.format("Failed to add benchmark [%s] to space [%d]", b.getName(), spaceId));
			}
			else{
				benchmarkIds.add(id);

				incrementCounter++;
				if (timer.getTime()>R.UPLOAD_STATUS_TIME_BETWEEN_UPDATES) {
					Uploads.incrementCompletedBenchmarks(statusId,incrementCounter);
					incrementCounter=0;
					timer.reset();
				}
				
			}
		}	
		if (incrementCounter>0) {
			Uploads.incrementCompletedBenchmarks(statusId,incrementCounter);
		}
		log.info(String.format("[%d] new benchmarks added to space [%d]", benchmarks.size(), spaceId));
		return benchmarkIds;
	}

	protected static List<Benchmark> addReturnList(List<Benchmark> benchmarks, DependValidator dataStruct, 
						       Integer statusId) throws Exception {		
		log.info("in addReturnList method - adding " + benchmarks.size()  + " benchmarks to database ");

		Benchmark b = new Benchmark();
		int incrementCounter=0;
		Timer timer=new Timer();
		for(int i = 0; i < benchmarks.size(); i++) {
			b = benchmarks.get(i);
			b = Benchmarks.addBenchWDepend(b, dataStruct, i, statusId);
			if(b == null) {
				throw new Exception("Failed to add benchmark to database");
			}
			incrementCounter++;
			if (timer.getTime()>R.UPLOAD_STATUS_TIME_BETWEEN_UPDATES) {
				Uploads.incrementCompletedBenchmarks(statusId,incrementCounter);
				incrementCounter=0;
				timer.reset();
			}
		}
		if (incrementCounter>0) {
			Uploads.incrementCompletedBenchmarks(statusId,incrementCounter);
		}
		log.info(String.format("[%d] new benchmarks added to database", benchmarks.size()));
		return benchmarks;	
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
	 * @param statusId statusId The ID of an upload status if one exists for this operation, null otherwise
	 * @return True if the operation was a success, false otherwise
	 * @author Benton McCune
	 */
	public static List<Integer> addWithDeps(List<Benchmark> benchmarks, int spaceId, Connection con, Integer depRootSpaceId, 
						Boolean linked, Integer userId, Integer statusId) {
		if (benchmarks.size()>0){
			try {			
				
				log.info("Adding (with deps) " + benchmarks.size() + " to Space " + spaceId);
				// Get the processor of the first benchmark (they should all have the same processor)
				Processor p = Processors.get(con, benchmarks.get(0).getType().getId());
				

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
				List<Benchmark> benches=Benchmarks.addReturnList(benchmarks, dataStruct, statusId);
				List<Integer> ids=new ArrayList<Integer>();
				for (Benchmark b : benches) {
					
					ids.add(b.getId());
				}
				Benchmarks.associate(ids, spaceId); //add the benchmarks to the space
				return ids;
			} catch (Exception e){			
				
				
			} 
		}
		else
		{
			log.info("No benches to add with this call to addWithDeps from space " + spaceId);
			return new ArrayList<Integer>();
		}
		return null;
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
	 * @param statusId The ID of an upload status if one exists for this operation, null otherwise
	 * @return A list of the IDs of the new benchmarks on success, and null otherwise
	 * @author Benton McCune
	 */
	public static List<Integer> addWithDeps(List<Benchmark> benchmarks, int spaceId, Integer depRootSpaceId, Boolean linked, 
						Integer userId, Integer statusId) {
		Connection con = null;			
		log.info("Going to add " + benchmarks.size() + "benchmarks (with dependencies) to space " + spaceId);
		try {			
			con = Common.getConnection();

			Common.beginTransaction(con);

			List<Integer> values = addWithDeps(benchmarks, spaceId, con, depRootSpaceId, linked, userId, statusId);
			if (values==null) {
				con.rollback();
			} else {
				Common.endTransaction(con);
			}
			

			return values;
		} catch (Exception e){			
			log.error(e.getMessage(), e);
			Common.doRollback(con);
		} finally {
			Common.safeClose(con);
		}

		return null;
	}	

	/**
	 * Associates a single benchmark with a single space.
	 * @param benchId The ID of the benchmark to put in a space. 
	 * @param spaceId The ID of the space to add the benchmark to
	 * @return True on success and false otherwise
	 * @author Albert Giegerich
	 */
	public static boolean associate(int benchId, int spaceId) {
		Connection con = null;			
		try {
			con = Common.getConnection();	
			return associate(benchId, spaceId, con);
		} catch (Exception e){			
			log.error(e.getMessage(), e);	
			Common.doRollback(con);
		} finally {
			Common.safeClose(con);
		}
		return false;
	}
	
	/**
	 * Associates a single benchmark with a single space
	 * @param benchId The ID of the benchmark to put in a space. 
	 * @param spaceId The ID of the space to add the benchmark to
	 * @param con The open connection to make the SQL call on
	 * @return True on success and false otherwise
	 */
	public static boolean associate(int benchId, int spaceId, Connection con) {
		CallableStatement procedure=null;

		try {
			procedure = con.prepareCall("{CALL AssociateBench(?, ?)}");

			procedure.setInt(1, benchId);
			procedure.setInt(2, spaceId);			
			procedure.executeUpdate();			
			
			return true;
		} catch (Exception e) {
			log.error(e.getMessage(),e);
		} finally {
			Common.safeClose(procedure);

		}
		return false;
			
	}
	
	
	/*
	 * Associates a list of benchmarks with the given space, keeping track of these associations
	 * in the correct Upload Status object for this upload
	 * @param benchIds The IDs of the benchmarks to associate
	 * @param spaceId The ID of the space to add benchmarks to
	 * @param XMLUploadId The ID of the UploadStatus object to track things in
	 * @return True on success and false otherwise.
	 *
	public static boolean associate(List<Integer> benchIds, int spaceId, int XMLUploadId) {
		Connection con = null;			
		int uploadCounter=0;
		try {
			con = Common.getConnection();	
			Common.beginTransaction(con);
			Timer timer=new Timer();
			for (int benchId: benchIds) {
				associate(benchId,spaceId, con);
				uploadCounter++;
				if (timer.getTime()>R.UPLOAD_STATUS_TIME_BETWEEN_UPDATES) {
					Uploads.incrementXMLCompletedBenchmarks(XMLUploadId, uploadCounter);
					uploadCounter=0;
					timer.reset();
				}
			}

			if (uploadCounter>0) {
				Uploads.incrementXMLCompletedBenchmarks(XMLUploadId, uploadCounter);

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
	*/

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

			for (int benchId: benchIds) {
				associate(benchId,spaceId, con);
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
	 * Given a set of benchmarks and a processor, this method runs each benchmark through
	 * the processor and adds a hashmap of attributes to the benchmark that are given from
	 * the processor.
	 * @param benchmarks The set of benchmarks to get attributes for
	 * @param p The processor to run each benchmark on
	 * @param statusId The ID of an upload status if one exists for this operation, null otherwise
	 * @return True if the operation is successful and false otherwise
	 */
	protected static Boolean attachBenchAttrs(List<Benchmark> benchmarks, Processor p, Integer statusId) throws IOException, StarExecException {
		log.info("Beginning processing for " + benchmarks.size() + " benchmarks");			
		int count = benchmarks.size();
		// For each benchmark in the list to process...
		int validatedCounter=0; //stores the number of benchmarks that have been validated since the last update
		int failedCounter=0; //stores the TOTAL number of benchmarks that failed 
		Timer timer=new Timer();
		for(Benchmark b : benchmarks) {
			List<File> files=new ArrayList<File>();
			files.add(new File(p.getFilePath()));
			files.add(new File(b.getPath()));
			File sandbox=Util.copyFilesToNewSandbox(files);
			String benchPath=new File(sandbox,new File(b.getPath()).getName()).getAbsolutePath();
			File working=new File(sandbox,new File(p.getFilePath()).getName());
			// Run the processor on the benchmark file
			log.info("executing - " + p.getExecutablePath() + " \"" + b.getPath() + "\"");
			String [] procCmd = new String[2];
			
			procCmd[0] = "./"+R.PROCESSOR_RUN_SCRIPT; 
			procCmd[1] = benchPath;
			String propstr = null;
			propstr = Util.executeSandboxCommand(procCmd,null,working);

			checkProcessorOutput(propstr);

			FileUtils.deleteQuietly(sandbox);
			// Load results into a properties file
			Properties prop = new Properties();
			prop.load(new StringReader(propstr));							
			log.debug("read this string from the processor: " + propstr);
			log.debug("read "+prop.size()+" properties");

			// Attach the attributes to the benchmark
			b.setAttributes(prop);
			count--;
			if (Benchmarks.isBenchValid(prop)){
				validatedCounter++;
				if (timer.getTime()>R.UPLOAD_STATUS_TIME_BETWEEN_UPDATES) {
					Uploads.incrementValidatedBenchmarks(statusId,validatedCounter);
					validatedCounter=0;
					timer.reset();
				}
			}
			else{
				failedCounter++;
				//TODO: Should we bin these like the other increments?
				Uploads.incrementFailedBenchmarks(statusId,1);
				if (failedCounter < R.MAX_FAILED_VALIDATIONS){
					Uploads.addFailedBenchmark(statusId,b.getName());
					String message = b.getName() + " failed validation";
					log.warn(message);
					Uploads.setBenchmarkErrorMessage(statusId, message);	
				}
				else{
					String message = "Major Benchmark Validation Errors - examine your validator";
					log.warn(message + ", status id = " + statusId);
					Uploads.setBenchmarkErrorMessage(statusId, message);	
				}
			}
			log.info(b.getName() + " processed. " + count + " more benchmarks to go.");
		}
		if (validatedCounter>0) {
			Uploads.incrementValidatedBenchmarks(statusId,validatedCounter);
		}
		return true;
	}

	/**
	 * Checks the processors output string for errors.
	 */
	private static void checkProcessorOutput(String processorOutput) throws StarExecException {
		final String method = "checkProcessorOutput";
		log.debug("Entering method "+method);
		if (processorOutput.contains("command not found")) {
			throw new StarExecException(String.format(
						"Processor used a command that StarExec does not recognize.%nProcessor Output:%s", processorOutput));
		}
	}


	/**
	 * Permanently removes a benchmark from the database
	 * @param benchId The ID of the benchmark to remove
	 * @param con The open connection to make the SQL call on
	 * @return True on success and false otherwise
	 */
	
	private static boolean removeBenchmarkFromDatabase(int benchId, Connection con) {
		log.debug("got request permanently remove this benchmark from the database "+benchId);
		CallableStatement procedure=null;
		try {
			procedure=con.prepareCall("CALL RemoveBenchmarkFromDatabase(?)");
			procedure.setInt(1,benchId);
			procedure.executeUpdate();
			return true;
			
		} catch (Exception e) {
			log.error(e.getMessage(),e);
		} finally {
			Common.safeClose(procedure);
		}
		return false;
	}
	
	/**
	 * Removes all benchmark database entries where the benchmark has been deleted
	 * AND has been orphaned
	 * @return True on success, false on error
	 */
	public static boolean cleanOrphanedDeletedBenchmarks() {
		Connection con=null;
		CallableStatement procedure=null;
		ResultSet results=null;
		
		//will contain the id of every benchmark that is associated with either a space or a pair
		HashSet<Integer> parentedBenchmarks=new HashSet<Integer>();
		try {
			con=Common.getConnection();
			procedure=con.prepareCall("{CALL GetBenchmarksAssociatedWithSpaces()}");
			results=procedure.executeQuery();
			while (results.next()) {
				parentedBenchmarks.add(results.getInt("id"));
			}
			Common.safeClose(procedure);
			Common.safeClose(results);
			
			
			procedure=con.prepareCall("{CALL GetBenchmarksAssociatedWithPairs()}");
			results=procedure.executeQuery();
			while (results.next())  {
				parentedBenchmarks.add(results.getInt("id"));
			}
			
			Common.safeClose(procedure);
			Common.safeClose(results);
			
			procedure=con.prepareCall("CALL GetDeletedBenchmarks()");
			results=procedure.executeQuery();
			while (results.next()) {
				int id=results.getInt("id");
				// the benchmark has been deleted AND it is not associated with any spaces or job pairs
				if (!parentedBenchmarks.contains(id)) {
					removeBenchmarkFromDatabase(id,con);
				}
			}	
			return true;
		} catch (Exception e) {
			log.error("cleanOrphanedDeletedBenchmarks says "+e.getMessage(),e);
		} finally {
			Common.safeClose(con);
			Common.safeClose(procedure);
			Common.safeClose(results);
		}
		return false;
	}
	/**
	 * Copies a list of benchmarks into a new space, making the given user the new owner
	 * @param benchmarks The benchmarks to copy
	 * @param userId The Id of the new owner
	 * @param spaceId The ID of the space to associate the new benchmarks with
	 * @return A list of IDs of all the new benchmarks
	 */
	public static List<Integer> copyBenchmarks(List<Benchmark> benchmarks,int userId, int spaceId) {
		List<Integer> ids=new ArrayList<Integer>();
		for (Benchmark b : benchmarks) {
			ids.add(copyBenchmark(b,userId,spaceId));
		}
		return ids;
	}

	/**
	 * Makes a deep copy of an existing benchmark, gives it a new user, and places it
	 * into a space
	 * @param b The existing benchmark to copy
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

			File uniqueDir = BenchmarkUploader.getDirectoryForBenchmarkUpload(userId, String.valueOf(b.getId()));
			uniqueDir.mkdirs();
			newBenchmark.setPath(uniqueDir.getAbsolutePath()+File.separator+benchmarkFile.getName());

			FileUtils.copyFileToDirectory(benchmarkFile, uniqueDir);
			int benchId= Benchmarks.add(newBenchmark, spaceId, null);
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
	 * Deletes each benchmark in a list of benchmarks
	 * @author Albert Giegerich
	 */
	public static void deleteEach(List<Benchmark> benchmarksToDelete) {
		for (Benchmark benchmark : benchmarksToDelete) {
			boolean success = delete(benchmark.getId());
			if (!success) {
				log.error("Benchmark with id="+benchmark.getId()+" was not deleted successfully.");		
			}
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
		CallableStatement procedure=null;
		
		try {
			//Cache.invalidateSpacesAssociatedWithBench(id);
			//Cache.invalidateAndDeleteCache(id, CacheType.CACHE_BENCHMARK);
			con = Common.getConnection();

			procedure = con.prepareCall("{CALL SetBenchmarkToDeletedById(?, ?)}");
			procedure.setInt(1, id);
			procedure.registerOutParameter(2, java.sql.Types.LONGNVARCHAR);
			procedure.executeUpdate();
			
			Util.safeDeleteDirectory(procedure.getString(2));		
			return true;
		} catch (Exception e){		
			log.error(e.getMessage(), e);		
		} finally {
			Common.safeClose(con);
			Common.safeClose(procedure);
		}
		log.debug(String.format("Deletion of benchmark [id=%d] failed.", id));
		return false;
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
				if (Validator.isValidBenchName(f.getName())) {
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
					return null;
				}
			}
		}

		return benchmarks;
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
	 * @param statusId statusId The ID of an upload status if one exists for this operation, null otherwise
	 * @return A single space containing all subspaces and benchmarks based on the file structure of the given directory.
	 * 
	 * @author Wyatt Kaiser
	 * @throws Exception Any exception with the description file, with an error message contained
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
					log.error("extractSpacesAndBenchmarks says "+e.getMessage(),e);
				} catch (IOException e) {
					log.error("extractSpacesAndBenchmarks says "+e.getMessage(),e);
				}
			}
		}
		space.setDescription(strUnzipped);
		int benchCounter=0;
		int spaceCounter=0;
		Timer timer=new Timer();
		Timer benchTimer=new Timer();
		for(File f : directory.listFiles()) {
			// If it's a sub-directory			
			if(f.isDirectory()) {
				// Recursively extract spaces/benchmarks from that directory
				space.getSubspaces().add(Benchmarks.extractSpacesAndBenchmarks(f, typeId, 
						userId, downloadable, perm, statusId));
				spaceCounter++;
				if (timer.getTime()>R.UPLOAD_STATUS_TIME_BETWEEN_UPDATES) {
					Uploads.incrementTotalSpaces(statusId,spaceCounter);//for upload status page
					spaceCounter=0;
					timer.reset();
				}
			} else if (!f.getName().equals(R.BENCHMARK_DESC_PATH)) { //Not a description file

				if (Validator.isValidBenchName(f.getName())) {
					Processor t = new Processor();
					t.setId(typeId);

					Benchmark b = new Benchmark();
					b.setPath(f.getAbsolutePath());
					b.setName(f.getName());
					b.setType(t);
					b.setUserId(userId);
					b.setDownloadable(downloadable);
					benchCounter++;
					if (benchTimer.getTime()>R.UPLOAD_STATUS_TIME_BETWEEN_UPDATES) {
						Uploads.incrementTotalBenchmarks(statusId,benchCounter);//for upload status page
						benchCounter=0;
						benchTimer.reset();
					}
					

					space.addBenchmark(b);
				} else {
				    String msg = "\""+f.getName() + "\" is not accepted as a legal benchmark name.";
				    Uploads.setBenchmarkErrorMessage(statusId, msg);
				    throw new Exception(msg);
				}

			}
		}
		if (benchCounter>0) {
			Uploads.incrementTotalBenchmarks(statusId,benchCounter);//for upload status page
		}
		if (spaceCounter>0) {
			Uploads.incrementTotalSpaces(statusId,spaceCounter);//for upload status page
		}
		return space;
	}

	/**
	 * Returns the bench id of the axiom file that is being looked for, -1 if not found or too many by that name
	 * @param spaceId the dependent bench root space
	 * @param includePath  the path that will be used to drill down and find bench
	 * @param linked true if the depRootSpace is the same as the first directory in the include statement
	 * @param userId the ID of the user that owns the benchmarks with these dependencies
	 * @return benchId
	 * @author Benton McCune	
	 */
	private static Integer findDependentBench(Integer spaceId, String includePath, Boolean linked, Integer userId) {
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
	 * @param con The connection to query with
	 * @param benchId The id of the benchmark to retrieve
	 * @param includeDeleted If true, deleted benchmarks may be returned. If false, will not return a deleted benchmark
	 * @return A benchmark object representing the benchmark with the given ID
	 * @author Tyler Jensen
	 * 
	 * @throws Exception 
	 */
	protected static Benchmark get(Connection con, int benchId,boolean includeDeleted) throws Exception {	
		CallableStatement procedure=null;
		ResultSet results=null;
	
		try {
			if (!includeDeleted) {
				procedure = con.prepareCall("{CALL GetBenchmarkById(?)}");

			} else {
				procedure = con.prepareCall("{CALL GetBenchmarkByIdIncludeDeletedAndRecycled(?)}");
			}
			procedure.setInt(1, benchId);					
			results = procedure.executeQuery();

			if(results.next()){
				Benchmark b = resultToBenchmark(results,"bench");

				Processor t = Processors.resultSetToProcessor(results, "types");

				b.setType(t);
				Common.safeClose(results);
				return b;				
			}													

		} catch (Exception e) {
			log.error("Benchmarks.get says "+e.getMessage(),e);
		} finally {
			Common.safeClose(procedure);
			Common.safeClose(results);
		}
		return null;
	}

	
	/**
	 * Retrieves a benchmark without attributes. Will not return a "deleted" benchmark
	 * @param benchId The id of the benchmark to retrieve
	 * @return A benchmark object representing the benchmark with the given ID
	 * @author Tyler Jensen
	 */
	
	public static Benchmark get(int benchId) {
		return Benchmarks.get(benchId,false,false);
	}
	
	/**
	 * @param benchId The id of the benchmark to retrieve
	 * @param includeAttrs Whether or not to to get this benchmark's attributes
	 * @param includeDeleted If true, may return a deleted benchmark. If false, deleted benchmarks will be ignored
	 * @return A benchmark object representing the benchmark with the given ID
	 * @author Tyler Jensen
	 */
	public static Benchmark get(int benchId, boolean includeAttrs, boolean includeDeleted) {
		Connection con = null;			

		try {
			con = Common.getConnection();		
			Benchmark b = Benchmarks.get(con, benchId,includeDeleted);
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
	 * Gets a list of benchmarks given a list of benchmark IDs
	 * @param benchIds The IDs of the benchmarks to retrieve.
	 * @return The benchmarks on success or null on failure. Attributes are not returned
	 */
	public static List<Benchmark> get(List<Integer> benchIds) {
		return get(benchIds,false);
	}

	/**
	 * @param benchIds A list of ids to get benchmarks for
	 * @return A list of benchmark object representing the benchmarks with the given IDs
	 * @param includeAttrs True to include attributes for all the given benchmarks, false if attributes should
	 * not be included
	 * @author Tyler Jensen
	 * 
	 */
	public static List<Benchmark> get(List<Integer> benchIds, boolean includeAttrs) {
		Connection con = null;			

		try {
			con = Common.getConnection();					
			List<Benchmark> benchList = new ArrayList<Benchmark>();

			for(int id : benchIds) {				
				benchList.add(Benchmarks.get(con, id,false));
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

	/**
	 * Returns a list of benchmarks owned by a given user
	 * 
	 * @param userId the id of the user who is the owner of the benchmarks we are to retrieve
	 * @return a list of benchmarks owned by a given user, may be empty
	 * @author Todd Elvers
	 */
	public static List<Benchmark> getByOwner(int userId) {
		Connection con = null;			
		ResultSet results=null;
		CallableStatement procedure = null;
		try {
			con = Common.getConnection();		
			 procedure = con.prepareCall("{CALL GetBenchmarksByOwner(?)}");
			procedure.setInt(1, userId);					
			 results = procedure.executeQuery();
			List<Benchmark> benchmarks = new LinkedList<Benchmark>();
			
			
			while(results.next()){
				Benchmark b = resultToBenchmark(results,"");
				Processor t = Processors.resultSetToProcessor(results, "types");
				b.setType(t);
				// Add benchmark object to list
				benchmarks.add(b);
			}			
			
			log.debug(String.format("%d benchmarks were returned as being owned by user %d.", benchmarks.size(), userId));
			
			return benchmarks;
		} catch (Exception e){			
			log.error(e.getMessage(), e);		
		} finally {
			Common.safeClose(con);
			Common.safeClose(procedure);
			Common.safeClose(results);
		}
		
		log.debug(String.format("Getting the benchmarks owned by user %d failed.", userId));
		return null;
	}
	
	
	
	/**
	 * Gets the IDs of every space that is associated with the given benchmark
	 * @param benchId The benchmark in question
	 * @return A list of space IDs that are associated with this benchmark
	 * @author Eric Burns
	 */
	public static List<Integer> getAssociatedSpaceIds(int benchId) {
		Connection con=null;
		CallableStatement procedure=null;
		ResultSet results = null;
		try {
			con=Common.getConnection();
			procedure=con.prepareCall("{CALL GetAssociatedSpaceIdsByBenchmark(?)}");
			procedure.setInt(1,benchId);
			results = procedure.executeQuery();
			List<Integer> ids=new ArrayList<Integer>();
			while (results.next()) {
				ids.add(results.getInt("space_id"));
			}
			return ids;
		} catch (Exception e) {
			log.error("Benchmarks.getAssociatedSpaceIds says "+e.getMessage(),e);
		} finally {
			Common.safeClose(con);
			Common.safeClose(results);
			Common.safeClose(procedure);
		}
		return null;
	}	
	
	/**
	 * Retrieves all attributes (key/value) of the given benchmark
	 * @param con The connection to make the query on
	 * @param benchId The id of the benchmark to get the attributes of
	 * @return The properties object which holds all the benchmark's attributes
	 * @author Tyler Jensen
	 * @throws Exception 
	 */
	protected static Properties getAttributes(Connection con, int benchId) throws Exception {
		CallableStatement procedure=null;
		ResultSet results=null;

		try {
			procedure = con.prepareCall("{CALL GetBenchAttrs(?)}");
			procedure.setInt(1, benchId);					
			results = procedure.executeQuery();

			Properties prop = new Properties();
			while(results.next()){
				prop.setProperty(results.getString("attr_key"), results.getString("attr_value"));
			}

			if(prop.size() <= 0) {
			    log.debug("No attributes found for benchmark "+new Integer(benchId));
			    prop = null;
			}

			return prop;
		} catch (Exception e) {
			log.error("getAttributes says "+e.getMessage(),e);
		} finally {
			 Common.safeClose(procedure);
			 Common.safeClose(results);
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
	 * Returns a list of benchmark dependencies that have the input benchmark as the primary benchmark
	 * 
	 * @param benchmarkId the id of the primary benchmark
	 * @return a list of benchmark dependencies for a given benchmark, may be empty
	 * @author Benton McCune
	 */
	public static List<BenchmarkDependency> getBenchDependencies(int benchmarkId) {
		Connection con = null;			
		CallableStatement procedure=null;
		ResultSet results=null;

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
			Common.safeClose(con);
			Common.safeClose(procedure);
			Common.safeClose(results);
		}

		log.debug(String.format("Getting the dependencies of benchmark %d failed.", benchmarkId));
		return null;
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
		CallableStatement procedure=null;
		ResultSet results=null;
		log.debug("(Within Method) Looking for Benchmark " + benchName +" in Space " + spaceId);
		try {
			con = Common.getConnection();		
			procedure = con.prepareCall("{CALL GetBenchByName(?,?)}");
			procedure.setInt(1, spaceId);
			procedure.setString(2, benchName);

			results = procedure.executeQuery();
			Integer benchId = -1;

			if(results.next()){
				benchId = (results.getInt("bench.id"));
				log.debug("Bench Id = " + benchId);
			}		
			results.last();
			Integer numResults = results.getRow();
			log.debug("# of Benchmarks with this name = " + numResults);
			if (numResults!=1) {
				return -1;
			}
			return benchId;
		} catch (Exception e){			
			log.error(e.getMessage(), e);		
		} finally {
			Common.safeClose(con);
			Common.safeClose(procedure);
			Common.safeClose(results);
		}
		return -1;
	}

	/**
	 * Get the total count of the benchmarks belong to a specific user
	 * @param userId Id of the user we are looking for
	 * @return The count of the benchmarks
	 * @author Wyatt Kaiser
	 */
	public static int getBenchmarkCountByUser(int userId) {
		Connection con = null;
		CallableStatement procedure=null;
		ResultSet results=null;
		try {
			con = Common.getConnection();
			procedure = con.prepareCall("{CALL GetBenchmarkCountByUser(?)}");
			procedure.setInt(1, userId);
			results = procedure.executeQuery();

			if (results.next()) {
				return results.getInt("benchCount");
			}
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		} finally {
			Common.safeClose(con);
			Common.safeClose(procedure);
			Common.safeClose(results);
		}

		return 0;		
	}
	/**
	 * Get the total count of the benchmarks belonging to a specific user that match the given query
	 * @param userId Id of the user we are looking for
	 * @param query The query to match the benchmarks on
	 * @return The count of the benchmarks
	 * @author Eric Burns
	 */
	public static int getBenchmarkCountByUser(int userId,String query) {
		Connection con = null;
		CallableStatement procedure=null;
		ResultSet results=null;
		try {
			con = Common.getConnection();
			procedure = con.prepareCall("{CALL GetBenchmarkCountByUserWithQuery(?, ?)}");
			procedure.setInt(1, userId);
			procedure.setString(2, query);
			results = procedure.executeQuery();

			if (results.next()) {
				return results.getInt("benchCount");
			}
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		} finally {
			Common.safeClose(con);
			Common.safeClose(procedure);
			Common.safeClose(results);
		}

		return 0;		
	}
	/**
	 * Get next page of the benchmarks belong to a specific user
	 * @param startingRecord specifies the number of the entry where should the query start
	 * @param recordsPerPage specifies how many records are going to be on one page
	 * @param isSortedASC specifies whether the sorting is in ascending order
	 * @param indexOfColumnSortedBy specifies which column the sorting is applied
	 * @param searchQuery the search query provided by the client
	 * @param userId Id of the user we are looking for
	 * @param recycled Whether to get recycled or non-recycled benchmarks
	 * @return a list of benchmarks belong to the user
	 * @author Wyatt Kaiser
	 */
	public static List<Benchmark> getBenchmarksByUserForNextPage(int startingRecord, int recordsPerPage, boolean isSortedASC, int indexOfColumnSortedBy, String searchQuery, int userId, boolean recycled) {
		Connection con = null;			
		NamedParameterStatement procedure=null;
		ResultSet results=null;
		try {
			con = Common.getConnection();
			PaginationQueryBuilder builder = new PaginationQueryBuilder(PaginationQueries.GET_BENCHMARKS_BY_USER_QUERY, startingRecord, recordsPerPage, getBenchmarkOrderColumn(indexOfColumnSortedBy), isSortedASC);

			procedure = new NamedParameterStatement(con, builder.getSQL());
			
			procedure.setInt("userId", userId);
			procedure.setString("query", searchQuery);
			procedure.setBoolean("recycled", recycled); 
			
			results = procedure.executeQuery();
			List<Benchmark> benchmarks = new LinkedList<Benchmark>();
			
			while(results.next()){
				//don't include deleted benchmarks in the results if getDeleted is false

				Benchmark b = new Benchmark();
				b.setId(results.getInt("id"));
				b.setName(results.getString("name"));
				b.setUserId(results.getInt("user_id"));
				if (results.getBoolean("deleted")) {
					b.setName(b.getName()+" (deleted)");
				}
				
				b.setDescription(results.getString("description"));
				b.setDeleted(results.getBoolean("deleted"));
				b.setRecycled(results.getBoolean("recycled"));
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
			Common.safeClose(procedure);
			Common.safeClose(results);
		}

		return null;
	}
	
	private static String getBenchmarkOrderColumn(int indexOfColumn) {
		if (indexOfColumn==0) {
			return "benchmarks.name";
		} else if (indexOfColumn==1) {
			return "processors.name";
		} else if (indexOfColumn==2) {
			return "bench_assoc.order_id";
		}
		return "benchmarks.name";

	}
	
	/**
	 * Retrieves benchmarks for the next page of a table on the space explorer
	 * @param startingRecord The index of the first benchmark to retrieve
	 * @param recordsPerPage The maximum number of benchmarks to retrieve
	 * @param isSortedASC True or false to sort ascending or descending
	 * @param indexOfColumnSortedBy The index of the datatable column to sort on
	 * @param searchQuery A query for filtering the benchmarks
	 * @param spaceId The ID of the space the benchmarks to retrieve are in
	 * @return A list of benchmarks on success or null on failure
	 */
	public static List<Benchmark> getBenchmarksForNextPage(int startingRecord, int recordsPerPage, boolean isSortedASC, int indexOfColumnSortedBy,  String searchQuery, int spaceId) {
		Connection con = null;			
		NamedParameterStatement procedure=null;
		ResultSet results=null;
		try {
			con = Common.getConnection();
			
			PaginationQueryBuilder builder = new PaginationQueryBuilder(PaginationQueries.GET_BENCHMARKS_IN_SPACE_QUERY, startingRecord, recordsPerPage, getBenchmarkOrderColumn(indexOfColumnSortedBy), isSortedASC);

			procedure = new NamedParameterStatement(con, builder.getSQL());

			procedure.setInt("spaceId", spaceId);
			procedure.setString("query", searchQuery);

			results = procedure.executeQuery();
			List<Benchmark> benchmarks = new LinkedList<Benchmark>();
			
			while(results.next()){
				Benchmark b = new Benchmark();
				b.setId(results.getInt("id"));
				b.setName(results.getString("name"));
				b.setUserId(results.getInt("user_id"));
				if (results.getBoolean("deleted")) {
					b.setName(b.getName()+" (deleted)");
				} else if (results.getBoolean("recycled")) {
					b.setName(b.getName()+" (recycled)");
				}
				b.setDeleted(results.getBoolean("deleted"));
				b.setRecycled(results.getBoolean("recycled"));
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
			Common.safeClose(procedure);
			Common.safeClose(results);
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
		CallableStatement procedure=null;
		ResultSet results=null;

		try {
			con = Common.getConnection();		
			procedure = con.prepareCall("{CALL GetSpaceBenchmarksById(?)}");
			procedure.setInt(1, spaceId);					
			results = procedure.executeQuery();
			List<Benchmark> benchmarks = new LinkedList<Benchmark>();

			while(results.next()){
				Benchmark b = resultToBenchmark(results,"bench"); 
				Processor t = Processors.resultSetToProcessor(results, "types");


				b.setType(t);
				benchmarks.add(b);
			}			

			return benchmarks;
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
	 * Retrieves the contents of a benchmark file from disk as a string
	 * @param b The benchmark to get the contents of (must have a valid path)
	 * @param limit the maximum number of lines to return, or no limit if less than 0
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
	 * Gets the number of Benchmarks in a given space
	 * 
	 * @param spaceId the id of the space to count the Benchmarks in
	 * @return the number of Benchmarks
	 * @author Todd Elvers
	 */
	public static int getCountInSpace(int spaceId) {
		log.debug("calling getCountInSpace for benchmarks");
		Connection con = null;
		CallableStatement procedure=null;
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
			Common.safeClose(con);
			Common.safeClose(procedure);
			Common.safeClose(results);
		}

		return 0;
	}

	/**
	 * Gets the number of Benchmarks in a given space that match a given query
	 * 
	 * @param spaceId the id of the space to count the Benchmarks in
	 * @param query The query to match the spaces on
	 * @return the number of Benchmarks
	 * @author Eric Burns
	 */
	public static int getCountInSpace(int spaceId, String query) {
		Connection con = null;
		CallableStatement procedure=null;
		ResultSet results = null;
		try {
			con = Common.getConnection();
			procedure = con.prepareCall("{CALL GetBenchmarkCountInSpaceWithQuery(?, ?)}");
			procedure.setInt(1, spaceId);
			procedure.setString(2, query);
			results = procedure.executeQuery();

			if (results.next()) {
				return results.getInt("benchCount");
			}
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		} finally {
			Common.safeClose(con);
			Common.safeClose(procedure);
			Common.safeClose(results);
		}

		return 0;
	}
	/**
	 * Retrieves a benchmark. If the benchmark is deleted, it will still be returned
	 * @param benchId The id of the benchmark to retrieve
	 * @param includeAttrs True if attributes for the benchmark should also be included and false otherwise
	 * @return A benchmark object representing the benchmark with the given ID
	 * @author Tyler Jensen
	 */

	public static Benchmark getIncludeDeletedAndRecycled(int benchId, boolean includeAttrs) {
		return Benchmarks.get(benchId,includeAttrs,true);
	}
	
	
	/**
	 * Gets the number of recycled benchmarks a user owns
	 * @param userId The ID of the user to count benchmarks before
	 * @return The integer count of recycled benchmarks
	 */
	public static int getRecycledBenchmarkCountByUser(int userId) {
		return getRecycledBenchmarkCountByUser(userId,"");
	}

	/**
	 * Gets the number of recycled benchmarks a user has that match the given query
	 * @param userId The ID of the user in question
	 * @param query The string query to match on
	 * @return The number of benchmarks, or -1 on failure
	 * @author Eric Burns
	 */
	
	public static int getRecycledBenchmarkCountByUser(int userId,String query) {
		Connection con=null;
		ResultSet results=null;
		CallableStatement procedure=null;
		try {
			con=Common.getConnection();
			procedure=con.prepareCall("CALL GetRecycledBenchmarkCountByUser(?,?)");
			procedure.setInt(1, userId);
			procedure.setString(2, query);
			results=procedure.executeQuery();
			if (results.next()) {
				return results.getInt("benchCount");
			}
		} catch (Exception e) {
			log.error("getRecycledBenchmarkCountByUser says "+e.getMessage(),e);
		} finally {
			Common.safeClose(con);
			Common.safeClose(results);
			Common.safeClose(procedure);
		}
		return -1;
	}

	/** Retrieves all attributes (key/value) of the given benchmark in order
	 * @param con The connection to make the query on
	 * @param benchId The id of the benchmark to get the attributes of
	 * @return The properties object which holds all the benchmark's attributes
	 * @author Wyatt Kaiser
	 * @throws Exception 
	 */
	protected static TreeMap<String,String> getSortedAttributes (Connection con, int benchId) throws Exception {
		CallableStatement procedure=null;
		ResultSet results=null;

		try {
			procedure = con.prepareCall("{CALL GetBenchAttrs(?)}");
			procedure.setInt(1, benchId);					
			results = procedure.executeQuery();

			TreeMap<String,String> sortedMap = new TreeMap<String,String>();
			while (results.next()){
				sortedMap.put(results.getString("attr_key"), results.getString("attr_value"));
			}
			return sortedMap;
		} catch (Exception e) {
			log.error("getSortedAttributes says "+e.getMessage(),e);
		} finally {
			Common.safeClose(procedure);
			Common.safeClose(results);
		}
		return null;
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
	

	/**
	 * Returns whether a benchmark with the given ID is present in the database with the 
	 * "deleted" column set to true
	 * @param benchId The ID of the benchmark to check
	 * @param con the open connection to make the SQL call on
	 * @return True if the benchmark exists in the database with the "deleted" column set to
	 * true, and false otherwise
	 * @author Eric Burns
	 */

	protected static boolean isBenchmarkDeleted(Connection con, int benchId) {
		CallableStatement procedure=null;
		ResultSet results=null;

		try {
			procedure = con.prepareCall("{CALL IsBenchmarkDeleted(?)}");
			procedure.setInt(1, benchId);					
			results = procedure.executeQuery();
			boolean deleted=false;
			if (results.next()) {
				deleted=results.getBoolean("benchDeleted");
			}
			return deleted;
		} catch (Exception e) {
			log.error("isBenchmarkDeleted says "+e.getMessage(),e);
		} finally {
			Common.safeClose(procedure);
			Common.safeClose(results);
		}
		return false;
	}	

	/**
	 * Returns whether a benchmark with the given ID is present in the database with the 
	 * "deleted" column set to true
	 * @param benchId The ID of the benchmark to check
	 * @return True if the benchmark exists in the database with the "deleted" column set to
	 * true, and false otherwise
	 * @author Eric Burns
	 */
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
	
	/**
	 * Returns whether a benchmark with the given ID is present in the database with the 
	 * "recycled" column set to true
	 * @param benchId The ID of the benchmark to check
	 * @param con the open connection to make the SQL call on
	 * @return True if the benchmark exists in the database with the "recycled" column set to
	 * true, and false otherwise
	 * @author Eric Burns
	 */

	protected static boolean isBenchmarkRecycled(Connection con, int benchId) {
		CallableStatement procedure=null;
		ResultSet results=null;

		try {
			procedure = con.prepareCall("{CALL IsBenchmarkRecycled(?)}");
			procedure.setInt(1, benchId);					
			results = procedure.executeQuery();
			boolean deleted=false;
			if (results.next()) {
				deleted=results.getBoolean("recycled");
			}
			return deleted;
		} catch (Exception e) {
			log.error("isBenchmarkRecycled says "+e.getMessage(),e);
		} finally {
			Common.safeClose(procedure);
			Common.safeClose(results);
		}
		return false;
	}
	/**
	 * Returns whether a benchmark with the given ID is present in the database with the 
	 * "recycled" column set to true
	 * @param benchId The ID of the benchmark to check
	 * @return True if the benchmark exists in the database with the "recycled" column set to
	 * true, and false otherwise
	 * @author Eric Burns
	 */
	public static boolean isBenchmarkRecycled(int benchId) {
		Connection con=null;
		try {
			con=Common.getConnection();
			return isBenchmarkRecycled(con,benchId);
		} catch (Exception e) {
			log.error("isBenchmarkRecycled says " +e.getMessage(),e );
		} finally {
			Common.safeClose(con);
		}
		return false;
	}

	/**
	 * Makes sure that a benchmark with the gived id exists.
	 * @param benchmarkId The id of a benchmark.
	 * @return true if the benchmark exists, otherwise false.
	 * @author Albert Giegerich
	 */
	public static boolean benchmarkExists(int benchmarkId) {
		Benchmark benchmark = Benchmarks.get(benchmarkId);
		return (benchmark != null);
	}	
	
	

	/**
	 * Internal helper method to determine if a benchmark is valid according to its attributes
	 * @param attrs The attributes of a benchmark
	 * @return True if the attributes are of a valid benchmark, false otherwise
	 */
	private static boolean isBenchValid(Properties attrs) {
		// A benchmark is valid if it has attributes and it has the special starexec-valid attribute
		return (attrs != null && Boolean.parseBoolean(attrs.getProperty("starexec-valid", "false")));
	}
	
	/**
	 * Determines whether the benchmark with the given ID is public. It is public if it is in at least
	 * one public space or if it is the default benchmark for some community
	 * @param benchId The ID of the benchmark in question
	 * @return True if the benchmark exists and is in a public space, false otherwise.
	 */

	public static boolean isPublic(int benchId) {
		Connection con = null;
		CallableStatement procedure=null;
		ResultSet results=null;
		try {
			con = Common.getConnection();
			procedure = con.prepareCall("{CALL IsBenchPublic(?)}");
			procedure.setInt(1, benchId);
			results = procedure.executeQuery();
			boolean publicSpace=false;
			if (results.next()) {
				publicSpace=(results.getInt("benchPublic") > 0);
			}
			
			if (publicSpace) {
				return true;
			}
			
			Common.safeClose(results);

			Common.safeClose(procedure);
			//if the benchmark is in no public spaces, check to see if it is the default benchmark for some community
			procedure=con.prepareCall("CALL IsBenchACommunityDefault(?)");
			procedure.setInt(1,benchId);
			results = procedure.executeQuery();
			if (results.next()) {
				return (results.getInt("benchDefault") > 0);
			}
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		} finally {
			Common.safeClose(con);
			Common.safeClose(procedure);
			Common.safeClose(results);
		}

		return false;
	}

	

	/**
	 * Sets the "recycled" flag in the database to true. Indicates the user has moved the 
	 * benchmark to the recycle bin, from which in can be deleted
	 * @param id the id of the benchmark to recycled
	 * @return True if the operation was a success, false otherwise
	 * @author Eric Burns
	 */
	
	public static boolean recycle(int id) {
		return setRecycledState(id,true);
	}
	
	/**
	 * Sets the "recycled" flag in the database to false. Indicates the user has removed
	 * the benchmark from the recycle bin
	 * @param id the id of the benchmark to be removed from the recycle bin
	 * @return True if the operation was a success, false otherwise
	 * @author Eric Burns
	 */
	
	public static boolean restore(int id) {
		return setRecycledState(id,false);
	}
	
	
	/**
	 * Restores all benchmarks a user has that have been recycled to normal
	 * @param userId The userId in question
	 * @return True on success, false otherwise
	 * @author Eric Burns
	 */
	public static boolean restoreRecycledBenchmarks(int userId) {
		Connection con=null;
		CallableStatement procedure=null;
		ResultSet results=null;
		try {
			con=Common.getConnection();

			Common.safeClose(procedure);
			procedure=con.prepareCall("CALL GetRecycledBenchmarkIds(?)");
			procedure.setInt(1, userId);
			results=procedure.executeQuery();
		
			while (results.next()) {
				Benchmarks.restore(results.getInt("id"));
			}
			return true;
		} catch (Exception e) {
			log.error("restoreRecycledBenchmarks says "+e.getMessage(),e);
		} finally {
			Common.safeClose(con);
			Common.safeClose(procedure);
			Common.safeClose(results);
		}
		return false;
	}

	/**
	 * Creates a Benchmark object from a SQL resultset
	 * @param results The resultset pointed at the row containing benchmark data
	 * @param prefix If the sql procedure used to create "results" used an "AS <name>" clause
	 * when getting benchmark data (as in SELECT * FROM benchmarks AS bench), then prefix
	 * should be <name>
	 * @return A Benchmark object
	 * @throws SQLException
	 */
	
	protected static Benchmark resultToBenchmark(ResultSet results, String prefix) throws SQLException {
		Benchmark b = new Benchmark();
		if (prefix==null || prefix=="") {
			b.setId(results.getInt("id"));
			b.setUserId(results.getInt("user_id"));
			b.setName(results.getString("name"));
			b.setUploadDate(results.getTimestamp("uploaded"));
			b.setPath(results.getString("path"));
			b.setDescription(results.getString("description"));
			b.setDownloadable(results.getBoolean("downloadable"));
			b.setDiskSize(results.getLong("disk_size"));
		} else {
			b.setId(results.getInt(prefix+".id"));
			b.setUserId(results.getInt(prefix+".user_id"));
			b.setName(results.getString(prefix+".name"));
			b.setUploadDate(results.getTimestamp(prefix+".uploaded"));
			b.setPath(results.getString(prefix+".path"));
			b.setDescription(results.getString(prefix+".description"));
			b.setDownloadable(results.getBoolean(prefix+".downloadable"));
			b.setDiskSize(results.getLong(prefix+".disk_size"));
		}
		return b;
	}

	/**
	 * Deletes all benchmarks that this user has in their recycle bin from both the 
	 * database and from disk
	 * @param userId The userId in question
	 * @return True on success, false otherwise
	 * @author Eric Burns
	 */
	public static boolean setRecycledBenchmarksToDeleted(int userId) {
		Connection con=null;
		CallableStatement procedure=null;
		ResultSet results=null;
		try {
			con=Common.getConnection();
			procedure=con.prepareCall("CALL GetRecycledBenchmarkPaths(?)");
			procedure.setInt(1,userId);
			results=procedure.executeQuery();
			
			while (results.next()) {
				Util.safeDeleteDirectory(results.getString("path")); 
			}
			Common.safeClose(procedure);
			procedure=con.prepareCall("CALL SetRecycledBenchmarksToDeleted(?)");
			procedure.setInt(1, userId);
			procedure.executeUpdate();
			
			return true;
		} catch (Exception e) {
			
		} finally {
			Common.safeClose(con);
			Common.safeClose(procedure);
			Common.safeClose(results);
		}
		return false;
	}

	/**
	 * Sets the "recycled" flag in the database to the given value. 
	 * @param id the id of the benchmark to recycled
	 * @param state True to set as recycled, false to remove recycled tag
	 * @return True if the operation was a success, false otherwise
	 * @author Eric Burns
	 */
	private static boolean setRecycledState(int id, boolean state){
		Connection con = null;			
		CallableStatement procedure=null;
		
		try {

			con = Common.getConnection();
			procedure = con.prepareCall("{CALL SetBenchmarkRecycledValue(?, ?)}");
			procedure.setInt(1, id);
			procedure.setBoolean(2,state);
			procedure.executeUpdate();		
			return true;
		} catch (Exception e){		
			log.error(e.getMessage(), e);		
		} finally {
			Common.safeClose(con);
			Common.safeClose(procedure);
		}
		return false;
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
		CallableStatement procedure=null;
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
			Common.safeClose(con);
			Common.safeClose(procedure);
		}

		log.debug(String.format("Benchmark [id=%d] failed to be updated.", id));
		return false;
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
	 * @param bench The benchmark that might have dependencies
	 * @param con database connection
	 * @param spaceId the id of the space where the axiom benchmarks lie
	 * @param linked true if the depRootSpace is the same as the first directory in the include statement
	 * @param userId the user's Id
	 * @param foundDependencies Dependencies for the benchmark that have already been found
	 * @return the data structure that has information about depedencies
	 * @author Benton McCune

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
	 * Gets rid of all the attributes a benchmark currently has in the database
	 * @param benchId The ID of the benchmark in question
	 * @param con The connection to make the call on 
	 * @return True on success, false on error
	 * @author Eric Burns
	 */
	
	public static boolean clearAttributes(int benchId, Connection con) {
		CallableStatement procedure=null;
		try {
			procedure=con.prepareCall("{CALL ClearBenchAttributes(?)}");
			procedure.setInt(1, benchId);
			procedure.executeUpdate();
			return true;
		} catch (Exception e) {
			log.error("clearAttributes says "+e.getMessage(),e);
		} finally {
			Common.safeClose(procedure);
		}
		return false;
	}
	
    /**
     * Re-process benchmarks in the given space.  Regular users can only re-process benchmarks they own.
     * Community leaders can reprocess any benchmarks.
     * @param spaceId the ID of the space in which to look for benchmarks to re-process
     * @param p the Processor to apply
     * @param hierarchy whether to process the hierarchy rooted at the space with the given ID, or just that space
     * @param userId the ID of the user requesting re-processing
     * @param clearOldAttrs true iff we should drop the old attributes we had from any earlier processing
     * @param isCommunityLeader true iff the user with the given userID is a community leader 
     * @author Eric Burns
     * @return The ID of an UploadStatus object for tracking progress of this request
     */
    public static Integer process(int spaceId,Processor p, boolean  hierarchy,int userId,boolean clearOldAttrs) {
		Integer statusId = Uploads.createBenchmarkUploadStatus(spaceId, userId);
		Uploads.benchmarkFileUploadComplete(statusId);
		Uploads.fileExtractComplete(statusId);
		Uploads.processingBegun(statusId);
		final int s = spaceId;
		final Processor proc=p;
		final boolean h=hierarchy;
		final int u=userId;
		final boolean c=clearOldAttrs;
		final Integer st=statusId;
		int comm=Spaces.getCommunityOfSpace(spaceId);
		
		final boolean l=Permissions.get(userId, comm).isLeader();
		//It will delay the redirect until this method is finished which is why a new thread is used
		Util.threadPoolExecute(new Runnable() {
			@Override
			public void run(){
				try {
				    process(s,proc,h,u,c,st,l);
				    Uploads.benchmarkEverythingComplete(st);
				} catch (Exception e) {
					
				}
				
			}
		});	
		
		return statusId;
	}
	
	/**
	 * Runs the given benchmark processor on all benchmarks in the given space (hierarchy)
	 * @param spaceId The ID of the relevant space
	 * @param p The benchmark processor to use
	 * @param hierarchy True if we want to run on the hierarchy, false otherwise
	 * @param statusId The ID of the UploadStatus object associated with this process, or null
	 * if one does not yet exist
	 * @param userId The ID of the user making this processing request
	 * @param clearOldAttrs If true, all existing benchmark attributes will be removed for every benchmark being processed.
	 * If false, old attributes are not cleared, but will be overwritten by new attributes with the same names
	 * @param isCommunityLeader True if the user is a community leader for this community and false otherwise
	 * @return The status ID on success, -1 otherwise
	 * @author Eric Burns

	 */
	
	private static boolean process(int spaceId, Processor p, boolean hierarchy, int userId, boolean clearOldAttrs, Integer statusId, 
				    boolean isCommunityLeader) {
	    Connection con=null;
		
	    log.info("Processing benchmarks in space "+new Integer(spaceId));
	    if (isCommunityLeader) {
			log.debug("User "+new Integer(userId)+" is a community leader, so they can process any benchmarks");
	    }

	    try {
			
			con=Common.getConnection();
			List<Benchmark> benchmarks=Benchmarks.getBySpace(spaceId);
			boolean success=Benchmarks.attachBenchAttrs(benchmarks, p,statusId);
			
			if (! success) {
				log.error("there was an error running the processor on each benchmark");
				return false;
			}
			int incrementCounter=0;
			Timer timer=new Timer();
			for (Benchmark b : benchmarks) {
			    //only work on the benchmarks the given user owns if they are not a community leader
			    if (!isCommunityLeader && b.getUserId()!=userId) {
					log.debug("Skipping benchmark "+b.getName());
					continue;
			    }
			    if (clearOldAttrs) {
			    	Benchmarks.clearAttributes(b.getId(),con);
			    }
					
			    Properties attrs=b.getAttributes();
			    if (!addAttributeSetToDbIfValid(con,attrs,b,statusId)) {
					return false;	
			    }
			    //updates the type of the benchmark with the new processor
			    Benchmarks.updateDetails(b.getId(), b.getName(), b.getDescription(), b.isDownloadable(), p.getId());
			    
			    incrementCounter++;
				if (timer.getTime()>R.UPLOAD_STATUS_TIME_BETWEEN_UPDATES) {
					Uploads.incrementCompletedBenchmarks(statusId,incrementCounter);
					incrementCounter=0;
					timer.reset();
				}
			}
			if (incrementCounter>0) {
				Uploads.incrementCompletedBenchmarks(statusId,incrementCounter);
			}
			success=true;
			if (hierarchy) {
			    List<Space> spaces=Spaces.getSubSpaceHierarchy(spaceId, userId);
			    for (Space s : spaces) {
			    	success=success && Benchmarks.process(s.getId(), p, false, userId,clearOldAttrs,statusId, isCommunityLeader);
			    }
			}
			return success;
			
	    } catch (Exception e) {
	    	log.error("process says "+e.getMessage(),e);
	    } finally {
	    	Common.safeClose(con);
	    }
	    return false;
		
	}
	/**
	 * Returns the ID of every benchmark a user owns that is orphaned
	 * @param userId The ID of the user who owns all the benchmarks to be returned
	 * @return A list of orphaned benchmark IDs owned by the given user, or null on error.
	 */
	public static List<Integer> getOrphanedBenchmarks(int userId) {
		Connection con=null;
		CallableStatement procedure=null;
		ResultSet results=null;
		List<Integer> ids=new ArrayList<Integer>();

		try {
			con=Common.getConnection();
			procedure=con.prepareCall("{CALL GetOrphanedBenchmarkIds(?)}");
			procedure.setInt(1, userId);
			results= procedure.executeQuery();
			while (results.next()) {
				ids.add(results.getInt("id"));
			}
			return ids;
		} catch (Exception e) {
			log.error(e.getMessage(),e);
		}finally {
			Common.safeClose(con);
			Common.safeClose(procedure);
			Common.safeClose(results);
		}
		
		return null;
	}
	
	/**
	 * Recycles all of the benchmarks a user has that are not in any spaces
	 * @param userId The ID of the user who will have their benchmarks recycled
	 * @return True on success or false otherwise
	 */
	public static boolean recycleOrphanedBenchmarks(int userId) {
		List<Integer> ids=getOrphanedBenchmarks(userId);
		//on error
		if (ids==null) {
			return false;
		}
		
		try {
			
			boolean success=true;
			for (Integer id : ids) {
				success=success && Benchmarks.recycle(id);
			}
			return success;
		} catch (Exception e) {
			log.error(e.getMessage(),e );
		}
		
		return false;
	}
	
	/**
	 * Gets every Benchmark that shares a space with the given user
	 * @param userId The ID of the user
	 * @return The list of Benchmarks, or null on error
	 */
	public static List<Benchmark> getBenchmarksInSharedSpaces(int userId) {
		Connection con=null;
		CallableStatement procedure=null;
		ResultSet results=null;
		try {
			con=Common.getConnection();
			procedure=con.prepareCall("{CALL GetBenchmarksInSharedSpaces(?)}");
			procedure.setInt(1,userId);
			
			results=procedure.executeQuery();
			List<Benchmark> Benchmarks=new ArrayList<Benchmark>();
			while (results.next()) {
				Benchmark b=resultToBenchmark(results,"");
				Processor t = Processors.resultSetToProcessor(results, "types");
				b.setType(t);
				Benchmarks.add(b);
			}
			return Benchmarks;
		}catch (Exception e) {
			log.error(e.getMessage(),e);
		} finally {
			Common.safeClose(con);
			Common.safeClose(procedure);
			Common.safeClose(results);
		}
		return null; //error
	}
	/**
	 * @return a list of all Benchmarks that reside in a public space
	 * @author Benton McCune
	 */
	
	public static List<Benchmark> getPublicBenchmarks(){
		Connection con = null;	
		CallableStatement procedure = null;
		ResultSet results = null;
		try {
			con = Common.getConnection();
			 procedure = con.prepareCall("{CALL GetPublicBenchmarks()}");				
			 results = procedure.executeQuery();
			List<Benchmark> Benchmarks = new LinkedList<Benchmark>();
			
			while(results.next()){
				Benchmark s=resultToBenchmark(results,"");
				Processor t = Processors.resultSetToProcessor(results, "types");
				s.setType(t);
				Benchmarks.add(s);
			}									
			return Benchmarks;
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
	 * Retrieves a list of every Benchmark the given user is allowed to use. Used for quick jobs.
	 * Benchmarks a user can see include Benchmarks they own, Benchmarks in public spaces,
	 * and Benchmarks in spaces the user is also in
	 * @param userId The user to get benchmarks for
	 * @return A list of benchmarks that the given user can see
	 */
	public static List<Benchmark> getByUser(int userId) {
		try {
			//will stores Benchmarks according to their IDs, used to remove duplicates
			HashMap<Integer,Benchmark> uniqueBenchmarks=new HashMap<Integer,Benchmark>();
			for (Benchmark s : getByOwner(userId)) {
				uniqueBenchmarks.put(s.getId(), s);
			}
			for (Benchmark s : Benchmarks.getPublicBenchmarks()) {
				uniqueBenchmarks.put(s.getId(), s);
			}
			
			for (Benchmark s : Benchmarks.getBenchmarksInSharedSpaces(userId)) {
				uniqueBenchmarks.put(s.getId(), s);
			}
			
			List<Benchmark> Benchmarks=new ArrayList<Benchmark>();
			for (Benchmark s : uniqueBenchmarks.values()) {
				Benchmarks.add(s);
			}
			return Benchmarks;
		} catch (Exception e) {
			log.error(e.getMessage(),e);
		}
		return null;
	}
	
	
	/**
	 * Filters a list of benchmarks using the given query
	 * @param Benchmarks The list of Benchmarks to filter
	 * @param searchQuery Query for the Benchmarks. Not case sensitive
	 * @return A subset of the given Benchmarks where, for every Benchmark returned, either the name
	 * or the description includes the search query.
	 */
	protected static List<Benchmark> filterBenchmarks(List<Benchmark> benchmarks, String searchQuery) {
		//no filtering is necessary if there's no query
		if (searchQuery==null || searchQuery=="") {
			return benchmarks;
		}
		searchQuery=searchQuery.toLowerCase();
		List<Benchmark> filteredBenchmarks=new ArrayList<Benchmark>();
		for (Benchmark b : filteredBenchmarks) {
			try {
				if (b.getName().toLowerCase().contains(searchQuery) || b.getDescription().toLowerCase().contains(searchQuery)) {
					filteredBenchmarks.add(b);
				}
			} catch (Exception e) {
				log.warn("filtering benchmarks had an exception for Benchmark id= " +b.getId());
			}	
		}
		
		return filteredBenchmarks;
	}

	
	
	
	/**
	 * Returns the Benchmarks needed to populate a DataTables page for a given user. Benchmarks include all
	 * Benchmarks the user can see
	 * @param startingRecord Index of Benchmark to start at
	 * @param recordsPerPage Number of Benchmarks to return. May return fewer if recordsPerPage is greater than the total number of Benchmarks
	 * @param isSortedASC True if sorted ascending, false otherwise
	 * @param indexOfColumnSortedBy  The column index of the datatable column to sort on
	 * @param searchQuery Query to filter Benchmarks by. Filter examines name and description
	 * @param userId ID of user to get Benchmarks for
	 * @param totals Size 2 array that, on return, will contain the total number of records as the first element
	 * and the total number of elements after filtering as the second element
	 * @return The list of benchmarks to display in the table
	 */
	public static List<Benchmark> getBenchmarksForNextPageByUser(int startingRecord, int recordsPerPage, boolean isSortedASC, 
			int indexOfColumnSortedBy, String searchQuery, int userId,int[] totals) {
		List<Benchmark> benchmarks=Benchmarks.getByUser(userId);
		
		totals[0]=benchmarks.size();
		benchmarks=Benchmarks.filterBenchmarks(benchmarks, searchQuery);

		totals[1]=benchmarks.size();
		BenchmarkComparator compare=new BenchmarkComparator(indexOfColumnSortedBy,isSortedASC);
		return Util.handlePagination(benchmarks, compare, startingRecord, recordsPerPage);

	}
}


