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
	
	// Global path information
	public static String SOLVER_PATH = null;								// The top-level directory in which to save the solver file(s)
	public static String BENCHMARK_PATH = null;								// The top-level directory in which to save the benchmark file(s)
	public static String STAREXEC_ROOT = null;								// The directory of the starexec webapp	
	public static String CONFIG_PATH = null;								// The directory of starexec's configuration and template files relative to the root path
	public static String NODE_WORKING_DIR = null;							// The directory on the local nodes where they can use for scratch space (read/write)
	public static String JOB_INBOX_DIR = null;								// Where to deposit new job scripts until SGE distributes it to a node    	
	public static String BENCH_TYPE_DIR = null;								// Where to deposit new benchmark type processor scripts
	
	// Job Manager (JM) constants
	public static String JOBFILE_FORMAT = null;								// The filename format (with standard java string formatting) for generated jobscript files
	public static String SOLVER_BIN_DIR = null;								// The path to the bin directory to look for runscripts (relative to the solver's toplevel directory)
	public static int NEXT_JID = 1;											// The number of the next Job to be ran
	public static int PAIR_ID = 1;											// The number of the next pair to be ran
	
	// Job status strings
	public static String JOB_STATUS_DONE = "Done";							// The status of a successfully finished job
	public static String JOB_STATUS_RUNNING = "Running";					// The status of a currently running job
	public static String JOB_STATUS_ENQUEUED = "Enqueued";					// The status of a job that SGE has queued up to be run
	public static String JOB_STATUS_ERR = "Error";							// The status of a failed job
	
	// Misc application properties
	public static boolean LOG_TO_CONSOLE = true;							// Whether or not to output log messages to the console
	public static String PWD_HASH_ALGORITHM = "SHA-512";					// Which algorithm to use to hash user passwords
	public static String PATH_DATE_FORMAT = "yyyyMMdd-kk.mm.ss";			// Which datetime format is used to create unique directory names
	public static boolean REMOVE_ARCHIVES = true;							// Whether or not to delete archive files after they're extracted
	public static String CONTACT_EMAIL = "";								// The default e-mail address to use for users to contact for support
	public static int CLUSTER_UPDATE_PERIOD = 60;							// How often (in seconds) to update the cluster's current usage status
	public static long DEFAULT_USER_QUOTA = 52428800;								// The default user disk quota to assign new users; currently 50MB
	
	// Queue and node status strings
	public static String QUEUE_STATUS_ACTIVE = "ACTIVE";					// Active status for an SGE queue (indicates the queue is live)
	public static String QUEUE_STATUS_INACTIVE = "INACTIVE";				// Inactive status for an SGE queue (indicates the queue is not currently live)
	public static String NODE_STATUS_ACTIVE = "ACTIVE";						// Active status for an SGE node (indicates the node is live)
	public static String NODE_STATUS_INACTIVE = "INACTIVE";					// Inactive status for an SGE node (indicates the node is not currently live)
	
	// SGE Configurations
	public static String QUEUE_LIST_COMMAND = "qconf -sql";					// The SGE command to execute to get a list of all job queues
	public static String QUEUE_DETAILS_COMMAND = "qconf -sq ";				// The SGE command to get configuration details about a queue
	public static String QUEUE_USAGE_COMMAND = "qstat -g c";				// The SGE command to get USAGE details about all queues
	public static String NODE_LIST_COMMAND = "qconf -sel";					// The SGE command to execute to get a list of all worker nodes
	public static String NODE_DETAILS_COMMAND = "qconf -se ";				// The SGE command to get hardware details about a node	
	public static String NODE_DETAIL_PATTERN = "[^\\s,][\\w|-]+=[^,\\s]+";  // The regular expression to parse out the key/value pairs from SGE's node detail output
	public static String QUEUE_DETAIL_PATTERN = "[\\w|-]+\\s+[^\t\r\n,]+";  // The regular expression to parse out the key/value pairs from SGE's queue detail output
	public static String QUEUE_ASSOC_PATTERN = "\\[.+=";  					// The regular expression to parse out the nodes that beint to a queue from SGE's queue detail output
}	
