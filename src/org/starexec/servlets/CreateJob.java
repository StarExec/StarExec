package org.starexec.servlets;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.starexec.constants.R;
import org.starexec.data.database.Benchmarks;
import org.starexec.data.database.Communities;
import org.starexec.data.database.Jobs;
import org.starexec.data.database.Permissions;
import org.starexec.data.database.Pipelines;
import org.starexec.data.database.Processors;
import org.starexec.data.database.Queues;
import org.starexec.data.database.Settings;
import org.starexec.data.database.Solvers;
import org.starexec.data.database.Spaces;
import org.starexec.data.security.ProcessorSecurity;
import org.starexec.data.security.ValidatorStatusCode;
import org.starexec.data.to.Benchmark;
import org.starexec.data.to.Configuration;
import org.starexec.data.to.DefaultSettings;
import org.starexec.data.to.Job;
import org.starexec.data.to.JobPair;
import org.starexec.data.to.Permission;
import org.starexec.data.to.Processor;
import org.starexec.data.to.Queue;
import org.starexec.data.to.Solver;
import org.starexec.data.to.Space;
import org.starexec.data.to.pipelines.JoblineStage;
import org.starexec.data.to.pipelines.PipelineStage;
import org.starexec.data.to.pipelines.SolverPipeline;
import org.starexec.jobs.JobManager;
import org.starexec.util.SessionUtil;
import org.starexec.util.Util;
import org.starexec.util.Validator;
/**
 * Creates a class to keep track of the Benchmark-Solver-Configuration Pairs
 * @author kais_wyatt
 */
class BSC {
    List<Benchmark> b;
    List<Solver> s;
    HashMap<Solver, List<Configuration>> sc;

    BSC (List<Benchmark> b, List<Solver> s, HashMap<Solver, List<Configuration>> sc) {
        this.b = b;
        this.s = s;
        this.sc = sc;
    }
}

/**
 * Servlet which handles incoming requests to create new jobs
 * @author Tyler Jensen
 */
@SuppressWarnings("serial")
public class CreateJob extends HttpServlet {		
	private static final Logger log = Logger.getLogger(CreateJob.class);	

	// Request attributes	
	private static final String name = "name";
	private static final String description = "desc";
	private static final String postProcessor = "postProcess";
	private static final String preProcessor = "preProcess";
	private static final String workerQueue = "queue";
	private static final String configs = "configs";
	private static final String run = "runChoice";
	private static final String benchChoice = "benchChoice";
	private static final String benchmarks = "bench";
	private static final String cpuTimeout = "cpuTimeout";
	private static final String clockTimeout = "wallclockTimeout";
	private static final String spaceId = "sid";
	private static final String traversal = "traversal";
	private static final String pause = "pause";
	private static final String maxMemory="maxMem";
	private static final String randSeed="seed";
	
	//unique to quick jobs
	private static final String benchProcessor = "benchProcess";
	private static final String benchName = "benchName";
	private static final String solver = "solver";



	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		response.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
	}
	
	
	/**
	 * Creates a quick job, which is a flat job with only a single solver and benchmark. Every configuration is run
	 * on the benchmark, so the number of job pairs is equal to the number of configurations in the solver
	 * @param j A job object for the job, which must have the following attributes set: userId, pre processor, post processor, 
	 * queue, name, description, seed
	 * @param cpuLimit The CPU runtime limit for the job pairs
	 * @param wallclockLimit The wallclock runtime limit for the job pairs
	 * @param memoryLimit The memory limit, in bytes, for the job pairs
	 * @param seed A number that will be handed to the pre processor
	 * @param solverId The ID of the solver that will be run
	 * @param benchId The ID of the benchmark that will be run
	 * @param sId The ID of the space to put the job in 
	 * @return The ID of the new job, or null if there was an error
	 */
	public static void buildQuickJob(Job j, int solverId, int benchId, Integer sId) {
		//Setup the job's attributes
		
		List<Configuration> config = Solvers.getConfigsForSolver(solverId);
		List<Integer> configIds = new ArrayList<Integer>();
		for (Configuration c :config) {
			configIds.add(c.getId());
		}
		List<Integer> benchmarkIds = new ArrayList<Integer>();
		benchmarkIds.add(benchId);
		JobManager.buildJob(j, benchmarkIds, configIds, sId, null);
	}
	/**
	 * Tests a solver using default info for the space it is being uploaded in
	 * @param solverId ID of the solver ot put the job in.
	 * @param spaceId Id of the space to put the job in
	 * @param userId Id of the user that will make this job
	 * @param settingsId ID of the default settings profile to use for the job
	 * @return The ID of the job that was newly created, or -1 on error
	 */
	public static int buildSolverTestJob(int solverId, int spaceId, int userId, int settingsId) {
		Solver s=Solvers.get(solverId);
		DefaultSettings settings=Settings.getProfileById(settingsId);
		Job j = JobManager.setupJob(
				userId,
				s.getName(), 
				"test job for new solver "+s.getName()+" "+"("+s.getId()+")",
				settings.getPreProcessorId(),
				settings.getPostProcessorId(), 
				Queues.getTestQueue(),
				0,settings.getCpuTimeout(),settings.getWallclockTimeout(),settings.getMaxMemory());
		
		buildQuickJob(j, solverId, settings.getBenchId(), spaceId);
		boolean submitSuccess = Jobs.add(j, spaceId);
		if (submitSuccess) {
			return j.getId();
		}
		return -1; //error
	}

	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {		
		final String method = "doPost";
		// Make sure the request is valid
		long a = System.currentTimeMillis();
		log.debug("starting job post");
		ValidatorStatusCode status=isValid(request);
		if(!status.isSuccess()) {
			//attach the message as a cookie so we don't need to be parsing HTML in StarexecCommand
			log.debug("received and invalid job creation request");
			response.addCookie(new Cookie(R.STATUS_MESSAGE_COOKIE, status.getMessage()));
			response.sendError(HttpServletResponse.SC_BAD_REQUEST, status.getMessage());
			return;
		}		
		log.debug("job validation took this long " + (System.currentTimeMillis() - a));
		int cpuLimit = Integer.parseInt((String)request.getParameter(cpuTimeout));
		int runLimit = Integer.parseInt((String)request.getParameter(clockTimeout));
		long memoryLimit=Util.gigabytesToBytes(Double.parseDouble(request.getParameter(maxMemory)));
		//a number that will be provided to the pre processor for every job pair in this job
		long seed=Long.parseLong(request.getParameter(randSeed));


		
		//ensure that the cpu limits are greater than 0 and also don't exceed the constant maximum
		cpuLimit = (cpuLimit <= 0) ? R.MAX_PAIR_CPUTIME : cpuLimit;
		runLimit = (runLimit <= 0) ? R.MAX_PAIR_RUNTIME : runLimit;
		
		//memory is in units of bytes
		memoryLimit = (memoryLimit <=0) ? R.DEFAULT_PAIR_VMEM : memoryLimit;
		
		int space = Integer.parseInt((String)request.getParameter(spaceId));
		int userId = SessionUtil.getUserId(request);

		boolean suppressTimestamp = request.getParameter(R.SUPPRESS_TIMESTAMP_INPUT_NAME).equals("yes");
		log.debug("("+method+")"+" User chose "+(suppressTimestamp?"":"not ")+"to suppress timestamps.");

		//Setup the job's attributes
		Job j = JobManager.setupJob(
				userId,
				(String)request.getParameter(name), 
				(String)request.getParameter(description),
				Integer.parseInt((String)request.getParameter(preProcessor)),
				Integer.parseInt((String)request.getParameter(postProcessor)), 
				Integer.parseInt((String)request.getParameter(workerQueue)),
				seed,cpuLimit,runLimit,memoryLimit,suppressTimestamp);
		
		log.debug("job setup took this long " + (System.currentTimeMillis() - a));

		
		String selection = request.getParameter(run);
		String benchMethod = request.getParameter(benchChoice);
		String traversalMethod = request.getParameter(traversal);
		HashMap<Integer, String> SP=null;
		//Depending on our run selection, handle each case differently
		String error=null;
		//if the user created a quickJob, they uploaded a single text benchmark and a solver to run
		if (selection.equals("quickJob")) {
			int solverId=Integer.parseInt(request.getParameter(solver));
			String benchText=request.getParameter(benchmarks);
			String bName=request.getParameter(benchName);
			int benchProc = Integer.parseInt(request.getParameter(benchProcessor));
			int benchId=BenchmarkUploader.addBenchmarkFromText(benchText, bName, userId, benchProc, false);
			log.debug("new benchmark created for quickJob with id = "+benchId);
			buildQuickJob(j, solverId, benchId, space);
		} else if (selection.equals("keepHierarchy")) {
			log.debug("User selected keepHierarchy");
			//Create the HashMap to be used for creating job-pair path
			SP =  Spaces.spacePathCreate(userId, Spaces.getSubSpaceHierarchy(space, userId), space);
			HashMap<Integer, List<JobPair>> spaceToPairs = new HashMap<Integer,List<JobPair>>();
			List<Space> spaces = Spaces.trimSubSpaces(userId, Spaces.getSubSpaceHierarchy(space, userId)); //Remove spaces the user is not a member of
			log.debug("got all the subspaces for the job");		
			spaces.add(0, Spaces.get(space));
			
			for (Space s : spaces) {
			    List<JobPair> pairs= JobManager.addJobPairsFromSpace(userId, s.getId(), SP.get(s.getId()));
			    
			    spaceToPairs.put(s.getId(), pairs);
			}
			log.debug("added all the job pairs from every space");
			
			//if we're doing "depth first", we just add all the pairs from space1, then all the pairs from space2, and so on
			if (traversalMethod.equals("depth")) {
				JobManager.addJobPairsDepthFirst(j, spaceToPairs);
				
				//otherwise, we are doing "breadth first", so we interleave pairs from all the spaces
			} else {
				log.debug("adding pairs round robin");
				JobManager.addJobPairsRoundRobin(j, spaceToPairs);
				
			}
		} else { //user selected "choose"
			
			SP =  Spaces.spacePathCreate(userId, Spaces.getSubSpaceHierarchy(space, userId), space);
			List<Integer> configIds = Util.toIntegerList(request.getParameterValues(configs));

			if (benchMethod.equals("runAllBenchInSpace")) {
			    List<JobPair> pairs= JobManager.addJobPairsFromSpace(userId, space, Spaces.getName(space),configIds);
			    if (pairs==null) {
			    	error="unable to get any job pairs for the space ID = "+space;
			    } else {
				    j.addJobPairs(pairs);
			    }
			
			
			}else if (benchMethod.equals("runAllBenchInHierarchy")) {
				log.debug("got request to run all in bench hierarchy");

				HashMap<Integer,List<JobPair>> spaceToPairs=JobManager.addBenchmarksFromHierarchy(Integer.parseInt(request.getParameter(spaceId)), SessionUtil.getUserId(request), configIds, SP);
				
				if (traversalMethod.equals("depth")) {
					log.debug("User selected depth-first traversal");

					JobManager.addJobPairsDepthFirst(j, spaceToPairs);
				} else {
					log.debug("users selected round robin traversal");
					JobManager.addJobPairsRoundRobin(j, spaceToPairs);
				}
				// We chose to run the hierarchy, so add subspace benchmark IDs to the list.
				if (j.getJobPairs().size() == 0) {
					// No pairs in the job means no benchmarks in the hierarchy; error out
					String message="Either no benchmarks were selected, or there are none available in the current space/hierarchy. Could not create job.";
					response.addCookie(new Cookie(R.STATUS_MESSAGE_COOKIE, message));

					response.sendError(HttpServletResponse.SC_BAD_REQUEST, message);
					return;
				}
			} else {
				List<Integer> benchmarkIds = Util.toIntegerList(request.getParameterValues(benchmarks));
				JobManager.buildJob(j, benchmarkIds, configIds, space, SP);
			}
		}
		
		if (error != null) {
		    response.sendError(HttpServletResponse.SC_BAD_REQUEST, error);
		    return;
		}

		if (j.getJobPairs().size() == 0) {
			String message="Error: no job pairs created for the job. Could not proceed with job submission.";
			response.addCookie(new Cookie(R.STATUS_MESSAGE_COOKIE, message));

			response.sendError(HttpServletResponse.SC_BAD_REQUEST, message);
			// No pairs in the job means something went wrong; error out
			return;
		}
		
		log.debug("jobpair creation took this long " + (System.currentTimeMillis() - a));

		

		boolean submitSuccess = Jobs.add(j, space);
		String start_paused = request.getParameter(pause);

		//if the user chose to immediately pause the job
		if (start_paused.equals("yes")) {
			Jobs.pause(j.getId());
		}
		
		if(submitSuccess) {
		    // If the submission was successful, send back to space explorer

			response.addCookie(new Cookie("New_ID", String.valueOf(j.getId())));
			if (!selection.equals("quickJob")) {
			    response.sendRedirect(Util.docRoot("secure/explore/spaces.jsp"));
			} else {
				response.sendRedirect(Util.docRoot("secure/details/job.jsp?id="+j.getId()));
			}
		}else  {
		    // Or else send an error
		    response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, 
				       "Your job failed to submit for an unknown reason. Please try again.");
		}
	}
	
	public static ValidatorStatusCode isValid(int userId, int queueId, int cpuLimit, int wallclockLimit, Integer preProcId, Integer postProcId) {
		List<Queue> userQueues = Queues.getUserQueues(userId); 
		Boolean queueFound=false;
		for (Queue queue:userQueues){
			if (queue.getId() == queueId){
				queueFound=true;
				break;
			}
		}
					
		if (!queueFound){
			return new ValidatorStatusCode(false, "The given queue does not exist or you do not have access to it");
		}
		
		Queue q=Queues.get(queueId);
		if (wallclockLimit > q.getWallTimeout()) {
			return new ValidatorStatusCode(false, "The given wallclock timeout exceeds the maximum allowed for this queue, which is "+q.getWallTimeout());
		}
		
		if (cpuLimit>q.getCpuTimeout()) {
			return new ValidatorStatusCode(false, "The given cpu timeout exceeds the maximum allowed for this queue, which is "+q.getCpuTimeout());
		}
		
		 if (preProcId != null) {
		    if (!ProcessorSecurity.canUserSeeProcessor(preProcId, userId).isSuccess()) {
		    	return new ValidatorStatusCode(false, "You do not have permission to use the given preprocessor, or it does not exist");
			} 
		 }
		 if (postProcId != null) {
			if (!ProcessorSecurity.canUserSeeProcessor(postProcId, userId).isSuccess()) {
			    return new ValidatorStatusCode(false, "You do not have permission to use the given postprocessor, or it does not exist");
			} 
		 }
		
		return new ValidatorStatusCode(true);
	}
	
	/**
	 * Uses the Validate util to ensure the incoming request is valid. This checks for illegal characters
	 * and content length requirements to ensure it is not malicious.
	 * @param request The request to validate
	 * @return True if the request is ok to act on, false otherwise
	 */
	private ValidatorStatusCode isValid(HttpServletRequest request) {
		try {
			// Make sure the parent space id is a int
			if(!Validator.isValidInteger(request.getParameter(spaceId))) {
				return new ValidatorStatusCode(false, "The given space ID needs to be a valid integer");
			}

			int userId = SessionUtil.getUserId(request);
			int sid = Integer.parseInt(request.getParameter(spaceId));
			// Make sure the user has access to the space
			Permission perm = Permissions.get(userId, sid);
			if (sid>=0) {
				if(perm == null || !perm.canAddJob()) {
					return new ValidatorStatusCode(false, "You do not have permission to add jobs in this space");
				}
			}

			// Make sure timeout an int
			if(!Validator.isValidInteger(request.getParameter(clockTimeout))) {
				return new ValidatorStatusCode(false, "The given wallclock timeout needs to be a valid integer");
			}

			if(!Validator.isValidInteger(request.getParameter(cpuTimeout))) {
				return new ValidatorStatusCode(false, "The given cpu timeout needs to be a valid integer");
			}
			
			if(!Validator.isValidDouble(request.getParameter(maxMemory))) {
				return new ValidatorStatusCode(false, "The given maximum memory needs to be a valid double");
			}
			
			if (!Validator.isValidLong(request.getParameter(randSeed))) {
				return new ValidatorStatusCode(false, "The random seed needs to be a valid long integer");
			}
			Integer preProc=null;
			Integer postProc=null;
			// If processors are specified, make sure they're valid ints
			if(Util.paramExists(postProcessor, request)) {
				
				if(!Validator.isValidInteger(request.getParameter(postProcessor))) {
					return new ValidatorStatusCode(false, "The given post processor ID needs to be a valid integer");
				}
				postProc=Integer.parseInt(request.getParameter(postProcessor));
				//means none was selected
				if (postProc<1) {
					postProc=null;
				}
			}
			
			if(Util.paramExists(preProcessor, request)) {
				if(!Validator.isValidInteger(request.getParameter(preProcessor))) {
					return new ValidatorStatusCode(false, "The given pre processor ID needs to be a valid integer");
				}
				preProc=Integer.parseInt(request.getParameter(preProcessor));
				if (preProc<1) {
					preProc=null;
				}
			}

			// Make sure the queue is a valid integer
			if(!Validator.isValidInteger(request.getParameter(workerQueue))) {
				return new ValidatorStatusCode(false, "The given queue ID needs to be a valid integer");
			}
			// Make sure the queue is a valid selection and user has access to it
			Integer queueId = Integer.parseInt(request.getParameter(workerQueue));
			
			//make sure both timeouts are <= the queue settings
			int cpuLimit = Integer.parseInt(request.getParameter(cpuTimeout));
			int runLimit = Integer.parseInt(request.getParameter(clockTimeout));
			
			
			
			
			// Ensure the job description is valid
			if(!Validator.isValidPrimDescription((String)request.getParameter(description))) {
				return new ValidatorStatusCode(false, "The given description is invalid, please see the help files to see the valid format");
			}		

			
			
			
			if (!Util.paramExists(run, request)) {
				return new ValidatorStatusCode(false, "You need to select a run choice for this job");
			}

			if (request.getParameter(run).equals("quickJob")) {
				//we only need to check to see if the space is valid if a space was actually specified
				if (!Util.paramExists(benchmarks, request)) {
					return new ValidatorStatusCode(false, "You need to select a benchmark to run a quick job");
				}
				
				if (!Validator.isValidInteger(request.getParameter(solver))) {
					return new ValidatorStatusCode(false, "The given solver ID is not a valid integer");
				}
				int solverId=Integer.parseInt(request.getParameter(solver));
				if (!Permissions.canUserSeeSolver(solverId, userId)) {
					return new ValidatorStatusCode(false, "You do not have permission to see the given solver ID");
				}
				
				if (Solvers.getConfigsForSolver(solverId).size()==0) {
					return new ValidatorStatusCode(false, "The given solver does not have any configurations");
				}
				
			}
				
				
			// Only need these checks if we're choosing which solvers and benchmarks to run.
			// In any other case, we automatically get them so we don't have to pass them
			// as part of the request.
			if (request.getParameter(run).equals("choose")) {

				// Check to see if we have a valid list of benchmark ids
				if (request.getParameter(benchChoice).equals("runChosenFromSpace")){
					if (!Validator.isValidIntegerList(request.getParameterValues(benchmarks))) {
						return new ValidatorStatusCode(false, "All selected benchmark IDs need to be valid integers");
					}
				}
				List<Integer> benchmarkIds=Util.toIntegerList(request.getParameterValues(benchmarks));
				if (request.getParameter(benchChoice).equals("runChosenFromSpace") && benchmarkIds.size() == 0) {
					return new ValidatorStatusCode(false, "You need to chose at least one benchmark to run a job");
				}
				// Make sure the user is using benchmarks they can see
				if(!Permissions.canUserSeeBenchs(benchmarkIds, userId)) {
					return new ValidatorStatusCode(false, "You do not have permission to use one or more of the benchmarks you have selected");
				}

				// Check to see if we have a valid list of configuration ids
				if(!Validator.isValidIntegerList(request.getParameterValues(configs))) {
					return new ValidatorStatusCode(false, "All selected configuration IDs need to be valid integers");
				}
				
				Set<Integer> solverIds=new HashSet<Integer>();
				for (Integer cid : Util.toIntegerList(request.getParameterValues(configs))) {
					solverIds.add(Solvers.getSolverByConfig(cid, false).getId());
				}
				if (solverIds.size()==0) {
					return new ValidatorStatusCode(false, "You need to select at least one configuration to run a job");
				}
				// Make sure the user is using solvers they can see
				if(!Permissions.canUserSeeSolvers(solverIds, userId)) {
					return new ValidatorStatusCode(false, "You do not have permission to use all of the selected solvers");
				}
			}
			// Passed all type checks-- next we check permissions 
			return CreateJob.isValid(userId,queueId,cpuLimit,runLimit,preProc,postProc);
		} catch (Exception e) {
			log.warn(e.getMessage(), e);
		}

		// Return false control flow is broken and ends up here
		return new ValidatorStatusCode(false, "Internal error creating job");
	}
}
