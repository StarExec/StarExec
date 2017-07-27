<%@page contentType="text/html" pageEncoding="UTF-8" %>
<%@taglib prefix="star" tagdir="/WEB-INF/tags" %>
<star:template title="Integration Tests" js="lib/jquery.dataTables.min, admin/testing" css="common/table, admin/testing">
		<fieldset id="fieldTable">
			<legend>Existing Tests</legend>
			<ul class="actionList">
				<li><a id="runAll">Run All Tests</a></li>
				<li><a id="runSelected">Run Selected Tests</a></li>
				<li><a id="runStress">Create Stress Test</a></li>
			</ul>
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
		</fieldset>
</star:template>
