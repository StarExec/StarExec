<%@page contentType="text/html" pageEncoding="UTF-8"
        import="org.starexec.data.security.GeneralSecurity, org.starexec.util.SessionUtil" %>
<%@taglib prefix="star" tagdir="/WEB-INF/tags" %>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%
	int user = SessionUtil.getUserId(request);
	boolean canUserRunTests =
			GeneralSecurity.canUserRunTestsNoRunningCheck(user).isSuccess();
	request.setAttribute("canUserRunTests", canUserRunTests);
%>
<star:template title="Integration Tests"
               js="lib/jquery.dataTables.min, admin/testing"
               css="common/table, admin/testing">
	<star:panel title="Existing Tests" withCount="false" expandable="false">
		<c:if test="${canUserRunTests}">
			<ul class="actionList">
				<li><a id="runAll">Run All Tests</a></li>
				<li><a id="runSelected">Run Selected Tests</a></li>
				<li><a id="runStress">Create Stress Test</a></li>
			</ul>
		</c:if>
		<table id="tableTests" class="shaded contentTbl">
			<thead>
			<tr>
				<th>name</th>
				<th id="totalHead">tests</th>
				<th id="passedHead">passed</th>
				<th id="failedHead">failed</th>
				<th>status</th>
				<th id="errorHead">trace</th>
			</tr>
			</thead>
		</table>
	</star:panel>
</star:template>
