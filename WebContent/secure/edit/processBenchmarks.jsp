<%@page contentType="text/html" pageEncoding="UTF-8" import="java.util.List, org.starexec.constants.*,org.starexec.data.security.*, java.lang.StringBuilder, java.io.File, org.apache.commons.io.FileUtils, org.starexec.data.database.*, org.starexec.data.to.*, org.starexec.util.*, org.starexec.constants.R" session="true"%>
<%@taglib prefix="star" tagdir="/WEB-INF/tags" %>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>

<%
try {
	int userId=SessionUtil.getUserId(request);
	int spaceId = Integer.parseInt((String)request.getParameter("sid"));

	// Grab relevant user id & processor info
	if (Users.isMemberOfSpace(userId,spaceId) || GeneralSecurity.hasAdminReadPrivileges(userId)) {
		request.setAttribute("sid",spaceId);
		List<Processor> procs=Processors.getByCommunity(Spaces.getCommunityOfSpace(spaceId),Processor.ProcessorType.BENCH);
		request.setAttribute("procs",procs);
	} else {
		response.sendError(HttpServletResponse.SC_FORBIDDEN, "You must be a member of the space in which you want to process benchmarks");
	}
	
	
} catch (Exception e) {
	response.sendError(HttpServletResponse.SC_BAD_REQUEST);
}
%>

<star:template title="process benchmarks" css="common/delaySpinner, common/table, edit/processBenchmarks, edit/shared" js=" common/delaySpinner, lib/jquery.validate.min, edit/processBenchmarks ">
	
	<form id="processBenchForm" method="post" action="${starexecRoot}/secure/process/benchmarks">
	<input type="hidden" name="sid" id="sid" value="${sid}"/>
	<fieldset>
		<legend>benchmark processors</legend>
		<table id="processorSelectionTable" class="shaded">
			<thead >
				<tr class="headerRow">
				<td>name</td>
				<td>description</td>
				</tr>
			</thead>
			<tbody>
				<c:forEach var="proc" items="${procs}">
					<tr>
						<td><input name="pid" type="hidden" value="${proc.id}"/><a href="${starexecRoot}/secure/edit/processor.jsp?id=${proc.id}">${proc.name} <img class="extLink" src="${starexecRoot}/images/external.png"/> </a></td>
						<td>${proc.description}</td>
					</tr>
				</c:forEach>
			</tbody>
		</table>
	</fieldset>
	<fieldset>
		<legend>options</legend>
		<table id="detailsTbl" class="shaded">
			<thead>
				<tr>
					<th class="label">attribute</th>
					<th>current value</th>
				</tr>
			</thead>
			<tbody>
				<tr title="Decide whether to process all the benchmarks in the hierarchy rooted at this space or process only this space" class="noHover">
					<td class="label">process hierarchy</td>
					<td>
						<div class="mobileBlock">
							<input type="radio" name="hier" id="hierTrue" value="true"/>full hierarchy
						</div>
						<div class="mobileBlock">
							<input type="radio" name="hier" id="hierFalse" checked="checked" value="false"/>this space
						</div>
					</td>
				</tr>
				<tr title="If yes, all attributes for all benchmarks being processed will be deleted before processing begins. If no, benchmarks will retain old attributes, and attribute values will be overwritten wherever the new
		 a new attribute has the same name as an old one" class="noHover">
					<td class="label">clear old attributes</td>
					<td>
						<div class="mobileBlock">
							<input type="radio" name="clear" id="clearOldTrue" value="true"/>yes
						</div>
						<div class="mobileBlock">
							<input type="radio" name="clear" id="clearOldFalse" checked="checked" value="false"/>no
						</div>
					</td>
				</tr>
				
			</tbody>	
		</table>
		
	</fieldset>
		<fieldset id="actionField">
		<legend>actions</legend>
		<button type="button" id="cancel">cancel</button>
		<button type="submit" id="process">process</button>
	</fieldset>
	</form>

</star:template>
