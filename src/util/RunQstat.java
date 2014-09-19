package util;
import java.io.BufferedInputStream;
import java.io.BufferedReader;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Pattern;

import org.jfree.util.Log;

/**
 * RunQstat.java, based on org.starexec.util.GridEngineUtil.setQueueAssociationsInDb()
 * 
 * @author Aaron Stump
 */
public class RunQstat {
	
    public static String QUEUE_ASSOC_PATTERN = "\\b[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,16}\\b";  // The regular expression to parse out the nodes that belong to a queue from SGE's qstat -f
    public static Pattern queueAssocPattern = Pattern.compile(QUEUE_ASSOC_PATTERN, Pattern.CASE_INSENSITIVE);

    protected static final ExecutorService threadPool = Executors.newCachedThreadPool();

    public static String executeCommand(String command, String[] env) {
	String[] cmd = new String[1];
	cmd[0] = command;
	return executeCommand(cmd,env);
    }

    public static String executeCommand(String[] command, String[] envp) {
	Runtime r = Runtime.getRuntime();
		
	BufferedReader reader = null;		
	//
	try {					
	    Process p;
	    if (command.length == 1) {
			
		p = r.exec(command[0], envp);
	    }
	    else {
		StringBuilder b = new StringBuilder();
		b.append("Executing the following command:\n");
		for (int i = 0; i < command.length; i++) {
		    b.append("  ");
		    b.append(command[i]);
		}

			    
		p = r.exec(command, envp);
	    }


	    InputStream in = p.getInputStream();
	    BufferedInputStream buf = new BufferedInputStream(in);
	    InputStreamReader inread = new InputStreamReader(buf);
	    reader = new BufferedReader(inread);		
			
	    //Also handle error stream
	    InputStream err = p.getErrorStream();
	    InputStreamReader inreadErr = new InputStreamReader(err);
	    final BufferedReader errReader = new BufferedReader(inreadErr);
	    StringBuilder sb = new StringBuilder();
			
	    threadPool.execute(new Runnable() {
		    @Override
	            public void run() {
			try {
			    while (errReader.readLine() != null)

			    errReader.close();
			}
			catch(Exception e) {
				//ignore if we can't read in the output correctly
			}
		    }
		});
	    String line = null;
	    while ((line = reader.readLine()) != null)
		sb.append(line + System.getProperty("line.separator"));
	    
	    reader.close();

	    return sb.toString();
	} catch (Exception e) {
		Log.error(e.getMessage(),e);
	}
		
	return null;
    }


    public static void qstat() {

	System.out.println("Beginning to run qstat and parsing the output.");

	String[] envp = new String[2];
	envp[0] = "SGE_LONG_QNAMES=-1"; // this tells qstat not to truncate the names of the nodes, which it does by default
	envp[1] = "SGE_ROOT=/cluster/sge-6.2u5"; // it seems we need to set this explicitly if we change the environment.
	String results = executeCommand("qstat -f -u tomcat",envp);

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
	
    public static void main(String args[]) {
	qstat();
	threadPool.shutdown();
	System.out.println("qstat complete.");
    }
}