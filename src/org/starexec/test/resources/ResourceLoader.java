package org.starexec.test.resources;

import org.apache.commons.io.FileUtils;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.htmlunit.HtmlUnitDriver;
import org.starexec.constants.DB;
import org.starexec.constants.R;
import org.starexec.data.database.*;
import org.starexec.data.to.*;
import org.starexec.data.to.Queue;
import org.starexec.data.to.Solver.ExecutableType;
import org.starexec.data.to.Status.StatusCode;
import org.starexec.data.to.enums.JobXmlType;
import org.starexec.data.to.enums.ProcessorType;
import org.starexec.data.to.pipelines.PipelineDependency;
import org.starexec.data.to.pipelines.PipelineDependency.PipelineInputType;
import org.starexec.data.to.pipelines.PipelineStage;
import org.starexec.data.to.pipelines.SolverPipeline;
import org.starexec.data.to.pipelines.StageAttributes.SaveResultsOption;
import org.starexec.jobs.JobManager;
import org.starexec.logger.StarLogger;
import org.starexec.servlets.UploadBenchmark;
import org.starexec.servlets.ProcessorManager;
import org.starexec.test.TestUtil;
import org.starexec.util.ArchiveUtil;
import org.starexec.util.Util;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * This file contains functions for loading test objects into the database.
 * Test objects are created with random names to avoid getting repeat
 * names when running tests multiple times.
 * Implements autocloseable so it can be used in a try-with-resources statement and automatically
 * delete all its loaded primitives.
 * @author Eric Burns
 *
 */
public class ResourceLoader implements AutoCloseable {
	private final StarLogger log = StarLogger.getLogger(ResourceLoader.class);

	// this class keeps track of all the primitives it creates. Calling deleteAllPrimitives
	// will delete all of these objects
	private final List<Integer> createdUserIds = new ArrayList<>();
	private final List<Integer> createdJobIds = new ArrayList<>();
	private final List<Integer> createdBenchmarkIds = new ArrayList<>();
	private final List<Integer> createdSolverIds = new ArrayList<>();
	private final List<Integer> createdProcessorIds = new ArrayList<>();
	private final List<Integer> createdSettingsIds = new ArrayList<>();
	private final List<Integer> createdSpaceIds = new ArrayList<>();
	private final List<Integer> createdQueueIds = new ArrayList<>();
	private final List<Integer> createdPipelineIds = new ArrayList<>();

	@Override
	public void close() {
		deleteAllPrimitives();
	}

	/**
	 * Deletes all of the primitives that were created using any of the 'load' methods
	 * of this class
	 */
	public void deleteAllPrimitives() {
		for (Integer i : createdJobIds) {
			try {
				Jobs.deleteAndRemove(i);
			} catch (SQLException e) {
				log.warn("Could not delete job with id: " + i);
			}
		}
		for (Integer i : createdSettingsIds) {
			Settings.deleteProfile(i);
		}
		for (Integer i : createdBenchmarkIds) {
			Benchmarks.deleteAndRemoveBenchmark(i);
		}
		for (Integer i :createdSolverIds) {
			Solvers.deleteAndRemoveSolver(i);
		}
		for (Integer i : createdProcessorIds) {
			Processors.delete(i);
		}
		for (Integer i : createdSpaceIds) {
			Spaces.removeSubspace(i);
		}
		for (Integer i : createdUserIds) {
			Users.deleteUser(i);
		}
		for (Integer i : createdQueueIds) {
			Queues.removeQueue(i);
		}
		for (Integer i : createdPipelineIds) {
			Pipelines.deletePipelineFromDatabase(i);
		}
	}

	/**
	 * Returns the path to the resource directory, which is where we are storing files like solvers
	 * and benchmarks that are used during processing
	 * @return The absolute file path
	 */
	public String getResourcePath() {
		return ResourceLoader.class.getResource("/org/starexec/test/resources").getFile();
	}

	/**
	 * Returns a File object representing the file with the given name from the resource directory
	 * @param name The name of the file, which must be present in the resource directory
	 * @return
	 */
	public File getResource(String name) {

		return new File(ResourceLoader.class.getResource("/org/starexec/test/resources/"+name).getFile());
	}

	/**
	 * Returns a File object representing the directory where all downloads performed during testing
	 * should be placed. For example, StarexecCommand tests use this downloads directory.
	 * @return
	 */
	public File getDownloadDirectory() {
		String filePath=getResourcePath();
		File file=new File(filePath,"downloads");
		file.mkdir();
		return file;
	}

	public Processor loadBenchProcessorIntoDatabase(int communityId) {
		return loadProcessorIntoDatabase("benchproc.zip", ProcessorType.BENCH, communityId);
	}

	public Processor loadProcessorIntoDatabase(ProcessorType type, int communityId) {
		return loadProcessorIntoDatabase("postproc.zip", type, communityId);
	}

	/**
	 * Loads a processor into the database
	 * @param fileName The name of the file in the resource directory
	 * @param type Either post, bench, or pre processor
	 * @param communityId The ID of the community to place the processor in
	 * @return The Processor object, with all of its attributes set (name, ID, etc.)
	 */
	public Processor loadProcessorIntoDatabase(String fileName,ProcessorType type, int communityId) {
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
				createdProcessorIds.add(id);
				return p;
			}

		} catch (Exception e) {
			log.error("loadProcessorIntoDatabase", e);
		}
		return null;
	}

	public Job loadJobIntoDatabase(int spaceId, int userId, int solverId, List<Integer> benchmarkIds) {
		List<Integer> solvers= new ArrayList<>();
		solvers.add(solverId);
		return loadJobIntoDatabase(spaceId,userId, -1,-1, solvers,benchmarkIds,10,10,1);
	}

	public Job loadJobIntoDatabase(int spaceId, int userId, int preProcessorId, int postProcessorId, int solverId, List<Integer> benchmarkIds,
			int cpuTimeout, int wallclockTimeout, int memory) {
		List<Integer> solvers= new ArrayList<>();
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
	 * @return The job object
	 */
	public Job loadJobIntoDatabase(int spaceId, int userId, int preProcessorId, int postProcessorId, List<Integer> solverIds, List<Integer> benchmarkIds,
			int cpuTimeout, int wallclockTimeout, int memory) {


		String name=TestUtil.getRandomJobName();

		Queue q=Queues.getAllQ();
		Job job=JobManager.setupJob(
				userId,
				name,
				"test job",
				preProcessorId,
				postProcessorId,
				q.getId(),
				0,
				cpuTimeout,
				wallclockTimeout,
				memory,
				false,
				0,
				SaveResultsOption.SAVE,
				R.DEFAULT_BENCHMARKING_FRAMEWORK);


		List<Integer> configIds= new ArrayList<>();
		for (Integer i : solverIds) {
			configIds.add(Solvers.getConfigsForSolver(i).get(0).getId());
		}

		JobManager.buildJob(job, benchmarkIds, configIds, spaceId);

		Jobs.add(job, spaceId);
		for (JobPair p : job.getJobPairs()) {
			JobPairs.setStatusForPairAndStages(p.getId(), StatusCode.STATUS_COMPLETE.getVal());
			writeFakeJobPairOutput(p);
		}
		createdJobIds.add(job.getId());
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
	public Job loadJobHierarchyIntoDatabase(int rootSpaceId, int userId, int preProcessorId, int postProcessorId) {
		List<Space> spaces = Spaces.getSubSpaceHierarchy(rootSpaceId, userId);
		spaces.add(Spaces.get(rootSpaceId));
		log.debug("loading this number of spaces into the job ="+spaces.size());
		String name=TestUtil.getRandomJobName();
		Queue q=Queues.getAllQ();

		Job job=JobManager.setupJob(
				userId,
				name,
				"test job",
				preProcessorId,
				postProcessorId,
				q.getId(),
				0,
				10,
				10,
				Util.gigabytesToBytes(1),
				false,
				0,
				SaveResultsOption.SAVE,
				R.DEFAULT_BENCHMARKING_FRAMEWORK);
		job.setPrimarySpace(rootSpaceId);
		HashMap<Integer, String> SP = Spaces.spacePathCreate(userId, spaces, rootSpaceId);
		HashMap<Integer,List<JobPair>> spaceToPairs= new HashMap<>();
		for (Space s : spaces) {
			List<JobPair> pairs=JobManager.addJobPairsFromSpace(s.getId(), SP.get(s.getId()));
			spaceToPairs.put(s.getId(), pairs);
		}
		JobManager.addJobPairsDepthFirst(job, spaceToPairs);
		Jobs.add(job, rootSpaceId);
		if (job.getId()<=0) {
			log.error("could not load a job hierarchy into the database");
			return null;
		}
		createdJobIds.add(job.getId());
		return job;
	}

	/**
	 * Creates a randomized DefaultSettings profile and inserts it into the database
	 * @param userId The user that will be the owner of the new profile
	 * @return
	 */
	public DefaultSettings loadDefaultSettingsProfileIntoDatabase(int userId) {
		Random rand=new Random();
		DefaultSettings settings=new DefaultSettings();
		settings.setName(TestUtil.getRandomAlphaString(DB.SETTINGS_NAME_LEN-1));
		settings.setPrimId(userId);
		settings.setCpuTimeout(rand.nextInt(1000)+1);
		settings.setWallclockTimeout(rand.nextInt(1000)+1);
		settings.setMaxMemory(rand.nextInt(1000)+1);
		int id=Users.createNewDefaultSettings(settings);
		if (id>0) {
			createdSettingsIds.add(id);
			return settings;
		}
		return null;
	}
	public DefaultSettings loadDefaultSettingsProfileIntoDatabaseWithDefaultBenchmarks(int userId, List<Integer> benchIds) throws SQLException {
		DefaultSettings settings = loadDefaultSettingsProfileIntoDatabase(userId);
		for(Integer bid : benchIds) {
			Settings.addDefaultBenchmark(settings.getId(), bid);
		}
		settings.setBenchIds(new ArrayList<>(benchIds));
		return settings;
	}

	/**
	 * Loads a configuration for a solver into the database
	 * @param fileName The name of the file in the resource directory
	 * @param solverId The ID of the solver to give the configuration to
	 * @return The Configuration object with all of its fields set (name, ID, etc.)
	 */

	public Configuration loadConfigurationFileIntoDatabase(String fileName, int solverId) {
		try {
			File file=getResource(fileName);
			return loadConfigurationIntoDatabase(FileUtils.readFileToString(file), solverId);

		} catch(Exception e) {
			log.error("loadConfigurationFileIntoDatabase", e);
		}
		return null;
	}

	/**
	 * Loads a configuration for a solver into the database
	 * @param contents The actual String configuration to give to the solver
	 * @param solverId The ID of the solver to give the configuration to
	 * @return The Configuration object with all of its fields set (name, ID, etc.)
	 */

	public Configuration loadConfigurationIntoDatabase(String contents,int solverId) {
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
			log.error("loadConfigurationIntoDatabase", e);
		}
		return null;
	}

	/**
	 * Loads benchmarks.zip into the database
	 * @param parentSpaceId
	 * @param userId
	 * @return A list of benchmark Ids for the newly created benchmarks
	 */
	public List<Integer> loadBenchmarksIntoDatabase(int parentSpaceId, int userId) {
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
	public List<Integer> loadBenchmarksIntoDatabase(String archiveName, int parentSpaceId, int userId) {
		try {
			File archive=getResource(archiveName);

			//make a copy of the archive, because the benchmark extraction function will delete the archive
			File archiveCopy=new File(getDownloadDirectory(),UUID.randomUUID()+archive.getName());
			FileUtils.copyFile(archive, archiveCopy);
			Integer statusId = Uploads.createBenchmarkUploadStatus(parentSpaceId, userId);
			Permission p=new Permission();
			List<Integer> ids=UploadBenchmark.addBenchmarksFromArchive(archiveCopy, userId, parentSpaceId, Processors.getNoTypeProcessor().getId(), false, p,
					"dump", statusId, false, false, null);
			for (Integer i : ids) {
				Benchmarks.updateDetails(i, TestUtil.getRandomAlphaString(DB.BENCH_NAME_LEN-2), TestUtil.getRandomAlphaString(50),
						false, Processors.getNoTypeProcessor().getId());
				Benchmarks.addBenchAttr(i, TestUtil.getRandomAlphaString(10), TestUtil.getRandomAlphaString(10));
			}
			createdBenchmarkIds.addAll(ids);
			return ids;
		} catch (Exception e) {
			log.error("loadBenchmarksIntoDatabase", e);
		}
		return null;
	}

	public Solver loadSolverIntoDatabase(int parentSpaceId, int userId) {
		return loadSolverIntoDatabase("CVC4.zip",parentSpaceId, userId);
	}
	/**
	 * Loads a solver into the database
	 * @param archiveName The name of the archive containing the solver in the Resource directory
	 * @param parentSpaceId The ID of the parent space for the solver
	 * @param userId The ID of the user that will own the solver
	 * @return The Solver object will all of its fields set.
	 */
	public Solver loadSolverIntoDatabase(String archiveName, int parentSpaceId, int userId) {
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
				createdSolverIds.add(id);
				s.setId(id);
				return s;
			} else {
				// there was an error of some kind
				return null;
			}
		} catch (Exception e) {
			log.error("loadSolverIntoDatabase", e);
		}
		return null;
	}

	public Space loadSpaceIntoDatabase(int userId, int parentSpaceId, String name) {
		Space space=new Space();
		space.setName(name);
		space.setDescription("test desc");
		space.setParentSpace(parentSpaceId);
		space.setPublic(false);
		int id=Spaces.add(space, userId);
		if (id>0) {
			createdSpaceIds.add(id);
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
	public Space loadSpaceIntoDatabase(int userId, int parentSpaceId) {
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
	public SolverPipeline loadPipelineIntoDatabase(int userId, List<Configuration> configs) {
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
			createdPipelineIds.add(returnValue);
	 		return pipe;
		}
		return null;

	}

	/**
	 * Loads a user into the database, without any particular name, email, password, and so on. Useful for testing.
	 * @return The user, with their ID and all parameters set, or null on error
	 */
	public User loadUserIntoDatabase() {
		return loadUserIntoDatabase(TestUtil.getRandomPassword(), R.DEFAULT_USER_ROLE_NAME);
	}

	public User loadUserIntoDatabase(String password) {
		return loadUserIntoDatabase(password, R.DEFAULT_USER_ROLE_NAME);
	}

	public User loadAdminIntoDatabase() {
		return loadUserIntoDatabase(TestUtil.getRandomPassword(), R.ADMIN_ROLE_NAME);
	}

	public User loadDevIntoDatabase() {
		return loadUserIntoDatabase(TestUtil.getRandomPassword(), R.DEVELOPER_ROLE_NAME);
	}

	public User loadUserIntoDatabase(String password, String role) {
		return loadUserIntoDatabase(TestUtil.getRandomAlphaString(10),TestUtil.getRandomAlphaString(10),password,password,"The University of Iowa",role);
	}

	public CommunityRequest loadCommunityRequestIntoDatabase(int userId, int commId) {
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
	public User loadUserIntoDatabase(String fname, String lname, String email, String password, String institution, String role) {
		User u=new User();
		u.setFirstName(fname);
		u.setLastName(lname);
		u.setPassword(password);
		u.setEmail(email);
		u.setInstitution(institution);
		u.setRole(role);
		u.setDiskQuota(Long.MAX_VALUE);
		u.setPairQuota(Integer.MAX_VALUE);
		u.setSubscribedToErrorLogs(false);
		int id=Users.add(u);
		if (id>0) {
			createdUserIds.add(id);
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

	public Queue loadQueueIntoDatabase(int wallTimeout, int cpuTimeout) {
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

			boolean success = Queues.updateQueueCpuTimeout(queueId, wallTimeout);
			if (success) {
				Queues.updateQueueWallclockTimeout(queueId, cpuTimeout);
			}
			createdQueueIds.add(queueId);
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
	public WebDriver getWebDriver(String email, String password, boolean visible) {
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

	public WebDriver getFirefoxDriver(String email, String password) {
	  return getWebDriver(email,password,true);
	}

	/**
	 * Retrieves an HTMLUnit WebDriver. This is the same as calling getWe
	 * @param email
	 * @param password
	 * @return
	 */
	public WebDriver getWebDriver(String email, String password) {
	  return getWebDriver(email,password,false);
	}

	/**
	 * Writes 1000 characters of output to the location this pairs output should be placed
	 * @param pair
	 */
	public void writeFakeJobPairOutput(JobPair pair) {
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
	 * Creates a test XML file that uses the basic template.
	 * @param configId id of the config to user.
	 * @param benchId id of the bench to user.
	 * @return the new file.
	 * @throws IOException
	 */
	public File getBasicTestXMLFile(int configId, int benchId) throws IOException {
		Map<String, String> templateReplacements = new HashMap<>();
		templateReplacements.put("$$CONFIG_ONE$$", configId+"");
		templateReplacements.put("$$BENCH_ONE$$", benchId+"");
		return getTestXMLFile(TestXML.BASIC, templateReplacements);
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
	public File getJoblineTestXMLFile(int configId1, int configId2, int benchId1, int benchId2) throws IOException {
		Map<String, String> templateReplacements = new HashMap<>();
		templateReplacements.put("$$CONFIG_ONE$$", configId1+"");
		templateReplacements.put("$$CONFIG_TWO$$", configId2+"");
		templateReplacements.put("$$BENCH_ONE$$", benchId1+"");
		templateReplacements.put("$$BENCH_TWO$$", benchId2+"");
		return getTestXMLFile(TestXML.JOBLINE, templateReplacements);
	}

	public enum TestXML {
		JOBLINE("jobXmls/jobXML.xml", JobXmlType.STANDARD),
		BASIC("jobXmls/basicJobXML.xml", JobXmlType.STANDARD);

		public final String filename;
		public final JobXmlType type;
		TestXML(String filename, JobXmlType type) {
			this.filename = filename;
			this.type = type;
		}
	}

	private File getTestXMLFile(TestXML testXml, Map<String, String> templateReplacements) throws IOException {
		File templateFile = getResource(testXml.filename);
		String xmlString = FileUtils.readFileToString(templateFile);
		final String schemaLocParam = "$$SCHEMA_LOC$$";
		if (!xmlString.contains(schemaLocParam)) {
			throw new IllegalStateException("Test XML files must contain the "+schemaLocParam+" template parameter.");
		}
		templateReplacements.put(schemaLocParam, testXml.type.schemaPath);
		// Replace the key in templateReplacements with the corresponding value in the XML string.
		for (Map.Entry<String, String> entry : templateReplacements.entrySet()) {
			xmlString = xmlString.replace(entry.getKey(), entry.getValue());
		}

		log.debug(xmlString);

		File f = new File(new File(R.STAREXEC_ROOT, R.DOWNLOAD_FILE_DIR), TestUtil.getRandomAlphaString(50)+".xml");
		FileUtils.writeStringToFile(f, xmlString);
		return f;
	}
	/**
	 * Creates a directory with exactly two fake configurations in it.
	 * @return A size-3 array containing the absolute path to the config directory and the two config names, in that order.
	 * @throws IOException
	 */
	public List<String> getTestConfigDirectory() throws IOException {
		List<String> strs = new ArrayList<>();
		File f = new File(new File(R.STAREXEC_ROOT, R.DOWNLOAD_FILE_DIR), TestUtil.getRandomAlphaString(50));
		File bin = new File(f, R.SOLVER_BIN_DIR);
		bin.mkdirs();
		strs.add(f.getAbsolutePath());
		String name = TestUtil.getRandomAlphaString(20);
		strs.add(name);
		File config = new File(bin, R.CONFIGURATION_PREFIX+name);
		config.createNewFile();
		name = TestUtil.getRandomAlphaString(20);
		strs.add(name);
		config = new File(bin, R.CONFIGURATION_PREFIX+name);
		config.createNewFile();
		return strs;

	}
}
