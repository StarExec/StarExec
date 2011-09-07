<%@ page language="java" contentType="text/html; charset=ISO-8859-1" pageEncoding="ISO-8859-1" import="com.starexec.constants.*"%>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
<head>
	<meta http-equiv="Content-Type" content="text/html; charset=ISO-8859-1">
	<title><%=T.VIEW_JOBS %></title>
	<%@ include file="/includes/jQuery.html" %>
	<link type="text/css" rel="StyleSheet" href="/starexec/css/viewjob.css" />
	<link type="text/css" rel="StyleSheet" href="/starexec/js/css/flexigrid.pack.css" />	
	<script type="text/javascript" src="/starexec/js/flexigrid.pack.js"></script>
	<script type="text/javascript" src="/starexec/js/viewjob.js"></script>	
</head>

<body>		
	<div id="wrapper">
		<jsp:include page="/includes/header.jsp" />
		<div class="content round">
			<img class='ul_icon' src="/starexec/images/icon_vjob.png"/>
			<h1>View Jobs</h1>
			<a class="help" href="#">Help</a>		
			<table id="jobs">
			</table>	
			
			<a onclick="showJobs()" class="btn ui-state-default ui-corner-all" id="btnBack"><span class="ui-icon ui-icon-circle-arrow-w left"></span>Back</a>		
		</div>		
	</div>
</body>
</html>