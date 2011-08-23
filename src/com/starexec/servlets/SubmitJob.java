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

    	Jobject job = new Jobject(new User("admin"));			// Start a new job.
    	String configIds = request.getParameter("config");		// Get the selected config ids
    	String benchmarkIds = request.getParameter("bench");	// Get the selected benchmark ids
    	String levelIds = request.getParameter("level");		// Get the selected level ids
    	String solverIds = request.getParameter("solver");		// Get the selected solver ids
    	Database database = new Database();						// Grab a new db connection
    	
    	ArrayList<Integer> bids = new ArrayList<Integer>();	
    	if(benchmarkIds != null && !benchmarkIds.isEmpty())		// Add all benchmark id's to the list to pass to the Jobject
    		for(String s : benchmarkIds.split(","))
    			bids.add(Integer.parseInt(s));
    	
    	if(levelIds != null && !levelIds.isEmpty())				// Get all benchmarks under a selected level and add it to the list
    		for(String s : levelIds.split(","))
    			bids.addAll(database.levelToBenchmarkIds(Integer.parseInt(s)));
    	    
    	ArrayList<Integer> cids = new ArrayList<Integer>();		// Add all config ids to the list to pass to the jobject
    	if(configIds != null && !configIds.isEmpty())
    		for(String s : configIds.split(","))
    			cids.add(Integer.parseInt(s));
    	
    	if(solverIds != null && !solverIds.isEmpty())			// Get all configs under the solver and add it to the list
    		for(String s : solverIds.split(","))
    			cids.addAll(database.solverToConfigIds(Integer.parseInt(s)));    	        	
    	   
		try {
	    	for(int bid : bids) {
	    		BenchmarkLink link = job.addBenchmark(bid);
				link.addConfigs(cids);
	    	}
	    	
			JobManager.doJob(job);
			
			response.sendRedirect("/starexec/viewjob.jsp");
			
//			String line;
//			File f = new File(String.format("/home/starexec/jobout/job_%d.out", JobManager.getJID()));
//			
//			while(!f.exists())
//				Thread.sleep(2000);
//			
//			Scanner in = new Scanner(f);
//			while(in.hasNext()) {
//				line = in.nextLine();
//				response.getWriter().println(line);
//			}
		} catch (Exception e) {
			log.error(e);
		}
	}    
}
