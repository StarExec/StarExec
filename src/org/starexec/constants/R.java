package org.starexec.constants;
import java.io.File;
import java.lang.UnsupportedOperationException;
import java.sql.Date;
import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.function.BiFunction;

import org.starexec.backend.Backend;
import org.starexec.backend.GridEngineBackend;
import org.starexec.backend.LocalBackend;
import org.starexec.backend.OARBackend;
import org.starexec.data.to.enums.BenchmarkingFramework;
import org.starexec.exceptions.StarExecException;
import org.w3c.dom.Element;

/**
 * Class which holds static resources (R) available for use
 * throughout the entire application. This will include many
 * constant strings and numbers that other classes rely on.
 * @author Tyler Jensen
 */
public class R {
    /* 
     * IMPORTANT: This class only supports string, int and boolean types.
     * DO NOT change field names without changing their corresponding keys
     * in starexec-config.xml. Field names must match property key names!
     * 
     * Any fields set here will be treated as defaults
     */
	

	private R() throws UnsupportedOperationException{
		throw new UnsupportedOperationException("Cannot instantiate class because it is static.");
    }

    public static String getBenchmarkPath() {
		return STAREXEC_DATA_DIR + "/Benchmarks";
	}

	public static String getSolverPath() {
		return STAREXEC_DATA_DIR +"/Solvers";
	}

	public static String getJobInboxDir() {
		return STAREXEC_DATA_DIR + "/jobin";
	}

	public static String getJobOutputDirectory() {
		return STAREXEC_DATA_DIR + "/joboutput";
	}


	public static String getProcessorDir() {
		return STAREXEC_DATA_DIR +"/processor_scripts";
	}

	public static String getPicturePath() {
		return STAREXEC_DATA_DIR + "/pictures";
	}

	public static String getSolverBuildOutputDir() {
		return getSolverPath()+"/buildoutput";
	}

	public static String getJobLogDir() {
		return getJobOutputDirectory()+"/logs";
	}
	public static String getSolverCacheClearLogDir() {
		return getJobLogDir()+"/solvercache";
	}

	public static String getBatchSpaceXMLDir() {
		return STAREXEC_DATA_DIR + "/batchSpace/uploads";
	}

	public static String getScriptDir() {
		return STAREXEC_DATA_DIR+"/sge_scripts";
	}


	public static final String SGE_TYPE = "sge";
    public static final String OAR_TYPE = "oar";
    public static final String LOCAL_TYPE = "local";

	/**
	 * Returns a Backend of the class corresponding to the BACKEND_TYPE set
	 * @return
	 * @throws StarExecException
	 */
	public static Backend getBackendFromType() throws StarExecException {
		if (BACKEND_TYPE.equals(SGE_TYPE)) {
			return new GridEngineBackend();
		} else if (BACKEND_TYPE.equals(OAR_TYPE)) {
			return new OARBackend();
		} else if (BACKEND_TYPE.equals(LOCAL_TYPE)) {
			return new LocalBackend();
		} else {
			throw new StarExecException("BACKEND_TYPE was configured as "+BACKEND_TYPE+", but one of 'sge' 'oar' or 'local' is required");
		}
	}

	public static String BACKEND_TYPE = null;
	public static Backend BACKEND = null;

    //maximum length properties
    public static int SPACE_NAME_LEN=250;
    public static int SPACE_DESC_LEN=1024;
    public static int USER_FIRST_LEN=32;
    public static int USER_LAST_LEN=32;
    public static int INSTITUTION_LEN=64;
    public static int EMAIL_LEN=64;
    public static int PASSWORD_LEN=20;
    public static int MSG_LEN=512;
    public static int BENCH_NAME_LEN=250;
    public static int BENCH_DESC_LEN=1024;
    public static int CONFIGURATION_NAME_LEN=128;
    public static int CONFIGURATION_DESC_LEN=1024;
    public static int SOLVER_NAME_LEN=64;
    public static int WEBSITE_NAME_LEN=64;
    public static int PIPELINE_NAME_LEN=128;
    public static int SETTINGS_NAME_LEN=32;
    public static int SOLVER_DESC_LEN=1024;
    public static int JOB_NAME_LEN=64;
    public static int JOB_DESC_LEN=1024;
    public static int URL_LEN=128;
    public static int PROCESSOR_NAME_LEN=64;
    public static int PROCESSOR_DESC_LEN=1024;
    public static int QUEUE_NAME_LEN=64;
    public static int TEXT_FIELD_LEN = 65000;

    // This value is dynamically set by configuration.
    public static int CLUSTER_UPDATE_PERIOD = 600;

    public enum DefaultSettingAttribute {
        PostProcess,
        BenchProcess,
        CpuTimeout,
        ClockTimeout,
        DependenciesEnabled,
        defaultbenchmark,
        defaultsolver,
        MaxMem,
        PreProcess;
    }

	// Matrix view settings
	public static final int MATRIX_VIEW_COLUMN_HEADER = 18; // Limit on number of letters for Solver or config name
	public static final int MAX_MATRIX_JOBPAIRS = 10000;

	// JSP page constants
	public static final String SUPPRESS_TIMESTAMP_INPUT_NAME = "suppressTimestamp"; // Name of input value for suppress timestamps in job.jsp


    //the number of increments we should accumulate in an upload status field before actually committing to the database
    //public static int UPLOAD_STATUS_UPDATE_THRESHOLD=100;
    public static long UPLOAD_STATUS_TIME_BETWEEN_UPDATES=9000; //number of milliseconds that should pass between updates
    															//to an upload status object
    // Maximum job pair settings
    public static int MAXIMUM_JOB_PAIRS=Integer.MAX_VALUE; // no restriction for now
    public static int MAXIMUM_SOLVER_CONFIG_PAIRS=5;
    public static int MAXIMUM_DATA_POINTS=30000;
    //Regex patterns
    public static String BOOLEAN_PATTERN="true|false";
    public static String LONG_PATTERN="^\\-?\\d+$";
    public static String USER_NAME_PATTERN="^[A-Za-z\\-\\s']{2," +String.valueOf(USER_FIRST_LEN)+ "}$";
    public static String INSTITUTION_PATTERN="^[\\w\\-\\s']{2," +String.valueOf(INSTITUTION_LEN) +"}$";
    public static String EMAIL_PATTERN="^[\\w.%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,4}$";
    public static String URL_PATTERN="https?://.\\S+{2,"+String.valueOf(URL_LEN)+"}";
    public static String PRIMITIVE_NAME_PATTERN="^[\\w\\-\\. \\+\\^=,!?:$%#@]+$";
    public static String SPACE_NAME_PATTERN="^[\\w\\-\\. \\+\\^=,!?:$%#@]{1,"+String.valueOf(SPACE_NAME_LEN)+"}$";


    public static String REQUEST_MESSAGE="^[\\w\\]\\[\\!\"#\\$%&'()\\*\\+,\\./:;=\\?@\\^_`{\\|}~\\- ]{2,"+R.MSG_LEN+"}$";
    public static String PRIMITIVE_DESC_PATTERN="^[^<>\"\'%;)(&\\+-]{0,"+String.valueOf(SPACE_DESC_LEN)+"}$";
    public static String PASSWORD_PATTERN="^(?=.*[A-Za-z0-9~`!@#\\$%\\^&\\*\\(\\)_\\-\\+\\=]+$)(?=.*[0-9~`!@#\\$%\\^&\\*\\(\\)_\\-\\+\\=]{1,})(?=.*[A-Za-z]{1,}).{5,32}$";
	public static String DATE_PATTERN="[0-9][0-9]/[0-9][0-9]/[0-9][0-9][0-9][0-9]";
	public static String DOUBLE_PATTERN="^\\-?((\\d+(\\.\\d*)?)|(\\.\\d+))$";


	public static String JOB_PAIR_PATH_DELIMITER="/";
    // Email properties
    public static String EMAIL_SMTP = null;
    public static int EMAIL_SMTP_PORT = 25;
    public static String EMAIL_USER = null;
    public static String EMAIL_PWD = null;

    // http properties
    public static String HTTP_PROXY_HOST = "https://proxy.divms.uiowa.edu";
    public static String HTTP_PROXY_PORT = "8888";

    // MySQL properties
    public static String MYSQL_DATABASE = null;								// Name of the MySQL database
    public static String MYSQL_URL = null;									// MySQL connection string for JDBC
    public static String MYSQL_USERNAME = null;								// Starexec's username for the database
    public static String MYSQL_PASSWORD = null;								// Starexec database password
    public static String COMPUTE_NODE_MYSQL_USERNAME = null;                // username for database to use from compute nodes
    public static String COMPUTE_NODE_MYSQL_PASSWORD = null;				// DB password for COMPUTE_NODE_MYSQL_USERNAME
    public static String MYSQL_DRIVER = "com.mysql.jdbc.Driver";			// MySQL java driver class (we use JDBC)
    public static int MYSQL_POOL_MAX_SIZE = 1;								// The maximum number of connections in the database pool
    public static int MYSQL_POOL_MIN_SIZE = 1;								// The minimum number of connections to keep open to the database
    public static String REPORT_HOST = "starexec1.star.cs.uiowa.edu";  // where to report job status updates during jobs

    // Global path information
    public static String SOLVER_BUILD_OUTPUT = "starexec_build_log";                        // The name of the file in which we're storing build output
    public static String STAREXEC_ROOT = null;								// The directory of the starexec webapp
    public static String CONFIG_PATH = null;								// The directory of starexec's configuration and template files relative to the root path
    public static String RUNSOLVER_PATH = null;								// The absolute filepath to the runsolver executable
    public static String STAREXEC_DATA_DIR = null;   						// the root of the data directory (where jobin/, jobout/, and dirs for primitive are)
    public static String DOWNLOAD_FILE_DIR = "/secure/files";				// Where to temporarily store processed files for downloading. Relative to webapp root
    public static String SPACE_XML_SCHEMA_RELATIVE_LOC = "public/batchSpaceSchema.xsd";						// Where the schema for batch space xml is located, relative to STAREXEC_ROOT.
    public static String JOB_XML_SCHEMA_RELATIVE_LOC = "public/batchJobSchema.xsd";
    public static String STAREXEC_URL_PREFIX = null;						//either "https" or "http"
	public static String JOBGRAPH_FILE_DIR = "/secure/jobgraphs";			// Location to store job graph image files. Relative to webapp root.
	public static String SANDBOX_DIRECTORY=null;                          //the sandbox directory for doing processing / building on the head node

	//Admin user info
    public static int ADMIN_USER_ID = 9;									//user id to use when administrator
    public static String ADMIN_USER_PASSWORD = "admin";
    public static String DEFAULT_QUEUE_NAME = "all.q";						//The name of the default queue
    public static int DEFAULT_QUEUE_ID=1;
    //Test info
    public static int TEST_COMMUNITY_ID=-1;
    public static boolean ALLOW_TESTING=false;								// whether tests should be allowed to run on this instance. False for production.
    //Public user info
    public static int PUBLIC_USER_ID = 0;									//user id to use when writing benchmarks, submitting jobs without login
    public static int PUBLIC_CPU_LIMIT = 30;
    public static int PUBLIC_CLOCK_TIMEOUT = 30;
    public static String PUBLIC_USER_EMAIL = "public";
    public static String PUBLIC_USER_PASSWORD ="public";
    // Job Manager (JM) constants
    public static String JOBFILE_FORMAT = "job_%d.bash";					// The filename format (with standard java string formatting) for generated jobscript files
    public static String DEPENDFILE_FORMAT = "depend_%d.txt";				// The filename format for dependencies
    public static String SOLVER_BIN_DIR = "/bin";							// The path to the bin directory to look for runscripts (relative to the solver's toplevel directory)
    public static String SANDBOX_USER_ONE=null;								// name of user that executes jobs in sandbox one
    public static String SANDBOX_USER_TWO=null; 							// name of user that executes jobs in sandbox two
    // Misc application properties
    public static String STAREXEC_SERVERNAME = null;
    public static String STAREXEC_APPNAME = null;
    public static String PWD_HASH_ALGORITHM = "SHA-512";					// Which algorithm to use to hash user passwords
    public static String PATH_DATE_FORMAT = "yyyyMMdd-kk.mm.ss.SSS";		// Which datetime format is used to create unique directory names
    public static boolean REMOVE_ARCHIVES = true;							// Whether or not to delete archive files after they're extracted
    public static String CONTACT_EMAIL = "";								// The default e-mail address to use for users to contact for support
    public static boolean IS_FULL_STAREXEC_INSTANCE = true;  				// should we run job tasks (see app/Starexec.java)
    public static int CLEAR_JOB_LOG_PERIOD = 14;                                                        // How often (in days) to clear job logs
    public static int JOB_SUBMISSION_PERIOD = 60;							// How often (in seconds) to write job scripts and submit to the backend
	public static final int MAX_NUMBER_OF_REPORTS_TO_SEND = 30;               // Maximum number of StarExec report emails to send every period
	public static final int WAIT_TIME_BETWEEN_EMAILING_REPORTS = 2;           // Number of seconds to wait between reports being sent
	public static final int EMAIL_REPORTS_DAY = Calendar.THURSDAY;              // Day of the week to email reports
    public static HashMap<Integer,HashMap<String,Long>> COMM_INFO_MAP = null;
    public static Long COMM_ASSOC_LAST_UPDATE = null;    //last time community_assoc table was updated (milliseconds)
    public static long COMM_ASSOC_UPDATE_PERIOD = 21600000;  //how much time we should wait before requerying for community_assoc table, currentely set to a 10 seconds (milliseconds)
    public static long DEFAULT_DISK_QUOTA = 52428800;						// The default user disk quota to assign new users; currently 50MB
    public static int DEFAULT_PAIR_QUOTA = 750000;							// The default max number of pairs a user should be able to own
    public static String PERSONAL_SPACE_DESCRIPTION =						// The default text that appears at the top of a user's personal space
	"this is your personal space";
    public static int MAX_FAILED_VALIDATIONS=50;						//More than this number of benchmark validation failures triggers a message and ends
	public static String VALID_BENCHMARK_ATTRIBUTE = "starexec-valid";      //Name of attribute given by benchmark processors to show a benchmark is valid
	//Reserved Names for users
	public static String STAREXEC_RESULT = "starexec-result";				// The key used for the starexec result in key-value pairs for a job pair
    public static final String DEFAULT_QUEUE_SLOTS = "2";                  // By default we assume there will be two pairs per node and so we divide the memory into two parts for each pair.
	public static String CONFIGURATION_PREFIX = "starexec_run_";            // The prefix for a file in the solver bin directory to be considered a configuration
	public static String EXPECTED_RESULT = "starexec-expected-result";    // key for key value pair in benchmark attributes
	public static String SOLVER_DESC_PATH = "starexec_description.txt";		// File that can be included within the archive solver file to include the description
	public static String SOLVER_BUILD_SCRIPT="starexec_build";
    public static String UPLOAD_TEST_JOB_XML="run_on_upload.xml";

    public static String PROCESSOR_RUN_SCRIPT="process";
	public static String BENCHMARK_DESC_PATH = "starexec_description.txt";	// File that can be included within the archive solver file to include the description
	public static String DESC_PATH = "starexec_description.txt";
	public static String STAREXEC_UNKNOWN="starexec-unknown";               // Result that indicates a pair should not be counted as wrong
	// Queue and node status strings

	public static String QUEUE_STATUS_ACTIVE = "ACTIVE";					// Active status for a backend queue (indicates the queue is live)
	public static String QUEUE_STATUS_INACTIVE = "INACTIVE";				// Inactive status for a backend queue (indicates the queue is not currently live)
	public static String NODE_STATUS_ACTIVE = "ACTIVE";						// Active status for a backend node (indicates the node is live)
	public static String NODE_STATUS_INACTIVE = "INACTIVE";					// Inactive status for a backend node (indicates the node is not currently live)

    // BACKEND configurations
    public static String BACKEND_ROOT = null; // root directory for the backend executable
    public static String BACKEND_WORKING_DIR = null;
    public static long MAX_PAIR_FILE_WRITE = 2097152;  						// The largest possible amount disk space (in kilobytes) a job pair is allowed to use
    public static long DEFAULT_PAIR_VMEM = 17179869184L;  					// The default limit on memory (in bytes) for job pairs
    public static int NODE_MULTIPLIER = 8;                                  // The number of job scripts to submit is the number of nodes in the queue times this

    public static int MAX_STAGES_PER_PIPELINE = 10000;
    public static int NUM_JOB_PAIRS_AT_A_TIME = 5;  // the number of job pairs from a job to submit at the same time, as we cycle through all jobs submitting pairs.
    public static int NUM_REPOSTPROCESS_AT_A_TIME = 200; // number of job pairs to re-postprocess at a time with our periodic task
    public static int DEFAULT_MAX_TIMEOUT = 259200;

    // The number of minutes that is considered ok for a pair to be enqueued but not running.
    public static int PAIR_ENQUEUE_TIME_THRESHOLD = 30;

    public static final int TIME_BETWEEN_SENDING_ERROR_LOGS = 1; // in days


    public static int NO_TYPE_PROC_ID=1;

    public static String STATUS_MESSAGE_COOKIE="STATUS_MESSAGE_STRING";

    public static String JOB_SCHEMA_LOCATION="public/batchJobSchema.xsd";

	// Role names
	public static String DEVELOPER_ROLE_NAME="developer";
    public static String SUSPENDED_ROLE_NAME="suspended";
    public static String DEFAULT_USER_ROLE_NAME="user";
    public static String ADMIN_ROLE_NAME="admin";
    public static String UNAUTHORIZED_ROLE_NAME="unauthorized";

	public static final String JOB_PAGE_DOWNLOAD_TYPE = "job_page";
	public static final String MATRIX_ELEMENT_ID_FORMAT ="%s%d-%s%d-%s%d";

    //some proxy data
    public static String PROXY_ADDRESS = "proxy.divms.uiowa.edu";
    public static int PROXY_PORT = 8888;

    public static boolean DEBUG_MODE_ACTIVE = false;

    //names of primitive types
	public static String SOLVER="solver";
	public static String BENCHMARK="bench";
	public static String CONFIGURATION="config";
	public static String SPACE_XML = "spaceXML";
	public static String JOB_XML = "jobXML";
	public static String PAIR_OUTPUT = "jp_output";
	public static String JOB = "job";
	public static String JOB_OUTPUT = "j_outputs";
	public static String SPACE = "space";
	public static String PROCESSOR = "proc";
	public static String JOB_OUTPUTS = "jp_outputs";
	public static String USER = "user";
    public static final String SOLVER_SOURCE = "solverSrc";

    //
    public static final String CONFIG_ID_ATTR = "config-id";
    public static final String CONFIG_NAME_ATTR = "config-id";


    public static final String ANONYMIZE_ALL = "all";
	public static final String ANONYMIZE_ALL_BUT_BENCH = "allButBench";
	public static final String ANONYMIZE_NONE = "none";

	// 2 years
	public static final int MAX_AGE_OF_ANONYMOUS_LINKS_IN_DAYS = 730;

    // Constants for BenchExec
    public static final BenchmarkingFramework DEFAULT_BENCHMARKING_FRAMEWORK = BenchmarkingFramework.RUNSOLVER;
    public static final String BENCHMARKING_FRAMEWORK_OPTION = "benchmarkingFramework";

	public static final String XML_BENCH_FRAMEWORK_ELE_NAME = "bench-framework";



    public static Timestamp earliestDateToRerunFailedPairs(){
        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        try {
            java.util.Date date = dateFormat.parse("2017-01-25");
            long time = date.getTime();
            return new Timestamp(time);
        } catch (ParseException e) {
            // This should never happen.
            return null;
        }
    }
}
