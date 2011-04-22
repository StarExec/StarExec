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

@WebServlet("/SubmitJob")
public class SubmitJob extends HttpServlet {
	private static final long serialVersionUID = 2L;       
    
	public SubmitJob(){
		super();
	}
	
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {    	
		Logger.getAnonymousLogger().info(request.getRequestURL().toString());
    	User usr = new User("admin");
    	
    	Jobject job = new Jobject(usr);
    	Database database = new Database();
    	String solverIds = request.getParameter("solver");
    	String benchmarkIds = request.getParameter("bench");
    	String levelIds = request.getParameter("level");
    	
    	ArrayList<Integer> bids = new ArrayList<Integer>();

    	if(benchmarkIds != null && !benchmarkIds.isEmpty())
    		for(String s : benchmarkIds.split(","))
    			bids.add(Integer.parseInt(s));
    	    
    	if(levelIds != null && !levelIds.isEmpty())
    		for(String s : levelIds.split(","))
    			bids.addAll(database.levelToBenchmarkIds(Integer.parseInt(s)));
    	
    	System.out.println(Arrays.toString(bids.toArray()));
    	
    	for(String s : solverIds.split(",")) {
    		int sid = Integer.parseInt(s);
    		try {
				SolverLink sl = job.addSolver(sid);
				sl.addBenchmarks(bids);				
			} catch (Exception e) {
				Logger.getAnonymousLogger().info(e.toString());
			}
    	}
    	
    	try {
			JobManager.doJob(job);
		} catch (Exception e) {
			Logger.getAnonymousLogger().info(e.toString());
		}
		
		String line;
		File f = new File(String.format("/home/starexec/jobout/job_%d.out", JobManager.getJID()));
		Scanner in = new Scanner(f);
		while(in.hasNext()) {
			line = in.nextLine();
			response.getWriter().println(line);
			Logger.getAnonymousLogger().info(line);
		}
	}    
}
