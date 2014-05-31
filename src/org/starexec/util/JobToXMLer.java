package org.starexec.util;

import java.io.File;
import java.util.logging.Logger;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.starexec.constants.R;
import org.starexec.data.database.Jobs;
import org.starexec.data.to.Job;
import org.starexec.data.to.JobPair;
import org.starexec.data.to.Space;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;


/**
 *  JobToXMLer only creates an xml given a job
 *  @author Julio Cervantes
 */

public class JobToXMLer {
	private static final Logger log = Logger.getLogger(JobToXMLer.class);
	
	private Document doc = null;
	
	/**
	 *  Will generate an xml file for a job.
	 *  @author Julio Cervantes
	 *  @param job The job for which we want an xml representation.
	 *  @param userId the id of the user making the request
	 *  @return xml file to represent job
	 *  @throws Exception   
	 */	
    public File generateXMLfile(Job job, int userId) throws Exception{
    	//TODO : should be able to handle specific jobs?  Currently only handles all jobs from a space.
		log.debug("Generating XML for Jobs from Space = " +space.getId());			
		DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
		
		DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
		
		doc = docBuilder.newDocument();
		Element rootSpace = generateJobsXML(job, userId);
		doc.appendChild(rootSpace);
		
		// write the content into xml file
		TransformerFactory transformerFactory = TransformerFactory.newInstance();
		Transformer transformer = transformerFactory.newTransformer();
		transformer.setOutputProperty(OutputKeys.INDENT, "yes");
		DOMSource source = new DOMSource(doc);
		
		File file = new File(R.STAREXEC_ROOT, space.getName() +"-Jobs.xml");
		
		StreamResult result = new StreamResult(file);
		transformer.transform(source, result);
		
		return file;
	}
	
    /**
     *  Will generate xml
     *  @author Julio Cervantes
     *  @param job The job for which we want an xml representation.
     *  @param userId the id of the user making the request
     *  @return jobsElement for the xml file to represent job hierarchy of input job	 *  
     */	
    public Element generateJobsXML(Job job, int userId){		
	log.debug("Generating Jobs XML " + space.getId());
	Element jobsElement=null;

	jobsElement = doc.createElementNS(Util.url("public/batchJobSchema.xsd"), "tns:Jobs");
	jobsElement.setAttribute("xmlns:xsi", "http://www.w3.org/2001/XMLSchema-instance");
		
	jobsElement.setAttribute("xsi:schemaLocation", 
					   Util.url("public/batchJobSchema.xsd batchJobSchema.xsd"));
	
	Element rootJobElement = generateJobXML(job, userId);
	jobsElement.appendChild(rootJobElement);
		
	
		
	return jobsElement;
    }
	
	/**
	 *  Generates the XML for an individual job.
	 *  @author Julio Cervantes
	 *  @param job The job for which we want an xml representation.
	 *  @param userId the id of the user making the request
	 *  @return jobElement for xml file to represent job pair info  of input job 
	 */	
    public Element generateJobXML(Job job, int userId){		
		log.debug("Generating Job XML for job " + job.getId());
		
		Element jobElement = doc.createElement("Job");
		

	
		Attr id = doc.createAttribute("id");
		id.setValue(Integer.toString(job.getId()));
		jobElement.setAttributeNode(id);		
		
		Attr name = doc.createAttribute("name");
		name.setValue(job.getName());
		jobElement.setAttributeNode(name);
		
		// New attributes to job XML elements
		// @author Tim Smith
		
		// Description attribute : description
		Attr description = doc.createAttribute("description");
		description.setValue(job.getDescription());
		jobElement.setAttributeNode(description);
		
		//Id of queue : queue-id
		Attr queueID = doc.createAttribute("queue-id");
		queueID.setValue(Integer.toString(job.getQueue().getId()));
		jobElement.setAttributeNode(queueID);
		
		// Should start paused attribute (default is false) : start-paused
		Attr startPaused = doc.createAttribute("start-paused");
		startPaused.setValue(Boolean.toString(false));
		jobElement.setAttributeNode(startPaused);
		
		//Preprocessor ID : preproc-id
		Attr preProcID = doc.createAttribute("preproc-id");
		preProcID.setValue(Integer.toString(job.getPreProcessor().getId()));
		jobElement.setAttributeNode(preProcID);
		
		//Postprocessor ID : postproc-id
		Attr postProcID = doc.createAttribute("postproc-id");
		postProcID.setValue(Integer.toString(job.getPostProcessor().getId()));
		jobElement.setAttributeNode(postProcID);
		
		//CPU timeout (seconds) : cpu-timeout
		Attr cpuTimeout = doc.createAttribute("cpu-timeout");
		cpuTimeout.setValue(Integer.toString(Jobs.getCpuTimeout(job.getId())));
		jobElement.setAttributeNode(cpuTimeout);
		
		//Wall Clock timeout (seconds) : wallclock-timeout
		Attr wallClockTimeout = doc.createAttribute("wallclock-timeout");
		wallClockTimeout.setValue(Integer.toString(Jobs.getWallclockTimeout(job.getId())));
		jobElement.setAttributeNode(wallClockTimeout);
		
		//Memory Limit (Gigabytes) : mem-limit
		Attr memLimit = doc.createAttribute("mem-limit");
		memLimit.setValue(Long.toString(Jobs.getMaximumMemory(job.getId())));
		jobElement.setAttributeNode(memLimit);
		
		// -------------------------------------------------
		
		for (JobPair jobpair:job.getJobPairs()){
			Element jobPair = doc.createElement("JobPair");	
			jobPair.setAttribute("bench-id", Integer.toString(jobpair.getBench().getId()));
			jobPair.setAttribute("config-id", Integer.toString(jobpair.getConfiguration().getId()));
			jobElement.appendChild(jobPair);
		}
		
		return jobElement;
	}
	


	
	
}
