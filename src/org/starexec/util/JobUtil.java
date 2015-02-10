package org.starexec.util;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
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
		
		String name = "";//name variable to check
		
		
		//validate all solver pipelines
		for (int i=0; i< listOfPipelines.getLength(); i++) {
			Node pipeline = listOfPipelines.item(i);
			int pipeId=createPipelineFromElement(userId, (Element) pipeline);
			log.debug("new pipeline received id = "+pipeId);
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
				Integer id = createJobFromElement(userId, spaceId, jobElement);
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
	 * Creates a single solver pipeline from a SolverPipeline XML element
	 * @param userId
	 * @param pipeElement
	 * @return
	 */
	private Integer createPipelineFromElement(int userId, Element pipeElement) {
		SolverPipeline pipeline=new SolverPipeline();
		pipeline.setUserId(userId);
		
		pipeline.setName(pipeElement.getAttribute("pipelineName"));
		NodeList stages= pipeElement.getElementsByTagName("PipelineStage");
		List<PipelineStage> stageList=new ArrayList<PipelineStage>();
		for (int i=0;i<stages.getLength();i++) {
			Element stage=(Element)stages.item(i);
			PipelineStage s=new PipelineStage();
			s.setKeepOutput(false);
			s.setExecutableId(Integer.parseInt(stage.getAttribute("executable")));
			if (stage.hasAttribute("keepoutput")) {
				s.setKeepOutput(Boolean.parseBoolean(stage.getAttribute("keepoutput")));
			}
			NodeList dependencies=stage.getChildNodes();
			int inputNumber=0;
			for (int x=0;x<dependencies.getLength();x++) {
				Node t=dependencies.item(x);
				if (t.getNodeType() == Node.ELEMENT_NODE) {
					inputNumber++;
					Element dependency = (Element) t;
					PipelineDependency dep = new PipelineDependency();
					dep.setInputNumber(inputNumber);
					if (dependency.getTagName().equals("stageDependency")) {
						dep.setType(PipelineInputType.ARTIFACT);
						dep.setDependencyId(Integer.parseInt(dependency.getAttribute("stage")));

					} else if (dependency.getTagName().equals("benchmarkDependency")) {
						dep.setType(PipelineInputType.BENCHMARK);
						dep.setDependencyId(Integer.parseInt(dependency.getAttribute("input")));
					} else {
						log.warn("the following bad dep was found "+dependency.getTagName());
					}
					
				}
			}
			stageList.add(s);
		}
		pipeline.setStages(stageList);
		return Pipelines.addPipelineToDatabase(pipeline);
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
			Element jobElement) {
	    try {
		

		Element jobAttributes = DOMHelper.getElementByName(jobElement,"JobAttributes");
		

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

		log.info("preProcId about to be set");
		
		Integer preProcId = null;

		if(DOMHelper.hasElement(jobAttributes,"preproc-id")){
		    
		    Element preProcEle = DOMHelper.getElementByName(jobAttributes,"preproc-id");
		    String preProc = preProcEle.getAttribute("value");
		    preProcId = Integer.parseInt(preProc);
		    if (preProcId != null && preProcId > 0) {
			Processor p = Processors.get(preProcId);
			if (p != null && p.getFilePath() != null) {
			    job.setPreProcessor(p);
			}
		    }
		}
		
		log.info("postProcId about to be set");

		Integer postProcId = null;
		
		if (DOMHelper.hasElement(jobAttributes,"postproc-id")){
		    Element postProcEle = DOMHelper.getElementByName(jobAttributes,"postproc-id");
		    String postProc = postProcEle.getAttribute("value");
		    postProcId = Integer.parseInt(postProc);
		    if (postProcId != null && postProcId > 0) {
			Processor p = Processors.get(postProcId);
			if (p != null && p.getFilePath() != null) {
			    job.setPostProcessor(p);
			}
		    }
		}
		

		log.info("queueId about to be set");

		Element queueIdEle = DOMHelper.getElementByName(jobAttributes,"queue-id");
		int queueId = Integer.parseInt(queueIdEle.getAttribute("value"));
		Queue queue = Queues.get(queueId);
		job.setQueue(queue);
		job.setPrimarySpace(spaceId);
		
		String rootName=Spaces.getName(spaceId);

		log.info("clock and mem limits about to be set");

		Element wallclockEle = DOMHelper.getElementByName(jobAttributes,"wallclock-timeout");
		log.info("wallclock-timeout: " + wallclockEle.getAttribute("value"));
		int wallclock = Integer.parseInt(wallclockEle.getAttribute("value"));

		Element cpuTimeoutEle = DOMHelper.getElementByName(jobAttributes, "cpu-timeout");
		log.info("cpu-timeout: " + cpuTimeoutEle.getAttribute("value"));
		int cpuTimeout = Integer.parseInt(cpuTimeoutEle.getAttribute("value"));

		Element memLimitEle = DOMHelper.getElementByName(jobAttributes, "mem-limit");
		log.info("mem-limit: " + memLimitEle.getAttribute("value"));
		double memLimit = Double.parseDouble(memLimitEle.getAttribute("value"));
		
		long memoryLimit=Util.gigabytesToBytes(memLimit);
		memoryLimit = (memoryLimit <=0) ? R.DEFAULT_PAIR_VMEM : memoryLimit;

		log.info("nodelist about to be set");
				
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
				jobPair.setCpuTimeout(cpuTimeout);
				jobPair.setWallclockTimeout(wallclock);
				jobPair.setMaxMemory(memoryLimit);
				
			Benchmark b = Benchmarks.get(benchmarkId);
			if (!Permissions.canUserSeeBench(benchmarkId, userId)){
			    errorMessage = "You do not have permission to see benchmark " + benchmarkId;
			    return -1;
			}
			jobPair.setBench(b);
				
			Solver s = Solvers.getSolverByConfig(configId, false);
			if (!Permissions.canUserSeeSolver(s.getId(), userId)){
			    errorMessage = "You do not have permission to see the solver " + s.getId();
			    return -1;
			}
				
			jobPair.setSolver(s);
			jobPair.setConfiguration(Solvers.getConfiguration(configId));
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
