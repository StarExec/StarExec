<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@taglib prefix="star" tagdir="/WEB-INF/tags" %>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>

<star:template title="login" css="login">	
	<form method="POST" action="j_security_check" id="loginForm">			
		<fieldset>
			<legend>credentials</legend>
			<table cellspacing="10px">								
			<tr>
				<td class="lable">email </td>
				<td><input type="text" name="j_username" /></td>
			</tr>
			<tr>
				<td class="lable">password </td>
				<td><input type="password" name="j_password" /></td>
			</tr>												
			<tr>
				<td colspan="2"><button type="submit" class="round">login</button></td>
			</tr>
		</table>
		</fieldset>					
	</form>
	<c:if test="${not empty param.result}">			
		<div class='error message'>invalid username or password</div>
	</c:if>			
</star:template>