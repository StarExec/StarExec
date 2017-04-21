<%@page contentType="text/html" pageEncoding="UTF-8" import="org.starexec.data.database.*, org.starexec.data.to.*, org.starexec.util.*, org.starexec.data.to.enums.ProcessorType"%>
<%@taglib prefix="star" tagdir="/WEB-INF/tags" %>
<star:template title="Analytics" js="lib/jquery.dataTables.min, lib/jquery.jstree, admin/analytics" css="common/table">

<form id="dateselector">
	<label>Start <input type="date" placeholder="yyyy-mm-dd" pattern="\d{4}\-\d{1,2}\-\d{1,2}" name="start" /></label>
	<label>End   <input type="date" placeholder="yyyy-mm-dd" pattern="\d{4}\-\d{1,2}\-\d{1,2}" name="end" /></label>
	<input type="submit" value="Update" />
</form>

<fieldset>
	<legend>Event Totals</legend>
	<table id="analytics_results">
		<thead><tr>
			<th>Event</th>
			<th>Count</th>
		</tr></thead>
		<tbody></tbody>
	</table>
</fieldset>

</star:template>

