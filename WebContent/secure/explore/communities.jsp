<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@taglib prefix="star" tagdir="/WEB-INF/tags" %>

<star:template title="view communities" js="lib/jquery.dataTables.min, lib/jquery.cookie, lib/jquery.jstree, explore/communities" css="common/table, explore/common">			
	<div id="explorer">
		<h3>official</h3>
		<ul id="exploreList">
		</ul>
	</div>
	
	<div id="detailPanel">
		<h3 id="commName"></h3>
		<p id="commDesc" class="accent"></p>
						
		<fieldset id="websiteField">
			<legend><span>0</span> website(s)</legend>						
			<div id="webDiv">				
				<ul id="websites" class="horizontal"></ul>
			</div>		
		</fieldset>
					
		<fieldset id="leaderField">
			<legend class="expd"><span>0</span> leaders</legend>
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
			<legend class="expd"><span>0</span> members</legend>
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
				<li><a id="joinComm" href="#">join</a></li>
				<li><a id="leaveComm">leave</a></li>
				<li><a id="editComm" href="#">edit</a></li>							
				<li><a id="removeUser">remove user</a></li>
			</ul>
		</fieldset>				
	</div>	
	
	<div id="dialog-confirm-delete" title="confirm delete">
		<p><span class="ui-icon ui-icon-alert" style="float:left; margin:0 7px 20px 0;"></span><span id="dialog-confirm-delete-txt"></span></p>
	</div>
	<div id="dialog-confirm-leave" title="leave community">
		<p><span class="ui-icon ui-icon-alert" style="float:left; margin:0 7px 20px 0;"></span><span id="dialog-confirm-leave-txt"></span></p>
	</div>
</star:template>