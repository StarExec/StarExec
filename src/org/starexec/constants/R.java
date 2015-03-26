package org.starexec.constants;
import java.util.HashMap;
import org.starexec.backend.*;

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

    //TODO : Many descriptions and names in this file reference SGE even though the concepts are not SGE specific, should be changed to refer to BACKEND so that they can be more meaningful
	

	public R() throws Exception{
	throw new Exception("Cannot instantiate class because it is static.");
    }

    public static Backend BACKEND = new GridEngineBackend();
	
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

    public static int SETTINGS_NAME_LEN=32;
    public static int SOLVER_DESC_LEN=1024;
    public static int JOB_NAME_LEN=64;
    public static int JOB_DESC_LEN=1024;
    public static int URL_LEN=128;
    public static int PROCESSOR_NAME_LEN=64;
    public static int PROCESSOR_DESC_LEN=1024;
    public static int QUEUE_NAME_LEN=64;
    
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
    //public static String URL_PATTERN="https?://([-\\w\\.]+)+(:\\d+)?(/([\\w/_\\.]*(\\?\\S+)?)?){1,"+ String.valueOf(URL_LEN)+"}";
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
    public static String EMAIL_SMTP = "mta.divms.uiowa.edu";
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
    public static String MYSQL_DRIVER = "com.mysql.jdbc.Driver";			// MySQL java driver class (we use JDBC)
    public static int MYSQL_POOL_MAX_SIZE = 1;								// The maximum number of connections in the database pool
    public static int MYSQL_POOL_MIN_SIZE = 1;								// The minimum number of connections to keep open to the database	
    public static String REPORT_HOST = "starexec1.star.cs.uiowa.edu";  // where to report job status updates during jobs
	
    // Global path information
    public static String SOLVER_PATH = null;								// The top-level directory in which to save the solver file(s)
    public static String SOLVER_BUILD_OUTPUT_DIR=null;                      // The top-level directory in which to save solver build script output
    public static String SOLVER_BUILD_OUTPUT = null;                        // The name of the file in which we're storing build output
    public static String BENCHMARK_PATH = null;								// The top-level directory in which to save the benchmark file(s)
    public static String STAREXEC_ROOT = null;								// The directory of the starexec webapp	
    public static String CONFIG_PATH = null;								// The directory of starexec's configuration and template files relative to the root path
    public static String STAREXEC_DATA_DIR = null;   // the root of the data directory (where jobin/, jobout/, and dirs for primitive are)
    public static String JOBPAIR_INPUT_DIR = null;
    public static String JOB_INBOX_DIR = null;								// Where to deposit new job scripts until SGE distributes it to a node
    public static String JOB_OUTPUT_DIR = null;								// Where to find the saved output from jobs	
    public static String NEW_JOB_OUTPUT_DIR= null;
    public static String JOB_LOG_DIR = null;								// Where to deposit job logs (output from SGE scripts when job runs)
    public static String PROCESSOR_DIR = null;								// Where to deposit new processor scripts
    public static String DOWNLOAD_FILE_DIR = null;							// Where to temporarily store processed files for downloading
    public static String CACHED_FILE_DIR = null;							// Where to temporarily store cached files for downloading
    public static String SPACE_XML_SCHEMA_RELATIVE_LOC = null;						// Where the schema for batch space xml is located, relative to STAREXEC_ROOT. 
    public static String PICTURE_PATH = null;								// Where the pictures are located
    public static String BATCH_SPACE_XML_DIR = null; 						// Place to locate uploaded XML.  Not necessary to keep files, but using Download Directory caused problems
    public static String STAREXEC_URL_PREFIX = null;						//either "https" or "http"
	public static String JOBGRAPH_FILE_DIR = null;
	public static String SANDBOX_DIRECTORY=null;                            //the sandbox directory for doing processing / building on the head node
    
	//Admin user info
    public static int ADMIN_USER_ID = 9;									//user id to use when administrator
    public static String ADMIN_USER_PASSWORD = "admin";			
    public static String DEFAULT_QUEUE_NAME = "all.q";						//The name of the default queue
    public static int DEFAULT_QUEUE_ID=1;
    //Test info
    public static int TEST_USER_ID=-1;
    public static int TEST_COMMUNITY_ID=-1;
    public static String TEST_USER_PASSWORD=null;
    
    //Public user info
    public static int PUBLIC_USER_ID = 0;									//user id to use when writing benchmarks, submitting jobs without login
    public static int PUBLIC_SPACE_ID = 0;                          		//space id to use when writing benchmarks, submitting jobs without login
    public static int PUBLIC_CPU_LIMIT = 30;
    public static int PUBLIC_CLOCK_TIMEOUT = 30;
    public static String PUBLIC_USER_EMAIL = "public";
    public static String PUBLIC_USER_PASSWORD ="public";
    // Job Manager (JM) constants
    public static String JOBFILE_FORMAT = null;								// The filename format (with standard java string formatting) for generated jobscript files
    public static String DEPENDFILE_FORMAT = null;							// The filename format for dependencies
    public static String SOLVER_BIN_DIR = null;								// The path to the bin directory to look for runscripts (relative to the solver's toplevel directory)	
	
    // Misc application properties
    public static String STAREXEC_SERVERNAME = null;
    public static String STAREXEC_APPNAME = null;
    public static boolean LOG_TO_CONSOLE = true;							// Whether or not to output log messages to the console
    public static String PWD_HASH_ALGORITHM = "SHA-512";					// Which algorithm to use to hash user passwords
    public static String PATH_DATE_FORMAT = "yyyyMMdd-kk.mm.ss";			// Which datetime format is used to create unique directory names
    public static boolean REMOVE_ARCHIVES = true;							// Whether or not to delete archive files after they're extracted
    public static String CONTACT_EMAIL = "";								// The default e-mail address to use for users to contact for support
    public static boolean IS_FULL_STAREXEC_INSTANCE = true;  // should we run SGE tasks (see app/Starexec.java)
    public static int CLUSTER_UPDATE_PERIOD = 600;							// How often (in seconds) to update the cluster's current usage status
    public static int SGE_STATISTICS_PERIOD = 120;							// How often (in seconds) to collect finished job statistics from the grid engine
    public static int JOB_SUBMISSION_PERIOD = 60;							// How often (in seconds) to write job scripts and submit to the grid engine
    public static int CREATE_QUEUE_PERIOD = 60;								// How often (in minutes) to check if todays date is the reserved_queue date and then associate nodes
    public static HashMap<Integer,HashMap<String,Long>> COMM_INFO_MAP = null;
    public static Long COMM_ASSOC_LAST_UPDATE = null;    //last time community_assoc table was updated (milliseconds)
    public static long COMM_ASSOC_UPDATE_PERIOD = 21600000;  //how much time we should wait before requerying for community_assoc table, currentely set to a 10 seconds (milliseconds)
    public static long DEFAULT_USER_QUOTA = 52428800;						// The default user disk quota to assign new users; currently 50MB
    public static String PERSONAL_SPACE_DESCRIPTION =						// The default text that appears at the top of a user's personal space 
	"this is your personal space";
    public static int MAX_FAILED_VALIDATIONS=50;							//More than this number of benchmark validation failures triggers a message and ends
	
	//Reserved Names for users
	public static String STAREXEC_RESULT = "starexec-result";				// The key used for the starexec result in key-value pairs for a job pair 
	public static String CONFIGURATION_PREFIX = "starexec_run_";            // The prefix for a file in the solver bin directory to be considered a configuration
	public static String EXPECTED_RESULT = "starexec-expected-result";    // key for key value pair in benchmark attributes
	public static String SOLVER_DESC_PATH = "starexec_description.txt";		// File that can be included within the archive solver file to include the description
	public static String SOLVER_BUILD_SCRIPT="starexec_build";
	public static String PROCESSOR_RUN_SCRIPT="process";
	public static String BENCHMARK_DESC_PATH = "starexec_description.txt";	// File that can be included within the archive solver file to include the description
	public static String DESC_PATH = "starexec_description.txt";
	public static String STAREXEC_UNKNOWN="starexec-unknown";               // Result that indicates a pair should not be counted as wrong
	// Queue and node status strings
	public static String QUEUE_STATUS_ACTIVE = "ACTIVE";					// Active status for an SGE queue (indicates the queue is live)
	public static String QUEUE_STATUS_INACTIVE = "INACTIVE";				// Inactive status for an SGE queue (indicates the queue is not currently live)
	public static String NODE_STATUS_ACTIVE = "ACTIVE";						// Active status for an SGE node (indicates the node is live)
	public static String NODE_STATUS_INACTIVE = "INACTIVE";					// Inactive status for an SGE node (indicates the node is not currently live)
	
    // BACKEND configurations
    //TODO : The name and meaning of SGE_ROOT should be changed, it should no longer be SGE specific but rather be the BACKEND_ROOT
    public static String SGE_ROOT = null; // root directory for SGE
    //TODO : SGE_ACCOUNTING_FILE should be changed, it should no longer be SGE specific but rather be BACKEND_ACCOUNTING_ROOT (backends should be able to provide job stats)
    public static String SGE_ACCOUNTING_FILE = null;  						// The absolute path to the SGE accounting file that holds job statistics
    public static int MAX_PAIR_RUNTIME = 86400;  							// The largest possible amount of time a job pair can run before being terminated (in seconds)
    public static int MAX_PAIR_CPUTIME = 86400;  							// The largest possible cpu time a job pair can run before being terminated (in seconds)
    public static long MAX_PAIR_FILE_WRITE = 2097152;  						// The largest possible amount disk space (in kilobytes) a job pair is allowed to use
    public static long DEFAULT_PAIR_VMEM = 17179869184L;  					// The default limit on memory (in bytes) for job pairs
    //public static int NUM_JOB_SCRIPTS = 100;								// The number of job scripts to write/submit each period
    public static int NODE_MULTIPLIER = 8;                                  // The number of job scripts to submit is the number of nodes in the queue times this
  
    public static int MAX_STAGES_PER_PIPELINE = 1000; //TODO: What is reasonable here? I think we should enforce something
    public static int NUM_JOB_PAIRS_AT_A_TIME = 5;  // the number of job pairs from a job to submit at the same time, as we cycle through all jobs submitting pairs.
    public static int NUM_REPOSTPROCESS_AT_A_TIME = 200; // number of job pairs to re-postprocess at a time with our periodic task
    public static int DEFAULT_MAX_TIMEOUT = 259200;
    
    
    public static int NO_TYPE_PROC_ID=1;
    
    public static String STATUS_MESSAGE_COOKIE="STATUS_MESSAGE_STRING";
    
    public static String JOB_SCHEMA_LOCATION="public/batchJobSchema.xsd";
    
    public static String SUSPENDED_ROLE_NAME="suspended";
    public static String DEFAULT_USER_ROLE_NAME="user";
    public static String ADMIN_ROLE_NAME="admin";
    public static String TEST_ROLE_NAME="test";
    public static String UNAUTHORIZED_ROLE_NAME="unauthorized";

}	
