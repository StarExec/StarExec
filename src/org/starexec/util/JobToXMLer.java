package org.starexec.util;

import org.starexec.constants.R;
import org.starexec.data.database.Jobs;
import org.starexec.data.database.Pipelines;
import org.starexec.data.to.Job;
import org.starexec.data.to.JobPair;
import org.starexec.data.to.pipelines.PipelineDependency;
import org.starexec.data.to.pipelines.PipelineDependency.PipelineInputType;
import org.starexec.data.to.pipelines.PipelineStage;
import org.starexec.data.to.pipelines.SolverPipeline;
import org.starexec.data.to.pipelines.StageAttributes;
import org.starexec.logger.StarLogger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.io.File;
import java.util.HashMap;
import java.util.List;


/**
 *  JobToXMLer deals with job xml creation
 *  @author Julio Cervantes
 */

public class JobToXMLer {
    private static final StarLogger log = StarLogger.getLogger(JobToXMLer.class);
	
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

		doc = XMLUtil.generateNewDocument();
		Element rootSpace = generateJobsXML(job, userId);
		doc.appendChild(rootSpace);	
		return XMLUtil.writeDocumentToFile(job.getName()+".xml", doc);
		
	}
    
    
    /**
     * Given a StageAttributes object, creates an XML element to represent it.
     * @param attrs The attributes to convert to XML
     * @return The XML element
     */
    public Element getStageAttributesElement(StageAttributes attrs) {
    	Element stageAttrs=doc.createElement("StageAttributes");
    	
    	Element stageNumberElement=doc.createElement("stage-num");
    	stageNumberElement.setAttribute("value", Integer.toString(attrs.getStageNumber()));
    	stageAttrs.appendChild(stageNumberElement);
    	
		Element cpuTimeoutElement = doc.createElement("cpu-timeout");

    	cpuTimeoutElement.setAttribute("value", Integer.toString(attrs.getCpuTimeout()));

		stageAttrs.appendChild(cpuTimeoutElement);
		

		Element wallClockTimeoutElement = doc.createElement("wallclock-timeout");

    	wallClockTimeoutElement.setAttribute("value", Integer.toString(attrs.getWallclockTimeout()));

		stageAttrs.appendChild(wallClockTimeoutElement);
		

		Element memLimitElement = doc.createElement("mem-limit");

    	memLimitElement.setAttribute("value", Double.toString(Util.bytesToGigabytes(attrs.getMaxMemory())));
    	
    	stageAttrs.appendChild(memLimitElement);
    	
    	
    	//all of the following attributes are optional, and so they are included only if they are not null.
    	if (attrs.getSpaceId()!=null && attrs.getSpaceId()>0) {
    		Element spaceIdElement = doc.createElement("space-id");

        	spaceIdElement.setAttribute("value", attrs.getSpaceId().toString());
        	
        	stageAttrs.appendChild(spaceIdElement);
    	}
    	if (attrs.getBenchSuffix()!=null) {
    		Element benchSuffixElement=doc.createElement("bench-suffix");
        	benchSuffixElement.setAttribute("value", attrs.getBenchSuffix());
    		stageAttrs.appendChild(benchSuffixElement);
    	}
    	
    	
    	if (attrs.getPostProcessor()!=null) {
    		Element postProcessorElement = doc.createElement("postproc-id");
        	postProcessorElement.setAttribute("value", String.valueOf(attrs.getPostProcessor().getId()));
        	
        	stageAttrs.appendChild(postProcessorElement);
    	}
    	
    	if (attrs.getPreProcessor()!=null) {
    		Element preProcessorElement = doc.createElement("preproc-id");

        	preProcessorElement.setAttribute("value", String.valueOf(attrs.getPreProcessor().getId()));
        	
        	stageAttrs.appendChild(preProcessorElement);
    	}
    	
    	
    	
    	return stageAttrs;
    	
    }
    
    public Element getDependencyElement(PipelineDependency dep) {
    	Element depElement = null;
    	if (dep.getType()==PipelineInputType.ARTIFACT) {
			depElement=doc.createElement("StageDependency");
			depElement.setAttribute("stage", String.valueOf(dep.getDependencyId()));
		} else if (dep.getType()==PipelineInputType.BENCHMARK) {
			depElement=doc.createElement("BenchmarkDependency");
			depElement.setAttribute("input", String.valueOf(dep.getDependencyId()));
		}
    	return depElement;
    }
    
    public Element getStageElement(SolverPipeline pipeline, PipelineStage stage) {
    	if (stage.isNoOp()) {
			return doc.createElement("noop");
		} else {
			Element stageElement= doc.createElement("PipelineStage");
			
			for (PipelineDependency dep : stage.getDependencies()) {
				stageElement.appendChild(getDependencyElement(dep));
			}
			stageElement.setAttribute("config-id", Integer.toString(stage.getConfigId()));
			stageElement.setAttribute("primary", Boolean.toString(stage.getId()==pipeline.getPrimaryStageNumber()));
			return stageElement;
		}
    }
    
    public Element getPipelineElement(SolverPipeline pipeline) {
    	Element pipeElement= doc.createElement("SolverPipeline");
    	pipeElement.setAttribute("name", pipeline.getName());

    	for (PipelineStage stage : pipeline.getStages()) {
    		pipeElement.appendChild(getStageElement(pipeline, stage));
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

	jobsElement = doc.createElementNS(Util.url(R.JOB_XML_SCHEMA_RELATIVE_LOC), "tns:Jobs");
	jobsElement.setAttribute("xmlns:xsi", "http://www.w3.org/2001/XMLSchema-instance");
	jobsElement.setAttribute("xsi:schemaLocation", 
					   Util.url("public/batchJobSchema.xsd batchJobSchema.xsd"));
	
	List<SolverPipeline> neededPipes = Pipelines.getPipelinesByJob(job.getId());
	log.debug("going to add this many pipelines to the xml document = "+ neededPipes.size());
	//add all needed pipelines to the XML
	for (SolverPipeline pipe : neededPipes) {
		jobsElement.appendChild(getPipelineElement(pipe));
	}
	Element rootJobElement = generateJobXML(job,neededPipes.size()!=0);
	jobsElement.appendChild(rootJobElement);
		
	
		
	return jobsElement;
    }
	
	/**
	 *  Generates the XML for an individual job.
	 *  @author Julio Cervantes
	 *  @param job The job for which we want an xml representation.
	 *  @param containsPipelines True if this job has pipelines and false otherwise
	 *  @return jobElement for xml file to represent job pair info  of input job 
	 */	
    
    
    public Element generateJobXML(Job job, boolean containsPipelines){
	log.info("Generating Job XML for job " + job.getId());
		
		Element jobElement = doc.createElement("Job");
		
		
		
		Element attrsElement = doc.createElement("JobAttributes");


		jobElement.setAttribute("name", job.getName());

		Element descriptionElement = doc.createElement("description");

		// Description attribute : description
		descriptionElement.setAttribute("value", job.getDescription());
		attrsElement.appendChild(descriptionElement);
		


		//Id of queue : queue-id
		Element queueIdElement = doc.createElement("queue-id");
		queueIdElement.setAttribute("value", job.getQueue()!=null ? Integer.toString(job.getQueue().getId()) : "-1");
		attrsElement.appendChild(queueIdElement);
		

		Element startPausedElement = doc.createElement("start-paused");

		// Should start paused attribute (default is false) : start-paused
		startPausedElement.setAttribute("value", "false");
		attrsElement.appendChild(startPausedElement);
		
		
		//CPU timeout (seconds) : cpu-timeout

		Element cpuTimeoutElement = doc.createElement("cpu-timeout");
		cpuTimeoutElement.setAttribute("value", Integer.toString(job.getCpuTimeout()));

		attrsElement.appendChild(cpuTimeoutElement);
		
		//Wall Clock timeout (seconds) : wallclock-timeout

		Element wallClockTimeoutElement = doc.createElement("wallclock-timeout");

		wallClockTimeoutElement.setAttribute("value", Integer.toString(job.getWallclockTimeout()));

		attrsElement.appendChild(wallClockTimeoutElement);
		
		//Memory Limit (Gigabytes) : mem-limit (defaulting to 1)

		Element memLimitElement = doc.createElement("mem-limit");

		memLimitElement.setAttribute("value", Double.toString(Util.bytesToGigabytes(job.getMaxMemory())));

		attrsElement.appendChild(memLimitElement);
		
		//add job attributes element
		jobElement.appendChild(attrsElement);

		if (containsPipelines) {
			for (StageAttributes attrs : job.getStageAttributes()) {
				jobElement.appendChild(getStageAttributesElement(attrs));
			}
		} else {
			// if we have no pipelines, then we can add the pre processor and post processor elements to the job attributes
			for (StageAttributes attrs : job.getStageAttributes()) {
				if (attrs.getPostProcessor()!=null) {
		    		Element postProcessorElement = doc.createElement("postproc-id");
		    		postProcessorElement.setAttribute("value", String.valueOf(attrs.getPostProcessor().getId()));
		        	
		        	attrsElement.appendChild(postProcessorElement);
		    	}
		    	if (attrs.getPreProcessor()!=null) {
		    		Element preProcessorElement = doc.createElement("preproc-id");
		    		preProcessorElement.setAttribute("value", String.valueOf(attrs.getPreProcessor().getId()));
		        	attrsElement.appendChild(preProcessorElement);
		    	}
				
			}
		}

		List<JobPair> pairs= Jobs.getPairsSimple(job.getId());
		
		HashMap<Integer,List<Integer>> benchInputs=Jobs.getAllBenchmarkInputsForJob(job.getId());
		
		for (JobPair jobpair:pairs){
			// if this job pair doesn't reference a pipeline
			Element jp=null;
			if (jobpair.getPipeline()==null) {
				jp = doc.createElement("JobPair");
				
				jp.setAttribute("config-id", Integer.toString(jobpair.getPrimaryConfiguration().getId()));
				jp.setAttribute("config-name", jobpair.getPrimaryConfiguration().getName());
				jp.setAttribute("solver-id", Integer.toString(jobpair.getPrimarySolver().getId()));
				jp.setAttribute("solver-name", jobpair.getPrimarySolver().getName());
			} else {
				//this job pair references a pipeline
				jp = doc.createElement("JobLine");
				jp.setAttribute("pipe-name", jobpair.getPipeline().getName());
				
				if (benchInputs.containsKey(jobpair.getId())) {
					List<Integer> inputs=benchInputs.get(jobpair.getId());
					
					for (Integer benchId : inputs) {
						Element input=doc.createElement("BenchmarkInput");
						input.setAttribute("bench-id", benchId.toString());
						jp.appendChild(input);
					}
					
				}
				
			}
			jp.setAttribute("bench-id", Integer.toString(jobpair.getBench().getId()));
			jp.setAttribute("bench-name", jobpair.getBench().getName());
			jp.setAttribute("job-space-id", Integer.toString(jobpair.getSpace().getId()));
			jp.setAttribute("job-space-path", jobpair.getPath());
			jobElement.appendChild(jp);

		}
		
		return jobElement;
	}

}
