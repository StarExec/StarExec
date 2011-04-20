<%@ page language="java" contentType="text/html; charset=ISO-8859-1"
    pageEncoding="ISO-8859-1" import="com.starexec.constants.*, com.starexec.data.*, com.starexec.data.to.*, java.util.*"%>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
<head>
	<meta http-equiv="Content-Type" content="text/html; charset=ISO-8859-1">
	<title><%=T.UPLOAD %></title>
	<%@ include file="includes/jQuery.html" %>
	<link type="text/css" rel="StyleSheet" href="/starexec/css/uploadsolver.css" />
	<script type="text/javascript" src="/starexec/js/uploadsolver.js"></script>
</head>

<body>
	<div id="wrapper">
		<%@ include file="includes/header.html" %>
		<div class="content round">
			<img class='ul_icon' src="/starexec/images/icon_up.png"/>
			<h1>Solver Upload</h1>
			<a class="help" href="#">Help</a>
			
			<div class="solverUpForm">
				<form id="upForm" enctype="multipart/form-data" action="" method="POST">		
					<ol class="steps">
						<li class='step'>
							<label>Solver ZIP</label>
							<input id="uploadFile" name="<%=P.UPLOAD_FILE %>" type="file"/>
						</li>
						<li class='step'>
							<label>Solver Name</label>
							<input id="sName" type="text"/>
						</li>
						<li class='step'>
							<label>Solver Description</label>
							<textarea rows="6" cols="40" id="sDesc"></textarea>
						</li>
						<li class='step'>
							<label>Supported Divisions</label>
							<ul style="margin-top:10px;" id="levels">
							</ul>
						</li>
					</ol>														
					<a onclick="doSubmit()" class="btn ui-state-default ui-corner-all" id="btnSubmit"><span class="ui-icon ui-icon-circle-arrow-e"></span>Submit</a>
				</form>
			</div>			
		</div>		
	</div>
</body>
</html>