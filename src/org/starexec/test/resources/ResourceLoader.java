package org.starexec.test.resources;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.htmlunit.HtmlUnitDriver;
import org.starexec.constants.R;
import org.starexec.data.database.Benchmarks;
import org.starexec.data.database.Cluster;
import org.starexec.data.database.JobPairs;
import org.starexec.data.database.Jobs;
import org.starexec.data.database.Pipelines;
import org.starexec.data.database.Processors;
import org.starexec.data.database.Queues;
import org.starexec.data.database.Requests;
import org.starexec.data.database.Solvers;
import org.starexec.data.database.Spaces;
import org.starexec.data.database.Uploads;
import org.starexec.data.database.Users;
import org.starexec.data.to.CommunityRequest;
import org.starexec.data.to.Configuration;
import org.starexec.data.to.DefaultSettings;
import org.starexec.data.to.Job;
import org.starexec.data.to.JobPair;
import org.starexec.data.to.Permission;
import org.starexec.data.to.Processor;
import org.starexec.data.to.Processor.ProcessorType;
import org.starexec.data.to.Queue;
import org.starexec.data.to.Solver;
import org.starexec.data.to.Solver.ExecutableType;
import org.starexec.data.to.SolverBuildStatus;
import org.starexec.data.to.Space;
import org.starexec.data.to.Status.StatusCode;
import org.starexec.data.to.User;
import org.starexec.jobs.JobManager;
import org.starexec.servlets.BenchmarkUploader;
import org.starexec.servlets.ProcessorManager;
import org.starexec.test.TestUtil;
import org.starexec.util.ArchiveUtil;
import org.starexec.util.Util;
import org.starexec.data.to.pipelines.PipelineDependency;
import org.starexec.data.to.pipelines.PipelineDependency.PipelineInputType;
import org.starexec.data.to.pipelines.PipelineStage;
import org.starexec.data.to.pipelines.SolverPipeline;

/**
 * This file contains functions for loading test objects into the database.
 * Test objects are created with random names to avoid getting repeat
 * names when running tests multiple times.
 * @author Eric Burns
 *
 */

public class ResourceLoader {
	private static final Logger log = Logger.getLogger(ResourceLoader.class);	
	
	
	/**
	 * Returns the path to the resource directory, which is where we are storing files like solvers
	 * and benchmarks that are used during processing
	 * @return The absolute file path
	 */
	public static String getResourcePath() {
		return ResourceLoader.class.getResource("/org/starexec/test/resources").getFile();
	}
	
	/**
	 * Returns a File object representing the file with the given name from the resource directory
	 * @param name The name of the file, which must be present in the resource directory
	 * @return 
	 */
	public static File getResource(String name) {
		
		return new File(ResourceLoader.class.getResource("/org/starexec/test/resources/"+name).getFile());
	}
	
	/**
	 * Returns a File object representing the directory where all downloads performed during testing
	 * should be placed. For example, StarexecCommand tests use this downloads directory.
	 * @return
	 */
	public static File getDownloadDirectory() {
		String filePath=getResourcePath();
		File file=new File(filePath,"downloads");
		file.mkdir();
		return file;
	}
	
	public static Processor loadBenchProcessorIntoDatabase(int communityId) {
		return loadProcessorIntoDatabase("benchproc.zip", ProcessorType.BENCH, communityId);
	}
	
	public static Processor loadProcessorIntoDatabase(ProcessorType type, int communityId) {
		return loadProcessorIntoDatabase("postproc.zip", type, communityId);
	}
	
	/**
	 * Loads a processor into the database
	 * @param fileName The name of the file in the resource directory
	 * @param type Either post, bench, or pre processor
	 * @param communityId The ID of the community to place the processor in
	 * @return The Processor object, with all of its attributes set (name, ID, etc.)
	 */
	public static Processor loadProcessorIntoDatabase(String fileName,ProcessorType type, int communityId) {
		try {
			Processor p=new Processor();
			p.setName(TestUtil.getRandomSolverName());
			p.setCommunityId(communityId);
			p.setType(type);
			
			File processorDir=ProcessorManager.getProcessorDirectory(communityId, p.getName());
			File processorFile=getResource(fileName);
			FileUtils.copyFileToDirectory(processorFile, processorDir);
			ArchiveUtil.extractArchive(new File(processorDir,processorFile.getName()).getAbsolutePath());
			File processorScript=new File(processorDir,R.PROCESSOR_RUN_SCRIPT);

			if (!processorScript.setExecutable(true, false)) {			
				log.warn("Could not set processor as executable: " + processorScript.getAbsolutePath());
			}
			p.setFilePath(processorDir.getAbsolutePath());			

			int id=Processors.add(p);
			if (id>0) {
				p.setId(id);
				return p;
			}
			
		} catch (Exception e) {
			log.error("loadProcessorIntoDatabase says "+e.getMessage(),e);
		}
		return null;
		
	}
	
	public static Job loadJobIntoDatabase(int spaceId, int userId, int solverId, List<Integer> benchmarkIds) {
		List<Integer> solvers=new ArrayList<Integer>();
		solvers.add(solverId);
		return loadJobIntoDatabase(spaceId,userId, -1,-1, solvers,benchmarkIds,10,10,1);
	}
	
	public static Job loadJobIntoDatabase(int spaceId, int userId, int preProcessorId, int postProcessorId, int solverId, List<Integer> benchmarkIds,
			int cpuTimeout, int wallclockTimeout, int memory) {
		List<Integer> solvers=new ArrayList<Integer>();
		solvers.add(solverId);
		return loadJobIntoDatabase(spaceId,userId,preProcessorId,postProcessorId,solvers,benchmarkIds,cpuTimeout,wallclockTimeout,memory);
	}
	
	
	/**
	 * This will load a job with the given solvers and benchmarks into the database.
	 * It is somewhat primitive right now-- for every solver
	 * a single configuration will be picked randomly to be added to the job, and the job will be added to a random
	 * queue that the given user owns. The job pairs will all be set to status 'complete' and given some fake output,
	 * so the job will be complete immediately upon return.
	 * @param spaceId The ID of the space to put the job in
	 * @param userId The ID of the user who will own the job
	 * @param preProcessorId The ID of the preprocessor to use for this job
	 * @param postProcessorId The ID of the postprocessor to use for this job
	 * @param solverIds The solvers to use for the job, which need to have at least 1 configuration each. The first
	 * 					configuration for every solver will be matched with every benchmark given
	 * @param benchmarkIds The benchmarks to use in job pairs
	 * @param cpuTimeout The cpu timeout for every pair in this job
	 * @param wallclockTimeout The wallclock timeout for every pair in this job
	 * @param memory The max memory limit, in bytes, for every pair in this job
	 * @param solvers The solverIds that will be matched to every benchmark
	 * @param benchmarks The benchmarkIDs to run
	 * @return The job object
	 */
	public static Job loadJobIntoDatabase(int spaceId, int userId, int preProcessorId, int postProcessorId, List<Integer> solverIds, List<Integer> benchmarkIds,
			int cpuTimeout, int wallclockTimeout, int memory) {
		
		
		String name=TestUtil.getRandomJobName();
		
		Queue q=Queues.getAllQ();
		Job job=JobManager.setupJob(userId, name, "test job", preProcessorId, postProcessorId, q.getId(),0,cpuTimeout,wallclockTimeout,memory, false, 0);
		
		
		List<Integer> configIds=new ArrayList<Integer>();
		for (Integer i : solverIds) {
			configIds.add(Solvers.getConfigsForSolver(i).get(0).getId());
		}
		List<Space> spaces=new ArrayList<Space>();
		spaces.add(Spaces.get(spaceId));

		JobManager.buildJob(job, benchmarkIds, configIds, spaceId);
		
		Jobs.add(job, spaceId);
		for (JobPair p : job.getJobPairs()) {
			JobPairs.setStatusForPairAndStages(p.getId(), StatusCode.STATUS_COMPLETE.getVal());
			ResourceLoader.writeFakeJobPairOutput(p);
		}
		return job;
	}
	/**
	 * 
	 * @param rootSpaceId
	 * @param userId
	 * @param preProcessorId
	 * @param postProcessorId
	 * @return
	 */
	public static Job loadJobHierarchyIntoDatabase(int rootSpaceId, int userId, int preProcessorId, int postProcessorId) {
		List<Space> spaces = Spaces.getSubSpaceHierarchy(rootSpaceId, userId); 
		spaces.add(Spaces.get(rootSpaceId));
		log.debug("loading this number of spaces into the job ="+spaces.size());
		String name=TestUtil.getRandomJobName();
		Queue q=Queues.getAllQ();

		Job job=JobManager.setupJob(userId, name, "test job", preProcessorId, postProcessorId, q.getId(),0,10,10,Util.gigabytesToBytes(1), false, 0);
		job.setPrimarySpace(rootSpaceId);
		HashMap<Integer, String> SP =  Spaces.spacePathCreate(userId, spaces, rootSpaceId);
		HashMap<Integer,List<JobPair>> spaceToPairs=new HashMap<Integer,List<JobPair>>();
		for (Space s : spaces) {
			List<JobPair> pairs=JobManager.addJobPairsFromSpace(userId, s.getId(), SP.get(s.getId()));
			spaceToPairs.put(s.getId(), pairs);
		}
		JobManager.addJobPairsDepthFirst(job, spaceToPairs);
		Jobs.add(job, rootSpaceId);
		if (job.getId()<=0) {
			log.error("could not load a job hierarchy into the database");
			return null;
		}
		return job;
	}
	
	/**
	 * Creates a randomized DefaultSettings profile and inserts it into the database
	 * @param userId The user that will be the owner of the new profile
	 * @return
	 */
	public static DefaultSettings loadDefaultSettingsProfileIntoDatabase(int userId) {
		Random rand=new Random();
		DefaultSettings settings=new DefaultSettings();
		settings.setName(TestUtil.getRandomAlphaString(R.SETTINGS_NAME_LEN-1));
		settings.setPrimId(userId);
		settings.setCpuTimeout(rand.nextInt(1000)+1);
		settings.setWallclockTimeout(rand.nextInt(1000)+1);
		settings.setMaxMemory(rand.nextInt(1000)+1);
		int id=Users.createNewDefaultSettings(settings);
		if (id>0) {
			settings.setBenchId(0);
			settings.setSolverId(0);
			settings.setPreProcessorId(0);
			settings.setPostProcessorId(0);
			return settings;
		}
		return null;
	}
	
	/**
	 * Loads a configuration for a solver into the database
	 * @param fileName The name of the file in the resource directory
	 * @param solverId The ID of the solver to give the configuration to
	 * @return The Configuration object with all of its fields set (name, ID, etc.)
	 */
	
	public static Configuration loadConfigurationFileIntoDatabase(String fileName, int solverId)  {
		try {
			File file=getResource(fileName);
			return loadConfigurationIntoDatabase(FileUtils.readFileToString(file), solverId);

		} catch(Exception e) {
			log.error("loadConfigurationIntoDatabase says "+e.getMessage(),e);
		}
		return null;
	}
	
	/**
	 * Loads a configuration for a solver into the database
	 * @param contents The actual String configuration to give to the solver
	 * @param solverId The ID of the solver to give the configuration to
	 * @return The Configuration object with all of its fields set (name, ID, etc.)
	 */
	
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
	
	/**
	 * Loads benchmarks.zip into the database
	 * @param parentSpaceId
	 * @param userId
	 * @return A list of benchmark Ids for the newly created benchmarks
	 */
	public static List<Integer> loadBenchmarksIntoDatabase(int parentSpaceId, int userId) {
		return loadBenchmarksIntoDatabase("benchmarks.zip", parentSpaceId, userId);
	}
	/**
	 * Loads an archive of benchmarks into the database. All benchmarks will be given a single random,
	 * unique attribute after they have been added. They will also be given randomized names that
	 * tests can assume are unique.
	 * @param archiveName The name of the archive containing the benchmarks in the Resource directory
	 * @param parentSpaceId The ID of the space to place the benchmarks in. Benchmarks will
	 * not be made into a hierarchy-- they will all be placed into the given space
	 * @param userId The ID of the owner of the benchmarks
	 * @return A list of benchmark Ids for the newly created benchmarks
	 */
	public static List<Integer> loadBenchmarksIntoDatabase(String archiveName, int parentSpaceId, int userId) {
		try {
			File archive=getResource(archiveName);
			
			//make a copy of the archive, because the benchmark extraction function will delete the archive
			File archiveCopy=new File(getDownloadDirectory(),UUID.randomUUID()+archive.getName());
			FileUtils.copyFile(archive, archiveCopy);
			Integer statusId = Uploads.createBenchmarkUploadStatus(parentSpaceId, userId);
			Permission p=new Permission();
			List<Integer> ids=BenchmarkUploader.addBenchmarksFromArchive(archiveCopy, userId, parentSpaceId, Processors.getNoTypeProcessor().getId(), false, p, 
					"dump", statusId, false, false, null);
			for (Integer i : ids) {
				Benchmarks.updateDetails(i, TestUtil.getRandomAlphaString(R.BENCH_NAME_LEN-2), TestUtil.getRandomAlphaString(50),
						false, Processors.getNoTypeProcessor().getId());
				Benchmarks.addBenchAttr(i, TestUtil.getRandomAlphaString(10), TestUtil.getRandomAlphaString(10));
			}
			return ids;
		} catch (Exception e) {
			log.error("loadBenchmarksIntoDatabase says "+e.getMessage(),e);
		}
		return null;

	}
	
	public static Solver loadSolverIntoDatabase(int parentSpaceId, int userId) {
		return loadSolverIntoDatabase("CVC4.zip",parentSpaceId, userId);
	}
	/**
	 * Loads a solver into the database
	 * @param archiveName The name of the archive containing the solver in the Resource directory
	 * @param parentSpaceId The ID of the parent space for the solver
	 * @param userId The ID of the user that will own the solver
	 * @return The Solver object will all of its fields set.
	 */
	public static Solver loadSolverIntoDatabase(String archiveName, int parentSpaceId, int userId) {
		try {
			Solver s=new Solver();
			s.setName(TestUtil.getRandomSolverName());
			s.setDescription("solver coming from test");
			s.setUserId(userId);
			SolverBuildStatus status = new SolverBuildStatus();
			status.setCode(SolverBuildStatus.SolverBuildStatusCode.BUILT.getVal());
			s.setBuildStatus(status);
			File archive=getResource(archiveName);
			File archiveCopy=new File(archive.getParent(),TestUtil.getRandomAlphaString(20)+".zip");
			FileUtils.copyFile(archive, archiveCopy);
			String filePath=Solvers.getDefaultSolverPath(userId, s.getName());
			s.setPath(filePath);
			s.setType(ExecutableType.SOLVER);
			File solverDir=new File(filePath);
			solverDir.mkdirs();
			ArchiveUtil.extractArchive(archiveCopy.getAbsolutePath(), solverDir.getAbsolutePath());
			
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
	
	public static Space loadSpaceIntoDatabase(int userId, int parentSpaceId, String name) {
		Space space=new Space();
		space.setName(name);
		space.setDescription("test desc");
		space.setParentSpace(parentSpaceId);
		space.setPublic(false);
		int id=Spaces.add(space, userId);
		if (id>0) {
			space.setId(id);
			return space;
		}
		return null;
		
	}
	
	/**
	 * Loads a new space into the database
	 * @param userId The ID of the user who is creating the space
	 * @param parentSpaceId The ID of the parent space for the new subspace
	 * @return The Space object with all of its fields set.
	 */
	public static Space loadSpaceIntoDatabase(int userId, int parentSpaceId) {
		return loadSpaceIntoDatabase(userId, parentSpaceId,TestUtil.getRandomSpaceName() );
		
	}
	
	/**
	 * Creates a new SolverPipeline for the given user, where a stage is created for each given
	 * configuration. The stages will always depend on previous stages and also on another fake input,
	 * and it will have a random name
	 * @param userId The ID of the user who will own the new pipeline
	 * @param configs The ordered list of configurations to make into a pipeline
	 * @return The SolverPipeline object
	 */
	public static SolverPipeline loadPipelineIntoDatabase(int userId, List<Configuration> configs) {
		SolverPipeline pipe=new SolverPipeline();
		pipe.setName(TestUtil.getRandomAlphaString(10));
		pipe.setUserId(userId);
		pipe.setPrimaryStageNumber(1);
		for (Configuration c : configs) {
			PipelineStage stage=new PipelineStage();
			stage.setConfigId(c.getId());
			PipelineDependency dep = new PipelineDependency();
			dep.setType(PipelineInputType.ARTIFACT);
			dep.setInputNumber(1);
			dep.setDependencyId(1);
			
			stage.addDependency(dep);
			dep = new PipelineDependency();
			dep.setType(PipelineInputType.BENCHMARK);
			dep.setInputNumber(1);
			dep.setDependencyId(2);
			pipe.addStage(stage);
		}
		int returnValue= Pipelines.addPipelineToDatabase(pipe);
		if (returnValue>0) {
	 		return pipe;
		}
		return null;
		
	}
	
	/**
	 * Loads a user into the database, without any particular name, email, password, and so on. Useful for testing.
	 * @return The user, with their ID and all parameters set, or null on error
	 */
	public static User loadUserIntoDatabase() {
		return loadUserIntoDatabase(TestUtil.getRandomPassword());
	}
	
	public static User loadUserIntoDatabase(String password) {
		return loadUserIntoDatabase(TestUtil.getRandomAlphaString(10),TestUtil.getRandomAlphaString(10),password,password,"The University of Iowa","test");
	}
	
	public static CommunityRequest loadCommunityRequestIntoDatabase(int userId, int commId) {
		CommunityRequest req=new CommunityRequest();
		req.setCode(UUID.randomUUID().toString());
		req.setCommunityId(commId);
		req.setUserId(userId);
		req.setMessage(TestUtil.getRandomAlphaString(30));
		
		boolean success=Requests.addCommunityRequest(Users.get(userId), commId, req.getCode(), req.getMessage());
		if (!success) {
			return null;
		}
		return req;
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
	
	/**
	 * Loads a queue with the given timeouts into the database.
	 * @param wallTimeout
	 * @param cpuTimeout
	 * @return
	 */
	
	public static Queue loadQueueIntoDatabase(int wallTimeout, int cpuTimeout) {
		try {
			String queueName=TestUtil.getRandomQueueName();
			R.BACKEND.createQueue(queueName, null,null);
			
			//reloads worker nodes and queues
			Cluster.loadWorkerNodes();
			Cluster.loadQueueDetails();
			int queueId=Queues.getIdByName(queueName);
			if (queueId<=0) {
				log.error("loadQueueIntoDatabase failed to create a queue!");
				return null;
			}
			
			boolean success =  Queues.updateQueueCpuTimeout(queueId, wallTimeout);
			success = success && Queues.updateQueueWallclockTimeout(queueId, cpuTimeout);
			
			return Queues.get(queueId);
		} catch (Exception e) {
			log.error(e.getMessage(),e);
		}
		return null;
		
	}
	/**
	 * Returns a WebDriver for selenium testing. The driver we be logged into the website 
	 * upon return
	 * @param email The email address of the user to log in
	 * @param password The password of the user to log in
	 * @return
	 */
	public static WebDriver getWebDriver(String email, String password, boolean visible) {
		WebDriver driver=null;
		if (visible) {
		    driver = new FirefoxDriver();

		} else {
		    driver = new HtmlUnitDriver(false);
		    HtmlUnitDriver test=(HtmlUnitDriver) driver;
		   
		}
	       
	        driver.get(Util.url("secure/index.jsp"));
	        WebElement userName=driver.findElement(By.name("j_username"));
	        userName.sendKeys(email);
	        driver.findElement(By.name("j_password")).sendKeys(password);
	        driver.manage().timeouts().implicitlyWait(2, TimeUnit.SECONDS);
	        userName.submit();
	        
	       return driver;
	}
	
	public static WebDriver getFirefoxDriver(String email, String password) {
	   return getWebDriver(email,password,true);
	}
	
	/**
	 * Retrieves an HTMLUnit WebDriver. This is the same as calling getWe
	 * @param email
	 * @param password
	 * @return
	 */
	public static WebDriver getWebDriver(String email, String password) {
	   return getWebDriver(email,password,false);
	}
	
	/**
	 * Writes 1000 characters of output to the location this pairs output should be placed
	 * @param pair
	 */
	public static void writeFakeJobPairOutput(JobPair pair) {
		try {
			File f=new File(JobPairs.getPairStdout(pair));
			f=f.getParentFile();
			f.mkdirs();
			String randomOutput=TestUtil.getRandomAlphaString(1000);
			FileUtils.writeStringToFile(new File(f,pair.getId()+".txt"), randomOutput);
			
		} catch (Exception e) {
			log.error(e.getMessage(),e);
		}
	}
	
	/**
	 * Creates a test XML file that uses a solver pipeline and returns the file
	 * @param configId1 First config to use
	 * @param configId2 Second config to use
	 * @param benchId1 First benchmark to use
	 * @param benchId2 Second benchmark to use
	 * @return File containing the XML
	 * @throws IOException
	 */
	public static File getTestXMLFile(int configId1, int configId2, int benchId1, int benchId2) throws IOException {
		File templateFile = getResource("jobXML.xml");
		String XMLString = FileUtils.readFileToString(templateFile);
		String schemaLoc = Util.url("public/batchJobSchema.xsd");
		log.debug("the schema loc is " +schemaLoc);
		XMLString = XMLString.replace("$$SCHEMA_LOC$$", schemaLoc);
		XMLString = XMLString.replace("$$CONFIG_ONE$$", configId1+"");
		XMLString = XMLString.replace("$$CONFIG_TWO$$", configId2+"");
		
		XMLString = XMLString.replace("$$BENCH_ONE$$", benchId1+"");
		XMLString = XMLString.replace("$$BENCH_TWO$$", benchId2+"");
		log.debug(XMLString);
		File f = new File(new File(R.STAREXEC_ROOT, R.DOWNLOAD_FILE_DIR), TestUtil.getRandomAlphaString(50)+".xml");
		FileUtils.writeStringToFile(f, XMLString);
		return f;
	}
}
