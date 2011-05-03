package com.starexec.servlets;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Scanner;
import java.util.logging.Logger;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.starexec.data.Database;
import com.starexec.data.to.User;
import com.starexec.manage.*;
import com.starexec.util.LogUtil;

@WebServlet("/SubmitJob")
public class SubmitJob extends HttpServlet {
	private static final long serialVersionUID = 2L;       
    
	public SubmitJob(){
		super();
	}
	
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {    	
		Logger.getAnonymousLogger().info(request.getRequestURL().toString());

    	Jobject job = new Jobject(new User("admin"));			// Start a new job.
    	String configIds = request.getParameter("config");		// Naked string "cid1, cid2, cid3"
    	String benchmarkIds = request.getParameter("bench");	// Naked string "bid1, bid2, bid3"
    	String levelIds = request.getParameter("level");		// ???
    	Database database = new Database();						// Grab new db connection.
    	
    	ArrayList<Integer> bids = new ArrayList<Integer>();	
    	if(benchmarkIds != null && !benchmarkIds.isEmpty())
    		for(String s : benchmarkIds.split(","))
    			bids.add(Integer.parseInt(s));
    	    
    	ArrayList<Integer> cids = new ArrayList<Integer>();
    	if(configIds != null && !configIds.isEmpty())
    		for(String s : configIds.split(","))
    			cids.add(Integer.parseInt(s));
    	
    	// What the heck is this?
    	if(levelIds != null && !levelIds.isEmpty())
    		for(String s : levelIds.split(","))
    			bids.addAll(database.levelToBenchmarkIds(Integer.parseInt(s)));
    	
    	System.out.println(Arrays.toString(bids.toArray()));

		try {
	    	for(int bid : bids) {
	    		BenchmarkLink link = job.addBenchmark(bid);
				link.addConfigs(cids);
	    	}
	    	
			JobManager.doJob(job);
			
			String line;
			File f = new File(String.format("/home/starexec/jobout/job_%d.out", JobManager.getJID()));
			
			while(!f.exists())
				Thread.sleep(2000);
			
			Scanner in = new Scanner(f);
			while(in.hasNext()) {
				line = in.nextLine();
				response.getWriter().println(line);
			}
		} catch (Exception e) {
			LogUtil.LogException(e);
		}
	}    
}
