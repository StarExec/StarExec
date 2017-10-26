package org.starexec.command;

import java.util.HashMap;
import java.util.Map;

/**
 * This class holds constants and global variables specific to StarexecCommand
 *
 * @author Eric
 */
public class C {
	private C() {
	} // Make C uninstantiable

	public static Boolean debugMode = false;

	public static final String VERSION = "July 10, 2017";

	public static final String USER_AGENT = "StarExecCommand (" + VERSION + ")";

	public static final String HELP_MESSAGE = "Welcome to StarexecCommand! This tool is intended to allow users to communicate with the "
			+ "Starxec server. For assistance in using this tool, please consult the documentation present in the archive this tool was "
			+ "packaged with.";
	public static final String TYPE_SESSIONID = "JSESSIONID";

	/**
	 * All of the following URLs are relative to the web app root path. They
	 * point to the various servlets and RESTServices on Starexec
	 */
	public static final String URL_STAREXEC_BASE = "https://www.starexec.org/starexec/";
	public static final String URL_SPACES = "secure/explore/spaces.jsp";
	public static final String URL_LOGGED_IN = "services/session/logged-in";
	public static final String URL_HOME = "secure/index.jsp";
	public static final String URL_LOGIN = "secure/j_security_check";
	public static final String URL_DOWNLOAD = "secure/download";

	public static final String URL_LOGOUT = "services/session/logout";
	public static final String URL_USERSETTING = "services/edit/user/";
	public static final String URL_UPLOADSOLVER = "secure/upload/solvers";
	public static final String URL_UPLOADBENCHMARKS = "/secure/upload/benchmarks";
	public static final String URL_UPLOADPROCESSOR = "secure/processors/manager";
	public static final String URL_UPLOADSPACE = "secure/upload/space";
	public static final String URL_UPLOADJOBXML = "secure/upload/jobXML";
	public static final String URL_DELETEPRIMITIVE = "services/delete";
	public static final String URL_ADDSPACE = "secure/add/space";
	public static final String URL_EDITSPACEVISIBILITY = "services/space/changePublic";
	public static final String URL_UPLOADCONFIG = "secure/upload/configurations";
	public static final String URL_POSTJOB = "secure/add/job";
	public static final String URL_GETSOLVERCONFIGS = "secure/details/solverconfigs.jsp";
	public static final String URL_GETPRIM = "services/space/{id}/{type}/pagination";
	public static final String URL_GETPRIMJSON = "services/details/{type}/{id}/";
	public static final String URL_GETUSERPRIM = "services/users/{id}/{type}/pagination";
	public static final String URL_GET_BENCH_UPLOAD_STATUS = "services/benchmarks/uploadDescription/{statusId}";
	public static final String URL_GETID = "services/users/getid";
	public static final String URL_COPYBENCH = "services/spaces/{spaceID}/add/benchmark";
	public static final String URL_COPYSOLVER = "services/spaces/{spaceID}/add/solver";
	public static final String URL_COPYSPACE = "services/spaces/{spaceID}/copySpace";
	public static final String URL_COPYJOB = "services/spaces/{spaceID}/add/job";

	public static final String URL_COMMUNITY_FROM_SPACE_SPACE_ID_PARAM = "{spaceID}";
	public static final String URL_COMMUNITY_FROM_SPACE = "services/space/community/"
			+ URL_COMMUNITY_FROM_SPACE_SPACE_ID_PARAM;

	public static final String URL_COPYUSER = "services/spaces/{spaceID}/add/user";
	public static final String URL_REMOVEPRIMITIVE = "services/remove";
	public static final String URL_PAUSEORRESUME = "services/{method}/job/{id}";
	public static final String URL_RERUNPAIR = "services/jobs/pairs/rerun/{id}";
	public static final String URL_RERUNJOB = "services/jobs/rerunallpairs/{id}";

	// Success codes for command parsing
	public static final int SUCCESS_EXIT    = Integer.MAX_VALUE - 0;
	public static final int SUCCESS_NOFILE  = Integer.MAX_VALUE - 1;
	public static final int SUCCESS_JOBDONE = Integer.MAX_VALUE - 2;
	public static final int SUCCESS_LOGOUT  = Integer.MAX_VALUE - 3;
	public static final int SUCCESS_LOGIN   = Integer.MAX_VALUE - 4;

	public static final Map<Integer, String> successMessages = new HashMap<>();

	static {
		successMessages.put(SUCCESS_EXIT, "Goodbye");
		successMessages.put(SUCCESS_NOFILE, "No new job results");
		successMessages.put(SUCCESS_JOBDONE, "Job complete, all results retrieved");
		successMessages.put(SUCCESS_LOGOUT, "Logout successful");
		successMessages.put(SUCCESS_LOGIN, "Login successful");
	}
	// Comment

	public static final String COMMENT_SYMBOL = "#";

	// Commands the user may use

	// General commands
	public static final String COMMAND_SLEEP = "sleep";
	public static final String COMMAND_HELP = "help";
	public static final String COMMAND_EXIT = "exit";
	public static final String COMMAND_LOGIN = "login";
	public static final String COMMAND_LOGOUT = "logout";
	public static final String COMMAND_RUNFILE = "runfile";
	public static final String COMMAND_RETURNIDS = "returnids";
	public static final String COMMAND_IGNOREIDS = "ignoreids";
	public static final String COMMAND_VIEWALL = "viewall";
	public static final String COMMAND_VIEWLIMITED = "viewlimit";
	public static final String COMMAND_PAUSEJOB = "pausejob";
	public static final String COMMAND_RESUMEJOB = "resumejob";
	public static final String COMMAND_RERUNPAIR = "rerunpair";
	public static final String COMMAND_RERUNJOB = "rerunjob";
	public static final String COMMAND_DEBUG = "debug";

	// Download commands
	public static final String COMMAND_GETJOBOUT = "getjobout";
	public static final String COMMAND_GETJOBINFO = "getjobinfo";
	public static final String COMMAND_GETSOLVER = "getsolver";
	public static final String COMMAND_GETSPACEXML = "getspacexml";
	public static final String COMMAND_GETJOBXML = "getjobxml";
	public static final String COMMAND_GETBENCH = "getbench";
	public static final String COMMAND_GETJOBPAIR = "getjobpair";
	public static final String COMMAND_GETJOBPAIRS = "getjobpairs";
	public static final String COMMAND_GETSPACE = "getspace";
	public static final String COMMAND_GETSPACEHIERARCHY = "getspacehierarchy";
	public static final String COMMAND_GETPREPROC = "getpreproc";
	public static final String COMMAND_GETPOSTPROC = "getpostproc";
	public static final String COMMAND_GETBENCHPROC = "getbenchproc";
	public static final String COMMAND_GETNEWJOBINFO = "getnewjobinfo";
	public static final String COMMAND_GETNEWJOBOUT = "getnewjobout";
	public static final String COMMAND_POLLJOB = "polljob";

	// get information commands
	public static final String COMMAND_VIEWSOLVER = "viewsolver";
	public static final String COMMAND_VIEWJOB = "viewjob";
	public static final String COMMAND_VIEWBENCH = "viewbench";
	public static final String COMMAND_VIEWSPACE = "viewspace";
	public static final String COMMAND_VIEWPROCESSOR = "viewproc";
	public static final String COMMAND_VIEWCONFIGURATION = "viewconfig";
	public static final String COMMAND_VIEWQUEUE = "viewqueue";
	// TODO: Think about a better way to handle this command
	public static final String COMMAND_GET_BENCH_UPLOAD_STATUS = "viewuploadstatus";

	// Setting commands
	public static final String COMMAND_SETFIRSTNAME = "setfirstname";
	public static final String COMMAND_SETLASTNAME = "setlastname";
	public static final String COMMAND_SETINSTITUTION = "setinstitution";
	public static final String COMMAND_SETSPACEPUBLIC = "setspacepublic";
	public static final String COMMAND_SETSPACEPRIVATE = "setspaceprivate";

	// Uploading commands
	public static final String COMMAND_PUSHSOLVER = "pushsolver";
	public static final String COMMAND_PUSHBENCHMARKS = "pushbenchmarks";
	public static final String COMMAND_PUSHPREPROC = "pushpreproc";
	public static final String COMMAND_PUSHPOSTPROC = "pushpostproc";
	public static final String COMMAND_PUSHBENCHPROC = "pushbenchproc";
	public static final String COMMAND_PUSHSPACEXML = "pushspacexml";
	public static final String COMMAND_PUSHJOBXML = "pushjobxml";
	public static final String COMMAND_PUSHCONFIGRUATION = "pushconfig";

	public static final String COMMAND_PRINT = "print";

	// deleting commands
	public static final String COMMAND_DELETESOLVER = "deletesolver";
	public static final String COMMAND_DELETEBENCH = "deletebench";
	public static final String COMMAND_DELETEPOSTPROC = "deletepostproc";
	public static final String COMMAND_DELETEBENCHPROC = "deletebenchproc";
	public static final String COMMAND_DELETEJOB = "deletejob";
	public static final String COMMAND_DELETECONFIG = "deleteconfig";

	// remove commands
	public static final String COMMAND_REMOVEUSER = "removeuser";
	public static final String COMMAND_REMOVEBENCHMARK = "removebench";
	public static final String COMMAND_REMOVESOLVER = "removesolver";
	public static final String COMMAND_REMOVEJOB = "removejob";
	public static final String COMMAND_REMOVESUBSPACE = "removesubspace";

	// creating commands
	public static final String COMMAND_CREATEJOB = "createjob";
	public static final String COMMAND_CREATESUBSPACE = "createsubspace";

	// copy and mirror commands
	public static final String COMMAND_COPYBENCH = "copybench";
	public static final String COMMAND_COPYSOLVER = "copysolver";
	public static final String COMMAND_LINKBENCH = "linkbench";
	public static final String COMMAND_LINKSOLVER = "linksolver";
	public static final String COMMAND_COPYSPACE = "copyspace";
	public static final String COMMAND_LINKJOB = "linkjob";
	public static final String COMMAND_LINKUSER = "linkuser";

	// listing commands
	public static final String COMMAND_LISTSOLVERS = "lssolvers";
	public static final String COMMAND_LISTSOLVERCONFIGS = "lsconfigs";
	public static final String COMMAND_LISTPRIMITIVES = "ls";
	public static final String COMMAND_LISTBENCHMARKS = "lsbenchmarks";
	public static final String COMMAND_LISTJOBS = "lsjobs";
	public static final String COMMAND_LISTUSERS = "lsusers";
	public static final String COMMAND_LISTSUBSPACES = "lssubspaces";

	// Param names expected at the command line
	public static final String PARAM_NAME = "n";
	public static final String PARAM_DESC = "d";
	public static final String PARAM_DESCRIPTION_FILE = "df";
	public static final String PARAM_DOWNLOADABLE = "downloadable";
	public static final String PARAM_URL = "url";
	public static final String PARAM_FILE = "f";
	public static final String PARAM_ID = "id";
	public static final String PARAM_VAL = "val";
	public static final String PARAM_BENCHTYPE = "bt";
	public static final String PARAM_DEPENDENCY = "dep";
	public static final String PARAM_LINKED = "link";
	public static final String PARAM_ENABLE_ALL_PERMISSIONS = "allperm";
	public static final String PARAM_OVERWRITE = "ow";
	public static final String PARAM_OUTPUT_FILE = "out";
	public static final String PARAM_LOCKED = "lock";
	public static final String PARAM_HIERARCHY = "hier";
	public static final String PARAM_QUEUEID = "qid";
	public static final String PARAM_PROCID = "pid";
	public static final String PARAM_PREPROCID = "preid";
	public static final String PARAM_WALLCLOCKTIMEOUT = "w";
	public static final String PARAM_CPUTIMEOUT = "cpu";
	public static final String PARAM_SINCE = "since";
	public static final String PARAM_LIMIT = "limit";
	public static final String PARAM_USER = "u";
	public static final String PARAM_PASSWORD = "p";
	public static final String PARAM_BASEURL = "addr";
	public static final String PARAM_GET_ATTRIBUTES = "attr";
	public static final String PARAM_VERBOSE = "verbose";
	public static final String[] PARAMS_PERMS = { "addSolver", "addUser", "addSpace", "addJob", "addBench", "removeSolver",
			"removeUser", "removeSpace", "removeJob", "removeBench" };
	public static final String PARAM_TIME = "t";
	public static final String PARAM_MESSAGE = "m";
	public static final String PARAM_GUEST = "guest";
	public static final String PARAM_FROM = "from";
	public static final String PARAM_TO = "to";
	public static final String PARAM_RECYCLE_PRIMS = "recycleprims";
	public static final String PARAM_INCLUDE_IDS = "incids";
	public static final String PARAM_EXCLUDE_SOLVERS = "nosolve";
	public static final String PARAM_EXCLUDE_BENCHMARKS = "nobench";
	public static final String PARAM_TRAVERSAL = "trav";
	public static final String PARAM_MEMORY = "mem";
	public static final String PARAM_PAUSED = "pause";
	public static final String PARAM_SEED = "seed";
	public static final String PARAM_RUN = "run";
	public static final String PARAM_ONLY_COMPLETED = "comp";
	public static final String PARAM_SETTING = "set";
	public static final String PARAM_TYPE = "type";
	public static final String PARAM_SUPPRESS_TIMESTAMPS = "suppresstime";
	public static final String PARAM_RESULTS_INTERVAL = "interval";
	public static final String PARAM_LONG_PATH = "longpath";
	public static final String PARAM_COPY_PRIMITIVES = "copyprimitives";
	public static final String ARG_ROUNDROBIN = "r";
	public static final String ARG_DEPTHFIRST = "d";

	// parameters expected by the StarExec server
	public static final String FORMPARAM_TYPE = "type";
	public static final String FORMPARAM_SINCE = "since";
	public static final String FORMPARAM_ID = "id";
	public static final String FORMPARAM_TRAVERSAL = "traversal";

	public static final Integer DEFAULT_SOLVER_TYPE = 1;

	public static final String STATUS_MESSAGE_COOKIE = "STATUS_MESSAGE_STRING";
}
