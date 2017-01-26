<%@page trimDirectiveWhitespaces="true" %>
<%@page contentType="text/html" pageEncoding="UTF-8" import="org.starexec.constants.*" %>
<%@taglib prefix="star" tagdir="/WEB-INF/tags" %>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>

<%
	request.setAttribute("debug_mode", R.DEBUG_MODE_ACTIVE);
%>

<star:template title="Login" css="accounts/login">
	<script>
		$(document).ready(function(){
			$('button').button();
		});
	</script>
	<c:if test="${debug_mode}">
		<p id="debugWarning">Notice: StarExec is currently down for maintenance. Logging in will not be possible until
		StarExec is back online. Please try again later.</p>
	</c:if>


	<form method="POST" action="j_security_check" id="loginForm">
		<span id="uniqueLoginTag"></span>
		<fieldset id="loginFieldset">
			<legend>Credentials</legend>
			<table cellspacing="10px">
			<tr id="emailRow">
				<td class="label">Email</td>
				<td><input type="text" name="j_username" /></td>
			</tr>
			<tr>
				<td class="label">Password</td>
				<td><input type="password" name="j_password" /></td>
			</tr>
			<tr>
				<td><a href="${starexecRoot}/public/password_reset.jsp">Forgot password?</a></td>
				<td><button type="submit" id="loginButton">Login</button></td>
			</tr>
			<tr>
				<td><a href="${starexecRoot}/public/registration.jsp">New user?</a></td>
				<td></td>
			</tr>
		</table>
		</fieldset>
		<input type="hidden" id="cookieexists" name="cookieexists" value="false">
	</form>
	<c:if test="${not empty param.result and param.result == 'failed'}">
		<div class='error message'>Invalid username or password</div>
	</c:if>

</star:template>
