package constants;

/**
 * Class which holds static resources (R) available for use
 * throughout the entire application. This will include many
 * constant strings and numbers that other classes rely on.
 * @author Tyler Jensen
 */
public class R {
	// String keys to properties in the web.xml file for MySQL
	// TODO: Crate a new MySQL account with limited privleges
	public static final String MYSQL_URL = "jdbc:mysql://localhost/starexec";
	public static final String MYSQL_USERNAME = "root";
	public static final String MYSQL_PASSWORD = "star3x3c2011!";
	
	// Global path information
	public static final String SOLVER_PATH = "/home/starexec/Solvers/";				// The directory in which to save the solver file(s)
	public static final String BENCHMARK_PATH = "/home/starexec/Solvers/Benchmarks/";		// The directory in which to save the benchmark file(s)
	public static final String JOBSCRIPT_PATH = "/home/starexec/Scripts/";
	
	//Tyler's local debug paths
    //public static final String SOLVER_PATH = "C:\\Users\\Tyler\\Desktop\\";			// The directory in which to save the solver file(s)
    //public static final String BENCHMARK_PATH = "C:\\Users\\Tyler\\Desktop\\Benchmarks\\";	// The directory in which to save the benchmark file(s)
    
    public static final String XML_OUTPUT_NAME = "output.xml";						// The output name for benchmark xml files
}
