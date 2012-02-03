package org.starexec.util;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

import org.apache.commons.compress.archivers.ArchiveInputStream;
import org.apache.commons.compress.archivers.ArchiveStreamFactory;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.compressors.CompressorInputStream;
import org.apache.commons.compress.compressors.CompressorStreamFactory;
import org.apache.commons.compress.utils.IOUtils;
import org.apache.log4j.Logger;
import org.starexec.constants.R;

/**
 * Contains helper methods for dealing with archived files (.zip .tar .tar.gz)
 * @author Tyler Jensen
 */
public class ArchiveUtil {
	private static final Logger log = Logger.getLogger(ArchiveUtil.class);
	
	/**
	 * Extracts/unpacks/uncompresses an archive file to a folder with the same name at the given destination.
	 * This method supports .zip, .tar and .tar.gz files. Once the contents are extracted the original archive file
	 * is deleted. Note if the extraction failed, some files/folders may have been partially created.
	 * @param fileName The full file path to the archive file
	 * @param destination The full path to the folder to extract the file to
	 * @return True if extraction was successful, false otherwise.
	 */
	public static boolean extractArchive(String fileName, String destination) {
		try {
			// Check for the appropriate file extension and hand off to the appropriate method
			if(fileName.endsWith(".zip")) {
				ArchiveUtil.extractZIP(fileName, destination);
			} else if(fileName.endsWith(".tar")) {
				ArchiveUtil.extractTAR(fileName, destination);
			} else if (fileName.endsWith(".tar.gz")) {
				// First un-GZIP it
				ArchiveUtil.extractGZ(fileName, destination);
				
				// Then unpack the tar that was the result of the un-gzip
				ArchiveUtil.extractTAR(fileName.substring(0, fileName.lastIndexOf('.')), destination);
			} else {
				// No valid file type found :(
				log.warn(String.format("Unsupported file extension for [%s] attempted to uncompress", fileName));
				return false;
			}
		    
		    log.debug(String.format("Successfully extracted [%s] to [%s]", fileName, destination));
		    return true;
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}
		
		return false;
	}
	
	/**
	 * Extracts/unpacks/uncompresses an archive file to the same folder the archive file exists within.
	 * This method supports .zip, .tar and .tar.gz files. Once the contents are extracted the original archive file
	 * is deleted. Note if the extraction failed, some files/folders may have been partially created.
	 * @param fileName The full file path to the archive file
	 * @return True if extraction was successful, false otherwise.
	 */
	public static boolean extractArchive(String fileName) {
		try {
			String parent = new File(fileName).getParentFile().getCanonicalPath() + File.separator;
			return ArchiveUtil.extractArchive(fileName, parent);			
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}
		
		return false;
	}
	
	/**
	 * Unzips a zip file and removes the original if the unzip was successful.
	 * @param fileName The full path to the file
	 * @param destination Where to unzip the contents to
	 */
	private static void extractZIP(String fileName, String destination) throws Exception {
		// Use the Apache commons compression library to open up the tar file...
		InputStream is = new FileInputStream(fileName);
		BufferedInputStream bis = new BufferedInputStream(is);
		ArchiveInputStream ais = new ArchiveStreamFactory().createArchiveInputStream("zip", bis); 
		ZipArchiveEntry entry = null;
		
		// For each 'file' in the tar file...
		while((entry = (ZipArchiveEntry)ais.getNextEntry()) != null) {
			if(!entry.isDirectory()) {
				// If it's not a directory...
				File fileToCreate = new File(destination, entry.getName());
				
				// Get the dir the file b elongs to
				File dir = new File(fileToCreate.getParent());
				if(!dir.exists()) {
					// And create it if it doesn't exist so we can write a file inside it
					dir.mkdirs();
				}
				
				// Finally, extract the file
				OutputStream out = new FileOutputStream(fileToCreate); 
				IOUtils.copy(ais, out);
				out.close();
			}			
		}
				
		ais.close();
		ArchiveUtil.removeArchive(fileName);
	}	
	
	/**
	 * Unpacks a tar file and removes the original if the unpack was successful.
	 * @param fileName The full path to the file
	 * @param destination Where to unpack the contents to
	 */
	private static void extractTAR(String fileName, String destination) throws Exception {
		// Use the Apache commons compression library to open up the tar file...
		InputStream is = new FileInputStream(fileName);
		BufferedInputStream bis = new BufferedInputStream(is);
		ArchiveInputStream ais = new ArchiveStreamFactory().createArchiveInputStream("tar", bis); 
		TarArchiveEntry entry = null;
		
		// For each 'file' in the tar file...
		while((entry = (TarArchiveEntry)ais.getNextEntry()) != null) {
			if(!entry.isDirectory()) {
				// If it's not a directory...
				File fileToCreate = new File(destination, entry.getName());
				
				// Get the dir the file b elongs to
				File dir = new File(fileToCreate.getParent());
				if(!dir.exists()) {
					// And create it if it doesn't exist so we can write a file inside it
					dir.mkdirs();
				}
				
				// Finally, extract the file
				OutputStream out = new FileOutputStream(fileToCreate); 
				IOUtils.copy(ais, out);
				out.close();
			}			
		}
				
		ais.close();
		ArchiveUtil.removeArchive(fileName);
	}
	
	/**
	 * Extracts a GZIP file and remoes the original if he extraction was successful.
	 * @param fileName The full path to the file
	 * @param destination Where to extract the contents to
	 */
	private static void extractGZ(String fileName, String destination) throws Exception {
		// Use the Apache commons compression library to magically extract a GZIP file...
		InputStream is = new FileInputStream(fileName);
		BufferedInputStream bis = new BufferedInputStream(is);		
		CompressorInputStream in = new CompressorStreamFactory().createCompressorInputStream("gz", bis);
		FileOutputStream out = new FileOutputStream(new File(destination, Util.getFileNameOnly(fileName)));
		IOUtils.copy(in, out);				
		
		in.close();
		out.close();
		ArchiveUtil.removeArchive(fileName);
	}
	
	/**
	 * Checks the global remove archive setting and removes the archive file if the setting is true.
	 * @param fileName The path to the archive file to remove
	 */
	private static void removeArchive(String fileName) {
		if(R.REMOVE_ARCHIVES){
			if(!new File(fileName).delete()) {
				log.warn("Failed to cleanup archive file: " + fileName);
			}
			else {
				log.debug("Cleaned up archive file: " + fileName);
			}
		}
	}
}