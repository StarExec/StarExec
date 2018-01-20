package org.starexec.constants;

import org.starexec.backend.Backend;
import org.starexec.backend.GridEngineBackend;
import org.starexec.backend.LocalBackend;
import org.starexec.backend.OARBackend;
import org.starexec.data.to.enums.BenchmarkingFramework;
import org.starexec.exceptions.StarExecException;

import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;

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

	public static String BACKEND_TYPE = "@BACKEND_TYPE@";
	public static Backend BACKEND;
	static {
		Backend b;
		switch (BACKEND_TYPE) {
		case SGE_TYPE:
			b = new GridEngineBackend();
			break;
		case OAR_TYPE:
			b = new OARBackend();
			break;
		case LOCAL_TYPE:
			b = new LocalBackend();
			break;
		default:
			b = null;
		}
		BACKEND = b;
	}

	//maximum length properties
	public static final int SPACE_NAME_LEN   = @MaxLength.Space.Name@;
	public static int SPACE_DESC_LEN         = @MaxLength.Space.Description@;
	public static int USER_FIRST_LEN         = @MaxLength.User.FirstName@;
	public static int USER_LAST_LEN          = @MaxLength.User.LastName@;
	public static int INSTITUTION_LEN        = @MaxLength.User.Institution@;
	public static int EMAIL_LEN              = @MaxLength.User.Email@;
	public static int PASSWORD_LEN           = @MaxLength.User.Password@;
	public static int MSG_LEN                = @MaxLength.Message@;
	public static int BENCH_NAME_LEN         = @MaxLength.Bench.Name@;
	public static int BENCH_DESC_LEN         = @MaxLength.Bench.Description@;
	public static int CONFIGURATION_NAME_LEN = @MaxLength.Config.Name@;
	public static int CONFIGURATION_DESC_LEN = @MaxLength.Config.Description@;
	public static int SOLVER_NAME_LEN        = @MaxLength.Solver.Name@;
	public static int WEBSITE_NAME_LEN       = @MaxLength.Website.Name@;
	public static int PIPELINE_NAME_LEN      = @MaxLength.Pipeline.Name@;
	public static int SETTINGS_NAME_LEN      = @MaxLength.Settings.Name@;
	public static int SOLVER_DESC_LEN        = @MaxLength.Solver.Description@;
	public static int JOB_NAME_LEN           = @MaxLength.Job.Name@;
	public static int JOB_DESC_LEN           = @MaxLength.Job.Description@;
	public static int URL_LEN                = @MaxLength.URL@;
	public static int PROCESSOR_NAME_LEN     = @MaxLength.Processor.Name@;
	public static int PROCESSOR_DESC_LEN     = @MaxLength.Processor.Description@;
	public static int QUEUE_NAME_LEN         = @MaxLength.Queue.Name@;
	public static int TEXT_FIELD_LEN         = @MaxLength.TextField@;

	public static int CLUSTER_UPDATE_PERIOD  = @Cluster.UpdatePeriod@;

	public enum DefaultSettingAttribute {
		PostProcess,
		BenchProcess,
		CpuTimeout,
		ClockTimeout,
		DependenciesEnabled,
		defaultbenchmark,
		defaultsolver,
		MaxMem,
		// This is the standard way to do enum names. Benchmarking framework needs to be all caps so it's not confused
		// with the enum BenchmarkingFramework.
		BENCHMARKING_FRAMEWORK,
		PreProcess
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
	public static int MAXIMUM_SOLVER_CONFIG_PAIRS = 5;
	public static int MAXIMUM_DATA_POINTS = 30000;
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
	public static String EMAIL_SMTP   = "@Email.Smtp@";
	public static int EMAIL_SMTP_PORT =  @Email.Port@;
	public static String EMAIL_USER   = "@Email.User@";
	public static String EMAIL_PWD    = "@Email.Pass@";

	// MySQL properties
	public static String MYSQL_DATABASE              = "@DB.Name@";         // Name of the MySQL database
	public static String MYSQL_URL                   = "@DB.Url@";          // MySQL connection string for JDBC
	public static String MYSQL_USERNAME              = "@DB.User@";         // Starexec's username for the database
	public static String MYSQL_PASSWORD              = "@DB.Pass@";         // Starexec database password
	public static int MYSQL_POOL_MAX_SIZE            =  @DB.Pool.Max@;      // The maximum number of connections in the database pool
	public static int MYSQL_POOL_MIN_SIZE            =  @DB.Pool.Min@;      // The minimum number of connections to keep open to the database
	public static String COMPUTE_NODE_MYSQL_USERNAME = "@Cluster.DB.User@"; // username for database to use from compute nodes
	public static String COMPUTE_NODE_MYSQL_PASSWORD = "@Cluster.DB.Pass@"; // DB password for COMPUTE_NODE_MYSQL_USERNAME
	public static String REPORT_HOST                 = "@Cluster.DB.Url@";  // where to report job status updates during jobs
	public static String MYSQL_DRIVER = "com.mysql.jdbc.Driver"; // MySQL java driver class (we use JDBC)

	// Global path information
	public static String SOLVER_BUILD_OUTPUT = "starexec_build_log";           // The name of the file in which we're storing build output
	public static String STAREXEC_ROOT = "@STAREXEC_ROOT@";                    // The directory of the starexec webapp
	public static String CONFIG_PATH = "@Config.Path@";                        // The directory of starexec's configuration and template files relative to the root path
	public static String RUNSOLVER_PATH = "@Runsolver.Path@";                  // The absolute filepath to the runsolver executable
	public static String STAREXEC_DATA_DIR = "@data_dir@";                     // the root of the data directory (where jobin/, jobout/, and dirs for primitive are)
	public static String DOWNLOAD_FILE_DIR = "@Web.Download.Directory@";       // Where to temporarily store processed files for downloading. Relative to webapp root
	public static String SPACE_XML_SCHEMA_RELATIVE_LOC = "@Web.Schema.Space@"; // Where the schema for batch space xml is located, relative to STAREXEC_ROOT.
	public static String JOB_XML_SCHEMA_RELATIVE_LOC = "@Web.Schema.Job@";
	public static String STAREXEC_URL_PREFIX = "@Web.Schema.Url@";             //either "https" or "http"
	public static String JOBGRAPH_FILE_DIR = "@Web.JobGraph.Path@";            // Location to store job graph image files. Relative to webapp root.
	public static String SANDBOX_DIRECTORY = "@SANDBOX_DIRECTORY@";            //the sandbox directory for doing processing / building on the head node

	//Admin user info
	public static int ADMIN_USER_ID = 9;                               //user id to use when administrator
	public static String ADMIN_USER_PASSWORD = "admin";
	public static String DEFAULT_QUEUE_NAME = "all.q";                 //The name of the default queue
	public static int DEFAULT_QUEUE_ID=1;
	//Test info
	public static int TEST_COMMUNITY_ID=@TEST_COMMUNITY_ID@;
	public static boolean ALLOW_TESTING=@ALLOW_TESTING@;               // whether tests should be allowed to run on this instance. False for production.
	//Public user info
	public static int PUBLIC_USER_ID = 0;                              //user id to use when writing benchmarks, submitting jobs without login
	public static int PUBLIC_CPU_LIMIT = 30;
	public static int PUBLIC_CLOCK_TIMEOUT = 30;
	public static String PUBLIC_USER_EMAIL = "public";
	public static String PUBLIC_USER_PASSWORD ="public";
	// Job Manager (JM) constants
	public static String JOBFILE_FORMAT = "job_%d.bash";               // The filename format (with standard java string formatting) for generated jobscript files
	public static String DEPENDFILE_FORMAT = "depend_%d.txt";          // The filename format for dependencies
	public static String SOLVER_BIN_DIR = "/bin";                      // The path to the bin directory to look for runscripts (relative to the solver's toplevel directory)
	public static String SANDBOX_USER_ONE = "@SANDBOX_USER_ONE@";      // name of user that executes jobs in sandbox one
	public static String SANDBOX_USER_TWO = "@SANDBOX_USER_TWO@";      // name of user that executes jobs in sandbox two
	// Misc application properties
	public static String STAREXEC_SERVERNAME = "@STAREXEC_SERVERNAME@";
	public static String STAREXEC_APPNAME = "@STAREXEC_APPNAME@";
	public static String PWD_HASH_ALGORITHM = "SHA-512";               // Which algorithm to use to hash user passwords
	public static String PATH_DATE_FORMAT = "yyyyMMdd-kk.mm.ss.SSS";   // Which datetime format is used to create unique directory names
	public static boolean REMOVE_ARCHIVES = true;                      // Whether or not to delete archive files after they're extracted
	public static String CONTACT_EMAIL = "@CONTACT_EMAIL@";            // The default e-mail address to use for users to contact for support
	public static boolean IS_FULL_STAREXEC_INSTANCE = true;            // should we run job tasks (see app/Starexec.java)
	public static int CLEAR_JOB_LOG_PERIOD = 14;                       // How often (in days) to clear job logs
	public static int JOB_SUBMISSION_PERIOD = @Job.SubmissionPeriod@;  // How often (in seconds) to write job scripts and submit to the backend
	public static final int MAX_NUMBER_OF_REPORTS_TO_SEND = 30;        // Maximum number of StarExec report emails to send every period
	public static final int WAIT_TIME_BETWEEN_EMAILING_REPORTS = 2;    // Number of seconds to wait between reports being sent
	public static final int EMAIL_REPORTS_DAY = Calendar.THURSDAY;     // Day of the week to email reports
	public static HashMap<Integer,HashMap<String,Long>> COMM_INFO_MAP = null;
	public static Long COMM_ASSOC_LAST_UPDATE = null;    //last time community_assoc table was updated (milliseconds)
	public static long COMM_ASSOC_UPDATE_PERIOD = 21600000;  //how much time we should wait before requerying for community_assoc table, currentely set to a 10 seconds (milliseconds)
	public static long DEFAULT_DISK_QUOTA = @User.DefaultDiskQuota@L;          // The default user disk quota to assign new users; currently 50MB
	public static int DEFAULT_PAIR_QUOTA = 750000;                            // The default max number of pairs a user should be able to own
	public static String PERSONAL_SPACE_DESCRIPTION =                            // The default text that appears at the top of a user's personal space
			"this is your personal space";
	public static int MAX_FAILED_VALIDATIONS = 50;                            //More than this number of benchmark validation failures triggers a message and ends
	public static String VALID_BENCHMARK_ATTRIBUTE = "starexec-valid"; //Name of attribute given by benchmark processors to show a benchmark is valid
	//Reserved Names for users
	public static String STAREXEC_RESULT = "starexec-result";                            // The key used for the starexec result in key-value pairs for a job pair
	public static final String DEFAULT_QUEUE_SLOTS = "2";              // By default we assume there will be two pairs per node and so we divide the memory into two parts for each pair.
	public static String CONFIGURATION_PREFIX = "starexec_run_";       // The prefix for a file in the solver bin directory to be considered a configuration
	public static String EXPECTED_RESULT = "starexec-expected-result"; // key for key value pair in benchmark attributes
	public static String SOLVER_DESC_PATH = "starexec_description.txt";// File that can be included within the archive solver file to include the description
	public static String SOLVER_BUILD_SCRIPT="starexec_build";
	public static String UPLOAD_TEST_JOB_XML="run_on_upload.xml";

	public static String PROCESSOR_RUN_SCRIPT="process";
	public static String BENCHMARK_DESC_PATH = "starexec_description.txt";  // File that can be included within the archive solver file to include the description
	public static String DESC_PATH = "starexec_description.txt";
	public static String STAREXEC_UNKNOWN="starexec-unknown";          // Result that indicates a pair should not be counted as wrong
	// Queue and node status strings

	public static String QUEUE_STATUS_ACTIVE = "ACTIVE";                            // Active status for a backend queue (indicates the queue is live)
	public static String QUEUE_STATUS_INACTIVE = "INACTIVE";                            // Inactive status for a backend queue (indicates the queue is not currently live)
	public static String NODE_STATUS_ACTIVE = "ACTIVE";                            // Active status for a backend node (indicates the node is live)
	public static String NODE_STATUS_INACTIVE = "INACTIVE";                            // Inactive status for a backend node (indicates the node is not currently live)

	public static final String buildVersion = "@buildVersion@";
	public static final String buildUser = "@buildUser@";
	public static final Date buildDate;
	static {
		Date d = null;
		try {
			SimpleDateFormat fmt = new SimpleDateFormat("MM/dd/yyyy hh:mm:ss aa");
			d = fmt.parse("@buildDate@");
		} catch (Exception e) {}
		buildDate = d;
	}

	// BACKEND configurations
	public static String BACKEND_ROOT = "@Backend.Root@"; // root directory for the backend executable
	public static String BACKEND_WORKING_DIR = "@Backend.WorkingDir@";
	public static long MAX_PAIR_FILE_WRITE = @JobPair.MaxFileWrite@;      // The largest possible amount disk space (in kilobytes) a job pair is allowed to use
	public static long DEFAULT_PAIR_VMEM = 17179869184L;                              // The default limit on memory (in bytes) for job pairs
	public static int NODE_MULTIPLIER = 8;                                  // The number of job scripts to submit is the number of nodes in the queue times this

	public static int MAX_STAGES_PER_PIPELINE = 10000;
	public static int NUM_JOB_PAIRS_AT_A_TIME = 5;  // the number of job pairs from a job to submit at the same time, as we cycle through all jobs submitting pairs.
	public static int NUM_REPOSTPROCESS_AT_A_TIME = 200; // number of job pairs to re-postprocess at a time with our periodic task
	public static int DEFAULT_MAX_TIMEOUT = 259200;

	/* Maximum Runsolver kill-delay */
	public static final int MAX_KILL_DELAY = 120;

	/* Minimum interval for jobs that report incremental results.
	 * Incremental reporting will be disabled beneth this threshold. */
	public static final int MINIMUM_RESULTS_INTERVAL = 10;

	// The number of minutes that is considered ok for a pair to be enqueued but not running.
	public static int PAIR_ENQUEUE_TIME_THRESHOLD = 1440;

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
	public static String PROXY_ADDRESS   = "@Proxy.Address@";
	public static int PROXY_PORT         =  @Proxy.Port@;
	public static String HTTP_PROXY_HOST = "@Proxy.URL@";
	public static String HTTP_PROXY_PORT = "@Proxy.Port@";

	public static boolean DEBUG_MODE_ACTIVE = false;

	//names of primitive types
	public static final String SOLVER="solver";
	public static final String BENCHMARK="bench";
	public static final String CONFIGURATION="config";
	public static final String SPACE_XML = "spaceXML";
	public static final String JOB_XML = "jobXML";
	public static final String PAIR_OUTPUT = "jp_output";
	public static final String JOB = "job";
	public static final String JOB_OUTPUT = "j_outputs";
	public static final String SPACE = "space";
	public static final String PROCESSOR = "proc";
	public static final String JOB_OUTPUTS = "jp_outputs";
	public static final String USER = "user";
	public static final String SOLVER_SOURCE = "solverSrc";

	//
	public static final String CONFIG_ID_ATTR = "config-id";
	public static final String CONFIG_NAME_ATTR = "config-id";

	public static final String ANONYMIZE_ALL = "all";
	public static final String ANONYMIZE_ALL_BUT_BENCH = "allButBench";
	public static final String ANONYMIZE_NONE = "none";

	// 2 years
	public static final int MAX_AGE_OF_ANONYMOUS_LINKS_IN_DAYS = 365 * 2;

	// Constants for BenchExec
	public static final BenchmarkingFramework DEFAULT_BENCHMARKING_FRAMEWORK = BenchmarkingFramework.RUNSOLVER;
	public static final String BENCHMARKING_FRAMEWORK_OPTION = "benchmarkingFramework";

	public static final String XML_BENCH_FRAMEWORK_ELE_NAME = "bench-framework";

	public static final String COPY_TO_STARDEV_USERNAME_PARAM = "username";
	public static final String COPY_TO_STARDEV_PASSWORD_PARAM = "password";
	public static final String COPY_TO_STARDEV_SPACE_ID_PARAM = "spaceId";
	public static final String COPY_TO_STARDEV_COPY_WITH_PROC_PARAM = "copyWithProcessor";
	public static final String COPY_TO_STARDEV_PROC_ID_PARAM = "procId";


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
