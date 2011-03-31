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
	public static String SOLVER_PATH = "/home/starexec/Solvers/";				// The directory in which to save the solver file(s)
	public static String BENCHMARK_PATH = "/home/starexec/Solvers/Benchmarks/";		// The directory in which to save the benchmark file(s)
	public static final String JOBSCRIPT_PATH = "/home/starexec/Scripts/";
    
    // Benchmark XML constants
    public static final String BXML_OUTPUT_NAME = "output.xml";						// The output name for benchmark xml files    
	public static final String BXML_DIR_NAME = "dir";
	public static final String BXML_BENCH_NAME = "bench";
	public static final String BXML_NAME_ATTR = "name";
}
