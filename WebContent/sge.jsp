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
		jt.setWorkingDirectory("/home/starexec");
		String id = ses.runJob(jt);
		out.write("<p>Job submitted with job ID: " + id + "</p>");				
		
		JobInfo info = ses.wait(id, Session.TIMEOUT_WAIT_FOREVER);		
		out.write("<p>Job submission state: " + jt.getJobSubmissionState() + "</p>");
		out.write("<p>Job exited: " + info.hasExited() + "</p>");
		out.write("<p>Job aborted: " + info.wasAborted() + "</p>");
		ses.deleteJobTemplate(jt);
		
		if(info.hasExited())
			out.write("<p>Job exit status: " + info.getExitStatus() + "</p>");
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