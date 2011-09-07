<%@page import="org.apache.tomcat.util.http.Parameters"%>
<%@page import="org.apache.jasper.tagplugins.jstl.core.Param"%>
<%@ page language="java" contentType="text/html; charset=ISO-8859-1" pageEncoding="ISO-8859-1" import="com.starexec.constants.*"%>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
<head>
	<% session.invalidate(); %>
	<meta http-equiv="Content-Type" content="text/html; charset=ISO-8859-1">
	<title><%=T.LOGOUT %></title>
	<%@ include file="/includes/jQuery.html" %>
	<link type="text/css" rel="StyleSheet" href="/starexec/css/maincontent.css" />	
</head>

<body>
	<div id="wrapper">
		<jsp:include page="/includes/header.jsp" />
		<div class="content round">
			<h1>Thank You</h1>
			<a class="help" href="#">Help</a>
						
			<p style="text-align: center; margin-top: 100px;">You have been successfully logged out.</p>					
		</div>		
	</div>
</body>
</html>