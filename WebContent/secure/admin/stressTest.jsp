<%@page contentType="text/html" pageEncoding="UTF-8" import="org.starexec.data.security.*, org.starexec.constants.*,org.starexec.data.database.*, org.starexec.data.to.*, org.starexec.util.*"%>
<%@taglib prefix="star" tagdir="/WEB-INF/tags" %>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%	
	try {		
		int userId = SessionUtil.getUserId(request);
		ValidatorStatusCode status=GeneralSecurity.canUserRunTests(userId);
		if (!status.isSuccess()) {
			response.sendError(HttpServletResponse.SC_FORBIDDEN,status.getMessage());
		} else {
			
		}
	}catch (Exception e) {
		response.sendError(HttpServletResponse.SC_NOT_FOUND, "You do not have permission to create a stress test");		
	}
%>
<star:template title="create a stress test" css="admin/stressTest" js="lib/jquery.validate.min, admin/stressTest">
	<form method="POST" action="/${starexecRoot}/secure/add/stressTest" id="createStressTestForm">
		<fieldset>
			<legend>configure a stress test</legend>		
			<table id="parameterTable" class="shaded">
				<thead>
					<tr>
						<th>attribute</th>
						<th>value</th>
					</tr>
				</thead>
				<tbody>
					<tr>
						<td title="the number of spaces that will be created by the test">space count</td>
						<td><input value="0" id="spaceCount" name="spaceCount" type="text"/></td>
					</tr>
					<tr>
						<td title="the number of users that will be created by the test">user count</td>
						<td><input value="0" id="userCount" name="userCount" type="text"/></td>
					</tr>
					
					
					<tr>
						<td title="the minimum number of users that will be placed in each space. Users are taken from the pool of users
						created for the test.">min users per space</td>
						<td><input value="0" id="minUsersPer" name="minUsersPer" type="text"/></td>
					</tr>
					<tr>
						<td title="the maximum number of users that will be placed in each space. Users are taken from the pool of users
						created for the test">max users per space</td>
						<td><input value="0" id="maxUsersPer" name="maxUsersPer" type="text"/></td>
					</tr>
					
					<tr>
						<td title="the minimum number of solvers that will be placed in each space. All solvers will be unique (no solver linked to multiple spaces)">min solvers per space</td>
						<td><input value="0" id="minSolversPer" name="minSolversPer" type="text"/></td>
					</tr>
					<tr>
						<td title="the maximum number of solvers that will be placed in each space. All solvers will be unique (no solver linked to multiple spaces)">max solvers per space</td>
						<td><input value="0" id="maxSolversPer" name="maxSolversPer" type="text"/></td>
					</tr>
					
					<tr>
						<td title="the minimum number of benchmarks that will be placed in each space. All benchmarks will be unique (no benchmark linked to multiple spaces)">min benchmarks per space</td>
						<td><input value="0" id="minBenchmarksPer" name="minBenchmarksPer" type="text"/></td>
					</tr>
					<tr>
						<td title="the maximum number of benchmarks that will be placed in each space. All benchmarks will be unique (no benchmark linked to multiple spaces)">max benchmarks per space</td>
						<td><input value="0" id="maxBenchmarksPer" name="maxBenchmarksPer" type="text"/></td>
					</tr>
					
					<tr>
						<td title="the number of jobs that will be created by the test">job count</td>
						<td><input value="0" id="jobCount" name="jobCount" type="text"/></td>
					</tr>
					
					<tr>
						<td title="The number of spaces per job. Each space will have the same number of solvers/ benchmarks as any other space">spaces per job</td>
						<td><input value="0" id="spacesPerJob" name="spacesPerJob" type="text"/></td>
					</tr>
				</tbody>
			</table>	
			<button class="cancelBtn" type="button">cancel</button>																
			<button class="submitBtn" type="submit">create</button>
		</fieldset>
	</form>
</star:template>