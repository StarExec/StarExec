<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@taglib prefix="star" tagdir="/WEB-INF/tags" %>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>

<star:template title="edit account" css="edit_account" js="edit_account">
	<p>review and edit your account details here.</p>
	<fieldset>
		<legend>personal information</legend>
		<table>
			<tr>
				<th>attribute</th>
				<th>current value</th>
			</tr>
			<tr class="shade">
				<td class="label">first name </td>
				<td id="editfirstname">${usr.firstName}</td>
			</tr>
			<tr>
				<td>last name</td>
				<td id="editlastname">${usr.lastName}</td>
			</tr>
			<tr class="shade">
				<td>institution </td>
				<td id="editinstitution">${usr.institution}</td>
			</tr>
			<tr>
				<td>email </td>
				<td id="editemail">${usr.email}</td>
			</tr>
		</table>
		<h6>(click the current value of an attribute to edit it)</h6>
	</fieldset>
	<fieldset>
		<legend>associated websites</legend>
		<table id="websites">
			<tr>
				<th class="label">name</th>
				<th>url</th>
			</tr>
		</table>
		<!--  TODO make the information editable -->
		<!-- <h6>(click the url to edit or delete it)</h6> -->
	</fieldset>
	<fieldset>
		<legend>password</legend>
		<table>
			<!--  doesn't do anything yet -->
			<tr class="shade">
				<td>current password</td>
				<td><input type="password" name="current_pass" /></td>
			</tr>
			<tr>
				<td>new password</td>
				<td><input type="password" name="new_pass1" /></td>
			</tr>
			<tr class="shade">
				<td>re-enter new password</td>
				<td><input type="password" name="new_pass2" /></td>
			</tr>
			<tr>
				<td colspan="2"><button class="round" id="changePass">submit change</button></td>
			</tr>
		</table>
	</fieldset>
	<fieldset>
		<legend>profile picture</legend>
		<p>coming soon...</p>
	</fieldset>
</star:template>