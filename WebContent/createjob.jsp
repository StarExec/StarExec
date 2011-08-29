<%@ page language="java" contentType="text/html; charset=ISO-8859-1" pageEncoding="ISO-8859-1" import="com.starexec.constants.*"%>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
<head>
	<meta http-equiv="Content-Type" content="text/html; charset=ISO-8859-1">
	<title><%=T.CREATE_JOB %></title>
	<%@ include file="includes/jQuery.html" %>
	<link type="text/css" rel="StyleSheet" href="/starexec/css/createajob.css" />	
	<link type="text/css" rel="StyleSheet" href="/starexec/css/maincontent.css" />
	<script type="text/javascript" src="/starexec/js/jquery.jstree.js"></script>
	<script type="text/javascript" src="/starexec/js/createajob.js"></script>
</head>

<body>		
	<div id="wrapper">
		<%@ include file="includes/header.html" %>
		<div class="content round">
			<img class='ul_icon' src="/starexec/images/icon_addjob.png"/>
			<h1>Job Creator</h1>
			<a class="help" href="#">Help</a>
			
			<div class="createJobForm">
				<form id="jobForm" action="" method="POST">		
					<ol class="steps">
						<li class='step'>
							<label>Select Solvers</label>
							<ul id="solverList"></ul>
						</li>
						<li class='step'>
							<label>Select Benchmarks</label>
							<ul id="benchList"></ul>
						</li>					
					</ol>														
																											
					<a onclick="doSubmit()" class="btn ui-state-default ui-corner-all" id="btnSubmit"><span class="ui-icon ui-icon-circle-arrow-e right"></span>Submit</a>					
				</form>
			</div>			
		</div>		
	</div>
</body>
</html>