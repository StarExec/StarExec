package org.starexec.util;

import java.util.zip.GZIPInputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.Enumeration;
import java.util.List;
import java.util.Queue;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.apache.commons.compress.archivers.ArchiveEntry;
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

	
	public static long getArchiveSize(String fileName) {
		if (fileName.endsWith(".zip")){
			
			return getZipSize(fileName);
		} else if (fileName.endsWith(".tar")) {
			return getTarSize(fileName);
		} else if (fileName.endsWith(".tgz") || fileName.endsWith(".tar.gz")) {
			return getTarGzSize(fileName);
		} else {
			log.warn(String.format("Unsupported file extension for [%s] attempted to uncompress", fileName));
			return-1;
		}
		
	}

	private static long getZipSize(String fileName) {
		try {
			long answer=0;
			ZipFile temp=new ZipFile(fileName);
			Enumeration<ZipArchiveEntry> x=temp.getEntries();
			while (x.hasMoreElements()) {
				answer=answer+x.nextElement().getSize();
			}
			temp.close();
			return answer;
		} catch (Exception e) {
			log.error("Archive Util says " + e.getMessage(), e);
			return -1;
		}
	}

	private static long getTarSize(String fileName) {
		try {
			InputStream is = new FileInputStream(fileName);
			BufferedInputStream bis = new BufferedInputStream(is);
			
			ArchiveInputStream ais = new ArchiveStreamFactory().createArchiveInputStream("tar", bis);
			ArchiveEntry entry = null;
			ais.getNextEntry().getSize();
			long answer=0;
			while ( (entry=ais.getNextEntry())!=null) {
				answer=answer+entry.getSize();
			}
			ais.close();
			bis.close();
			is.close();
			return answer;
		} catch (Exception e) {
			log.error("Archive Util says " + e.getMessage(), e);
			return -1;
		}
	}

	//Returns the size of the TAR file and not the size of the un-archived files within
	private static long getTarGzSize(String fileName) {
		try {
			FileInputStream instream= new FileInputStream(fileName);
			GZIPInputStream ginstream =new GZIPInputStream(instream);
			long answer=0;
			long temp=0;
			while (true) {
				temp=ginstream.skip(100000000);
				if (temp==0) {
					break;
				}
				
				answer+=temp;
			}
			instream.close();
			ginstream.close();
			return answer;
		} catch (Exception e) {
			log.error("Archive Util says " + e.getMessage(), e);
			return -1;
		}
	}
	
	
	/**
	 * Extracts/unpacks/uncompresses an archive file to a folder with the same name at the given destination.
	 * This method supports .zip, .tar and .tar.gz files. Once the contents are extracted the original archive file
	 * is deleted. Note if the extraction failed, some files/folders may have been partially created.
	 * @param fileName The full file path to the archive file
	 * @param destination The full path to the folder to extract the file to
	 * @return True if extraction was successful, false otherwise.
	 * 
	 * @author Tyler Jensen
	 */
	public static Boolean extractArchive(String fileName, String destination) {
		log.debug("ExtractingArchive for " + fileName);
		try {
			// Check for the appropriate file extension and hand off to the appropriate method
			if(fileName.endsWith(".zip")) {
				ArchiveUtil.extractZIP(fileName, destination);
			} else if(fileName.endsWith(".tar")) {
				ArchiveUtil.extractTAR(fileName, destination);
			} else if (fileName.endsWith(".tar.gz") || fileName.endsWith(".tgz")) {
				// First rename it if it's a .tgz

			    String[] lsCmd = new String[3];
			    lsCmd[0] = "ls";
			    lsCmd[1] = "-l";

				log.debug("destination is " + destination);

				lsCmd[2] = fileName;
				BufferedReader reader = Util.executeCommand(lsCmd);
				String results = Util.bufferToString(reader);
				log.debug("ls -l of tgz results = " + results);

				lsCmd[2] = destination;
				reader = Util.executeCommand(lsCmd);
				results = Util.bufferToString(reader);
				log.debug("ls -l destination results = " + results);

				//not verbose in case it's an issue with the buffer size
				String[] tarCmd = new String[5];
				tarCmd[0] = "tar";
				tarCmd[1] = "-xf";
				tarCmd[2] = fileName;
				tarCmd[3] = "-C";
				tarCmd[4] = destination;
				log.debug("about to execute command tar command");
				reader = Util.executeCommand(tarCmd);
				results = Util.bufferToString(reader);
				log.debug("command was executed, results = " + results);
				log.debug("now removing the archived file " + fileName);
				ArchiveUtil.removeArchive(fileName);
				lsCmd[2] = destination;
				reader = Util.executeCommand(lsCmd);
				results = Util.bufferToString(reader);
				log.debug("command was executed - ls -l destination results = " + results);

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
	public static Boolean extractArchive(String fileName) {
		try {
			String parent = new File(fileName).getParentFile().getCanonicalPath() + File.separator;
			return ArchiveUtil.extractArchive(fileName, parent);			
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}

		return true;
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
		
		is.close();
		bis.close();
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
		is.close();
		bis.close();
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
	 * @param baseName specifies the name that will be given to the file path.
	 * @author Skylar Stark & Wyatt Kaiser
	 */
	
	public static void createArchive(File path, File destination, String format, String baseName, boolean reupload) {
		log.info("creating archive, path = " + path + ", dest = " + destination +", format = " + format);
		try {
			if (format.equals(".zip")) {
				ArchiveUtil.createZip(path, destination, baseName, reupload);
			} else if (format.equals(".tar") || format.equals(".tar.gz")) {
				ArchiveUtil.createTar(path, destination, baseName, reupload, format);
			}
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}
	}
	
	public static void createArchive(File path, File destination, String format, boolean reupload) {
		createArchive(path,destination,format,"", reupload);
	}
	
	
	/**
	 * Creates an archive in the same way as above, but with a list of files
	 * @author Eric Burns
	 */
	
	public static void createArchive(List<File> files, File destination, String format) {
		log.info("creating archive with multiple paths, dest = "+destination+", format = "+format);
		try {
			if (format.equals(".zip")) {
				ArchiveUtil.createZip(files, destination);
			} else if (format.equals(".tar")) {
				ArchiveUtil.createTar(files, destination);
			} else if (format.equals(".tar.gz") || format.equals(".tgz")) {
				ArchiveUtil.createTarGz(files, destination);
			}
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}
		
	}

	
	
	/**
	 * Creates a .zip file of the specified directory "path" and saves it to "destination"
	 * @param path the path to be zipped
	 * @param destination where to save the .zip file created
	 * @param baseName-- the name to be given to the file specified in path
	 * @author Skylar Stark & Wyatt Kaiser
	 */
	public static void createZip(File path, File destination, String baseName, boolean reupload) throws Exception {
		log.debug("creating zip, path = " + path + ", dest = " + destination);

		FileOutputStream fOut = null;
		BufferedOutputStream bOut = null;
		ZipArchiveOutputStream zOut = null;

		try {
			fOut = new FileOutputStream(destination);
			bOut = new BufferedOutputStream(fOut);
			zOut = new ZipArchiveOutputStream(bOut);

			addFileToZip(zOut, path, "",baseName, reupload, 0);
		} finally {
			zOut.finish();

			zOut.close();
			bOut.close();
			fOut.close();
		}
	}
	
	/**
	 * Calls createZip without any name specified, so the name in path will be used
	 * @param path the path to be zipped
	 * @param destination
	 * 
	 */
	public static void createZip(File path, File destination) throws Exception {
		createZip(path,destination,"", false);
	}
	
	
	/**
	 * Creates a zip in the same way as above, but with multiple files
	 * @author Eric Burns
	 */
	public static void createZip(List<File> paths, File destination, String baseName) throws Exception {
		log.debug("creating zip, of multiple files, dest = " + destination);
		FileOutputStream fOut=null;
		BufferedOutputStream bOut = null;
		ZipArchiveOutputStream zOut = null;
		try {
			fOut=new FileOutputStream(destination);
			bOut=new BufferedOutputStream(fOut);
			zOut = new ZipArchiveOutputStream(bOut);
			for (File x : paths ) {
				addFileToZip(zOut,x,"",baseName, false, 0);
			}
		} finally {
			zOut.finish();
			zOut.close();
			bOut.close();
			fOut.close();
		}
	}
	
	public static void createZip(List<File> paths, File destination) throws Exception {
		createZip(paths,destination,"");
	}	
	
	
	/**
	 * Adds a file to the .zip archive. If the file is a folder, recursively adds the contents of the
	 * folder to the archive
	 * @param zOut the zip file we are creating
	 * @param path the path of the file we are adding
	 * @param base the base prefix for the name of the zip file entry
	 * 
	 * @author Skylar Stark & Wyatt Kaiser
	 */
	private static void addFileToZip(ZipArchiveOutputStream zOut, File path, String base, String baseName, boolean reupload, int progress) throws IOException {
		String entryName = null;
		String curDesc = "no description";
		if (reupload && (base.equals("\\"))) {
			base = "";
		}
		log.debug("Base = " + base);
		if (baseName.equals("")) {
			entryName = base + path.getName();
			log.debug("ENTRY NAME = " + entryName);
		} else {
			entryName=base+baseName;
		}
		
		//If not download for re-upload and first iteration
		if(!(reupload && progress == 0)) {
			ZipArchiveEntry zipEntry = new ZipArchiveEntry(path, entryName);
			zOut.putArchiveEntry(zipEntry);
			
			if (path.isFile()) {
				FileInputStream fis = new FileInputStream(path);
				IOUtils.copy(fis, zOut);
				fis.close(); 
				zOut.closeArchiveEntry();
			} else {
				zOut.closeArchiveEntry();
			}
		} else {	//If it is download for re-upload and it is the first iteration
			entryName = "";
		}
	
		File[] children = path.listFiles();

		if (children!=null) {
			log.debug("Number of files = " + children.length);
			/*
			for (File child: children) {
				log.debug("NAME = " + child.getName());
				if (child.getName().equals(R.SOLVER_DESC_PATH)) {
					log.debug("Description has been located");
				}
			}
			*/
			/*
			//If there does not exist a description file already, and there DOES exist a description on the web-site
			
			if (descriptions.size() != 0) {
				curDesc = descriptions.remove();
				log.debug("DESCRIPTION = " + curDesc);
			}
			
			
			if (descriptionSeen == false && !(curDesc.equals("no description")) ) {
				File description = new File(path.getAbsolutePath() + File.separator + R.SOLVER_DESC_PATH);
				log.debug("Description not seen");
				
				//Write to the description file
				FileWriter fw = new FileWriter(description.getAbsoluteFile());
				BufferedWriter bw = new BufferedWriter(fw);
				bw.write(curDesc);
				bw.close();

				addFileToZip(zOut, description, entryName + File.separator , "", reupload, 1, descriptions, true);
				descriptionSeen = true;
			}
			*/
		} else {
			log.debug("Number of files = " + 1);
		}
		if (children != null) {
			for (File child : children) {
				addChildToZip(zOut, child, entryName, reupload);
			}
		}
		children = null;
	}

	private static void addChildToZip(ZipArchiveOutputStream zOut, File child, String entryName, boolean reupload) throws IOException{
		File tempChild = new File(child.getAbsolutePath());
		addFileToZip(zOut, tempChild, entryName + File.separator, "", reupload, 1);
		tempChild = null;

	}

	/**
	 * Creates a .tar or .tar.gz file of the specified directory "path" and saves it to "destination"
	 * @param path the path to be tarred
	 * @param destination where to save the .tar file created
	 * @param baseName the name given to the file specified in path
	 * 
	 * @author Skylar Stark
	 */
	public static void createTar(File path, File destination, String baseName, boolean reupload, String format) throws Exception {
		FileOutputStream fOut = null;
		BufferedOutputStream bOut = null;
		GzipCompressorOutputStream gzOut = null;
		TarArchiveOutputStream tOut = null;

		try {
			fOut = new FileOutputStream(destination);
			bOut = new BufferedOutputStream(fOut);
			if (format.equals(".tar.gz")) {
				gzOut = new GzipCompressorOutputStream(bOut);
				tOut = new TarArchiveOutputStream(gzOut);
			} else {
				tOut = new TarArchiveOutputStream(bOut);
			}

			addFileToTar(tOut, path, "", baseName, reupload, 0);
		} finally {
			tOut.finish();

			tOut.close();
			bOut.close();
			fOut.close();
		}
	}
	
	public static void createTar(File path, File destination) throws Exception {
		createTar(path,destination,"", false, ".tar");
	}
	
	public static void createTarGz(File path, File destination) throws Exception {
		createTar(path, destination, "", false, ".tar.gz");
	}
	
	/**
	 * Creates a Tar as above, but with a list of files
	 * @author Eric Burns
	 */
	
	public static void createTar(List<File> files, File destination, String baseName) throws Exception {
		FileOutputStream fOut = null;
		BufferedOutputStream bOut = null;
		TarArchiveOutputStream tOut = null;

		try {
			fOut = new FileOutputStream(destination);
			bOut = new BufferedOutputStream(fOut);
			tOut = new TarArchiveOutputStream(bOut);
			for (File x: files) {
				addFileToTar(tOut, x, "", baseName, false, 0);
			}
			
		} finally {
			tOut.finish();

			tOut.close();
			bOut.close();
			fOut.close();
		}
	}
	
	public static void createTar(List<File> files, File destination) throws Exception {
		createTar(files,destination,"");
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
	private static void addFileToTar(TarArchiveOutputStream tOut, File path, String base, String baseName, boolean reupload, int progress) throws IOException {
		String entryName = null;
		if (reupload && base.equals("\\")) {
			base = "";
		}
		if (baseName.equals("")) {
			entryName = base + path.getName();
		} else {
			entryName=base+baseName;
		}
		
		if(!(reupload && progress == 0)) {
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
			}
		} else {
			entryName = "";
		}
	
		File[] children = path.listFiles();

		if (children!=null) {
			log.debug("Number of files = " + children.length);
		} else {
			log.debug("Number of files = " + 1);
		}
		if (children != null) {
			for (File child : children) {
				addChildToTar(tOut, child, entryName, reupload);
			}
		}	
		children = null;
	}
	
	private static void addChildToTar(TarArchiveOutputStream tOut, File child, String entryName, boolean reupload) throws IOException{
		File tempChild = new File(child.getAbsolutePath());
		addFileToTar(tOut, tempChild, entryName + File.separator, "", reupload, 1);
		tempChild = null;

	}
	
	/**
	 * Creates a TarGz as above, but with a list of files
	 * @author Eric Burns
	 */
	
	public static void createTarGz(List<File> files, File destination, String baseName) throws Exception {
		FileOutputStream fOut = null;
		BufferedOutputStream bOut = null;
		GzipCompressorOutputStream gzOut = null;
		TarArchiveOutputStream tOut = null;

		try {
			fOut = new FileOutputStream(destination);
			bOut = new BufferedOutputStream(fOut);
			gzOut = new GzipCompressorOutputStream(bOut);
			tOut = new TarArchiveOutputStream(gzOut);
			for (File x : files) {
				addFileToTarGz(tOut, x, "", baseName, false, 0);
			}
			
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
	
	public static void createTarGz(List<File> files, File destination) throws Exception {
		createTarGz(files,destination,"");
	}

	/**
	 * Adds a file to a .tar.gz archive. If the file is a folder, recursively adds the contents of the
	 * folder to the archive
	 * @param tOut the tar.gz file we are adding to
	 * @param path the path of the file we are adding
	 * @param base the base prefix for the name of the tar.gz file entry
	 * @param baseName the name given to the file. If empty, the name of path is used
	 * @author Skylar Stark
	 */
	private static void addFileToTarGz(TarArchiveOutputStream tOut, File path, String base, String baseName, boolean reupload, int progress) throws IOException {
		String entryName;
		if (reupload && base.equals("\\")) {
			base = "";
		}
		if (baseName.equals("")) {
			entryName = base + path.getName();
		} else {
			entryName=base+baseName;
		}
		
		log.debug("ENTRYNAME = " + entryName);
		if (reupload && progress==0) {
			entryName = "";
			log.debug("entryName as been reset");
			File[] children = path.listFiles();
			if (children != null) {
				log.debug("Number of files = " + children.length);
			} else {
				log.debug("Number of files = " + 1);
			}
			
			if (children != null) {
				for (File child: children) {
					addChildToTarGz(tOut, child, entryName, reupload);
				}
			}
		} else {
		
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
					log.debug("Number of files = " + children.length);
				} else {
					log.debug("Number of files = " + 1);
				}
				if (children != null) {
					for (File child : children) {
						addChildToTarGz(tOut, child, entryName, reupload);
					}
				}
				children = null;
			}
		}
	}
	
	private static void addChildToTarGz(TarArchiveOutputStream tOut, File child, String entryName, boolean reupload) throws IOException {
		File tempChild = new File(child.getAbsolutePath());
		addFileToTarGz(tOut, tempChild, entryName + File.separator, "", reupload, 1);
		tempChild = null;
	}
}