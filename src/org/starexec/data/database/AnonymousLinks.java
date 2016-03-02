package org.starexec.data.database;


import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.UUID;

import org.apache.log4j.Logger;
import org.apache.commons.lang3.tuple.ImmutableTriple;
import org.apache.commons.lang3.tuple.Triple;

import org.starexec.util.LogUtil;
import org.starexec.constants.R;
import org.starexec.constants.Web;

import org.starexec.data.database.Jobs;

import org.starexec.data.to.Benchmark;
import org.starexec.data.to.Configuration;
import org.starexec.data.to.Job;
import org.starexec.data.to.JobPair;
import org.starexec.data.to.JobSpace;
import org.starexec.data.to.Solver;
import org.starexec.data.to.SolverStats;
import org.starexec.data.to.pipelines.JoblineStage;

import org.starexec.util.Util;


public class AnonymousLinks {
	private static final Logger log = Logger.getLogger(AnonymousLinks.class);			
	private static final LogUtil logUtil = new LogUtil(log);
	private static final int MAX_UUID_LENGTH = 36;


	public enum PrimitivesToAnonymize {
		ALL, ALL_BUT_BENCH, NONE
	}

	/**
	 * Utility method that determines if benchmarks are anonymized in a PrimitivesToAnonmize enum.
	 * @author Albert Giegerich
	 */
	public static boolean areBenchmarksAnonymized( PrimitivesToAnonymize primitivesToAnonymize ) {
		return isEverythingAnonymized( primitivesToAnonymize );
	}

	/**
	 * Utility method that determines if solves are anonymized in a PrimitivesToAnonmize enum.
	 * @author Albert Giegerich
	 */
	public static boolean areSolversAnonymized( PrimitivesToAnonymize primitivesToAnonymize ) {
		return isEverythingAnonymized(primitivesToAnonymize) || primitivesToAnonymize == PrimitivesToAnonymize.ALL_BUT_BENCH;
	}

	/**
	 * Utility method that determines if jobs are anonymized in a PrimitivesToAnonmize enum.
	 * @author Albert Giegerich
	 */
	public static boolean areJobsAnonymized( PrimitivesToAnonymize primitivesToAnonymize ) {
		return isEverythingAnonymized( primitivesToAnonymize ) || primitivesToAnonymize == PrimitivesToAnonymize.ALL_BUT_BENCH;
	}

	/**
	 * Utility method that determines if all primitives are anonymized in a PrimitivesToAnonmize enum.
	 * @author Albert Giegerich
	 */
	public static boolean isEverythingAnonymized( PrimitivesToAnonymize primitivesToAnonymize ) {
		return primitivesToAnonymize == PrimitivesToAnonymize.ALL;
	}

	/**
	 * Utility method that determines if no primitives are anonymized in a PrimitivesToAnonmize enum.
	 * @author Albert Giegerich
	 */
	public static boolean isNothingAnonymized( PrimitivesToAnonymize primitivesToAnonymize ) {
		return primitivesToAnonymize == PrimitivesToAnonymize.NONE;
	}

	/**
	 * Converts from a PrimitivesToAnonymize enum name to a PrimitiveToAnonymize enum.
	 * @param enumName the string representation of the PrimitivesToAnonymize enum.
	 * @return the PrimitivesToAnonymize enum represented by the input string.
	 * @author Albert Giegerich
	 */
	public static PrimitivesToAnonymize createPrimitivesToAnonymize( String enumName ) {
		PrimitivesToAnonymize primitivesToAnonymize = null;
		switch (enumName) {
			case R.ANONYMIZE_ALL: 
				primitivesToAnonymize = PrimitivesToAnonymize.ALL;
				break;
			case R.ANONYMIZE_ALL_BUT_BENCH:
				primitivesToAnonymize = PrimitivesToAnonymize.ALL_BUT_BENCH;
				break;
			case R.ANONYMIZE_NONE:
				primitivesToAnonymize = PrimitivesToAnonymize.NONE;
				break;
			default:
				throw new IllegalArgumentException( "There is no PrimitivesToAnonymize enum associated with the string: "+enumName);
		}
		return primitivesToAnonymize;
	}

	/**
	 * Converts from a PrimitivesToAnonymize enum to the string represenation of that enum.
	 * @param primitivesToAnonymize a PrimitivesToAnonymize enum.
	 * @return the String representation of the input enum.
	 * @author Albert Giegerich
	 */
	public static String getPrimitivesToAnonymizeName( PrimitivesToAnonymize primitivesToAnonymize ) {
		String enumName = null;
		switch ( primitivesToAnonymize ) {
			case ALL:
				enumName = R.ANONYMIZE_ALL;
				break;
			case ALL_BUT_BENCH:
				enumName = R.ANONYMIZE_ALL_BUT_BENCH;
				break;
			case NONE:
				enumName= R.ANONYMIZE_NONE;
				break;
			default:
				throw new IllegalArgumentException( "A case has not been implemented for the input PrimitivesToAnonymize." );
		}
		return enumName;
	}

	/**
	 * Adds a new anonymous link entry to the database.
	 * @param primitiveId The id of the primitive for which a public anonymous link will be generated.
	 * @param primitiveType The type of primitive (benchmark, solver, etc.) that a link will be generated for.
	 * @param primitivesToAnonymize the enum representing which primitives should be anonymized for this anonymous page.
	 * @throws SQLException if the database fails.
	 * @author Albert Giegerich
	 */
	public static String addAnonymousLink(
			final String primitiveType,
			final int primitiveId,
			final PrimitivesToAnonymize primitivesToAnonymize ) throws SQLException {

		final String methodName = "addAnonymousLink";
		logUtil.entry(methodName);

		final String universallyUniqueId = UUID.randomUUID().toString();

		log.debug( "Adding anonymous link for " + primitiveType + " with id=" + primitiveId );

		Connection con = null;
		CallableStatement procedure = null;

		try {
			// Setup the AddAnonymousLink procedure.
			con = Common.getConnection();
			Common.beginTransaction(con);
			procedure = con.prepareCall("{CALL AddAnonymousLink(?, ?, ?, ?)}");

			// Set the parameters
			procedure.setString( 1, universallyUniqueId );
			procedure.setString( 2, primitiveType );
			procedure.setInt( 3, primitiveId );
			procedure.setString( 4, getPrimitivesToAnonymizeName( primitivesToAnonymize ));

			// Do update and commit the changes.
			procedure.executeUpdate();
			Common.endTransaction(con);

			return universallyUniqueId;
		} catch (SQLException e) { 
			logUtil.error( methodName, "Threw an exception while adding anonymous link for " + primitiveType + " with id=" + primitiveId +
				"\nError message: " + e.getMessage() );
			Common.doRollback( con );

			throw e;
		} finally {
			Common.safeClose(con);
			Common.safeClose(procedure);
		}
	}

	/**
	 * Get the unique code that links to a given primitive from the database.
	 * @param primitiveType The type of primitive for which a code will be retrieved (solver, bench, etc.)
	 * @param primitiveId The id of the primitive for which a code will be retrieved.
	 * @param primitivesToAnonymize an enum that determines which primitives wil be anonymized on the anonymous page. 
	 * @return an Optional containing the code if present in the database otherwise an empty Optional.
	 * @throws SQLException if the database fails.
	 * @author Albert Giegerich
	 */
	public static Optional<String> getAnonymousLinkCode(
			final String primitiveType,
			final int primitiveId,
			final PrimitivesToAnonymize primitivesToAnonymize ) throws SQLException {

		final String methodName = "getAnonymousLinkCode";
		logUtil.entry(methodName);

		Connection con = null;
		CallableStatement procedure = null;
		ResultSet results=null;

		try {
			con = Common.getConnection();
			Common.beginTransaction( con );

			// Setup the GetAnonymousLink procedure.
			procedure = con.prepareCall( "{CALL GetAnonymousLink(?, ?, ?)}" );
			procedure.setString( 1, primitiveType );
			procedure.setInt( 2, primitiveId );
			procedure.setString( 3, getPrimitivesToAnonymizeName( primitivesToAnonymize ) );

			results = procedure.executeQuery();
			Common.endTransaction(con);
			// If the code is in the database return it otherwise return an empty Optional.
			if ( results.next() ) {
				return Optional.of( results.getString("unique_id") );
			} else {
				return Optional.empty();
			}
		} catch (SQLException e) {
			log.error( "Caught an exception while trying to get an anonymous link from database.", e );
			Common.doRollback( con );
			throw e;
		} finally {
			Common.safeClose(con);
			Common.safeClose(procedure);
			Common.safeClose(results);
		}
	}

	/**
	 * Gets the id of the job associated with a given UUID
	 * @param universallyUniqueId the UUID associated with the job whose id will be retrieved.
	 * @return the id of the job associated with the given UUID
	 * @throws SQLException if the database fails.
	 * @author Albert Giegerich
	 */
	public static Optional<Integer> getIdOfJobAssociatedWithLink( final String universallyUniqueId ) throws SQLException {
		return getIdOfPrimitiveAssociatedWithLink( universallyUniqueId, R.JOB );
	}

	/**
	 * Gets the id of the benchmark associated with a given UUID
	 * @param universallyUniqueId The UUID associated with the benchmark whose id will be retrieved.
	 * @return The id of the benchmark associated with the given UUID
	 * @throws SQLException if the database fails.
	 * @author Albert Giegerich
	 */
	public static Optional<Integer> getIdOfBenchmarkAssociatedWithLink( final String universallyUniqueId ) throws SQLException {
		return getIdOfPrimitiveAssociatedWithLink( universallyUniqueId, R.BENCHMARK );
	}

	/**
	 * Gets the id of the solver associated with a given UUID
	 * @param universallyUniqueId the UUID associated with the solver whose id will be retrieved.
	 * @return the id of the solver associated with the given UUID
	 * @throws SQLException if the database fails.
	 * @author Albert Giegerich
	 */
	public static Optional<Integer> getIdOfSolverAssociatedWithLink( final String universallyUniqueId ) throws SQLException {
		return getIdOfPrimitiveAssociatedWithLink( universallyUniqueId, R.SOLVER );
	}
	

	/**
	 * Gets the id of the primitive associated with a given UUID
	 * @param universallyUniqueId the UUID associated with the primitive whose id will be retrieved.
	 * @param primitiveType the type of primitive whose id will be retrieved.
	 * @return the id of the primitive associated with the given UUID
	 * @throws SQLException if the database fails.
	 * @author Albert Giegerich
	 */
	private static Optional<Integer> getIdOfPrimitiveAssociatedWithLink(
			final String universallyUniqueId, 
			final String primitiveType ) throws SQLException {

		final String methodName = "getIdOfPrimitiveAssociatedWithCode";
		logUtil.entry( methodName );

		// UUIDs have a max size of 36.
		if ( universallyUniqueId.length() > MAX_UUID_LENGTH ) {
			return Optional.empty();
		}

		Connection con = null;
		CallableStatement procedure = null;
		ResultSet results = null;

		try {
			con = Common.getConnection();
			Common.beginTransaction( con );

			// Setup the GetAnonymousLink procedure.
			procedure = con.prepareCall( "{CALL GetIdOfPrimitiveAssociatedWithLink(?, ?)}" );
			procedure.setString( 1, universallyUniqueId );
			procedure.setString( 2, primitiveType );

			results = procedure.executeQuery();
			Common.endTransaction(con);
			// If the code is in the database return it otherwise return an empty Optional.
			if ( results.next() ) {
				return Optional.of( results.getInt("primitive_id") );
			} else {
				return Optional.empty();
			}
		} catch (SQLException e) {
			logUtil.error( methodName, e.toString() );
			Common.doRollback( con );
			throw e;
		} finally {
			Common.safeClose( con );
			Common.safeClose( procedure );
			Common.safeClose( results );
		}
	}

	/**
	 * Gets which primitives to anonymize for an anonymous solver page.
	 * @param linkUuid the anonymous link unique identifier.
	 * @return an Optional containing an enum specifying which primitives to anonymize if the anonymous page exists otherwise an empty Optional.
	 * @throws SQLException if the database fails.
	 * @author Albert Giegerich
	 */
	public static Optional<PrimitivesToAnonymize> getPrimitivesToAnonymizeForSolver( String linkUuid ) throws SQLException {
		return getPrimitivesToAnonymize( linkUuid, R.SOLVER );
	}

	/**
	 * Gets which primitives to anonymize for an anonymous job page.
	 * @param linkUuid the anonymous link unique identifier.
	 * @return an Optional containing an enum specifying which primitives to anonymize if the anonymous page exists otherwise an empty Optional.
	 * @throws SQLException if the database fails.
	 * @author Albert Giegerich
	 */
	public static Optional<PrimitivesToAnonymize> getPrimitivesToAnonymizeForJob( String linkUuid ) throws SQLException {
		return getPrimitivesToAnonymize( linkUuid, R.JOB );
	}

	/**
	 * Gets which primitives to anonymize for an anonymous benchmark page.
	 * @param linkUuid the anonymous link unique identifier.
	 * @return an Optional containing an enum specifying which primitives to anonymize if the anonymous page exists otherwise an empty Optional.
	 * @throws SQLException if the database fails.
	 * @author Albert Giegerich
	 */
	public static Optional<PrimitivesToAnonymize> getPrimitivesToAnonymizeForBenchmark( String linkUuid ) throws SQLException {
		return getPrimitivesToAnonymize( linkUuid, R.BENCHMARK );
	}

	private static Optional<PrimitivesToAnonymize> getPrimitivesToAnonymize( String linkUuid, String primitiveType ) throws SQLException {
		final String methodName = "getPrimitivesToAnonymize";
		logUtil.entry( methodName );

		// UUIDs have a max size of 36.
		if ( linkUuid.length() > MAX_UUID_LENGTH ) {
			return Optional.empty();
		}

		Connection con = null;
		CallableStatement procedure = null;
		ResultSet results = null;

		try {
			con = Common.getConnection();

			// Setup the GetPrimitivesToAnonymize procedure.
			procedure = con.prepareCall( "{CALL GetPrimitivesToAnonymize(?, ?)}" );
			procedure.setString( 1, linkUuid );
			procedure.setString( 2, primitiveType );

			results = procedure.executeQuery();
			// If the code is in the database return it otherwise return an empty Optional.
			if ( results.next() ) {
				return Optional.of( createPrimitivesToAnonymize( results.getString( "primitives_to_anonymize" )));
			} else {
				return Optional.empty();
			}
		} catch (SQLException e) {
			logUtil.error( methodName, e.toString() );
			throw e;
		} finally {
			Common.safeClose( con );
			Common.safeClose( procedure );
			Common.safeClose( results );
		}
	}

	/**
	 * Deletes all old links in the database.
	 * @param ageThresholdInDays the minimum age a link must be to be deleted.
	 * @author Albert Giegerich
	 */
	public static void deleteOldLinks( int ageThresholdInDays ) throws SQLException {
		final String methodName = "deleteOldLinks";
		Connection con = null;
		CallableStatement procedure = null;

		try {
			con = Common.getConnection();
			Common.beginTransaction( con );
			
			procedure = con.prepareCall( "{CALL DeleteOldLinks(?)}" );
			procedure.setInt( 1, ageThresholdInDays );

			procedure.executeUpdate();

			Common.endTransaction(con);
		} catch (SQLException e) {
			logUtil.error( methodName, Util.getStackTrace( e ));
			Common.doRollback( con );
			throw e;
		} finally {
			Common.safeClose( con );
			Common.safeClose( procedure );
		}
	}

	/**
	 * Deletes all anonymous link that corresponds to the given UUIDs
	 * @param uuids the identifiers for the anonymous links.
	 * @author Albert Giegerich
	 */
	public static void delete( List<String> uuids ) throws SQLException {
		final String methodName = "delete(List<String>)";
		Connection con = null;
		try {
			con = Common.getConnection();
			Common.beginTransaction( con );
			for ( String uuid : uuids ) {
				delete( uuid, con );
			}
			Common.endTransaction( con );
		} catch (SQLException e) {
			logUtil.error( methodName, Util.getStackTrace( e ));
			Common.doRollback( con );
			throw e;
		} finally {
			Common.safeClose( con );
		}
	}

	/**
	 * Deletes an anonymous link that corresponds to a given UUID
	 * @param uuid the identifier for the anonymous link.
	 * @author Albert Giegerich
	 */
	public static void delete( String uuid ) throws SQLException {
		final String methodName = "delete(String)";
		Connection con = null;
		try {
			con = Common.getConnection();
			Common.beginTransaction( con );
			delete( uuid, con );
			Common.endTransaction(con);
		} catch (SQLException e) {
			logUtil.error( methodName, Util.getStackTrace( e ));
			Common.doRollback( con );
		} finally {
			Common.safeClose( con );
		}
	}

	private static void delete( String uuid, Connection con ) throws SQLException {
		final String methodName = "delete(String, Connection)";

		CallableStatement procedure = null;

		try {
			procedure = con.prepareCall( "{CALL DeleteAnonymousLink(?)}" );
			procedure.setString( 1, uuid );
			procedure.executeUpdate();
		} catch (SQLException e) {
			throw e;
		} finally {
			Common.safeClose( procedure );
		}
	}

	public static boolean hasJobBeenAnonymized( final int jobId ) throws SQLException {
		final String methodName= "hasJobBeenAnonymized";
		logUtil.entry(methodName);
		Connection con = null;
		CallableStatement procedure = null;
		ResultSet results = null;
		try {
			con = Common.getConnection();
			procedure = con.prepareCall("{CALL GetAnonymousNamesForJob(?)}");

			// Set the parameters
			procedure.setInt( 1, jobId );

			// Do update and commit the changes.
			results = procedure.executeQuery();
			// return true/false depending on if there are any results.
			return results.next();
		} catch( SQLException e ) {
			logUtil.error( methodName, Util.getStackTrace(e));
			throw e;
		} finally {
			Common.safeClose( con );
			Common.safeClose( procedure );
			Common.safeClose( results );
		}
	}

	/**
	 * Adds all primitives in a job to the anonymous names table for use in the anonymous job page.
	 * @param jobId the id of the job from which to get primitives.
	 * @throws SQLException if the database fails.
	 * @author Albert Giegerich
	 */
	public static void addAnonymousNamesForJob( final int jobId ) throws SQLException {
		final String methodName = "addAnonymousNamesForJob";
		logUtil.entry( methodName );

		Job job = Jobs.getWithSimplePairs( jobId );

		List<JobPair> jobPairs = job.getJobPairs();

		List<Benchmark> benchmarks = new ArrayList<>();
		List<Solver> solvers = new ArrayList<>();
		for ( JobPair pair : jobPairs ) {
			benchmarks.add( pair.getBench() );
			for ( JoblineStage stage : pair.getStages() ) {
				solvers.add( stage.getSolver() );
			}
		}

		Connection con = null;
		try {

			// Generate a map of anonymized solver names and add them to the database.
			Map<Integer, String> anonymizedSolverNames = buildAnonymizedSolverNamesMap( solvers );

			con = Common.getConnection();
			Common.beginTransaction(con);

			for ( Integer solverId : anonymizedSolverNames.keySet() ) {

				// For each solver, add all the anonymous config names.
				Map<Integer, String> anonymizedConfigNames = buildAnonymizedConfigNamesMap( solverId );
				for ( Integer configId : anonymizedConfigNames.keySet() ) {
					String anonymousConfigName = anonymizedConfigNames.get( configId );
					addAnonymousPrimitiveName( anonymousConfigName, configId, R.CONFIGURATION, jobId,con );
				}

				String anonymousSolverName = anonymizedSolverNames.get( solverId );
				addAnonymousPrimitiveName( anonymousSolverName, solverId, R.SOLVER, jobId,con );
			}

			// Generate a map of anonymized benchmark names and add them to the database.
			Map<Integer, String> anonymizedBenchmarkNames = buildAnonymizedBenchmarkNamesMap( benchmarks );
			for ( Integer benchmarkId : anonymizedBenchmarkNames.keySet() ) {
				String anonymousBenchmarkName = anonymizedBenchmarkNames.get( benchmarkId );
				addAnonymousPrimitiveName( anonymousBenchmarkName, benchmarkId, R.BENCHMARK, jobId, con );
			}

			Common.endTransaction( con );
		} catch (SQLException e) {
			logUtil.error( methodName, Util.getStackTrace( e ) );
			throw e;
		} finally {
			Common.doRollback( con );
			Common.safeClose( con );
		}
	}

	/**
	 * @param jobId the id of the anonynmized job to get solver information for.
	 * @author Albert Giegerich
	 */
	public static List<Triple<String, String, Integer>> getAnonymousSolverNamesKey(int jobId) throws SQLException {
		final String methodName = "getAnonymizedSolverNamesKey";
		Connection con = null;
		CallableStatement procedure = null;
		ResultSet results = null;
		try {
			con = Common.getConnection();
			procedure = con.prepareCall( "{CALL GetAnonymousSolverNamesAndIds(?)}" );
			procedure.setInt(1, jobId);

			results = procedure.executeQuery();

			List<Triple<String, String, Integer>> anonymizedSolverNamesKey = new ArrayList<>();
			while ( results.next() ) {
				int solverId= results.getInt( "primitive_id" );
				Solver solver = Solvers.get( con, solverId, false );
				String anonymizedSolverName = results.getString( "anonymous_name" );

				anonymizedSolverNamesKey.add( new ImmutableTriple<>(solver.getName(), anonymizedSolverName, solverId) );
			}
			return anonymizedSolverNamesKey;
		} catch (SQLException e) {
			logUtil.error(methodName, "Database failure while geting anonymized solver names key.\n"+Util.getStackTrace( e ) );
			throw e;
		} finally {
			Common.safeClose(con);
			Common.safeClose(procedure);
			Common.safeClose(results);
		}
	}


	private static void addAnonymousPrimitiveName( 
			final String anonymousName, 
			final int primitiveId, 
			final String primitiveType, 
			final int jobId,
			final Connection con ) throws SQLException {

		final String methodName = "addAnonymousPrimitiveName";
		logUtil.entry( methodName );

		CallableStatement procedure = null;

		try {
			procedure = con.prepareCall( "{CALL AddAnonymousPrimitiveName(?, ?, ?, ?)}" );
			procedure.setString( 1, anonymousName );
			procedure.setInt( 2, primitiveId );
			procedure.setString( 3, primitiveType );
			procedure.setInt( 4, jobId );

			procedure.executeUpdate();

		} catch ( SQLException e ) {
			logUtil.error( methodName, Util.getStackTrace( e ) );
			throw e;
		} finally {
			Common.safeClose( procedure );
		}	
	}


	/**
	 * @param solverId The solver id to get the configuration map for
	 * @author Albert Giegerich
	 * @return A map from configuration id's to their anonymized names for use in the anonymous page feature.
	 */
	private static Map<Integer, String> buildAnonymizedConfigNamesMap(int solverId) {
		List<Configuration> configs = Solvers.getConfigsForSolver( solverId );

		Map<Integer, String> configIdToNameMap  = new HashMap<>();
		int numberToAppend = 1;
		for (Configuration config : configs) {
			if (!configIdToNameMap.containsKey( config.getId() )) {
				configIdToNameMap.put( config.getId(), "Config"+numberToAppend );
				numberToAppend +=1 ;
			}
		}
		return configIdToNameMap;
	}

	/**
	 * Gets a mapping of solver ID's to unique anonymized names.
	 * @param jobId The id of the job to get a mapping for.
	 * @author Albert Giegerich
	*/
	private static Map<Integer, String> buildAnonymizedSolverNamesMap( final List<Solver> solvers ) {
		// Build a mapping of solvers to anonymized names.
		Map<Integer, String> solverIdToAnonymizedName = new HashMap<>();
		int numberToAppend = 1;
		for ( Solver solver : solvers ) {
			int solverId =  solver.getId();
			if ( !solverIdToAnonymizedName.containsKey( solverId )) {
				solverIdToAnonymizedName.put( solverId, "Solver" + numberToAppend ); 
				numberToAppend += 1;
			}
		}
		return solverIdToAnonymizedName;
	}


	/**
	 * Gets a mapping of bencmark ID's to unique anonymized names.
	 * @param benchmarks a list of benchmarks to get anonymized names for.
	 * @return a mapping from benchmark ID's to an anonymous name for the benchmark.
	 * @author Albert Giegerich
	 */
	private static Map<Integer, String> buildAnonymizedBenchmarkNamesMap( final List<Benchmark> benchmarks ) {
		final String methodName = "buildAnonymizedBenchmarkNamesMap";
		logUtil.entry( methodName );

		Map<Integer, String> benchmarkIdToAnonymizedName = new HashMap<>();
		int numberToAppend = 1;
		for ( Benchmark bench : benchmarks ) {
			int benchId =  bench.getId();
			if ( !benchmarkIdToAnonymizedName.containsKey( benchId )) {
				benchmarkIdToAnonymizedName.put( benchId, "Benchmark" + numberToAppend ); 
				numberToAppend += 1;
			}
		}
		return benchmarkIdToAnonymizedName;
	}

	/**
	 * Gets a mapping of job space ID's to unique anonymized names.
	 * @param jobId the id of the job to get a mapping for.
	 * @author Albert Giegerich
	 */
	public static Map<Integer, String> getAnonymizedJobSpaceNames( final int jobId ) {
		Job job = Jobs.get( jobId );

		List<JobSpace> jobSpaces = Spaces.getSubSpacesForJob( job.getPrimarySpace(), true );
		jobSpaces.add( Spaces.getJobSpace( job.getPrimarySpace() ));
		Collections.sort( jobSpaces, (space1, space2) -> space1.getId() - space2.getId() );

		Map<Integer, String> jobSpaceIdToAnonymizedName = new HashMap<>();
		int numberToAppend = 1;
		for ( JobSpace space : jobSpaces ) {
			int spaceId =  space.getId();
			if ( !jobSpaceIdToAnonymizedName.containsKey( spaceId )) {
				jobSpaceIdToAnonymizedName.put( spaceId, "Space" + numberToAppend ); 
				numberToAppend += 1;
			}
		}
		return jobSpaceIdToAnonymizedName;
	}

}
