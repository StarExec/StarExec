<%@page import="org.apache.tomcat.util.http.Parameters"%>
<%@page import="org.apache.jasper.tagplugins.jstl.core.Param"%>
<%@ page language="java" contentType="text/html; charset=ISO-8859-1" pageEncoding="ISO-8859-1" import="com.starexec.constants.*"%>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
<head>	
	<meta http-equiv="Content-Type" content="text/html; charset=ISO-8859-1">
	<title><%=T.LOGIN %></title>
	<%@ include file="/includes/jQuery.html" %>
	<link type="text/css" rel="StyleSheet" href="/starexec/css/maincontent.css" />
	<link type="text/css" rel="StyleSheet" href="/starexec/css/login.css" />
</head>

<body>
	<div id="wrapper">
		<jsp:include page="/includes/header.jsp" />
		<div class="content round">
			<h1>Login</h1>
			<a class="help" href="#">Help</a>
						
			<form method="POST" action="j_security_check" id="loginForm">			
				<table cellspacing="10">					
					<tr>
						<td class="login_label">Email </td>
						<td><input type="text" name="j_username" /></td>
					</tr>
					<tr>
						<td class="login_label">Password </td>
						<td><input type="password" name="j_password"/ ></td>
					</tr>
					<c:if test="${param.result != 'null' && not empty param.result}">
						<tr>
							<td colspan="2"><p class='loginError'>* Invalid username or password</p></td>
						</tr>
					</c:if>					
					<tr>
						<td colspan="2"><a onclick="$('#loginForm').submit()" class="btn ui-state-default ui-corner-all" id="btnLogin"><span class="ui-icon ui-icon-locked right"></span>Login</a></td>
					</tr>
				</table>				
			</form>					
		</div>		
	</div>
</body>
</html>