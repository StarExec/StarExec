<%@taglib prefix="star" tagdir="/WEB-INF/tags" %>

<star:template title="Password reset" css="accounts/password_reset" js="accounts/temp_pass">	
	<p>A temporary password has been generated for you - you can change it after you log in with it</p>
	<form id="resetForm">	
	<fieldset>			
		<legend>Password information</legend>
		<table>								
			<tr>
				<td class="label">Temporary password:</td>
				<td><input id="temp_pass" type="text" readonly="readonly" value="${pwd}"/></td>
			</tr>
		</table>		
	</fieldset>	
	</form>
</star:template>