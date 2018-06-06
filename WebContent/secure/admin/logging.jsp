<%@page contentType="text/html" pageEncoding="UTF-8" %>
<%@taglib prefix="star" tagdir="/WEB-INF/tags" %>
<star:template title="Logging"
               js="admin/logging, lib/jquery.dataTables.min, lib/jquery.jstree, lib/jquery.qtip.min, lib/jquery.heatcolor.0.0.1.min,lib/jquery.validate.min"
               css="common/table, details/shared, explore/common, explore/spaces, admin/admin">
	<star:panel title="Logging Levels" withCount="false" expandable="false">
		<table id="tableLevels" class="shaded contentTbl">
			<thead>
			<tr>
				<th>level</th>
			</tr>
			</thead>
			<tbody>
			<tr id="clearRow" value="clear">
				<td>clear</td>
			</tr>
			<tr id="offRow" value="off">
				<td>off</td>
			</tr>
			<tr id="traceRow" value="trace">
				<td>trace</td>
			</tr>
			<tr id="debugRow" value="debug">
				<td>debug</td>
			</tr>
			<tr id="infoRow" value="info">
				<td>info</td>
			</tr>
			<tr id="warnRow" value="warn">
				<td>warn</td>
			</tr>
			<tr id="errorRow" value="error">
				<td>error</td>
			</tr>
			<tr id="fatalRow" value="fatal">
				<td>fatal</td>
			</tr>
			</tbody>
		</table>
	</star:panel>

	<star:panel title="actions" withCount="false" expandable="false">
		<input id="className" type="text"/>
		<button id="applyAll">Apply Level</button>
		<button id="applyToClass">Apply Level to Class</button>
		<button id="applyToClassAllOthersOff">Apply Level to Class And Turn Off
			All Other Classes
		</button>
	</star:panel>
</star:template>
