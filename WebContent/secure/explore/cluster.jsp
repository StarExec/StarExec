<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@taglib prefix="star" tagdir="/WEB-INF/tags" %>

<star:template title="star cluster" js="lib/jquery.dataTables.min, lib/jquery.cookie, lib/jquery.jstree, explore/cluster" css="common/table, explore/common">			
	<div id="explorer">
		<h3>online nodes</h3>
		<ul id="exploreList">
		</ul>
	</div>
	
	<div id="detailPanel">
		<h3 id="workerName"></h3>					
		<fieldset id="detailField">
			<legend>details</legend>
			<table id="details">
				<thead>
					<tr>
						<th>attribute</th>
						<th>value</th>						
					</tr>
				</thead>			
			</table>
		</fieldset>	
		
		<fieldset>
			<legend>actions</legend>
			<ul id="actionList">
				<li><span class="round button"><a id="addSpace" href="#">take offline</a></span></li>				
			</ul>
		</fieldset>				
	</div>	
</star:template>