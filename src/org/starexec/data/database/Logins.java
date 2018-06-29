package org.starexec.data.database;

import org.starexec.logger.StarLogger;

/**
 * Class for accessing the logins table.
 */
public class Logins {

	private static final StarLogger log = StarLogger.getLogger(Logins.class);

	/**
	 * Gets the number of unique user logins in the logins table.
	 *
	 * @return number of unique user logins, null if an Exception occurs.
	 * @author Albert Giegerich
	 */
	public static Integer getNumberOfUniqueLogins() {
		try {
			Common.query(
					"{CALL GetNumberOfUniqueLogins()}",
					p -> {},
					results -> {
						results.first();
						return results.getInt(1);
					}
			);
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}
		return null;
	}

	/**
	 * Clears all data in the logins table.
	 *
	 * @author Albert Giegerich
	 */
	public static void resetLogins() {
		try {
			Common.update("{CALL ResetLogins()}", p -> {});
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}
	}
}
