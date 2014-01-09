<%@page contentType="text/html" pageEncoding="UTF-8"%>	
<%@taglib prefix="star" tagdir="/WEB-INF/tags" %>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>

<star:template title="login" css="accounts/login">	
	<script>
		$(document).ready(function(){
			$('button').button();
		});
	</script>
        <br>
	<p>StarExec is in read-only mode for a security scan, January 9, 2014.</p>
	<br>
	<form method="POST" action="j_security_check" id="loginForm">	
		<span id="uniqueLoginTag"></span>		
		<fieldset>
			<legend>credentials</legend>
			<table cellspacing="10px">								
			<tr>
				<td class="label">email </td>
				<td><input type="text" name="j_username" /></td>
			</tr>
			<tr>
				<td class="label">password </td>
				<td><input type="password" name="j_password" /></td>
			</tr>												
			<tr>
				<td><a href="/${starexecRoot}/public/password_reset.jsp">forgot password?</a></td>
				<td><button type="submit">login</button></td>
			</tr>
		</table>
		</fieldset>	
		<input type="hidden" name="cookieexists" value="false">				
	</form>
	<body onload="cc()">
	<c:if test="${not empty param.result and param.result == 'failed'}">
		<div class='error message'>invalid username or password</div>
	</c:if>	
	
		
	<script language="JavaScript">
		<!--
		function cc()
		{
		 /* check for a cookie */
		  if (document.cookie == "") 
		  {
		    /* if a cookie is not found - alert user -
		     change cookieexists field value to false */
		    alert("Cookies need to be enabled");
		
		    /* If the user has Cookies disabled an alert will let him know 
		        that cookies need to be enabled to log on.*/ 
		
		    document.Form1.cookieexists.value ="false"  
		  } else {
		   /* this sets the value to true and nothing else will happen,
		       the user will be able to log on*/
		    document.Form1.cookieexists.value ="true"
		  }
		}
		
		/* Set a cookie to be sure that one exists.
		   Note that this is outside the function*/
		document.cookie = 'killme' + escape('nothing')
		// -->
		</script>
	
</star:template>