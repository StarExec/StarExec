package org.starexec.backend;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.io.File;
import org.apache.log4j.Logger;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class BackendUtil{
    private static final Logger log = Logger.getLogger(BackendUtil.class);
    protected static final ExecutorService threadPool = Executors.newCachedThreadPool();

   public static String executeCommand(String command) throws IOException {
		String[] cmd = new String[1];
		cmd[0] = command;
		return executeCommand(cmd);
    }

    public static String executeCommand(String command, String[] env) throws IOException {
		String[] cmd = new String[1];
		cmd[0] = command;
		return executeCommand(cmd,env,null);
    }

    /** Convenience method for executeCommand() 
     * @throws IOException */
    public static String executeCommand(String[] command) throws IOException {
    	return executeCommand(command,null,null);
    }

    /**
     * Runs a command on the system command line (bash for unix, command line for windows)
     * and returns the results from the command as a buffered reader which can be processed.
     * MAKE SURE TO CLOSE THE READER WHEN DONE. Null is returned if the command failed.
     * @param command An array holding the command and then its arguments
     * @param envp The environment
     * @param workingDirectory the working directory to use
     * @return A buffered reader holding the output from the command.
     * @throws IOException We do not want to catch exceptions at this level, because this code is generic and
     * has no useful way to handle them! Throwing an exception to higher levels is the desired behavior.
     */
	
    public static String executeCommand(String[] command, String[] envp, File workingDirectory) throws IOException {
    	Runtime r = Runtime.getRuntime();
					
	    Process p;
	    if (command.length == 1) {
		log.debug("Executing the following command: " + command[0]);
			
		p = r.exec(command[0], envp);
	    }
	    else {
		StringBuilder b = new StringBuilder();
		b.append("Executing the following command:\n");
		for (int i = 0; i < command.length; i++) {
		    b.append("  ");
		    b.append(command[i]);
		}

		log.info(b.toString());
			    
		p = r.exec(command, envp, workingDirectory);
	    }

	    return drainStreams(p);

	
    }

    protected static String drainStreams(final Process p) {
	    
	/* to handle the separate streams of regular output and
	   error output correctly, it is necessary to try draining
	   them in parallel.  Otherwise, draining one can block
	   and prevent the other from making progress as well (since
	   the process cannot advance in that case). */
	final StringBuffer b = new StringBuffer();
	threadPool.execute(new Runnable() {
		@Override
		    public void run() {
		    try {
				if (drainInputStream(b,p.getErrorStream()))
					
				    log.error("The process produced stderr output.");
					log.error(b.toString());
			    }
		    catch(Exception e) {
				log.error("Error draining stderr from process: "+e.toString());
		    }
		}
	    });
	drainInputStream(b,p.getInputStream());
	return b.toString();
    }

   /** 
     * drains the given InputStream, adding each line read to the given StringBuffer.
     * @param sb the StringBuffer to which to append lines 
     * @param s the InputStream to drain
     * @return true iff we read a string 
     */
    protected static boolean drainInputStream(StringBuffer sb,InputStream s) {
		InputStreamReader ins = new InputStreamReader(s);
		BufferedReader reader = new BufferedReader(ins);		
	
		boolean readsomething = false;
		String line = null;
		try {
		    while ((line = reader.readLine()) != null) {
			readsomething = true;
			sb.append(line + System.getProperty("line.separator"));
		    }
		    reader.close();
		}
		catch (IOException e) {
		    log.warn("drainInputStream caught: "+e.toString(), e);
		}
		finally {
		    try {
			reader.close();
		    }
		    catch (Exception e) {
			log.warn("Caught exception closing reader while draining streams.");
		    }
		}
		return readsomething;
    }


}