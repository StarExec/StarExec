<%@page contentType="text/html" pageEncoding="UTF-8" %>
<%@taglib prefix="star" tagdir="/WEB-INF/tags" %>
<star:template title="Status"
               js="lib/jquery.dataTables.min, lib/jquery.validate.min, admin/status"
               css="common/table, details/shared, explore/common, admin/admin">
	<star:panel title="Status" withCount="false" expandable="false">
		<form>
			<table>
				<tbody>
					<tr>
						<td><label for="message">Message</label></td>
						<td><input  id="message" name="message" value="" /></td>
					</tr>
					<tr>
						<td><label for="url">More Info URL</label></td>
						<td><input  id="url" name="url" value="" type="url" /></td>
					</tr>
					<tr>
						<td><label for="enabled">Enabled?</label></td>
						<td><input  id="enabled" name="enabled" value="checked" type="checkbox" /></td>
					</tr>
				</tbody>
			</table>
			<input value="Submit" type="submit" />
		<form>
	</star:panel>
</star:template>
