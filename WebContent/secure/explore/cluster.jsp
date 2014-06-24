<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@taglib prefix="star" tagdir="/WEB-INF/tags" %>

<star:template title="Compute Cluster" js="lib/jquery.dataTables.min, lib/jquery.cookie, lib/jquery.jstree, explore/cluster, lib/jquery.progressbar.min" css="explore/cluster, common/table, explore/common">			
	<div id="explorer">
		<h3>Active Queues</h3>
		<ul id="exploreList">
		</ul>
	</div>
	
	<div id="detailPanel">
		<h3 id="workerName"></h3> <span id="progressBar"></span> <span id="activeStatus"></span>		
		<p class="accent" id="queueID"></p>
		<fieldset id="detailField">
			<legend class="expd" id="clusterExpd">Job Pairs</legend>
			<table id="details" class="shaded">
				<thead>
					<tr>
						<th>job</th>
						<th>user</th>	
						<th id="benchHead">benchmark</th>
						<th>solver</th>
						<th>config</th>
						<th>path</th>					
					</tr>
				</thead>	
				<tbody>
					<!-- This will be populated by the job pair pagination feature -->
				</tbody>		
			</table>
		</fieldset>	
		
		<!-- <fieldset>
			<legend>actions</legend>
			<ul id="actionList">							
			</ul>
		</fieldset>-->				
	</div>	
</star:template>