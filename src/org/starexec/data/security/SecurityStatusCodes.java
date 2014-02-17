package org.starexec.data.security;

public class SecurityStatusCodes {
	public static final int ERROR_DATABASE=1;
	public static final int ERROR_INVALID_WEBSITE_TYPE=1;
	public static final int ERROR_EDIT_VAL_ABSENT=1;
	public static final int ERROR_IDS_NOT_GIVEN=1;
	public static final int ERROR_SPACE_ALREADY_PUBLIC=1;
	public static final int ERROR_SPACE_ALREADY_public=1;
	
	public static final int ERROR_INVALID_PERMISSIONS=2;
	public static final int ERROR_INVALID_PASSWORD=2;
	
	public static final int ERROR_JOB_INCOMPLETE=3;
	public static final int ERROR_INVALID_PARAMS=3;
	public static final int ERROR_CANT_REMOVE_FROM_SUBSPACE=3;
	public static final int ERROR_PASSWORDS_NOT_EQUAL=3;
	public static final int ERROR_CANT_EDIT_LEADER_PERMS=3;
	public static final int ERROR_CANT_PROMOTE_SELF=3;
	public static final int ERROR_CANT_PROMOTE_LEADER=3;
	public static final int ERROR_JOB_NOT_PROCESSING=3;
	
	public static final int ERROR_NOT_IN_SPACE=4;
	public static final int ERROR_CANT_REMOVE_LEADER=4;
	public static final int ERROR_NOT_ALL_DELETED=4;
	public static final int ERROR_WRONG_PASSWORD=4; 
	
	public static final int ERROR_CANT_REMOVE_SELF=5;
	public static final int ERROR_SPACE_LOCKED=5;
	
	public static final int ERROR_CANT_LINK_TO_SUBSPACE=6;
	public static final int ERROR_CANT_REMOVE_LEADER_IN_SUBSPACE=6;
	
	public static final int ERROR_NOT_UNIQUE_NAME=7;
	
	public static final int ERROR_INSUFFICIENT_QUOTA=8;
	
	public static final int ERROR_NAME_NOT_EDITABLE=9;
	
	public static final int ERROR_PRIM_ALREADY_DELETED=11;
	
	public static final int ERROR_TOO_MANY_JOB_PAIRS=13;
	public static final int ERROR_TOO_MANY_SOLVER_CONFIG_PAIRS=12;
}
