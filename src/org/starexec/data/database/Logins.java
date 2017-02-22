package org.starexec.data.database;

import org.starexec.logger.StarLogger;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.ResultSet;


/**
 * Class for accessing the logins table.
 */
public class Logins {

	private static final StarLogger log = StarLogger.getLogger(Logins.class);

	/**
	 * Gets the number of unique user logins in the logins table.
	 * @return number of unique user logins, null if an Exception occurs.
	 * @author Albert Giegerich
	 */
	public static Integer getNumberOfUniqueLogins() {
		Connection con = null;
		CallableStatement procedure = null;
		ResultSet results = null;

		try {
			con = Common.getConnection();
			procedure = con.prepareCall("{CALL GetNumberOfUniqueLogins()}");
			results = procedure.executeQuery();

			results.first();
			int numberOfUniqueLogins = results.getInt(1);

			return numberOfUniqueLogins;
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		} finally {
			Common.safeClose(con);
			Common.safeClose(procedure);
			Common.safeClose(results);
		}
		return null;
	}

	/**
	 * Clears all data in the logins table.
	 * @author Albert Giegerich
	 */
	public static void resetLogins() {
		Connection con = null;
		CallableStatement procedure = null;

		try {
			con = Common.getConnection();
			procedure = con.prepareCall("{CALL ResetLogins()}");
			procedure.executeQuery();
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		} finally {
			Common.safeClose(con);
			Common.safeClose(procedure);
		}
	}
}
