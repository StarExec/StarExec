package org.starexec.util;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.apache.commons.compress.archivers.ArchiveInputStream;
import org.apache.commons.compress.archivers.ArchiveStreamFactory;
import org.apache.commons.compress.archivers.tar.*;
import org.apache.commons.compress.archivers.zip.*;
import org.apache.commons.compress.compressors.CompressorInputStream;
import org.apache.commons.compress.compressors.CompressorStreamFactory;
import org.apache.commons.compress.compressors.gzip.*;
import org.apache.commons.compress.utils.IOUtils;
import org.apache.log4j.Logger;
import org.starexec.constants.R;

/**
 * Contains helper methods for dealing with archived files (.zip .tar .tar.gz, .tgz)
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
	 * 
	 * @author Tyler Jense
	 */
	public static boolean extractArchive(String fileName, String destination) {
		log.debug("ExtractingArchive for " + fileName);
		try {
			// Check for the appropriate file extension and hand off to the appropriate method
			if(fileName.endsWith(".zip")) {
				ArchiveUtil.extractZIP(fileName, destination);
			} else if(fileName.endsWith(".tar")) {
				ArchiveUtil.extractTAR(fileName, destination);
			} else if (fileName.endsWith(".tar.gz") || fileName.endsWith(".tgz")) {
				// First rename it if it's a .tgz
				if (fileName.endsWith(".tgz")){
					/*
					String oldName = fileName;
					fileName = fileName.substring(0, fileName.lastIndexOf('.')) + ".tar.gz";
					Util.executeCommand("mv " + oldName + " " + fileName);
					*/
					//extract from command line (initially only for .tgz.
					log.debug("destination is " + destination);
					
					BufferedReader reader = Util.executeCommand("ls -l " + fileName);
					String results = Util.bufferToString(reader);
					log.info("ls -l of tgz results = " + results);
					
					reader = Util.executeCommand("ls -l " + destination);
					results = Util.bufferToString(reader);
					log.info("ls -l destination results = " + results);
					
					String commandString = "tar -xvf " + fileName + " -C " + destination;
					log.info("about to execute command: " + commandString);
					/*String[] commandArray = new String[3];
					commandArray[0] = "tar";
					commandArray[1] = "-xvf";
					commandArray[2] = fileName;
					Runtime r = Runtime.getRuntime();	
					try {
						r.exec(commandArray);
						}
					catch (IOException e) {
							log.error("extract error: " + e);
					}*/
					reader = Util.executeCommand(commandString);
					results = Util.bufferToString(reader);
					log.info("command was executed, results = " + results);
					reader = Util.executeCommand("ls -l " + destination);
					results = Util.bufferToString(reader);
					log.info("command was executed - ls -l destination results = " + results);
				}
				else{
				// First un-GZIP it
				ArchiveUtil.extractGZ(fileName, destination);
				
				// Then unpack the tar that was the result of the un-gzip
				ArchiveUtil.extractTAR(fileName.substring(0, fileName.lastIndexOf('.')), destination);	
				}
			} else {
				// No valid file type found :(
				log.warn(String.format("Unsupported file extension for [%s] attempted to uncompress", fileName));
				return false;
			}
		    
		    log.debug(String.format("Successfully extracted [%s] to [%s]", fileName, destination));
		    return true;
		} catch (Exception e) {
			log.error("Archive Util says " + e.getMessage(), e);
		}
		
		return false;
	}
	
	/**
	 * Extracts/unpacks/uncompresses an archive file to the same folder the archive file exists within.
	 * This method supports .zip, .tar and .tar.gz files. Once the contents are extracted the original archive file
	 * is deleted. Note if the extraction failed, some files/folders may have been partially created.
	 * @param fileName The full file path to the archive file
	 * @return True if extraction was successful, false otherwise.
	 * 
	 * @author Tyler Jensen
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
	 * 
	 * @author Tyler Jensen
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
				
				// Get the dir the file b eints to
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
	 * 
	 * @author Tyler Jensen
	 */
	private static void extractTAR(String fileName, String destination) throws Exception {
		// Use the Apache commons compression library to open up the tar file...
		log.debug("extracting tar");
		InputStream is = new FileInputStream(fileName);
		BufferedInputStream bis = new BufferedInputStream(is);
	    ArchiveInputStream ais = new ArchiveStreamFactory().createArchiveInputStream("tar", bis); 
		TarArchiveEntry entry = null;
	
		// For each 'file' in the tar file...
		while((entry = (TarArchiveEntry)ais.getNextEntry()) != null) {
			if(!entry.isDirectory()) {
				// If it's not a directory...
				//String mode = Integer.toOctalString(entry.getMode());
				//log.info("The mode for " + entry.getName() + " is " + mode);
				File fileToCreate = new File(destination, entry.getName());
				/*Boolean shouldBeExecutable = mode.contains("1")||mode.contains("3")||mode.contains("5")||mode.contains("7");
				if (shouldBeExecutable){
					fileToCreate.setExecutable(true, false);		
				}
				log.info(fileToCreate.getName() + " is executable = " + fileToCreate.canExecute());	
			   */
				// Get the dir the file b eints to
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
	 * Extracts a GZIP file and removes the original if he extraction was successful.
	 * @param fileName The full path to the file
	 * @param destination Where to extract the contents to
	 * 
	 * @author Tyler Jensen
	 */
	private static void extractGZ(String fileName, String destination) throws Exception {
		// Use the Apache commons compression library to magically extract a GZIP file...
		log.debug("extractingGZ");
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
	
	/**
	 * Creates an archive in the specified format (between .zip, .tar, and .tar.gz) of the folder
	 * in directory "path", and saves it in the File "destination"
	 * @param path the path to the folder to be archived
	 * @param destination the path to the output folder
	 * @param format the preferred archive type
	 * 
	 * @author Skylar Stark
	 */
	public static void createArchive(File path, File destination, String format) {
		log.info("creating archive, path = " + path + ", dest = " + destination +", format = " + format);
		try {
			if (format.equals(".zip")) {
				ArchiveUtil.createZip(path, destination);
			} else if (format.equals(".tar")) {
				ArchiveUtil.createTar(path, destination);
			} else if (format.equals(".tar.gz") || format.equals(".tgz")) {
				ArchiveUtil.createTarGz(path, destination);
			}
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}
	}
	
	/**
	 * Creates a .zip file of the specified directory "path" and saves it to "destination"
	 * @param path the path to be zipped
	 * @param destination where to save the .zip file created
	 * 
	 * @author Skylar Stark
	 */
	public static void createZip(File path, File destination) throws Exception {
		log.debug("creating zip, path = " + path + ", dest = " + destination);

		FileOutputStream fOut = null;
		BufferedOutputStream bOut = null;
		ZipArchiveOutputStream zOut = null;
 
		try {
			fOut = new FileOutputStream(destination);
			bOut = new BufferedOutputStream(fOut);
			zOut = new ZipArchiveOutputStream(bOut);
 
			addFileToZip(zOut, path, "");
		} finally {
			zOut.finish();
 
			zOut.close();
			bOut.close();
			fOut.close();
		}
		
	}
	
	/**
	 * Adds a file to the .zip archive. If the file is a folder, recursively adds the contents of the
	 * folder to the archive
	 * @param zOut the zip file we are creating
	 * @param path the path of the file we are adding
	 * @param base the base prefix for the name of the zip file entry
	 * 
	 * @author Skylar Stark
	 */
	private static void addFileToZip(ZipArchiveOutputStream zOut, File path, String base) throws IOException {
		String entryName = base + path.getName();
		ZipArchiveEntry zipEntry = new ZipArchiveEntry(path, entryName);
 
		zOut.putArchiveEntry(zipEntry);
		log.debug("adding File to zip = " + entryName);
		if (path.isFile()) {
			FileInputStream fis = new FileInputStream(path);
			IOUtils.copy(fis, zOut);
			fis.close();
			zOut.closeArchiveEntry();
		} else {
			zOut.closeArchiveEntry();
 
			File[] children = path.listFiles();
			log.debug("Number of files = " + children.length);
			if (children != null) {
				for (File child : children) {
					addChildToZip(zOut, child, entryName);
				}
			}
			children = null;
		}
	}
	
	private static void addChildToZip(ZipArchiveOutputStream zOut, File child, String entryName) throws IOException{
		File tempChild = new File(child.getAbsolutePath());
		addFileToZip(zOut, tempChild, entryName + File.separator);
		tempChild = null;
		
	}

	/**
	 * Creates a .tar file of the specified directory "path" and saves it to "destination"
	 * @param path the path to be tarred
	 * @param destination where to save the .tar file created
	 * 
	 * @author Skylar Stark
	 */
	public static void createTar(File path, File destination) throws Exception {
		FileOutputStream fOut = null;
		BufferedOutputStream bOut = null;
		TarArchiveOutputStream tOut = null;
 
		try {
			fOut = new FileOutputStream(destination);
			bOut = new BufferedOutputStream(fOut);
			tOut = new TarArchiveOutputStream(bOut);
 
			addFileToTar(tOut, path, "");
		} finally {
			tOut.finish();
 
			tOut.close();
			bOut.close();
			fOut.close();
		}
	}
	
	/**
	 * Adds a file to a .tar archive. If the file is a folder, recursively adds the contents of the
	 * folder to the archive
	 * @param tOut the tar file we are adding to
	 * @param path the path of the file we are adding
	 * @param base the base prefix for the name of the tar file entry
	 * 
	 * @author Skylar Stark
	 */
	private static void addFileToTar(TarArchiveOutputStream tOut, File path, String base) throws IOException {
		String entryName = base + path.getName();
		TarArchiveEntry tarEntry = new TarArchiveEntry(path, entryName);
 
		tOut.setLongFileMode(TarArchiveOutputStream.LONGFILE_GNU);
		tOut.putArchiveEntry(tarEntry);
 
		if (path.isFile()) {
			FileInputStream fis = new FileInputStream(path);
			IOUtils.copy(fis, tOut);
			fis.close();
			tOut.closeArchiveEntry();
		} else {
			tOut.closeArchiveEntry();
 
			File[] children = path.listFiles();
 
			if (children != null) {
				for (File child : children) {
					addFileToTar(tOut, new File(child.getAbsolutePath()), entryName + "/");
				}
		}
		}
	}
	
	/**
	 * Creates a .tfar.gz file of the specified directory "path" and saves it to "destination"
	 * @param path the path to be tar.gz'ed
	 * @param destination where to save the .tar.gz file created
	 * 
	 * @author Skylar Stark
	 */
	public static void createTarGz(File path, File destination) throws Exception {
		FileOutputStream fOut = null;
	    BufferedOutputStream bOut = null;
	    GzipCompressorOutputStream gzOut = null;
	    TarArchiveOutputStream tOut = null;
	 
	    try {
	        fOut = new FileOutputStream(destination);
	        bOut = new BufferedOutputStream(fOut);
	        gzOut = new GzipCompressorOutputStream(bOut);
	        tOut = new TarArchiveOutputStream(gzOut);
	 
	        addFileToTarGz(tOut, path, "");
	    } catch (Exception e) {
	    	log.error(e.getMessage(), e);
	    } finally {
	        tOut.finish();
	 
	        tOut.close();
	        gzOut.close();
	        bOut.close();
	        fOut.close();
	    }
	}
	
	/**
	 * Adds a file to a .tar.gz archive. If the file is a folder, recursively adds the contents of the
	 * folder to the archive
	 * @param tOut the tar.gz file we are adding to
	 * @param path the path of the file we are adding
	 * @param base the base prefix for the name of the tar.gz file entry
	 * 
	 * @author Skylar Stark
	 */
	private static void addFileToTarGz(TarArchiveOutputStream tOut, File path, String base) throws IOException {
	    String entryName = base + path.getName();
	    TarArchiveEntry tarEntry = new TarArchiveEntry(path, entryName);
	 
	    tOut.setLongFileMode(TarArchiveOutputStream.LONGFILE_GNU);
	    tOut.putArchiveEntry(tarEntry);
	 
	    if (path.isFile()) {
	        FileInputStream fis = new FileInputStream(path);
	    	IOUtils.copy(fis, tOut);
	    	fis.close();
	        tOut.closeArchiveEntry();
	    } else {
	        tOut.closeArchiveEntry();
	 
	        File[] children = path.listFiles();
	 
	        if (children != null) {
	            for (File child : children) {
	                addFileToTarGz(tOut, new File(child.getAbsolutePath()), entryName + "/");
	            }
	        }
	    }
	}
}