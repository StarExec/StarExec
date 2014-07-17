$(document).ready(function(){
	
	// Set the path to the css theme fr the jstree plugin
	 $.jstree._themes = starexecRoot+"css/jstree/";
	 
	 var id = -1;
	 
	// Initialize the jstree plugin for the community list
	jQuery("#exploreList").jstree({  
		"json_data" : { 
			"ajax" : { 
				"url" : starexecRoot+"services/communities/all"	// Where we will be getting json data from 				
			} 
		}, 
		"themes" : { 
			"theme" : "default", 					
			"dots" : false, 
			"icons" : true
		},			
		"types" : {				
			"max_depth" : -2,
			"max_children" : -2,					
			"valid_children" : [ "space" ],
			"types" : {						
				"space" : {
					"valid_children" : [ "space" ],
					"icon" : {
						"image" : starexecRoot+"images/jstree/users.png"
					}
				}
			}
		},
		"plugins" : ["types", "themes", "json_data", "ui", "cookies"] ,
		"core" : { animation : 200 }
	}).bind("select_node.jstree", function (event, data) {
		// When a node is clicked, get its ID and display the info in the details pane		
	   id = data.rslt.obj.attr("id");
	   updateActionId(id);
	   //getCommunityDetails(id);
	}).delegate("a", "click", function (event, data) { event.preventDefault(); });	// This just disable's links in the node title

	initUI(id);
	initDataTables();
	
	 setInterval(function() {
		 $("#commRequests").fnDraw(false);
	 }, 10000);
	
});

function initUI(id){
		
	$("#newCommunity").button({
		icons: {
			primary: "ui-icon-plusthick"
		}
    });
	
	$("#removeCommLeader").button({
		icons: {
			primary: "ui-icon-minusthick"
		}
	});
	
	$("#promoteCommLeader").button({
		icons: {
			primary: "ui-icon-circle-arrow-n"
		}
	});
	
	$('#newCommunity').attr('href', starexecRoot+"secure/add/space.jsp?sid=1");
	
	if (id == -1) {
		$("#removeCommLeader").hide();
		$("#promoteCommLeader").hide();
	}	
}


function updateActionId(id) {
	$('#removeCommLeader').attr('href', starexecRoot+"secure/edit/community.jsp?cid=" + id);
	$('#promoteCommLeader').attr('href', starexecRoot+"secure/edit/community.jsp?cid=" + id);
	
	$("#removeCommLeader").show();
	$("#promoteCommLeader").show();
}

function initDataTables(){
	// Setup the DataTable objects
	$('#commRequests').dataTable( {
		"sDom"			: 'rt<"bottom"flpi><"clear">',
		"iDisplayStart"	: 0,
		"iDisplayLength": defaultPageSize,
		"bServerSide"	: true,
		"sAjaxSource"	: starexecRoot+"services/",
		"sServerMethod" : 'POST',
		"fnServerData"	: fnPaginationHandler
	});
}

function fnPaginationHandler(sSource, aoData, fnCallback){
	// Request the next page of primitives from the server via AJAX
	$.post(  
			sSource + "community/pending/requests",
			aoData,
			function(nextDataTablePage){
				switch(nextDataTablePage){
				case 1:
					showMessage('error', "failed to get the next page of results; please try again", 5000);
					break;
				case 2:		
					// This error is a nuisance and the fieldsets are already hidden on spaces where the user lacks permissions
					//showMessage('error', "you do not have sufficient permissions to view primitives in this space", 5000);
					break;
				default:	// Have to use the default case since this process returns JSON objects to the client

					// Update the number displayed in this DataTable's fieldset
					$('#communityExpd').children('span:first-child').text(nextDataTablePage.iTotalRecords);
				
				// Replace the current page with the newly received page
				fnCallback(nextDataTablePage);
				

				break;
				}
			},  
			"json"
	).error(function(){
		//showMessage('error',"Internal error populating table",5000); Seems to show up on redirects
	});
}

function handleRequest(code, isApproved) {
	alert(code + " " + isApproved);
}

