package org.starexec.util;

import java.awt.Color;
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
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.Throwable;
import java.lang.reflect.Field;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.URL;
import java.net.URLConnection;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.Part;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.LineIterator;
import org.apache.commons.io.filefilter.FileFilterUtils;
import org.apache.commons.io.filefilter.IOFileFilter;
import org.apache.log4j.Logger;

import org.starexec.constants.R;
import org.starexec.test.TestUtil;

/**
 * This class contains utility functions used throughout Starexec, including many
 * for executing commands and interacting with the filesystem.
 * @author Eric
 *
 */
public class Util {	
    private static final Logger log = Logger.getLogger(Util.class);

    protected static final ExecutorService threadPool = Executors.newCachedThreadPool();
    
    /**
     * Checks to see if the two given objects are equal without throwing any null pointers.
     * if a and b are both null, returns true
     * @param a 
     * @param b
     * @return True if they are equal and false otherwise
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
     * 
     * @param c The string color
     * @return The Java color corresponding to the string, or null if no such color exists
     * Obtained at (http://stackoverflow.com/questions/2854043/converting-a-string-to-color-in-java)
     */
    public static Color getColorFromString(String c) {
    	Color color;
    	try {
    	    Field field = Class.forName("java.awt.Color").getField(c);
    	    color = (Color)field.get(null);
    	} catch (Exception e) {
    	    color = null; // Not defined
    	}
    	return color;
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
	log.debug("calling readFileLimited");
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
	    } 
		// If the file doesn't exist...
		log.warn("Could not find file to open: " + f.getAbsolutePath());
	    
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
     * @return True if this is production and false if it is a test instance
     */
    public static boolean isTestingAllowed() {
		return R.ALLOW_TESTING;
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
     * @throws Exception if termination of the thread pool is interrupted for taking
     * longer than 2 seconds
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
	
    /**
     * Clamps the given value to within the given range
     * @param min The min of the range, inclusive
     * @param max The max of the range, inclusive
     * @param value The valu to clamp.
     * @return min, if the value is lower, and max if the value is larger. 
     * The value itself otherwise
     */
    public static long clamp(long min, long max, long value) {
		if (value<min) {
		    return min;
		}
		if (value > max) {
		    return max;
		}
		return value;
    }
	
    /**
     * Initializes Starexec data directories by creating them if they
     * do not exist
     */
    public static void initializeDataDirectories() {
		File file=new File(R.STAREXEC_DATA_DIR);
		file.mkdir();
			
		file=new File(R.getJobInboxDir());
		file.mkdir();
		file=new File(R.getJobLogDir());
		file.mkdir();
		file=new File(R.getBenchmarkPath());
		file.mkdir();
		file=new File(R.getSolverPath());
		file.mkdir();
		file=new File(R.getSolverBuildOutputDir());
		file.mkdir();
		file=new File(R.getProcessorDir());
		file.mkdir();
		file=new File(R.getJobOutputDirectory());
		file.mkdir();
		file=new File(R.getPicturePath());
		file.mkdir();
			
			
			
		File downloadDir=new File(R.STAREXEC_ROOT,R.DOWNLOAD_FILE_DIR);
		downloadDir.mkdirs();
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
    /**
     * Generates a list of strings from a csv, where each string in the list
     * is a single value (separated by commas) of the csv
     * @param csv
     * @return The list
     */
	public static List<String> csvToList(String csv) {
		log.debug("Entering csvToList");
		log.debug("Got csv: " + csv);
		// trim whitespace off the ends of the input so we don't get blank elements when we split
		csv = csv.trim();
		List<String> csvList = new ArrayList<String>();
		String[] splitCsv = csv.split(",");
		for (int i = 0; i < splitCsv.length; i++) {
			csvList.add(splitCsv[i].trim());
		}
		
		log.debug("csvList.size(): "+csvList.size());
		return csvList;
	}
	
	
	/**
	 * 
	 * @param name
	 * @param request
	 * @return True if the value of the param given by name is not null in the given request
	 */
    public static boolean paramExists(String name, HttpServletRequest request){
    	return !isNullOrEmpty(request.getParameter(name));
    }
	/**
	 * 
	 * @param s
	 * @return True if s is null or empty and false otherwise
	 */
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
     * @throws Exception If the request is malformed
     */
    public static HashMap<String, Object> parseMultipartRequest(HttpServletRequest request) throws Exception {
		// Use Tomcat's multipart form utilities
		HashMap<String, Object> form = new HashMap<String, Object>();
		for (Part p : request.getParts()) {
			PartWrapper wrapper = new PartWrapper(p);
		    // If we're dealing with a regular form field...
		    if(!wrapper.isFile()) { 
				// Add the field name and field value to the hashmap
				form.put(p.getName(), IOUtils.toString(p.getInputStream()));				
		    } else {
				// Else we've encountered a file, so add the entire wrapper to the HashMap.
		    	// The wrapper provides all the relevant interface of a FileItem
				form.put(p.getName(), wrapper);					
		    }	
		}
			
		return form;
    }
	
    /**
     * Calls executeCommand with a size 1 String[]
     * @param command
     * @return See full executeCommand documentation
     * @throws IOException
     */
    public static String executeCommand(String command) throws IOException {
		String[] cmd = new String[1];
		cmd[0] = command;
		return executeCommand(cmd);
    }
    /**
     * Calls executecommand with a size 1 String[] and a null working directory
     * @param command
     * @param env
     * @return See full executeCommand documentation
     * @throws IOException
     */
    public static String executeCommand(String command, String[] env) throws IOException {
		String[] cmd = new String[1];
		cmd[0] = command;
		return executeCommand(cmd,env,null);
    }

    /**
     * Calls executeCommand with a null environment and working directory
     * @param command
     * @return See full executeCommand documentation
     * @throws IOException
     */
    public static String executeCommand(String[] command) throws IOException {
    	return executeCommand(command,null,null);
    }
    /**
     * Calls executeSandboxCommand with a null environment and working directory
     * @param command
     * @return See full executeCommand documentation
     * @throws IOException
     */
    public static String executeSandboxCommand(String[] command) throws IOException {
    	return executeSandboxCommand(command,null,null);
    }
    /**
     * Executes a command as the sandbox user using sudo
     * @param command The command to execute, tokenized
     * @param envp Environment variables for the command
     * @param workingDirectory Directory to use as the command working directory
     * @return The combined stdout and stderr from the command
     * @throws IOException
     */
    public static String executeSandboxCommand(String[] command, String[] envp, File workingDirectory) throws IOException {
    	String[] newCommand=new String[command.length+3];
    	newCommand[0]="sudo";
    	newCommand[1]="-u";
    	newCommand[2]="sandbox";
    	for (int i=0;i<command.length;i++) {
    		newCommand[i+3]=command[i];
    	}
    	return executeCommand(newCommand,envp,workingDirectory);
    }
    
    /**
     * Runs a command on the system command line (bash for unix, command line for windows)
     * and returns the process representing the command
     * @param command An array holding the command and then its arguments
     * @param envp The environment
     * @param workingDirectory the working directory to use
     * @return A String containing both stderr and stdout from the command
     * @throws IOException We do not want to catch exceptions at this level, because this code is generic and
     * has no useful way to handle them! Throwing an exception to higher levels is the desired behavior.
     */
    //TODO: Why isn't the working directory being set if the command has length 1? That seems wrong.
    public static Process executeCommandAndReturnProcess(String[] command, String[] envp, File workingDirectory) throws IOException {
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
	    return p;
	
    }
	
    /**
     * Runs a command on the system command line (bash for unix, command line for windows)
     * and returns the results from the command as a string
     * @param command An array holding the command and then its arguments
     * @param envp The environment
     * @param workingDirectory the working directory to use
     * @return A String containing both stderr and stdout from the command
     * @throws IOException We do not want to catch exceptions at this level, because this code is generic and
     * has no useful way to handle them! Throwing an exception to higher levels is the desired behavior.
     */
	
    public static String executeCommand(String[] command, String[] envp, File workingDirectory) throws IOException {
	    return drainStreams(executeCommandAndReturnProcess(command,envp,workingDirectory));
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

    /**
     * Drains both the stdout and stderr streams of a process and returns 
     * @param p
     * @return The combined stdout and stderr from the process
     */
    public static String drainStreams(final Process p) {
		    
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
					if (drainInputStream(b,p.getErrorStream())) {
						log.error("The process produced stderr output.");
						log.error(b.toString());
					}
					    
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

	/**
	 * @param nums
	 * @return A string containing a comma-separated list of the given numbers
	 */
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
     * Retrieves all files in the given directory that are as old as, or older than the specified number of days
     * @param directory The directory to clear old files out of (non-recursive)
     * @param daysAgo Files older than this many days ago will be deleted
     * @param includeDirs Whether to include directories as well as files
     * @return All files older than the given filter
     */
    public static Collection<File> getOldFiles(String directory, int daysAgo, boolean includeDirs) {
    	File dir = new File(directory);
		
	    if(!dir.exists()) {
	    	return null;
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
	    	File[] files=dir.listFiles((FileFilter)dateFilter);
	    	outdatedFiles=new ArrayList<File>();
	    	for (File f : files) {
	    		outdatedFiles.add(f);
	    	}
	    }
	    return outdatedFiles;
    }
    
    /**
     * Deletes all files in the given directory that are as old as, or older than the specified number of days.
     * The given directory itself is NOT deleted
     * @param directory The directory to clear old files out of (non-recursive)
     * @param daysAgo Files older than this many days ago will be deleted
     */
    public static void clearOldSandboxFiles(String directory, int daysAgo){
		try {
		    Collection<File> outdatedFiles=getOldFiles(directory,daysAgo,true);
		    log.debug("found a total of "+outdatedFiles.size() +" outdated files to delete in "+directory);
		    // Remove them all
		    for(File f : outdatedFiles) {
		    	sandboxChmodDirectory(f);
		    	if (f.isDirectory()) {
			    	FileUtils.deleteDirectory(f);
		    	} else {
		    		FileUtils.deleteQuietly(f);
		    	}
		    }					
		} catch (Exception e) {
		    log.warn(e.getMessage(), e);
		}
    }
    
    /**
     * Deletes all files in the given directory that are as old as, or older than the specified number of days
     * @param directory The directory to clear old files out of (non-recursive)
     * @param daysAgo Files older than this many days ago will be deleted
     * @param includeDirs Whether to delete directories as well as files
     */
    public static void clearOldFiles(String directory, int daysAgo,boolean includeDirs){
		try {
		    Collection<File> outdatedFiles=getOldFiles(directory,daysAgo,includeDirs);
		    log.debug("found a total of "+outdatedFiles.size() +" outdated files to delete in "+directory);
		    // Remove them all
		    for(File f : outdatedFiles) {
		    	
		    	FileUtils.deleteQuietly(f);
		    }					
		} catch (Exception e) {
		    log.warn(e.getMessage(), e);
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
     * @param s
     * @return a path to the given document relative to STAREXEC_ROOT
     * @author Aaron Stump
     */
    public static String docRoot(String s) {
    	initDocRoot();
    	return docRoot + s;
    }
    /**
     * Prepend the "https://", the server name, and the document root, to form an absolute path (URL).
     * @param s The relative path to create a url for
     * @return the absolute url associated with the given relative path
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
     * @return Number of bytes representing the given gigabytes
     */
    public static long gigabytesToBytes(double gigabytes) {
    	long bytes=(long)(1073741824*gigabytes);
    	return bytes;
    }
    /**
     * 
     * @param bytes
     * @return Converts bytes to megabytes, truncated to the nearest integer megabyte
     */
    public static long bytesToMegabytes(long bytes) {
    	return (bytes / (1024*1024));
    }
    
    /**
     * 
     * @param bytes
     * @return the number of gigabytes representing the given number of bytes
     */
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
     * @param <T> Type of the given list and comparator. Can be any sortable object type.
     * @param arr List to sort
     * @param compare Comparator object that will be used to determine the ordering of objects during sorting
     * @param start Record to start on
     * @param records Number of records to give back (actual number will be less if the size of the list is less than records)
     * @return Entries sorted and filtered according to the given comparator
     */
    
    public static <T> List<T> handlePagination(List<T> arr, Comparator<T> compare,int start, int records) {
    	Collections.sort(arr,compare);
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
	 * Gets a String representation of a Throwable object's
	 * stack trace.
	 * @param t a throwable object
	 * @return the string representation of t's stack trace.
	 * @author Albert Giegerich
	 */
	public static String getStackTrace(Throwable t) {
		StringWriter sw = new StringWriter();
		PrintWriter pw = new PrintWriter(sw);
		t.printStackTrace(pw);
		return sw.toString(); 	
	}
    
    
    /**
     * Recursively grants full permission to the owner of everything in the given
     * directory. The top level directory is not affected, only everything inside
     * @param dir
     * @throws IOException
     */
    public static void sandboxChmodDirectory(File dir) throws IOException {
    	if (!dir.isDirectory()) {
    		return;
    	}
    	//give sandbox full permissions over the solver directory
		String[] chmod=new String[7];
		chmod[0]="sudo";
		chmod[1]="-u";
		chmod[2]="sandbox";
		chmod[3]="chmod";
		chmod[4]="-R";
		chmod[5]="u+rwx,g+rwx";	
		for (File f : dir.listFiles()) {
			chmod[6]=f.getAbsolutePath();
			Util.executeCommand(chmod);
		}
    }
    /**
     * Adds rwx permissions to the directory for either the owner or the group
     * @param dir The directory to modify
     * @param group True to modify permissions for the group and false for the directory
     * @throws IOException
     */
    public static void chmodDirectory(String dir,boolean group) throws IOException {
		String[] chmod=new String[4];
		chmod[0]="chmod";
		chmod[1]="-R";
		if (group) {
		    chmod[2]="g+rwx";	
	
		} else {
		    chmod[2]="u+rwx";	
	
		}
		chmod[3]=dir;
		Util.executeCommand(chmod);
    }

    /**
     * Copies all of the given files to a single, newly created sandbox directory
     * and returns the sandbox directory. The sandbox user will be the owner and
     * have full permissions over everthing in the sandbox directory. 
     * @param files
     * @return The sandbox directory that contains all the copied files
     * @throws IOException
     */
    public static File copyFilesToNewSandbox(List<File> files) throws IOException {
    	File sandbox=null;
    	File sandbox2=null;
	try {
	    sandbox=getRandomSandboxDirectory();
	    sandbox2=getRandomSandboxDirectory();
	    String[] cpCmd=new String[4];
	    cpCmd[0]="cp";
	    cpCmd[1]="-r";
	    cpCmd[3]=sandbox.getAbsolutePath();
	    for (File f : files) {
    		cpCmd[2]=f.getAbsolutePath();
    		Util.executeCommand(cpCmd);
	    }

	    //next, copy the files over so they are owned by sandbox
	    String[] sudoCpCmd=new String[4];
    	
	    sudoCpCmd[0]="cp";
	    sudoCpCmd[1]="-r";
	    sudoCpCmd[3]=sandbox2.getAbsolutePath();
	    for (File f : sandbox.listFiles()) {
    		sudoCpCmd[2]=f.getAbsolutePath();
    		Util.executeSandboxCommand(sudoCpCmd);
	    }
	    sandboxChmodDirectory(sandbox2);
	}
	finally {
	    FileUtils.deleteQuietly(sandbox);
	}
    	return sandbox2;
    }
    
    /**
     * Creates and returns a unique, empty directory immediately inside
     * of the sandbox directory on the head node
     * @return The sandbox directory that was created
     */
    public static File getRandomSandboxDirectory() {
		File sandboxDirectory=Util.getSandboxDirectory();
		String randomDirectory=TestUtil.getRandomAlphaString(64);
		
		File sandboxDir=new File(sandboxDirectory,randomDirectory);
		                        
		sandboxDir.mkdirs();
		return sandboxDir;
    }
    /**
     * Executes ls -l -R on the sandbox directory and logs the results
     */
    public static void logSandboxContents() {
    	try {
        	log.debug(Util.executeCommand("ls -l -R "+Util.getSandboxDirectory().getAbsolutePath()));

    	} catch (Exception e) {
    		log.error(e.getMessage(),e);
    
    	}
    }

	/**
	 * Gets the HTML for a web page as a String with query parameters.
	 * @param url The url to get the page from.
	 * @param queryParameters A mapping of query parameters to their values.
	 * @param cookiesToSend Cookies to send with the request
	 * @return the web page in string form.
	 * @author Albert Giegerich
	 * @throws IOException if there is some error getting the web pages
	 */
	public static String getWebPage(String url, Map<String, String> queryParameters, List<Cookie> cookiesToSend) throws IOException {
		if (queryParameters.keySet().size() == 0) {
			return url; 
		}

		// Initially contains the ? necessary for the query string.
		//StringJoiner queryStringJoiner = new StringJoiner("&", "?", "");
		StringBuilder queryStringBuilder = new StringBuilder();

		
		queryStringBuilder.append("?");
		for (String parameter : queryParameters.keySet()) {
			String value = queryParameters.get(parameter);
			queryStringBuilder.append(parameter+"="+value+"&");
			//queryStringJoiner.add(parameter+"="+value);
		}
		// delete the last & character
		queryStringBuilder.deleteCharAt(queryStringBuilder.length() - 1);

		return getWebPage(url+queryStringBuilder.toString(), cookiesToSend);
	}

	/**
	 * Gets the HTML for a web page as a String.
	 * @param url The url to get the page from.
	 * @param cookiesToSend The cookies to attach to this request.
	 * @return the web page in String form.
	 * @throws IOException
	 * @author Albert Giegerich
	 */
	public static String getWebPage(String url, List<Cookie> cookiesToSend) throws IOException {
		String nextLine;
		StringBuilder outputHtml = new StringBuilder();
		URL inputUrl = new URL(url);
		URLConnection urlConnection = inputUrl.openConnection();
		if (cookiesToSend != null) {
			String cookieString = buildCookieString(cookiesToSend);
			urlConnection.setRequestProperty("Cookie", cookieString);
		}
		try (BufferedReader reader = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()))) {
			while ((nextLine = reader.readLine()) != null) {
				outputHtml.append(nextLine);
			}
		} 
		return outputHtml.toString();
	}

	/**
	 * Builds a String representing a list of Cookies that we can pass to URLConnection.setRequestPropery to send cookies.
	 */
	private static String buildCookieString(List<Cookie> cookies) {
		//StringJoiner cookieStringJoiner = new StringJoiner("; ");
		StringBuilder cookieStringBuilder = new StringBuilder();
		for (Cookie cookie : cookies) {
			cookieStringBuilder.append(cookie.getName()+"="+cookie.getValue() + ";");
		}
		if (cookies.size() > 0) {
			cookieStringBuilder.deleteCharAt(cookieStringBuilder.length() - 1);
		}
		return cookieStringBuilder.toString();
	}

    /**
     * Attempts to copy the file at the end of the given URL to the given file, using a proxy
     * @param url
     * @param archiveFile
     * @return True on success and false otherwise
     */
    public static boolean copyFileFromURLUsingProxy(URL url, File archiveFile) {
    	try {
    		Proxy proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(R.PROXY_ADDRESS, R.PROXY_PORT));
        	URLConnection connection = url.openConnection(proxy);
        	FileUtils.copyInputStreamToFile(connection.getInputStream(), archiveFile);
        	return true;
    	} catch (Exception e) {
    		log.error(e.getMessage(),e);
    	}
    	return false;
    }
    /**
     * Writes the given InputStream to the given file. The InputStream
     * will be close on return
     * @param stream The InputStream to copy
     * @param outputFile The File to write to
     * @throws IOException If there were any writing exceptions
     */
    public static void writeInputStreamToFile(InputStream stream, File outputFile) throws IOException {
    	FileOutputStream output = new FileOutputStream(outputFile);
    	IOUtils.copy(stream, output);
    	stream.close();
    	output.close();
    }
    
}
