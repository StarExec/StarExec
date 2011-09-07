package com.starexec.servlets;

import java.io.IOException;
import java.util.ArrayList;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;

import com.starexec.data.Database;
import com.starexec.data.Databases;
import com.starexec.data.to.User;
import com.starexec.manage.BenchmarkLink;
import com.starexec.manage.JobManager;
import com.starexec.manage.Jobject;

@WebServlet("/SubmitJob")
public class SubmitJob extends HttpServlet {
	private static final Logger log = Logger.getLogger(SubmitJob.class);
	private static final long serialVersionUID = 2L;       
    
	public SubmitJob(){
		super();
	}
	
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {    	
		log.info(request.getRequestURL().toString());

    	Jobject job = new Jobject(new User(1));					// Start a new job.
    	String configIds = request.getParameter("config");		// Get the selected config ids
    	String benchmarkIds = request.getParameter("bench");	// Get the selected benchmark ids
    	String levelIds = request.getParameter("level");		// Get the selected level ids
    	String solverIds = request.getParameter("solver");		// Get the selected solver ids    	
    	
    	ArrayList<Integer> bids = new ArrayList<Integer>();	
    	if(benchmarkIds != null && !benchmarkIds.isEmpty()) {
    		// Add all benchmark id's to the list to pass to the Jobject    	
    		for(String s : benchmarkIds.split(",")) {
    			bids.add(Integer.parseInt(s));
    		}
    	}
    	
    	if(levelIds != null && !levelIds.isEmpty()) {
    		// Get all benchmarks under a selected level and add it to the list    	
    		for(String s : levelIds.split(",")) {
    			bids.addAll(Databases.next().levelToBenchmarkIds(Integer.parseInt(s)));
    		}
    	}
    	    
    	// Add all config ids to the list to pass to the jobject
    	ArrayList<Integer> cids = new ArrayList<Integer>();
    	if(configIds != null && !configIds.isEmpty()) {
    		for(String s : configIds.split(",")) {
    			cids.add(Integer.parseInt(s));
    		}
    	}
    	
    	// Get all configs under the solver and add it to the list
    	if(solverIds != null && !solverIds.isEmpty()) {
    		for(String s : solverIds.split(",")) {
    			cids.addAll(Databases.next().solverToConfigIds(Integer.parseInt(s)));
    		}
    	}
    	   
		try {
	    	for(int bid : bids) {
	    		BenchmarkLink link = job.addBenchmark(bid);
				link.addConfigs(cids);
	    	}
	    	
			JobManager.doJob(job);			
			response.sendRedirect("/starexec/viewjob.jsp");			
		} catch (Exception e) {
			log.error(e);
		}
	}    
}
