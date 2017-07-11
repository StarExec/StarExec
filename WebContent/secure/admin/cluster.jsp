<%@page contentType="text/html" pageEncoding="UTF-8" %>
<%@taglib prefix="star" tagdir="/WEB-INF/tags" %>
<star:template title="Cluster Admin" js="admin/cluster, lib/jquery.dataTables.min, lib/jquery.jstree, lib/jquery.qtip.min, lib/jquery.heatcolor.0.0.1.min, shared/sharedFunctions" css="common/table, details/shared, explore/common, explore/spaces, admin/admin, shared/cluster">
	<div id="explorer">
		<h3>queues</h3>
		<ul id="exploreList"></ul>
		<div id="explorerAction">
			<ul id="exploreActions">
				<li><a type="btnRun" id="newQueue" href="${starexecRoot}/secure/admin/queue.jsp">Add New Queue</a></li>
			</ul>
		</div>
	</div>

	<div id="detailPanel">
		<fieldset>
			<legend>actions</legend>
			<ul id="actionList">
				<li id="editQueue"><a>edit queue</a></li>
				<li id="removeQueue"><a>remove queue</a></li>
				<li id="removeGlobal"><a>remove global access</a></li>
				<li id="clearErrorStates"><a>clear error states</a></li>
				<li id="moveNodes"><a>move nodes to this queue</a></li>
				<li id="CommunityAssoc"><a>give communities access</a></li>
				<li id="makeTest"><a>set queue as test queue</a></li>
				<li id="makeGlobal"><a>give queue global access</a></li>
			</ul>
		</fieldset>
	</div>
</star:template>
