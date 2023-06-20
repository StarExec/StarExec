<%@page import="org.starexec.data.database.Uploads" %>
<%@page import="org.starexec.data.security.BenchmarkSecurity, org.starexec.data.to.Benchmark" %>
<%@page contentType="text/html" pageEncoding="UTF-8"
        import="org.starexec.data.to.BenchmarkUploadStatus,org.starexec.util.SessionUtil, java.util.List" %>
<%@taglib prefix="star" tagdir="/WEB-INF/tags" %>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%
	try {
		int userId = SessionUtil.getUserId(request);
		int statusId = Integer.parseInt(request.getParameter("id"));

		BenchmarkUploadStatus s = null;
		List<Benchmark> bS = null;
		if (BenchmarkSecurity.canUserSeeBenchmarkStatus(statusId, userId)) {
			s = Uploads.getBenchmarkStatus(statusId);
			bS = Uploads.getFailedBenches(statusId);
		}

		if (s != null) {
			request.setAttribute("status", s);
			if (!s.isEverythingComplete()) {
				response.setIntHeader("Refresh", 10);
			}
		} else {
			response.sendError(
					HttpServletResponse.SC_NOT_FOUND,
					"Upload Status does not exist or is restricted"
			);
			return;
		}
		if (bS != null) {
			request.setAttribute("badBenches", bS);
		} else {
			response.sendError(
					HttpServletResponse.SC_NOT_FOUND,
					"Upload Status does not exist or is restricted"
			);
			return;
		}
	} catch (NumberFormatException nfe) {
		response.sendError(
				HttpServletResponse.SC_BAD_REQUEST,
				"The given upload status id was in an invalid format"
		);
		return;
	} catch (Exception e) {
		response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
		                   e.getMessage()
		);
		return;
	}
%>

<star:template title="upload status"
               js="details/shared, lib/jquery.dataTables.min"
               css="details/shared, common/table, details/uploadStatus">
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
				                    value="${status.uploadDate}"/></td>
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
			<tr>
				<td>resumable</td>
				<td>${status.resumable}</td>
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
					<th>output</th>
				</tr>
				</thead>
				<tbody>
				<c:forEach var="bench" items="${badBenches}">
					<tr>
						<td>${bench.name}</td>
						<td>
							<a href="${starexecRoot}/services/uploads/stdout/${bench.id}">view</a>
						</td>
					</tr>
				</c:forEach>
				</tbody>
			</table>
		</fieldset>
	</c:if>

	<a id="returnLink" href="${starexecRoot}/secure/explore/spaces.jsp">back</a>

</star:template>
