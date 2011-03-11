<%@ page language="java" contentType="text/html; charset=ISO-8859-1"
    pageEncoding="ISO-8859-1" import="constants.*, data.*, data.to.*, java.util.*"%>    
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=ISO-8859-1">
<title><%=T.UPLOAD %></title>
<%@ include file="includes/jQuery.html" %>
<jsp:useBean id="uploader" class="beans.UploadBean" scope="request"/>
<jsp:setProperty name="uploader" property="isBenchmark" value="true"/>
</head>
<body>
<table>
<tr><th>ID</th><th>Name</th><th>Path</th></tr>
<%
	uploader.doUpload(request);
	for(Benchmark b : uploader.getUploadedBenchmarks()) {
%>		
	<tr><td><%=b.getId() %></td><td><%=b.getFileName() %></td><td><%=b.getPath() %></td></tr>
<%
	}
%>
</table>

</body>
</html>