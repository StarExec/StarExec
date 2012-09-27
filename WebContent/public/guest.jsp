<%@page contentType="text/html" pageEncoding="UTF-8"%>	
<%@taglib prefix="star" tagdir="/WEB-INF/tags" %>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>

<star:template title="login" css="accounts/login" >	
	<script>
		$(document).ready(function(){
			$('button').button();
		});
	</script>
	<form method="POST" action="j_security_check" id="loginForm">			
		<fieldset>
			<legend>credentials</legend>
			<table cellspacing="10px">								
			<tr>
				<td class="label">email </td>
				<td><input id="j_userName" type="text" name="j_username" value="public"/> </td>
			</tr>
			<tr>
				<td class="label">password </td>
				<td><input id="j_password" type="password" value="public" name="j_password" /></td>
			</tr>												
			<tr>
				<td></td>
				<td><button type="submit">login</button></td>
			</tr>
		</table>
		</fieldset>					
	</form>
	<c:if test="${not empty param.result and param.result == 'failed'}">
		<div class='error message'>invalid username or password</div>
	</c:if>			
	
</star:template>