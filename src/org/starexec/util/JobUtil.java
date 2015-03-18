package org.starexec.util;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.ErrorManager;

import javax.servlet.http.HttpServletResponse;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Source;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;

import org.apache.log4j.Logger;
import org.starexec.constants.R;
import org.starexec.data.database.Benchmarks;
import org.starexec.data.database.JobPairs;
import org.starexec.data.database.Jobs;
import org.starexec.data.database.Permissions;
import org.starexec.data.database.Pipelines;
import org.starexec.data.database.Processors;
import org.starexec.data.database.Queues;
import org.starexec.data.database.Solvers;
import org.starexec.data.database.Spaces;
import org.starexec.data.security.ProcessorSecurity;
import org.starexec.data.security.ValidatorStatusCode;
import org.starexec.data.to.Benchmark;
import org.starexec.data.to.Job;
import org.starexec.data.to.JobPair;
import org.starexec.data.to.Permission;
import org.starexec.data.to.Processor;
import org.starexec.data.to.Queue;
import org.starexec.data.to.Solver;
import org.starexec.data.to.pipelines.*;
import org.starexec.data.to.pipelines.PipelineDependency.PipelineInputType;
import org.starexec.jobs.JobManager;
import org.starexec.servlets.CreateJob;
import org.starexec.util.DOMHelper;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public class JobUtil {
	private static final Logger log = Logger.getLogger(JobUtil.class);
	
	private Boolean jobCreationSuccess = false;
	private String errorMessage = "";//this will be used to given information to user about failures in validation
	
	/**
	 * Creates jobs from the xml file. This also creates any solver pipelines defined in the XML document
	 * @author Tim Smith
	 * @param file the xml file we wish to create jobs from
	 * @param userId the userId of the user making the request
	 * @param spaceId the space that will serve as the root for jobs to run under
	 * @return List<Integer> Null on failure, and a list of jobIds on success. Some jobs may have failed,
	 * resulting in values of -1 in the list.
	 * @throws SAXException
	 * @throws ParserConfigurationException
	 * @throws IOException
	 */
	public List<Integer> createJobsFromFile(File file, int userId, Integer spaceId) throws Exception {
		List<Integer> jobIds=new ArrayList<Integer>();
		if (!validateAgainstSchema(file)){
			log.warn("File from User " + userId + " is not Schema valid.");
			return null;
		}
		
		Permission p = Permissions.get(userId, spaceId);
		if (!p.canAddJob()){
			errorMessage = "You do not have permission to create a job on this space";
			return null;
		}
		
		DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder docBuilder = docBuilderFactory.newDocumentBuilder();
        Document doc = docBuilder.parse(file);
        Element jobsElement = doc.getDocumentElement();
		NodeList listOfJobElements = jobsElement.getElementsByTagName("Job");
		
		NodeList listOfPipelines = doc.getElementsByTagName("SolverPipeline");
		log.info("# of pipelines = " + listOfPipelines.getLength());
		
        //Check Jobs and Job Pairs
        NodeList listOfJobs = doc.getElementsByTagName("Job");
		log.info("# of Jobs = " + listOfJobs.getLength());
        NodeList listOfJobPairs = doc.getElementsByTagName("JobPair");
        
		log.info("# of JobPairs = " + listOfJobPairs.getLength());
		NodeList listOfJobLines= doc.getElementsByTagName("JobLine");
		log.info(" # of JobLines = "+listOfJobLines.getLength());
		//this job has nothing to run
		if (listOfJobLines.getLength()+listOfJobPairs.getLength()==0) {
			errorMessage="Every job must have at least one job pair or job line to be created";
			return null;
		}
		String name = "";//name variable to check
		
		
		//validate all solver pipelines
		
		//data structure to ensure all pipeline names in this upload are unique
		HashMap<String,SolverPipeline> pipelineNames=new HashMap<String,SolverPipeline>();
		for (int i=0; i< listOfPipelines.getLength(); i++) {
			Node pipeline = listOfPipelines.item(i);
			SolverPipeline pipe=createPipelineFromElement(userId, (Element) pipeline);
			if (pipelineNames.containsKey(pipe.getName())) {
				errorMessage=" Duplicate pipline name = "+pipe.getName()+". All pipelines in this upload must have unique names";
				return null;
			}
			pipelineNames.put(pipe.getName(),pipe);
		}
		
		// Make sure jobs are named
		for (int i = 0; i < listOfJobs.getLength(); i++){
			Node jobNode = listOfJobs.item(i);
			if (jobNode.getNodeType() == Node.ELEMENT_NODE){
				Element jobElement = (Element)jobNode;
				name = jobElement.getAttribute("name");
				if (name == null) {
					log.info("Name not found");
					errorMessage = "Job elements must include a 'name' attribute.";
					return null;
				}
				log.debug("Job Name = " + name);
				if (name.length()<1){
					log.info("Name was not long enough");
					errorMessage = name + "is not a valid name.  It must have one characters.";
					return null;
				}
				
			}
			else{
				log.warn("Job Node should be an element, but isn't");
			}
		}
		
		this.jobCreationSuccess = true;
		
		for (int i = 0; i < listOfJobElements.getLength(); i++){
			Node jobNode = listOfJobElements.item(i);
			if (jobNode.getNodeType() == Node.ELEMENT_NODE){
				Element jobElement = (Element)jobNode;
				log.info("about to create job from element");
				Integer id = createJobFromElement(userId, spaceId, jobElement,pipelineNames);
				if (id < 0) {
				    this.jobCreationSuccess = false;
				    break; // out of for loop
				}
				jobIds.add(id);
			}
		}

		return jobIds;
	}
	
	
	/**
	 * Creates a single solver pipeline from a SolverPipeline XML element. If there are any errors,
	 * returns null
	 * @param userId
	 * @param pipeElement
	 * @return
	 */
	private SolverPipeline createPipelineFromElement(int userId, Element pipeElement) {
		boolean foundPrimary=false;
		SolverPipeline pipeline=new SolverPipeline();
		pipeline.setUserId(userId);
		
		pipeline.setName(pipeElement.getAttribute("name"));
		
		NodeList stages= pipeElement.getChildNodes();
		
		//data structure for storing all of the unique benchmark inputs in this upload.
		//We need to validate the following rule-- if there are n unique benchmark inputs,
		//then the numbers on those inputs must go exactly from 1 to n.
		
		HashSet<Integer> benchmarkInputs=new HashSet<Integer>();
		
		//XML files have a stage tag for each stage
		int currentStage=0;
		List<PipelineStage> stageList=new ArrayList<PipelineStage>();
		for (int i=0;i<stages.getLength();i++) {
			
			if (stages.item(i).getNodeName().equals("PipelineStage")) {
				currentStage+=1;
				Element stage=(Element)stages.item(i);
				PipelineStage s=new PipelineStage();
				s.setNoOp(false);
				
				
				if (stage.hasAttribute("primary")) {
					boolean currentPrimary=Boolean.parseBoolean(stage.getAttribute("primary"));
					if (currentPrimary) {
						if (foundPrimary) {
							errorMessage="More than one primary stage for pipeline "+pipeline.getName();
							return null;
						}
						foundPrimary=true;
					}
					s.setPrimary(true);
					pipeline.setPrimaryStageNumber(currentStage);
				} else {
					s.setPrimary(false);
				}
				
				s.setConfigId(Integer.parseInt(stage.getAttribute("config")));
				// make sure the user is authorized to use the solver they are trying to use
				Solver solver = Solvers.getSolverByConfig(s.getConfigId(), false);
				if (!Permissions.canUserSeeSolver(solver.getId(), userId)){
				    errorMessage = "You do not have permission to see the solver " + s.getId();
				    return null;
				}
				
				
				NodeList dependencies=stage.getChildNodes();
				int inputNumber=0; 
				for (int x=0;x<dependencies.getLength();x++) {
					
					Node t=dependencies.item(x);
					if (t.getNodeType() == Node.ELEMENT_NODE) {
						Element dependency = (Element) t;
						PipelineDependency dep = new PipelineDependency();
						if (dependency.getTagName().equals("stageDependency")) {
							inputNumber++;
							
							dep.setType(PipelineInputType.ARTIFACT);
							int neededStageId=Integer.parseInt(dependency.getAttribute("stage"));
							
							if (neededStageId<1) {
								errorMessage = "Invalid stage dependency-- all stages are numbered 1 or greater";
								return null;
							} else if (neededStageId>=(currentStage-1)) {
								errorMessage="Invalid stage dependency-- stages can only depend on earlier stages, and a"
										+ " stages implicitly depend on previous stages. Bad dependency =  Stage "+currentStage+" depends on"
												+ " stage "+neededStageId;
										
							}
							dep.setDependencyId(neededStageId);		
						} else if (dependency.getTagName().equals("benchmarkDependency")) {
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
				currentStage+=1;
				PipelineStage stage=new PipelineStage();
				stage.setNoOp(true);
				stage.setConfigId(null);
				stage.setPrimary(false);
				stageList.add(stage);

			}
			
		}
		//ensure that benchmark inputs are ordered correctly
		if (benchmarkInputs.size()>0) {
			int maxSeen=Collections.max(benchmarkInputs);
			if (maxSeen!=benchmarkInputs.size()) {
				errorMessage="Invalid benchmark inputs for pipeline = "+pipeline.getName()+". Benchmark inputs must be numbered from 1 to n, where n is the total number of expected inputs";
				return null;
			}
		}
		
		if (stageList.size()>R.MAX_STAGES_PER_PIPELINE) {
			errorMessage="Too many stages in pipeline "+pipeline.getName()+". The maximum is "+R.MAX_STAGES_PER_PIPELINE;
			return null;
		}
		if (!foundPrimary) {
			errorMessage="No primary stage specified for pipeline "+pipeline.getName();
			return null;
		}
		pipeline.setStages(stageList);
		int id=Pipelines.addPipelineToDatabase(pipeline);
		if (id<=0) { //if there was a database error
			return null;
		}
		return pipeline;
	}

	/**
	 * Creates a single job from an XML job element.
	 * @param userId the ID of the user creating the job
	 * @param spaceId the space in which the job will be created
	 * @param jobElement the XML job element as defined in the deployed public/batchJobSchema.xsd
	 * @return The id of the new job on success or -1 on failure
	 * @author Tim Smith
	 */
	private Integer createJobFromElement(int userId, Integer spaceId,
			Element jobElement, HashMap<String,SolverPipeline> pipelines) {
	    try {
			
	    	
			Element jobAttributes = DOMHelper.getElementByName(jobElement,"JobAttributes");
			HashMap<Integer,Solver> configIdsToSolvers=new HashMap<Integer,Solver>();
	
			Job job = new Job();
			job.setName(jobElement.getAttribute("name"));
			log.info("name set");
			if(DOMHelper.hasElement(jobAttributes,"description")){
			    Element description = DOMHelper.getElementByName(jobAttributes,"description");
			    job.setDescription(description.getAttribute("value"));
			}
			else{
			    job.setDescription("no description");
			}
		    
		    
			//job.setDescription(jobElement.getAttribute("description"));
			log.info("description set");
			job.setUserId(userId);
			
			log.info("job id about to be set");
			String jobId = jobElement.getAttribute("id");
			if(jobId != "" && jobId != null){
			    log.info("job id set: " + jobId);
			    job.setId(Integer.parseInt(jobId));
			    
			}
			
	
			Element queueIdEle = DOMHelper.getElementByName(jobAttributes,"queue-id");
			int queueId = Integer.parseInt(queueIdEle.getAttribute("value"));
			
			Queue queue = Queues.get(queueId);
			job.setQueue(queue);
			job.setPrimarySpace(spaceId);
			
			String rootName=Spaces.getName(spaceId);
	
	
			Element wallclockEle = DOMHelper.getElementByName(jobAttributes,"wallclock-timeout");
			int wallclock = Integer.parseInt(wallclockEle.getAttribute("value"));
			job.setWallclockTimeout(wallclock);
	
			Element cpuTimeoutEle = DOMHelper.getElementByName(jobAttributes, "cpu-timeout");
			int cpuTimeout = Integer.parseInt(cpuTimeoutEle.getAttribute("value"));
			job.setCpuTimeout(cpuTimeout);
	
			Element memLimitEle = DOMHelper.getElementByName(jobAttributes, "mem-limit");
			double memLimit = Double.parseDouble(memLimitEle.getAttribute("value"));
			long memoryLimit=Util.gigabytesToBytes(memLimit);
			memoryLimit = (memoryLimit <=0) ? R.DEFAULT_PAIR_VMEM : memoryLimit; //bounds memory limit by system max
			
			//validate memory limits
			ValidatorStatusCode status=CreateJob.isValid(userId, queueId, cpuTimeout, wallclock, null, null);
			if (!status.isSuccess()) {
				errorMessage=status.getMessage();
				return -1;
			}
			
			job.setMaxMemory(memoryLimit);
			log.info("nodelist about to be set");
			
			int maxStages=0;
			for (SolverPipeline pipe : pipelines.values()) {
				maxStages=Math.max(maxStages, pipe.getStages().size());
			}
			
			//next, we set the per-stage job attributes
			NodeList stageAttributeElements=jobElement.getElementsByTagName("StageAttributes");
			for (int index=0;index<stageAttributeElements.getLength();index++) {
				Element stageAttributes= (Element) stageAttributeElements.item(index);
				StageAttributes attrs=new StageAttributes();
				
				
				//first,  we need to find which stage this is for, given the name of a pipeline and the stage number (not ID)
				
				int neededStageNum=Integer.parseInt(DOMHelper.getElementByName(stageAttributes, "stage-num").getAttribute("value"));
				if (neededStageNum<=0 || neededStageNum>maxStages) {
					errorMessage="StageAttributes tag has invalid stage-num = "+neededStageNum;
					return -1;
				}
				attrs.setStageNumber(neededStageNum);

				
				// all timeouts are optional-- they default to job timeouts if not given
				int stageCpu=cpuTimeout;
				if (DOMHelper.hasElement(stageAttributes, "cpu-timeout")) {
					stageCpu=Integer.parseInt(DOMHelper.getElementByName(stageAttributes, "cpu-timeout").getAttribute("value"));
				}
				int stageWallclock=wallclock;
				if (DOMHelper.hasElement(stageAttributes, "wallclock-timeout")) {
					stageWallclock=Integer.parseInt(DOMHelper.getElementByName(stageAttributes, "wallclock-timeout").getAttribute("value"));
				}
				long stageMemory=memoryLimit;
				if (DOMHelper.hasElement(stageAttributes, "mem-limit")) {
					Double gigMem=Double.parseDouble(DOMHelper.getElementByName(stageAttributes, "mem-limit").getAttribute("value"));
					stageMemory=Util.gigabytesToBytes(memLimit);
				}
				Integer stageSpace=null;
				if (DOMHelper.hasElement(stageAttributes, "space-id")) {
					stageSpace=Integer.parseInt(DOMHelper.getElementByName(stageAttributes, "space-id").getAttribute("value"));
				}
				
				Integer stagePostProcId=null;
				if (DOMHelper.hasElement(stageAttributes, "postproc-id")) {
					stagePostProcId=Integer.parseInt(DOMHelper.getElementByName(stageAttributes, "postproc-id").getAttribute("value"));
					attrs.setPostProcessor(Processors.get(stagePostProcId));

				}
				Integer stagePreProcId=null;
				if (DOMHelper.hasElement(stageAttributes, "preproc-id")) {
					stagePreProcId=Integer.parseInt(DOMHelper.getElementByName(stageAttributes, "preproc-id").getAttribute("value"));
					attrs.setPreProcessor(Processors.get(stagePreProcId));
				
				}
				
				//validate this new set of parameters
				ValidatorStatusCode stageStatus=CreateJob.isValid(userId, queueId, cpuTimeout, wallclock, stagePreProcId, stagePostProcId);
				if (!status.isSuccess()) {
					errorMessage=status.getMessage();
					return -1;
				}
				
				//also make sure the user can add both spaces and benchmarks to the given space
				if (stageSpace!=null) {
					Permission p = Permissions.get(userId,stageSpace);
					if (!p.canAddBenchmark() || !p.canAddSpace()) {
						errorMessage="You do not have permission to add benchmarks or spaces to the space with id = "+stageSpace;
						return -1;
					}
				}
				
				//user can specify an optional space ID 
				
				attrs.setWallclockTimeout(stageWallclock);
				attrs.setCpuTimeout(stageCpu);
				attrs.setMaxMemory(stageMemory);
				attrs.setSpaceId(stageSpace);
				
				job.addStageAttributes(attrs);
			}
			
			//this is the set of every top level space path given in the XML. There must be exactly 1 top level space,
			// so if there is more than one then we will need to prepend the rootName onto every pair path to condense it
			// to a single root space
			HashSet<String> jobRootPaths=new HashSet<String>();
			NodeList jobPairs = jobElement.getElementsByTagName("JobPair");
			for (int i = 0; i < jobPairs.getLength(); i++) {
			    Node jobPairNode = jobPairs.item(i);
			    if (jobPairNode.getNodeType() == Node.ELEMENT_NODE){
					Element jobPairElement = (Element)jobPairNode;
						
						JobPair jobPair = new JobPair();
						int benchmarkId = Integer.parseInt(jobPairElement.getAttribute("bench-id"));
						int configId = Integer.parseInt(jobPairElement.getAttribute("config-id"));
						String path = jobPairElement.getAttribute("job-space-path");
						if (path.equals("")) {
							path=rootName;
							
						}
						jobPair.setPath(path);
						if (path.contains(R.JOB_PAIR_PATH_DELIMITER)) {
							jobRootPaths.add(path.substring(0,path.indexOf(R.JOB_PAIR_PATH_DELIMITER)));
						} else {
							jobRootPaths.add(path);
						}
						
					Benchmark b = Benchmarks.get(benchmarkId);
					if (!Permissions.canUserSeeBench(benchmarkId, userId)){
					    errorMessage = "You do not have permission to see benchmark " + benchmarkId;
					    return -1;
					}
					jobPair.setBench(b);
					if (!configIdsToSolvers.containsKey(configId)) {
						Solver s = Solvers.getSolverByConfig(configId, false);
						if (!Permissions.canUserSeeSolver(s.getId(), userId)){
						    errorMessage = "You do not have permission to see the solver " + s.getId();
						    return -1;
						}
						
						s.addConfiguration(Solvers.getConfiguration(configId));
						configIdsToSolvers.put(configId, s);
					}
					Solver s = configIdsToSolvers.get(configId);
					
					//no actual pipeline yet exists for this stage-- one will be created 
					// when the job is added
					JoblineStage stage=new JoblineStage();
					stage.setSolver(s);
					stage.setConfiguration(s.getConfigurations().get(0));
					
					jobPair.addStage(stage);
					//the primary stage is the one we just added
					jobPair.setPrimaryStageNumber(jobPair.getStages().size());
					jobPair.setSpace(Spaces.get(spaceId));
					
						
					job.addJobPair(jobPair);
			    }
			}
			NodeList jobLines = jobElement.getElementsByTagName("JobLine");
			for (int i = 0; i < jobLines.getLength(); i++) {
			    Node jobLineNode = jobLines.item(i);
			    if (jobLineNode.getNodeType() == Node.ELEMENT_NODE){
			    	Element jobLineElement = (Element)jobLineNode;
					
					JobPair jobPair = new JobPair();
					int benchmarkId = Integer.parseInt(jobLineElement.getAttribute("bench-id"));
					String pipeName = jobLineElement.getAttribute("pipe-name");
					if (!pipelines.containsKey(pipeName)) {
						errorMessage="the pipeline with name = "+pipeName+" is not declared as a pipeline in this file";
						return -1;
					}
					SolverPipeline currentPipe=pipelines.get(pipeName);
					String path = jobLineElement.getAttribute("job-space-path");
					if (path.equals("")) {
						path=rootName;
					}
					jobPair.setPath(path);
					if (path.contains(R.JOB_PAIR_PATH_DELIMITER)) {
						jobRootPaths.add(path.substring(0,path.indexOf(R.JOB_PAIR_PATH_DELIMITER)));
					} else {
						jobRootPaths.add(path);
					}
					
					Benchmark b = Benchmarks.get(benchmarkId);
					if (!Permissions.canUserSeeBench(benchmarkId, userId)){
					    errorMessage = "You do not have permission to see benchmark " + benchmarkId;
					    return -1;
					}
					jobPair.setBench(b);
					
					NodeList inputs=jobLineElement.getElementsByTagName("BenchmarkInput");
					for (int inputIndex=0;inputIndex<inputs.getLength();inputIndex++) {
						Element inputElement=(Element)inputs.item(inputIndex);
						int benchmarkInput=Integer.parseInt(inputElement.getAttribute("bench-id"));
						if (!Permissions.canUserSeeBench(benchmarkInput, userId)){
						    errorMessage = "You do not have permission to see benchmark input " + benchmarkId;
						    return -1;
						}
						jobPair.addBenchInput(benchmarkInput);
					}
					if (currentPipe.getRequiredNumberOfInputs()!=jobPair.getBenchInputs().size()) {
						errorMessage="Job pairs have invalid inputs. Given inputs = "+jobPair.getBenchInputs().size()+", but "
								+ "required inputs = "+currentPipe.getRequiredNumberOfInputs();
					}
					// add all the jobline stages to this pair
					for (PipelineStage s : currentPipe.getStages()) {
						JoblineStage stage = new JoblineStage();
						if (s.isNoOp()) {
							stage.setNoOp(true);
							
						} else {
							stage.setNoOp(false);
							int configId=s.getConfigId();
							
							if (!configIdsToSolvers.containsKey(configId)) {
								Solver solver = Solvers.getSolverByConfig(configId, false);
								if (!Permissions.canUserSeeSolver(s.getId(), userId)){
								    errorMessage = "You do not have permission to see the solver " + s.getId();
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
								jobPair.setPrimaryStageNumber(jobPair.getStages().size()+1);
							}
						}
						jobPair.addStage(stage);
						
					}
					jobPair.setSpace(Spaces.get(spaceId));
					job.addJobPair(jobPair);
			    }
			}
			
			
			log.info("job pairs set");
	
			if (job.getJobPairs().size() == 0) {
			    // No pairs in the job means something is wrong; error out
			    errorMessage = "Error: no job pairs created for the job. Could not proceed with job submission.";
			    return -1;
			}
			
			// pairs must have exactly 1 root space, so if there is more than one, we prepend the rootname onto every path
			if (jobRootPaths.size()>1) {
				for (JobPair p : job.getJobPairs()) {
					p.setPath(rootName+R.JOB_PAIR_PATH_DELIMITER+p.getPath());
				}
			} else {
				rootName=jobRootPaths.iterator().next();
			}
			
			for (StageAttributes attrs: job.getStageAttributes()) {
				if (attrs.getSpaceId()!=null) {
					if (Spaces.getSubSpaceIDbyName(attrs.getSpaceId(), rootName)!=-1) {
						errorMessage = "Error: Can't use space id = "+attrs.getSpaceId()+" to save benchmarks because it already contains a subspace with the name "+rootName;
						return -1;
					}

				}
			}
			
			
			log.info("job pair size nonzero");
	
			boolean startPaused = false;
	
			if(DOMHelper.hasElement(jobAttributes,"start-paused")){
			    Element startPausedEle = DOMHelper.getElementByName(jobAttributes,"start-paused");
			    log.info("startPausedEle: " + startPausedEle.getAttribute("value"));
			    startPaused = Boolean.valueOf(startPausedEle.getAttribute("value"));
			}
	
			log.info("start-paused: " + (new Boolean(startPaused).toString()));
	
			boolean submitSuccess = Jobs.add(job, spaceId);
			if (!submitSuccess){
			    errorMessage = "Error: could not add job with id " + job.getId() + " to space with id " + spaceId;
			    return -1;
			} else if (startPaused) {
			    Jobs.pause(job.getId());
			}
			return job.getId();
		
	    }
	    catch (Exception e) {
			log.error(e.getMessage(),e);
			errorMessage = "Internal error when creating your job: "+e.getMessage();
			return -1;
	    }
	}

	/**
	 * Checks that the XML file uploaded is valid against the deployed public/batchJobSchema.xsd
	 * @param file the XML file to be validated against the XSD
	 * @author Tim Smith
	 */
	public Boolean validateAgainstSchema(File file) throws ParserConfigurationException, IOException{
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		factory.setValidating(false);//This is true for DTD, but not W3C XML Schema that we're using
		factory.setNamespaceAware(true);

		SchemaFactory schemaFactory = SchemaFactory.newInstance("http://www.w3.org/2001/XMLSchema");

		try {
			String schemaLoc = Util.url("public/batchJobSchema.xsd");
			factory.setSchema(schemaFactory.newSchema(new Source[] {new StreamSource(schemaLoc)}));
			Schema schema = factory.getSchema();
			DocumentBuilder builder = factory.newDocumentBuilder();
			Document document = builder.parse(file);
			Validator validator = schema.newValidator();
			DOMSource source = new DOMSource(document);
            validator.validate(source);
            log.debug("Job XML File has been validated against the schema.");
            return true;
        } catch (SAXException ex) {
            log.warn("File is not valid because: \"" + ex.getMessage() + "\"");
            errorMessage = "File is not valid because: \"" + ex.getMessage() + "\"";
            this.jobCreationSuccess = false;
            return false;
        }
		
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
