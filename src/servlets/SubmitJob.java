package servlets;

import java.io.IOException;
import java.util.ArrayList;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import manage.*;

public class SubmitJob extends HttpServlet {
	private static final long serialVersionUID = 1L;       
    
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
    	Jobject job = new Jobject();
    	String[] solverIds = request.getParameterValues("solver");
    	String[] benchmarkIds = request.getParameterValues("bench");
    	
    	//ArrayList<Integer> sids = new ArrayList<Integer>();
    	ArrayList<Integer> bids = new ArrayList<Integer>();

    	for(String s : benchmarkIds)
    		bids.add(Integer.parseInt(s));
    	    
    	for(String s : solverIds) {
    		int sid = Integer.parseInt(s);
    		try {
				SolverLink sl = job.addSolver(sid);
				sl.addBenchmarks(bids);
				
			} catch (Exception e) {
				e.printStackTrace();
			}
    	}
    	
    	try {
			JobManager.doJob(job);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}    
}
