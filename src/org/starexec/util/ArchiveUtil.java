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
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.zip.GZIPInputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.ArchiveInputStream;
import org.apache.commons.compress.archivers.ArchiveStreamFactory;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream;
import org.apache.commons.compress.archivers.zip.ZipFile;
import org.apache.commons.compress.compressors.gzip.GzipCompressorOutputStream;
import org.apache.commons.compress.utils.IOUtils;
import org.apache.log4j.Logger;
import org.starexec.constants.R;

/**
 * Contains helper methods for dealing with archived files (.zip .tar .tar.gz, .tgz)
 */
public class ArchiveUtil {
	private static final Logger log = Logger.getLogger(ArchiveUtil.class);

	/**
	 *Checks to see if the given zip file is valid
	 */
	 public static boolean isValidZip(File file) {
		    ZipFile zipfile = null;
		    try {
		        zipfile = new ZipFile(file);
		        return true;
		    } catch (Exception e) {
		        return false;
		    } finally {
		        try {
		            if (zipfile != null) {
		                zipfile.close();
		                zipfile = null;
		            }
		        } catch (IOException e) {
		        }
		    }
		}
	
	
	/**
	 * Gets the uncompressed size of an archive
	 * @param filePath The path to the file to get the size of 
	 * @return The size of the uncompressed archive, in bytes
	 * @author Eric Burns
	 */
	public static long getArchiveSize(String filePath) {
		if (filePath.endsWith(".zip")){
			
			return getZipSize(filePath);
		} else if (filePath.endsWith(".tar")) {
			return getTarSize(filePath);
		} else if (filePath.endsWith(".tgz") || filePath.endsWith(".tar.gz")) {
			return getTarGzSize(filePath);
		} else {
			log.warn(String.format("Unsupported file extension for [%s] attempted to uncompress", filePath));
			return-1;
		}
		
	}
	/**
	 * Gets the uncompressed size of a zip archive
	 * @param filePath The path to the file to get the size of 
	 * @return The size of the uncompressed zip archive, in bytes
	 * @author Eric Burns
	 */
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
	/**
	 * Gets the uncompressed size of a tar archive
	 * @param filePath The path to the file to get the size of 
	 * @return The size of the uncompressed tar archive, in bytes
	 * @author Eric Burns
	 */
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
	/**
	 * Gets the APPROXIMATE uncompressed size of a tarGz archive. The actual value returned
	 * is the size of the TAR file, which will be slightly larger than the size of the completely 
	 * unarchived file.
	 * @param filePath The path to the file to get the size of 
	 * @return The size of the uncompressed TAR file, in bytes
	 * @author Eric Burns
	 */
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
	
	public static Boolean extractArchiveAsSandbox(String fileName,String destination) {
		log.debug("ExtractingArchive for " + fileName);
		try {
			// Check for the appropriate file extension and hand off to the appropriate method
			if(fileName.endsWith(".zip")) {
				String[] unzipCmd = new String[7];
				unzipCmd[0] = "sudo";
				unzipCmd[1] = "-u";
				unzipCmd[2] = "sandbox";
				unzipCmd[3] = "unzip";
				unzipCmd[4] = fileName;
				unzipCmd[5] = "-d";
				unzipCmd[6] = destination;
				log.debug("about to execute command unzip command");
				Util.executeCommand(unzipCmd);
				log.debug("now removing the archived file " + fileName);
				ArchiveUtil.removeArchive(fileName);
				
			} else if (fileName.endsWith(".tar.gz") || fileName.endsWith(".tgz") || fileName.endsWith(".tar")) {
				// First rename it if it's a .tgz

			
				/* by default, tar applies (supposedly) the user's umask when setting
				   permissions for extracted files.  So we do not need to do anything
				   further with that. */
				String[] tarCmd = new String[8];
				tarCmd[0] = "sudo";
				tarCmd[1] = "-u";
				tarCmd[2] = "sandbox";
				tarCmd[3] = "tar";
				tarCmd[4] = "-xf";
				tarCmd[5] = fileName;
				tarCmd[6] = "-C";
				tarCmd[7] = destination;
				log.debug("about to execute command tar command");
				Util.executeCommand(tarCmd);
				ArchiveUtil.removeArchive(fileName);

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
				String results = Util.executeCommand(lsCmd);
				log.debug("ls -l of tgz results = " + results);

				lsCmd[2] = destination;
				results = Util.executeCommand(lsCmd);
				log.debug("ls -l destination results = " + results);

				/* by default, tar applies (supposedly) the user's umask when setting
				   permissions for extracted files.  So we do not need to do anything
				   further with that. */
				String[] tarCmd = new String[5];
				tarCmd[0] = "tar";
				tarCmd[1] = "-xf";
				tarCmd[2] = fileName;
				tarCmd[3] = "-C";
				tarCmd[4] = destination;
				log.debug("about to execute command tar command");
				results = Util.executeCommand(tarCmd);
				log.debug("command was executed, results = " + results);
				log.debug("now removing the archived file " + fileName);
				ArchiveUtil.removeArchive(fileName);
				lsCmd[2] = destination;
				results = Util.executeCommand(lsCmd);
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
	 * @param removeTopLevel determines if archive should contain the top level directory or not (useful in reupload)
	 * @author Skylar Stark & Wyatt Kaiser
	 */
	
	public static void createArchive(File path, File destination, String format, String baseName, boolean removeTopLevel) {
		log.info("creating archive, path = " + path + ", dest = " + destination +", format = " + format);
		try {
			if (format.equals(".zip")) {
				ArchiveUtil.createZip(path, destination, baseName, removeTopLevel);
			} else if (format.equals(".tar") || format.equals(".tar.gz")) {
				ArchiveUtil.createTar(path, destination, baseName, removeTopLevel, format);
			}
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}
	}
	
	public static void createArchive(File path, File destination, String format, boolean removeTopLevel) {
		createArchive(path,destination,format,"", removeTopLevel);
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
	
	public static void addStringToArchive(ZipOutputStream zos, String str, String zipFileName) throws Exception {
		ZipEntry entry=new ZipEntry(zipFileName);
		zos.putNextEntry(entry);
		PrintWriter writer=new PrintWriter(zos);
		writer.write(str);
		writer.flush();
		zos.closeEntry();
	}
	
	
	public static void addFileToArchive(ZipOutputStream zos, File srcFile, String zipFileName) throws Exception {
		ZipEntry entry=new ZipEntry(zipFileName);
		zos.putNextEntry(entry);
		FileInputStream input=new FileInputStream(srcFile);
		
		IOUtils.copy(input, zos);
		zos.closeEntry();
		input.close();
	}
	
	public static void addDirToArchive(ZipOutputStream zos, File srcFile, String zipFileName) throws Exception {
		File[] files=srcFile.listFiles();
		for (int index=0;index<files.length;index++) {
			if (files[index].isDirectory()) {
				addDirToArchive(zos,files[index],zipFileName+File.separator+files[index].getName());
				continue;
			}
			addFileToArchive(zos,files[index],zipFileName+File.separator+files[index].getName());
		}
	}
	/**
	 * Writes several files to one zip file at the location indicated by the given outputstream
	 * @param paths The list of files to add to the zip
	 * @param output The outputstream to write to
	 * @param baseName If not null or empty, all files will be in one directory with this name
	 * @throws Exception
	 */
	public static void createAndOutputZip(List<File> paths, OutputStream output, String baseName) throws Exception {
		String newFileName=baseName;
		ZipOutputStream stream=new ZipOutputStream(output);
		for (File f : paths) {
			log.debug("adding new file to zip = "+f.getAbsolutePath());
			log.debug("directory status = "+f.isDirectory());
			if (baseName==null || baseName.length()==0) {
				newFileName=f.getName();
			} else {
				newFileName=baseName+File.separator+f.getName();
			}
			if (f.isDirectory()) {
				addDirToArchive(stream,f,newFileName);
			} else {
				addFileToArchive(stream,f,newFileName);
			}
		}
		stream.close();
	}
	/**
	 * Writes a directory recursively to a zip file at the location indicated by the given output stream.
	 * @param paths The directory or file to zip
	 * @param output The outputstream to write to
	 * @param baseName If not null or empty, all files will be in one directory with this name
	 * @param removeTopLevel If true, includes all files in the given directory but not the directory itself. Basename will
	 * be IGNORED if this is true. It should be set to false if the desire is to simply rename the top level.
	 * @throws Exception
	 */
	public static void createAndOutputZip(File path, OutputStream output, String baseName, boolean removeTopLevel) throws Exception {
		if (removeTopLevel) {
			File[] files=path.listFiles();
			List<File> f=new ArrayList<File>();
			for (File temp : files) {
				f.add(temp);
			}
			createAndOutputZip(f,output,"");
			return;
		}
		ZipOutputStream stream=new ZipOutputStream(output);
		boolean dir=path.isDirectory();
		if (baseName==null || baseName.length()>0) {
			if (dir) {
				addDirToArchive(stream,path,baseName);
			} else {
				addFileToArchive(stream,path,baseName+File.separator+path.getName());
			}
		} else {
			if (dir) {
				addDirToArchive(stream,path,path.getName());
			} else {
				addFileToArchive(stream,path,path.getName());
			}
		}
		
		stream.close();
		
	}
	
	
	/**
	 * Creates a .zip file of the specified directory "path" and saves it to "destination"
	 * @param path the path to be zipped
	 * @param destination where to save the .zip file created
	 * @param baseName-- the name to be given to the file specified in path
	 * @author Eric Burns
	 */
	public static void createZip(File path, File destination, String baseName, boolean removeTopLevel) throws Exception {
		
		
		//removing the top level is the same as just adding all the subdirectories to one zip archive
		if (removeTopLevel) {
			List<File> files=new ArrayList<File>();
			for (File f : path.listFiles()) {
				files.add(f);
			}
			createZip(files,destination);
			return;
		}
		
		//log.debug("creating zip, path = " + path + ", dest = " + destination);
		String[] zipCommand;
		
		//for some reason, zip fails if the name has dashes. We need to remove them, zip the file, then rename it
		String destName=destination.getName();
		String newDestName=destName.replace("-", "");
		File tempDest=new File(destination.getParentFile(),newDestName);
		File cd;
		
		cd=path.getParentFile();
		zipCommand=new String[6];
		zipCommand[0]="zip";
		zipCommand[1]="-r";
		zipCommand[2]="-q";
		zipCommand[3]="-1";
		zipCommand[4]=tempDest.getAbsolutePath();
		zipCommand[5]=path.getName(); //we are trying to run this command in the required directory, so an absolute path is not needed
	
		Util.executeCommand(zipCommand,null,cd);
		
		//put the dashes back into the file path
		if (!destName.equals(newDestName)) {
			String[] renameArchiveCommand=new String[3];
			renameArchiveCommand[0]="mv";
			renameArchiveCommand[1]=tempDest.getAbsolutePath();
			renameArchiveCommand[2]=destination.getAbsolutePath();
			Util.executeCommand(renameArchiveCommand);
		}
		
		//rename the top level if it exists and we have a name for it
		
		
		log.debug("the newly created archive exists = "+destination.exists());
		
		
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
	
	
	public static void createZip(List<File> paths, File destination) throws Exception {
		
		String[] zipCommand;
		
		//for some reason, zip fails if the name has dashes. We need to remove them, zip the file, then rename it
		String destName=destination.getName();
		String newDestName=destName.replace("-", "");
		File tempDest=new File(destination.getParentFile(),newDestName);
		for (File file : paths) {
			zipCommand=new String[5];
			zipCommand[0]="zip";
			zipCommand[1]="-r";
			zipCommand[2]="-q";
			zipCommand[3]=tempDest.getAbsolutePath();
			zipCommand[4]=file.getName(); //we will be executing the command from the parent directory of the needed file
			Util.executeCommand(zipCommand,null,file.getParentFile());
		}
		
		//put the dashes back into the file path
		if (!destName.equals(newDestName)) {
			String[] renameArchiveCommand=new String[3];
			renameArchiveCommand[0]="mv";
			renameArchiveCommand[1]=tempDest.getAbsolutePath();
			renameArchiveCommand[2]=destination.getAbsolutePath();
			Util.executeCommand(renameArchiveCommand);
		}
		
		
		
		
		/*
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
		}*/
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
	private static void addFileToZip(ZipArchiveOutputStream zOut, File path, String base, String baseName, boolean removeTopLevel, int progress) throws IOException {
		String entryName = null;
		if (removeTopLevel && (base.equals("\\"))) {
			base = "";
		}
		//log.debug("Base = " + base);
		if (baseName.equals("")) {
			entryName = base + path.getName();
			//log.debug("ENTRY NAME = " + entryName);
		} else {
			entryName=base+baseName;
		}
		
		//If user wants TopLevel folder
		if(!(removeTopLevel && progress == 0)) {
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
		} else {	//If user does not want top-level folder
			entryName = "";
		}
		File[] children = path.listFiles();

		if (children!=null) {
			//log.debug("Number of files = " + children.length);
			for (File child : children) {
				addChildToZip(zOut, child, entryName, removeTopLevel);
			}
		} else {
			//log.debug("Number of files = " + 1);
		}
		children = null;
	}

	private static void addChildToZip(ZipArchiveOutputStream zOut, File child, String entryName, boolean removeTopLevel) throws IOException{
		File tempChild = new File(child.getAbsolutePath());
		addFileToZip(zOut, tempChild, entryName + File.separator, "", removeTopLevel, 1);
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
	public static void createTar(File path, File destination, String baseName, boolean removeTopLevel, String format) throws Exception {
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

			addFileToTar(tOut, path, "", baseName, removeTopLevel, 0);
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
	private static void addFileToTar(TarArchiveOutputStream tOut, File path, String base, String baseName, boolean removeTopLevel, int progress) throws IOException {
		String entryName = null;
		if (removeTopLevel && base.equals("\\")) {
			base = "";
		}
		if (baseName.equals("")) {
			entryName = base + path.getName();
		} else {
			entryName=base+baseName;
		}
		
		if(!(removeTopLevel && progress == 0)) {
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
				addChildToTar(tOut, child, entryName, removeTopLevel);
			}
		}	
		children = null;
	}
	
	private static void addChildToTar(TarArchiveOutputStream tOut, File child, String entryName, boolean removeTopLevel) throws IOException{
		File tempChild = new File(child.getAbsolutePath());
		addFileToTar(tOut, tempChild, entryName + File.separator, "", removeTopLevel, 1);
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
	
	/**
	 * Creates a TarGz archive containing all the files in the given list
	 * @param files The files to archive
	 * @param destination The destination of the archive
	 * @throws Exception
	 */
	
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
	 * @param removeTopLevel true if you want to omit the removeTopLevel folder (useful for reupload)
	 * @author Skylar Stark
	 */
	private static void addFileToTarGz(TarArchiveOutputStream tOut, File path, String base, String baseName, boolean removeTopLevel, int progress) throws IOException {
		String entryName;
		if (removeTopLevel && base.equals("\\")) {
			base = "";
		}
		if (baseName.equals("")) {
			entryName = base + path.getName();
		} else {
			entryName=base+baseName;
		}
		
		log.debug("ENTRYNAME = " + entryName);
		if (removeTopLevel && progress==0) {
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
					addChildToTarGz(tOut, child, entryName, removeTopLevel);
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
						addChildToTarGz(tOut, child, entryName, removeTopLevel);
					}
				}
				children = null;
			}
		}
	}
	
	private static void addChildToTarGz(TarArchiveOutputStream tOut, File child, String entryName, boolean removeTopLevel) throws IOException {
		File tempChild = new File(child.getAbsolutePath());
		addFileToTarGz(tOut, tempChild, entryName + File.separator, "", removeTopLevel, 1);
		tempChild = null;
	}
}