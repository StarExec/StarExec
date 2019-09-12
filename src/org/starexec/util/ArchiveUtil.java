package org.starexec.util;

import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.ArchiveInputStream;
import org.apache.commons.compress.archivers.ArchiveStreamFactory;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream;
import org.apache.commons.compress.archivers.zip.ZipFile;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;
import org.apache.commons.compress.utils.IOUtils;
import org.starexec.constants.R;
import org.starexec.logger.StarLogger;

import java.io.*;
import java.util.*;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.Files;

/**
 * Contains helper methods for dealing with .zip files
 */
public class ArchiveUtil {
	private static final StarLogger log = StarLogger.getLogger(ArchiveUtil.class);

	/**
	 * Gets the uncompressed size of an archive
	 *
	 * @param filePath The path to the file to get the size of
	 * @return The size of the uncompressed archive, in bytes
	 * @author Eric Burns
	 */
	public static long getArchiveSize(String filePath) {
		if (filePath.endsWith(".zip")) {

			return getZipSize(filePath);
		} else if (filePath.endsWith(".tar")) {
			return getTarSize(filePath);
		} else if (filePath.endsWith(".tgz") || filePath.endsWith(".tar.gz")) {
			return getTarGzSize(filePath);
		} else {
			log.warn(String.format("Unsupported file extension for [%s] attempted to uncompress", filePath));
			return -1;
		}

	}

	/**
	 * Gets the uncompressed size of a zip archive
	 *
	 * @param fileName The path to the file to get the size of
	 * @return The size of the uncompressed zip archive, in bytes
	 * @author Eric Burns
	 */
	private static long getZipSize(String fileName) {
		try {
			long answer = 0;
			ZipFile temp = new ZipFile(fileName);
			Enumeration<ZipArchiveEntry> x = temp.getEntries();
			while (x.hasMoreElements()) {
				answer += x.nextElement().getSize();
			}
			temp.close();
			return answer;
		} catch (Exception e) {
			log.error("getZipSize", e);
			return -1;
		}
	}

	/**
	 * Gets the uncompressed size of a tar archive
	 *
	 * @param fileName The path to the file to get the size of
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
			long answer = 0;
			while ((entry = ais.getNextEntry()) != null) {
				answer += entry.getSize();
			}
			ais.close();
			bis.close();
			is.close();
			return answer;
		} catch (Exception e) {
			log.error("getTarSize", e);
			return -1;
		}
	}

	/**
	 * Gets the APPROXIMATE uncompressed size of a tarGz archive. The actual value returned
	 * is the size of the TAR file, which will be slightly larger than the size of the completely
	 * unarchived file.
	 *
	 * @param fileName The path to the file to get the size of
	 * @return The size of the uncompressed TAR file, in bytes
	 * @author Eric Burns
	 */
	//Returns the size of the TAR file and not the size of the un-archived files within
	private static long getTarGzSize(String fileName) {
		try {
			FileInputStream instream = new FileInputStream(fileName);
			GzipCompressorInputStream ginstream = new GzipCompressorInputStream(instream);
			long answer = 0;
			long temp;
			do {
				temp = ginstream.skip(100000000);
				answer += temp;
			} while (temp != 0);
			instream.close();
			ginstream.close();
			return answer;
		} catch (Exception e) {
			log.error("getTarGzSize", e);
			return -1;
		}
	}

	/**
	 * Extracts an archive as the sandbox user, meaning the files extracted
	 * will be owned by the sandbox user
	 *
	 * @param fileName The absolute path to the archive
	 * @param destination The directory to place the output in
	 * @return True on success and false otherwise
	 */
	public static Boolean extractArchiveAsSandbox(String fileName, String destination) {
		log.debug("ExtractingArchive for " + fileName);
		try {
			// Check for the appropriate file extension and hand off to the appropriate method
			if (fileName.endsWith(".zip")) {
				String[] unzipCmd = new String[7];
				unzipCmd[0] = "sudo";
				unzipCmd[1] = "-u";
				unzipCmd[2] = R.SANDBOX_USER_ONE;
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

				String[] tarCmd = new String[8];
				tarCmd[0] = "sudo";
				tarCmd[1] = "-u";
				tarCmd[2] = R.SANDBOX_USER_ONE;
				tarCmd[3] = "tar";
				tarCmd[4] = "-xf";
				tarCmd[5] = fileName;
				tarCmd[6] = "-C";
				tarCmd[7] = destination;
				log.debug("about to execute command tar command");
				Util.executeCommand(tarCmd);
				ArchiveUtil.removeArchive(fileName);
				Util.chmodDirectory(destination, false);

			} else {
				// No valid file type found :(
				log.warn(String.format("Unsupported file extension for [%s] attempted to uncompress", fileName));
				return false;
			}

			log.debug(String.format("Successfully extracted [%s] to [%s]", fileName, destination));
			return true;
		} catch (Exception e) {
			log.error("extractArchiveAsSandbox", e);
		}

		return false;
	}

	/**
	 * Extracts/unpacks/uncompresses an archive file to a folder with the same name at the given destination.
	 * This method supports .zip, .tar and .tar.gz files. Once the contents are extracted the original archive file
	 * is deleted. Note if the extraction failed, some files/folders may have been partially created.
	 *
	 * @param fileName The full file path to the archive file
	 * @param destination The full path to the folder to extract the file to
	 * @return True if extraction was successful, false otherwise.
	 * @author Tyler Jensen
	 */
	public static Boolean extractArchive(String fileName, String destination) {
		log.debug("ExtractingArchive for " + fileName);
		try {
			// Check for the appropriate file extension and hand off to the appropriate method
			if (fileName.endsWith(".zip")) {
				ArchiveUtil.extractArchiveOfType(fileName, destination, ArchiveType.ZIP);
			} else if (fileName.endsWith(".tar")) {
				ArchiveUtil.extractArchiveOfType(fileName, destination, ArchiveType.TAR);
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

				/* it appears that the permissions are not set correctly by tar for tomcat.
				   We need group read permissions for files, because elsewhere,
				   we try to copy these files as the sandbox user, and hence need
				   to be able to read them.  So we do an explicit chmod*/
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

				// now chmod the directory so sandbox can access it

				Util.chmodDirectory(destination, true);

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
			log.error("extractArchive", e);
		}

		return false;
	}

	/**
	 * Extracts/unpacks/uncompresses an archive file to the same folder the archive file exists within.
	 * This method supports .zip, .tar and .tar.gz files. Once the contents are extracted the original archive file
	 * is deleted. Note if the extraction failed, some files/folders may have been partially created.
	 *
	 * @param fileName The full file path to the archive file
	 * @author Tyler Jensen
	 */
	public static void extractArchive(String fileName) {
		try {
			String parent = new File(fileName).getParentFile().getCanonicalPath() + File.separator;
			ArchiveUtil.extractArchive(fileName, parent);
		} catch (Exception e) {
			log.error("extractArchive", e);
		}
	}

	/**
	 * Unpacks a tar file and removes the original if the unpack was successful.
	 *
	 * @param fileName The full path to the file
	 * @param destination Where to unpack the contents to
	 * @author Tyler Jensen
	 */
	private static void extractArchiveOfType(String fileName, String destination, ArchiveType archiveType) throws
			Exception {
		final String methodName = "extractArchiveOfType";
		// Use the Apache commons compression library to open up the tar file...
		log.debug("extracting " + archiveType);
		InputStream is = new FileInputStream(fileName);
		BufferedInputStream bis = new BufferedInputStream(is);
		ArchiveInputStream ais = new ArchiveStreamFactory().createArchiveInputStream(archiveType.type, bis);
		ArchiveEntry entry = null;

		// For each 'file' in the tar file...
		while ((entry = ais.getNextEntry()) != null) {
			if (!entry.isDirectory()) {
				// If it's not a directory...
				File fileToCreate = new File(destination, entry.getName());
				File dir = new File(fileToCreate.getParent());
				boolean success = true;
				if (!dir.exists()) {
					// And create it if it doesn't exist so we can write a file inside it
					success = dir.mkdirs();
					if (!success) {
						log.warn("Could not create directory: " + dir.getAbsolutePath() + "\n" +
						         Util.getCurrentStackTrace());
						log.warn(methodName, "Did file already exist: " + dir.exists());
						log.warn(methodName, "User was: " + System.getProperty("user.name"));
						log.warn(methodName, "canWrite for file: " + dir.canWrite());
					}
				}
				if (success) {
					// Finally, extract the file
					OutputStream out = new FileOutputStream(fileToCreate);
					IOUtils.copy(ais, out);
					out.close();
				}

			}
		}
		is.close();
		bis.close();
		ais.close();
		ArchiveUtil.removeArchive(fileName);
	}

	/**
	 * Checks the global remove archive setting and removes the archive file if the setting is true.
	 *
	 * @param fileName The path to the archive file to remove
	 */
	private static void removeArchive(String fileName) {
		if (R.REMOVE_ARCHIVES) {
			if (new File(fileName).delete()) {
				log.debug("Cleaned up archive file: " + fileName);
			} else {
				log.warn("Failed to cleanup archive file: " + fileName);
			}
		}
	}

	/**
	 * Adds a raw string to a zip archive, saving the string in a file specified by zipFileName
	 *
	 * @param zos
	 * @param str
	 * @param zipFileName
	 * @throws Exception
	 */
	public static void addStringToArchive(ZipArchiveOutputStream zos, String str, String zipFileName) throws Exception {
		final byte[] data = str.getBytes(zos.getEncoding());
		java.util.zip.CRC32 crc = new java.util.zip.CRC32();
		crc.update(data);
		ZipArchiveEntry entry = new ZipArchiveEntry(zipFileName);
		entry.setSize(data.length);
		entry.setCrc(crc.getValue());
		entry.setMethod(ZipArchiveEntry.STORED);
		entry.setInternalAttributes(1);
		zos.putArchiveEntry(entry);
		zos.write(data, 0, data.length);
		zos.closeArchiveEntry();
	}

	/**
	 * Adds the given file to the given output stream if and only if it was modified after some timestamp
	 *
	 * @param zos
	 * @param srcFile
	 * @param zipFileName
	 * @param earlyDate Milliseconds since the epoch. Only get files modified after this (non-inclusive)
	 * @return max of timestamp and earlyDate
	 * @throws IOException
	 */
	public static long addFileToArchive(ZipArchiveOutputStream zos, File srcFile, String zipFileName, long earlyDate) throws
			IOException {
		long timestamp = srcFile.lastModified();
		if (timestamp > earlyDate) {
			addFileToArchive(zos, srcFile, zipFileName);
			return timestamp;
		}
		return earlyDate;
	}

	/**
	 * Calculate Unix file permissions for file f
	 * @param f File to calculate permissions for
	 * @return Unix file permissions
	 */
	private static int getUnixMode(File f) throws IOException {
		return (Integer) Files.getAttribute(f.toPath(), "unix:mode");
	}

	/**
	 * Adds the given source file to the given zip output stream using the given name
	 *
	 * @param zos
	 * @param srcFile
	 * @param zipFileName
	 * @return timestamp of file added
	 * @throws IOException
	 */
	public static long addFileToArchive(ZipArchiveOutputStream zos, File srcFile, String zipFileName) throws IOException {
		ZipArchiveEntry entry = new ZipArchiveEntry(srcFile, zipFileName);
		try {
			long timestamp = srcFile.lastModified();
			zos.putArchiveEntry(entry);
			FileInputStream input = new FileInputStream(srcFile);
			entry.setUnixMode(getUnixMode(srcFile));
			entry.setSize(srcFile.length());
			//entry.setInternalAttributes(Util.isBinaryFile(srcFile)?0:1);
			IOUtils.copy(input, zos);
			zos.closeArchiveEntry();
			input.close();
			return timestamp;
		} catch (java.io.FileNotFoundException e) {
			if (srcFile.getCanonicalPath().equals(srcFile.getAbsolutePath())) {
				throw e;
			}
			log.debug("File not found exception probably broken symlink for: " + srcFile.getAbsolutePath());
			return -1;
		}

	}

	/**
	 * Recursively adds the given directory to the given zipoutputstream, using the given name as the prefix
	 * for all files that get added. Only add files that were modified after the specified time.
	 *
	 * @param zos
	 * @param srcFile
	 * @param zipFileName
	 * @param earlyDate
	 * @return max of earlyDate and timestamp of most recently modified file
	 * @throws IOException
	 */
	public static long addDirToArchive(ZipArchiveOutputStream zos, File srcFile, String zipFileName, long earlyDate) throws
			IOException {
		long maxTime = earlyDate;
		final File[] files = srcFile.listFiles();
		for (File file : files) {
			final long t;
			final String fileName = zipFileName + File.separator + file.getName();
			if (file.isDirectory()) {
				t = addDirToArchive(zos, file, fileName, earlyDate);
			} else {
				t = addFileToArchive(zos, file, fileName, earlyDate);
			}

			if (t > maxTime) {
				maxTime = t;
			}
		}
		return maxTime;
	}

	/**
	 * See addDirToArchive overload. All files are included with no date filter
	 *
	 * @param zos
	 * @param srcFile
	 * @param zipFileName
	 * @throws IOException
	 */
	public static long addDirToArchive(ZipArchiveOutputStream zos, File srcFile, String zipFileName) throws IOException {
		return addDirToArchive(zos, srcFile, zipFileName, -1);
	}

	/**
	 * Writes several files to one zip file at the location indicated by the given outputstream
	 *
	 * @param paths The list of files to add to the zip
	 * @param output The outputstream to write to
	 * @param baseName If not null or empty, all files will be in one directory with this name
	 * @throws IOException
	 */
	public static void createAndOutputZip(Iterable<File> paths, OutputStream output, String baseName) throws IOException {
		String newFileName = baseName;
		ZipArchiveOutputStream stream = new ZipArchiveOutputStream(output);
		for (File f : paths) {
			log.debug("adding new file to zip = " + f.getAbsolutePath());
			log.debug("directory status = " + f.isDirectory());
			
			if (Util.isNullOrEmpty(baseName)) {
				newFileName = f.getName();
			} else {
				newFileName = baseName + File.separator + f.getName();
			}

			if (f.isDirectory()) {
				addDirToArchive(stream, f, newFileName);
			} else {
				addFileToArchive(stream, f, newFileName);
			}
		}
		stream.finish();
		stream.flush();
		stream.close();
	}

	/**
	 * Writes a directory recursively to a zip file at the location indicated by the given output stream.
	 *
	 * @param path The directory or file to zip
	 * @param output The outputstream to write to
	 * @param baseName If not null or empty, all files will be in one directory with this name
	 * @param removeTopLevel If true, includes all files in the given directory but not the directory itself. Basename
	 * will
	 * be IGNORED if this is true. It should be set to false if the desire is to simply rename the top level.
	 * @throws Exception
	 */
	public static void createAndOutputZip(File path, OutputStream output, String baseName, boolean removeTopLevel)
			throws IOException {
		log.debug("Creating and outputting .zip file...");
		if (removeTopLevel) {
			File[] files = path.listFiles();
			List<File> f = Arrays.asList(files);
			createAndOutputZip(f, output, "");
			return;
		}
		ZipArchiveOutputStream stream = new ZipArchiveOutputStream(output);
		boolean dir = path.isDirectory();
		if (!Util.isNullOrEmpty(baseName)) {
			if (dir) {
				addDirToArchive(stream, path, baseName);
			} else {
				addFileToArchive(stream, path, baseName + File.separator + path.getName());
			}
		} else {
			if (dir) {
				addDirToArchive(stream, path, path.getName());
			} else {
				addFileToArchive(stream, path, path.getName());
			}
		}
		stream.close();
	}

	private enum ArchiveType {
		ZIP("zip"), TAR("tar");

		final String type;

		ArchiveType(String type) {
			this.type = type;
		}
	}

}
