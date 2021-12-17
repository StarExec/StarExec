package org.starexec.util;

import org.starexec.constants.R;
import org.starexec.data.database.*;
import org.starexec.data.security.ValidatorStatusCode;
import org.starexec.data.to.*;
import org.starexec.data.to.Queue;
import org.starexec.data.to.enums.BenchmarkingFramework;
import org.starexec.data.to.enums.ConfigXmlAttribute;
import org.starexec.data.to.enums.JobXmlType;
import org.starexec.data.to.pipelines.*;
import org.starexec.data.to.pipelines.PipelineDependency.PipelineInputType;
import org.starexec.data.to.pipelines.StageAttributes.SaveResultsOption;
import org.starexec.data.to.tuples.ConfigAttrMapPair;
import org.starexec.logger.StarLogger;
import org.starexec.servlets.CreateJob;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.IOException;
import java.util.*;

public class JobUtil {
	private static final StarLogger log = StarLogger.getLogger(JobUtil.class);

	private Boolean jobCreationSuccess = false;
	private String errorMessage = "";//this will be used to given information to user about failures in validation
	private String secondaryErrorMessage = ""; // this will be used for additional error message useful to developers.


	/**
	 * Creates jobs from the xml file. This also creates any solver pipelines defined in the XML document
	 *
	 * @param file the xml file we wish to create jobs from
	 * @param userId the userId of the user making the request
	 * @param spaceId the space that will serve as the root for jobs to run under
	 * @return List<Integer> Null on failure, and a list of jobIds on success. Some jobs may have failed,
	 * resulting in values of -1 in the list.
	 * @throws SAXException
	 * @throws ParserConfigurationException
	 * @throws IOException
	 * @author Tim Smith
	 */
	public List<Integer> createJobsFromFile(File file, int userId, Integer spaceId, JobXmlType xmlType,
	                                        ConfigAttrMapPair configAttrMapPair) throws IOException,
			ParserConfigurationException, SAXException {

		final String method = "createJobsFromFile";
		List<Integer> jobIds = new ArrayList<>();
		if (!validateAgainstSchema(file, xmlType)) {
			log.debug(method, "File '" + file.getName() + "' from User " + userId + " is not Schema valid.");
			return null;
		}

		Permission p = Permissions.get(userId, spaceId);
		if (!p.canAddJob()) {
			log.info(method,
					"User with id=" + userId + " does not have permission to create a job on space with id=" +
					spaceId);
			errorMessage = "You do not have permission to create a job on this space";
			return null;
		}

		DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory.newInstance();
		DocumentBuilder docBuilder = docBuilderFactory.newDocumentBuilder();
		Document doc = docBuilder.parse(file);
		Element jobsElement = doc.getDocumentElement();
		NodeList listOfJobElements = jobsElement.getElementsByTagName("Job");

		NodeList listOfPipelines = doc.getElementsByTagName("SolverPipeline");
		log.info(method, "# of pipelines = " + listOfPipelines.getLength());

		//Check Jobs and Job Pairs
		NodeList listOfJobs = doc.getElementsByTagName("Job");
		log.info(method, "# of Jobs = " + listOfJobs.getLength());
		NodeList listOfJobPairs = doc.getElementsByTagName("JobPair");
		NodeList listOfUploadedSolverJobPairs = doc.getElementsByTagName("UploadedSolverJobPair");


		log.info(method, "# of JobPairs = " + listOfJobPairs.getLength());
		log.info(method, "# of UploadedSolverJobPairs = " + listOfUploadedSolverJobPairs.getLength());

		NodeList listOfJobLines = doc.getElementsByTagName("JobLine");
		log.info(method, " # of JobLines = " + listOfJobLines.getLength());
		//this job has nothing to run
		int pairCount =
				listOfJobPairs.getLength() + listOfJobLines.getLength() + listOfUploadedSolverJobPairs.getLength();
		if (pairCount == 0) {
			errorMessage = "Every job must have at least one job pair or job line to be created";
			return null;
		}
		User u = Users.get(userId);
		int pairsAvailable = Math.max(0, u.getPairQuota() - Jobs.countPairsByUser(userId));
		// This just checks if a quota is totally full, which is sufficient for quick jobs and as a fast sanity check
		// for full jobs. After the number of pairs have been acquired for a full job this check will be done
		// factoring them in.
		if (pairsAvailable < pairCount) {
			errorMessage = "Error: You are trying to create " + pairCount + " pairs, but you have " + pairsAvailable +
			               " remaining in your quota. Please delete some old jobs before continuing.";
			return null;
		}

		if (Users.isDiskQuotaExceeded(userId)) {
			errorMessage =
					"Your disk quota has been exceeded: please clear out some old solvers, jobs, or benchmarks " +
					"before proceeding";
			return null;
		}

		//validate all solver pipelines

		//data structure to ensure all pipeline names in this upload are unique
		HashMap<String, SolverPipeline> pipelineNames = new HashMap<>();
		log.info(method, "Creating pipelines from elements.");
		for (int i = 0; i < listOfPipelines.getLength(); i++) {
			Node pipeline = listOfPipelines.item(i);
			SolverPipeline pipe = createPipelineFromElement(userId, (Element) pipeline);
			if (pipe == null) {
				log.info("error creating pipeline");
				secondaryErrorMessage = "Solver pipeline was null.";
				return null; // this means there was some error. The error message should have been set already
				// the call to createPipelineFromElement
			}
			if (pipelineNames.containsKey(pipe.getName())) {
				errorMessage = " Duplicate pipeline name = " + pipe.getName() +
				               ". All pipelines in this upload must have unique names";
				return null;
			}
			pipelineNames.put(pipe.getName(), pipe);
		}
		log.info(method, "Finished creating pipelines from elements.");

		// Make sure jobs are named
		log.info(method, "Checking to make sure jobs are named.");
		for (int i = 0; i < listOfJobs.getLength(); i++) {
			Node jobNode = listOfJobs.item(i);
			if (jobNode.getNodeType() == Node.ELEMENT_NODE) {
				Element jobElement = (Element) jobNode;
				String name = jobElement.getAttribute("name");
				if (name == null) {
					log.info(method, "Name not found");
					errorMessage = "Job elements must include a 'name' attribute.";
					return null;
				}
				log.debug(method, "Job Name = " + name);

				if (!org.starexec.util.Validator.isValidJobName(name)) {
					errorMessage = name + "is not a valid job name";
					return null;
				}

			} else {
				log.warn("Job Node should be an element, but isn't");
			}
		}

		log.info(method, "Finished checking to make sure jobs are named.");


		log.info(method, "Creating jobs from elements.");
		for (int i = 0; i < listOfJobElements.getLength(); i++) {
			Node jobNode = listOfJobElements.item(i);
			if (jobNode.getNodeType() == Node.ELEMENT_NODE) {
				Element jobElement = (Element) jobNode;
				log.info("about to create job from element");

				Integer id = createJobFromElement(userId, spaceId, jobElement, pipelineNames, configAttrMapPair);

				if (id < 0) {
					secondaryErrorMessage = "createJobFromElement returned: " + id;
					return null; // means there was an error. Error message should have been set
				}
				jobIds.add(id);
			}
		}
		log.info(method, "Finished creating jobs from elements, returning job ids.");
		this.jobCreationSuccess = true;

		return jobIds;
	}


	/**
	 * Creates a single solver pipeline from a SolverPipeline XML element. If there are any errors,
	 * returns null
	 *
	 * @param userId The ID of the user who is doing this upload
	 * @param pipeElement The XML element corresponding to the <SolverPipeline> tag.
	 * @return The SolverPipeline object, where the pipeline will already have been added to the database.
	 * On error, null is returned, and the errorMessage string will be set
	 */
	private SolverPipeline createPipelineFromElement(int userId, Element pipeElement) {
		boolean foundPrimary = false;
		SolverPipeline pipeline = new SolverPipeline();
		pipeline.setUserId(userId);
		String name = pipeElement.getAttribute("name");

		if (!org.starexec.util.Validator.isValidPipelineName(name)) {
			errorMessage = name + " is not a valid pipeline name";
			return null;
		}
		pipeline.setName(name);

		NodeList stages = pipeElement.getChildNodes();

		//data structure for storing all of the unique benchmark inputs in this upload.
		//We need to validate the following rule-- if there are n unique benchmark inputs,
		//then the numbers on those inputs must go exactly from 1 to n.

		HashSet<Integer> benchmarkInputs = new HashSet<>();

		//XML files have a stage tag for each stage
		int currentStage = 0;
		List<PipelineStage> stageList = new ArrayList<>();
		for (int i = 0; i < stages.getLength(); i++) {

			if (stages.item(i).getNodeName().equals("PipelineStage")) {
				currentStage += 1;
				Element stage = (Element) stages.item(i);
				PipelineStage s = new PipelineStage();
				s.setNoOp(false);


				if (stage.hasAttribute("primary")) {
					boolean currentPrimary = Boolean.parseBoolean(stage.getAttribute("primary"));
					if (currentPrimary) {
						if (foundPrimary) {
							errorMessage = "More than one primary stage for pipeline " + pipeline.getName();
							return null;
						}
						foundPrimary = true;
					}
					s.setPrimary(true);
					pipeline.setPrimaryStageNumber(currentStage);
				} else {
					s.setPrimary(false);
				}

				s.setConfigId(Integer.parseInt(stage.getAttribute("config-id")));
				// make sure the user is authorized to use the solver they are trying to use
				Solver solver = Solvers.getSolverByConfig(s.getConfigId(), false);
				if (solver == null) {
					errorMessage = "The given configuration could not be found";
					return null;
				}
				if (!Permissions.canUserSeeSolver(solver.getId(), userId)) {
					errorMessage = "You do not have permission to see the solver" + solver.getId();
					return null;
				}


				NodeList dependencies = stage.getChildNodes();
				int inputNumber = 0;
				for (int x = 0; x < dependencies.getLength(); x++) {

					Node t = dependencies.item(x);
					if (t.getNodeType() == Node.ELEMENT_NODE) {
						Element dependency = (Element) t;
						PipelineDependency dep = new PipelineDependency();
						if (dependency.getTagName().equals("StageDependency")) {
							inputNumber++;

							dep.setType(PipelineInputType.ARTIFACT);
							int neededStageId = Integer.parseInt(dependency.getAttribute("stage"));

							if (neededStageId < 1) {
								errorMessage = "Invalid stage dependency-- all stages are numbered 1 or greater";
								return null;
							} else if (neededStageId >= (currentStage - 1)) {
								errorMessage =
										"Invalid stage dependency-- stages can only depend on earlier stages, and a" +
										" stages implicitly depend on previous stages. Bad dependency =  Stage " +
										currentStage + " depends on" + " stage " + neededStageId;
								return null;
							}
							dep.setDependencyId(neededStageId);
						} else if (dependency.getTagName().equals("BenchmarkDependency")) {
							inputNumber++;

							dep.setType(PipelineInputType.BENCHMARK);

							dep.setDependencyId(Integer.parseInt(dependency.getAttribute("input")));
							benchmarkInputs.add(dep.getDependencyId());
						}
						dep.setInputNumber(inputNumber);
						s.addDependency(dep);

					}
				}
				stageList.add(s);
			} else if (stages.item(i).getNodeName().equals("noop")) {
				currentStage += 1;
				PipelineStage stage = new PipelineStage();
				stage.setNoOp(true);
				stage.setConfigId(null);
				stage.setPrimary(false);
				stageList.add(stage);

			}

		}
		//ensure that benchmark inputs are ordered correctly. Benchmark inputs must be ordered from
		//1 to n, where n is the total number of inputs.
		if (!benchmarkInputs.isEmpty()) {
			int maxSeen = Collections.max(benchmarkInputs);
			if (maxSeen != benchmarkInputs.size()) {
				errorMessage = "Invalid benchmark inputs for pipeline = " + pipeline.getName() +
				               ". Benchmark inputs must be numbered from 1 to n, where n is the total number of " +
				               "expected inputs";
				return null;
			}
		}

		if (stageList.size() > R.MAX_STAGES_PER_PIPELINE) {
			errorMessage = "Too many stages in pipeline " + pipeline.getName() + ". The maximum is " +
			               R.MAX_STAGES_PER_PIPELINE;
			return null;
		}
		if (!foundPrimary) {
			errorMessage = "No primary stage specified for pipeline " + pipeline.getName();
			return null;
		}
		pipeline.setStages(stageList);
		int id = Pipelines.addPipelineToDatabase(pipeline);
		if (id <= 0) { //if there was a database error
			errorMessage = " Internal database error adding a pipeline";
			return null;
		}
		return pipeline;
	}

	/**
	 * Converts an XML element to a StageAttributes object
	 *
	 * @param stageAttributes The element to convert
	 * @param maxStages The max stages present in any pipeline in the job that owns this element. Used
	 * to verify that stage-nums are chosen properly
	 * @param userId ID of user that will own this job. Used to verify they have permission to create benchmarks
	 * in their chosen space
	 * @param cpuTimeout Default when not specified
	 * @param wallclock Default when not specified
	 * @param memoryLimit Default when not specified
	 * @param queueId ID of the queue for this job. Used to validate chosen timeouts.
	 * @return
	 */
	private StageAttributes elementToStageAttributes(Element stageAttributes, int maxStages, int userId, int
			cpuTimeout, int wallclock, long memoryLimit, int queueId) {
		StageAttributes attrs = new StageAttributes();

		//first,  we need to find which stage this is for, given the name of a pipeline and the stage number (not ID)

		int neededStageNum =
				Integer.parseInt(DOMHelper.getElementByName(stageAttributes, "stage-num").getAttribute("value"));
		// the stage number needs to be between 1 and n if there are a maximum of n stages in any pipeline in this job
		if (neededStageNum <= 0 || neededStageNum > maxStages) {
			errorMessage = "StageAttributes tag has invalid stage-num = " + neededStageNum;
			return null;
		}
		attrs.setStageNumber(neededStageNum);


		// all timeouts are optional-- they default to job timeouts if not given
		int stageCpu = cpuTimeout;
		if (DOMHelper.hasElement(stageAttributes, "cpu-timeout")) {
			stageCpu =
					Integer.parseInt(DOMHelper.getElementByName(stageAttributes, "cpu-timeout").getAttribute("value"));
		}
		int stageWallclock = wallclock;
		if (DOMHelper.hasElement(stageAttributes, "wallclock-timeout")) {
			stageWallclock = Integer.parseInt(DOMHelper.getElementByName(stageAttributes, "wallclock-timeout")
			                                           .getAttribute("value"));
		}
		long stageMemory = memoryLimit;
		if (DOMHelper.hasElement(stageAttributes, "mem-limit")) {
			Double gigMem =
					Double.parseDouble(DOMHelper.getElementByName(stageAttributes, "mem-limit").getAttribute("value"));
			stageMemory = Util.gigabytesToBytes(gigMem);
		}

		//the space to put new benchmarks created from the job output into
		Integer stageSpace = null;
		if (DOMHelper.hasElement(stageAttributes, "space-id")) {
			stageSpace =
					Integer.parseInt(DOMHelper.getElementByName(stageAttributes, "space-id").getAttribute("value"));
		}

		// if no suffix is given, benchmarks will retain their old suffixes when they are created
		String stageBenchSuffix = null;
		if (DOMHelper.hasElement(stageAttributes, "bench-suffix")) {
			stageBenchSuffix = DOMHelper.getElementByName(stageAttributes, "bench-suffix").getAttribute("value");
		}

		// If processors are not given in the stage attributes, that means they are not used for this ttage
		Integer stagePostProcId = null;
		if (DOMHelper.hasElement(stageAttributes, "postproc-id")) {
			stagePostProcId =
					Integer.parseInt(DOMHelper.getElementByName(stageAttributes, "postproc-id").getAttribute("value"));
			attrs.setPostProcessor(Processors.get(stagePostProcId));

		}
		Integer stagePreProcId = null;
		if (DOMHelper.hasElement(stageAttributes, "preproc-id")) {
			stagePreProcId =
					Integer.parseInt(DOMHelper.getElementByName(stageAttributes, "preproc-id").getAttribute("value"));
			attrs.setPreProcessor(Processors.get(stagePreProcId));

		}

		if (DOMHelper.hasElement(stageAttributes, "results-interval")) {
			int resultsInterval = Integer.parseInt(DOMHelper.getElementByName(stageAttributes, "results-interval")
			                                                .getAttribute("value"));
			attrs.setResultsInterval(resultsInterval);
		} else {
			attrs.setResultsInterval(0);
		}

		if (DOMHelper.hasElement(stageAttributes, "stdout-save")) {
			attrs.setStdoutSaveOption(SaveResultsOption
					.stringToOption(DOMHelper.getElementByName(stageAttributes, "stdout-save").getAttribute("value")));
		}

		if (DOMHelper.hasElement(stageAttributes, "other-save")) {
			attrs.setExtraOutputSaveOption(SaveResultsOption
					.stringToOption(DOMHelper.getElementByName(stageAttributes, "other-save").getAttribute("value")));
		}

		//validate this new set of parameters
		ValidatorStatusCode stageStatus =
				CreateJob.isValid(userId, queueId, cpuTimeout, wallclock, stagePreProcId, stagePostProcId);
		if (!stageStatus.isSuccess()) {
			errorMessage = stageStatus.getMessage();
			return null;
		}

		//also make sure the user can add both spaces and benchmarks to the given space
		if (stageSpace != null) {
			Permission p = Permissions.get(userId, stageSpace);
			if (!p.canAddBenchmark() || !p.canAddSpace()) {
				errorMessage =
						"You do not have permission to add benchmarks or spaces to the space with id = " + stageSpace;
				return null;
			}
		}


		attrs.setWallclockTimeout(stageWallclock);
		attrs.setCpuTimeout(stageCpu);
		attrs.setMaxMemory(stageMemory);
		attrs.setSpaceId(stageSpace);
		attrs.setBenchSuffix(stageBenchSuffix);
		return attrs;
	}

	/**
	 * Creates a single job from an XML job element.
	 *
	 * @param userId the ID of the user creating the job
	 * @param spaceId the space in which the job will be created
	 * @param jobElement the XML job element as defined in the deployed public/batchJobSchema.xsd
	 * @return The id of the new job on success or -1 on failure
	 * @author Tim Smith
	 */
	private Integer createJobFromElement(int userId, Integer spaceId, Element jobElement, HashMap<String,
			SolverPipeline> pipelines, ConfigAttrMapPair configAttrMapPair) {
		try {
			final String method = "createJobFromElement";

			Element jobAttributes = DOMHelper.getElementByName(jobElement, "JobAttributes");
			HashMap<Integer, Solver> configIdsToSolvers = new HashMap<>();

			Job job = new Job();
			job.setName(jobElement.getAttribute("name"));
			log.info("name set");
			if (DOMHelper.hasElement(jobAttributes, "description")) {
				Element description = DOMHelper.getElementByName(jobAttributes, "description");
				job.setDescription(description.getAttribute("value"));
			} else {
				job.setDescription("no description");
			}

			if (DOMHelper.hasElement(jobAttributes, R.XML_BENCH_FRAMEWORK_ELE_NAME)) {
				Element framework = DOMHelper.getElementByName(jobAttributes, R.XML_BENCH_FRAMEWORK_ELE_NAME);

				BenchmarkingFramework selectedFramework =
						BenchmarkingFramework.valueOf(framework.getAttribute("value").toUpperCase());

				job.setBenchmarkingFramework(selectedFramework);
			} else {
				job.setBenchmarkingFramework(R.DEFAULT_BENCHMARKING_FRAMEWORK);
			}

			job.setUserId(userId);

			String jobId = jobElement.getAttribute("id");
			if (!Util.isNullOrEmpty(jobId)) {
				log.info("job id set: " + jobId);
				job.setId(Integer.parseInt(jobId));
			}


			Element queueIdEle = DOMHelper.getElementByName(jobAttributes, "queue-id");
			int queueId = Integer.parseInt(queueIdEle.getAttribute("value"));

			Queue queue = Queues.get(queueId);
			job.setQueue(queue);
			job.setPrimarySpace(spaceId);

			String rootName = Spaces.getName(spaceId);

			// this attributes object will be ignored if there is an explicit StageAttributes tag for stage 1
			StageAttributes stageOneAttributes = new StageAttributes();
			stageOneAttributes.setStageNumber(1);

			Element wallclockEle = DOMHelper.getElementByName(jobAttributes, "wallclock-timeout");
			int wallclock = Integer.parseInt(wallclockEle.getAttribute("value"));
			job.setWallclockTimeout(wallclock);
			stageOneAttributes.setWallclockTimeout(wallclock);

			Element cpuTimeoutEle = DOMHelper.getElementByName(jobAttributes, "cpu-timeout");
			int cpuTimeout = Integer.parseInt(cpuTimeoutEle.getAttribute("value"));
			job.setCpuTimeout(cpuTimeout);
			stageOneAttributes.setCpuTimeout(cpuTimeout);

			if (DOMHelper.hasElement(jobAttributes, "seed")) {
				final Element seedElement = DOMHelper.getElementByName(jobAttributes, "seed");
				final long seed = Long.parseLong(seedElement.getAttribute("value"));
				job.setSeed(seed);
			}

			if (DOMHelper.hasElement(jobAttributes, "kill-delay")) {
				Element killDelayEle = DOMHelper.getElementByName(jobAttributes, "kill-delay");
				int killDelay = Integer.parseInt(killDelayEle.getAttribute("value"));
				if (killDelay >= 10 && killDelay <= R.MAX_KILL_DELAY) {
					job.setKillDelay(killDelay);
				}
			}

			if (DOMHelper.hasElement(jobAttributes, "soft-time-limit")) {
				Element softTimeLimitEle = DOMHelper.getElementByName(jobAttributes, "soft-time-limit");
				int softTimeLimit = Integer.parseInt(softTimeLimitEle.getAttribute("value"));
				if (softTimeLimit >= 0) {
					job.setSoftTimeLimit(softTimeLimit);
				}
			}

			Element memLimitEle = DOMHelper.getElementByName(jobAttributes, "mem-limit");
			double memLimit = Double.parseDouble(memLimitEle.getAttribute("value"));
			long memoryLimit = Util.gigabytesToBytes(memLimit);
			memoryLimit = (memoryLimit <= 0) ? R.DEFAULT_PAIR_VMEM : memoryLimit; //bounds memory limit by system max
			job.setMaxMemory(memoryLimit);
			stageOneAttributes.setMaxMemory(memoryLimit);

			// If processors are not given in the stage attributes, that means they are not used for this ttage
			Integer postProcId = null;
			if (DOMHelper.hasElement(jobAttributes, "postproc-id")) {
				postProcId = Integer.parseInt(DOMHelper.getElementByName(jobAttributes, "postproc-id")
				                                       .getAttribute("value"));
				stageOneAttributes.setPostProcessor(Processors.get(postProcId));

			}
			Integer preProcId = null;
			if (DOMHelper.hasElement(jobAttributes, "preproc-id")) {
				preProcId =
						Integer.parseInt(DOMHelper.getElementByName(jobAttributes, "preproc-id").getAttribute
								("value"));
				stageOneAttributes.setPreProcessor(Processors.get(preProcId));
			}

			if (DOMHelper.hasElement(jobAttributes, "results-interval")) {
				int resultsInterval = Integer.parseInt(DOMHelper.getElementByName(jobAttributes, "results-interval")
				                                                .getAttribute("value"));

				stageOneAttributes.setResultsInterval(resultsInterval);
			} else {
				stageOneAttributes.setResultsInterval(0);
			}

			//validate memory limits
			ValidatorStatusCode status = CreateJob.isValid(userId, queueId, cpuTimeout, wallclock, null, null);
			if (!status.isSuccess()) {
				errorMessage = "CreateJob.isValid: " + status.getMessage();
				return -1;
			}

			log.info("nodelist about to be set");

			int maxStages = 0;
			for (SolverPipeline pipe : pipelines.values()) {
				maxStages = Math.max(maxStages, pipe.getStages().size());
			}

			//next, we set the per-stage job attributes
			NodeList stageAttributeElements = jobElement.getElementsByTagName("StageAttributes");
			for (int index = 0; index < stageAttributeElements.getLength(); index++) {
				Element stageAttributes = (Element) stageAttributeElements.item(index);
				StageAttributes attrs =
						elementToStageAttributes(stageAttributes, maxStages, userId, cpuTimeout, wallclock,
								memoryLimit, queueId);
				if (attrs == null) {
					errorMessage = "elementToStageAttributes returned null.";
					return -1;
				}
				job.addStageAttributes(attrs);
			}

			if (!job.containsStageOneAttributes()) {
				job.addStageAttributes(stageOneAttributes);
			}
			//this is the set of every top level space path given in the XML. There must be exactly 1 top level space,
			// so if there is
			// more than one then we will need to prepend the rootName onto every pair path to condense it
			// to a single root space
			HashSet<String> jobRootPaths = new HashSet<>();
			Map<Integer, Benchmark> accessibleCachedBenchmarks = new HashMap<>();
			// IMPORTANT: For efficieny reasons this function has the side-effect of populating configIdsToSolvers
			//			  as well as accessibleCachedBenchmarks

			final NodeList jobPairs = jobElement.getElementsByTagName("JobPair");
			Optional<String> potentialError =
					JobPairs.populateConfigIdsToSolversMapAndJobPairsForJobXMLUpload(rootName, userId,
							accessibleCachedBenchmarks, configIdsToSolvers, job, spaceId, jobRootPaths, new
									ConfigAttrMapPair(ConfigXmlAttribute.ID), // We need to use config-id as the
							// attribute for JobPair elements.
							jobPairs);
			if (potentialError.isPresent()) {
				errorMessage = "Error parsing JobPair elements: " + potentialError.get();
				return -1;
			}
			final NodeList uploadedSolverJobPairs = jobElement.getElementsByTagName("UploadedSolverJobPair");
			potentialError =
					JobPairs.populateConfigIdsToSolversMapAndJobPairsForJobXMLUpload(rootName, userId,
							accessibleCachedBenchmarks, configIdsToSolvers, job, spaceId, jobRootPaths,
							configAttrMapPair, uploadedSolverJobPairs);
			if (potentialError.isPresent()) {
				errorMessage = "Error parsing UploadedSolverJopPair elements: " + potentialError.get();
				return -1;
			}

			//JobLine elements are still job pairs, but they are how multi-stage pairs are denoted
			//in the XML
			NodeList jobLines = jobElement.getElementsByTagName("JobLine");
			for (int i = 0; i < jobLines.getLength(); i++) {
				Node jobLineNode = jobLines.item(i);
				if (jobLineNode.getNodeType() == Node.ELEMENT_NODE) {
					Element jobLineElement = (Element) jobLineNode;

					JobPair jobPair = new JobPair();
					int benchmarkId = Integer.parseInt(jobLineElement.getAttribute("bench-id"));

					//JobLine elements must reference some pipeline that was created in this file
					String pipeName = jobLineElement.getAttribute("pipe-name");
					if (!pipelines.containsKey(pipeName)) {
						errorMessage =
								"the pipeline with name = " + pipeName + " is not declared as a pipeline in this file";
						return -1;
					}
					SolverPipeline currentPipe = pipelines.get(pipeName);
					if (!job.isUsingDependencies() && currentPipe.usesDependencies()) {
						job.setUsingDependencies(true);
					}
					//get the path of the job space for this pair. If empty, just use the root space
					String path = jobLineElement.getAttribute("job-space-path");
					if (path.isEmpty()) {
						path = rootName;
					}
					jobPair.setPath(path);

					//add the top level space of this path to the set of all top level spaces
					if (path.contains(R.JOB_PAIR_PATH_DELIMITER)) {
						jobRootPaths.add(path.substring(0, path.indexOf(R.JOB_PAIR_PATH_DELIMITER)));
					} else {
						jobRootPaths.add(path);
					}

					Benchmark b = null;
					if (accessibleCachedBenchmarks.containsKey(benchmarkId)) {
						b = accessibleCachedBenchmarks.get(benchmarkId);
					} else {
						b = Benchmarks.get(benchmarkId);
						if (!Permissions.canUserSeeBench(benchmarkId, userId)) {
							errorMessage = "You do not have permission to see benchmark " + benchmarkId;
							return -1;
						}
						accessibleCachedBenchmarks.put(benchmarkId, b);
					}
					jobPair.setBench(b);

					//the benchmark inputs for the pair.
					NodeList inputs = jobLineElement.getElementsByTagName("BenchmarkInput");
					for (int inputIndex = 0; inputIndex < inputs.getLength(); inputIndex++) {
						Element inputElement = (Element) inputs.item(inputIndex);
						int benchmarkInput = Integer.parseInt(inputElement.getAttribute("bench-id"));
						// If the benchmark cache already contains the bench id then we know the user can see it.
						if (!accessibleCachedBenchmarks.containsKey(benchmarkInput)) {
							if (!Permissions.canUserSeeBench(benchmarkInput, userId)) {
								errorMessage = "You do not have permission to see benchmark input " + benchmarkId;
								return -1;
							}
						}
						jobPair.addBenchInput(benchmarkInput);
					}

					//make sure that if the pipeline requires n inputs, this pair actually has n specified inputs
					if (currentPipe.getRequiredNumberOfInputs() != jobPair.getBenchInputs().size()) {
						errorMessage =
								"Job pairs have invalid inputs. Given inputs = " + jobPair.getBenchInputs().size() +
								", but " + "required inputs = " + currentPipe.getRequiredNumberOfInputs();
						return -1;
					}


					// add all the jobline stages to this pair, generating the jobline stages from the pipeline stages
					// of the pipeline being referenced.
					int stageNumber = 0;
					for (PipelineStage s : currentPipe.getStages()) {
						stageNumber++;
						JoblineStage stage = new JoblineStage();
						stage.setStageNumber(stageNumber);
						if (s.isNoOp()) {
							stage.setNoOp(true);

						} else {
							stage.setNoOp(false);
							int configId = s.getConfigId();

							if (!configIdsToSolvers.containsKey(configId)) {
								Solver solver = Solvers.getSolverByConfig(configId, false);
								if (!Permissions.canUserSeeSolver(solver.getId(), userId)) {
									errorMessage = "You do not have permission to see the solver " + solver.getId();
									return -1;
								}
								solver.addConfiguration(Solvers.getConfiguration(configId));
								configIdsToSolvers.put(configId, solver);
							}
							Solver solver = configIdsToSolvers.get(configId);
							stage.setSolver(solver);
							stage.setConfiguration(solver.getConfigurations().get(0));
							stage.setStageId(s.getId());

							// if the stage is primary, then set it as such in the job pair
							if (s.isPrimary()) {
								jobPair.setPrimaryStageNumber(jobPair.getStages().size() + 1);
							}
						}
						jobPair.addStage(stage);

					}
					jobPair.setSpace(Spaces.get(spaceId));
					job.addJobPair(jobPair);
				}
			}

			log.info("job pairs set");

			if (job.getJobPairs().isEmpty()) {
				// No pairs in the job means something is wrong; error out
				errorMessage = "Error: no job pairs created for the job. Could not proceed with job submission.";
				return -1;
			}


			// pairs must have exactly 1 root space, so if there is more than one, we prepend the rootname onto every
			// path
			if (jobRootPaths.size() > 1) {
				for (JobPair p : job.getJobPairs()) {
					p.setPath(rootName + R.JOB_PAIR_PATH_DELIMITER + p.getPath());
				}
			} else {
				rootName = jobRootPaths.iterator().next();
			}
			//check to make sure that, for all spaces where we will be creating mirrored hierarchies to store new
			// benchmarks,
			//that we actually can create the mirrored hierarchy without name collisions.
			for (StageAttributes attrs : job.getStageAttributes()) {
				if (attrs.getSpaceId() != null) {
					if (Spaces.getSubSpaceIDbyName(attrs.getSpaceId(), rootName) != -1) {
						errorMessage = "Error: Can't use space id = " + attrs.getSpaceId() +
						               " to save benchmarks because it already contains a subspace with the name " +
						               rootName;
						return -1;
					}

				}
			}

			log.info("job pair size nonzero");

			boolean startPaused = getBooleanElementValue(false, "start-paused", jobAttributes);
			boolean suppressTimestamps = getBooleanElementValue(false, "suppress-timestamps", jobAttributes);

			job.setSuppressTimestamp(suppressTimestamps);


			log.info("start-paused: " + (Boolean.toString(startPaused)));

			boolean submitSuccess = Jobs.add(job, spaceId);
			if (!submitSuccess) {
				errorMessage = "Error: could not add job with id " + job.getId() + " to space with id " + spaceId;
				return -1;
			} else if (startPaused) {
				Jobs.pause(job.getId());
			}

			int newJobId = job.getId();
			if (newJobId == -1) {
				errorMessage = "Job id was never set.";
				// could just skip this and return newJobId but this makes the -1 return explicit.
				return -1;
			}
			return newJobId;
		} catch (Exception e) {
			log.error(e.getMessage(), e);
			errorMessage = "Internal error when creating your job: " + e.getMessage();
			return -1;
		}
	}

	/**
	 * Gets the value of a boolean element contained in an XML element.
	 *
	 * @param defaultValue return this value if the element is not found.
	 * @param elementName the name of the element to get the value of.
	 * @param attributes the containing element of the element to find.
	 * @author Albert Giegerich
	 */
	public boolean getBooleanElementValue(boolean defaultValue, String elementName, Element attributes) {
		if (DOMHelper.hasElement(attributes, elementName)) {
			Element booleanElement = DOMHelper.getElementByName(attributes, elementName);
			log.info(elementName + booleanElement.getAttribute("value"));
			return Boolean.valueOf(booleanElement.getAttribute("value"));
		} else {
			return defaultValue;
		}
	}

	/**
	 * Checks that the XML file uploaded is valid against the deployed public/batchJobSchema.xsd
	 *
	 * @param file the XML file to be validated against the XSD
	 * @author Tim Smith
	 */
	public Boolean validateAgainstSchema(File file, JobXmlType jobXmlType) throws ParserConfigurationException,
			IOException {
		ValidatorStatusCode code = XMLUtil.validateAgainstSchema(file, jobXmlType.schemaPath);
		errorMessage = code.getMessage();
		return code.isSuccess();
	}

	public String getSecondaryErrorMessage() {
		return secondaryErrorMessage;
	}

	public String getErrorMessage() {
		return errorMessage;
	}

	public void setErrorMessage(String errorMessage) {
		this.errorMessage = errorMessage;
	}

	public Boolean getJobCreationSuccess() {
		return jobCreationSuccess;
	}

	public void setJobCreationSuccess(Boolean jobCreationSuccess) {
		this.jobCreationSuccess = jobCreationSuccess;
	}

}
