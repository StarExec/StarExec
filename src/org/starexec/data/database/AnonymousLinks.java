package org.starexec.data.database;


import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;

import java.util.Optional;
import java.util.UUID;

import org.apache.log4j.Logger;

import org.starexec.util.LogUtil;
import org.starexec.constants.R;

public class AnonymousLinks {
	private static final Logger log = Logger.getLogger(AnonymousLinks.class);			
	private static final LogUtil logUtil = new LogUtil(log);

	private static final int MAX_UUID_LENGTH = 36;

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
			final boolean hidePrimitiveName ) throws SQLException {

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
			procedure.setBoolean( 4, hidePrimitiveName );

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
	 */
	public static Optional<Boolean> isJobNameHidden( final String universallyUniqueId ) throws SQLException {
		return isPrimitiveNameHidden( universallyUniqueId, R.JOB );
	}

	/**
	 * Checks whether the benchmark name for a link should be hidden.
	 * @param universallyUniqueId The uuid associated with the primitive
	 * @return An optional containing the result if the link exists otherwise an empty optional.
	 * @author Albert Giegerich
	 */
	public static Optional<Boolean> isBenchmarkNameHidden( final String universallyUniqueId ) throws SQLException {
		return isPrimitiveNameHidden( universallyUniqueId, R.BENCHMARK );
	}

	/**
	 * Checks whether the solver name for a link should be hidden.
	 * @param universallyUniqueId The uuid associated with the primitive
	 * @return An optional containing the result if the link exists otherwise an empty optional.
	 * @author Albert Giegerich
	 */
	public static Optional<Boolean> isSolverNameHidden( final String universallyUniqueId ) throws SQLException {
		return isPrimitiveNameHidden( universallyUniqueId, R.SOLVER );
	}

	/**
	 * Checks whether the primitive name for a link should be hidden.
	 * @param universallyUniqueId The uuid associated with the primitive
	 * @author Albert Giegerich
	 */
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
		final String sqlProcedureName = "IsPrimitiveNameHidden";

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
				return Optional.of( results.getBoolean("hide_primitive_name") );
			} else {
				return Optional.empty();
			}
		} catch ( SQLException e ) {
			logUtil.error( methodName, "SQLException thrown while retrieving link associated with UUID=" + universallyUniqueId );
			Common.doRollback( con );
			throw e;
		} finally {
			Common.safeClose( con );
			Common.safeClose( procedure );
			Common.safeClose( results );
		}
	}


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
			final boolean hidePrimitiveName ) throws SQLException {

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
			procedure.setBoolean( 3, hidePrimitiveName );

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

	//public static void deleteOldLinks
}
