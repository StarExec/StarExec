package org.starexec.data.database;


import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;

import java.util.Optional;

import org.apache.log4j.Logger;

import org.starexec.util.LogUtil;

public class AnonymousLinks {
	private static final Logger log = Logger.getLogger(AnonymousLinks.class);			
	private static final LogUtil logUtil = new LogUtil(log);

	/**
	 * Adds a new anonymous link entry to the database.
	 * @param universallyUniqueId A string that will uniquely identify the benchmark.
	 * @param primitiveId The id of the primitive for which a public anonymous link will be generated.
	 * @param primitiveType The type of primitive (benchmark, solver, etc.) that a link will be generated for.
	 * @param hidePrimitiveName Whether or not the primitive's name should be hidden on the link page.
	 * @author Albert Giegerich
	 */
	public static void addAnonymousLink(
			final String universallyUniqueId,
			final String primitiveType,
			final int primitiveId,
			final boolean hidePrimitiveName ) throws SQLException {

		final String methodName = "addAnonymousLink";
		logUtil.entry(methodName);

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

		} catch (SQLException e) { 
			logUtil.error( methodName, "Threw an exception while adding anonymous link for " + primitiveType + " with id=" + primitiveId +
				"\nError message: " + e.getMessage() );

			throw e;
		} finally {
			Common.safeClose(con);
			Common.safeClose(procedure);
		}
		log.debug( "Finished adding anonymous link for benchmark with id=" + primitiveId );
	}

	/**
	 * Checks whether the primitive name for a link should be hidden.
	 * @param universallyUniqueId The uuid associated with the primitive
	 * @author Albert Giegerich
	 */
	public static Optional<Boolean> isPrimitiveNameHidden( final String universallyUniqueId ) throws SQLException {
		final String methodName = "isPrimitiveNameHidden";
		logUtil.entry( methodName );

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
			// If the code is in the database return it otherwise return an empty Optional.
			if ( results.next() ) {
				return Optional.of( results.getBoolean("hide_primitive_name") );
			} else {
				return Optional.empty();
			}
		} catch ( SQLException e ) {
			logUtil.error( methodName, "SQLException thrown while retrieving link associated with UUID=" + universallyUniqueId );
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
			// If the code is in the database return it otherwise return an empty Optional.
			if ( results.next() ) {
				return Optional.of( results.getString("unique_id") );
			} else {
				return Optional.empty();
			}
		} catch (SQLException e) {
			log.error( "Caught an exception while trying to get an anonymous link from database.", e );
			throw e;
		} finally {
			Common.safeClose(con);
			Common.safeClose(procedure);
			Common.safeClose(results);
		}
	}

	/**
	 * Gets the id of the primitive associated with a given UUID
	 * @param universallyUniqueId The UUID associated with the primitive whose id will be retrieved.
	 * @param primitiveType The type of primitive whose id will be retrieved.
	 * @return The id of the primitive associated with the given UUID
	 * @author Albert Giegerich
	 */
	public static Optional<Integer> getIdOfPrimitiveAssociatedWithLink( final String universallyUniqueId ) throws SQLException {

		final String methodName = "getIdOfPrimitiveAssociatedWithCode";
		logUtil.entry( methodName );

		Connection con = null;
		CallableStatement procedure = null;
		ResultSet results = null;
		final String procedureName = "GetIdOfPrimitiveAssociatedWithLink";

		try {
			con = Common.getConnection();
			Common.beginTransaction( con );

			// Setup the GetAnonymousLink procedure.
			procedure = con.prepareCall( "{CALL " + procedureName + "(?)}" );
			procedure.setString( 1, universallyUniqueId );

			results = procedure.executeQuery();
			// If the code is in the database return it otherwise return an empty Optional.
			if ( results.next() ) {
				return Optional.of( results.getInt("primitive_id") );
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
}
