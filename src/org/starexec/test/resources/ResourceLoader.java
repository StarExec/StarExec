package org.starexec.test.resources;

import java.io.File;
import java.net.URL;
import java.util.List;
import java.util.UUID;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import org.starexec.data.database.Solvers;
import org.starexec.data.database.Spaces;
import org.starexec.data.database.Uploads;
import org.starexec.data.database.Users;
import org.starexec.data.to.Configuration;
import org.starexec.data.to.Permission;
import org.starexec.data.to.Solver;
import org.starexec.data.to.Space;
import org.starexec.data.to.User;
import org.starexec.servlets.BenchmarkUploader;
import org.starexec.test.TestUtil;
import org.starexec.util.ArchiveUtil;

public class ResourceLoader {
	private static final Logger log = Logger.getLogger(ResourceLoader.class);	
	
	public static String getResourcePath() {
		return ResourceLoader.class.getResource("/org/starexec/test/resources").getFile();
	}
	public static File getResource(String name) {
		URL url=ResourceLoader.class.getResource("/org/starexec/test/resources/"+"CVC4.zip");
		System.out.println(url);
		return new File(ResourceLoader.class.getResource("/org/starexec/test/resources/"+name).getFile());
	}
	
	public static File getDownloadDirectory() {
		String filePath=getResourcePath();
		File file=new File(filePath,"downloads");
		file.mkdir();
		return file;
	}
	
	
	
	
	public static List<Integer> loadBenchmarksIntoDatabase(String archiveName, int parentSpaceId, int userId) {
		try {
			File archive=getResource(archiveName);
			
			//make a copy of the archive, because the benchmark extraction function will delete the archive
			File archiveCopy=new File(getDownloadDirectory(),UUID.randomUUID()+archive.getName());
			FileUtils.copyFile(archive, archiveCopy);
			Integer statusId = Uploads.createUploadStatus(parentSpaceId, userId);
			Permission p=new Permission();
			List<Integer> ids=BenchmarkUploader.handleUploadRequestAfterExtraction(archiveCopy, userId, parentSpaceId, 1, false, p, 
					"dump", statusId, false, false, null);
			return ids;
		} catch (Exception e) {
			log.error("loadBenchmarksIntoDatabase says "+e.getMessage(),e);
		}
		return null;
		
		
		
	}
	
	public static Solver loadSolverIntoDatabase(String archiveName, int parentSpaceId, int userId) {
		try {
			Solver s=new Solver();
			s.setName(TestUtil.getRandomSolverName());
			s.setDescription("solver coming from here");
			s.setUserId(userId);
			File archive=getResource(archiveName);
			String filePath=Solvers.getDefaultSolverPath(userId, s.getName());
			s.setPath(filePath);
			File solverDir=new File(filePath);
			solverDir.mkdirs();
			
			FileUtils.copyFileToDirectory(archive, solverDir);
			File archiveFile=new File(solverDir,s.getName());
			ArchiveUtil.extractArchive(archiveFile.getAbsolutePath());
			
			//Find configurations from the top-level "bin" directory
			for(Configuration c : Solvers.findConfigs(solverDir.getAbsolutePath())) {
				s.addConfiguration(c);
			}
			
			int id=Solvers.add(s, parentSpaceId);
			if (id>0) {
				s.setId(id);
				return s;
			} else {
				// there was an error of some kind
				return null;
			}
		} catch (Exception e) {
			log.error("loadSolverIntoDatabase says "+e.getMessage(),e);
		}
		return null;	
	}
	
	public static Space loadSpaceIntoDatabase(int userId, int parentSpaceId) {
		Space space=new Space();
		space.setName(TestUtil.getRandomSpaceName());
		space.setDescription("test desc");
		int id=Spaces.add(space, parentSpaceId, userId);
		if (id>0) {
			space.setId(id);
			return space;
		} else {
			return null;
		}
	}
	/**
	 * Loads a user into the database, without any particular name, email, password, and so on. Useful for testing.
	 * @return The user, with their ID and all parameters set, or null on error
	 */
	public static User loadUserIntoDatabase() {
		return loadUserIntoDatabase("test","user",TestUtil.getRandomPassword(),TestUtil.getRandomPassword(),"The University of Iowa","user");
	}
	/**
	 * Creates a user with the given attributes and adds them to the database
	 * @param fname The first name for the user
	 * @param lname The last name for the user
	 * @param email The email of the user
	 * @param password The plaintext password for the user
	 * @param institution
	 * @param role The role of the user-- should be either "user" or "admin"
	 * @return The User on success, or null on error. Their ID will be set on success.
	 */
	public static User loadUserIntoDatabase(String fname, String lname, String email, String password, String institution, String role) {
		User u=new User();
		u.setFirstName(fname);
		u.setLastName(lname);
		u.setPassword(password);
		u.setEmail(email);
		u.setInstitution(institution);
		u.setRole(role);
		int id=Users.add(u);
		if (id>0) {
			u.setId(id);
			return u;
		}
		log.debug("loadUserIntoDatabase could not generate a user, returning null");
		return null;
	}
	
}
