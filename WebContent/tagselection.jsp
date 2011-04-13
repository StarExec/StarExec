<%@ page language="java" contentType="text/html; charset=ISO-8859-1"
    pageEncoding="ISO-8859-1" import="com.starexec.constants.*, com.starexec.data.*, com.starexec.data.to.*, java.util.*"%>    
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=ISO-8859-1">
<title><%=T.UPLOAD %></title>
<%@ include file="includes/jQuery.html" %>
<jsp:useBean id="uploader" class="com.starexec.beans.UploadBean" scope="page"/>
<jsp:setProperty name="uploader" property="isBenchmark" value="true"/>
</head>
<body>

</body>
</html>