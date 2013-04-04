<%@page contentType="text/html" pageEncoding="UTF-8" import="java.util.List, org.starexec.constants.*, java.lang.StringBuilder, java.io.File, org.apache.commons.io.FileUtils, org.starexec.data.database.*, org.starexec.data.to.*, org.starexec.util.*, org.starexec.constants.R" session="true"%>
<%@taglib prefix="star" tagdir="/WEB-INF/tags" %>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>

<%
try {
	// Grab relevant user id & processor info
	request.setAttribute("processorNameLen", R.PROCESSOR_NAME_LEN);
	request.setAttribute("processorDescLen", R.PROCESSOR_DESC_LEN);
	int procId = Integer.parseInt((String)request.getParameter("id"));
	int userId = SessionUtil.getUserId(request);
	request.setAttribute("procType", request.getParameter("type"));
	Processor proc=Processors.get(procId);
	
	// Only allowing editing of a processor if the user
	// is a leader of the space the processor belongs to
	List<User> leaders=Spaces.getLeaders(proc.getCommunityId());
	boolean validUser=false;
	
	for (User x : leaders) {
		if (x.getId()==userId) {
			validUser=true;
			break;
		}
	}
	
	if (!validUser) {
		proc=null;
	}

	// The user has permissions and the processor is valid
	if(proc != null) {
		request.setAttribute("proc", proc);
		
	} else {
		response.sendError(HttpServletResponse.SC_NOT_FOUND, "the processor does not exist or is restricted");
	}
} catch (Exception e) {
	response.sendError(HttpServletResponse.SC_BAD_REQUEST);
}
%>

<star:template title="edit ${proc.name}" css="edit/processor, edit/shared" js="lib/jquery.validate.min, edit/processor">
	<input type="hidden" id="cid" value="${proc.communityId}"/>
	<form id="editProcForm">
	<fieldset>
		<legend>processor details</legend>
		<table id="detailsTbl" class="shaded">
			<thead>
				<tr>
					<th class="label">attribute</th>
					<th>current value</th>
				</tr>
			</thead>
			<tbody>
				<tr>
					<td class="label">name</td>
					<td><input id="name" type="text" name="name" maxlength="${processorNameLen}" value="${proc.name}"/></td>
				</tr>
				<tr>
					<td class="label">description</td>
					<td><textarea id="description" name="description" length="${processorDescLen}" >${proc.description}</textarea></td>
				</tr>
				
			</tbody>	
		</table>
		<button type="button" id="delete">delete</button>
		<button type="button" id="cancel">cancel</button>
		<button type="button" id="update">update</button>
	</fieldset>
	</form>
</star:template>