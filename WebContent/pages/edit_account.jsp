<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@taglib prefix="star" tagdir="/WEB-INF/tags" %>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>

<star:template title="edit account" css="edit_account" js="edit_account">
	<p>review and edit your account details here.</p>
	<fieldset>
		<legend>personal information</legend>
		<table id="personal">
			<tr>
				<th class="label">attribute</th>
				<th>current value</th>
			</tr>
			<tr>
				<td>first name </td>
				<td id="editfirstname">${user.firstName}</td>
			</tr>
			<tr>
				<td>last name</td>
				<td id="editlastname">${user.lastName}</td>
			</tr>
			<tr>
				<td>institution </td>
				<td id="editinstitution">${user.institution}</td>
			</tr>
			<tr>
				<td>email </td>
				<td id="editemail">${user.email}</td>
			</tr>
		</table>
		<h6>(click the current value of an attribute to edit it)</h6>
	</fieldset>
	<fieldset>
		<legend>associated websites</legend>
		<ul id="websites">
		</ul>
	</fieldset>
	<fieldset>
		<legend>password</legend>
		<table id="password">
			<!--  doesn't do anything yet -->
			<tr>
				<td>current password</td>
				<td><input type="password" id="current_pass" /></td>
			</tr>
			<tr>
				<td>new password</td>
				<td><input type="password" id="new_pass" /></td>
			</tr>
			<tr>
				<td>re-enter new password</td>
				<td><input type="password" id="confirm_pass" /></td>
			</tr>
			<tr>
				<td colspan="2"><button class="round" id="changePass">change</button></td>
			</tr>
		</table>
	</fieldset>
	<fieldset>
		<legend>profile picture</legend>
		<p>coming soon...</p>
	</fieldset>
</star:template>