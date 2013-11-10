<%@page contentType="text/html" pageEncoding="UTF-8" import="java.util.List, org.starexec.constants.*, java.lang.StringBuilder, java.io.File, org.apache.commons.io.FileUtils, org.starexec.data.database.*, org.starexec.data.to.*, org.starexec.util.*, org.starexec.constants.R" session="true"%>
<%@taglib prefix="star" tagdir="/WEB-INF/tags" %>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>

<%
try {
	// Grab relevant user id & processor info
	int spaceId = Integer.parseInt((String)request.getParameter("sid"));
	request.setAttribute("sid",spaceId);
	List<Processor> procs=Processors.getByCommunity(Spaces.GetCommunityOfSpace(spaceId),Processor.ProcessorType.BENCH);
	request.setAttribute("procs",procs);
	
} catch (Exception e) {
	response.sendError(HttpServletResponse.SC_BAD_REQUEST);
}
%>

<star:template title="process benchmarks" css="common/delaySpinner, common/table, edit/processBenchmarks, edit/shared" js="jquery.cookie.js, common/delaySpinner, lib/jquery.validate.min, edit/processBenchmarks ">
	
	<form id="processBenchForm" method="post" action="/${starexecRoot}/secure/process/benchmarks">
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
						<td><input name="pid" type="hidden" value="${proc.id}"/><a href="/${starexecRoot}/secure/edit/processor.jsp?type=bench&id=${proc.id}">${proc.name} <img class="extLink" src="/${starexecRoot}/images/external.png"/> </a></td>
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
				<tr class="noHover">
					<td class="label">process hierarchy</td>
					<td><input type="radio" name="hier" id="hierTrue" value="true"/>full hierarchy <input type="radio" name="hier" id="hierFalse" checked="checked" value="false"/>this space</td>
				</tr>
				<tr class="noHover">
					<td class="label">clear old attributes</td>
					<td><input type="radio" name="clear" id="clearOldTrue" value="true"/>yes <input type="radio" name="clear" id="clearOldFalse" checked="checked" value="false"/>no</td>
				</tr>
				
			</tbody>	
		</table>
		
	</fieldset>
		<fieldset id="actionField">
		<legend>actions</legend>
		<button type="submit" id="process">process</button>
		<button type="button" id="cancel">cancel</button>
	</fieldset>
	</form>

</star:template>