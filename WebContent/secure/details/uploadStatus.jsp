<%@page import="java.util.LinkedList"%>
<%@page import="java.util.ArrayList, java.util.List"%>
<%@page contentType="text/html" pageEncoding="UTF-8"
	import="org.apache.commons.io.*, org.starexec.data.database.*, org.starexec.data.to.*, org.starexec.util.*"%>
<%@taglib prefix="star" tagdir="/WEB-INF/tags"%>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<%@taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt"%>
<%
	try {
		int userId = SessionUtil.getUserId(request);
		int statusId = Integer.parseInt(request.getParameter("id"));

		UploadStatus s = null;
		List<String> bS = null;
		if (Permissions.canUserSeeStatus(statusId, userId)) {
			s = Uploads.get(statusId);
			bS = Uploads.getFailedBenches(statusId);
		}

		if (s != null) {
			request.setAttribute("status", s);
			if (!s.isEverythingComplete()) {
				response.setIntHeader("Refresh", 10);
			}
		} else {
			response.sendError(HttpServletResponse.SC_NOT_FOUND,
					"Upload Status does not exist or is restricted");
		}
		if (bS != null) {
			request.setAttribute("badBenches", bS);
		} else {
			response.sendError(HttpServletResponse.SC_NOT_FOUND,
					"Upload Status does not exist or is restricted");
		}
	} catch (NumberFormatException nfe) {
		response.sendError(HttpServletResponse.SC_BAD_REQUEST,
				"The given upload status id was in an invalid format");
	} catch (Exception e) {
		response.sendError(
				HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
				e.getMessage());
	}
%>

<star:template title="upload status"
	js="details/shared, lib/jquery.dataTables.min"
	css="details/shared, common/table">
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
					<td><fmt:formatDate pattern="MMM dd yyyy"
							value="${status.uploadDate}" /></td>
				</tr>
				<tr>
					<td>file upload complete</td>
					<td>${status.fileUploadComplete}</td>
				</tr>
				<tr>
					<td>file extraction complete</td>
					<td>${status.fileExtractionComplete}</td>
				</tr>
				<tr>
					<td>begun validating</td>
					<td>${status.processingBegun}</td>
				</tr>
				<tr>
					<td>total benchmarks</td>
					<td>${status.totalBenchmarks}</td>
				</tr>
				<tr>
					<td>validated benchmarks</td>
					<td>${status.validatedBenchmarks}</td>
				</tr>
				<tr>
					<td>benchmarks failing validation</td>
					<td>${status.failedBenchmarks}</td>
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
	<c:if test="${not empty badBenches}">
		<fieldset>
			<legend>failed benchmarks</legend>
			<table class="shaded">
				<thead>
					<tr>
						<th>name</th>
					</tr>
				</thead>
				<tbody>
					<c:forEach var="bench" items="${badBenches}">
						<tr>
							<td>${bench}</td>
						</tr>
					</c:forEach>
				</tbody>
			</table>
		</fieldset>
	</c:if>

	<a id="returnLink" href="/${starexecRoot}/secure/explore/spaces.jsp">back</a>

</star:template>