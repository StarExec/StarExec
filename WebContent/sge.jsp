<%@ page language="java" contentType="text/html; charset=ISO-8859-1"
    pageEncoding="ISO-8859-1" import="org.ggf.drmaa.*, java.util.*, java.io.*"%>
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=ISO-8859-1">
<title>Insert title here</title>
</head>
<body>
<%	

	// This is a test for submitting a job in SGE using DRMAA
	Session ses = null;
	 
	try {
		SessionFactory factory = SessionFactory.getFactory();
		ses = factory.getSession();
				
		ses.init("");
		JobTemplate jt = ses.createJobTemplate();
		jt.setRemoteCommand("/home/starexec/test");
		//jt.setOutputPath("starexec:/project/tomcat-webapps/webapps");		
		jt.setWorkingDirectory("/project/tomcat-webapps/webapps");
		jt.setJoinFiles(true);
		String id = ses.runJob(jt);							
		JobInfo info = ses.wait(id, Session.TIMEOUT_WAIT_FOREVER);
				
		if(info.wasAborted())
			out.write("<h2 style='color: red;'>JOB FAILED (abort)</h2>");
		else
			out.write("<h2 style='color: green;'>JOB SUBMITTED (success)</h2>");		
		
%>
		<table>
			<tr><th>Property</th><th>Value</th></tr>
			<tr><td>Job ID</td><td><%=id %></td></tr>
			<tr><td>Job Name</td><td><%=jt.getJobName() %></td></tr>			
			<tr><td>Submission State</td><td><%=jt.getJobSubmissionState() %></td></tr>
			<tr><td>Working Directory</td><td><%=jt.getWorkingDirectory() %></td></tr>
			<tr><td>Standard Out</td><td><%=jt.getOutputPath() %></td></tr>				
			<tr><td>Merge err/out</td><td><%=jt.getJoinFiles() %></td></tr>			
			<tr><td>Remote Command</td><td><%=jt.getRemoteCommand() %></td></tr>
			<tr><td>Error Path</td><td><%=jt.getErrorPath() %></td></tr>
			<tr><td>Core Dump Available</td><td><%=info.hasCoreDump() %></td></tr>
			<tr><td>Working Dir Const</td><td><%=jt.WORKING_DIRECTORY %></td></tr>
			<tr><td>Home Dir Const</td><td><%=jt.HOME_DIRECTORY %></td></tr>
		</table>
<%		
	} catch (DrmaaException de) {
		out.write("<p>Grid Engine Error: " + de.getMessage() + "</p><br/><pre>");
		de.printStackTrace(new PrintWriter(out, true));
		out.write("</pre>");
	} finally {
		if(ses != null)
			ses.exit();
	}
%>	
</body>
</html>