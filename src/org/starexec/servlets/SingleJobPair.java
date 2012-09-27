package org.starexec.servlets;

import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import org.starexec.constants.R;
import org.starexec.data.database.Benchmarks;
import org.starexec.data.database.Solvers;
import org.starexec.data.to.Benchmark;
import org.starexec.data.to.Configuration;
import org.starexec.data.to.Job;
import org.starexec.data.to.Solver;
import org.starexec.jobs.JobManager;
import org.starexec.util.SessionUtil;
import org.starexec.util.Util;
import org.starexec.util.Validator;

/**
 * Supports the writing of a new benchmark by a public user not logged in.  Soon, will also execute jobs.
 * 
 * @author Benton McCune
 */
@SuppressWarnings("serial")
public class SingleJobPair extends HttpServlet {
	private static final Logger log = Logger.getLogger(SingleJobPair.class);	
	private DateFormat shortDate = new SimpleDateFormat(R.PATH_DATE_FORMAT);
    // Param constants to use to process the form
    private static final String BENCHMARK_CONTENTS = "benchmarkContents";
    //private static final Integer PUBLIC_USER_ID = R.PUBLIC_USER_ID;
    //private static final Integer PUBLIC_USER_SPACE = R.PUBLIC_SPACE_ID;
    //private static final String SOLVER_ID = "solverId";
    private static final String SOLVER_ID = "publicSolver";
    //private static final String CONFIG_CONTENTS = "saveConfigContents";
    //private static final String CONFIG_NAME = "saveConfigName";    		
    
    //Constants for Job parameters
    private Integer queueId = 1;
    private String jobDescription = "This is a public job.";
    private Integer preProcessorId = 0;
    private Integer postProcessorId = 0;
    private Integer cpuLimit = R.PUBLIC_CPU_LIMIT;
    private Integer clockTimeout = R.PUBLIC_CLOCK_TIMEOUT;
    
    
    
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
    	response.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED, "Wrong type of request.");
    }
    
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
    	//int userId = SessionUtil.getUserId(request);
    	try {	
			// Parameter validation
			if(!this.isValidRequest(request)) {
				response.sendError(HttpServletResponse.SC_BAD_REQUEST, "The save configuration request was malformed.");
				return;
			} 
			
			// Permissions check; ensure the user owns the solver to which they are saving
			/*if(Solvers.get(Integer.parseInt(request.getParameter(SOLVER_ID))).getUserId() != userId){
				response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Only owners of a solver may save configurations to it.");
				return;
			}
			*/
			// Process the configuration file and write it to the parent solver's /bin directory, then update the solver's disk_size attribute
			int jobId = handleBenchmark(request);
			
			// Redirect user based on how the configuration handling went
			if(jobId<0) {
				response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Failed to save new benchmark.");	
			} else {
				log.debug("success!!");
				response.sendRedirect("/starexec/public/jobs/job.jsp?id=" + jobId);	
			}									
    	} catch (Exception e) {
    		response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
    		log.error(e.getMessage(), e);
    	}	
	}
    
    
    /**
     * Handles writing a new benchmark file to disk
     * 
     * @param request the object containing the new benchmark contents
     * @return true if successful
     * @author Benton McCune
     */
	public int handleBenchmark(HttpServletRequest request) {
		try {
			
			// Set up a new configuration object with the submitted information
			Solver solver = Solvers.get(Integer.parseInt(request.getParameter(SOLVER_ID)));
	
			//Solver solver = Solvers.get(Integer.parseInt((String)request.getParameter(SOLVER_ID)));
			Benchmark bench = new Benchmark();
			bench.setName("public"+shortDate.format(new Date()));
			log.debug("Public user id = " + R.PUBLIC_USER_ID);
			bench.setUserId(R.PUBLIC_USER_ID);
			log.debug("bench user id is " + bench.getUserId());
			log.debug("Solver is " + solver.getName());
			//bench.setDownloadable(false);
			
			//Configuration newConfig = new Configuration();
			//newConfig.setName(request.getParameter(CONFIG_NAME));
			//newConfig.setDescription(request.getParameter(CONFIG_DESC));
			//newConfig.setSolverId(solver.getId());
			String benchmarkContents = request.getParameter(BENCHMARK_CONTENTS);
			
			log.debug("bench contents = " + benchmarkContents);
			
			
			// Build a path to the newly written benchmark
		  
		    File newBenchmarkFile = new File(R.BENCHMARK_PATH, "" + bench.getUserId());
		    newBenchmarkFile = new File(newBenchmarkFile,  shortDate.format(new Date()));
		    bench.setPath(newBenchmarkFile.getAbsolutePath());

			// Write the new benchmark file to disk 
			FileUtils.writeStringToFile(newBenchmarkFile, benchmarkContents);
			
			// Make sure the configuration has the right line endings
		   	//Util.normalizeFile(newConfigFile);
			
			// Pass new configuration, and the parent solver objects, to the database & return the result
			log.debug("bench has id " + bench.getId());
			Boolean benchAdded = Benchmarks.add(bench, R.PUBLIC_SPACE_ID);			
			log.debug("bench now has id " + bench.getId());
			if (benchAdded){
				log.debug("userid " + R.PUBLIC_USER_ID);
				log.debug("benchname = " + bench.getName());
				log.debug("jobdescription = " + jobDescription);
				Job j = JobManager.setupJob(R.PUBLIC_USER_ID, bench.getName(), jobDescription, preProcessorId, postProcessorId, queueId);  
				log.debug("job id is " + j.getId());
				List<Integer> benchmarkIds = new LinkedList<Integer>();//Job methods takes lists so need this
				benchmarkIds.add(bench.getId());
				List<Integer> solverIds = new LinkedList<Integer>();
				Integer solverId = Integer.parseInt((String)request.getParameter(SOLVER_ID));
				solverIds.add(solverId);
				List<Integer> configIds = Solvers.getDefaultConfigForSolver(solverId);
				
				JobManager.buildJob(j, R.PUBLIC_USER_ID, cpuLimit, clockTimeout, benchmarkIds, solverIds, configIds, R.PUBLIC_SPACE_ID);
				int jobId = JobManager.submitJobReturnId(j, R.PUBLIC_SPACE_ID);
				return jobId;
			}
			else
			{	
				return -1;
			}
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}
	
		
		return -1;
	}	
	
	
	/**
	 * Validates 
	 * 
	 * @param request the object containing benchmark, solver info
	 * @return true iff all necessary configuration file info was provided by the client and is valid
	 * @author Benton McCune
	 */
	private boolean isValidRequest(HttpServletRequest request) {
		log.debug("solver id = " + request.getParameter(SOLVER_ID));
		log.debug("benchmark contents = " + request.getParameter(BENCHMARK_CONTENTS));
		try {
			if( Util.isNullOrEmpty((String) request.getParameter(SOLVER_ID))
					|| Util.isNullOrEmpty((String)request.getParameter(BENCHMARK_CONTENTS))) {
					return false;
			}
			
			// Ensure the solver id is valid
			Integer.parseInt((String)request.getParameter(SOLVER_ID));
						
			return true;
		} catch (Exception e) {
			log.warn(e.getMessage(), e);
		}
		
		return false;
	}
}