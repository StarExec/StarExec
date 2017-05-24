<%@page contentType="text/html" pageEncoding="UTF-8" import="org.starexec.data.database.*, org.starexec.data.to.*, org.starexec.util.*, org.starexec.data.to.enums.ProcessorType"%>
<%@taglib prefix="star" tagdir="/WEB-INF/tags" %>
<star:template title="Analytics" js="lib/jquery.dataTables.min, lib/jquery.jstree, admin/analytics" css="common/table, admin/analytics">

<script src="https://d3js.org/d3.v4.min.js"></script>

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
			<th>Users</th>
		</tr></thead>
		<tbody></tbody>
	</table>
</fieldset>

<fieldset>
	<legend>Timeline</legend>
	<div id="analytics_timeline"></div>
</fieldset>

</star:template>

