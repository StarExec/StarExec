package org.starexec.constants;

/**
 * Class which holds constants that are enforced by the database.
 * These values my only be changed by also issuing a schema change to update
 * them at the database level as well.
 */
public class DB {
	public static final int SPACE_NAME_LEN         = 250;
	public static final int SPACE_DESC_LEN         = 1024;
	public static final int USER_FIRST_LEN         = 32;
	public static final int USER_LAST_LEN          = 32;
	public static final int INSTITUTION_LEN        = 64;
	public static final int EMAIL_LEN              = 64;
	public static final int PASSWORD_LEN           = 20;
	public static final int MSG_LEN                = 512;
	public static final int BENCH_NAME_LEN         = 250;
	public static final int BENCH_DESC_LEN         = 1024;
	public static final int CONFIGURATION_NAME_LEN = 128;
	public static final int CONFIGURATION_DESC_LEN = 1024;
	public static final int SOLVER_NAME_LEN        = 64;
	public static final int WEBSITE_NAME_LEN       = 64;
	public static final int PIPELINE_NAME_LEN      = 128;
	public static final int SETTINGS_NAME_LEN      = 32;
	public static final int SOLVER_DESC_LEN        = 1024;
	public static final int JOB_NAME_LEN           = 64;
	public static final int JOB_DESC_LEN           = 1024;
	public static final int URL_LEN                = 128;
	public static final int PROCESSOR_NAME_LEN     = 64;
	public static final int PROCESSOR_DESC_LEN     = 1024;
	public static final int QUEUE_NAME_LEN         = 64;
	public static final int TEXT_FIELD_LEN         = 65000;
}
