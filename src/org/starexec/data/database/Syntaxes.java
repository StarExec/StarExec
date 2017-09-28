package org.starexec.data.database;

import org.starexec.data.to.Syntax;
import org.starexec.logger.StarLogger;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collections;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * Handles all database interaction for bench, pre and post processors
 */
public class Syntaxes {
	private static final StarLogger log = StarLogger.getLogger(Syntaxes.class);
	private static Map<Integer, Syntax> all = null;

	/**
	 * @param id of a particular Syntax
	 * @return Syntax represented by id, or default Syntax
	 */
	public static Syntax get(int id) {
		setAll();
		Syntax s = all.get(id);
		if (s == null) {
			log.warn("get", "Could not find Syntax: " + id);
			s = all.get(1);
		}
		return s;
	}

	/**
	 * @return an unmodifiable Collection of all Syntaxes
	 */
	public static Collection<Syntax> getAll() {
		setAll();
		return Collections.unmodifiableCollection(all.values());
	}

	/**
	 * Given a result set where the current row points to a processor, return the processor
	 *
	 * @param results
	 * @return The processor if it exists
	 * @throws SQLException If the ResultSet does not contain a required processor attribute
	 */
	public static Syntax resultSetToSyntax(ResultSet results) throws SQLException {
		int id = results.getInt("id");
		String name = results.getString("name");
		String classname = results.getString("class");
		String js = results.getString("js");
		return new Syntax(id, name, classname, js);
	}

	private static void setAll() {
		if (all == null) {
			all = new HashMap<>();
			try {
				Common.query(
					"{CALL GetAllSyntaxes()}",
					p -> {},
					results -> {
						while (results.next()) {
							Syntax s = resultSetToSyntax(results);
							all.put(s.getId(), s);
						}
						return null;
					}
				);
			} catch (SQLException e) {
				log.error("setAll", "Error loading syntaxes", e);
				all = Collections.emptyMap();
			}
		}
	}
}
