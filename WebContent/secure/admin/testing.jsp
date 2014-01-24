<%@page contentType="text/html" pageEncoding="UTF-8" import="org.starexec.test.*,java.util.List, org.starexec.data.database.*, org.starexec.constants.*, org.starexec.util.*, org.starexec.data.to.*;"%>
<%@taglib prefix="star" tagdir="/WEB-INF/tags" %>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>

<star:template title="run diagnostic tests" js="admin/testing, lib/jquery.dataTables.min, lib/jquery.cookie, lib/jquery.jstree, lib/jquery.qtip.min, lib/jquery.heatcolor.0.0.1.min,lib/jquery.validate.min" css="common/table, details/shared, explore/common, explore/spaces, admin/admin, admin/testing">	
		<fieldset id="fieldTable">
			<legend>Existing Tests</legend>
			<table id="tableTests" class="shaded contentTbl">
				<thead>
					<tr>
						<th>name</th>
						<th>total tests</th>
						<th>tests passed</th>
						<th>tests failed</th>
						<th>status</th>
						
					</tr>
				</thead>
				<tbody>
								
				</tbody>
			</table>
	</fieldset>
		<fieldset id="actionField">
			<legend>actions</legend>
			
			<button id="runAll">Run All Tests</button>	
			<button id="runSelected">Run Selected Tests</button>
		</fieldset>		
	
</star:template>