<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@taglib prefix="star" tagdir="/WEB-INF/tags" %>

<star:template title="star cluster" js="lib/jquery.dataTables.min, lib/jquery.cookie, lib/jquery.jstree, explore/cluster, lib/jquery.progressbar.min" css="common/table, explore/common">			
	<div id="explorer">
		<h3>active queues</h3>
		<ul id="exploreList">
		</ul>
	</div>
	
	<div id="detailPanel">
		<h3 id="workerName"></h3> <span id="progressBar"></span> <span id="activeStatus"></span>		
		<p class="accent" id="queueID"></p>
		<fieldset id="detailField">
			<legend>details</legend>
			<table id="details" class="shaded">
				<thead>
					<tr>
						<th>attribute</th>
						<th>value</th>						
					</tr>
				</thead>			
			</table>
		</fieldset>	
		
		<!-- <fieldset>
			<legend>actions</legend>
			<ul id="actionList">							
			</ul>
		</fieldset>-->				
	</div>	
</star:template>