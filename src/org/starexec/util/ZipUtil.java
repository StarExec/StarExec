package org.starexec.util;

import java.io.*;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Contains helper methods for dealing with zip files
 * TODO: Add support for .tar.gz
 * 
 * @author Tyler Jensen
 */
public class ZipUtil {
	/**
	 * This method simply takes in the name of the zip file, and extracts its contents into
	 * the desired location. It will automatically create any sort of file structure necessary to unzip to.
	 * @param fileName The absolute name of the zip file to extract
	 * @param destination The path to the folder to extract the file to
	 * @throws FileNotFoundException If there was a problem locating the zip file
	 * @throws IOException If there was a problem extracting from the zip file	
	 */
	@SuppressWarnings("unchecked")
	public static void extractZip(String fileName, String destination) throws FileNotFoundException, IOException{		
	    ZipFile zipFile = new ZipFile(fileName);													// Create a zip file object
	    Enumeration<ZipEntry> entries = (Enumeration<ZipEntry>) zipFile.entries();	    			// Get all of the entries in the zip file
	    
	    while(entries.hasMoreElements()) {															// For each file in the zip file...
	    	ZipEntry entry = (ZipEntry)entries.nextElement();

	        if(!entry.isDirectory()) {																// If it's actually a file and not a directory
	        	File parentDir = new File(destination, entry.getName()).getParentFile();				// Get the file's parent directory
	        	
	        	if(!parentDir.exists())																// If the directory structure doesn't exist, create it
	        		parentDir.mkdirs();
	        	
	        	FileOutputStream fileOut = new FileOutputStream(destination + entry.getName());		// Open a new file for writing
		        extractFile(zipFile.getInputStream(entry), new BufferedOutputStream(fileOut));		// Extract the zipped file to the newly opened file (essentially copy it)		 
	        } else {																				// If it is a directory...
	        	File dir = new File(destination, entry.getName());									// Get the file's parent directory
	        	if(!dir.exists())																	// Create it if it doesn't exist
	        		dir.mkdirs();
	        }
	      }

	      zipFile.close();																			// Close the zip file when finished
	}
	
	/**
	 * This method simply takes in the name of the zip file, and extracts its contents into
	 * the same directory that the zip file resides in. It will automatically create any sort
	 * of file structure necessary to unzip to.
	 * @param fileName The absolute name of the zip file to extract
	 * @throws FileNotFoundException If there was a problem locating the zip file
	 * @throws IOException If there was a problem extracting from the zip file	
	 */
	public static void extractZip(String fileName) throws FileNotFoundException, IOException{
		String parent = new File(fileName).getParentFile().getCanonicalPath() + File.separator;		// Get the directory the zip file is in (this will be the directory to extract to)
		extractZip(fileName, parent);
	}
	
	/**
	 * Copies a inputstream to an output stream. Helper method for zip file extraction
	 * @param in The input stream to copy from
	 * @param out The output stream to copy to
	 * @throws IOException
	 */
	private static void extractFile(InputStream in, OutputStream out) throws IOException {
	    byte[] buffer = new byte[1024];
	    int len;

	    while((len = in.read(buffer)) >= 0)
	      out.write(buffer, 0, len);

	    in.close();
	    out.close();
    }
}