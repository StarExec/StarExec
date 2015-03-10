package org.starexec.util;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.apache.log4j.Logger;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.starexec.constants.R;
import org.starexec.util.Util;
import org.starexec.data.database.Jobs;
import org.starexec.data.database.Pipelines;
import org.starexec.data.to.Job;
import org.starexec.data.to.JobPair;
import org.starexec.data.to.Processor;
import org.starexec.data.to.Status;
import org.starexec.data.to.pipelines.PipelineDependency;
import org.starexec.data.to.pipelines.PipelineDependency.PipelineInputType;
import org.starexec.data.to.pipelines.PipelineStage;
import org.starexec.data.to.pipelines.SolverPipeline;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.starexec.servlets.Download;


/**
 *  JobToXMLer deals with job xml creation
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
	//TODO : attributes are being sorted alphabetically, is there a way to keep order of insertion instead?
    	
	log.info("Start generating XML for Job = " +job.getId());	
			
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
		
		File file = new File(R.STAREXEC_ROOT, job.getName().replaceAll("\\s+", "") +".xml");
		
		
		StreamResult result = new StreamResult(file);
		transformer.transform(source, result);

		return file;
		
	}
    //TODO: Handle noop
    public Element getPipelineElement(SolverPipeline pipeline) {
    	Element pipeElement= doc.createElement("SolverPipeline");
    	for (PipelineStage stage : pipeline.getStages()) {
    		Element stageElement= doc.createElement("PipelineStage");
    		Attr configId=doc.createAttribute("config");
    		Attr keepOutput=doc.createAttribute("keepoutput");
    		Attr isPrimary = doc.createAttribute("primary");
    		
			configId.setValue(Integer.toString(stage.getConfigId()));
			keepOutput.setValue(Boolean.toString(stage.doKeepOutput()));
			isPrimary.setValue(Boolean.toString(stage.getId()==pipeline.getPrimaryStageId()));
			
			for (PipelineDependency dep : stage.getDependencies()) {
				if (dep.getType()==PipelineInputType.ARTIFACT) {
					Element depElement=doc.createElement("stageDependency");
					Attr stageAttr = doc.createAttribute("stage");
					stageAttr.setValue(String.valueOf(dep.getDependencyId()));
					depElement.setAttributeNode(stageAttr);
					stageElement.appendChild(depElement);
				} else if (dep.getType()==PipelineInputType.BENCHMARK) {
					Element depElement=doc.createElement("benchmarkDependency");
					Attr stageAttr = doc.createAttribute("input");
					stageAttr.setValue(String.valueOf(dep.getDependencyId()));
					depElement.setAttributeNode(stageAttr);
					stageElement.appendChild(depElement);
				}
			}
			
			
		    stageElement.setAttributeNode(configId);
    		stageElement.setAttributeNode(keepOutput);
    		stageElement.setAttributeNode(isPrimary);
    		pipeElement.appendChild(stageElement);
    	}
    	
    	return pipeElement;
    }
	
    /**
     *  Will generate xml
     *  @author Julio Cervantes
     *  @param job The job for which we want an xml representation.
     *  @param userId the id of the user making the request
     *  @return jobsElement for the xml file to represent job hierarchy of input job	 *  
     */	
    public Element generateJobsXML(Job job, int userId){		
	log.info("Generating Jobs XML " + job.getId());
	Element jobsElement=null;

	jobsElement = doc.createElementNS(Util.url("public/batchJobSchema.xsd"), "tns:Jobs");
	jobsElement.setAttribute("xmlns:xsi", "http://www.w3.org/2001/XMLSchema-instance");
	jobsElement.setAttribute("xsi:schemaLocation", 
					   Util.url("public/batchJobSchema.xsd batchJobSchema.xsd"));
	
	List<SolverPipeline> neededPipes = Pipelines.getPipelinesByJob(job.getId());
	
	//add all needed pipelines to the XML
	for (SolverPipeline pipe : neededPipes) {
		jobsElement.appendChild(getPipelineElement(pipe));
	}
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
    
    //TODO: Put stage attributes in here where they exist
    public Element generateJobXML(Job job, int userId){		
	log.info("Generating Job XML for job " + job.getId());
		
		Element jobElement = doc.createElement("Job");
		
		Element attrsElement = doc.createElement("JobAttributes");


		
		Attr name = doc.createAttribute("name");
		name.setValue(job.getName());
		jobElement.setAttributeNode(name);


		
		Element descriptionElement = doc.createElement("description");

		// Description attribute : description
		Attr description = doc.createAttribute("value");
		description.setValue(job.getDescription());
		descriptionElement.setAttributeNode(description);

		attrsElement.appendChild(descriptionElement);
		

		Element queueIdElement = doc.createElement("queue-id");

		//Id of queue : queue-id
		Attr queueID = doc.createAttribute("value");
		queueID.setValue(Integer.toString(job.getQueue().getId()));
		queueIdElement.setAttributeNode(queueID);

		attrsElement.appendChild(queueIdElement);
		

		Element startPausedElement = doc.createElement("start-paused");

		// Should start paused attribute (default is false) : start-paused
		Attr startPaused = doc.createAttribute("value");
		startPaused.setValue(Boolean.toString(false));
		startPausedElement.setAttributeNode(startPaused);

		attrsElement.appendChild(startPausedElement);
		
		//Preprocessor ID : preproc-id
		Processor pre = job.getPreProcessor();
		if(pre != null){

		    Element preprocIdElement = doc.createElement("preproc-id");

		    Attr preProcID = doc.createAttribute("value");
		    preProcID.setValue(Integer.toString(pre.getId()));
		    preprocIdElement.setAttributeNode(preProcID);

		    attrsElement.appendChild(preprocIdElement);
		}
		//Postprocessor ID : postproc-id
		Processor post = job.getPostProcessor();
		if(post != null){

		    Element postprocIdElement = doc.createElement("postproc-id");

		    Attr postProcID = doc.createAttribute("value");
		    postProcID.setValue(Integer.toString(post.getId()));
		    postprocIdElement.setAttributeNode(postProcID);

		    attrsElement.appendChild(postprocIdElement);
		}
		
		//CPU timeout (seconds) : cpu-timeout

		Element cpuTimeoutElement = doc.createElement("cpu-timeout");

		Attr cpuTimeout = doc.createAttribute("value");
		cpuTimeout.setValue(Integer.toString(job.getCpuTimeout()));
		cpuTimeoutElement.setAttributeNode(cpuTimeout);

		attrsElement.appendChild(cpuTimeoutElement);
		
		//Wall Clock timeout (seconds) : wallclock-timeout

		Element wallClockTimeoutElement = doc.createElement("wallclock-timeout");

		Attr wallClockTimeout = doc.createAttribute("value");
                wallClockTimeout.setValue(Integer.toString(job.getWallclockTimeout()));
		wallClockTimeoutElement.setAttributeNode(wallClockTimeout);

		attrsElement.appendChild(wallClockTimeoutElement);
		
		//Memory Limit (Gigabytes) : mem-limit (defaulting to 1)

		Element memLimitElement = doc.createElement("mem-limit");

		Attr memLimit = doc.createAttribute("value");
		memLimit.setValue(Double.toString(Util.bytesToGigabytes(job.getMaxMemory())));
		memLimitElement.setAttributeNode(memLimit);

		attrsElement.appendChild(memLimitElement);
		
		//add job attributes element
		jobElement.appendChild(attrsElement);

		List<JobPair> pairs= Jobs.getPairsSimple(job.getId());
		
		
		
		for (JobPair jobpair:pairs){
			// if this job pair doesn't reference a pipeline
			Element jp=null;
			if (jobpair.getPipeline()==null) {
				jp = doc.createElement("JobPair");
				
				Attr configID = doc.createAttribute("config-id");
				Attr configName = doc.createAttribute("config-name");
				configID.setValue(Integer.toString(jobpair.getPrimaryConfiguration().getId()));
				configName.setValue(jobpair.getPrimaryConfiguration().getName());

				Attr solverId=doc.createAttribute("solver-id");
				Attr solverName=doc.createAttribute("solver-name");

				solverId.setValue(Integer.toString(jobpair.getPrimarySolver().getId()));
				solverName.setValue(jobpair.getPrimarySolver().getName());

				jp.setAttributeNode(solverName);
				jp.setAttributeNode(solverId);

				jp.setAttributeNode(configName);
				jp.setAttributeNode(configID);
				
			} else {
				//this job pair references a pipeline
				jp = doc.createElement("JobLine");
				Attr pipeName=doc.createAttribute("pipe-name");
				pipeName.setValue(jobpair.getPipeline().getName());
				jp.setAttributeNode(pipeName);
			}
			
			Attr benchID = doc.createAttribute("bench-id");
			Attr benchName = doc.createAttribute("bench-name");
			benchID.setValue(Integer.toString(jobpair.getBench().getId()));
			benchName.setValue(jobpair.getBench().getName());
			Attr spaceId=doc.createAttribute("job-space-id");
			Attr spacePath=doc.createAttribute("job-space-path");
			spaceId.setValue(Integer.toString(jobpair.getSpace().getId()));
			spacePath.setValue(jobpair.getPath());
			
			
			jp.setAttributeNode(spaceId);
			jp.setAttributeNode(spacePath);
			jp.setAttributeNode(benchID);
			jp.setAttributeNode(benchName);
			
			jobElement.appendChild(jp);

		}
		
		return jobElement;
	}

}
