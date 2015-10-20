package util;
import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Pattern;

import org.jfree.util.Log;
import org.starexec.util.Util;

/**
 * RunQstat.java, based on org.starexec.util.GridEngineUtil.setQueueAssociationsInDb()
 * 
 * @author Aaron Stump
 */
public class RunQstat {
	
    public static String QUEUE_ASSOC_PATTERN = "\\b[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,16}\\b";  // The regular expression to parse out the nodes that belong to a queue from SGE's qstat -f
    public static Pattern queueAssocPattern = Pattern.compile(QUEUE_ASSOC_PATTERN, Pattern.CASE_INSENSITIVE);


    public static void qstat() throws IOException {

		System.out.println("Beginning to run qstat and parsing the output.");
	
		String[] envp = new String[2];
		envp[0] = "SGE_LONG_QNAMES=-1"; // this tells qstat not to truncate the names of the nodes, which it does by default
		envp[1] = "SGE_ROOT=/cluster/gridengine-8.1.8"; // it seems we need to set this explicitly if we change the environment.
		String results = Util.executeCommand("qstat -f -u tomcat",envp);
	
		// Parse the output from the SGE call to get the child worker nodes
		java.util.regex.Matcher matcher = queueAssocPattern.matcher(results);
	
		String[] capture;  // string array to store a queue and its associated node
		//Remove all association info from db so stale data isn't displayed
		// For each match...
		while(matcher.find()) {
		    // Parse out the queue and node names from the regex parser and add it to the return list			
		    capture = matcher.group().split("@");
		    System.out.println("queue = " + capture[0]);
		    System.out.println("node = " + capture[1]);
		}
	
		System.out.println("Completed running qstat and parsing the output.");
    }
	
    public static void main(String args[]) throws IOException {
    	qstat();
		System.out.println("qstat complete.");
    }
}