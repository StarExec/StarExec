package org.starexec.servlets;

import java.io.*;
import java.util.*;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.starexec.app.Starexec;
import org.starexec.constants.R;
import org.starexec.data.database.Benchmarks;
import org.starexec.data.database.Jobs;
import org.starexec.data.database.Solvers;
import org.starexec.data.to.Benchmark;
import org.starexec.data.to.JobPair;
import org.starexec.data.to.Solver;
import org.starexec.data.to.User;
import org.starexec.util.SessionUtil;
import org.starexec.util.Util;
import org.starexec.util.Validator;

@SuppressWarnings("serial")
public class GetPicture extends HttpServlet{
	private static final Logger log = Logger.getLogger(CreateJob.class);
    
	@Override
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		response.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
	}
	
	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		if (false == validateRequest(request)) {
			response.sendError(HttpServletResponse.SC_BAD_REQUEST, "the GetPicture request was invalid");
			return;
		}
		
    	String fileName = "";
    	String defaultName = "";
    	
		if (request.getParameter("type").equals("uthn")) {
			fileName = String.format("/users/Pic%s_thn.jpg", request.getParameter("Id").toString());
			defaultName = String.format ("/users/Pic0.jpg");
		} else if (request.getParameter("type").equals("uorg")) {
			fileName = String.format("/users/Pic%s_org.jpg", request.getParameter("Id").toString());
			defaultName = String.format ("/users/Pic0.jpg");
		} else if (request.getParameter("type").equals("sthn")) {
			fileName = String.format("/solvers/Pic%s_thn.jpg", request.getParameter("Id").toString());
			defaultName = String.format ("/solvers/Pic0.jpg");
		} else if (request.getParameter("type").equals("sorg")) {
			fileName = String.format("/solvers/Pic%s_org.jpg", request.getParameter("Id").toString());
			defaultName = String.format ("/solvers/Pic0.jpg");
		} else if (request.getParameter("type").equals("bthn")) {
			fileName = String.format("/benchmarks/Pic%s_thn.jpg", request.getParameter("Id").toString());
			defaultName = String.format ("/benchmarks/Pic0.jpg");
		} else if (request.getParameter("type").equals("borg")) {
			fileName = String.format("/benchmarks/Pic%s_org.jpg", request.getParameter("Id").toString());
			defaultName = String.format ("/benchmarks/Pic0.jpg");
		}
		
    	String filePath = R.PICTURE_PATH;
    	String filenamedownload;
    	String filenamedisplay;
		
		File file = new File(filePath + "/" + fileName);
		if (file.exists())
		{
			filenamedownload = filePath + "/" + fileName;
			filenamedisplay = fileName;
		}
		else
		{
			fileName = String.format(defaultName);
			filenamedownload = filePath + "/" + fileName;
			filenamedisplay = fileName;
		}
		
		response.setContentType("application/x-download");
		response.addHeader("Content-Disposition", "attachment;filename="
		    + filenamedisplay);
		  try {
		   java.io.OutputStream os = response.getOutputStream();
		   java.io.FileInputStream fis = new java.io.FileInputStream(
		     filenamedownload);
		   byte[] b = new byte[1024];
		   int i = 0;
		   while ((i = fis.read(b)) > 0) {
		    os.write(b, 0, i);
		   }
		   fis.close();
		   os.flush();
		   os.close();
		  } catch (Exception e) {
		  }
    }
	
    public static boolean validateRequest(HttpServletRequest request) {
    	try {
    		if (!Util.paramExists("type", request)
    			|| !Util.paramExists("Id", request)) {
    			return false;
    		}
    		
    		if (!Validator.isValidInteger(request.getParameter("Id"))) {
    			return false;
    		}
        	
    		if (!(request.getParameter("type").equals("uthn") ||
    			  request.getParameter("type").equals("uorg") ||
    			  request.getParameter("type").equals("sthn") ||
    			  request.getParameter("type").equals("sorg") ||
    			  request.getParameter("type").equals("bthn") ||
    			  request.getParameter("type").equals("borg")
    			  )) {
    			return false;
    		}
    		
    		return true;
    	} catch (Exception e) {
    		log.warn(e.getMessage(), e);
    	}
    	
    	return false;
    }

}
