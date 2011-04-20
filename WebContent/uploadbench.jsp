<%@ page language="java" contentType="text/html; charset=ISO-8859-1"
    pageEncoding="ISO-8859-1" import="com.starexec.constants.*, com.starexec.data.*, com.starexec.data.to.*, java.util.*"%>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=ISO-8859-1">
<title><%=T.UPLOAD %></title>
<%@ include file="includes/jQuery.html" %>

<jsp:useBean id="uploader" class="com.starexec.beans.UploadBean" scope="page"/>
<jsp:setProperty name="uploader" property="isBenchmark" value="true"/>

<style type="text/css">
* {
	padding: 0px; 
	margin: 0px; 
}

body {
	background-color: #fff; 
	background-image: url('/starexec/images/background.png');
	background-repeat: repeat-x;
	font-family: sans-serif;
}

#header {
	background-image: url('/starexec/images/header.png');
	background-repeat: repeat-x;
	width: 100%;
}

#wrapper {
	width: 1280px;
	min-height: 100%;
	height: 100%;	
	margin-left: auto;
	margin-right: auto;	
	position: relative;
}

#content {	
	width: 100%;
	min-height: 894px;
}

#badge {
	width: 204px;
	height: 124px;
	background-image: url('/starexec/images/badge.png');
	background-repeat: no-repeat;
	position:relative;
	left: 100px;
}

#nav {
	position: absolute;
	height: 43px;
	top: 81px;
	left: 330px;
}

#nav li {
	list-style: none;
	position: absolute;
	top: 0px;
	background-image: url('/starexec/images/btn_inactive.png');
	background-repeat: no-repeat;
	height: 43px;
	width: 112px;
}

#nav li, #nav a {
	height: 43px;
	display: block;	
	text-decoration: none;
	text-align: center;
	text-shadow: 0.04em 0.04em 0.03em #eeeeee;	
}

#nav li span {	
	font-size: 18px;		
	margin-top: 10px;
	display:inline-table;		
	color: #323232;
}

#dashboard {
	left: 0px;
	width: 112px;
}
#solver {
	left: 122px;
	width: 112px;
}
#benchmark 
{
	left: 244px;
	width: 112px;	
}
#job {	
	left: 366px;
	width: 112px;
}

#nav li a:hover {background: url('/starexec/images/btn_hover.png');}
#nav li a:active {background: url('/starexec/images/btn_press.png');}
#nav li a.current {background: url('/starexec/images/btn_active.png');}

#user {
	position: absolute;
	right: 10px;
	top: 20px;
	color: #ffffff;
	text-align: right;
	font-size: 12px;
}

#user .username {
	font-weight: bold;
	font-size: 14px;
}

#user a {
	color: #323232;
}

#user ul {
	list-style: none;
	float: right;	
	display:block;
	clear: left;
	margin-top: 5px;
}


#user li {	
	float: left;
	margin-left: 8px;
}

div .content {
	width: 815px;
	height: 299px;
	background-image: url('/starexec/images/content_medium.png');
	background-repeat: no-repeat;
	margin-left: auto;
	margin-right: auto;
	margin-top: 80px;
	position:relative;
}

div .content .ul_icon {
	position: absolute;
	top: -12px;
	left: -10px;
}

div .content h1 {	
	position:relative;	
	top: 7px;
	margin-left: 60px;
	font-size: 24px;
	text-shadow: 0.04em 0.04em 0.03em #eeeeee;
	color: #323232;
}

.benchUpForm {
	margin-top: 95px;
	width: 300px;
	margin-left: auto;
	margin-right: auto;
	font-weight: bold;
}

#btnSubmit {
	position: absolute;
	bottom: 10px;
	right: 10px;
}

.fg-button { 
   outline: 0; 
   margin:0 4px 0 0; 
   padding: .4em 1em; 
   text-decoration:none !important; 
   cursor:pointer; 
   position: relative; 
   text-align: center; 
   zoom: 1; 
}

.fg-button span { 
   float: right;
   margin-left: 5px;
   margin-top: 2px; 
}

</style>
</head>

<body>
	<div id="wrapper">
		<div id="header">
			<div id="badge"></div>			
			<ul id="nav">
				<li id="dashboard"><a href="#"><span>Dashboard</span></a></li>
				<li id="solver"><a href="#"><span>Solvers</span></a></li>
				<li id="benchmark"><a href="#"><span>Benchmarks</span></a></li>
				<li id="job"><a href="#"><span>Jobs</span></a></li>
			</ul>
			<div id="user">
				<div class="greeting">
					<span>Hello, </span>
					<span class="username">Tyler Jensen</span>
				</div>
				<ul>
					<li><a href="#">Settings</a></li>
					<li>|</li>
					<li><a href="#">Logout</a></li>
				</ul>
			</div>
		</div>
		<div class="content">
			<img class='ul_icon' src="/starexec/images/icon_up.png"/>
			<h1>Benchmark Upload</h1>
			<div class="benchUpForm">
				<form id="upForm" enctype="multipart/form-data" action="UploadBench" method="POST">		
					<label>Zip File</label>
					<input id="uploadFile" name="<%=P.UPLOAD_FILE %>" type="file"/>				
					<a onclick="$('#upForm').submit()" class="fg-button ui-state-default ui-corner-all" id="btnSubmit"><span class="ui-icon ui-icon-circle-arrow-e"></span>Submit</a>
				</form>
			</div>			
		</div>		
	</div>
</body>
</html>