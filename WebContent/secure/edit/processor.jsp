<%@page contentType="text/html" pageEncoding="UTF-8"
        import="org.starexec.constants.DB, org.starexec.data.database.Communities, org.starexec.data.database.Processors, org.starexec.data.database.Syntaxes, org.starexec.data.security.GeneralSecurity, org.starexec.data.security.ProcessorSecurity, org.starexec.data.to.DefaultSettings, org.starexec.data.to.Processor, org.starexec.data.to.Syntax, org.starexec.data.to.enums.Primitive, org.starexec.data.to.enums.ProcessorType, org.starexec.util.SessionUtil"
        session="true" %>
<%@taglib prefix="star" tagdir="/WEB-INF/tags" %>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%
	try {
		// Grab relevant user id & processor info
		request.setAttribute("processorNameLen", DB.PROCESSOR_NAME_LEN);
		request.setAttribute("processorDescLen", DB.PROCESSOR_DESC_LEN);
		int procId = Integer.parseInt((String) request.getParameter("id"));
		int userId = SessionUtil.getUserId(request);
		Processor proc = Processors.get(procId);

		try {
			DefaultSettings settings =
					Communities.getDefaultSettings(proc.getCommunityId());
			request.setAttribute("defaultPPId", settings.getPostProcessorId());
			request.setAttribute("primitiveType", Primitive.PROCESSOR);
			request.setAttribute(
					"hasAdminReadPrivileges",
					GeneralSecurity.hasAdminReadPrivileges(userId)
			);
		} catch (Exception e) {
			//We couldn't find the default post processor ID, which is not a big deal
		}
		boolean validUser = false;
		if (ProcessorSecurity.doesUserOwnProcessor(procId, userId)
		                     .isSuccess()) {
			validUser = true;
		}

		if (!validUser || proc == null) {
			response.sendError(
					HttpServletResponse.SC_NOT_FOUND,
					"the processor does not exist or is restricted"
			);
			return;
		} else {
			request.setAttribute("proc", proc);

			if (proc.getType() == ProcessorType.BENCH) {
				StringBuilder syntaxes =
						new StringBuilder("<select name='syntax'>");
				int thisSyntax = proc.getSyntax().getId();
				for (Syntax s : Syntaxes.getAll()) {
					syntaxes.append("<option value='").append(s.getId())
					        .append("'");
					if (thisSyntax == s.getId()) {
						syntaxes.append(" selected");
					}
					syntaxes.append(">").append(s.name).append("</option>");
				}
				syntaxes.append("</select>");
				request.setAttribute("syntaxes", syntaxes.toString());
				request.setAttribute("benchmarkProcessor", 1 == 1);
			}
		}
	} catch (Exception e) {
		response.sendError(HttpServletResponse.SC_BAD_REQUEST);
		return;
	}
%>
<star:template title="edit ${proc.name}"
               css="details/shared, edit/processor, edit/shared, shared/copyToStardev"
               js="lib/jquery.validate.min, edit/processor, shared/copyToStardev">
	<star:primitiveTypes/>
	<star:primitiveIdentifier primId="${proc.id}"
	                          primType="${primitiveType.toString()}"/>
	<input type="hidden" id="cid" value="${proc.communityId}"/>
	<input type="hidden" id="ppid" value="${defaultPPId}"/>
	<p>id = ${proc.id}</p>
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
					<td><input id="name" type="text" name="name"
					           maxlength="${processorNameLen}"
					           value="${proc.name}"/></td>
				</tr>
				<tr>
					<td class="label">description</td>
					<td><textarea id="description" name="description"
					              length="${processorDescLen}">${proc.description}</textarea>
					</td>
				</tr>
				<tr>
					<td class="label">
						time limit
						<span class="ui-icon ui-icon-help"
						      title="the maximum wallclock time (in minutes) that this processor can execute before it is terminated"></span>
					</td>
					<td><input type="number" name="timelimit" min="1" max="60"
					           value="${proc.timeLimit}"/> minutes
					</td>
				</tr>
				<c:if test="${benchmarkProcessor}">
					<tr>
						<td class="label">Syntax Highlighting</td>
						<td>${syntaxes}</td>
					</tr>
				</c:if>
				</tbody>
			</table>
		</fieldset>
	</form>
	<fieldset id="actionField">
		<legend>actions</legend>
		<button type="button" id="delete">delete</button>
		<button type="button" id="cancel">cancel</button>
		<button type="button" id="update">update</button>
		<c:if test="${hasAdminReadPrivileges}">
			<star:copyToStardevButton/>
		</c:if>
	</fieldset>
	<c:if test="${hasAdminReadPrivileges}">
		<star:copyToStardevDialog/>
	</c:if>
</star:template>
