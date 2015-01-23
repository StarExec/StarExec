package org.starexec.data.security;

import org.starexec.data.database.Permissions;
import org.starexec.data.database.Processors;
import org.starexec.data.database.Settings;
import org.starexec.data.database.Spaces;
import org.starexec.data.database.Users;
import org.starexec.data.to.DefaultSettings;
import org.starexec.data.to.DefaultSettings.SettingType;
import org.starexec.data.to.Permission;
import org.starexec.data.to.Processor;
import org.starexec.data.to.Processor.ProcessorType;
import org.starexec.data.to.Space;
import org.starexec.util.Util;
import org.starexec.util.Validator;

public class SettingSecurity {
	
	
	/**
	 * Checks whether the given user can modify the settings profile with the given ID. They can do this
	 * assuming they either own the profile or are a leader in the space the profile is associated with
	 * @param id
	 * @param userId
	 * @return
	 */
	public static ValidatorStatusCode canModifySettings(int id, int userId) {
		DefaultSettings d=Settings.getProfileById(id);
		if (d==null) {
			return new ValidatorStatusCode(false, "The given setting profile could not be found");
		}
		if (d.getType()==SettingType.USER) {
			if (d.getPrimId()!=userId && !Users.isAdmin(userId)) {
				return new ValidatorStatusCode(false, "You may not update default setting profiles of other users");
			}
		} else {
			Permission perm = Permissions.get(userId, d.getPrimId());		
			if(perm == null || !perm.isLeader()) {
				return new ValidatorStatusCode(false, "Only leaders can update default settings in a space");
			}
		}
		return new ValidatorStatusCode(true);
	}
	/**
	 * Checks whether a user can update the default settings (default timeouts, max-memory, etc.) of a 
	 * community.
	 * @param id the ID of the DefaultSettings object
	 * @param attribute The name of the setting being changed
	 * @param newValue The new value that would be given to the setting
	 * @param userId The ID of the user making the request
	 * @return 0 if the operation is allowed and a status code from ValidatorStatusCodes otherwise
	 */
	
	public static ValidatorStatusCode canUpdateSettings(int id, String attribute, String newValue, int userId) {
		
				
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
		} else if (attribute.equals("PostProcess")) {
			if (!ProcessorSecurity.canUserSeeProcessor(Integer.parseInt(newValue), userId).isSuccess()) {
				return new ValidatorStatusCode(false, "You do not have permission to see the given solver, or the given solver does not exist");
			}
			Processor p=Processors.get(Integer.parseInt(newValue));
			if (p.getType()!=ProcessorType.POST){
				return new ValidatorStatusCode(false,"The given processor is not a preprocessor");
			}	
		} else if (attribute.equals("BenchProcess")) {
			if (!ProcessorSecurity.canUserSeeProcessor(Integer.parseInt(newValue), userId).isSuccess()) {
				return new ValidatorStatusCode(false, "You do not have permission to see the given solver, or the given solver does not exist");
			}
			Processor p=Processors.get(Integer.parseInt(newValue));
			if (p.getType()!=ProcessorType.BENCH){
				return new ValidatorStatusCode(false,"The given processor is not a preprocessor");
			}
		} else if (attribute.equals("defaultbenchmark")) {
			if (!Permissions.canUserSeeBench(Integer.parseInt(newValue), userId)) {
				return new ValidatorStatusCode(false, "You do not have permission to see the given solver, or the given solver does not exist");
			}	
		} else if (attribute.equals("defaultsolver")) {
			if (!Permissions.canUserSeeSolver(Integer.parseInt(newValue), userId)) {
				return new ValidatorStatusCode(false, "You do not have permission to see the given solver, or the given solver does not exist");
			}
		} else if (attribute.equals("PreProcess")) {
			if (!ProcessorSecurity.canUserSeeProcessor(Integer.parseInt(newValue), userId).isSuccess()) {
				return new ValidatorStatusCode(false, "You do not have permission to see the given solver, or the given solver does not exist");
			}
			Processor p=Processors.get(Integer.parseInt(newValue));
			if (p.getType()!=ProcessorType.PRE){
				return new ValidatorStatusCode(false,"The given processor is not a preprocessor");
			}
		}
		
		return canModifySettings(id,userId);
	}
}
