<%@ page language="java" contentType="text/html; charset=ISO-8859-1"
    pageEncoding="ISO-8859-1" import="com.starexec.constants.*, com.starexec.data.*, com.starexec.data.to.*, java.util.*"%>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
<head>
	<meta http-equiv="Content-Type" content="text/html; charset=ISO-8859-1">
	<title><%=T.UPLOAD %></title>
	<%@ include file="includes/jQuery.html" %>
	<link type="text/css" rel="StyleSheet" href="/starexec/css/uploadbench.css" />
</head>

<body>
	<div id="wrapper">
		<%@ include file="includes/header.html" %>
		<div class="content round">
			<img class='ul_icon' src="/starexec/images/icon_up.png"/>
			<h1>Benchmark Upload</h1>
			<a class="help" href="#">Help</a>
			
			<div class="benchUpForm">
				<form id="upForm" enctype="multipart/form-data" action="UploadBench" method="POST">		
					<label>Zip File</label>
					<input id="uploadFile" name="<%=P.UPLOAD_FILE %>" type="file"/>		
					<a onclick="$('#upForm').submit()" class="btn ui-state-default ui-corner-all" id="btnSubmit"><span class="ui-icon ui-icon-circle-arrow-e"></span>Submit</a>
				</form>
			</div>			
		</div>		
	</div>
</body>
</html>