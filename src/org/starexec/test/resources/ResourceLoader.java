package org.starexec.test.resources;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import org.starexec.data.database.Jobs;
import org.starexec.data.database.Queues;
import org.starexec.data.database.Solvers;
import org.starexec.data.database.Spaces;
import org.starexec.data.database.Uploads;
import org.starexec.data.database.Users;
import org.starexec.data.to.Benchmark;
import org.starexec.data.to.Configuration;
import org.starexec.data.to.Job;
import org.starexec.data.to.Permission;
import org.starexec.data.to.Queue;
import org.starexec.data.to.Solver;
import org.starexec.data.to.Space;
import org.starexec.data.to.User;
import org.starexec.jobs.JobManager;
import org.starexec.servlets.BenchmarkUploader;
import org.starexec.test.TestUtil;
import org.starexec.util.ArchiveUtil;
import org.starexec.util.Util;

public class ResourceLoader {
	private static final Logger log = Logger.getLogger(ResourceLoader.class);	
	
	public static String getResourcePath() {
		return ResourceLoader.class.getResource("/org/starexec/test/resources").getFile();
	}
	public static File getResource(String name) {
		URL url=ResourceLoader.class.getResource("/org/starexec/test/resources/"+"CVC4.zip");
		return new File(ResourceLoader.class.getResource("/org/starexec/test/resources/"+name).getFile());
	}
	
	public static File getDownloadDirectory() {
		String filePath=getResourcePath();
		File file=new File(filePath,"downloads");
		file.mkdir();
		return file;
	}
	
	/**
	 * This will load a job with the given solvers and benchmarks into the database, after which it should
	 * be picked up by the periodic task and started running. It is soemwhat primitive right now-- for every solver
	 * a single configuration will be picked randomly to be added to the job, and the job will be added to a random
	 * queue that the given user owns
	 * @param spaceId The ID of the space to put the job in
	 * @param userId The ID of the user who will own the job
	 * @param solvers The solverIds that will be matched to every benchmark
	 * @param benchmarks The benchmarkIDs to run
	 * @return The job object
	 */
	public static Job loadJobIntoDatabase(int spaceId, int userId, int preProcessorId, int postProcessorId, List<Integer> solverIds, List<Integer> benchmarkIds) {
		
		Space space=Spaces.get(spaceId);
		String name=TestUtil.getRandomSpaceName();
		Queue q=Queues.getUserQueues(userId).get(0);
		Job job=JobManager.setupJob(userId, name, "test job", preProcessorId, postProcessorId, q.getId());
		
		
		List<Integer> configIds=new ArrayList<Integer>();
		for (Integer i : solverIds) {
			configIds.add(Solvers.getConfigsForSolver(i).get(0).getId());
		}
		List<Space> spaces=new ArrayList<Space>();
		spaces.add(Spaces.get(spaceId));
		HashMap<Integer,String> SP = new HashMap<Integer,String>();
		SP.put(spaceId, Spaces.get(spaceId).getName());
		JobManager.buildJob(job, userId, 100, 100, Util.gigabytesToBytes(1), benchmarkIds, solverIds, configIds, spaceId, SP);
		Jobs.add(job, spaceId);
		return job;
	}
	
	public static Configuration loadConfigurationIntoDatabase(File contentFile, int solverId)  {
		try {
			return loadConfigurationIntoDatabase(FileUtils.readFileToString(contentFile), solverId);

		} catch(Exception e) {
			log.error("loadConfigurationIntoDatabase says "+e.getMessage(),e);
		}
		return null;
	}
	
	public static Configuration loadConfigurationIntoDatabase(String contents,int solverId)  {
		try {
			Configuration c=new Configuration();
			c.setName(TestUtil.getRandomSolverName());
			c.setSolverId(solverId);
			Solver solver=Solvers.get(solverId);
			// Build a path to the appropriate solver bin directory and ensure the file pointed to by newConfigFile doesn't already exist
			File newConfigFile = new File(Util.getSolverConfigPath(solver.getPath(), c.getName()));
			// If a configuration file exists on disk with the same name, just throw an error. This really should never
			//happen because we are given the configs long, random names, so we have a problem if this occurs
			if(newConfigFile.exists()){
				return null;
			}
			FileUtils.writeStringToFile(newConfigFile, contents);
			
			// Make sure the configuration has the right line endings
			Util.normalizeFile(newConfigFile);
			
			//Makes executable
			newConfigFile.setExecutable(true);
			int id=Solvers.addConfiguration(solver, c);
			if (id>0) {
				c.setId(id);
				return c;
			}
	 		
			return null;
		} catch (Exception e){
			log.error("loadConfigurationIntoDatabase says "+e.getMessage(),e);
		}
		return null;
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
		return loadUserIntoDatabase("test","user",TestUtil.getRandomPassword(),TestUtil.getRandomPassword(),"The University of Iowa","test");
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
