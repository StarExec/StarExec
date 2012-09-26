package org.starexec.servlets;

import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

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
import org.starexec.data.to.Solver;
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
    private static final String SOLVER_ID = "1";
    //private static final String CONFIG_CONTENTS = "saveConfigContents";
    //private static final String CONFIG_NAME = "saveConfigName";    		
    
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
			Boolean result = handleBenchmark(request);
			
			// Redirect user based on how the configuration handling went
			if(!result) {
				response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Failed to save new benchmark.");	
			} else {
				log.debug("success!!");
				//response.sendRedirect("/starexec/secure/details/solver.jsp?id=" + Integer.parseInt((String)request.getParameter(SOLVER_ID)));	
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
	public Boolean handleBenchmark(HttpServletRequest request) {
		try {
			
			// Set up a new configuration object with the submitted information
			//Solver solver = Solvers.get(Integer.parseInt(request.getParameter(SOLVER_ID)));
			Solvers.get(1);
			Benchmark bench = new Benchmark();
			bench.setName("ben");
			log.debug("Public user id = " + R.PUBLIC_USER_ID);
			bench.setUserId(R.PUBLIC_USER_ID);
			log.debug("bench user id is " + bench.getUserId());
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
			return benchAdded;
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}
	
		
		return false;
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
					|| Util.isNullOrEmpty((String)request.getParameter(BENCHMARK_CONTENTS))) 
					{
				//return false;
				return true;
				}
			
			// Ensure the parent solver id is valid
			Integer.parseInt((String)request.getParameter(SOLVER_ID));
			
			// Ensure the configuration's name and description are valid
/*			if(!Validator.isValidPrimName(request.getParameter(CONFIG_NAME))
					|| !Validator.isValidPrimDescription(request.getParameter(CONFIG_DESC))) {
				return false;
			}
			
	*/		return true;
		} catch (Exception e) {
			log.warn(e.getMessage(), e);
		}
		
		return false;
	}
}