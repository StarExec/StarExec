<%@taglib prefix="star" tagdir="/WEB-INF/tags" %>

<star:template title="password reset" css="accounts/password_reset" js="accounts/temp_pass">	
	<p>a temporary password has been generated for you, after you login with it you can change it</p>
	<form id="resetForm">	
	<fieldset>			
		<legend>password information</legend>
		<table>								
			<tr>
				<td class="label">temporary password:</td>
				<td><input id="temp_pass" type="text" readonly="readonly" value="${pwd}"/></td>
			</tr>
		</table>		
	</fieldset>	
	</form>
</star:template>