package org.starexec.util;

import java.util.HashMap;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.apache.log4j.Logger;
import org.starexec.constants.P;
import org.starexec.data.Database;
import org.starexec.data.to.*;

/**
 * Contains handy methods for accessing data within a user's session
 * @author Tyler Jensen
 */
public class SessionUtil {
	private static final Logger log = Logger.getLogger(SessionUtil.class);
	
	/**
	 * @param request The request to retrieve the user object from
	 * @return The user object representing the currently logged in user
	 */
	public static User getUser(HttpServletRequest request) {
		return (User)request.getSession().getAttribute(P.SESSION_USER);
	}
	
	/**
	 * @param session The session to retrieve the user object from
	 * @return The user object representing the currently logged in user
	 */
	public static User getUser(HttpSession session) {
		return (User)session.getAttribute(P.SESSION_USER);
	}	
	
	/**
	 * @param request The request to get the user's id from
	 * @return The current user's id
	 */
	public static long getUserId(HttpServletRequest request) {
		 return SessionUtil.getUserId(request.getSession());
	}
	
	/**
	 * @param session The session to get the user's id from
	 * @return The current user's id
	 */
	public static long getUserId(HttpSession session) {
		 return ((User)session.getAttribute(P.SESSION_USER)).getId();
	}
	
	/**
	 * @param request The request to get the user's cache from
	 * @return The current user's permission cache
	 */
	public static HashMap<Long, Permission> getPermissionCache(HttpServletRequest request) {
		return SessionUtil.getPermissionCache(request.getSession());
	}
		
	/**
	 * @param session The session to get the user's cache from
	 * @return The current user's permission cache
	 */
	@SuppressWarnings("unchecked")
	public static HashMap<Long, Permission> getPermissionCache(HttpSession session) {
		return (HashMap<Long, Permission>)session.getAttribute(P.PERMISSION_CACHE);
	}
	
	/**
	 * @param request The request to get the permission from
	 * @param spaceId The space to get the current user's permissions for
	 * @return The permission associated with the given space
	 */
	public static Permission getPermission(HttpServletRequest request, long spaceId) {
		return SessionUtil.getPermission(request.getSession(), spaceId);
	}
	
	/**
	 * @param session The session to get the permission from
	 * @param spaceId The space to get the current user's permissions for
	 * @return The permission associated with the given space
	 */
	public static Permission getPermission(HttpSession session, long spaceId) {
		HashMap<Long, Permission> cache = SessionUtil.getPermissionCache(session);
		
		// If the cache doesn't contain the requested permission...
		if(!cache.containsKey(spaceId)) {
			// Then cache it
			SessionUtil.cachePermission(session, spaceId);
		}
		
		if(cache.containsKey(spaceId)) {
			// If the cache was successful and it was added, return the permission
			return cache.get(spaceId);
		}
		
		// Return null if the cache couldn't add it, or it doesn't exist
		return null;
	}
	
	/**
	 * Looks up the user's permissions on the given space from the database and adds it
	 * to the user's permission cache
	 * @param request The request where the cache is located
	 * @param spaceId The id of the space to cache the permission for
	 */
	public static void cachePermission(HttpServletRequest request, long spaceId) {
		SessionUtil.cachePermission(request.getSession(), spaceId);
	}
	
	/**
	 * Looks up the user's permissions on the given space from the database and adds it
	 * to the user's permission cache
	 * @param session The session where the cache is located
	 * @param spaceId The id of the space to cache the permission for
	 */
	public static void cachePermission(HttpSession session, long spaceId) {
		// Make sure we cache the permission for this space for the user
		HashMap<Long, Permission> cache = SessionUtil.getPermissionCache(session);
		if(!cache.containsKey(spaceId)) {
			// If the cache does not have the permission, add it
			Permission p = Database.getUserPermissions(SessionUtil.getUserId(session), spaceId);
			
			if(p != null) {
				cache.put(spaceId, p);
			}
		}
	}
}
