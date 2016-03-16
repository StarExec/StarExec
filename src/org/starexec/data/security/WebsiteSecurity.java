package org.starexec.data.security;

import org.starexec.constants.R;
import org.starexec.data.database.Permissions;
import org.starexec.data.database.Solvers;
import org.starexec.data.database.Users;
import org.starexec.data.database.Websites;
import org.starexec.data.to.Permission;
import org.starexec.data.to.Website;
import org.starexec.data.to.Website.WebsiteType;
import org.starexec.util.Validator;

/**
 * This class contains functions for deciding whether users can add, view, and delete websites.
 * @author Eric
 */
public class WebsiteSecurity {

	/**
	 * Determines whether a user can add a website to some primitive
	 * @param primId The ID of the primitive 
	 * @param type The type of the primitive (solver, user, space)
	 * @param userId ID of the user making the request
	 * @param name The name to give the new site
	 * @param URL The URL of the new site
	 * @return A ValidatorStatusCode object
	 */
	public static ValidatorStatusCode canUserAddWebsite(int primId, String type, int userId, String name, String URL) {
		if (!Validator.isValidWebsiteName(name) ) {
			return new ValidatorStatusCode(false, "The website name is not formatted correctly. Please refer to the help pages to see the correct format");
		}
		
		if (!Validator.isValidWebsite(URL)) {
			return new ValidatorStatusCode(false, "The website url is not formatted correctly. Please refer to the help pages to see the correct format");
		}
		if (type==R.SOLVER) {
			if (!SolverSecurity.userOwnsSolverOrIsAdmin(primId, userId)) {
				return new ValidatorStatusCode(false, "You do not have permission to add a website to this solver");
			}
		} else if (type==R.SPACE) {
			Permission p=Permissions.get(userId, primId);
			if (p==null || !p.isLeader()) {
				return new ValidatorStatusCode(false, "You do not have permission to associate websites with this space");
			}
		} else if (type==R.USER) {
			boolean visitingUserIsOwner = (primId == userId);
			if (!(visitingUserIsOwner || GeneralSecurity.hasAdminWritePrivileges(userId))) {
				return new ValidatorStatusCode(false, "You do not have permission to add a website here.");
			}
			if (Users.isPublicUser(primId)) {
				return new ValidatorStatusCode(false, "The guest user profile cannot be updated");
			}
		} else {
			return new ValidatorStatusCode(false, "The given type is not valid");
		}
		return new ValidatorStatusCode(true);
		
	}
	/**
	 * Determines whether a user can delete some website
	 * @param websiteId The ID of the site to delete
	 * @param userId The ID of the user making the request
	 * @return A ValidatorStatusCode object
	 */
	public static ValidatorStatusCode canUserDeleteWebsite(int websiteId, int userId) {
		Website w = Websites.getWebsite(websiteId);
		if (w==null) {
			return new ValidatorStatusCode(false, "The given website could not be found");
		}
		if (w.getType()==WebsiteType.USER) {
			return canDeleteUserWebsite(w, userId);
		} else if (w.getType()==WebsiteType.SOLVER) {
			return canDeleteSolverWebsite(w,userId);
		} else {
			return canDeleteSpaceWebsite(w,userId);
		}
	}
	
	/**
	 * Checks to see whether the given user is allowed to delete a website associated with a user
	 * @param site A website object, which should be of user type and have its prim id set
	 * @param userId The ID of the user who is making the request
	 * @return new ValidatorStatusCode(true) if the operation is allowed and a status code from ValidatorStatusCodes otherwise
	 */
	private static ValidatorStatusCode canDeleteUserWebsite(Website site, int userId){
		if (Users.isPublicUser(site.getPrimId())) {
			return new ValidatorStatusCode(false, "The guest user profile cannot be updated");
		}
		if (!GeneralSecurity.hasAdminWritePrivileges(userId) && userId!=site.getPrimId()) {
			return new ValidatorStatusCode(false, "You do not have permission to alter another user's websites");
		}
		
		return new ValidatorStatusCode(true);
	}
	
	/**
	 * Checks to see whether the given user is allowed to delete a website associated with a solver
	 * @param site A website object, which should be of solver type and have its prim id set
	 * @param userId The ID of the user making the request
	 * @return new ValidatorStatusCode(true) if the operation is allowed and a status code from ValidatorStatusCodes otherwise
	 */
	private static ValidatorStatusCode canDeleteSolverWebsite(Website site,int userId){
		int solverId = site.getPrimId();
		if (!SolverSecurity.userOwnsSolverOrIsAdmin(Solvers.get(solverId),userId)) {
			return new ValidatorStatusCode(false, "You do not have permission to delete websites from this solver");
		}
		return new ValidatorStatusCode(true);
	}
	
	/**
	 * Checks to see whether the given user is allowed to delete a website associated with a space
	 * @param site The ID of the website to be deleted
	 * @param userId The ID of the user making the request
	 * @return new ValidatorStatusCode(true) if the operation is allowed and a status code from ValidatorStatusCodes otherwise
	 */
	private static ValidatorStatusCode canDeleteSpaceWebsite(Website site,int userId){
		int spaceId = site.getPrimId();
		Permission p=Permissions.get(userId, spaceId);
		if (p==null || !p.isLeader()) {
			return new ValidatorStatusCode(false, "You do not have permission to delete websites associated with this space");
		}
		
		return new ValidatorStatusCode(true);
	}
	
}
