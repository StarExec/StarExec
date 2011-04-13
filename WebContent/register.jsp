<%@ page language="java" contentType="text/html; charset=ISO-8859-1"
    pageEncoding="ISO-8859-1" import="com.starexec.constants.*, com.starexec.data.*, com.starexec.data.to.*"%>
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=ISO-8859-1">
<title><%=T.REGISTER %></title>
<%@ include file="includes/jQuery.html" %>

<script type="text/javascript">
	function register(){
		$.post("Registration", { <%=P.USER_USERNAME %>: $('#iUsername').val(), <%=P.USER_PASSWORD %>: $('#iPassword').val(), 
								 <%=P.USER_AFILIATION %>: $('#iAfiliation').val(), <%=P.USER_EMAIL %>: $('#iEmail').val(), 
								 <%=P.USER_FIRSTNAME %>: $('#iFirstname').val(), <%=P.USER_LASTNAME %>: $('#iLastname').val() },
			   function(data) {
				 if(data.trim() == 'true')
			     	alert("Registration Successful!");
				 else
					 alert("Registration Failed");
			   }, "text"
	    );
	}
</script>

</head>
<body>
	<table>
		<tr>
			<td><label for="<%=P.USER_USERNAME %>">Username</label></td>
			<td><input id="iUsername" name="<%=P.USER_USERNAME %>" type="text"/></td>
		</tr>
		<tr>
			<td><label for="<%=P.USER_PASSWORD %>">Password</label></td>
			<td><input id="iPassword" name="<%=P.USER_PASSWORD %>" type="password"/></td>
		</tr>
		<tr>
			<td><label for="<%=P.USER_AFILIATION %>">Affiliation</label></td>
			<td><input id="iAfiliation" name="<%=P.USER_AFILIATION %>" type="text"/></td>
		</tr>
		<tr>
			<td><label for="<%=P.USER_EMAIL %>">Email Address</label></td>
			<td><input id="iEmail" name="<%=P.USER_EMAIL %>" type="text"/></td>
		</tr>
		<tr>
			<td><label for="<%=P.USER_FIRSTNAME %>">First Name</label></td>
			<td><input id="iFirstname" name="<%=P.USER_FIRSTNAME %>" type="text"/></td>
		</tr>
		<tr>
			<td><label for="<%=P.USER_LASTNAME %>">Last Name</label></td>
			<td><input id="iLastname" name="<%=P.USER_LASTNAME %>" type="text"/></td>
		</tr>
	</table>											
	
	<button onclick="register()">Register</button>
</body>
</html>