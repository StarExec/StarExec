<%@ page language="java" contentType="text/html; charset=ISO-8859-1"
    pageEncoding="ISO-8859-1" import="com.starexec.data.*, com.starexec.data.to.*, com.starexec.manage.*, java.util.*"%>
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=ISO-8859-1">
<title>Manager Tester</title>
</head>

<%!
	Date date = new Date();
	User usr = new User("admin");
	Jobject job = new Jobject(usr);	// Where the job is stored
	SolverLink lnk;	// A subjob (1 solver and 1 or more benchmarks)
%>

<body>
	<h1>Manager Tester!</h1>
	<p>This will run the manager with default solver and benchmark and dump a file in /home/starexec/jobin. Woo!</p>
	<P>Last run at <%= date %></P>
	<p>I'm adding solver ID 1 and associating benchmark ID 1 with it, then passing the jobject to the JobManager.</p>
	<% 
	try {
		lnk = job.addSolver(1);
		lnk.addBenchmark(1);
		
		JobManager.doJob(job);
	} catch(Exception e) {
		out.write("<h3 style='color: red;'>Exception\n" + e + "\n</h3>");
	}
	%>
</body>
</html>