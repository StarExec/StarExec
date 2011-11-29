<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@taglib prefix="star" tagdir="/WEB-INF/tags" %>

<star:template title="view communities" js="lib/jquery.dataTables.min, lib/jquery.cookie, lib/jquery.jstree, view_communities" css="table, view_communities">			
	<div id="communities">
		<h4>official</h4>
		<ul id="commList">
		</ul>
	</div>
	
	<div id="detailPanel">				
		<fieldset>
			<legend>community</legend>			
			<h3 id="commName"></h3>
			<p id="commDesc"></p>
			<div id="webDiv">
				<hr/>
				<ul id="websites"></ul>
			</div>		
		</fieldset>
					
		<fieldset id="leaderField">
			<legend onclick="toggleTable(this)" class="expd"><span>0</span> leaders <span>(+)</span></legend>
			<table id="leaders">
				<thead>
					<tr>
						<th>name</th>
						<th>institution</th>
						<th style="width:270px;">email</th>
					</tr>
				</thead>			
			</table>
		</fieldset>
											
		<fieldset id="memberField">
			<legend onclick="toggleTable(this)" class="expd"><span>0</span> members <span>(+)</span></legend>
			<table id="members">
				<thead>
					<tr>
						<th>name</th>
						<th>institution</th>
						<th style="width:270px;">email</th>
					</tr>
				</thead>			
			</table>
		</fieldset>							
		
		<fieldset>
			<legend>actions</legend>
			<ul id="actionList">
				<li><span class="round button"><a id="joinComm" href="#">join</a></span></li>
				<li><span class="round button"><a id="leaveComm" href="#">leave</a></span></li>
				<li><span class="round button"><a id="editComm" href="#">edit</a></span></li>							
			</ul>
		</fieldset>				
	</div>	
</star:template>