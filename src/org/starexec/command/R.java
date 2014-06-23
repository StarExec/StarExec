package org.starexec.command;

import java.util.HashMap;

public class R {
	
	public static String VERSION="6/23/2014-1";
	
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
	public static String URL_UPLOADBENCHMARKS="/secure/upload/benchmarks";
	public static String URL_UPLOADPROCESSOR="secure/processors/manager";
	public static String URL_UPLOADSPACE="secure/upload/space";
        public static String URL_UPLOADJOBXML="secure/upload/jobXML";
	public static String URL_DELETEPRIMITIVE="services/delete";
	public static String URL_ADDSPACE="secure/add/space";
	public static String URL_EDITSPACEVISIBILITY="services/space";
	public static String URL_UPLOADCONFIG="secure/upload/configurations";
	public static String URL_ADDJOB="secure/add/job.jsp";
	public static String URL_POSTJOB="secure/add/job";
        public static String URL_GETSOLVERCONFIGS="secure/details/solverconfigs.jsp";
	public static String URL_GETPRIM="services/space/{id}/{type}/pagination";
	public static String URL_GETUSERPRIM="services/users/{id}/{type}/pagination";
	public static String URL_GETID="services/users/getid";
	public static String URL_COPYBENCH="services/spaces/{spaceID}/add/benchmark";
	public static String URL_COPYSOLVER="services/spaces/{spaceID}/add/solver";
	public static String URL_COPYSPACE="services/spaces/{spaceID}/copySpace";
	public static String URL_COPYJOB="services/spaces/{spaceID}/add/job";
	public static String URL_COPYUSER="services/spaces/{spaceID}/add/user";
	public static String URL_REMOVEPRIMITIVE="services/remove";
	public static String URL_PAUSEORRESUME="services/{method}/job/{id}";
	//Success codes for command parsing
	public static int SUCCESS_EXIT=1;
	public static int SUCCESS_NOFILE=2;
	public static int SUCCESS_JOBDONE=3;
	public static int SUCCESS_LOGOUT=4;
	public static int SUCCESS_LOGIN=5;
	
	public static HashMap<Integer,String> successMessages=new HashMap<Integer,String>();
	
	static {
		successMessages.put(SUCCESS_EXIT, "Goodbye");
		successMessages.put(SUCCESS_NOFILE, "No new job results");
		successMessages.put(SUCCESS_JOBDONE, "Job complete, all results retrieved");
		successMessages.put(SUCCESS_LOGOUT, "Logout successful");
		successMessages.put(SUCCESS_LOGIN, "Login successful");
	}
	
	
	
	//constants and regular expressions from StarExec for validation
	
	 //maximum length properties
    public static int SPACE_NAME_LEN=255;
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
	public static String COMMAND_RETURNIDS="returnids";
	public static String COMMAND_IGNOREIDS="ignoreids";
	public static String COMMAND_PAUSEJOB="pausejob";
	public static String COMMAND_RESUMEJOB="resumejob";
		//Download commands
	public static String COMMAND_GETJOBOUT="getjobout";
	public static String COMMAND_GETJOBINFO="getjobinfo";
	public static String COMMAND_GETSOLVER="getsolver";
	public static String COMMAND_GETSPACEXML="getspacexml";
        public static String COMMAND_GETJOBXML="getjobxml";
	public static String COMMAND_GETBENCH="getbench";
	public static String COMMAND_GETJOBPAIR="getjobpair";
	public static String COMMAND_GETSPACE="getspace";
	public static String COMMAND_GETSPACEHIERARCHY="getspacehierarchy";
	public static String COMMAND_GETPREPROC="getpreproc";
	public static String COMMAND_GETPOSTPROC="getpostproc";
	public static String COMMAND_GETBENCHPROC="getbenchproc";
	public static String COMMAND_GETNEWJOBINFO="getnewjobinfo";
	public static String COMMAND_GETNEWJOBOUT="getnewjobout";
	public static String COMMAND_POLLJOB="polljob";

	
	//Setting commands
	public static String COMMAND_SETFIRSTNAME="setfirstname";
	public static String COMMAND_SETLASTNAME="setlastname";
	public static String COMMAND_SETINSTITUTION="setinstitution";
	public static String COMMAND_SETSPACEPUBLIC="setspacepublic";
	public static String COMMAND_SETSPACEPRIVATE="setspaceprivate";
	
	//Uploading commands
	public static String COMMAND_PUSHSOLVER="pushsolver";
	public static String COMMAND_PUSHBENCHMARKS="pushbenchmarks";
	public static String COMMAND_PUSHPOSTPROC="pushpostproc";
	public static String COMMAND_PUSHBENCHPROC="pushbenchproc";
	public static String COMMAND_PUSHSPACEXML="pushspacexml";
        public static String COMMAND_PUSHJOBXML="pushjobxml";
	public static String COMMAND_PUSHCONFIGRUATION="pushconfig";
	
	//deleting commands
	public static String COMMAND_DELETESOLVER="deletesolver";
	public static String COMMAND_DELETEBENCH="deletebench";
	public static String COMMAND_DELETEPOSTPROC="deletepostproc";
	public static String COMMAND_DELETEBENCHPROC="deletebenchproc";
	public static String COMMAND_DELETEJOB="deletejob";
	public static String COMMAND_DELETESPACE="deletespace";
	public static String COMMAND_DELETECONFIG="deleteconfig";
	
	//remove commands
	public static String COMMAND_REMOVEUSER="removeuser";
	public static String COMMAND_REMOVEBENCHMARK="removebench";
	public static String COMMAND_REMOVESOLVER="removesolver";
	public static String COMMAND_REMOVEJOB="removejob";
	public static String COMMAND_REMOVESUBSPACE="removesubspace";
	
	//creating commands
	public static String COMMAND_CREATEJOB="createjob";
	public static String COMMAND_CREATESUBSPACE="createsubspace";
	
	//copy and mirror commands
	public static String COMMAND_COPYBENCH="copybench";
	public static String COMMAND_COPYSOLVER="copysolver";
	public static String COMMAND_LINKBENCH="linkbench";
	public static String COMMAND_LINKSOLVER="linksolver";
	public static String COMMAND_COPYSPACE="copyspace";
	public static String COMMAND_LINKJOB="linkjob";
	public static String COMMAND_LINKUSER="linkuser";
	
	
	//listing commands
	public static String COMMAND_LISTSOLVERS="lssolvers";
        public static String COMMAND_LISTSOLVERCONFIGS="lsconfigs";
	public static String COMMAND_LISTPRIMITIVES="ls";
	public static String COMMAND_LISTBENCHMARKS="lsbenchmarks";
	public static String COMMAND_LISTJOBS="lsjobs";
	public static String COMMAND_LISTUSERS="lsusers";
	public static String COMMAND_LISTSUBSPACES="lssubspaces";
	
	
	//Param names expected at the command line
	public static String PARAM_TEST="test";
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
	public static String PARAM_PREPROCID="preid";
	public static String PARAM_WALLCLOCKTIMEOUT="w";
	public static String PARAM_CPUTIMEOUT="cpu";
	public static String PARAM_SINCE="since";
	public static String PARAM_LIMIT="limit";
	public static String PARAM_USER="u";
	public static String PARAM_PASSWORD="p";
	public static String PARAM_BASEURL="addr";
	public static String PARAM_VERBOSE="verbose";
	public static String[] PARAMS_PERMS={"addSolver","addUser","addSpace","addJob","addBench","removeSolver","removeUser","removeSpace","removeJob","removeBench"};
	public static String PARAM_TIME="t";
	public static String PARAM_GUEST="guest";
	public static String PARAM_FROM="from";
	public static String PARAM_TO="to";
	public static String PARAM_DELETE_PRIMS="deleteprims";
	public static String PARAM_INCLUDE_IDS="incids";
	public static String PARAM_EXCLUDE_SOLVERS="nosolve";
	public static String PARAM_EXCLUDE_BENCHMARKS="nobench";
	public static String PARAM_TRAVERSAL="trav";
	public static String PARAM_MEMORY="mem";
	public static String PARAM_PAUSED="pause";
	public static String PARAM_SEED="seed";
	public static String PARAM_ONLY_COMPLETED="comp";
	public static String ARG_ROUNDROBIN="r";
	public static String ARG_DEPTHFIRST="d";
	
	//parameters expected by the StarExec server
	public static String FORMPARAM_TYPE="type";
	public static String FORMPARAM_SINCE="since";
	public static String FORMPARAM_ID="id";
	public static String FORMPARAM_TRAVERSAL="traversal";
	
	public static String FORMARG_ROUNDROBIN="robin";
	public static String FORMARG_DEPTHFIRST="depth";
}
