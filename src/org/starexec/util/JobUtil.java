package org.starexec.util;

import java.io.File;
import java.io.IOException;
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
import org.starexec.data.database.Jobs;
import org.starexec.data.database.Processors;
import org.starexec.data.database.Queues;
import org.starexec.data.database.Solvers;
import org.starexec.data.database.Spaces;
import org.starexec.data.to.Job;
import org.starexec.data.to.JobPair;
import org.starexec.data.to.Processor;
import org.starexec.data.to.Queue;
import org.starexec.jobs.JobManager;
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
	 * Creates jobs from the xml file.
	 * @author Tim Smith
	 * @param file the xml file we wish to create jobs from
	 * @param userId the userId of the user making the request
	 * @param spaceId the space that will serve as the root for jobs to run under
	 * @return Boolean true if the jobs are successfully created
	 * @throws SAXException
	 * @throws ParserConfigurationException
	 * @throws IOException
	 */
	public Boolean createJobsFromFile(File file, int userId, Integer spaceId) throws Exception {
		if (!validateAgainstSchema(file)){
			log.warn("File from User " + userId + " is not Schema valid.");
			return false;
		}
		
		DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder docBuilder = docBuilderFactory.newDocumentBuilder();
        Document doc = docBuilder.parse(file);
        Element jobsElement = doc.getDocumentElement();
		NodeList listOfJobElements = jobsElement.getChildNodes();
		
        //Check Jobs and Job Pairs
        NodeList listOfJobs = doc.getElementsByTagName("Job");
		log.info("# of Jobs = " + listOfJobs.getLength());
        NodeList listOfJobPairs = doc.getElementsByTagName("JobPair");
		log.info("# of JobPairs = " + listOfJobPairs.getLength());
		
		String name = "";//name variable to check
		
		// Make sure jobs are named
		for (int i = 0; i < listOfJobs.getLength(); i++){
			Node jobNode = listOfJobs.item(i);
			if (jobNode.getNodeType() == Node.ELEMENT_NODE){
				Element jobElement = (Element)jobNode;
				name = jobElement.getAttribute("name");
				if (name == null) {
					log.debug("Name not found");
					errorMessage = "Job elements must include a 'name' attribute.";
					return false;
				}
				log.debug("Space Name = " + name);
				if (name.length()<2){
					log.debug("Name was not long enough");
					errorMessage = name + "is not a valid name.  It must have two characters.";
					return false;
				}
				
			}
			else{
				log.warn("Job Node should be an element, but isn't");
			}
		}
		
		// TODO: Verify the user has access to the benchmarks and solvers each of the JobPair elements

		
		this.jobCreationSuccess = true;
		
		for (int i = 0; i < listOfJobElements.getLength(); i++){
			Node jobNode = listOfJobElements.item(i);
			if (jobNode.getNodeType() == Node.ELEMENT_NODE){
				Element jobElement = (Element)jobNode;
				
				jobCreationSuccess = jobCreationSuccess && createJobFromElement(userId, spaceId, jobElement);
			}
		}
		
		return true;
	}
	
	private boolean createJobFromElement(int userId, Integer spaceId,
			Element jobElement) {
		// TODO Create a job object with a list of job pairs and
		// 			use Jobs.add() to put the job on the space
		
		try {
		errorMessage = "Name, ";
		if (Spaces.notUniquePrimitiveName(jobElement.getAttribute("name"), spaceId, 3)) {
			errorMessage = "The job should have a unique name in the space.";
			return false;
		}
		
		errorMessage += "Name, ";
		Job job = new Job();
		job.setName(jobElement.getAttribute("name"));
		
		errorMessage += "Proc, ";
		Integer preProcId = null;
		String preProc = jobElement.getAttribute("preproc-id");
		if (preProc != null && !preProc.equals("")){
			preProcId = Integer.parseInt(preProc);
			if (preProcId != null && preProcId > 0) job.setPreProcessor(Processors.get(preProcId));
		}
		
		Integer postProcId = null;
		String postProc = jobElement.getAttribute("postproc-id");
		if (postProc != null && !postProc.equals("")){
			postProcId = Integer.parseInt(postProc);
			if (postProcId != null && postProcId > 0) job.setPostProcessor(Processors.get(postProcId));
		}
		
		errorMessage += "Queue, ";
		int queueId = Integer.parseInt(jobElement.getAttribute("queue-id"));
		Queue queue = Queues.get(queueId);
		job.setQueue(queue);
//		Job job = JobManager.setupJob(
//				userId, 
//				jobElement.getAttribute("name"),
//				"",
//				preProcId, 
//				postProcId, 
//				Integer.parseInt(jobElement.getAttribute("queue-id"))
//			);
		job.setPrimarySpace(spaceId);
		
		errorMessage += "Timeouts, ";
		
		int wallclock = Integer.parseInt(jobElement.getAttribute("wallclock-timeout"));
		int cpuTimeout = Integer.parseInt(jobElement.getAttribute("cpu-timeout"));
		double memLimit = Double.parseDouble(jobElement.getAttribute("mem-limit"));
		
		long memoryLimit=Util.gigabytesToBytes(memLimit);
		memoryLimit = (memoryLimit <=0) ? R.MAX_PAIR_VMEM : memoryLimit;
		
		errorMessage += "Starting job pairs, ";
		
		NodeList jobPairs = jobElement.getChildNodes();
		for (int i = 0; i < jobPairs.getLength(); i++) {
			Node jobPairNode = jobPairs.item(i);
			if (jobPairNode.getNodeType() == Node.ELEMENT_NODE){
				Element jobPairElement = (Element)jobPairNode;
				
				JobPair jobPair = new JobPair();
				int benchmarkId = Integer.parseInt(jobPairElement.getAttribute("benchmark-id"));
				int solverId = Integer.parseInt(jobPairElement.getAttribute("solver-id"));
				int configId = Integer.parseInt(jobPairElement.getAttribute("configuration-id"));
				
				jobPair.setCpuTimeout(cpuTimeout);
				jobPair.setWallclockTimeout(wallclock);
				jobPair.setMaxMemory(memoryLimit);
				
				jobPair.setBench(Benchmarks.get(benchmarkId));
				jobPair.setSolver(Solvers.get(solverId));
				jobPair.setConfiguration(Solvers.getConfiguration(configId));
				
				job.addJobPair(jobPair);
			}
		}
		
		errorMessage += "Going to submit job. ";
		
		if (job.getJobPairs().size() == 0) {
			// No pairs in the job means something is wrong; error out
			errorMessage = "Error: no job pairs created for the job. Could not proceed with job submission.";
			return false;
		}
		
		List<Job> jobs = new LinkedList<Job>();
		jobs.add(job);
		
		errorMessage += "Submitting...";
		JobManager.submitJobs(jobs, queue, jobPairs.getLength());
		return true;
		
		}
		catch (Exception e) {
			return false;
		}
	}

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
