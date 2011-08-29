<%@ page language="java" contentType="text/html; charset=ISO-8859-1" pageEncoding="ISO-8859-1" import="com.starexec.constants.*"%>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
<head>
	<meta http-equiv="Content-Type" content="text/html; charset=ISO-8859-1">
	<title><%=T.DASHBOARD %></title>
	<%@ include file="includes/jQuery.html" %>
	<link type="text/css" rel="StyleSheet" href="/starexec/css/maincontent.css" />
</head>

<body>
	<div id="wrapper">
		<%@ include file="includes/header.html" %>
		<div class="content round">
			<h1>Dashboard</h1>
			<a class="help" href="#">Help</a>
			
			<h2 style="text-align: center; margin-top:100px;">Welcome To <%=T.APP_TITLE %></h2>			
		</div>		
	</div>
</body>
</html>