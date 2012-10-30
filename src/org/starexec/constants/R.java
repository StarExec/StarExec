package org.starexec.constants;

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
	
	public R() throws Exception{
		throw new Exception("Cannot instantiate class because it is static.");
	}
	
	// Email properties
	public static String EMAIL_SMTP = "localhost";
	public static int EMAIL_SMTP_PORT = 25;
	public static String EMAIL_USER = null;
	public static String EMAIL_PWD = null;
	
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
	public static String BENCHMARK_PATH = null;								// The top-level directory in which to save the benchmark file(s)
	public static String STAREXEC_ROOT = null;								// The directory of the starexec webapp	
	public static String CONFIG_PATH = null;								// The directory of starexec's configuration and template files relative to the root path
	public static String NODE_WORKING_DIR = null;							// The directory on the local nodes where they can use for scratch space (read/write)
	public static String JOB_INBOX_DIR = null;								// Where to deposit new job scripts until SGE distributes it to a node
	public static String JOB_OUTPUT_DIR = null;								// Where to find the saved output from jobs	
	public static String JOB_LOG_DIR = null;								// Where to deposit job logs (output from SGE scripts when job runs)
	public static String NODE_OUTPUT_DIR = null;							// The path to the directory on the local node where output should be placed by the user in order to be saved by starexec
	public static String PROCESSOR_DIR = null;								// Where to deposit new processor scripts
	public static String DOWNLOAD_FILE_DIR = null;							// Where to temporarily store processed files for downloading
	public static String SPACE_XML_SCHEMA_LOC = null;						// Where the schema for batch space xml is located. 
	public static String PICTURE_PATH = null;								// Where the pictures are located
	public static String BATCH_SPACE_XML_DIR = null;                        // Place to locate uploaded XML.  Not necessary to keep files, but using Download Directory caused problems
	
	
	//Public user info
	public static int PUBLIC_USER_ID = 0;							//user id to use when writing benchmarks, submitting jobs without login
	public static int PUBLIC_SPACE_ID = 0;                           //space id to use when writing benchmarks, submitting jobs without login
	public static int PUBLIC_CPU_LIMIT = 30;
	public static int PUBLIC_CLOCK_TIMEOUT = 30;
	public static String PUBLIC_USER_EMAIL = "public";
	public static String PUBLIC_USER_PASSWORD ="public";
	// Job Manager (JM) constants
	public static String JOBFILE_FORMAT = null;								// The filename format (with standard java string formatting) for generated jobscript files
	public static String SOLVER_BIN_DIR = null;								// The path to the bin directory to look for runscripts (relative to the solver's toplevel directory)	
	public static int TEMP_JOBPAIR_LIMIT = 1000;						    // A temporary limit to the number of job pairs
	
	// Misc application properties
	public static boolean LOG_TO_CONSOLE = true;							// Whether or not to output log messages to the console
	public static String PWD_HASH_ALGORITHM = "SHA-512";					// Which algorithm to use to hash user passwords
	public static String PATH_DATE_FORMAT = "yyyyMMdd-kk.mm.ss";			// Which datetime format is used to create unique directory names
	public static boolean REMOVE_ARCHIVES = true;							// Whether or not to delete archive files after they're extracted
	public static String CONTACT_EMAIL = "";								// The default e-mail address to use for users to contact for support
	public static int CLUSTER_UPDATE_PERIOD = 60;							// How often (in seconds) to update the cluster's current usage status
	public static int SGE_STATISTICS_PERIOD = 120;							// How often (in seconds) to collect finished job statistics from the grid engine
	public static int JOB_SUBMISSION_PERIOD = 60;							// How often (in seconds) to write job scripts and submit to the grid engine
	public static long DEFAULT_USER_QUOTA = 52428800;						// The default user disk quota to assign new users; currently 50MB
	public static String PERSONAL_SPACE_DESCRIPTION =						// The default text that appears at the top of a user's personal space 
		"this is your personal space";
	
	//Reserved Names for users
	public static String STAREXEC_RESULT = "starexec-result";				// The key used for the starexec result in key-value pairs for a job pair 
	public static String CONFIGURATION_PREFIX = "starexec_run_";            // The prefix for a file in the solver bin directory to be considered a configuration
	public static String EXPECTED_RESULT = "starexec-expected-result";    // key for key value pair in benchmark attributes
	// Queue and node status strings
	public static String QUEUE_STATUS_ACTIVE = "ACTIVE";					// Active status for an SGE queue (indicates the queue is live)
	public static String QUEUE_STATUS_INACTIVE = "INACTIVE";				// Inactive status for an SGE queue (indicates the queue is not currently live)
	public static String NODE_STATUS_ACTIVE = "ACTIVE";						// Active status for an SGE node (indicates the node is live)
	public static String NODE_STATUS_INACTIVE = "INACTIVE";					// Inactive status for an SGE node (indicates the node is not currently live)
	
	// SGE Configurations
	public static String QUEUE_LIST_COMMAND = "qconf -sql";					// The SGE command to execute to get a list of all job queues
	public static String QUEUE_DETAILS_COMMAND = "qconf -sq ";				// The SGE command to get configuration details about a queue
	public static String QUEUE_USAGE_COMMAND = "qstat -g c";				// The SGE command to get USAGE details about all queues
	public static String QUEUE_STATS_COMMAND = "qstat -f";				// The SGE command to get stats about all the queues
	public static String NODE_LIST_COMMAND = "qconf -sel";					// The SGE command to execute to get a list of all worker nodes
	public static String NODE_DETAILS_COMMAND = "qconf -se ";				// The SGE command to get hardware details about a node	
	public static String NODE_DETAIL_PATTERN = "[^\\s,][\\w|-]+=[^,\\s]+";  // The regular expression to parse out the key/value pairs from SGE's node detail output
	public static String QUEUE_DETAIL_PATTERN = "[\\w|-]+\\s+[^\t\r\n,]+";  // The regular expression to parse out the key/value pairs from SGE's queue detail output
	//public static String QUEUE_ASSOC_PATTERN = "\\[.+=";  					// The regular expression to parse out the nodes that belong to a queue from SGE's queue detail output
	public static String QUEUE_ASSOC_PATTERN = "\\b[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,16}\\b";  // The regular expression to parse out the nodes that belong to a queue from SGE's qstat -f
	public static String STATS_ENTRY_PATTERN = "^([\\w\\d\\.]+:){5}%d:.+$"; // The regular expression to parse out entries in the sge accounting file
	public static String SGE_ACCOUNTING_FILE = null;  						// The absolute path to the SGE accounting file that holds job statistics
	public static int MAX_PAIR_RUNTIME = 86400;  							// The largest possible amount of time a job pair can run before being terminated (in seconds)
	public static int MAX_PAIR_CPUTIME = 86400;  							// The largest possible cpu time a job pair can run before being terminated (in seconds)
	public static long MAX_PAIR_FILE_WRITE = 2097152;  						// The largest possible amount disk space (in kilobytes) a job pair is allowed to use
	public static long MAX_PAIR_VMEM = 768000;  							// The largest possible amount of memory (in kilobytes) a job pair is allowed to use
	public static int NUM_JOB_SCRIPTS = 100;								// The number of job scripts to write/submit each period
}	
