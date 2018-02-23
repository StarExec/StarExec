package org.starexec.util;

import org.starexec.constants.R;
import org.starexec.data.database.Permissions;
import org.starexec.data.database.Spaces;
import org.starexec.data.database.Users;
import org.starexec.data.to.Permission;
import org.starexec.data.to.User;
import org.starexec.logger.StarLogger;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.util.HashMap;

/**
 * Contains handy methods for accessing data within a user's session
 * @author Tyler Jensen
 */

public class SessionUtil {	
	private static final StarLogger log = StarLogger.getLogger(SessionUtil.class);
	public static final String USER = "user";	// The string we store the user's User object under
	public static final String PERMISSION_CACHE = "perm";	// The string we store the user's permission cache object under
	private static User publicUser = null;
	/**
	 * @param request The request to retrieve the user object from
	 * @return The user object representing the currently logged in user
	 */
	public static User getUser(HttpServletRequest request) {
		final String methodName = "getUser";
		Object user = request.getSession().getAttribute(SessionUtil.USER);
		if (user==null) {
			log.debug(methodName, "User was null.");
			if (publicUser==null) {
				log.debug(methodName, "public user was null.");
				publicUser = Users.get(R.PUBLIC_USER_ID);
			}
			return publicUser;
		}
		User castUser = (User)user;
		log.debug("Returning user with id="+castUser.getId()+" email="+((User) user).getEmail());
		return castUser;
	}
	
	/**
	 * @param request The request to get the user's id from
	 * @return The current user's id
	 */
	public static int getUserId(HttpServletRequest request) {
		 return SessionUtil.getUserId(request.getSession());
	}
	
	/**
	 * @param session The session to get the user's id from
	 * @return The current user's id
	 */
	private static int getUserId(HttpSession session) {
		if (session.getAttribute(SessionUtil.USER)==null) {
			return R.PUBLIC_USER_ID;
		}
		 return ((User)session.getAttribute(SessionUtil.USER)).getId();
	}
	
	/**
	 * @param request The request to get the user's cache from
	 * @return The current user's permission cache
	 */
	public static HashMap<Integer, Permission> getPermissionCache(HttpServletRequest request) {
		return SessionUtil.getPermissionCache(request.getSession());
	}
		
	/**
	 * @param session The session to get the user's cache from
	 * @return The current user's permission cache
	 */
	@SuppressWarnings("unchecked")
	public static HashMap<Integer, Permission> getPermissionCache(HttpSession session) {
		if (session.getAttribute(SessionUtil.PERMISSION_CACHE)==null) {
			return new HashMap<>();
		}
		return (HashMap<Integer, Permission>)session.getAttribute(SessionUtil.PERMISSION_CACHE);
	}
	
	/**
	 * @param request The request to get the permission from
	 * @param spaceId The space to get the current user's permissions for
	 * @return The permission associated with the given space
	 */
	public static Permission getPermission(HttpServletRequest request, int spaceId) {
		return SessionUtil.getPermission(request.getSession(), spaceId);
	}
	
	
	/**
	 * @param session The session to get the permission from
	 * @param spaceId The space to get the current user's permissions for
	 * @return The permission associated with the given space
	 */
	private static Permission getPermission(HttpSession session, int spaceId) {
		HashMap<Integer, Permission> cache = SessionUtil.getPermissionCache(session);
		

		// If the cache doesn't contain the requested permission...
		if(!cache.containsKey(spaceId)) {
			// Then cache it
			SessionUtil.cachePermission(session, spaceId);
		}
		
		if(cache.containsKey(spaceId)) {
			// If the cache was successful and it was added, return the permission
			return cache.get(spaceId);
		}

		//if the cache couldn't add it, or it doesn't exist, but the space is public
		if (Spaces.isPublicSpace(spaceId)){
			log.debug("Returning public users permissions");
			return Permissions.getEmptyPermission();
		}
		
		
		// Return null if the cache couldn't add it and space is private, or it doesn't exist
		return null;
	}

	/**
	 * Looks up the user's permissions on the given space from the database and adds it
	 * to the user's permission cache
	 * @param session The session where the cache is located
	 * @param spaceId The id of the space to cache the permission for
	 */
	private static void cachePermission(HttpSession session, int spaceId) {
		// Make sure we cache the permission for this space for the user
		HashMap<Integer, Permission> cache = SessionUtil.getPermissionCache(session);
		if(!cache.containsKey(spaceId)) {
			// If the cache does not have the permission, add it
			Permission p = Permissions.get(SessionUtil.getUserId(session), spaceId);
			
			if(p != null) {
				cache.put(spaceId, p);
			}
		}
	}
	
	/**
	 * Removes a user's permission for a given space from cache; this prevents
	 * the cached permissions and the actual permissions from becoming desynchronized 
	 *  
	 * @param request the session where the cache is located
	 * @param spaceId the id of the space to remove permissions from cache for
	 * @author Todd Elvers
	 */
	public static void removeCachePermission(HttpServletRequest request, int spaceId){
		HashMap<Integer, Permission> cache = SessionUtil.getPermissionCache(request.getSession());
		cache.remove(spaceId);
	}
}
