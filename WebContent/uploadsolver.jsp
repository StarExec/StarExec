<%@ page language="java" contentType="text/html; charset=ISO-8859-1"
    pageEncoding="ISO-8859-1" import="constants.*, data.*, data.to.*, java.util.*"%>
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<%@taglib prefix="c" uri="http://java.sun.com/jstl/core" %>

<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=ISO-8859-1">
<title><%=T.UPLOAD %></title>
<%@ include file="includes/jQuery.html" %>

<script type="text/javascript">
	$(document).ready(function(){
		$.ajax( {
			type:'Get',
			url:'/starexec/services/echo/whats up',
			success:function(data) {
			 	alert(data);
			},
			error:function(error) {
				alert("Error!");
			}
		});		
	});
</script>

</head>
<body>
	<form id="upForm" enctype="multipart/form-data" action="UploadSolver" method="POST">
		<h2>Solver Upload</h2>
		<label>Solver ZIP</label>
		<input id="uploadFile" name="<%=P.UPLOAD_FILE %>" type="file"/>											
		<input type="submit"/>
	</form>
</body>
</html>