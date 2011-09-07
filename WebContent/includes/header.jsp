<%@page import="org.apache.tomcat.util.http.Parameters"%>
<%@page import="org.apache.jasper.tagplugins.jstl.core.Param"%>
<%@page import="com.starexec.data.to.*"%>
<%@ page language="java" contentType="text/html; charset=ISO-8859-1" pageEncoding="ISO-8859-1" import="com.starexec.constants.*"%>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<div id="header">
	<div id="badge"></div>			
	<ul id="nav">
		<li id="dashboard"><a href="/starexec/pages/dashboard.jsp"><span>Dashboard</span></a></li>
		<li id="solver"><a href="/starexec/pages/uploadsolver.jsp"><span>Solvers</span></a></li>
		<li id="benchmark"><a href="/starexec/pages/uploadbench.jsp"><span>Benchmarks</span></a></li>
		<li id="cjob"><a href="/starexec/pages/createjob.jsp"><span>Create Job</span></a></li>
		<li id="vjob"><a href="/starexec/pages/viewjob.jsp"><span>View Jobs</span></a></li>
	</ul>
	<c:if test="<%= session.getAttribute(P.SESSION_USER) != null %>">
		<div id="user">
			<div class="greeting">
				<span>Hello, </span>
				<span class="username">
					<% 
						if(session.getAttribute(P.SESSION_USER) != null) {
							out.write(((User)session.getAttribute(P.SESSION_USER)).getFullName()); 						
						}
					%>
				</span>
			</div>
			<c:if test=""></c:if>
			<ul>
				<li><a href="#">Edit Account</a></li>
				<li>|</li>
				<li><a href="/starexec/logout.jsp">Logout</a></li>
			</ul>
		</div>
	</c:if>
</div>