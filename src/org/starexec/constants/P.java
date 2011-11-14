package org.starexec.constants;

/**
 * Class which holds static request parameter strings so that
 * request parameters can be sent/read between pages
 * 
 * @author Tyler Jensen
 */
public class P {
	
	public P() throws Exception{
		throw new Exception("Cannot instantiate class because it is static.");
	}
	
	public static String USER_COMMUNITY = "cm";
	public static String USER_PASSWORD = "pwd";
	public static String USER_INSTITUTION = "inst";
	public static String USER_EMAIL = "em";
	public static String USER_FIRSTNAME = "fn";
	public static String USER_LASTNAME = "ln";
	
	public static String VERIFY_EMAIL = "conf";
	
	public static String UPLOAD_FILE = "f";
	
	public static String BENCH_XML = "bxml";
	public static String FILE_PARENT = "parent";
	public static String FILE_REQUEST_TYPE = "type";
	
	public static String SUPPORT_DIV = "lvl";
	public static String SOLVER_NAME = "n";
	
	/* OUTDATED BUT KEPT FOR REFERENCE
	public static String JOB_ID = "jid";
	public static String JOB_STATUS = "status";
	public static String JOB_NODE = "node";
	public static String PAIR_ID = "pid";
	public static String PAIR_RESULT = "result";
	public static String PAIR_START_TIME = "stime";
	public static String PAIR_END_TIME = "etime"; */
	
	public static String SESSION_USER = "user";
	public static String PERMISSION_CACHE = "permissions";
}
