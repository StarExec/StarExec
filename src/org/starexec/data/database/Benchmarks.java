package org.starexec.data.database;

import org.apache.commons.io.FileUtils;
import org.starexec.constants.PaginationQueries;
import org.starexec.constants.DB;
import org.starexec.constants.R;
import org.starexec.data.to.*;
import org.starexec.data.to.compare.BenchmarkComparator;
import org.starexec.exceptions.StarExecException;
import org.starexec.exceptions.StarExecValidationException;
import org.starexec.logger.StarLogger;
import org.starexec.servlets.UploadBenchmark;
import org.starexec.util.*;
import org.starexec.util.Timer;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.function.Supplier;

/**
 * Handles all database interaction for benchmarks.
 */
public class Benchmarks {
	private static final StarLogger log = StarLogger.getLogger(Benchmarks.class);

	/**
	 * Deletes a benchmark and permanently removes it from the database. This is NOT the normal procedure for
	 * deleting a
	 * benchmark. It is used for testing. Calling "delete" is typically what is desired
	 *
	 * @param id The ID of the benchmark
	 * @return True on success and false otherwise
	 */
	public static boolean deleteAndRemoveBenchmark(int id) {
		Benchmark b = Benchmarks.getIncludeDeletedAndRecycled(id, false);
		if (b == null) {
			return true;
		}

		boolean success = true;
		if (!b.isDeleted()) {
			success = Benchmarks.delete(id);
		}
		if (!success) {
			log.warn("there was an error deleting benchmark with id = " + id);
			return false;
		}
		Connection con = null;
		try {
			con = Common.getConnection();
			return Benchmarks.removeBenchmarkFromDatabase(id, con);
		} catch (Exception e) {
			log.error("deleteAndRemoveBenchmark", e);
		} finally {
			Common.safeClose(con);
		}
		return false;
	}

	/**
	 * Recycles all benchmarks that are owned by the given user in the given collection
	 *
	 * @param benchmarks The benchmarks to potentially recycle
	 * @param userId The user who owns the benchmarks to recycle
	 * @return True on success and false on any error
	 */
	public static boolean recycleAllOwnedByUser(Collection<Benchmark> benchmarks, int userId) {
		boolean success = true;
		for (Benchmark b : benchmarks) {
			if (b.getUserId() == userId) {
				success = success && Benchmarks.recycle(b.getId());
			}
		}
		return success;
	}

	/**
	 * Adds the given attributes to the given benchmark
	 *
	 * @param attrs The attrs to add. Old attributes sharing keys will be overwritten
	 * @param benchmark The benchmark to add attributes to
	 * @param statusId The ID of a benchmark status upload object, or null if we aren't using one
	 */
	public static void addAttributeSetToDbIfValid(Map<String, String> attrs, Benchmark benchmark, Integer
			statusId) {
		Connection con = null;
		try {
			con = Common.getConnection();
			addAttributeSetToDbIfValid(con, attrs, benchmark, statusId);
		} catch (Exception e) {
			log.error("addAttributeSetToDbIfValid", e);
		} finally {
			Common.safeClose(con);
		}
	}

	/**
	 * Add the given set of benchmark attributes to the database, if isBenchValid() returns true for the attrs.
	 *
	 * @param con the db Connection to use
	 * @param attrs the attributes
	 * @param benchmark the Benchmark
	 * @param statusId the id of the upload page, or null if there isn't one
	 * @return True on success and false otherwise
	 */
	private static boolean addAttributeSetToDbIfValid(
			Connection con, Map<String, String> attrs, Benchmark benchmark, Integer statusId
	) {
		if (!Benchmarks.isBenchValid(attrs)) {
			Uploads.setBenchmarkErrorMessage(
					statusId, ("The benchmark processor did not validate the benchmark " + benchmark.getName() + " (" +
							R.VALID_BENCHMARK_ATTRIBUTE + " was not true)."));
			return false;
		}

		// Discard the valid attribute, we don't need it
		attrs.remove(R.VALID_BENCHMARK_ATTRIBUTE);
		log.info("bench is valid.  Adding " + attrs.entrySet().size() + " attributes");
		// For each attribute (key, value)...
		int count = 0;
		for (String key : attrs.keySet()) {
			String val = attrs.get(key);
			// Add the attribute to the database
			count++;
			log.debug("Adding att number " + count + " " + key + ", " + val + " to bench " + benchmark.getId());

			if (!Benchmarks.addBenchAttr(con, benchmark.getId(), key, val)) {
				Uploads.setBenchmarkErrorMessage(
						statusId, "Problem adding the following attribute-value pair to the db, for benchmark " +
								benchmark.getId() + ": " + key + ", " + val);
				return false;
			}
		}
		return true;
	}

	/**
	 * Adds an attribute to an existing benchmark
	 *
	 * @param benchId The ID of the benchmark
	 * @param key The attribute key. Will overwrite any other attribute with the same key.
	 * @param val The attribute val
	 */
	public static void addBenchAttr(int benchId, String key, String val) {
		Connection con = null;
		try {
			con = Common.getConnection();
			addBenchAttr(con, benchId, key, val);
		} catch (Exception e) {
			log.error("addBenchAttr", e);
		} finally {
			Common.safeClose(con);
		}
	}

	/**
	 * Adds a new attribute to a benchmark
	 *
	 * @param con The connection to make the insertion on
	 * @param benchId The id of the benchmark the attribute is for
	 * @param key The key of the attribute
	 * @param val The value of the attribute
	 * @return True if the operation was a success, false otherwise
	 * @author Tyler Jensen
	 */
	private static boolean addBenchAttr(Connection con, int benchId, String key, String val) {
		CallableStatement procedure = null;
		Supplier<String> trace = ()->
			  "\n\tbenchId :" + benchId
			+ "\n\tkey:     " + key
			+ "\n\tval:     " + val
		;
		try {
			if (key.length() > 128) {
				log.warn("addBenchAttr", "key exceeds max length" + trace.get());
			}
			if (val.length() > 128) {
				log.warn("addBenchAttr", "val exceeds max length" + trace.get());
			}
			procedure = con.prepareCall("{CALL AddBenchAttr(?, ?, ?)}");
			procedure.setInt(1, benchId);
			procedure.setString(2, key);
			procedure.setString(3, val);
			procedure.executeUpdate();
			return true;
		} catch (Exception e) {
			log.error("addBenchAttr", e);
		} finally {
			Common.safeClose(procedure);
		}
		return false;
	}

	/**
	 * Adds the benchmark dependency to starexec db
	 *
	 * @param primaryBenchId the bench that is dependent on another bench
	 * @param secondaryBenchId e.g. the axiom
	 * @param includePath the path that will be used locally at execution time
	 * @param con db connection
	 * @return
	 * @author Benton McCune
	 */
	private static Boolean addBenchDependency(
			int primaryBenchId, Integer secondaryBenchId, String includePath, Connection con
	) {
		CallableStatement procedure = null;
		try {
			// Setup normal information for the benchmark dependency
			procedure = con.prepareCall("{CALL AddBenchDependency(?, ?, ?)}");
			procedure.setInt(1, primaryBenchId);
			procedure.setInt(2, secondaryBenchId);
			procedure.setString(3, includePath);

			// Execute procedure and get back the benchmark's id
			procedure.executeUpdate();
			return true;
		} catch (Exception e) {
			log.error("addBenchDependency",
				    "\tprimaryBenchId:   " + primaryBenchId
				+ "\n\tsecondaryBenchId: " + secondaryBenchId
				+ "\n\tincludePath:      " + includePath,
				e
			);
		} finally {
			Common.safeClose(procedure);
		}
		return false;
	}

	/**
	 * Adds the benchmark dependency to starexec db
	 *
	 * @param primaryBenchId the bench that is dependent on another bench
	 * @param secondaryBenchId e.g. the axiom
	 * @param includePath the path that will be used locally at execution time
	 * @return true on success and false otherwise
	 * @author Benton McCune
	 */
	public static Boolean addBenchDependency(int primaryBenchId, Integer secondaryBenchId, String includePath) {
		Connection con = null;
		try {
			con = Common.getConnection();
			return addBenchDependency(primaryBenchId, secondaryBenchId, includePath, con);
		} catch (Exception e) {
			log.error("addBenchDependency", e);
		} finally {
			Common.safeClose(con);
		}
		return false;
	}

	/**
	 * Add a benchmark.
	 *
	 * @param bench the benchmark to be added
	 * @param statusId The ID of an upload status if one exists for this operation, null otherwise
	 * @return benchmark returns the benchmark added (not typically needed)
	 * @author Benton McCune
	 */
	public static Benchmark add(Benchmark bench, Integer statusId) {
		Connection con = null;
		try {
			con = Common.getConnection();
			return add(bench, statusId, con);
		} catch (Exception e) {
			log.debug("add", e);
		} finally {
			Common.safeClose(con);
		}
		return null;
	}

	/**
	 * Add a benchmark.
	 *
	 * @param benchmark the benchmark to be added
	 * @param statusId The ID of an upload status if one exists for this operation, null otherwise
	 * @return benchmark returns the benchmark added (not typically needed)
	 * @throws Exception
	 * @author Benton McCune
	 */
	protected static Benchmark add(Benchmark benchmark, Integer statusId, Connection con) {
		CallableStatement procedure = null;

		try {
			Common.beginTransaction(con);

			Map<String, String> attrs = benchmark.getAttributes();
			// Setup normal information for the benchmark
			procedure = con.prepareCall("{CALL AddBenchmark(?, ?, ?, ?, ?, ?, ?, ?)}");
			procedure.setString(1, benchmark.getName());
			procedure.setString(2, benchmark.getPath());
			procedure.setBoolean(3, benchmark.isDownloadable());
			procedure.setInt(4, benchmark.getUserId());
			procedure.setInt(5, Benchmarks.isBenchValid(attrs) ? benchmark.getType().getId() : R.NO_TYPE_PROC_ID);
			procedure.setLong(6, FileUtils.sizeOf(new File(benchmark.getPath())));
			procedure.setString(7, benchmark.getDescription());
			procedure.registerOutParameter(8, java.sql.Types.INTEGER);

			// Execute procedure and get back the benchmark's id
			procedure.executeUpdate();
			benchmark.setId(procedure.getInt(8));

			// If the benchmark is valid according to its processor...

			if (!addAttributeSetToDbIfValid(con, attrs, benchmark, statusId)) {
				return null;
			}

			//do previously validated dependencies here
			for (BenchmarkDependency d : benchmark.getDependencies()) {
				Benchmarks.addBenchDependency(benchmark.getId(), d.getSecondaryBench().getId(), d.getDependencyPath(),
				                              con
				);
			}
			Common.endTransaction(con);// benchmarks should be in db now
			return benchmark;
		} catch (Exception e) {
			log.error("add", e);
			Common.doRollback(con);
			return null;
		} finally {
			Common.safeClose(procedure);
		}
	}

	/**
	 * Adds a single benchmark to the database under the given spaceId
	 *
	 * @param benchmark The benchmark to add to the database
	 * @param spaceId The id of the space the benchmark will belong to
	 * @param statusId the id for the upload page for adding this benchmark, if there is an upload page for this
	 * action.
	 * Otherwise, null
	 * @return The new benchmark ID on success, -1 otherwise
	 * @throws Exception Any database error that gets thrown
	 * @author Tyler Jensen
	 */
	public static int addAndAssociate(Benchmark benchmark, Integer spaceId, Integer statusId) throws SQLException {
		if (Benchmarks.isBenchValid(benchmark.getAttributes())) {
			Connection con = null;
			try {
				con = Common.getConnection();

				// Add benchmark to database
				int benchId = Benchmarks.add(benchmark, statusId, con).getId();
				if (benchId >= 0) {
					if (spaceId != null) {
						Benchmarks.associate(benchId, spaceId, con);
					}
					log.debug("bench successfully added");
					return benchId;
				}
			} catch (SQLException e) {
				log.error("addAndAssociate", "rethrowing exception", e);
				throw e;
			} finally {
				Common.safeClose(con);
			}
		}
		log.debug("Add called on invalid benchmark, no additions will be made to the database");
		Uploads.setBenchmarkErrorMessage(
				statusId, "Benchmark validation failed for benchmark " + benchmark.getName() + ".");

		return -1;
	}

	/**
	 * Adds a list of benchmarks to the database and associates them with the given space ID
	 *
	 * @param benchmarks
	 * @param spaceId
	 * @param statusId
	 * @return
	 * @throws Exception
	 */
	protected static List<Integer> addAndAssociate(List<Benchmark> benchmarks, Integer spaceId, Integer statusId)
	throws SQLException, StarExecException {
		ArrayList<Integer> benchmarkIds = new ArrayList<>();
		log.info("in add (list) method (no con parameter )- adding " + benchmarks.size() + " benchmarks to space " +
				         spaceId);
		int incrementCounter = 0;
		Timer timer = new Timer();
		for (Benchmark b : benchmarks) {
			int id = Benchmarks.addAndAssociate(b, spaceId, statusId);
			if (id < 0) {
				String message = ("failed to add bench " + b.getName());
				Uploads.setBenchmarkErrorMessage(statusId, message);
				//Note - this does not occur when Benchmark fails validation even though those benchmarks not added
				throw new StarExecValidationException(String.format("Failed to add benchmark [%s] to space [%d]", b.getName(), spaceId));
			}

			benchmarkIds.add(id);
			incrementCounter++;
			if (timer.getTime() > R.UPLOAD_STATUS_TIME_BETWEEN_UPDATES) {
				Uploads.incrementCompletedBenchmarks(statusId, incrementCounter);
				incrementCounter = 0;
				timer.reset();
			}
		}
		Uploads.incrementCompletedBenchmarks(statusId, incrementCounter);

		log.info(String.format("[%d] new benchmarks added to space [%d]", benchmarks.size(), spaceId));
		return benchmarkIds;
	}

	/**
	 * Runs the given benchmark processor on the list of benchmarks before adding them to the database and associates
	 * them with the given spaceId. The benchmark types are also processed based on the type of the first benchmark
	 * only. This method will also introduced dependencies if the benchmark processor produces the right attributes.
	 *
	 * @param benchmarks The list of benchmarks to add
	 * @param spaceId The space the benchmarks will belong to. If null, it is not added to a space
	 * @param depRootSpaceId the id of the space where the axiom benchmarks lie
	 * @param linked true if the depRootSpace is the same as the first directory in the include statement
	 * @param statusId statusId The ID of an upload status if one exists for this operation, null otherwise
	 * @return True if the operation was a success, false otherwise
	 */
	public static List<Integer> processAndAdd(
			List<Benchmark> benchmarks, Integer spaceId, Integer depRootSpaceId, Boolean linked, Integer statusId
	) {
		return processAndAdd(benchmarks, spaceId, depRootSpaceId, linked, statusId, true);
	}

	/**
	 * Runs the given benchmark processor on the list of benchmarks before adding them to the database and associates
	 * them with the given spaceId. The benchmark types are also processed based on the type of the first benchmark
	 * only. This method will also introduced dependencies if the benchmark processor produces the right attributes.
	 *
	 * @param benchmarks The list of benchmarks to add
	 * @param spaceId The space the benchmarks will belong to. If null, it is not added to a space
	 * @param depRootSpaceId the id of the space where the axiom benchmarks lie
	 * @param linked true if the depRootSpace is the same as the first directory in the include statement
	 * @param statusId statusId The ID of an upload status if one exists for this operation, null otherwise
	 * @param usesDeps if set to true check dependencies, otherwise ignore dependency related messages
	 */
	public static List<Integer> processAndAdd(
			List<Benchmark> benchmarks, Integer spaceId, Integer depRootSpaceId, Boolean linked, Integer statusId,
			Boolean usesDeps
	) {
		Connection con = null;
		try {
			con = Common.getConnection();
			return processAndAdd(benchmarks, spaceId, depRootSpaceId, linked, statusId, usesDeps, con);
		} catch (StarExecException e) {
			log.debug("processAndAdd", e);
			return null;
		} catch (Exception e) {
			log.error("processAndAdd", e);
			return null;
		} finally {
			Common.safeClose(con);
		}
	}

	/**
	 * Runs the given benchmark processor on the list of benchmarks before adding them to the database and associates
	 * them with the given spaceId. The benchmark types are also processed based on the type of the first benchmark
	 * only. This method will also introduced dependencies if the benchmark processor produces the right attributes.
	 *
	 * @param benchmarks The list of benchmarks to add
	 * @param spaceId The space the benchmarks will belong to. If null, it is not added to a space
	 * @param depRootSpaceId the id of the space where the axiom benchmarks lie
	 * @param linked true if the depRootSpace is the same as the first directory in the include statement
	 * @param statusId statusId The ID of an upload status if one exists for this operation, null otherwise
	 * @param usesDeps if set to true check dependencies, otherwise ignore dependency related messages
	 * @return True if the operation was a success, false otherwise
	 * @author Benton McCune
	 */
	public static List<Integer> processAndAdd(
			List<Benchmark> benchmarks, Integer spaceId, Integer depRootSpaceId, Boolean linked, Integer statusId,
			Boolean usesDeps, Connection con
	) throws IOException, SQLException, StarExecException {
		if (!benchmarks.isEmpty()) {
			log.info("Adding (with deps) " + benchmarks.size() + " to Space " + spaceId);
			// Get the processor of the first benchmark (they should all have the same processor)
			Processor p = Processors.get(benchmarks.get(0).getType().getId());

			log.info("About to attach attributes to " + benchmarks.size());

			Benchmarks.attachBenchAttrs(benchmarks, p, statusId);
			if (usesDeps) {
				boolean success = Benchmarks.validateDependencies(benchmarks, depRootSpaceId, linked, statusId);
				if (!success) {
					Uploads.setBenchmarkErrorMessage(
							statusId,
							"Benchmark dependencies failed to validate. Please check your processor output"
					);
				return null;
				}
			}

			// Next add them to the database (must happen AFTER they are processed and have dependencies
			// validated);
			return Benchmarks.addAndAssociate(benchmarks, spaceId, statusId);
		} else {
			log.info("No benches to add with this call to addWithDeps from space " + spaceId);
			return new ArrayList<>();
		}
	}

	/**
	 * Associates a single benchmark with a single space.
	 *
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
		} catch (Exception e) {
			log.error("associate", e);
			Common.doRollback(con);
		} finally {
			Common.safeClose(con);
		}
		return false;
	}

	/**
	 * Associates a single benchmark with a single space
	 *
	 * @param benchId The ID of the benchmark to put in a space.
	 * @param spaceId The ID of the space to add the benchmark to
	 * @param con The open connection to make the SQL call on
	 * @return True on success and false otherwise
	 */
	public static boolean associate(int benchId, int spaceId, Connection con) {
		CallableStatement procedure = null;

		try {
			procedure = con.prepareCall("{CALL AssociateBench(?, ?)}");
			procedure.setInt(1, benchId);
			procedure.setInt(2, spaceId);
			procedure.executeUpdate();

			return true;
		} catch (Exception e) {
			log.error("associate", e);
		} finally {
			Common.safeClose(procedure);
		}
		return false;
	}

	/**
	 * Associates the benchmarks with the given ids to the given space
	 *
	 * @param benchIds The list of benchmark ids to associate with the space
	 * @param spaceId The id of the space the benchmarks will be associated with
	 * @return True if the operation was a success, false otherwise
	 * @author Tyler Jensen
	 */
	public static boolean associate(List<Integer> benchIds, int spaceId) {
		Connection con = null;
		try {
			con = Common.getConnection();

			for (int benchId : benchIds) {
				associate(benchId, spaceId, con);
			}

			return true;
		} catch (Exception e) {
		    log.error("associate: " + e.toString());
		} finally {
			Common.safeClose(con);
		}
		return false;
	}

	/**
	 * Given a set of benchmarks and a processor, this method runs each benchmark through the processor and adds a
	 * hashmap of attributes to the benchmark that are given from the processor. Attributes are NOT added to the
	 * database! They are just added to the benchmark objects themselves.
	 *
	 * @param benchmarks The set of benchmarks to get attributes for
	 * @param p The processor to run each benchmark on
	 * @param statusId The ID of an upload status if one exists for this operation, null otherwise
	 * @return True if the operation is successful and false otherwise
	 */
	public static Boolean attachBenchAttrs(List<Benchmark> benchmarks, Processor p, Integer statusId)
			throws IOException, StarExecException {
		// if we are using the no_type processor, we do not need to actually execute anything-- just validate every
		// benchmark.
		if (p.getId() == Processors.getNoTypeProcessor().getId()) {
			for (Benchmark b : benchmarks) {
				Map<String, String> prop = new HashMap<>();
				prop.put(R.VALID_BENCHMARK_ATTRIBUTE, "true");
				b.setAttributes(prop);
			}
			Uploads.incrementValidatedBenchmarks(statusId, benchmarks.size());
			return true;
		}

		log.info("Beginning processing for " + benchmarks.size() + " benchmarks");
		int count = benchmarks.size();
		// For each benchmark in the list to process...
		int validatedCounter = 0; //stores the number of benchmarks that have been validated since the last update
		int failedCounter = 0; //stores the TOTAL number of benchmarks that failed
		Timer timer = new Timer();
		for (Benchmark b : benchmarks) {
			List<File> files = new ArrayList<>();
			files.add(new File(p.getFilePath()));
			files.add(new File(b.getPath()));
			File sandbox = Util.copyFilesToNewSandbox(files);
			String benchPath = new File(sandbox, new File(b.getPath()).getName()).getAbsolutePath();
			File working = new File(sandbox, new File(p.getFilePath()).getName());
			// Run the processor on the benchmark file
			log.info("executing - " + p.getExecutablePath() + " \"" + b.getPath() + "\"");
			String[] procCmd = new String[2];

			procCmd[0] = "./" + R.PROCESSOR_RUN_SCRIPT;
			procCmd[1] = benchPath;
			String propstr = null;
			propstr = Util.executeSandboxCommand(procCmd, null, working);

			checkProcessorOutput(propstr);

			FileUtils.deleteQuietly(sandbox);
			// Load results into a properties file
			Properties prop = new Properties();

			prop.load(new StringReader(propstr));

			log.debug("read this string from the processor: " + propstr);
			log.debug("read " + prop.size() + " properties");

			// Attach the attributes to the benchmark
			Map<String, String> attrs = new HashMap<>();

			for (Object o : prop.keySet()) {
				attrs.put((String) o, (String) prop.get(o));
			}
			b.setAttributes(attrs);
			count--;
			if (Benchmarks.isBenchValid(attrs)) {
				validatedCounter++;
				if (timer.getTime() > R.UPLOAD_STATUS_TIME_BETWEEN_UPDATES) {
					Uploads.incrementValidatedBenchmarks(statusId, validatedCounter);
					validatedCounter = 0;
					timer.reset();
				}
			} else {
				failedCounter++;
				Uploads.incrementFailedBenchmarks(statusId, 1);
				if (failedCounter < R.MAX_FAILED_VALIDATIONS) {
					if (propstr.length() > DB.TEXT_FIELD_LEN) {
						propstr = propstr.substring(0, DB.TEXT_FIELD_LEN);
					}
					Uploads.addFailedBenchmark(statusId, b.getName(), propstr);
					String message = b.getName() + " failed validation";
					log.debug(message);
					Uploads.setBenchmarkErrorMessage(statusId, message);
				} else {
					String message = "Major Benchmark Validation Errors - examine your validator";
					log.warn(message + ", status id = " + statusId);
					Uploads.setBenchmarkErrorMessage(statusId, message);
				}
			}
			log.info(b.getName() + " processed. " + count + " more benchmarks to go.");
		}
		if (validatedCounter > 0) {
			Uploads.incrementValidatedBenchmarks(statusId, validatedCounter);
		}
		return true;
	}

	/**
	 * Checks the processors output string for errors.
	 */
	private static void checkProcessorOutput(String processorOutput) throws StarExecException {
		final String method = "checkProcessorOutput";
		log.entry(method);
		if (processorOutput.contains("command not found")) {
			throw new StarExecException(
					String.format("Processor used a command that StarExec does not recognize.%nProcessor Output:%s",
					              processorOutput
					));
		}
	}

	/**
	 * Permanently removes a benchmark from the database
	 *
	 * @param benchId The ID of the benchmark to remove
	 * @param con The open connection to make the SQL call on
	 * @return True on success and false otherwise
	 */
	private static boolean removeBenchmarkFromDatabase(int benchId, Connection con) {
		log.debug("got request permanently remove this benchmark from the database " + benchId);
		CallableStatement procedure = null;
		try {
			procedure = con.prepareCall("CALL RemoveBenchmarkFromDatabase(?)");
			procedure.setInt(1, benchId);
			procedure.executeUpdate();
			return true;
		} catch (Exception e) {
			log.error("removeBenchmarkFromDatabaseremoveBenchmarkFromDatabase", e);
		} finally {
			Common.safeClose(procedure);
		}
		return false;
	}

	/**
	 * Removes all benchmark database entries where the benchmark has been deleted AND has been orphaned
	 *
	 * @return True on success, false on error
	 */
	public static boolean cleanOrphanedDeletedBenchmarks() {
		Connection con = null;
		CallableStatement procedure = null;
		ResultSet results = null;

		//will contain the id of every benchmark that is associated with either a space or a pair
		HashSet<Integer> parentedBenchmarks = new HashSet<>();
		try {
			con = Common.getConnection();

			procedure = con.prepareCall("{CALL GetBenchmarksAssociatedWithSpaces()}");
			results = procedure.executeQuery();
			while (results.next()) {
				parentedBenchmarks.add(results.getInt("id"));
			}
			Common.safeClose(procedure);
			Common.safeClose(results);

			procedure = con.prepareCall("{CALL GetBenchmarksAssociatedWithPairs()}");
			results = procedure.executeQuery();
			while (results.next()) {
				parentedBenchmarks.add(results.getInt("id"));
			}
			Common.safeClose(procedure);
			Common.safeClose(results);

			procedure = con.prepareCall("CALL GetDeletedBenchmarks()");
			results = procedure.executeQuery();
			while (results.next()) {
				Benchmark b = resultToBenchmark(results);
				if (new File(b.getPath()).exists()) {
					log.warn("a deleted benchmark still exists on disk! id = " + b.getId());
					if (!Util.safeDeleteFileAndEmptyParents(b.getPath(), R.getBenchmarkPath())) {
						log.warn("the benchmark could not be deleted! Not removing benchmark from the database");
						continue;
					}
				}
				// the benchmark has been deleted AND it is not associated with any spaces or job pairs
				if (!parentedBenchmarks.contains(b.getId())) {
					removeBenchmarkFromDatabase(b.getId(), con);
				}
			}
			return true;
		} catch (Exception e) {
			log.error("cleanOrphanedDeletedBenchmarks", e);
		} finally {
			Common.safeClose(con);
			Common.safeClose(procedure);
			Common.safeClose(results);
		}
		return false;
	}

	/**
	 * Copies a list of benchmarks into a new space, making the given user the new owner.
	 *
	 * @param benchmarks The benchmarks to copy
	 * @param userId The Id of the new owner
	 * @param spaceId The ID of the space to associate the new benchmarks with
	 * @return A list of IDs of all the new benchmarks. Ids will be returned in the same order as their copies in the
	 * input list of benchmarks.
	 */
	public static List<Integer> copyBenchmarks(List<Benchmark> benchmarks, int userId, int spaceId) {
		List<Integer> ids = new ArrayList<>();
		for (Benchmark b : benchmarks) {
			ids.add(copyBenchmark(b, userId, spaceId));
		}
		return ids;
	}

	/**
	 * Makes a deep copy of an existing benchmark, gives it a new user, and places it into a space
	 *
	 * @param b The existing benchmark to copy
	 * @param userId The userID that the new benchmark will be given
	 * @param spaceId The space ID of the space to place the new benchmark in to
	 * @return The ID of the new benchmark, or -1 on failure
	 * @author Eric Burns
	 */
	public static int copyBenchmark(Benchmark b, int userId, int spaceId) {
		try {
			log.debug("Copying benchmark " + b.getName() + " to new user id= " + String.valueOf(userId));
			Benchmark newBenchmark = new Benchmark();
			newBenchmark.setAttributes(b.getAttributes());
			newBenchmark.setType(b.getType());

			newBenchmark.setDescription(b.getDescription());
			newBenchmark.setName(b.getName());
			newBenchmark.setUserId(userId);
			newBenchmark.setUploadDate(b.getUploadDate());
			newBenchmark.setDiskSize(b.getDiskSize());
			newBenchmark.setDownloadable(b.isDownloadable());

			if (newBenchmark.getAttributes() == null) {
				newBenchmark.setAttributes(new HashMap<>());
			}

			//this benchmark must be valid, since it is just a copy of
			//an old benchmark that already passed validation
			newBenchmark.getAttributes().put(R.VALID_BENCHMARK_ATTRIBUTE, "true");
			File benchmarkFile = new File(b.getPath());

			File uniqueDir = UploadBenchmark.getDirectoryForBenchmarkUpload(userId, String.valueOf(b.getId()));
			uniqueDir.mkdirs();
			newBenchmark.setPath(uniqueDir.getAbsolutePath() + File.separator + benchmarkFile.getName());

			FileUtils.copyFileToDirectory(benchmarkFile, uniqueDir);
			int benchId = Benchmarks.addAndAssociate(newBenchmark, spaceId, null);
			if (benchId < 0) {
				log.error("Benchmark being copied could not be successfully added to the database");
				return benchId;
			}
			log.debug("Benchmark added successfully to the database, now adding dependency associations");
			List<BenchmarkDependency> deps = Benchmarks.getBenchDependencies(b.getId());

			for (BenchmarkDependency dep : deps) {
				Benchmarks.addBenchDependency(benchId, dep.getSecondaryBench().getId(), dep.getDependencyPath());
			}

			log.debug("Benchmark copied successfully, return new benchmark ID = " + benchId);
			return benchId;
		} catch (Exception e) {
			log.error("copyBenchmark", e);
			return -1;
		}
	}

	/**
	 * Sets a benchmark to 'deleted' in the database and actually removes in on disk Deletes a benchmark from the
	 * database (cascading deletes handle all dependencies)
	 *
	 * @param id the id of the benchmark to delete
	 * @return True if the operation was a success, false otherwise
	 * @author Todd Elvers
	 */
	public static boolean delete(int id) {
		Connection con = null;
		CallableStatement procedure = null;

		try {
			con = Common.getConnection();

			procedure = con.prepareCall("{CALL SetBenchmarkToDeletedById(?, ?)}");
			procedure.setInt(1, id);
			procedure.registerOutParameter(2, java.sql.Types.LONGNVARCHAR);
			procedure.executeUpdate();

			return Util.safeDeleteFileAndEmptyParents(procedure.getString(2), R.getBenchmarkPath());
		} catch (Exception e) {
			log.error("delete", e);
		} finally {
			Common.safeClose(con);
			Common.safeClose(procedure);
		}
		log.debug(String.format("Deletion of benchmark [id=%d] failed.", id));
		return false;
	}

	private static Benchmark constructBenchmark(File f, int typeId, boolean downloadable, int userId) {
		Benchmark b = new Benchmark();
		Processor t = new Processor();
		t.setId(typeId);
		b.setPath(f.getAbsolutePath());
		b.setName(f.getName());
		b.setType(t);
		b.setUserId(userId);
		b.setDownloadable(downloadable);
		return b;
	}

	/**
	 * Creates a space named after the directory and finds any benchmarks within the directory. Then the process
	 * recursively adds any subspaces found (other directories) until all directories under the original one are
	 * traversed. Also extracts the description file(if there is one) and sets it as the description for the space.
	 * *New* deletes .git directory (if present) and does not add .gitignore .gitattributes .gitmodules and
	 * README.md files as benchmarks.
	 *
	 * @param directory The directory to extract data from
	 * @param typeId The bench type id to set for all the found benchmarks
	 * @param userId The user is of the owner of all the benchmarks found
	 * @param downloadable Whether or now to mark any found benchmarks as downloadable
	 * @param perm The default permissions to set for this space
	 * @param statusId statusId The ID of an upload status if one exists for this operation, null otherwise
	 * @return A single space containing all subspaces and benchmarks based on the file structure of the given
	 * directory.
	 * @throws Exception Any exception with the description file, with an error message contained
	 * @author Wyatt Kaiser, Steve Fiolic
	 */
	public static Space extractSpacesAndBenchmarks(
			File directory, int typeId, int userId, boolean downloadable, Permission perm, Integer statusId
	) throws IOException, StarExecException {
		// Create a space for the current directory and set it's name
            log.info("Extracting Spaces and Benchmarks for " + userId + " in " + directory.getName());
		Space space = new Space();
		space.setName(directory.getName());
		space.setPermission(perm);
		String spaceDescription = "";

		// Search for description file within the directory...
		File descriptionFile = new File(directory, R.BENCHMARK_DESC_PATH);
		if (descriptionFile.exists()) {
			spaceDescription = FileUtils.readFileToString(descriptionFile);
		}

		space.setDescription(spaceDescription);
		int benchCounter = 0;
		int spaceCounter = 0;
		Timer spaceTimer = new Timer();
		Timer benchTimer = new Timer();
		for (File f : directory.listFiles()) {

			// If it's a sub-directory
			if (f.isDirectory()) {
        if (f.getName().equals(".git")){
          FileUtils.deleteDirectory(f);
        }
        else{
  				// Recursively extract spaces/benchmarks from that directory
  				space.getSubspaces()
  				     .add(Benchmarks.extractSpacesAndBenchmarks(f, typeId, userId, downloadable, perm, statusId));
  				spaceCounter++;
  				if (spaceTimer.getTime() > R.UPLOAD_STATUS_TIME_BETWEEN_UPDATES) {
  					Uploads.incrementTotalSpaces(statusId, spaceCounter);//for upload status page
  					spaceCounter = 0;
  					spaceTimer.reset();
  				}
        }
			}

      else if ((!f.getName().equals(R.BENCHMARK_DESC_PATH)) && (!f.getName().equals("README.md")) &&
              (!f.getName().equals(".gitattributes")) && (!f.getName().equals(".gitignore")) &&
							(!f.getName().equals(".gitmodules")) && (!f.getName().equals(".git")))

       { //Not a description file, readme, .gitattributes, .gitmodules, and .gitignore

				if (Validator.isValidBenchName(f.getName())) {
					space.addBenchmark(constructBenchmark(f, typeId, downloadable, userId));
					benchCounter++;
					if (benchTimer.getTime() > R.UPLOAD_STATUS_TIME_BETWEEN_UPDATES) {
						Uploads.incrementTotalBenchmarks(statusId, benchCounter);//for upload status page
						benchCounter = 0;
						benchTimer.reset();
					}
				}
        else {
					String msg = "\"" + f.getName() + "\" is not accepted as a legal benchmark name.";
					Uploads.setBenchmarkErrorMessage(statusId, msg);
					throw new StarExecException(msg);
				}
			}
		}
		Uploads.incrementTotalBenchmarks(statusId, benchCounter);//for upload status page
		Uploads.incrementTotalSpaces(statusId, spaceCounter);//for upload status page

		return space;
	}

	/**
	 * Returns the bench id of the axiom file that is being looked for, -1 if not found or too many by that name
	 *
	 * @param spaceId the dependent bench root space
	 * @param includePath the path that will be used to drill down and find bench
	 * @param linked true if the depRootSpace is the same as the first directory in the include statement
	 * @param userId the ID of the user that owns the benchmarks with these dependencies
	 * @return benchId
	 * @author Benton McCune
	 */
	private static Integer findDependentBench(Integer spaceId, String includePath, Boolean linked, Integer userId) {
		String[] spaces = includePath.split("/");//splitting up path
		log.debug("Length of spaces string array = " + spaces.length);
		if (spaces.length == 0) {
			return -1;
		}
		int index = (linked) ? 1 : 0;
		log.debug("First Space(or Bench) to look for = " + spaces[index]);
		//List<Space> subSpaces;
		Integer currentSpaceId = spaceId;
		log.debug("Current Space Id = " + currentSpaceId);
		//dig through subspaces while you have to
		while ((index < (spaces.length - 1)) && (currentSpaceId > -1)) {
			log.info("Looking for SubSpace " + spaces[index] + " in Space " + currentSpaceId);
			currentSpaceId = Spaces.getSubSpaceIDbyName(currentSpaceId, userId, spaces[index]);
			log.info("Returned with subspace " + currentSpaceId);
			index++;
		}
		//now find bench in the subspace you've found
		if (currentSpaceId > 1) {
			log.info("Looking for Benchmark " + spaces[index] + " in Space " + currentSpaceId);
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
	 * @throws Exception
	 * @author Tyler Jensen
	 */
	protected static Benchmark get(Connection con, int benchId, boolean includeDeleted) {
		CallableStatement procedure = null;
		ResultSet results = null;

		try {
			if (includeDeleted) {
				procedure = con.prepareCall("{CALL GetBenchmarkByIdIncludeDeletedAndRecycled(?)}");
			} else {
				procedure = con.prepareCall("{CALL GetBenchmarkById(?)}");
			}
			procedure.setInt(1, benchId);
			results = procedure.executeQuery();

			if (results.next()) {
				Benchmark b = resultToBenchmarkWithPrefix(results, "bench");

				Processor t = Processors.resultSetToProcessor(results, "types");

				b.setType(t);
				Common.safeClose(results);
				return b;
			}
		} catch (Exception e) {
			log.error("get", e);
		} finally {
			Common.safeClose(procedure);
			Common.safeClose(results);
		}
		return null;
	}

    /**
     * @param con The connection to query with
     * @param benchId The id of the benchmark to retrieve
     * @return A Benchmark containing just the path to the benchmark
     * @author Aaron Stump
     */
	protected static Benchmark getSkeletal(Connection con, int benchId) {
		CallableStatement procedure = null;
		ResultSet results = null;

		try {
		    procedure = con.prepareCall("{CALL GetBenchmarkPathById(?)}");
		    procedure.setInt(1, benchId);
		    results = procedure.executeQuery();

		    if (results.next()) {
			Benchmark b = new Benchmark();
			b.setId(benchId);
			b.setName(results.getString("name"));
			b.setPath(results.getString("path"));
			Common.safeClose(results);
			return b;
		    }
		} catch (Exception e) {
			log.error("getSkeletal", e);
		} finally {
			Common.safeClose(procedure);
			Common.safeClose(results);
		}
		return null;
	}

	/**
	 * Retrieves a benchmark without attributes. Will not return a "deleted" benchmark
	 *
	 * @param benchId The id of the benchmark to retrieve
	 * @return A benchmark object representing the benchmark with the given ID
	 * @author Tyler Jensen
	 */
	public static Benchmark get(int benchId) {
		return Benchmarks.get(benchId, false, false);
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
			return get(con, benchId, includeAttrs, includeDeleted);
		} catch (Exception e) {
			log.error("get", e);
		} finally {
			Common.safeClose(con);
		}
		return null;
	}

	protected static Benchmark get(Connection con, int benchId, boolean includeAttrs, boolean includeDeleted) {

		try {
			Benchmark b = Benchmarks.get(con, benchId, includeDeleted);
			if (b == null) {
				return null;
			}
			if (includeAttrs) {
				b.setAttributes(Benchmarks.getAttributes(con, benchId));
			}
			return b;
		} catch (Exception e) {
			log.error("get", e);
		}
		return null;
	}

	/**
	 * Gets a list of benchmarks given a list of benchmark IDs
	 *
	 * @param benchIds The IDs of the benchmarks to retrieve.
	 * @return The benchmarks on success or null on failure. Attributes are not returned
	 */
	public static List<Benchmark> get(List<Integer> benchIds) {
		return get(benchIds, false);
	}

	/**
	 * @param benchIds A list of ids to get benchmarks for
	 * @param includeAttrs True to include attributes for all the given benchmarks, false if attributes should not be
	 * included
	 * @return A list of benchmark object representing the benchmarks with the given IDs
	 * @author Tyler Jensen
	 */
	public static List<Benchmark> get(List<Integer> benchIds, boolean includeAttrs) {
		Connection con = null;

		try {
			con = Common.getConnection();
			List<Benchmark> benchList = new ArrayList<>();

			for (int id : benchIds) {
				benchList.add(Benchmarks.get(con, id, false));
				if (includeAttrs) {
					benchList.get(benchList.size() - 1).setAttributes(Benchmarks.getAttributes(con, id));
				}
			}
			return benchList;
		} catch (Exception e) {
			log.error("get", e);
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
		ResultSet results = null;
		CallableStatement procedure = null;
		try {
			con = Common.getConnection();
			procedure = con.prepareCall("{CALL GetBenchmarksByOwner(?)}");
			procedure.setInt(1, userId);
			results = procedure.executeQuery();
			return resultsToBenchmarkWithType(results);
		} catch (Exception e) {
			log.error("getByOwner", e);
		} finally {
			Common.safeClose(con);
			Common.safeClose(procedure);
			Common.safeClose(results);
		}

		log.debug(String.format("Getting the benchmarks owned by user %d failed.", userId));
		return null;
	}

	/**
	 * Gets all benchmarks in a given job.
	 *
	 * @param jobId The job to get the benchmarks from.
	 * @return The list of benchmarks in the job.
	 * @throws SQLException if something goes wrong with the database.
	 */
	public static List<Benchmark> getByJob(int jobId) throws SQLException {
		log.debug("Inside benchmarks.getByJob");
		return Common.query("{CALL GetBenchmarksByJob(?)}", procedure -> {
			log.debug("Setting GetBenchmarksByJob parameter.");
			procedure.setInt(1, jobId);
		}, results -> {
			List<Benchmark> benchmarks = new ArrayList<>();
			int test = 0;
			log.debug("Compiling result for GetBenchmarksByJob");
			while (results.next()) {
				log.debug("GetBenchmarksByJob results.next() called");
				test += 1;
				benchmarks.add(resultToBenchmark(results));
			}
			return benchmarks;
		});
	}

	/**
	 * Gets the IDs of every space that is associated with the given benchmark
	 *
	 * @param benchId The benchmark in question
	 * @return A list of space IDs that are associated with this benchmark
	 * @author Eric Burns
	 */
	public static List<Integer> getAssociatedSpaceIds(int benchId) {
		Connection con = null;
		CallableStatement procedure = null;
		ResultSet results = null;
		try {
			con = Common.getConnection();
			procedure = con.prepareCall("{CALL GetAssociatedSpaceIdsByBenchmark(?)}");
			procedure.setInt(1, benchId);
			results = procedure.executeQuery();
			List<Integer> ids = new ArrayList<>();
			while (results.next()) {
				ids.add(results.getInt("space_id"));
			}
			return ids;
		} catch (Exception e) {
			log.error("getAssociatedSpaceIds", e);
		} finally {
			Common.safeClose(con);
			Common.safeClose(results);
			Common.safeClose(procedure);
		}
		return null;
	}

	/**
	 * Retrieves all attributes (key/value) of the given benchmark
	 *
	 * @param con The connection to make the query on
	 * @param benchId The id of the benchmark to get the attributes of
	 * @return The properties object which holds all the benchmark's attributes. Null on error.
	 * @throws Exception
	 * @author Tyler Jensen
	 */
	protected static Map<String, String> getAttributes(Connection con, int benchId) {
		CallableStatement procedure = null;
		ResultSet results = null;

		try {
			procedure = con.prepareCall("{CALL GetBenchAttrs(?)}");
			procedure.setInt(1, benchId);
			results = procedure.executeQuery();

			Map<String, String> prop = new HashMap<>();
			while (results.next()) {
				prop.put(results.getString("attr_key"), results.getString("attr_value"));
			}
			return prop;
		} catch (Exception e) {
			log.error("getAttributes", e);
		} finally {
			Common.safeClose(procedure);
			Common.safeClose(results);
		}
		return null;
	}

	/**
	 * Retrieves all attributes (key/value) of the given benchmark
	 *
	 * @param benchId The id of the benchmark to get the attributes of
	 * @return The properties object which holds all the benchmark's attributes
	 * @author Tyler Jensen
	 */
	public static Map<String, String> getAttributes(int benchId) {
		Connection con = null;
		try {
			con = Common.getConnection();
			return Benchmarks.getAttributes(con, benchId);
		} catch (Exception e) {
			log.error("getAttributes", e);
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
		CallableStatement procedure = null;
		ResultSet results = null;

		try {
			con = Common.getConnection();
			procedure = con.prepareCall("{CALL getPathsForBenchmarkDependencies(?)}");
			procedure.setInt(1, benchmarkId);
			results = procedure.executeQuery();
			List<BenchmarkDependency> dependencies = new LinkedList<>();

			Benchmark primary = Benchmarks.getSkeletal(con,benchmarkId);

			while (results.next()) {
				// Build benchmark dependency object
				BenchmarkDependency benchD = new BenchmarkDependency();
				benchD.setPrimaryBench(primary);
				Benchmark secondary = new Benchmark();
				secondary.setId(results.getInt("id"));
				secondary.setName(results.getString("name"));
				secondary.setPath(results.getString("path"));
				benchD.setSecondaryBench(secondary);
				benchD.setDependencyPath(results.getString("include_path"));

				// Add benchmark dependency object to list of dependencies
				dependencies.add(benchD);
			}

			log.debug(
					String.format("%d dependencies were returned as being needed by benchmark %d.", dependencies
							              .size(), benchmarkId
					));

			return dependencies;
		} catch (Exception e) {
			log.error("getBenchDependencies", e);
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
	 *
	 * @param spaceId space that bench should be in
	 * @param benchName name of bench
	 * @return benchId
	 * @author Benton McCune
	 */
	public static Integer getBenchIdByName(Integer spaceId, String benchName) {
		Connection con = null;
		CallableStatement procedure = null;
		ResultSet results = null;
		log.debug("getBenchIdByName", "Looking for Benchmark " + benchName + " in Space " + spaceId);
		try {
			con = Common.getConnection();
			procedure = con.prepareCall("{CALL GetBenchByName(?,?)}");
			procedure.setInt(1, spaceId);
			procedure.setString(2, benchName);

			results = procedure.executeQuery();
			Integer benchId = -1;

			if (results.next()) {
				benchId = (results.getInt("bench.id"));
				log.debug("Bench Id = " + benchId);
			}
			results.last();
			Integer numResults = results.getRow();
			log.debug("# of Benchmarks with this name = " + numResults);
			if (numResults != 1) {
				return -1;
			}
			return benchId;
		} catch (Exception e) {
			log.error("getBenchIdByName", e);
		} finally {
			Common.safeClose(con);
			Common.safeClose(procedure);
			Common.safeClose(results);
		}
		return -1;
	}

	/**
	 * Get the total count of the benchmarks belong to a specific user
	 *
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
				return results.getInt("benchCount");
			}
		} catch (Exception e) {
			log.error("getBenchmarkCountByUser", e);
		} finally {
			Common.safeClose(con);
			Common.safeClose(procedure);
			Common.safeClose(results);
		}
		return 0;
	}

	/**
	 * Get the total count of the benchmarks belonging to a specific user that match the given query
	 *
	 * @param userId Id of the user we are looking for
	 * @param query The query to match the benchmarks on
	 * @return The count of the benchmarks
	 * @author Eric Burns
	 */
	public static int getBenchmarkCountByUser(int userId, String query) {
		Connection con = null;
		CallableStatement procedure = null;
		ResultSet results = null;
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
			log.error("getBenchmarkCountByUser", e);
		} finally {
			Common.safeClose(con);
			Common.safeClose(procedure);
			Common.safeClose(results);
		}
		return 0;
	}

	/**
	 * Get next page of the benchmarks belong to a specific user
	 *
	 * @param query a DataTablesQuery object
	 * @param userId Id of the user we are looking for
	 * @param recycled Whether to get recycled or non-recycled benchmarks
	 * @return a list of benchmarks belong to the user
	 * @author Wyatt Kaiser
	 */
	public static List<Benchmark> getBenchmarksByUserForNextPage(DataTablesQuery query, int userId, boolean recycled) {
		Connection con = null;
		NamedParameterStatement procedure = null;
		ResultSet results = null;
		try {
			con = Common.getConnection();
			PaginationQueryBuilder builder = new PaginationQueryBuilder(PaginationQueries.GET_BENCHMARKS_BY_USER_QUERY,
			                                                            getBenchmarkOrderColumn(query.getSortColumn()),
			                                                            query
			);

			procedure = new NamedParameterStatement(con, builder.getSQL());
			procedure.setInt("userId", userId);
			procedure.setString("query", query.getSearchQuery());
			procedure.setBoolean("recycled", recycled);

			results = procedure.executeQuery();
			List<Benchmark> benchmarks = new LinkedList<>();

			while (results.next()) {
				//don't include deleted benchmarks in the results if getDeleted is false
				Benchmark b = new Benchmark();
				b.setId(results.getInt("id"));
				b.setName(results.getString("name"));
				b.setUserId(results.getInt("user_id"));
				if (results.getBoolean("deleted")) {
					b.setName(b.getName() + " (deleted)");
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
		} catch (Exception e) {
			log.error("getBenchmarksByUserForNextPage", e);
		} finally {
			Common.safeClose(con);
			Common.safeClose(procedure);
			Common.safeClose(results);
		}
		return null;
	}

	private static String getBenchmarkOrderColumn(int indexOfColumn) {
		switch (indexOfColumn) {
		case 0:
			return "benchmarks.name";
		case 1:
			return "processors.name";
		case 2:
			return "bench_assoc.order_id";
		}
		return "benchmarks.name";
	}

	/**
	 * Retrieves benchmarks for the next page of a table on the space explorer
	 *
	 * @param query A DataTablesQuery object
	 * @param spaceId The ID of the space the benchmarks to retrieve are in
	 * @return A list of benchmarks on success or null on failure
	 */
	public static List<Benchmark> getBenchmarksForNextPage(DataTablesQuery query, int spaceId) {
		Connection con = null;
		NamedParameterStatement procedure = null;
		ResultSet results = null;
		try {
			con = Common.getConnection();

			PaginationQueryBuilder builder = new PaginationQueryBuilder(PaginationQueries
					                                                            .GET_BENCHMARKS_IN_SPACE_QUERY,
			                                                            getBenchmarkOrderColumn(query.getSortColumn()),
			                                                            query
			);

			procedure = new NamedParameterStatement(con, builder.getSQL());
			procedure.setInt("spaceId", spaceId);
			procedure.setString("query", query.getSearchQuery());

			results = procedure.executeQuery();
			List<Benchmark> benchmarks = new LinkedList<>();

			while (results.next()) {
				Benchmark b = new Benchmark();
				b.setId(results.getInt("id"));
				b.setName(results.getString("name"));
				b.setUserId(results.getInt("user_id"));
				if (results.getBoolean("deleted")) {
					b.setName(b.getName() + " (deleted)");
				} else if (results.getBoolean("recycled")) {
					b.setName(b.getName() + " (in trash)");
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
		} catch (Exception e) {
			log.error("getBenchmarksForNextPage", e);
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
		CallableStatement procedure = null;
		ResultSet results = null;

		try {
			con = Common.getConnection();
			procedure = con.prepareCall("{CALL GetSpaceBenchmarksById(?)}");
			procedure.setInt(1, spaceId);
			results = procedure.executeQuery();
			List<Benchmark> benchmarks = new LinkedList<>();

			while (results.next()) {
				Benchmark b = resultToBenchmarkWithPrefix(results, "bench");
				Processor t = Processors.resultSetToProcessor(results, "types");

				b.setType(t);
				benchmarks.add(b);
			}
			return benchmarks;
		} catch (Exception e) {
			log.error("getBySpace", e);
		} finally {
			Common.safeClose(con);
			Common.safeClose(procedure);
			Common.safeClose(results);
		}
		return null;
	}

	/**
	 * Retrieves the contents of a benchmark file from disk as a string
	 *
	 * @param b The benchmark to get the contents of (must have a valid path)
	 * @param limit the maximum number of lines to return, or no limit if less than 0
	 * @return The file contents as a string
	 */
	public static Optional<String> getContents(Benchmark b, int limit) throws IOException {
		File file = new File(b.getPath());
		return Util.readFileLimited(file, limit);
	}

	/**
	 * Retrieves the contents of a benchmark file from disk as a string
	 *
	 * @param benchId The id of the benchmark to get the contents of
	 * @param limit the maximum number of lines to return
	 * @return The file contents as a string
	 */
	public static Optional<String> getContents(int benchId, int limit) throws IOException {
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
		return getCountInSpace(spaceId, "");
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
		CallableStatement procedure = null;
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
			log.error("getCountInSpace", e);
		} finally {
			Common.safeClose(con);
			Common.safeClose(procedure);
			Common.safeClose(results);
		}
		return 0;
	}

	/**
	 * Retrieves a benchmark. If the benchmark is deleted, it will still be returned
	 *
	 * @param benchId The id of the benchmark to retrieve
	 * @param includeAttrs True if attributes for the benchmark should also be included and false otherwise
	 * @return A benchmark object representing the benchmark with the given ID
	 * @author Tyler Jensen
	 */
	public static Benchmark getIncludeDeletedAndRecycled(int benchId, boolean includeAttrs) {
		return Benchmarks.get(benchId, includeAttrs, true);
	}

	public static Benchmark getIncludeDeletedAndRecycled(Connection con, int benchId, boolean includeAttrs) {
		return Benchmarks.get(con, benchId, includeAttrs, true);
	}

	/**
	 * Gets the number of recycled benchmarks a user owns
	 *
	 * @param userId The ID of the user to count benchmarks before
	 * @return The integer count of recycled benchmarks
	 */
	public static int getRecycledBenchmarkCountByUser(int userId) {
		return getRecycledBenchmarkCountByUser(userId, "");
	}

	/**
	 * Gets the number of recycled benchmarks a user has that match the given query
	 *
	 * @param userId The ID of the user in question
	 * @param query The string query to match on
	 * @return The number of benchmarks, or -1 on failure
	 * @author Eric Burns
	 */
	public static int getRecycledBenchmarkCountByUser(int userId, String query) {
		Connection con = null;
		ResultSet results = null;
		CallableStatement procedure = null;
		try {
			con = Common.getConnection();
			procedure = con.prepareCall("CALL GetRecycledBenchmarkCountByUser(?,?)");
			procedure.setInt(1, userId);
			procedure.setString(2, query);
			results = procedure.executeQuery();
			if (results.next()) {
				return results.getInt("benchCount");
			}
		} catch (Exception e) {
			log.error("getRecycledBenchmarkCountByUser", e);
		} finally {
			Common.safeClose(con);
			Common.safeClose(results);
			Common.safeClose(procedure);
		}
		return -1;
	}

	/**
	 * Retrieves all attributes (key/value) of the given benchmark in order
	 *
	 * @param con The connection to make the query on
	 * @param benchId The id of the benchmark to get the attributes of
	 * @return The properties object which holds all the benchmark's attributes
	 * @throws Exception
	 * @author Wyatt Kaiser
	 */
	protected static TreeMap<String, String> getSortedAttributes(Connection con, int benchId) {
		CallableStatement procedure = null;
		ResultSet results = null;

		try {
			procedure = con.prepareCall("{CALL GetBenchAttrs(?)}");
			procedure.setInt(1, benchId);
			results = procedure.executeQuery();

			TreeMap<String, String> sortedMap = new TreeMap<>();
			while (results.next()) {
				sortedMap.put(results.getString("attr_key"), results.getString("attr_value"));
			}
			return sortedMap;
		} catch (Exception e) {
			log.error("getSortedAttributes", e);
		} finally {
			Common.safeClose(procedure);
			Common.safeClose(results);
		}
		return null;
	}

	/**
	 * Retrieves all attributes (key/value of the given benchmark in alphabetic order
	 *
	 * @param benchId the id of the benchmark to get the attributes of
	 * @return The properties object which holds all the benchmark's attributes
	 * @author Wyatt Kaiser
	 */
	public static TreeMap<String, String> getSortedAttributes(int benchId) {
		Connection con = null;
		try {
			con = Common.getConnection();
			return Benchmarks.getSortedAttributes(con, benchId);
		} catch (Exception e) {
			log.error("getSortedAttributes", e);
		} finally {
			Common.safeClose(con);
		}
		return null;
	}

	/**
	 * Returns whether a benchmark with the given ID is present in the database with the "deleted" column set to true
	 *
	 * @param benchId The ID of the benchmark to check
	 * @param con the open connection to make the SQL call on
	 * @return True if the benchmark exists in the database with the "deleted" column set to true, and false otherwise
	 * @author Eric Burns
	 */
	protected static boolean isBenchmarkDeleted(Connection con, int benchId) {
		CallableStatement procedure = null;
		ResultSet results = null;
		try {
			procedure = con.prepareCall("{CALL IsBenchmarkDeleted(?)}");
			procedure.setInt(1, benchId);
			results = procedure.executeQuery();
			boolean deleted = false;
			if (results.next()) {
				deleted = results.getBoolean("benchDeleted");
			}
			return deleted;
		} catch (Exception e) {
			log.error("isBenchmarkDeleted", e);
		} finally {
			Common.safeClose(procedure);
			Common.safeClose(results);
		}
		return false;
	}

	/**
	 * Returns whether a benchmark with the given ID is present in the database with the "deleted" column set to true
	 *
	 * @param benchId The ID of the benchmark to check
	 * @return True if the benchmark exists in the database with the "deleted" column set to true, and false otherwise
	 * @author Eric Burns
	 */
	public static boolean isBenchmarkDeleted(int benchId) {
		Connection con = null;
		try {
			con = Common.getConnection();
			return isBenchmarkDeleted(con, benchId);
		} catch (Exception e) {
			log.error("isBenchmarkDeleted", e);
		} finally {
			Common.safeClose(con);
		}
		return false;
	}

	/**
	 * Returns whether a benchmark with the given ID is present in the database with the "recycled" column set to true
	 *
	 * @param benchId The ID of the benchmark to check
	 * @param con the open connection to make the SQL call on
	 * @return True if the benchmark exists in the database with the "recycled" column set to true, and false otherwise
	 * @author Eric Burns
	 */
	protected static boolean isBenchmarkRecycled(Connection con, int benchId) {
		CallableStatement procedure = null;
		ResultSet results = null;
		try {
			procedure = con.prepareCall("{CALL IsBenchmarkRecycled(?)}");
			procedure.setInt(1, benchId);
			results = procedure.executeQuery();
			boolean deleted = false;
			if (results.next()) {
				deleted = results.getBoolean("recycled");
			}
			return deleted;
		} catch (Exception e) {
			log.error("isBenchmarkRecycled", e);
		} finally {
			Common.safeClose(procedure);
			Common.safeClose(results);
		}
		return false;
	}

	/**
	 * Returns whether a benchmark with the given ID is present in the database with the "recycled" column set to true
	 *
	 * @param benchId The ID of the benchmark to check
	 * @return True if the benchmark exists in the database with the "recycled" column set to true, and false otherwise
	 * @author Eric Burns
	 */
	public static boolean isBenchmarkRecycled(int benchId) {
		Connection con = null;
		try {
			con = Common.getConnection();
			return isBenchmarkRecycled(con, benchId);
		} catch (Exception e) {
			log.error("isBenchmarkRecycled", e);
		} finally {
			Common.safeClose(con);
		}
		return false;
	}

	/**
	 * Makes sure that a benchmark with the gived id exists.
	 *
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
	 *
	 * @param attrs The attributes of a benchmark
	 * @return True if the attributes are of a valid benchmark, false otherwise
	 */
	private static boolean isBenchValid(Map<String, String> attrs) {
		// A benchmark is valid if it has attributes and it has the special R.VALID_BENCHMARK_ATTRIBUTE attribute
		return (attrs != null && Boolean.parseBoolean(attrs.getOrDefault(R.VALID_BENCHMARK_ATTRIBUTE, "false")));
	}

	/**
	 * Determines whether the benchmark with the given ID is public. It is public if it is in at least one public space
	 * or if it is the default benchmark for some community
	 *
	 * @param benchId The ID of the benchmark in question
	 * @return True if the benchmark exists and is in a public space, false otherwise.
	 */
	public static boolean isPublic(int benchId) {
		Connection con = null;
		try {
			con = Common.getConnection();
			return Benchmarks.isPublic(con, benchId);
		} catch (Exception e) {
			log.error("isPublic", e);
		} finally {
			Common.safeClose(con);
		}
		return false;
	}

	protected static boolean isPublic(Connection con, int benchId) {
		CallableStatement procedure = null;
		ResultSet results = null;
		try {
			procedure = con.prepareCall("{CALL IsBenchPublic(?)}");
			procedure.setInt(1, benchId);
			results = procedure.executeQuery();
			boolean publicSpace = false;
			if (results.next()) {
				publicSpace = (results.getInt("benchPublic") > 0);
			}

			if (publicSpace) {
				return true;
			}

			Common.safeClose(results);
			Common.safeClose(procedure);
			//if the benchmark is in no public spaces, check to see if it is the default benchmark for some community
			procedure = con.prepareCall("CALL IsBenchACommunityDefault(?)");
			procedure.setInt(1, benchId);
			results = procedure.executeQuery();
			if (results.next()) {
				return (results.getInt("benchDefault") > 0);
			}
		} catch (Exception e) {
			log.error("isPublic", e);
		} finally {
			Common.safeClose(procedure);
			Common.safeClose(results);
		}
		return false;
	}

	/**
	 * Sets the "recycled" flag in the database to true. Indicates the user has moved the benchmark to the recycle bin,
	 * from which in can be deleted
	 *
	 * @param id the id of the benchmark to recycled
	 * @return True if the operation was a success, false otherwise
	 * @author Eric Burns
	 */
	public static boolean recycle(int id) {
		return setRecycledState(id, true);
	}

	/**
	 * Sets the "recycled" flag in the database to false. Indicates the user has removed the benchmark from the recycle
	 * bin
	 *
	 * @param id the id of the benchmark to be removed from the recycle bin
	 * @return True if the operation was a success, false otherwise
	 * @author Eric Burns
	 */
	public static boolean restore(int id) {
		return setRecycledState(id, false);
	}

	/**
	 * Restores all benchmarks a user has that have been recycled to normal
	 *
	 * @param userId The userId in question
	 * @return True on success, false otherwise
	 * @author Eric Burns
	 */
	public static boolean restoreRecycledBenchmarks(int userId) {
		Connection con = null;
		CallableStatement procedure = null;
		ResultSet results = null;
		try {
			con = Common.getConnection();

			Common.safeClose(procedure);
			procedure = con.prepareCall("CALL GetRecycledBenchmarkIds(?)");
			procedure.setInt(1, userId);
			results = procedure.executeQuery();

			while (results.next()) {
				Benchmarks.restore(results.getInt("id"));
			}
			return true;
		} catch (Exception e) {
			log.error("restoreRecycledBenchmarks", e);
		} finally {
			Common.safeClose(con);
			Common.safeClose(procedure);
			Common.safeClose(results);
		}
		return false;
	}

	public static Benchmark resultToBenchmark(ResultSet results) throws SQLException {
		return resultToBenchmarkWithPrefix(results, null);
	}

	/**
	 * Creates a Benchmark object from a SQL resultset
	 *
	 * @param results The resultset pointed at the row containing benchmark data
	 * @param prefix If the sql procedure used to create "results" used an "AS <name>" clause when getting benchmark
	 * data (as in SELECT * FROM benchmarks AS bench), then prefix should be <name>
	 * @return A Benchmark object
	 * @throws SQLException
	 */
	public static Benchmark resultToBenchmarkWithPrefix(ResultSet results, String prefix) throws SQLException {
		Benchmark b = new Benchmark();
		if (Util.isNullOrEmpty(prefix)) {
			b.setId(results.getInt("id"));
			b.setUserId(results.getInt("user_id"));
			b.setName(results.getString("name"));
			b.setUploadDate(results.getTimestamp("uploaded"));
			b.setPath(results.getString("path"));
			b.setDescription(results.getString("description"));
			b.setDownloadable(results.getBoolean("downloadable"));
			b.setDiskSize(results.getLong("disk_size"));
			b.setRecycled(results.getBoolean("recycled"));
			b.setDeleted(results.getBoolean("deleted"));
		} else {
			b.setId(results.getInt(prefix + ".id"));
			b.setUserId(results.getInt(prefix + ".user_id"));
			b.setName(results.getString(prefix + ".name"));
			b.setUploadDate(results.getTimestamp(prefix + ".uploaded"));
			b.setPath(results.getString(prefix + ".path"));
			b.setDescription(results.getString(prefix + ".description"));
			b.setDownloadable(results.getBoolean(prefix + ".downloadable"));
			b.setDiskSize(results.getLong(prefix + ".disk_size"));
			b.setRecycled(results.getBoolean(prefix + ".recycled"));
			b.setDeleted(results.getBoolean(prefix + ".deleted"));
		}
		return b;
	}

	/**
	 * Deletes all benchmarks that this user has in their recycle bin from both the database and from disk
	 *
	 * @param userId The userId in question
	 * @return True on success, false otherwise
	 * @author Eric Burns
	 */
	public static boolean setRecycledBenchmarksToDeleted(int userId) {
		Connection con = null;
		CallableStatement procedure = null;
		ResultSet results = null;
		try {
			con = Common.getConnection();
			procedure = con.prepareCall("CALL GetRecycledBenchmarkPaths(?)");
			procedure.setInt(1, userId);
			results = procedure.executeQuery();

			while (results.next()) {
				Util.safeDeleteDirectory(results.getString("path"));
			}
			Common.safeClose(procedure);
			procedure = con.prepareCall("CALL SetRecycledBenchmarksToDeleted(?)");
			procedure.setInt(1, userId);
			procedure.executeUpdate();

			return true;
		} catch (Exception e) {
			log.error("setRecycledBenchmarksToDeleted", e);
		} finally {
			Common.safeClose(con);
			Common.safeClose(procedure);
			Common.safeClose(results);
		}
		return false;
	}

	/**
	 * Sets the "recycled" flag in the database to the given value.
	 *
	 * @param id the id of the benchmark to recycled
	 * @param state True to set as recycled, false to remove recycled tag
	 * @return True if the operation was a success, false otherwise
	 * @author Eric Burns
	 */
	private static boolean setRecycledState(int id, boolean state) {
		Connection con = null;
		CallableStatement procedure = null;

		try {
			con = Common.getConnection();
			procedure = con.prepareCall("{CALL SetBenchmarkRecycledValue(?, ?)}");
			procedure.setInt(1, id);
			procedure.setBoolean(2, state);
			procedure.executeUpdate();
			return true;
		} catch (Exception e) {
			log.error("setRecycledState", e);
		} finally {
			Common.safeClose(con);
			Common.safeClose(procedure);
		}
		return false;
	}

	/**
	 * Updates the details of a benchmark. This ONLY updates the database: it does not handle actually executing the
	 * given processor!
	 *
	 * @param id the id of the benchmark to update
	 * @param name the new name to apply to the benchmark
	 * @param description the new description to apply to the benchmark
	 * @param isDownloadable boolean indicating whether or not this benchmark is downloadable
	 * @param benchTypeId the new benchmark type to apply to the benchmark
	 * @return True if the operation was a success, false otherwise
	 * @author Todd Elvers
	 */
	public static boolean updateDetails(
			int id, String name, String description, boolean isDownloadable, int benchTypeId
	) {
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
		} catch (Exception e) {
			log.error("updateDetails", e);
		} finally {
			Common.safeClose(con);
			Common.safeClose(procedure);
		}

		log.debug(String.format("Benchmark [id=%d] failed to be updated.", id));
		return false;
	}

	/**
	 * Validates the dependencies for a list of benchmarks (usually all benches of a single space)
	 *
	 * @param benchmarks The list of benchmarks that might have dependencies
	 * @param spaceId the id of the space where the axiom benchmarks lie
	 * @param linked true if the depRootSpace is the same as the first directory in the include statement
	 * @return the data structure that has information about dependencies
	 * @author Eric Burns
	 */
	private static boolean validateDependencies(List<Benchmark> benchmarks, Integer spaceId, Boolean linked, Integer statusID) {
		HashMap<String, BenchmarkDependency> foundDependencies = new HashMap<>();
		for (Benchmark benchmark1 : benchmarks) {
			Benchmark benchmark = benchmark1;
			String out = validateIndBenchDependencies(benchmark, spaceId, linked, foundDependencies);
			if (out != "true") {
				log.warn("Dependent benchs not found for Bench " + benchmark.getName());
				Uploads.addFailedBenchmark(statusID, benchmark.getName(), "Dependancy check failed for this benchmark. Failed search for " + out + ".");
				return false;
			}
		}
		return true;
	}

	/**
	 * Validates the dependencies for a benchmark. Adds the dependencies to the benchmark as well
	 *
	 * @param bench The benchmark that might have dependencies
	 * @param spaceId the id of the space where the axiom benchmarks lie
	 * @param linked true if the depRootSpace is the same as the first directory in the include statement
	 * @return "true" if the dependencies are valid, the name of the failed dependency if otherwise
	 * @author Benton McCune
	 */
	private static String  validateIndBenchDependencies(
			Benchmark bench, Integer spaceId, Boolean linked, HashMap<String, BenchmarkDependency> foundDependencies
	) {
		Map<String, String> atts = bench.getAttributes();
		String includePath = "";
		try {
			Integer numberDependencies = Integer.valueOf(atts.getOrDefault("starexec-dependencies", "0"));
			log.info("validateIndBenchDependencies", "# of dependencies = " + numberDependencies);
			for (int i = 1; i <= numberDependencies; i++) {
				includePath = atts.getOrDefault("starexec-dependency-" + i, "");
				log.debug("validateIndBenchDependencies", "Dependency Path of Dependency " + i + " is " + includePath);
				if (!includePath.isEmpty()) {
					//checkMap first
					if (foundDependencies.get(includePath) != null) {
						log.info("validateIndBenchDependencies", "Already found this one before, its id is " +
								         foundDependencies.get(includePath).getSecondaryBench().getId());
					} else {
						log.info("validateIndBenchDependencies", "This include path (" + includePath + ") is new so we must search the database.");
						int depBenchId = Benchmarks.findDependentBench(spaceId, includePath, linked, bench.getUserId
								());
						if (depBenchId > 0) {
							// these are new benchmarks, so the primary benchmark has no ID yet. This is fine:
							// the DB code for entering benchmarks will utilize the correct ID
							foundDependencies.put(includePath, new BenchmarkDependency(0, depBenchId, includePath));
							log.info("validateIndBenchDependencies", "Dependent Bench = " + depBenchId);
						}
					}
				}

				if (!foundDependencies.containsKey(includePath)) {
					log.warn("validateIndBenchDependencies", "Dependent Bench not found for " + bench.getName());
					return includePath;
				}
				bench.addDependency(foundDependencies.get(includePath));
			}
		} catch (Exception e) {
			log.error("validateIndBenchDependencies", "validate dependency failed on bench " + bench.getName(), e);
			return includePath;
		}
		return "true";
	}

	/**
	 * Gets rid of all the attributes a benchmark currently has in the database
	 *
	 * @param benchId The ID of the benchmark in question
	 * @return True on success, false on error
	 * @author Eric Burns
	 */
	public static boolean clearAttributes(int benchId) {
		Connection con = null;
		try {
			con = Common.getConnection();
			return clearAttributes(benchId, con);
		} catch (Exception e) {
			log.error("clearAttributes", e);
		} finally {
			Common.safeClose(con);
		}
		return false;
	}

	/**
	 * Gets rid of all the attributes a benchmark currently has in the database
	 *
	 * @param benchId The ID of the benchmark in question
	 * @param con The connection to make the call on
	 * @return True on success, false on error
	 * @author Eric Burns
	 */
	private static boolean clearAttributes(int benchId, Connection con) {
		CallableStatement procedure = null;
		try {
			procedure = con.prepareCall("{CALL ClearBenchAttributes(?)}");
			procedure.setInt(1, benchId);
			procedure.executeUpdate();
			return true;
		} catch (Exception e) {
			log.error("clearAttributes", e);
		} finally {
			Common.safeClose(procedure);
		}
		return false;
	}

	/**
	 * Re-process benchmarks in the given space.  Regular users can only re-process benchmarks they own. Community
	 * leaders can reprocess any benchmarks.
	 *
	 * @param spaceId the ID of the space in which to look for benchmarks to re-process
	 * @param p the Processor to apply
	 * @param hierarchy whether to process the hierarchy rooted at the space with the given ID, or just that space
	 * @param userId the ID of the user requesting re-processing
	 * @param clearOldAttrs true iff we should drop the old attributes we had from any earlier processing
	 * @return The ID of an UploadStatus object for tracking progress of this request
	 * @author Eric Burns
	 */
	public static Integer process(int spaceId, Processor p, boolean hierarchy, int userId, boolean clearOldAttrs) {
		Integer statusId = Uploads.createBenchmarkUploadStatus(spaceId, userId);
		Uploads.benchmarkFileUploadComplete(statusId);
		Uploads.fileExtractComplete(statusId);
		Uploads.processingBegun(statusId);
		final int s = spaceId;
		final Processor proc = p;
		final boolean h = hierarchy;
		final int u = userId;
		final boolean c = clearOldAttrs;
		final Integer st = statusId;
		int comm = Spaces.getCommunityOfSpace(spaceId);
		Permission perm = Permissions.get(userId, comm);

		final boolean l = perm != null && perm.isLeader();
		//It will delay the redirect until this method is finished which is why a new thread is used
		Util.threadPoolExecute(() -> {
			try {
				process(s, proc, h, u, c, st, l);
				Uploads.benchmarkEverythingComplete(st);
			} catch (Exception e) {
				log.error("process", e);
			}
		});
		return statusId;
	}

	/**
	 * Runs the given benchmark processor on all benchmarks in the given space (hierarchy)
	 *
	 * @param spaceId The ID of the relevant space
	 * @param p The benchmark processor to use
	 * @param hierarchy True if we want to run on the hierarchy, false otherwise
	 * @param statusId The ID of the UploadStatus object associated with this process, or null if one does not yet
	 * exist
	 * @param userId The ID of the user making this processing request
	 * @param clearOldAttrs If true, all existing benchmark attributes will be removed for every benchmark being
	 * processed. If false, old attributes are not cleared, but will be overwritten by new attributes with the same
	 * names
	 * @param isCommunityLeader True if the user is a community leader for this community and false otherwise
	 * @return The status ID on success, -1 otherwise
	 * @author Eric Burns
	 */
	private static boolean process(
			int spaceId, Processor p, boolean hierarchy, int userId, boolean clearOldAttrs, Integer statusId,
			boolean isCommunityLeader
	) {
		Connection con = null;

		log.info("Processing benchmarks in space " + spaceId);
		if (isCommunityLeader) {
			log.debug("User " + userId + " is a community leader, so they can process any benchmarks");
		}

		try {

			con = Common.getConnection();
			List<Benchmark> benchmarks = Benchmarks.getBySpace(spaceId);
			boolean success = Benchmarks.attachBenchAttrs(benchmarks, p, statusId);

			if (!success) {
				log.error("there was an error running the processor on each benchmark");
				return false;
			}
			int incrementCounter = 0;
			Timer timer = new Timer();
			for (Benchmark b : benchmarks) {
				//only work on the benchmarks the given user owns if they are not a community leader
				if (!isCommunityLeader && b.getUserId() != userId) {
					log.debug("Skipping benchmark " + b.getName());
					continue;
				}
				if (clearOldAttrs) {
					Benchmarks.clearAttributes(b.getId(), con);
				}

				Map<String, String> attrs = b.getAttributes();
				if (!addAttributeSetToDbIfValid(con, attrs, b, statusId)) {
					return false;
				}
				//updates the type of the benchmark with the new processor
				Benchmarks.updateDetails(b.getId(), b.getName(), b.getDescription(), b.isDownloadable(), p.getId());

				incrementCounter++;
				if (timer.getTime() > R.UPLOAD_STATUS_TIME_BETWEEN_UPDATES) {
					Uploads.incrementCompletedBenchmarks(statusId, incrementCounter);
					incrementCounter = 0;
					timer.reset();
				}
			}
			if (incrementCounter > 0) {
				Uploads.incrementCompletedBenchmarks(statusId, incrementCounter);
			}
			success = true;
			if (hierarchy) {
				List<Space> spaces = Spaces.getSubSpaceHierarchy(spaceId, userId);
				for (Space s : spaces) {
					success = success &&
							Benchmarks.process(s.getId(), p, false, userId, clearOldAttrs, statusId,
							                   isCommunityLeader);
				}
			}
			return success;
		} catch (Exception e) {
			log.error("process", e);
		} finally {
			Common.safeClose(con);
		}
		return false;
	}

	/**
	 * Returns the ID of every benchmark a user owns that is orphaned
	 *
	 * @param userId The ID of the user who owns all the benchmarks to be returned
	 * @return A list of orphaned benchmark IDs owned by the given user, or null on error.
	 */
	public static List<Integer> getOrphanedBenchmarks(int userId) {
		Connection con = null;
		CallableStatement procedure = null;
		ResultSet results = null;
		List<Integer> ids = new ArrayList<>();

		try {
			con = Common.getConnection();
			procedure = con.prepareCall("{CALL GetOrphanedBenchmarkIds(?)}");
			procedure.setInt(1, userId);
			results = procedure.executeQuery();
			while (results.next()) {
				ids.add(results.getInt("id"));
			}
			return ids;
		} catch (Exception e) {
			log.error("getOrphanedBenchmarks", e);
		} finally {
			Common.safeClose(con);
			Common.safeClose(procedure);
			Common.safeClose(results);
		}
		return null;
	}

	/**
	 * Recycles all of the benchmarks a user has that are not in any spaces
	 *
	 * @param userId The ID of the user who will have their benchmarks recycled
	 * @return True on success or false otherwise
	 */
	public static boolean recycleOrphanedBenchmarks(int userId) {
		List<Integer> ids = getOrphanedBenchmarks(userId);
		//on error
		if (ids == null) {
			return false;
		}

		try {
			boolean success = true;
			for (Integer id : ids) {
				success = success && Benchmarks.recycle(id);
			}
			return success;
		} catch (Exception e) {
			log.error("recycleOrphanedBenchmarks", e);
		}
		return false;
	}

	/**
	 * Gets every Benchmark that shares a space with the given user
	 *
	 * @param userId The ID of the user
	 * @return The list of Benchmarks, or null on error
	 */
	public static List<Benchmark> getBenchmarksInSharedSpaces(int userId) {
		Connection con = null;
		CallableStatement procedure = null;
		ResultSet results = null;
		try {
			con = Common.getConnection();
			procedure = con.prepareCall("{CALL GetBenchmarksInSharedSpaces(?)}");
			procedure.setInt(1, userId);

			results = procedure.executeQuery();
			return resultsToBenchmarkWithType(results);
		} catch (Exception e) {
			log.error("getBenchmarksInSharedSpaces", e);
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
	public static List<Benchmark> getPublicBenchmarks() {
		Connection con = null;
		CallableStatement procedure = null;
		ResultSet results = null;
		try {
			con = Common.getConnection();
			procedure = con.prepareCall("{CALL GetPublicBenchmarks()}");
			results = procedure.executeQuery();
			return resultsToBenchmarkWithType(results);
		} catch (Exception e) {
			log.error("getPublicBenchmarks", e);
		} finally {
			Common.safeClose(con);
			Common.safeClose(procedure);
			Common.safeClose(results);
		}
		return null;
	}

	private static List<Benchmark> resultsToBenchmarkWithType(ResultSet results) throws SQLException {
		List<Benchmark> benchmarks = new ArrayList<>();
		while (results.next()) {
			Benchmark s = resultToBenchmark(results);
			Processor t = Processors.resultSetToProcessor(results, "types");
			s.setType(t);
			benchmarks.add(s);
		}
		return benchmarks;
	}

	/**
	 * Retrieves a list of every Benchmark the given user is allowed to use. Used for quick jobs. Benchmarks a user can
	 * see include Benchmarks they own, Benchmarks in public spaces, and Benchmarks in spaces the user is also in
	 *
	 * @param userId The user to get benchmarks for
	 * @return A list of benchmarks that the given user can see
	 */
	public static List<Benchmark> getByUser(int userId) {
		try {
			//will stores Benchmarks according to their IDs, used to remove duplicates
			HashMap<Integer, Benchmark> uniqueBenchmarks = new HashMap<>();
			for (Benchmark s : getByOwner(userId)) {
				uniqueBenchmarks.put(s.getId(), s);
			}
			for (Benchmark s : Benchmarks.getPublicBenchmarks()) {
				uniqueBenchmarks.put(s.getId(), s);
			}
			for (Benchmark s : Benchmarks.getBenchmarksInSharedSpaces(userId)) {
				uniqueBenchmarks.put(s.getId(), s);
			}
			List<Benchmark> benchmarks = new ArrayList<>();
			benchmarks.addAll(uniqueBenchmarks.values());
			return benchmarks;
		} catch (Exception e) {
			log.error("getByUser", e);
		}
		return null;
	}

	/**
	 * Filters a list of benchmarks using the given query
	 *
	 * @param searchQuery Query for the Benchmarks. Not case sensitive
	 * @return A subset of the given Benchmarks where, for every Benchmark returned, either the name or the description
	 * includes the search query.
	 */
	protected static List<Benchmark> filterBenchmarks(List<Benchmark> benchmarks, String searchQuery) {
		//no filtering is necessary if there's no query
		if (Util.isNullOrEmpty(searchQuery)) {
			return benchmarks;
		}

		searchQuery = searchQuery.toLowerCase();
		List<Benchmark> filteredBenchmarks = new ArrayList<>();
		for (Benchmark b : benchmarks) {
			log.debug("benchmark name " + b.getName());
			try {
				if (b.getName().toLowerCase().contains(searchQuery) ||
						b.getDescription().toLowerCase().contains(searchQuery)) {
					filteredBenchmarks.add(b);
				}
			} catch (Exception e) {
				log.error("filterBenchmarks", e);
			}
		}
		return filteredBenchmarks;
	}

	/**
	 * Returns the Benchmarks needed to populate a DataTables page for a given user. Benchmarks include all Benchmarks
	 * the user can see
	 *
	 * @param query DataTablesQuery object containing parameters for this search
	 * @param userId ID of user to get Benchmarks for
	 * @param totals Size 2 array that, on return, will contain the total number of records as the first element and
	 * the
	 * total number of elements after filtering as the second element
	 * @return The list of benchmarks to display in the table
	 */
	public static List<Benchmark> getBenchmarksForNextPageByUser(DataTablesQuery query, int userId, int[] totals) {
		List<Benchmark> benchmarks = Benchmarks.getByUser(userId);

		totals[0] = benchmarks.size();
		benchmarks = Benchmarks.filterBenchmarks(benchmarks, query.getSearchQuery());

		totals[1] = benchmarks.size();
		BenchmarkComparator compare = new BenchmarkComparator(query.getSortColumn(), query.isSortASC());
		return Util.handlePagination(benchmarks, compare, query.getStartingRecord(), query.getNumRecords());
	}

	public static List<Benchmark> getBrokenBenchDependencies(int benchId) throws SQLException {
		Connection con = null;
		CallableStatement procedure = null;
		ResultSet results = null;
		try {
			con = Common.getConnection();
			procedure = con.prepareCall("{CALL GetBrokenBenchDependencies(?)}");
			procedure.setInt(1, benchId);
			results = procedure.executeQuery();
			List<Benchmark> Benchmarks = new LinkedList<>();

			while (results.next()) {
				Benchmark s = get(results.getInt("id"), true, true);
				Benchmarks.add(s);
			}
			return Benchmarks;
		} catch (SQLException e) {
			log.error("getBrokenBenchDependencies", "rethrowing exception", e);
			throw e;
		} finally {
			Common.safeClose(con);
			Common.safeClose(procedure);
			Common.safeClose(results);
		}
	}

	public static Boolean benchHasBrokenDependencies(int benchId) {
		Connection con = null;
		CallableStatement procedure = null;
		ResultSet results = null;
		try {
			con = Common.getConnection();
			procedure = con.prepareCall("{CALL GetBrokenBenchDependencies(?)}");
			procedure.setInt(1, benchId);
			results = procedure.executeQuery();
			List<Benchmark> Benchmarks = new LinkedList<>();
			return results.isBeforeFirst();
		} catch (Exception e) {
			log.error("benchHasBrokenDependencies", e);
		} finally {
			Common.safeClose(con);
			Common.safeClose(procedure);
			Common.safeClose(results);
		}
		return null;
	}
}
