package org.starexec.data.database;

import org.starexec.data.security.GeneralSecurity;
import org.starexec.data.to.Website;
import org.starexec.data.to.Website.WebsiteType;
import org.starexec.logger.StarLogger;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * Handles all database interaction for user-defined websites
 */
public class Websites {
	private static final StarLogger log = StarLogger.getLogger(Websites.class);

	/**
	 * Adds a new website associated with the specified entity
	 *
	 * @param id The ID of the entity the website is associated with
	 * @param url The URL for the website
	 * @param name The display name of the website
	 * @param type Which type of entity to associate the website with (space, solver, user)
	 * @return True if the operation was a success, false otherwise
	 * @author Tyler Jensen
	 */
	public static boolean add(int id, String url, String name, WebsiteType type) {
		Connection con = null;
		CallableStatement procedure = null;

		try {
			con = Common.getConnection();
			procedure = null;

			switch (type) {
				case USER:
					procedure = con.prepareCall("{CALL AddUserWebsite(?, ?, ?)}");
					break;
				case SPACE:
					procedure = con.prepareCall("{CALL AddSpaceWebsite(?, ?, ?)}");
					break;
				case SOLVER:
					procedure = con.prepareCall("{CALL AddSolverWebsite(?, ?, ?)}");
					break;
				default:
					throw new Exception("Unhandled value for WebsiteType");
			}

			procedure.setInt(1, id);
			procedure.setString(2, url);
			procedure.setString(3, name);

			procedure.executeUpdate();
			log.info(
					String.format("Added new website of with [%s] id [%d] with name [%s] and url [%s]", type
							              .toString(), id, name, url
					));
			return true;
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		} finally {
			Common.safeClose(con);
			Common.safeClose(procedure);
		}

		return false;
	}

	/**
	 * Returns a new website with the same URL and name as the original, but where the name and URL strings have been
	 * escaped to be safe for javascript insertion
	 *
	 * @param site The site to process
	 * @return A new, identical website with javascript-safe attributes
	 */
	public static Website processWebsiteForJavaScript(Website site) {

		Website newSite = new Website();
		newSite.setId(site.getId());
		newSite.setUrl(GeneralSecurity.getJavascriptSafeString(site.getUrl()));
		newSite.setName(GeneralSecurity.getJavascriptSafeString(site.getName()));
		return newSite;
	}

	/**
	 * Returns a new website with the same URL and name as the original, but where the name and URL strings have been
	 * escaped to be safe for HTML insertion
	 *
	 * @param site The site to process
	 * @return A new, identical website with javascript-safe attributes
	 */
	public static Website processWebsiteForHTML(Website site) {
		Website newSite = new Website();
		newSite.setId(site.getId());
		newSite.setUrl(GeneralSecurity.getHTMLAttributeSafeString(site.getUrl()));
		newSite.setName(GeneralSecurity.getHTMLSafeString(site.getName()));
		return newSite;
	}

	/**
	 * Deletes the website associated with the given website ID.
	 *
	 * @param websiteId the ID of the website to delete
	 * @return True if the operation was a success, false otherwise
	 * @author Tyler Jensen
	 */
	public static boolean delete(int websiteId) {
		Connection con = null;
		CallableStatement procedure = null;
		try {
			con = Common.getConnection();
			procedure = con.prepareCall("{CALL DeleteWebsite(?)}");


			procedure.setInt(1, websiteId);

			procedure.executeUpdate();
			return true;
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		} finally {
			Common.safeClose(con);
			Common.safeClose(procedure);
		}

		return false;
	}

	/**
	 * Gets all the websites for the given entity, processed so that they are suitable for insertion into HTML code
	 * without risking a cross site scripting attack
	 *
	 * @param id
	 * @param webType
	 * @return The list of website, where strings have been HTML escaped
	 */
	public static List<Website> getAllForHTML(int id, WebsiteType webType) {
		List<Website> sites = getAll(id, webType);
		List<Website> answer = new ArrayList<>();

		for (Website s : sites) {
			answer.add(processWebsiteForHTML(s));
		}
		return answer;
	}

	/**
	 * Gets all websites for the given entity, where escaping has been done so that strings like the website name / url
	 * have been escaped such that they can be safely embedded into Javascript.
	 *
	 * @param id
	 * @param webType
	 * @return The list of websites, where strings have been Javascript escaped
	 */

	public static List<Website> getAllForJavascript(int id, WebsiteType webType) {
		List<Website> sites = getAll(id, webType);
		List<Website> answer = new ArrayList<>();

		for (Website s : sites) {
			answer.add(processWebsiteForJavaScript(s));
		}
		return answer;
	}

	/**
	 * Returns the website with the given primary ID
	 *
	 * @param websiteId
	 * @return The website, or null on error
	 */
	public static Website getWebsite(int websiteId) {
		Connection con = null;
		CallableStatement procedure = null;
		ResultSet results = null;
		try {
			con = Common.getConnection();
			procedure = con.prepareCall("{CALL GetWebsiteById(?)}");
			procedure.setInt(1, websiteId);
			results = procedure.executeQuery();
			if (results.next()) {
				return resultToWebsite(results);
			}
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		} finally {
			Common.safeClose(con);
			Common.safeClose(procedure);
			Common.safeClose(results);
		}
		return null;
	}

	private static Website resultToWebsite(ResultSet results) {
		try {
			Website w = new Website();
			w.setId(results.getInt("id"));
			w.setName(results.getString("name"));
			w.setUrl(results.getString("url"));
			if (results.getInt("solver_id") > 0) {
				w.setPrimId(results.getInt("solver_id"));
				w.setType(WebsiteType.SOLVER);
			} else if (results.getInt("user_id") > 0) {
				w.setPrimId(results.getInt("user_id"));
				w.setType(WebsiteType.USER);
			} else if (results.getInt("space_id") > 0) {
				w.setPrimId(results.getInt("space_id"));
				w.setType(WebsiteType.SPACE);
			}
			return w;
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}
		return null;
	}

	/**
	 * Returns a list of websites associated with the given entity based on its type
	 *
	 * @param id The id of the entity to get websites for
	 * @param webType The type of entity to get websites for (solver, user or space) are safe for insertion into
	 * javascript
	 * @return A list of websites associated with the entity
	 * @author Tyler Jensen
	 */
	public static List<Website> getAll(int id, WebsiteType webType) {
		Connection con = null;
		CallableStatement procedure = null;
		ResultSet results = null;
		try {
			con = Common.getConnection();

			switch (webType) {
				case USER:
					procedure = con.prepareCall("{CALL GetWebsitesByUserId(?)}");
					break;
				case SPACE:
					procedure = con.prepareCall("{CALL GetWebsitesBySpaceId(?)}");
					break;
				case SOLVER:
					procedure = con.prepareCall("{CALL GetWebsitesBySolverId(?)}");
					break;
				default:
					throw new Exception("Unhandled value for WebsiteType");
			}

			procedure.setInt(1, id);

			results = procedure.executeQuery();
			List<Website> websites = new LinkedList<>();

			while (results.next()) {
				Website w = resultToWebsite(results);

				websites.add(w);
			}

			return websites;
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		} finally {
			Common.safeClose(con);
			Common.safeClose(procedure);
			Common.safeClose(results);
		}

		return null;
	}
}
