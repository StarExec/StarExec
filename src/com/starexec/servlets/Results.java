package com.starexec.servlets;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;

import com.starexec.constants.P;
import com.starexec.data.Database;
import com.starexec.util.Util;

/**
 * Servlet implementation class Results
 */
@WebServlet("/Results")
public class Results extends HttpServlet {
	private static final Logger log = Logger.getLogger(Results.class);
	private static final long serialVersionUID = 1L;
	private Database database = new Database();
       
    /**
     * @see HttpServlet#HttpServlet()
     */
    public Results() {
        super();        
    }

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		// When a request comes in to the results service...
		if(Util.paramExists(P.JOB_ID, request))			// If it has a job id parameter, handle the request as a job
			handleJob(request, response);
		else if (Util.paramExists(P.PAIR_ID, request))	// Else if it has a pair id parameter, handle the request as a pair
			handlePair(request, response);					
	}
	
	private void handleJob(HttpServletRequest request, HttpServletResponse response){
		int jobId = Integer.parseInt(request.getParameter(P.JOB_ID));		// Get the job id
		String status = request.getParameter(P.JOB_STATUS);					// Get the status (may be empty/null)
		String node = request.getParameter(P.JOB_NODE);						// Get the node the ran on (may be empty/null)
			
		database.updateJobStatus(jobId, status, node);
		
		log.info(String.format("Changed job %d status to %s, node is %s", jobId, status, node));
	}
	
	private void handlePair(HttpServletRequest request, HttpServletResponse response){
		int pairId = Integer.parseInt(request.getParameter(P.PAIR_ID));
		
		String result = request.getParameter(P.PAIR_RESULT);
		String status = request.getParameter(P.JOB_STATUS);
		String node = request.getParameter(P.JOB_NODE);
		
		Long startTime = -1L;
		Long endTime = -1L;
		if(Util.paramExists(P.PAIR_START_TIME, request))
			startTime = Long.parseLong(request.getParameter(P.PAIR_START_TIME));
		if(Util.paramExists(P.PAIR_END_TIME, request))
			endTime = Long.parseLong(request.getParameter(P.PAIR_END_TIME));			
		
		database.updatePairResult(pairId, status, result, node, startTime, endTime);

		log.info(String.format("Changed pair %d result to %s", pairId, result));		
	}

	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

	}

}
