<%@ page language="java" contentType="text/html; charset=ISO-8859-1"
    pageEncoding="ISO-8859-1" import="constants.*, data.*, data.to.*, java.util.*"%>
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=ISO-8859-1">
<title><%=T.UPLOAD %></title>
<%@ include file="includes/jQuery.html" %>

<script type="text/javascript">
	
</script>

</head>
<body>
	<form id="upForm" enctype="multipart/form-data" action="tagselection.jsp" method="POST">
		<h2>File Upload</h2>
		<%
			// Check to see if the upload is a success if this is a post-back
			String success = request.getParameter("s");			
			if(success != null && Boolean.parseBoolean(success)){
				out.write("<h3 style='color: green;'>Success</h3>");			
			} else if(success != null) {
				out.write("<h3 style='color: red;'>Failure</h3>");
			}		
		%>
		
		<label>File</label>
		<input id="uploadFile" name="<%=P.UPLOAD_FILE %>" type="file"/>									
		<!-- <input id="uploadFile" name="<%=P.UPLOAD_FILE %>" type="file"/>-->
		<input type="submit"/>
	</form>
</body>
</html>