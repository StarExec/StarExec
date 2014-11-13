package org.starexec.util;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.LineIterator;
import org.apache.commons.io.filefilter.FileFilterUtils;
import org.apache.commons.io.filefilter.IOFileFilter;
import org.apache.log4j.Logger;
import org.apache.tomcat.util.http.fileupload.FileItem;
import org.apache.tomcat.util.http.fileupload.FileItemFactory;
import org.apache.tomcat.util.http.fileupload.disk.DiskFileItemFactory;
import org.apache.tomcat.util.http.fileupload.servlet.ServletFileUpload;
import org.starexec.constants.R;
import org.starexec.data.database.Benchmarks;
import org.starexec.data.database.Cache;
import org.starexec.data.database.Communities;
import org.starexec.data.database.Jobs;
import org.starexec.data.database.JobPairs;
import org.starexec.data.database.Solvers;
import org.starexec.data.database.Spaces;
import org.starexec.data.database.Users;
import org.starexec.data.to.Benchmark;
import org.starexec.data.to.Job;
import org.starexec.data.to.Solver;
import org.starexec.data.to.Space;
import org.starexec.data.to.User;
import org.starexec.data.to.JobPair;
import org.starexec.test.TestUtil;

public class Util {	
    private static final Logger log = Logger.getLogger(Util.class);
    protected static final ExecutorService threadPool = Executors.newCachedThreadPool();


    /**
     * Finds the standard output of a job pair and returns it as a string. Null
     * is returned if the output doesn't exist or cannot be found
     * @param limit The max number of characters to return
     * @param pair The pair to get output for
     * @return All console output from a job pair run for the given pair
     */
    public static String getStdOut(JobPair pair, int limit) {
    	pair = JobPairs.getPairDetailed(pair.getId());
    	return Util.getStdOut(pair.getId(),limit);
    }

    /**
     * Finds the standard output of a job pair and returns it as a string. Null
     * is returned if the output doesn't exist or cannot be found
     * @param jobId The id of the job the pair is apart of
     * @param pairId The pair to get output for
     * @param limit The maximum number of lines to return
     * @param path The path to the job pair file
     * @return All console output from a job pair run for the given pair
     */
    public static String getStdOut(int pairId,int limit) {		
    	File stdoutFile = Util.getStdOutFile(pairId);		
    	return Util.readFileLimited(stdoutFile, limit);
    }

    /**
     * Finds the standard output of a job pair and returns its file.
     * @param jobId The id of the job the pair is apart of
     * @param pairId The pair to get output for
     * @param path The space path to the job pair file
     * @return All console output from a job pair run for the given pair
     */
    public static File getStdOutFile(int pairId) {	
    	String stdoutPath=JobPairs.getFilePath(pairId);
    	log.info("The stdoutPath is: " + stdoutPath);

    	return (new File(stdoutPath));	
    }
    
    /**
     * Checks to see if the two given objects are equal without throwing any null pointers.
     * if a and b are both null, returns true
     * @param a 
     * @param b
     * @return
     */
    public static boolean objectsEqual(Object a, Object b) {
    	if (a==null && b==null) {
    		return true;
    	} else if (a==null) {
    		return false;
    	} else {
    		return a.equals(b);
    	}
    	
    }
	
    /**
     * Gives back a String that is the contents of the first n lines of the file where n always less
     * than or equal to lineLimit
     * @param f The file to read
     * @param lineLimit The maximum number of lines to read (anything less than 0 indicates no limit)
     * @return The contents of the file as a String (null if it could not be found)
     */
    public static String readFileLimited(File f, int lineLimit) {
	LineIterator lineItr = null;
		
	try {
	    // Set limit to max if it's less than 0 (anything less than 0 inclusive indicates no limit)
	    lineLimit = Math.min(lineLimit, Integer.MAX_VALUE);
			
	    // If we found the correct std out file...
	    if(f.exists()) {
		// Create a buffer to store the lines in and an iterator to iterate over the lines
		StringBuilder sb = new StringBuilder();
		lineItr = FileUtils.lineIterator(f);
		int i = 0;
				
		// While there are more lines in the file...
		while (lineItr.hasNext()) {
		    // If we've reached the line limit, break out, we're done.
		    if(i++ == lineLimit) {
			break;
		    }
					
		    // If we're still under the limit, add the line to the buffer
		    sb.append(lineItr.nextLine());
					
		    // Don't forget to add a new line, since they are stripped as they are read
		    sb.append("\n");
		}
				
		// Return the buffer
		return sb.toString();
	    } else {
		// If the file doesn't exist...
		log.warn("Could not find file to open: " + f.getAbsolutePath());
	    }
	} catch (Exception e) {
	    log.warn(e.getMessage(), e);
	} finally {
	    // Release the line iterator without potential error
	    LineIterator.closeQuietly(lineItr);
	}
		
	return null;
    }
    /**
     * Determines whether we are currently running on production.
     * @return
     */
    public static boolean isProduction() {
	if (R.STAREXEC_SERVERNAME.equalsIgnoreCase("www.starexec.org")) {
	    return true; 
	}
	return false;
    }
	
    /** 
     * execute the following Runnable using a thread from our cached thread pool
     * @param c the Runnable to execute
     * @author Aaron Stump
     */
    public static void threadPoolExecute(Runnable c) {
	threadPool.execute(c);
    }
    /**
     * Shuts down the reserved threadpool this util uses.
     */
    public static void shutdownThreadPool() throws Exception {
	threadPool.shutdown();
	threadPool.awaitTermination(2, TimeUnit.SECONDS);
    }


    /**
     * Returns a File object representing the sandbox directory for the headnode
     * @return the File object
     * @author Eric Burns
     */
    public static File getSandboxDirectory() {
    	return new File(R.SANDBOX_DIRECTORY);
    }
	
    /**
     * Ensures a number is within a given range
     * @param min The minimum value the given value can be
     * @param max The maximum value the given value can be
     * @param value The actual value to clamp
     * @return min if value is less than min, max if value is 
     * greater than max, or value if it is between min and max
     */
    public static int clamp(int min, int max, int value) {
	return Math.max(Math.min(value, max), min);
    }
	
    public static long clamp(long min, long max, long value) {
	if (value<min) {
	    return min;
	}
	if (value > max) {
	    return max;
	}
	return value;
    }
	
    public static void initializeDataDirectories() {
	File file=new File(R.STAREXEC_DATA_DIR);
	file.mkdir();
		
	file=new File(R.JOB_INBOX_DIR);
	file.mkdir();
	file=new File(R.JOB_LOG_DIR);
	file.mkdir();
	file=new File(R.JOB_OUTPUT_DIR);
	file.mkdir();
	file=new File(R.JOB_OUTPUT_DIR);
	file.mkdir();
	file=new File(R.JOBPAIR_INPUT_DIR);
	file.mkdir();
	file=new File(R.BENCHMARK_PATH);
	file.mkdir();
	file=new File(R.SOLVER_PATH);
	file.mkdir();
	file=new File(R.SOLVER_BUILD_OUTPUT_DIR);
	file.mkdir();
	file=new File(R.PROCESSOR_DIR);
	file.mkdir();
	file=new File(R.NEW_JOB_OUTPUT_DIR);
	file.mkdir();
	file=new File(R.PICTURE_PATH);
	file.mkdir();
		
		
		
	File downloadDir=new File(R.STAREXEC_ROOT,R.DOWNLOAD_FILE_DIR);
	downloadDir.mkdirs();
	File cacheDir=new File(R.STAREXEC_ROOT,R.CACHED_FILE_DIR);
	cacheDir.mkdirs();
	File graphDir=new File(R.STAREXEC_ROOT,R.JOBGRAPH_FILE_DIR);
	graphDir.mkdirs();
    }
	
    /**
     * Extracts the file extesion from a file path
     * @param s The file path
     * @return The extension of the file
     */
    public static String getFileExtension(String s){
	return s.substring(s.lastIndexOf('.') + 1);
    }
	
    /**
     * @return The platform-dependent line separator
     */
    public static String getLineSeparator(){
	return System.getProperty("line.separator");
    }
	
	
	
    public static boolean paramExists(String name, HttpServletRequest request){
	return !isNullOrEmpty(request.getParameter(name));
    }
	
    public static boolean isNullOrEmpty(String s){
	return (s == null || s.trim().length() <= 0);
    }	
	
    /**
     * Generates a temporary password of between 6-20 characters, with at least 4 letters,
     * 1 number, and 1 special character
     * character
     * 
     * @return a temporary password
     */
    public static String getTempPassword() {
        Random r = new Random();
        
        // Random temp password length between 6-20 characters
        int newPassLength = r.nextInt(15) + 6;
        int set = 0;
        String[] charSets = {"ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz", "0123456789", "`~!@#$%^&*()_+-="};
        StringBuffer sb = new StringBuffer();
        
        // Hash to store which character sets have been used
        HashSet<Integer> setsUsed = new HashSet<Integer>();
        
        while (sb.length() != newPassLength) {
	    // Choose a random character set to use & get a random character from it
            set = r.nextInt(charSets.length);
            setsUsed.add(set);
            sb.append(charSets[set].charAt(r.nextInt(charSets[set].length())));
            
            // By the end, if the temporary password doesn't contain a character
            // from all character sets, restart and generate a new temporary password 
            if (sb.length() == newPassLength && setsUsed.size() != 3) {
                sb.delete(0, sb.length());
                setsUsed.clear();
            }
        }
        return sb.toString();
    }

    /**
     * Parses a multipart request and returns a hashmap of form parameters
     * @param request The request to parse
     * @return A hashmap containing the field name to field value mapping
     */
    public static HashMap<String, Object> parseMultipartRequest(HttpServletRequest request) throws Exception {
		// Use Tomcat's multipart form utilities
		FileItemFactory factory = new DiskFileItemFactory();
		ServletFileUpload upload = new ServletFileUpload(factory);
		List<FileItem> items = upload.parseRequest(request);
		HashMap<String, Object> form = new HashMap<String, Object>();
			
		for(FileItem f : items) {
		    // If we're dealing with a regular form field...
		    if(f.isFormField()) {
			// Add the field name and field value to the hashmap
			form.put(f.getFieldName(), f.getString());				
		    } else {
			// Else we've encountered a file, so add the FileItem to the hashmap
			form.put(f.getFieldName(), f);					
		    }	
		}
			
		return form;
    }
	
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
    
    public static String executeSandboxCommand(String[] command) throws IOException {
    	return executeSandboxCommand(command,null,null);
    }
    
    public static String executeSandboxCommand(String[] command, String[] envp, File workingDirectory) throws IOException {
    	String[] newCommand=new String[command.length+3];
    	newCommand[0]="sudo";
    	newCommand[1]="-u";
    	newCommand[2]="sandbox";
    	for (int i=0;i<command.length;i++) {
    		newCommand[i+3]=command[i];
    	}
    	return  executeCommand(newCommand,envp,workingDirectory);
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
	

    protected static String drainInputStream(InputStream s) {
		InputStreamReader ins = new InputStreamReader(s);
		BufferedReader reader = new BufferedReader(ins);		
	
		String line = null;
		StringBuilder sb = new StringBuilder();
		try {
		    while ((line = reader.readLine()) != null)
			sb.append(line + System.getProperty("line.separator"));
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
		return sb.toString();
    }


    protected static String drainStreams(final Process p) {
	    
	/* to handle the separate streams of regular output and
	   error output correctly, it is necessary to try draining
	   them in parallel.  Otherwise, draining one can block
	   and prevent the other from making progress as well (since
	   the process cannot advance in that case). */
	threadPool.execute(new Runnable() {
		@Override
		    public void run() {
		    try {
			String es = drainInputStream(p.getErrorStream());
			if (es.length() > 0)
			    log.error("stderr from process follows:\n"+es);
		    }
		    catch(Exception e) {
			log.error("Error draining stderr from process: "+e.toString());
		    }
		}
	    });
	return drainInputStream(p.getInputStream());
    }

	
    /**
     * Takes in a string buffer and produces a single string out of its contents. This method
     * will attempt to close the reader when finished.
     * @param reader The reader to convert
     * @return The string value that is the result of appending all lines within the buffer.
     */
    public static String bufferToString(BufferedReader reader) {
		try {
		    StringBuilder sb = new StringBuilder();
				
		    String line;		
		    while((line = reader.readLine()) != null) {
			sb.append(line + Util.getLineSeparator());
		    }
				
		    return sb.toString();
		} catch (Exception e) {
		    log.warn(e.getMessage(), e);
		} finally {
		    // Try to safely close the reader
		    try { reader.close(); } catch (Exception e) {}
		}
			
		return null;
    }

    /**
     * Converts a list of strings into a list of ints
     * @param stringList The list of numeric strings to convert to ints
     * @return A list of ints parsed from the string list
     */
    public static List<Integer> toIntegerList(String[] stringList) {
	if (stringList != null) {
	    ArrayList<Integer> retList = new ArrayList<Integer>(stringList.length);
		
	    for(String s : stringList){
		retList.add(Integer.parseInt(s));
	    }
		
	    return retList;
	}
		
	return new ArrayList<Integer>();
    }
	
    /**
     * Normalizes all line endings in the given file to the line ending of the OS the JVM is running on
     * @param f The file to normalize
     */
    public static void normalizeFile(File f) {		
	File temp = null;
	BufferedReader bufferIn = null;
	BufferedWriter bufferOut = null;		
		
	try {			
	    if(f.exists()) {
		// Create a new temp file to write to
		temp = new File(f.getAbsolutePath() + ".normalized");
		temp.createNewFile();
						
		// Get a stream to read from the file un-normalized file
		FileInputStream fileIn = new FileInputStream(f);
		DataInputStream dataIn = new DataInputStream(fileIn);
		bufferIn = new BufferedReader(new InputStreamReader(dataIn));
				
		// Get a stream to write to the noramlized file
		FileOutputStream fileOut = new FileOutputStream(temp);
		DataOutputStream dataOut = new DataOutputStream(fileOut);
		bufferOut = new BufferedWriter(new OutputStreamWriter(dataOut));
				
		// For each line in the un-normalized file
		String line;
		while ((line = bufferIn.readLine()) != null) {
		    // Write the original line plus the operating-system dependent newline
		    bufferOut.write(line);
		    bufferOut.newLine();								
		}
			
		bufferIn.close();
		bufferOut.close();
				
		// Remove the original file
		f.delete();
				
		// And rename the original file to the new one
		temp.renameTo(f);
	    } else {
		// If the file doesn't exist...
		log.warn("Could not find file to open: " + f.getAbsolutePath());
	    }
	} catch (Exception e) {
	    log.warn(e.getMessage(), e);
	} finally {
	    // Clean up, temp should never exist
	    FileUtils.deleteQuietly(temp);
	    IOUtils.closeQuietly(bufferIn);
	    IOUtils.closeQuietly(bufferOut);
	}
    }

	
    public static String makeCommaSeparatedList(List<Integer> nums) {
    	StringBuilder sb=new StringBuilder();
    	for (Integer id : nums) {
    		sb.append(id);
    		sb.append(",");
    	}
    	sb.delete(sb.length()-1, sb.length());
    	return sb.toString(); 
    	
    }
    
    /**
     * Deletes all files in the given directory that are as old as, or older than the specified number of days
     * @param directory The directory to clear old files out of (non-recursive)
     * @param daysAgo Files older than this many days ago will be deleted
     */
    public static void clearOldFiles(String directory, int daysAgo,boolean includeDirs){
	try {
	    File dir = new File(directory);
			
	    if(!dir.exists()) {
		return;
	    }
			
	    // Subtract days from the current time
	    Calendar calendar = Calendar.getInstance();
	    calendar.add(Calendar.DATE, -daysAgo);			
	    // Create a new filter for files older than this new time
	    IOFileFilter dateFilter = FileFilterUtils.ageFileFilter(calendar.getTime());
	    Collection<File> outdatedFiles;
	    // Get all of the outdated files
	    if (!includeDirs) {
		    outdatedFiles = FileUtils.listFiles(dir, dateFilter, null);
		    
	    } else {
	    	IOFileFilter dateDirFilter=FileFilterUtils.makeDirectoryOnly(dateFilter);
	    	
	    	File[] files=dir.listFiles((FileFilter)dateFilter);
	    	outdatedFiles=new ArrayList<File>();
	    	for (File f : files) {
	    		outdatedFiles.add(f);
	    	}
	    }
	    log.debug("found a total of "+outdatedFiles.size() +" outdated files to delete in "+directory);
	    // Remove them all
	    for(File f : outdatedFiles) {
		FileUtils.deleteQuietly(f);
	    }					
	} catch (Exception e) {
	    log.warn(e.getMessage(), e);
	}
    }
    /** Deletes all the cached files that have not been accessed in the given amount of days
     * @daysSinceLastAccess The number of days a file should have gone without being accessed to delete
     * @author Eric Burns
     */
    public static void clearOldCachedFiles(int daysSinceLastAccess) {
	log.debug("calling clearOldCachedFiles (periodic)");
	try {
	    Cache.deleteOldPaths(daysSinceLastAccess);
	} catch (Exception e) {
	    log.error("clearOldCachedFiles says "+e.getMessage(),e);
	}
		
    }
	
	
    /**
     * Returns a configuration's absolute file path given the solver's path and 
     * the configuration's name
     *
     * @param solverPath the absolute path to the solver's directory
     * @param configName the configuration's name (which is also the filename)
     * @return null if the solver path or configuration's name are null or empty, otherwise
     * this returns the absolute path to the given configuration's file on disk
     * @author Todd Elvers
     */
    public static String getSolverConfigPath(String solverPath, String configName){
	if(isNullOrEmpty(solverPath) || isNullOrEmpty(configName)){
	    return null;
	}
		
	StringBuilder sb = new StringBuilder();
	sb.append(solverPath);			// Path = .../solvers/{user_id}/{solver_name}/{unique_timestamp}/
	sb.append(R.SOLVER_BIN_DIR);	// Path = .../solvers/{user_id}/{solver_name}/{unique_timestamp}/bin
	sb.append(File.separator);		// Path = .../solvers/{user_id}/{solver_name}/{unique_timestamp}/bin/
	// Append 'run_' prefix to the configuration's filename if it isn't already there
	if(false == configName.startsWith(R.CONFIGURATION_PREFIX)){
	    sb.append(R.CONFIGURATION_PREFIX);
	}
	sb.append(configName);			// Path = .../solvers/{user_id}/{solver_name}/{unique_timestamp}/bin/{starexec_run_configName}
	return sb.toString();
    }
	
    private static String docRoot = null;
    private static String docRootUrl = null;
    
    
    private static void initDocRoot() {
    	if (docRoot == null) {
	    docRoot = "/" + R.STAREXEC_APPNAME + "/";
    	}
    }
    
    private static void initDocRootUrl() {
    	initDocRoot();
    	if (docRootUrl == null) {
	    docRootUrl = R.STAREXEC_URL_PREFIX+"://" + R.STAREXEC_SERVERNAME + docRoot;
    	}
    }
    /**
     * Prepend the document root to the given path, to form a site root-relative path.
     *
     * @author Aaron Stump
     */
    public static String docRoot(String s) {
    	initDocRoot();
    	return docRoot + s;
    }
    /**
     * Prepend the "https://", the server name, and the document root, to form an absolute path (URL).
     *
     * @author Aaron Stump
     */
    public static String url(String s) {
    	initDocRootUrl();
    	return docRootUrl + s;
    }
    
    /**
     * Given an integer number of bytes, returns a human-readable string
     * approximating the number of bytes given with the proper units
     * @param bytes The number of bytes
     * @return The number of bytes to two decimal places in a useful unit
     * @author Eric Burns 
     */
    public static String byteCountToDisplaySize(long bytes) {
    	String[] suffix=new String[]{"Bytes", "KB", "MB", "GB", "TB", "PB", "EB"};
    	int suffixIndex=0;
    	double b=(double)bytes;
    	while (b>1024) {
	    suffixIndex+=1;
	    b=b/1024;
    	}
    	DecimalFormat df=new DecimalFormat("#.##");
    	
    	return df.format(b) +" "+suffix[suffixIndex];
    }
    /**
     * Converts gigabytes to bytes.
     * @param gigabytes
     * @return
     */
    public static long gigabytesToBytes(double gigabytes) {
    	long bytes=(long)(1073741824*gigabytes);
    	return bytes;
    }
    /**
     * Converts bytes to megabytes, truncated to the nearest integer megabyte
     * @param bytes
     * @return
     */
    public static long bytesToMegabytes(long bytes) {
    	return (bytes / (1024*1024));
    }
    
    
    public static double bytesToGigabytes(long bytes) {
    	return ((double)bytes/1073741824.0);
    }
    
    /**
     * Attempts to delete the directory or file specified the given path without
     * throwing any errors
     * @param path The path to the directory to delete
     * @return True on success, false otherwise
     * @author Eric Burns
     */
    public static boolean safeDeleteDirectory(String path) {
    	try {
	    File file=new File(path);
	    if (file.isDirectory()) {
		FileUtils.deleteDirectory(file);
	    } else {
		FileUtils.deleteQuietly(file);
	    }
    	} catch (Exception e) {
	    log.error("safeDeleteDirectory says "+e.getMessage(),e);
    	}
    	return false;
    }
    
    /**
     * Given a list, a comparator, and all of the attributes needed to paginate a DataTables object,
     * returns a sublist of the given list containing the ordered items to display
     * @param arr List to sort
     * @param compare Comparator object that will be used to determine the ordering of objects during sorting
     * @param start Record to start on
     * @param records Number of records to give back (actual number will be less if the size of the list is less than records)
     * @param asc True if sort is ascending, false otherwise
     * @return
     */
    
    public static <T> List<T> handlePagination(List<T> arr, Comparator<T> compare,int start, int records, boolean asc) {
    	Collections.sort(arr,compare);

		if (!asc) {
			Collections.reverse(arr);
		}

		List<T> returnList=new ArrayList<T>();
		if (start>=arr.size()) {
			//we'll just return nothing
		} else if (start+records>arr.size()) {
			returnList = arr.subList(start, arr.size());
		} else {
			 returnList = arr.subList(start,start+records);
		}
		return returnList;
    }
    
    
    /**
     * Recursively grants full permission to the owner of everything in the given
     * directory. The top level directory is not affected, only everything inside
     * @param dir
     * @param group If true, does chmod g+rwx. If false, does chmod u+rwx. The former is used
     * to give Tomcat permission to work with files owned by Sandbox, and the latter is used
     * to allow Sandbox to access its own files.
     * @throws IOException
     */
    public static void sandboxChmodDirectory(File dir,boolean group) throws IOException {
    	//give sandbox full permissions over the solver directory
		String[] chmod=new String[7];
		chmod[0]="sudo";
		chmod[1]="-u";
		chmod[2]="sandbox";
		chmod[3]="chmod";
		chmod[4]="-R";
		if (group) {
			chmod[5]="g+rwx";	

		} else {
			chmod[5]="u+rwx";	

		}
		for (File f : dir.listFiles()) {
			chmod[6]=f.getAbsolutePath();
			Util.executeCommand(chmod);
		}
    }
    
    /**
     * Copies all of the given files to a single, newly created sandbox directory
     * and returns the sandbox directory. The sandbox user will be the owner and
     * have full permissions over everthing in the sandbox directory. 
     * @param files
     * @return
     * @throws IOException
     */
    public static File copyFilesToNewSandbox(List<File> files) throws IOException {
    	File sandbox=getRandomSandboxDirectory();
    	File sandbox2=getRandomSandboxDirectory();
    	String[] cpCmd=new String[4];
    	cpCmd[0]="cp";
    	cpCmd[1]="-r";
    	cpCmd[3]=sandbox.getAbsolutePath();
    	for (File f : files) {
    		cpCmd[2]=f.getAbsolutePath();
    		Util.executeCommand(cpCmd);
    	}
    	log.debug(Util.executeCommand("ls -l -R "+sandbox.getAbsolutePath()));
    	log.debug(Util.executeCommand("ls -l -R "+sandbox2.getAbsolutePath()));

    	//next, copy the files over so they are owned by sandbox
    	String[] sudoCpCmd=new String[4];
    	
    	sudoCpCmd[0]="cp";
    	sudoCpCmd[1]="-r";
    	sudoCpCmd[3]=sandbox2.getAbsolutePath();
    	for (File f : sandbox.listFiles()) {
    		sudoCpCmd[2]=f.getAbsolutePath();
    		Util.executeSandboxCommand(sudoCpCmd);
    	}
    	log.debug(Util.executeCommand("ls -l -r "+sandbox.getAbsolutePath()));
    	log.debug(Util.executeCommand("ls -l -r "+sandbox2.getAbsolutePath()));

    	
    	sandboxChmodDirectory(sandbox2,false);
    	FileUtils.deleteQuietly(sandbox);
    	return sandbox2;
    }
    
    /**
     * Creates and returns a unique, empty directory immediately inside
     * of the sandbox directory on the head node
     * @return
     */
    public static File getRandomSandboxDirectory() {
		File sandboxDirectory=Util.getSandboxDirectory();
		String randomDirectory=TestUtil.getRandomAlphaString(64);
		
		File sandboxDir=new File(sandboxDirectory,randomDirectory);
		                        
		sandboxDir.mkdirs();
		return sandboxDir;
    }
    
}
