<%@page contentType="text/html" pageEncoding="UTF-8" import="java.util.List, org.starexec.constants.*, java.lang.StringBuilder, java.io.File, org.apache.commons.io.FileUtils, org.starexec.data.database.*, org.starexec.data.to.*, org.starexec.util.*, org.starexec.constants.R" session="true"%>
<%@taglib prefix="star" tagdir="/WEB-INF/tags" %>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>

<%
try {
	// Grab relevant user id and community id
	int comId = Integer.parseInt((String)request.getParameter("id"));
	int userId = SessionUtil.getUserId(request);
	request.setAttribute("comId",comId);
	// Only allowing editing of the default benchmark if the user
	// is a leader of the community being edited
	List<User> leaders=Spaces.getLeaders(comId);
	boolean validUser=false;
	for (User x : leaders) {
		if (x.getId()==userId) {
			validUser=true;
			break;
		}
	}
	// The user does not have permission
	if(!validUser) {
		response.sendError(HttpServletResponse.SC_NOT_FOUND, "only community leaders can select default benchmarks");
	}
} catch (Exception e) {
	response.sendError(HttpServletResponse.SC_BAD_REQUEST);
}
%>

<star:template title="select default benchmark" css="common/delaySpinner, edit/defaultBenchmark, edit/shared, common/table" js="common/delaySpinner, lib/jquery.cookie, lib/jquery.dataTables.min, lib/jquery.validate.min, edit/defaultBenchmark">
	<span style="display:none;" id="cid" value="${comId}"></span>
	<fieldset>
	<form id="selectDefaultBench">
		<fieldset>
			<legend class="expd" id="benchExpd"><span>0</span> benchmarks</legend>
			<table id="benchmarks">
				<thead>
					<tr>
						<th style="width:75%;">name</th>
						<th>type</th>											
					</tr>
				</thead>		
			</table>	
		</fieldset>
		<button type="button" id="cancel">cancel</button>
		<button type="button" id="update">update</button>
		</form>
	</fieldset>
</star:template>