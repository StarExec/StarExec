<%@page contentType="text/html" pageEncoding="UTF-8" %>
<%@taglib prefix="star" tagdir="/WEB-INF/tags" %>

<star:template title="Compute Cluster"
               js="common/format, lib/jquery.dataTables.min, lib/jquery.jstree, shared/sharedFunctions, explore/cluster, lib/jquery.progressbar.min, lib/jquery.heatcolor.0.0.1.min"
               css="explore/cluster, common/table, explore/common, shared/cluster">
	<div id="explorer">
		<h3>Active Queues</h3>
		<ul id="exploreList">
		</ul>
	</div>

	<div id="detailPanel">
		<h3 id="workerName"></h3>
		<span id="progressBar"></span>
		<span id="activeStatus"></span>
		<p class="accent" id="queueID"></p>

		<fieldset id="descriptionContainer">
			<legend class="Queue Description">Queue Description</legend>
			<div id="queueDescriptionWrapper">
				<p id="queueDescriptionText"></p>
			</div>
		</fieldset>

		<fieldset id="jobsContainer" class="expdContainer">
			<legend class="expd"><span class="list-count"></span> Running Jobs</legend>
			<table id="jobs"></table>
		</fieldset>

		<fieldset id="detailField">
			<legend class="expd" id="clusterExpd">Job Pairs</legend>
			<table id="details" class="shaded">
				<thead>
				<tr>
					<th>Submit Time</th>
					<th>Job</th>
					<th>User</th>
					<th id="benchHead">Benchmark</th>
					<th>Solver</th>
					<th>Config</th>
					<th>Path</th>
				</tr>
				</thead>
				<tbody>
					<%-- This will be populated by the job pair pagination feature --%>
				</tbody>
			</table>
		</fieldset>

		<fieldset id="loadsField">
			<legend>queue load</legend>
			<button id="refreshLoads">refresh</button>
			<textarea id="loadOutput" readonly></textarea>
		</fieldset>

		<fieldset id="qstatField">
			<legend>qstat output</legend>
			<button id="refreshQstat">refresh</button>
			<textarea id="qstatOutput" readonly wrap="soft"></textarea>
		</fieldset>

		<fieldset id="graphs">
			<legend>graphs</legend>
			<%-- "default the queuegraph image to all.q (1) because all.q always exists" --%>
			<img id="queuegraph" src="${starexecRoot}/secure/clustergraphs/1_queuegraph.png" width="400" height="400"/>
		</fieldset>
	</div>
</star:template>
