package org.starexec.data.database;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.apache.log4j.Logger;
import org.owasp.esapi.ESAPI;
import org.starexec.data.security.GeneralSecurity;
import org.starexec.data.to.Website;
import org.starexec.util.Validator;

/**
 * Handles all database interaction for user-defined websites
 */
public class Websites {
	public static enum WebsiteType { USER, SOLVER, SPACE }
	private static final Logger log = Logger.getLogger(Websites.class);;
	
	/**
	 * Adds a new website associated with the specified entity
	 * @param id The ID of the entity the website is associated with
	 * @param url The URL for the website
	 * @param name The display name of the website
	 * @param type Which type of entity to associate the website with (space, solver, user)
	 * @return True if the operation was a success, false otherwise
	 * @author Tyler Jensen
	 */
	public static boolean add(int id, String url, String name, WebsiteType type) {
		Connection con = null;			
		CallableStatement procedure= null;
		
		try {
			con = Common.getConnection();		
			 procedure = null;
			
			switch(type) {
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
			log.info(String.format("Added new website of with [%s] id [%d] with name [%s] and url [%s]", type.toString(), id, name, url));
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
	 * Returns a new website with the same URL and name as the original, but where
	 * the name and URL strings have been escaped to be safe for javascript insertion
	 * @param site The site to process
	 * @return A new, identical website with javascript-safe attributes
	 */
	public static Website processWebsiteForJavaScript(Website site) {
		
		Website newSite=new Website();
		newSite.setId(site.getId());
		newSite.setUrl(GeneralSecurity.getJavascriptSafeString(site.getUrl()));
		newSite.setName(GeneralSecurity.getJavascriptSafeString(site.getName()));
		return newSite;
	}
	
	/**
	 * Returns a new website with the same URL and name as the original, but where
	 * the name and URL strings have been escaped to be safe for HTML insertion
	 * @param site The site to process
	 * @return A new, identical website with javascript-safe attributes
	 */
	public static Website processWebsiteForHTML(Website site) {
			Website newSite=new Website();
			newSite.setId(site.getId());
			newSite.setUrl(GeneralSecurity.getHTMLAttributeSafeString(site.getUrl()));
			newSite.setName(GeneralSecurity.getHTMLSafeString(site.getName()));
			return newSite;

	}
	
	/**
	 * Deletes the website associated with the given website ID.
	 * @param websiteId the ID of the website to delete
	 * @param entityId the ID of the entity that the website belongs to (user, solver, space)
	 * @param webType the type of entity the website belongs to
	 * @return True if the operation was a success, false otherwise
	 * @author Tyler Jensen
	 */
	public static boolean delete(int websiteId, int entityId, WebsiteType webType) {
		Connection con = null;			
		CallableStatement procedure= null;
		try {
			con = Common.getConnection(); 			
			
			switch(webType) {
				case USER:
					procedure = con.prepareCall("{CALL DeleteUserWebsite(?, ?)}");
					break;
				case SPACE:
					procedure = con.prepareCall("{CALL DeleteSpaceWebsite(?, ?)}");
					break;
				case SOLVER:
					procedure = con.prepareCall("{CALL DeleteSolverWebsite(?, ?)}");
					break;
				default:
					throw new Exception("Unhandled value for WebsiteType");
			}
			
			procedure.setInt(1, websiteId);
			procedure.setInt(2, entityId);
			
			procedure.executeUpdate();					
			log.info(String.format("Website [%d] deleted from entity [%d]", websiteId, entityId));
			return true;			
		} catch (Exception e){			
			log.error(e.getMessage(), e);		
		} finally {
			Common.safeClose(con);
			Common.safeClose(procedure);
		}
		
		return false;
	}	
	
	public static List<Website> getAllForHTML(int id, WebsiteType webType) {
		List<Website> sites=getAll(id,webType);
		List<Website> answer=new ArrayList<Website>();
		
		for (Website s : sites) {
			answer.add(processWebsiteForHTML(s));
		}
		return answer;
	}
	
	public static List<Website> getAllForJavascript(int id, WebsiteType webType) {
		List<Website> sites=getAll(id,webType);
		List<Website> answer=new ArrayList<Website>();
		
		for (Website s : sites) {
			answer.add(processWebsiteForJavaScript(s));
		}
		return answer;
	}
	
	
	/**
	 * Returns a list of websites associated with the given entity based on its type
	 * @param id The id of the entity to get websites for
	 * @param webType The type of entity to get websites for (solver, user or space)
	 * @param javascriptSafe If true, websites with their attributes escaped such that they 
	 * are safe for insertion into javascript
	 * @return A list of websites associated with the entity
	 * @author Tyler Jensen
	 */
	public static List<Website> getAll(int id, WebsiteType webType) {
		Connection con = null;
		CallableStatement procedure= null;
		ResultSet results=null;
		try {
			con = Common.getConnection();
			
			switch(webType) {
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
			List<Website> websites = new LinkedList<Website>();
			
			while (results.next()) {
				Website w = new Website();
				w.setId(results.getInt("id"));
				w.setName(results.getString("name"));
				w.setUrl(results.getString("url"));
				websites.add(w);				

				
			}
			
			return websites;
			
		} catch (Exception e){			
			log.error(e.getMessage(), e);		
		} finally {
			Common.safeClose(con);
			Common.safeClose(procedure);
			Common.safeClose(results);
		}
		
		return null;
	}	
}