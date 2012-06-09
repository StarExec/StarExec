package org.starexec.servlets;

import java.io.*;
import java.util.*;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.FileUtils;
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

/**
 * Handles the request to get the picture from the file system. If there
 * is such a picture for the request, or else a default one, named as 
 * Pic0.jpg is returned.
 * @author Ruoyu Zhang & Todd Elvers
 */
@SuppressWarnings("serial")
public class GetPicture extends HttpServlet{
	private static final Logger log = Logger.getLogger(GetPicture.class);
    
	@Override
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		response.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED, "Wrong type of request.");
	}
	
	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		// If the request is not valid, then respond with an error
		if (false == validateRequest(request)) {
			response.sendError(HttpServletResponse.SC_BAD_REQUEST, "The get picture request was malformed.");
			return;
		}
		
		// Check what type is the request, and generate file in different folders according to it.
    	String defaultPicFilename = new String();
    	String picFilename = new String();
    	String pictureDir = R.PICTURE_PATH;
    	StringBuilder sb = new StringBuilder();
    	
		if (request.getParameter("type").equals("uthn")) {
			sb.delete(0, sb.length());
			sb.append("users");
			sb.append(File.separator);
			sb.append("Pic");
			sb.append(request.getParameter("Id").toString());
			sb.append("_thn.jpg");
			
			defaultPicFilename = GetPicture.getDefaultPicture("users");
		} else if (request.getParameter("type").equals("uorg")) {
			sb.delete(0, sb.length());
			sb.append("users");
			sb.append(File.separator);
			sb.append("Pic");
			sb.append(request.getParameter("Id").toString());
			sb.append("_org.jpg");
			
			defaultPicFilename = GetPicture.getDefaultPicture("users");
		} else if (request.getParameter("type").equals("sthn")) {
			sb.delete(0, sb.length());
			sb.append("solvers");
			sb.append(File.separator);
			sb.append("Pic");
			sb.append(request.getParameter("Id").toString());
			sb.append("_thn.jpg");
			
			defaultPicFilename = GetPicture.getDefaultPicture("solvers");
		} else if (request.getParameter("type").equals("sorg")) {
			sb.delete(0, sb.length());
			sb.append("solvers");
			sb.append(File.separator);
			sb.append("Pic");
			sb.append(request.getParameter("Id").toString());
			sb.append("_org.jpg");
			
			defaultPicFilename = GetPicture.getDefaultPicture("solvers");
		} else if (request.getParameter("type").equals("bthn")) {
			sb.delete(0, sb.length());
			sb.append("benchmarks");
			sb.append(File.separator);
			sb.append("Pic");
			sb.append(request.getParameter("Id").toString());
			sb.append("_thn.jpg");
			
			defaultPicFilename = GetPicture.getDefaultPicture("benchmarks");
		} else if (request.getParameter("type").equals("borg")) {
			sb.delete(0, sb.length());
			sb.append("benchmarks");
			sb.append(File.separator);
			sb.append("Pic");
			sb.append(request.getParameter("Id").toString());
			sb.append("_org.jpg");
			
			defaultPicFilename = GetPicture.getDefaultPicture("benchmarks");
		}
		picFilename = sb.toString();
		
    	sb.delete(0, sb.length());
    	sb.append(pictureDir);
    	sb.append(File.separator);
    	sb.append(picFilename);
		File file = new File(sb.toString());
		
		// If the desired file exists, then the file will return it, or else return the default file Pic0.jpg
		if (file.exists() == false) {
			sb.delete(0, sb.length());
			sb.append(pictureDir);
			sb.append(File.separator);
			sb.append(defaultPicFilename);
			file = new File(sb.toString());
		}
		
		// Return the file in the response.
		try {
			java.io.OutputStream os = response.getOutputStream();
			FileUtils.copyFile(file, os);
		} catch (Exception e) {
			log.warn(e.getMessage(), e);
		}
	}
	
	/**
	 * Validates the GetPicture request to make sure the requested data is of the right format
	 * @param request The request need to be validated.
	 * @return true if the request is valid.
	 */
    private static boolean validateRequest(HttpServletRequest request) {
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
    
    
    /**
     * Gets the path of the default picture for a particular primitive
     * 
     * @param primType the type of primitive whose default picture we need
     * @return the default picture of the specified primitive type
     * @author Todd Elvers
     */
    private static String getDefaultPicture(String primType){
    	StringBuilder sb = new StringBuilder();
    	
    	switch(primType.charAt(0)){
	    	case 'u':
	    		sb.append("users");
	    		sb.append(File.separator);
	    		sb.append("Pic0.jpg");
	    		break;
	    	case 'b':
	    		sb.append("benchmarks");
	    		sb.append(File.separator);
	    		sb.append("Pic0.jpg");
	    		break;
	    	case 's':
	    		sb.append("solvers");
	    		sb.append(File.separator);
	    		sb.append("Pic0.jpg");
	    		break;
    	}
    	
    	return sb.toString();
    }
    
}
