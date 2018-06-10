<%@page contentType="text/html" pageEncoding="UTF-8"
%>
<%@taglib prefix="star" tagdir="/WEB-INF/tags" %>
<star:template title="Analytics"
               js=" common/format, lib/jquery.dataTables.min, lib/jquery.jstree, admin/jobpairErrors"
               css="common/table">

	<form id="dateselector">
		<label>Start <input type="date" placeholder="yyyy-mm-dd"
		                    pattern="\d{4}\-\d{1,2}\-\d{1,2}"
		                    name="start"/></label>
		<label>End <input type="date" placeholder="yyyy-mm-dd"
		                  pattern="\d{4}\-\d{1,2}\-\d{1,2}" name="end"/></label>
		<input type="submit" value="Update"/>
	</form>

	<fieldset>
		<legend>Event Totals</legend>
		<table id="jobpairErrors"></table>
	</fieldset>

</star:template>

