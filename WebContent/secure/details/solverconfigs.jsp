<%@page contentType="text/html" pageEncoding="UTF-8" import="org.starexec.data.security.*,java.io.OutputStreamWriter, java.util.HashMap, java.util.ArrayList, java.util.List, org.starexec.data.database.*, org.starexec.data.to.*, org.starexec.util.*"%>
<%@taglib prefix="star" tagdir="/WEB-INF/tags" %>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>

<%-- handles requests to print out solver configurations 
currently only used in StarexecCommand --%>
<%		
	try {
		int solverid = Integer.parseInt(request.getParameter("solverid"));
		int userId=SessionUtil.getUserId(request);
		if(!Permissions.canUserSeeSolver(solverid, userId)) {
			response.sendError(HttpServletResponse.SC_FORBIDDEN, "Invalid Permissions");
		} else {
			int limit = Integer.parseInt(request.getParameter("limit"));
			
			

			Solver s = Solvers.get(solverid);

			List<Configuration> cs = Solvers.getConfigsForSolver(solverid);
			
			String str = "";
			int count = 0;

			for(Configuration c : cs){
			   str = str + "id=" + c.getId() + " : name=" + c.getName() + "\n";
			   count++;
			   if(count == limit){break;}
			}

			OutputStreamWriter writer = new OutputStreamWriter(response.getOutputStream());
	    	writer.write(str);
	    	writer.flush();
	    	writer.close();
		}
				
		
	} catch (NumberFormatException nfe) {
		nfe.printStackTrace();
		response.sendError(HttpServletResponse.SC_BAD_REQUEST, "The given space id was in an invalid format");
	} catch (Exception e) {
		e.printStackTrace();
		response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Life, Jim, but not as we know it");
	}
%>

