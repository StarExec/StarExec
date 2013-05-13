<%@page import="org.starexec.constants.*" %>
<%@taglib prefix="star" tagdir="/WEB-INF/tags" %>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>

<%
	request.setAttribute("firstNameLen", R.USER_FIRST_LEN);
	request.setAttribute("lastNameLen", R.USER_LAST_LEN);
	request.setAttribute("emailLen", R.EMAIL_LEN);
%>

<star:template title="password reset" css="accounts/password_reset" js="lib/jquery.validate.min, accounts/password_reset">	
	<p>enter your credentials to reset your password</p>
	<form method="POST" action="/${starexecRoot}/public/reset_password" id="resetForm">	
	<fieldset>			
		<legend>credentials</legend>
		<table class="shaded">
			<thead>
				<tr>
					<th>attribute</th>
					<th id="value_header">value</th>
				</tr>
			</thead>	
			<tbody>
				<tr>
					<td class="label">first name: </td>
					<td><input id="firstname" type="text" name="fn" maxlength="${firstNameLen}"/></td>
				</tr>
				<tr>
					<td class="label">last name: </td>
					<td><input id="lastname" type="text" name="ln" maxlength="${lastNameLen}"/></td>
				</tr>
				<tr>
					<td class="label">email: </td>
					<td><input id="email" type="text" name="em" maxlength="${emailLen}"/></td>
				</tr>
				<tr>
					<td colspan="2"><button type="submit" id="submit" value="Submit">reset</button></td>
				</tr>
			</tbody>
		</table>		
	</fieldset>	
	</form>
	<c:if test="${not empty param.result and param.result == 'success'}">			
		<div class='success message'>an email has been sent to you to complete the password reset process</div>
	</c:if>
	<c:if test="${not empty param.result and param.result == 'noUserFound'}">			
		<div class='error message'>sorry, those credentials do not match our records</div>
	</c:if>
	<c:if test="${not empty param.result and param.result == 'expired'}">			
		<div class='error message'>sorry, the link you are trying to access has expired and no inter exists</div>
	</c:if>
</star:template>