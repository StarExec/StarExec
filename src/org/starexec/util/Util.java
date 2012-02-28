package org.starexec.util;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Random;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.LineIterator;
import org.apache.log4j.Logger;
import org.apache.tomcat.util.http.fileupload.FileItem;
import org.apache.tomcat.util.http.fileupload.FileItemFactory;
import org.apache.tomcat.util.http.fileupload.disk.DiskFileItemFactory;
import org.apache.tomcat.util.http.fileupload.servlet.ServletFileUpload;
import org.starexec.constants.R;

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
        int newPassLength = r.nextInt((20-6)+1) + 6;
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
	
	/**
	 * Runs a command on the system command line (bash for unix, command line for windows)
	 * and returns the results from the command as a buffered reader which can be processed.
	 * MAKE SURE TO CLOSE THE READER WHEN DONE. Null is returned if the command failed.
	 * @param command The command to execute
	 * @return A buffered reader holding the output from the command.
	 */
	public static BufferedReader executeCommand(String command) {
		Runtime r = Runtime.getRuntime();		
		BufferedReader reader = null;		
		
		try {		
			Process p = r.exec(command);
			InputStream in = p.getInputStream();
			BufferedInputStream buf = new BufferedInputStream(in);
			InputStreamReader inread = new InputStreamReader(buf);
			reader = new BufferedReader(inread);			

			/*if (p.waitFor() != 0) {
				log.warn("Command failed with value " + p.exitValue() + ": " + command);				
			}*/			
			
			return reader;
		} catch (Exception e) {
			log.warn(e.getMessage(), e);		
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
		ArrayList<Integer> retList = new ArrayList<Integer>(stringList.length);
		
		for(String s : stringList){
			retList.add(Integer.parseInt(s));
		}
		
		return retList;
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
}
