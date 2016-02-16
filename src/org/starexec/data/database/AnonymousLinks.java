package org.starexec.data.database;


import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.UUID;

import org.apache.log4j.Logger;

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

import org.starexec.util.Util;


public class AnonymousLinks {
	private static final Logger log = Logger.getLogger(AnonymousLinks.class);			
	private static final LogUtil logUtil = new LogUtil(log);
	private static final int MAX_UUID_LENGTH = 36;


	public enum PrimitivesToAnonymize {
		ALL, ALL_BUT_BENCH, NONE
	}

	public static boolean areBenchmarksAnonymized( PrimitivesToAnonymize primitivesToAnonymize ) {
		return isEverythingAnonymized( primitivesToAnonymize );
	}

	public static boolean areSolversAnonymized( PrimitivesToAnonymize primitivesToAnonymize ) {
		return isEverythingAnonymized(primitivesToAnonymize) || primitivesToAnonymize == PrimitivesToAnonymize.ALL_BUT_BENCH;
	}

	public static boolean areJobsAnonymized( PrimitivesToAnonymize primitivesToAnonymize ) {
		return isEverythingAnonymized( primitivesToAnonymize ) || primitivesToAnonymize == PrimitivesToAnonymize.ALL_BUT_BENCH;
	}

	public static boolean isEverythingAnonymized( PrimitivesToAnonymize primitivesToAnonymize ) {
		return primitivesToAnonymize == PrimitivesToAnonymize.ALL;
	}

	public static boolean isNothingAnonymized( PrimitivesToAnonymize primitivesToAnonymize ) {
		return primitivesToAnonymize == PrimitivesToAnonymize.NONE;
	}

	/**
	 * Converts from a PrimitivesToAnonymize enum name to a PrimitiveToAnonymize enum.
	 * @param enumName the string representation of the PrimitivesToAnonymize enum.
	 * @return the PrimitivesToAnonymize enum represented by the input string.
	 * @author Albert Giegerich
	 */
	public static PrimitivesToAnonymize createPrimitivesToAnonymize(String enumName ) {
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
	 * @param universallyUniqueId A string that will uniquely identify the benchmark.
	 * @param primitiveId The id of the primitive for which a public anonymous link will be generated.
	 * @param primitiveType The type of primitive (benchmark, solver, etc.) that a link will be generated for.
	 * @param hidePrimitiveName Whether or not the primitive's name should be hidden on the link page.
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
		final String procedureName = "AddAnonymousLink";

		try {
			// Setup the AddAnonymousLink procedure.
			con = Common.getConnection();
			Common.beginTransaction(con);
			procedure = con.prepareCall("{CALL " + procedureName + "(?, ?, ?, ?)}");

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
	 * Checks whether the job name for a link should be hidden.
	 * @param universallyUniqueId The uuid associated with the primitive
	 * @return An optional containing the result if the link exists otherwise an empty optional.
	 * @author Albert Giegerich
	public static Optional<Boolean> isJobNameHidden( final String universallyUniqueId ) throws SQLException {
		return isPrimitiveNameHidden( universallyUniqueId, R.JOB );
	}

	/**
	 * Checks whether the benchmark name for a link should be hidden.
	 * @param universallyUniqueId The uuid associated with the primitive
	 * @return An optional containing the result if the link exists otherwise an empty optional.
	 * @author Albert Giegerich
	public static Optional<Boolean> isBenchmarkNameHidden( final String universallyUniqueId ) throws SQLException {
		return isPrimitiveNameHidden( universallyUniqueId, R.BENCHMARK );
	}

	/**
	 * Checks whether the solver name for a link should be hidden.
	 * @param universallyUniqueId The uuid associated with the primitive
	 * @return An optional containing the result if the link exists otherwise an empty optional.
	 * @author Albert Giegerich
	public static Optional<Boolean> isSolverNameHidden( final String universallyUniqueId ) throws SQLException {
		return isPrimitiveNameHidden( universallyUniqueId, R.SOLVER );
	}

	/**
	 * Checks whether the primitive name for a link should be hidden.
	 * @param universallyUniqueId The uuid associated with the primitive
	 * @author Albert Giegerich
	private static Optional<Boolean> isPrimitiveNameHidden( final String universallyUniqueId, final String primitiveType ) throws SQLException {
		final String methodName = "isPrimitiveNameHidden";
		logUtil.entry( methodName );

		// UUIDs have a max size of 36.
		if ( universallyUniqueId.length() > MAX_UUID_LENGTH ) {
			return Optional.empty();
		}

		Connection con = null;
		CallableStatement procedure = null;
		ResultSet results = null;
		final String sqlProcedureName = "GetPrimitivesToAnonymize";

		try {
			con = Common.getConnection();
			Common.beginTransaction( con );

			// Setup the SQL procedure.
			procedure = con.prepareCall( "{CALL " + sqlProcedureName + "(?)}" );
			procedure.setString( 1, universallyUniqueId );

			results = procedure.executeQuery();
			Common.endTransaction(con);
			// If the code is in the database return it otherwise return an empty Optional.
			if ( results.next() ) {
				String primitivesToAnonymize = results.getString("primitives_to_anonymize");
				if ( primitivesToAnonymize.equals( R.ANONYMIZE_ALL )) {
					return Optional.of( true );
				} else if ( primitivesToAnonymize.equals( R.ANONYMIZE_ALL_BUT_BENCH ) && primitiveType != R.BENCHMARK ) {
					return Optional.of( true );
				} else if ( primitivesToAnonymize.equals( R.ANONYMIZE_ALL_BUT_BENCH ) && primitiveType == R.BENCHMARK ) {
					return Optional.of( false );
				} else if ( primitivesToAnonymize.equals( R.ANONYMIZE_NONE )) {
					return Optional.of ( false );
				} else {
					throw new SQLException( "primitives_to_anonymize column did not have a valid result." );
				}
			} else {
				return Optional.empty();
			}
		} catch ( SQLException e ) {
			logUtil.error( methodName, "SQLException thrown while retrieving link associated with UUID=" + universallyUniqueId );
			logUtil.error( methodName, Util.getStackTrace( e ));
			Common.doRollback( con );
			throw e;
		} finally {
			Common.safeClose( con );
			Common.safeClose( procedure );
			Common.safeClose( results );
		}
	}
	*/


	/**
	 * Get the unique code that links to a given primitive from the database.
	 * @param primitiveType The type of primitive for which a code will be retrieved (solver, bench, etc.)
	 * @param primitiveId The id of the primitive for which a code will be retrieved.
	 * @param hidePrimitiveName Whether or not the link code anonymizes the primitive name.
	 * @return An Optional containing the code if present in the database otherwise an empty Optional.
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
		final String procedureName = "GetAnonymousLink";

		try {
			con = Common.getConnection();
			Common.beginTransaction( con );

			// Setup the GetAnonymousLink procedure.
			procedure = con.prepareCall( "{CALL " + procedureName + "(?, ?, ?)}" );
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
	 * @param universallyUniqueId The UUID associated with the job whose id will be retrieved.
	 * @param jobType The type of job whose id will be retrieved.
	 * @return The id of the job associated with the given UUID
	 * @author Albert Giegerich
	 */
	public static Optional<Integer> getIdOfJobAssociatedWithLink( final String universallyUniqueId ) throws SQLException {
		return getIdOfPrimitiveAssociatedWithLink( universallyUniqueId, R.JOB );
	}

	/**
	 * Gets the id of the benchmark associated with a given UUID
	 * @param universallyUniqueId The UUID associated with the benchmark whose id will be retrieved.
	 * @param benchmarkType The type of benchmark whose id will be retrieved.
	 * @return The id of the benchmark associated with the given UUID
	 * @author Albert Giegerich
	 */
	public static Optional<Integer> getIdOfBenchmarkAssociatedWithLink( final String universallyUniqueId ) throws SQLException {
		return getIdOfPrimitiveAssociatedWithLink( universallyUniqueId, R.BENCHMARK );
	}

	/**
	 * Gets the id of the solver associated with a given UUID
	 * @param universallyUniqueId The UUID associated with the solver whose id will be retrieved.
	 * @return The id of the solver associated with the given UUID
	 * @author Albert Giegerich
	 */
	public static Optional<Integer> getIdOfSolverAssociatedWithLink( final String universallyUniqueId ) throws SQLException {
		return getIdOfPrimitiveAssociatedWithLink( universallyUniqueId, R.SOLVER );
	}
	

	/**
	 * Gets the id of the primitive associated with a given UUID
	 * @param universallyUniqueId The UUID associated with the primitive whose id will be retrieved.
	 * @param primitiveType The type of primitive whose id will be retrieved.
	 * @return The id of the primitive associated with the given UUID
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
		final String procedureName = "GetIdOfPrimitiveAssociatedWithLink";

		try {
			con = Common.getConnection();
			Common.beginTransaction( con );

			// Setup the GetAnonymousLink procedure.
			procedure = con.prepareCall( "{CALL " + procedureName + "(?, ?)}" );
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

	public static Optional<PrimitivesToAnonymize> getPrimitivesToAnonymizeForSolver( String linkUuid ) throws SQLException {
		return getPrimitivesToAnonymize( linkUuid, R.SOLVER );
	}
	public static Optional<PrimitivesToAnonymize> getPrimitivesToAnonymizeForJob( String linkUuid ) throws SQLException {
		return getPrimitivesToAnonymize( linkUuid, R.JOB );
	}
	public static Optional<PrimitivesToAnonymize> getPrimitivesToAnonymizeForBenchmark( String linkUuid ) throws SQLException {
		return getPrimitivesToAnonymize( linkUuid, R.BENCHMARK );
	}

	public static Optional<PrimitivesToAnonymize> getPrimitivesToAnonymize( String linkUuid, String primitiveType ) throws SQLException {
		final String methodName = "getPrimitivesToAnonymize";
		logUtil.entry( methodName );

		// UUIDs have a max size of 36.
		if ( linkUuid.length() > MAX_UUID_LENGTH ) {
			return Optional.empty();
		}

		Connection con = null;
		CallableStatement procedure = null;
		ResultSet results = null;
		final String procedureName = "GetPrimitivesToAnonymize";

		try {
			con = Common.getConnection();

			// Setup the GetAnonymousLink procedure.
			procedure = con.prepareCall( "{CALL " + procedureName + "(?, ?)}" );
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
	 *
	 * Anonymizes the names of primitives in a list of job pairs.
	 * @param jobPairs the job pairs to anonymize.
	 * @param jobId the job id associated with the job pairs.
	 * @param stageNumber stage to filter solvers by.
	 * @param primitivesToAnonymize an enum dictating what should be anonymized.
	 * @return the anonymized job pairs.
	 * @author Albert Giegerich
	 */
	public static void anonymizeJobPairs( final List<JobPair> jobPairs, int jobId, int stageNumber, PrimitivesToAnonymize primitivesToAnonymize ) {
		final String methodName = "anonymizePrimitiveNames";
		logUtil.entry( methodName );
		// There are no primitives to anonymize
		if ( jobPairs.size() == 0 ) {
			return;
		}

		// We don't want to anonymize anything.
		if ( isNothingAnonymized( primitivesToAnonymize )) {
			return;
		}

		// Get a mapping of benchmark/solver ids to their anonymized names.
		Map<Integer, String> anonymizedBenchmarkNames = getAnonymizedBenchmarkNames( jobId );
		Map<Integer, String> anonymizedSolverNames = getAnonymizedSolverNames( jobId, stageNumber );
		for ( JobPair pair : jobPairs ) {
			if ( areBenchmarksAnonymized( primitivesToAnonymize )) {
				// Set each benchmark's name to an anonymized one.
				Benchmark pairBench = pair.getBench();
				pairBench.setName( anonymizedBenchmarkNames.get( pairBench.getId() ));
			}

			// Set each solver's name to an anonymized one.
			Solver pairSolver = pair.getStageFromNumber( stageNumber ).getSolver();
			pairSolver.setName( anonymizedSolverNames.get( pairSolver.getId() ));

			Map<Integer, String> configToNameMap = Solvers.getConfigToAnonymizedNameMap( pairSolver.getId() );
			Configuration pairConfig = pair.getStageFromNumber( stageNumber ).getConfiguration();
			pairConfig.setName( configToNameMap.get( pairConfig.getId() ));
		}
	}

	/**
	 * Anonymizes the names of solver stats deterministically based on the job and stage number.
	 * @param allSolver
	 * @author Albert Giegerich
	 */
	public static void anonymizeSolverStats( List<SolverStats> allSolverStats, int jobId, int stageNumber ) {
		Map<Integer, String> idToAnonymizedSolverName = getAnonymizedSolverNames( jobId, stageNumber );
		for ( SolverStats stats : allSolverStats ) {
			Solver solver = stats.getSolver();
			Configuration config = stats.getConfiguration();
			Map<Integer, String> configIdToName = Solvers.getConfigToAnonymizedNameMap( solver.getId() );
			config.setName( configIdToName.get( config.getId() ));

			solver.setName( idToAnonymizedSolverName.get( solver.getId() ));
		}
	}

	/**
	 * Gets a mapping of solver ID's to unique anonymized names.
	 * @param jobId The id of the job to get a mapping for.
	 * @param stageNumber Stage number to filter job pairs by.
	 * @author Albert Giegerich
	 */
	public static Map<Integer, String> getAnonymizedSolverNames( final int jobId, final int stageNumber ) {
		Job job = Jobs.getWithSimplePairs( jobId );
		List<JobPair> jobPairs = job.getJobPairs();

		Comparator<JobPair> sortJobPairBySolverId =	(firstPair, secondPair) -> {
			int firstSolverId = firstPair.getStageFromNumber( stageNumber ).getSolver().getId();
			int secondSolverId = secondPair.getStageFromNumber( stageNumber ).getSolver().getId();
			return firstSolverId - secondSolverId;
		};	

		// Filter out any pairs that don't have the specified stage
		List<JobPair> jobPairsFilteredByStage = jobPairs.stream()
				.filter( pair -> pair.hasStage( stageNumber ))
				.sorted( sortJobPairBySolverId )
				.collect( Collectors.toList() );

		Map<Integer, String> solverIdToAnonymizedName = new HashMap<>();
		int numberToAppend = 1;
		for ( JobPair pair : jobPairsFilteredByStage ) {
			int solverId =  pair.getStageFromNumber( stageNumber ).getSolver().getId();
			if ( !solverIdToAnonymizedName.containsKey( solverId )) {
				solverIdToAnonymizedName.put( solverId, "Solver" + numberToAppend ); 
				numberToAppend += 1;
			}
		}
		return solverIdToAnonymizedName;

	}

	/**
	 * Gets a mapping of bencmark ID's to unique anonymized names in a deterministic manner depending on the job id.
	 * @param jobId The id of the job to get a mapping for.
	 * @return a mapping from benchmark ID's to an anonymous name for the benchmark.
	 * @author Albert Giegerich
	 */
	public static Map<Integer, String> getAnonymizedBenchmarkNames( final int jobId ) {
		final String methodName = "getAnonymizedBenchmarkNames";
		logUtil.entry( methodName );
		Job job = Jobs.getWithSimplePairs( jobId );
		List<JobPair> jobPairs = job.getJobPairs();

		// Sort the pairs by bench id
		Collections.sort( jobPairs, (pair1, pair2) -> pair1.getBench().getId() - pair2.getBench().getId() );

		Map<Integer, String> benchmarkIdToAnonymizedName = new HashMap<>();
		int numberToAppend = 1;
		for ( JobPair pair : jobPairs ) {
			int benchId =  pair.getBench().getId();
			if ( !benchmarkIdToAnonymizedName.containsKey( benchId )) {
				benchmarkIdToAnonymizedName.put( benchId, "Benchmark" + numberToAppend ); 
				numberToAppend += 1;
			}
		}
		return benchmarkIdToAnonymizedName;
	}

	/**
	 * Gets a mapping of job space ID's to unique anonymized names.
	 * @param jobId The id of the job to get a mapping for.
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
