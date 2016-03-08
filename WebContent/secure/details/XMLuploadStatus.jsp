<%@page import="java.util.LinkedList"%>
<%@page import="java.util.ArrayList, java.util.List"%>
<%@page contentType="text/html" pageEncoding="UTF-8"
	import="org.apache.commons.io.*,org.starexec.data.security.*, org.starexec.data.database.*, org.starexec.data.to.*, org.starexec.util.*"%>
<%@taglib prefix="star" tagdir="/WEB-INF/tags"%>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<%@taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt"%>
<%
	try {
		int userId = SessionUtil.getUserId(request);
		int statusId = Integer.parseInt(request.getParameter("id"));

		SpaceXMLUploadStatus s = null;
		if (SpaceSecurity.canUserSeeSpaceXMLStatus(statusId, userId)) {
			s = Uploads.getSpaceXMLStatus(statusId);
		}

		if (s != null) {
			request.setAttribute("status", s);
			if (!s.isEverythingComplete()) {
				response.setIntHeader("Refresh", 10);
			}
		} else {
			response.sendError(HttpServletResponse.SC_NOT_FOUND,"XML Upload Status does not exist or is restricted");
		}
		
	} catch (Exception e) {
		response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,e.getMessage());
	}
%>

<star:template title="space xml upload status" js="details/shared, lib/jquery.dataTables.min" css="details/shared, common/table">
	<fieldset>
		<legend>details</legend>
		<table class="shaded">
			<thead>
				<tr>
					<th>attribute</th>
					<th>value</th>
				</tr>
			</thead>
			<tbody>
				<tr>
					<td>upload date</td>
					<td><fmt:formatDate pattern="MMM dd yyyy" value="${status.uploadDate}" /></td>
				</tr>
				<tr>
					<td>file upload complete</td>
					<td>${status.fileUploadComplete}</td>
				</tr>
				
				<tr>
					<td>total benchmarks</td>
					<td>${status.totalBenchmarks}</td>
				</tr>
				
				<tr>
					<td>completed benchmarks</td>
					<td>${status.completedBenchmarks}</td>
				</tr>
				<tr>
					<td>total spaces</td>
					<td>${status.totalSpaces}</td>
				</tr>
				<tr>
					<td>completed spaces</td>
					<td>${status.completedSpaces}</td>
				</tr>
				<tr>
					<td>total solvers</td>
					<td>${status.totalSolvers}</td>
				</tr>
				<tr>
					<td>completed solvers</td>
					<td>${status.completedSolvers}</td>
				</tr>
				<tr>
					<td>total benchmark updates</td>
					<td>${status.totalUpdates}</td>
				</tr>
				<tr>
					<td>completed benchmark updates</td>
					<td>${status.completedUpdates}</td>
				</tr>
				<tr>
					<td>entire upload complete</td>
					<td>${status.everythingComplete}</td>
				</tr>
				<tr>
					<td>upload error message</td>
					<td>${status.errorMessage}</td>
				</tr>
			</tbody>
		</table>
	</fieldset>
	<a id="returnLink" href="${starexecRoot}/secure/explore/spaces.jsp">back</a>

</star:template>