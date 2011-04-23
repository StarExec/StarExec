package com.starexec.servlets;

import java.io.IOException;
import java.util.logging.Logger;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.starexec.constants.P;
import com.starexec.constants.R;
import com.starexec.data.Database;
import com.starexec.util.LogUtil;

/**
 * Servlet implementation class Results
 */
@WebServlet("/Results")
public class Results extends HttpServlet {
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
		if(request.getParameter(P.JOB_ID) != null){
			int jobId = Integer.parseInt(request.getParameter(P.JOB_ID));
			String status = request.getParameter(P.JOB_STATUS);
			String node = request.getParameter(P.JOB_NODE);
			
			
			database.updateJobStatus(jobId, status, node);
			
			LogUtil.LogInfo(String.format("Changed job %d status to %s, node is %s", jobId, status, node));
		} else if (request.getParameter(P.PAIR_ID) != null) {
			int pairId = Integer.parseInt(request.getParameter(P.PAIR_ID));
			String result = request.getParameter(P.PAIR_RESULT);
			
			database.updatePairResult(pairId, result);

			LogUtil.LogInfo(String.format("Changed pair %d result to %s", pairId, result));
		}											
	}

	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

	}

}
