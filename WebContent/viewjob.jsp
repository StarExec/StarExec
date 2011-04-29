<%@ page language="java" contentType="text/html; charset=ISO-8859-1"
    pageEncoding="ISO-8859-1" import="com.starexec.constants.*, com.starexec.data.*, com.starexec.data.to.*, java.util.*"%>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
<head>
	<meta http-equiv="Content-Type" content="text/html; charset=ISO-8859-1">
	<title>View Jobs</title>
	<%@ include file="includes/jQuery.html" %>
	<link type="text/css" rel="StyleSheet" href="/starexec/css/viewjob.css" />	
	<script type="text/javascript" src="/starexec/js/jquery.jstree.js"></script>
	<script type="text/javascript" src="/starexec/js/viewjob.js"></script>	
</head>

<body>		
	<div id="wrapper">
		<%@ include file="includes/header.html" %>
		<div class="content round">
			<img class='ul_icon' src="/starexec/images/icon_vjob.png"/>
			<h1>View Jobs</h1>
			<a class="help" href="#">Help</a>		
			<table id="jobs">
				<tr>
					<th>Job #</th>
					<th>Status</th>
					<th>Submitted</th>
					<th>Completed</th>
					<th>Node</th>
					<th>Timeout</th>
				</tr>
			</table>			
		</div>		
	</div>
</body>
</html>