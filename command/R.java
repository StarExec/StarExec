package org.starexec.command;

import java.util.HashMap;

public class R {
	
	public static String HELP_MESSAGE = "Welcome to StarexecCommand! This tool is intended to allow users to communicate with the " +
			"Starxec server. For assistance in using this tool, please consult the documentation present in the archive this tool was " +
			"packaged with.";
	
	
	public static String URL_STAREXEC_BASE = "https://www.starexec.org/starexec/";
	public static String URL_STARDEV_BASE = "http://stardev.cs.uiowa.edu/starexec/";
	public static String URL_LOCAL_BASE = "http://localhost:8080/starexec/";
	
	public static String TYPE_SESSIONID="JSESSIONID=";
	
	public static String URL_HOME= "secure/index.jsp";
	public static String URL_LOGIN= "secure/j_security_check";
	public static String URL_SPACES= "secure/explore/spaces";
	public static String URL_DOWNLOAD = "secure/download";
	public static String URL_LOGOUT="services/session/logout";
	public static String URL_USERSETTING="services/edit/user/";
	public static String URL_UPLOADSOLVER="secure/upload/solvers";
	public static String URL_UPLOADPROCESSOR="secure/processors/manager";
	public static String URL_UPLOADSPACE="secure/upload/space";
	public static String URL_DELETEITEM="services/delete";
	public static String URL_ADDSPACE="secure/add/space";
	public static String URL_EDITSPACEVISIBILITY="services/space";
	public static String URL_UPLOADCONFIG="secure/upload/configruations";
	public static String URL_ADDJOB="secure/add/job.jsp";
	public static String URL_POSTJOB="secure/add/job";
	public static String URL_GETPRIM="services/space/{id}/{type}/pagination";
	//Success codes for command parsing
	public static int SUCCESS_EXIT=1;
	public static int SUCCESS_NOFILE=2;
	public static int SUCCESS_JOBDONE=3;
	
	//Error codes for command parsing
	public static int ERROR_BAD_COMMAND=-1;
	public static int ERROR_BAD_ARGS=-2;
	public static int ERROR_UNKNOWN=-3;
	public static int ERROR_SERVER=-4;
	public static int ERROR_BAD_ARCHIVETYPE=-5;
	public static int ERROR_FILE_AND_URL=-6;
	public static int ERROR_MISSING_PARAM=-7;
	public static int ERROR_FILE_NOT_FOUND=-8;
	public static int ERROR_INVALID_FILEPATH=-9;
	public static int ERROR_ARCHIVE_NOT_FOUND=-10;
	public static int ERROR_FILE_EXISTS=-11;
	public static int ERROR_BAD_PARENT_SPACE=-12;
	public static int ERROR_BAD_CREDENTIALS=-13;
	public static int ERROR_COMMAND_NOT_IMPLENETED=-14;
	public static int ERROR_URL_NOT_ALLOWED=-15;
	public static int ERROR_INVALID_ID=-16;
	public static int ERROR_INVALID_TIMEOUT=-17;
	public static int ERROR_CONNECTION_LOST=-18;
	public static int ERROR_BAD_NAME=-19;
	public static int ERROR_BAD_DESCRIPTION=-20;
	public static int ERROR_BAD_TIME=-21;
	public static int ERROR_NOT_LOGGED_IN=-22;
	public static int ERROR_CONNECTION_EXISTS=-23;
	public static int ERROR_BAD_URL=-24;
	public static int ERROR_BAD_INSTITUTION;
	//error messages
	public static HashMap<Integer,String> errorMessages=new HashMap<Integer,String>();
	static {
		errorMessages=new HashMap<Integer,String>();
		errorMessages.put(R.ERROR_BAD_COMMAND, "Unrecognized command");
		errorMessages.put(R.ERROR_BAD_ARGS, "Parameters must be in the form {key}={value}");
		errorMessages.put(R.ERROR_UNKNOWN,"Error parsing command");
		errorMessages.put(R.ERROR_SERVER,"Error communicating with server");
		errorMessages.put(R.ERROR_BAD_ARCHIVETYPE,"Bad archive type-- valid types include zip, tar, and tgz");
		errorMessages.put(R.ERROR_FILE_AND_URL,"An upload should contain either a url or a local file, not both");
		errorMessages.put(R.ERROR_INVALID_FILEPATH,"The given filepath is invalid");
		errorMessages.put(R.ERROR_MISSING_PARAM,"Command is missing a required parameter-- please consult the StarexecCommand reference");
		errorMessages.put(R.ERROR_FILE_NOT_FOUND, "The file to be uploaded could not be found");
		errorMessages.put(R.ERROR_ARCHIVE_NOT_FOUND, "You do not have permission to download the requested archive, or the archive does not exist-- please ensure the given ID is correct");
		errorMessages.put(R.ERROR_FILE_EXISTS, "The specified filepath already exists-- use the flag \"ow\" to overwrite.");
		errorMessages.put(R.ERROR_BAD_PARENT_SPACE,"You do not have permission to add subspaces to the given parent space, or the parent space does not exist");
		errorMessages.put(R.ERROR_BAD_CREDENTIALS,"Invalid username and/or password");
		errorMessages.put(R.ERROR_COMMAND_NOT_IMPLENETED,"Command not currently implemented");
		errorMessages.put(R.ERROR_URL_NOT_ALLOWED,"URL uploads are not allowed here-- please upload a local archive");
		errorMessages.put(R.ERROR_INVALID_ID, "Invalid ID-- IDs must be positive integers");
		errorMessages.put(R.ERROR_INVALID_TIMEOUT, "Invalid timeout-- Timouts must be positive integers");
		errorMessages.put(R.ERROR_CONNECTION_LOST, "The connection to the server was lost");
		errorMessages.put(R.ERROR_BAD_NAME, "The specified name is invalid");
		errorMessages.put(R.ERROR_BAD_DESCRIPTION, "The specified description is invalid");
		errorMessages.put(R.ERROR_BAD_TIME, "The time should be a positive double, measured in seconds");
		errorMessages.put(R.ERROR_NOT_LOGGED_IN, "You must log in before issuing commands to the server");
		errorMessages.put(R.ERROR_CONNECTION_EXISTS, "You must log out of the existing session before you can start a new one");
		errorMessages.put(R.ERROR_BAD_URL, "The given URL does not point to a valid Starexec instance. Ensure that you are using the correct protocol (http vs https) and that the address ends with a /");
		errorMessages.put(R.ERROR_BAD_INSTITUTION, "The institution given has invalid characters or is too long");
	}
	
	
	//constants and regular expressions from StarExec for validation
	
	 //maximum length properties
    public static int SPACE_NAME_LEN=128;
    public static int SPACE_DESC_LEN=1024;
    public static int USER_FIRST_LEN=32;
    public static int USER_LAST_LEN=32;
    public static int INSTITUTION_LEN=64;
    public static int EMAIL_LEN=64;
    public static int PASSWORD_LEN=20;
    public static int MSG_LEN=512;
    public static int BENCH_NAME_LEN=64;
    public static int BENCH_DESC_LEN=1024;
    public static int COMMUNITY_NAME_LEN=64;
    public static int COMMUNITY_DESC_LEN=300;
    public static int CONFIGURATION_NAME_LEN=64;
    public static int CONFIGURATION_DESC_LEN=1024;
    public static int SOLVER_NAME_LEN=64;
    public static int SOLVER_DESC_LEN=1024;
    public static int JOB_NAME_LEN=64;
    public static int JOB_DESC_LEN=1024;
    public static int URL_LEN=128;
    public static int PROCESSOR_NAME_LEN=64;
    public static int PROCESSOR_DESC_LEN=1024;
    
    //Regex patterns
    public static String BOOLEAN_PATTERN="true|false";
    public static String LONG_PATTERN="^\\-?\\d+$";
    public static String USER_NAME_PATTERN="^[A-Za-z\\-\\s']{2," +String.valueOf(USER_FIRST_LEN)+ "}$";
    public static String INSTITUTION_PATTERN="^[\\w\\-\\s']{2," +String.valueOf(INSTITUTION_LEN) +"}$";
    public static String EMAIL_PATTERN="^[\\w.%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,4}$";
    public static String URL_PATTERN="https?://([-\\w\\.]+)+(:\\d+)?(/([\\w/_\\.]*(\\?\\S+)?)?){1,"+ String.valueOf(URL_LEN)+"}";
    public static String PRIMITIVE_NAME_PATTERN="^[\\w\\-\\. \\+\\^=,!?:$%#@]{1,"+String.valueOf(SPACE_NAME_LEN)+"}$";
    public static String REQUEST_MESSAGE="^[\\w\\]\\[\\!\"#\\$%&'()\\*\\+,\\./:;=\\?@\\^_`{\\|}~\\- ]{2,512}$";
    public static String PRIMITIVE_DESC_PATTERN="^.{0,"+String.valueOf(SPACE_DESC_LEN)+"}$";
    public static String PASSWORD_PATTERN="^(?=.*[A-Za-z0-9~`!@#\\$%\\^&\\*\\(\\)_\\-\\+\\=]+$)(?=.*[0-9~`!@#\\$%\\^&\\*\\(\\)_\\-\\+\\=]{1,})(?=.*[A-Za-z]{1,}).{5,32}$";
	
	//Commands the user may use
	
	//General commands
    public static String COMMAND_SLEEP="sleep";
    public static String COMMAND_HELP="help";
	public static String COMMAND_EXIT="exit";
	public static String COMMAND_LOGIN="login";
	public static String COMMAND_LOGOUT="logout";
	public static String COMMAND_RUNFILE="runfile";
	public static String COMMAND_POLLJOB="polljob";
	//Download commands
	public static String COMMAND_GETJOBOUT="getjobout";
	public static String COMMAND_GETJOBINFO="getjobinfo";
	public static String COMMAND_GETSOLVER="getsolver";
	public static String COMMAND_GETSPACEXML="getspacexml";
	public static String COMMAND_GETBENCH="getbench";
	public static String COMMAND_GETJOBPAIR="getjobpair";
	public static String COMMAND_GETSPACE="getspace";
	public static String COMMAND_GETSPACEHIERARCHY="getspacehierarchy";
	public static String COMMAND_GETPOSTPROC="getpostproc";
	public static String COMMAND_GETBENCHPROC="getbenchproc";
	public static String COMMAND_GETNEWJOBINFO="getnewjobinfo";
	public static String COMMAND_GETNEWJOBOUT="getnewjobout";
	
	//Setting commands
	public static String COMMAND_SETARCHIVETYPE="setarchivetype";
	public static String COMMAND_SETFIRSTNAME="setfirstname";
	public static String COMMAND_SETLASTNAME="setlastname";
	//public static String COMMAND_SETEMAIL="setemail";
	public static String COMMAND_SETINSTITUTION="setinstitution";
	public static String COMMAND_SETSPACEPUBLIC="setspacepublic";
	public static String COMMAND_SETSPACEPRIVATE="setspaceprivate";
	
	//Uploading commands
	public static String COMMAND_PUSHSOLVER="pushsolver";
	public static String COMMAND_PUSHBENCHMARKS="pushbenchmarks";
	public static String COMMAND_PUSHPOSTPROC="pushpostproc";
	public static String COMMAND_PUSHBENCHPROC="pushbenchproc";
	public static String COMMAND_PUSHSPACEXML="pushspacexml";
	public static String COMMAND_PUSHCONFIGRUATION="pushconfig";
	
	
	//deleting commands
	public static String COMMAND_DELETESOLVER="deletesolver";
	public static String COMMAND_DELETEBENCH="deletebench";
	public static String COMMAND_DELETEPOSTPROC="deletepostproc";
	public static String COMMAND_DELETEBENCHPROC="deletebenchproc";
	public static String COMMAND_DELETEJOB="deletejob";
	public static String COMMAND_DELETESPACE="deletespace";
	public static String COMMAND_DELETECONFIG="deleteconfig";
	
	//creating commands
	public static String COMMAND_CREATEJOB="createjob";
	public static String COMMAND_CREATESUBSPACE="createsubspace";
	
	//listing commands
	public static String COMMAND_LISTSOLVERS="lssolvers";
	public static String COMMAND_LISTPRIMITIVES="ls";
	public static String COMMAND_LISTBENCHMARKS="lsbenchmarks";
	public static String COMMAND_LISTJOBS="lsjobs";
	public static String COMMAND_LISTUSERS="lsusers";
	public static String COMMAND_LISTSUBSPACES="lssubspaces";
	
	
	//Param names
	public static String PARAM_NAME="n";
	public static String PARAM_DESC="d";
	public static String PARAM_DESCRIPTION_FILE="df";
	public static String PARAM_DOWNLOADABLE="downloadable";
	public static String PARAM_URL="url";
	public static String PARAM_FILE="f";
	public static String PARAM_ID="id";
	public static String PARAM_VAL="val";
	public static String PARAM_BENCHTYPE="bt";
	public static String PARAM_DEPENDENCY="dep";
	public static String PARAM_LINKED="link";
	public static String PARAM_ENABLE_ALL_PERMISSIONS="allperm";
	public static String PARAM_OVERWRITE="ow";
	public static String PARAM_OUTPUT_FILE="out";
	public static String PARAM_LOCKED="lock";
	public static String PARAM_HIERARCHY="hier";
	public static String PARAM_QUEUEID="qid";
	public static String PARAM_PROCID="pid";
	public static String PARAM_WALLCLOCKTIMEOUT="w";
	public static String PARAM_CPUTIMEOUT="cpu";
	public static String PARAM_SINCE="since";
	public static String PARAM_LIMIT="limit";
	public static String PARAM_USERNAME="u";
	public static String PARAM_PASSWORD="p";
	public static String PARAM_BASEURL="addr";
	public static String PARAM_VERBOSE="verbose";
	public static String[] PARAMS_PERMS={"addSolver","addUser","addSpace","addJob","addBench","removeSolver","removeUser","removeSpace","removeJob","removeBench"};
	public static String PARAM_TIME="t";
	public static String PARAM_GUEST="guest";
	
	public static String FORMPARAM_TYPE="type";
	public static String FORMPARAM_SINCE="since";
}
