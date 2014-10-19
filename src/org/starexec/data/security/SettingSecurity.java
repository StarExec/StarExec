package org.starexec.data.security;

import org.starexec.data.database.Permissions;
import org.starexec.data.database.Settings;
import org.starexec.data.database.Spaces;
import org.starexec.data.database.Users;
import org.starexec.data.to.DefaultSettings;
import org.starexec.data.to.DefaultSettings.SettingType;
import org.starexec.data.to.Permission;
import org.starexec.data.to.Space;
import org.starexec.util.Validator;

public class SettingSecurity {
	/**
	 * Checks whether a user can update the default settings (default timeouts, max-memory, etc.) of a 
	 * community.
	 * @param spaceId The ID of the space that would have its settings changed
	 * @param attribute The name of the setting being changed
	 * @param newValue The new value that would be given to the setting
	 * @param userId The ID of the user making the request
	 * @return 0 if the operation is allowed and a status code from ValidatorStatusCodes otherwise
	 */
	
	//TODO: Consider how to handle where to use the Validator class
	public static ValidatorStatusCode canUpdateSettings(int id, String attribute, String newValue, int userId) {
		DefaultSettings d=Settings.getProfileById(id);
		if (d==null) {
			return new ValidatorStatusCode(false, "The given setting profile could not be found");
		}
		if (d.getType()==SettingType.USER) {
			if (id!=userId && !Users.isAdmin(userId)) {
				return new ValidatorStatusCode(false, "You may not update default setting profiles of other users");
			}
		} else {
			Permission perm = Permissions.get(userId, id);		
			if(perm == null || !perm.isLeader()) {
				return new ValidatorStatusCode(false, "Only leaders can update default settings in a space");
			}
		}
				
		if (attribute.equals("CpuTimeout") || attribute.equals("ClockTimeout")) {
			if (! Validator.isValidInteger(newValue)) {
				return new ValidatorStatusCode(false, "The new limit needs to be a valid integer");
			}
			int timeout=Integer.parseInt(newValue);
			if (timeout<=0) {
				return new ValidatorStatusCode(false, "The new limit needs to be greater than 0");
			}
		} else if (attribute.equals("MaxMem")) {
			if (!Validator.isValidDouble(newValue)) {
				return new ValidatorStatusCode(false, "The new limit needs to be a valid double");
			}
			
			double limit=Double.parseDouble(newValue);
			if (limit<=0) {
				return new ValidatorStatusCode(false, "The new limit needs to be greater than 0");
			}
		}
		
		return new ValidatorStatusCode(true);
	}
}
