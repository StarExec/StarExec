package org.starexec.util;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Random;

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
import org.starexec.data.database.Cache;

public class Util {	
	private static final Logger log = Logger.getLogger(Util.class);
	
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
	
	/**
	 * Extracts the file extesion from a file path
	 * @param s The file path
	 * @return The extension of the file
	 */
	public static String getFileExtension(String s){
		return s.substring(s.lastIndexOf('.') + 1);
	}
	
	/**
	 * @return The platform-dependant line separator
	 */
	public static String getLineSeparator(){
		return System.getProperty("line.separator");
	}
	
	/**
	 * Extracts the file name from an absolute path
	 * @param path The path of the file to extract the name from
	 * @return The file's name, not including its extension
	 */
	public static String getFileNameOnly(String path){
		int lastSep = path.lastIndexOf(File.separator);
		int lastDot = path.lastIndexOf('.');
		return path.substring(lastSep + 1, lastDot);
	}
	
	public static boolean paramExists(String name, HttpServletRequest request){
		return !isNullOrEmpty(request.getParameter(name));
	}
	
	public static boolean isNullOrEmpty(String s){
		return (s == null || s.trim().length() <= 0);
	}	
	
	/**
	 * Generates a temporary password consisting of 4 letters, 1 digit and 1 special
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
	
	public static BufferedReader executeCommand(String command) {
		String[] cmd = new String[1];
		cmd[0] = command;
		return executeCommand(cmd);
	}

	public static BufferedReader executeCommand(String command, String[] env) {
		String[] cmd = new String[1];
		cmd[0] = command;
		return executeCommand(cmd,env);
	}

	/** Convenience method for executeCommand() */
	public static BufferedReader executeCommand(String[] command) {
		return executeCommand(command,null);
	}

	/**
	 * Runs a command on the system command line (bash for unix, command line for windows)
	 * and returns the results from the command as a buffered reader which can be processed.
	 * MAKE SURE TO CLOSE THE READER WHEN DONE. Null is returned if the command failed.
	 * @param command An array holding the command and then its arguments
	 * @param envp The environment
	 * @return A buffered reader holding the output from the command.
	 */
	
    public static BufferedReader executeCommand(String[] command, String[] envp) {
		Runtime r = Runtime.getRuntime();
		
		BufferedReader reader = null;		
		//
		try {					
		    Process p;
		    if (command.length == 1) {
			log.info("Executing the following command: " + command[0]);
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
			    
			p = r.exec(command, envp);
		    }
		    InputStream in = p.getInputStream();
		    BufferedInputStream buf = new BufferedInputStream(in);
		    InputStreamReader inread = new InputStreamReader(buf);
		    reader = new BufferedReader(inread);		
			
		    //Also handle error stream
		    InputStream err = p.getErrorStream();
		    BufferedInputStream bufErr = new BufferedInputStream(err);
		    InputStreamReader inreadErr = new InputStreamReader(bufErr);
		    BufferedReader errReader = new BufferedReader(inreadErr);
		    String errLine = null;
		    while ((errLine = errReader.readLine()) != null){
			log.error("stdErr = " + errLine);
		    }
		    errReader.close();
		    //This will hang indefinitely if the stream is too large.  TODO: fix increase size?
		    if (p.waitFor() != 0) {
			log.warn("Command "+command[0]+" failed with value " + p.exitValue());				
		    }
		    return reader;
		} catch (Exception e) {
			log.warn("execute command says " + e.getMessage(), e);		
		}
		
		return null;
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
	
	/**
	 * Deletes all files in the given directory that are as old as, or older than the specified number of days
	 * @param directory The directory to clear old files out of (non-recursive)
	 * @param daysAgo Files older than this many days ago will be deleted
	 */
	public static void clearOldFiles(String directory, int daysAgo){
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
			
			// Get all of the outdated files
			Collection<File> outdatedFiles = FileUtils.listFiles(dir, dateFilter, null);
			
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
    
    public static double bytesToGigabytes(long bytes) {
    	return ((double)bytes/1073741824.0);
    }
    /**
     * Attempts to delete the directory specified the given path without
     * throwing any errors
     * @param path The path to the directory to delete
     * @return True on success, false otherwise
     * @author Eric Burns
     */
    public static boolean safeDeleteDirectory(String path) {
    	try {
    		FileUtils.deleteDirectory(new File(path));
    	} catch (Exception e) {
    		log.error("safeDeleteDirectory says "+e.getMessage(),e);
    	}
    	return false;
    }
    
}
