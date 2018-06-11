<%@page contentType="text/html" pageEncoding="UTF-8"
	import="java.text.SimpleDateFormat, java.sql.Date" %>
<%@taglib prefix="star" tagdir="/WEB-INF/tags" %>
<%
	String start = "";
	String end = "";
	try {
		/* This is simply a matter of checking that the input strings conform to
		 * the date format we are looking for.
		 * TODO: Replace this with a RegEx?
		 */
		java.util.Date startDate = new SimpleDateFormat("yyyy-MM-dd").parse(request.getParameter("start"));
		java.util.Date endDate = new SimpleDateFormat("yyyy-MM-dd").parse(request.getParameter("end"));
		start = (new Date(startDate.getTime())).toString();
		end = (new Date(endDate.getTime())).toString();
	} catch (Exception e) {} // Errors don't matter
	request.setAttribute("startDate", start);
	request.setAttribute(  "endDate", end);
%>
<star:template title="JobPair Errors"
               js=" common/format, lib/jquery.dataTables.min, lib/jquery.jstree, admin/jobpairErrors"
               css="common/table">
	<form id="dateselector">
		<label>Start <input type="date" placeholder="yyyy-mm-dd"
		                    pattern="\d{4}\-\d{1,2}\-\d{1,2}"
		                    name="start" value="${startDate}" /></label>
		<label>End <input type="date" placeholder="yyyy-mm-dd"
		                  pattern="\d{4}\-\d{1,2}\-\d{1,2}"
		                  name="end" value="${endDate}" /></label>
		<input type="submit" value="Update"/>
	</form>

	<fieldset class="expdContainer">
		<legend>Event Totals</legend>
		<table id="jobpairErrors"></table>
	</fieldset>
</star:template>

