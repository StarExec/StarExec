package org.starexec.servlets;

import java.io.File;
import java.io.IOException;
import java.util.Scanner;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;

import org.starexec.constants.*;


/**
 * Servlet which handles incoming requests for files and gives the browser back the requested file
 * 
 * @deprecated This file is way out of date, but keeping around for reference
 */
@WebServlet("/GetFile")
public class GetFile extends HttpServlet {	
	private static final Logger log = Logger.getLogger(GetFile.class);
	private static final long serialVersionUID = 1L;
       
    /**
     * @see HttpServlet#HttpServlet()
     */
    public GetFile() {
        super();
        // TODO Auto-generated constructor stub
    }

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		String type = request.getParameter(P.FILE_REQUEST_TYPE);
		
		if(type.equals(P.BENCH_XML)){
			File f = new File(R.BENCHMARK_PATH, request.getParameter(P.FILE_PARENT));
			f = new File(f.getAbsolutePath(), "");
			response.setContentType("text/xml");
		    response.setHeader("Cache-Control", "no-cache");
		    response.setHeader("pragma","no-cache");
		    		    
		    Scanner scanner = new Scanner(f);
		    while(scanner.hasNext()){
		    	response.getWriter().write(scanner.nextLine());	
		    }		    
		    
		    scanner.close();
		}
	}

	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		// TODO Auto-generated method stub
	}

}
