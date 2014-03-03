var reserved;
var requests;
var type;

$(document).ready(function(){
	
	// Set the path to the css theme fr the jstree plugin
	 $.jstree._themes = starexecRoot+"css/jstree/";
	 
	 var id = -1;
	 
	// Initialize the jstree plugin for the community list
	jQuery("#exploreList").jstree({  
		"json_data" : { 
			"ajax" : { 
				"url" : starexecRoot+"services/cluster/queues",	// Where we will be getting json data from
				"data" : function (n) {  							
					return { id : n.attr ? n.attr("id") : -1 }; // What the default space id should be
				} 
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
				"active_queue" : {
					"valid_children" : [ "enabled_node", "disabled_node" ],
					"icon" : {
						"image" : starexecRoot+"images/jstree/on.png"
					}
				},
				"inactive_queue" : {
					"valid_children" : [ "enabled_node", "disabled_node"],
					"icon" : {
						"image" : starexecRoot + "images/jstree/off.png"
					}
				},
				"enabled_node" : {
					"valid_children" : [],
					"icon" : {
						"image" : starexecRoot+"images/jstree/on.png"
					}
				},
				"disabled_node" : {
					"valid_children" : [],
					"icon" : {
						"image" : starexecRoot+"images/jstree/off.png"
					}
				}
			}
		},
		"plugins" : ["types", "themes", "json_data", "ui", "cookies"] ,
		"core" : { animation : 200 }
	}).bind("select_node.jstree", function (event, data) {
		// When a node is clicked, get its ID and display the info in the details pane		
	   id = data.rslt.obj.attr("id");
	   window['type'] = data.rslt.obj.attr("rel");
	   var permanent = data.rslt.obj.attr("permanent");
	   updateActionId(id, type, permanent);
	   //getCommunityDetails(id);
	}).delegate("a", "click", function (event, data) { event.preventDefault(); });	// This just disable's links in the node title

	initUI(id);
	
	initDataTables();
	
});

function initUI(id){
	
	$('#dialog-confirm-remove').hide();
	$('#dialog-confirm-permanent').hide();

	
	$("#newQueue").button({
		icons: {
			primary: "ui-icon-plusthick"
		}
    });
	
	$("#newPermanent").button({
		icons: {
			primary: "ui-icon-plusthick"
		}
	});
	
	$("#removeQueue").button({
		icons: {
			primary: "ui-icon-minusthick"
		}
	});
	
	$("#manageNodes").button({
		icons: {
			primary: "ui-icon-clipboard"
		}
	});
	
	$("#makePermanent").button({
		icons: {
			primary: "ui-icon-locked"
		}
	});
	
	$("#moveNodes").button({
		icons: {
			primary: "ui-icon-locked"
		}
	});
	
	if (id == -1) {
		$("#reserveQueue").hide();
		$("#removeQueue").hide();
		$("#makePermanent").hide();
		$("#moveNodes").hide();
	}
	
	//Make tables expandable/collapsable
	$('#reservationField').expandable(false);
	$('#reservedField').expandable(false);
	$('#historicField').expandable(true);

	


}

function updateActionId(id, type, permanent) {
	if (id != 1 && (type=="active_queue" || type=="inactive_queue")) {		// if not all.q and not permanent
		$("#removeQueue").show();
		$("#makePermanent").show();
		if (permanent == 'false') {
			$("#makePermanent").show();
			$("#moveNodes").hide();
		} else {
			$("#moveNodes").show();
			$("#makePermanent").hide();
		}
	} else {																// if permanent or all.q
		$("#removeQueue").hide();
		$("#makePermanent").hide();
		$("#moveNodes").hide();
		if ((type=="active_queue" || type=="inactive_queue") && (permanent == 'true')) {	
			$("#makePermanent").hide();
			$("#moveNodes").show();
		}
	}
	
	$('#moveNodes').attr('href', starexecRoot+"secure/admin/moveNodes.jsp?id=" + id);
	
	
	$("#makePermanent").click(function() {
		$('#dialog-confirm-permanent-txt').text('are you sure you want to make this queue permanent?');
	
		$('#dialog-confirm-permanent').dialog({
			modal: true,
			width: 380,
			height: 165,
			buttons: {
				'OK': function() {
					log('user confirmed to make queue permanent');
					$('#dialog-confirm-permanent').dialog('close');
					$.post(
							starexecRoot+"services/permanent/queue/" + id,
							function(returnCode) {
								switch (returnCode) {
									case 0:
										showMessage('success', "the queue is now permament", 5000);
										setTimeout(function(){document.location.reload(true);}, 1000);
										break;
									case 1:
										showMessage('error', "queue was not made permanent; please try again", 5000);
								}
							},
							"json"
					);
				},
				"cancel": function() {
					log('user canceled make queue permanent');
					$(this).dialog("close");
				}
			}
		});
	});
	
	
	$("#removeQueue").click(function(){
		$('#dialog-confirm-remove-txt').text('are you sure you want to remove this queue?');
		
		$('#dialog-confirm-remove').dialog({
			modal: true,
			width: 380,
			height: 165,
			buttons: {
				'OK': function() {
					log('user confirmed queue removal.');
					$('#dialog-confirm-remove').dialog('close');
					$.post(
							starexecRoot+"services/remove/queue/" + id,
							function(returnCode) {
								switch (returnCode) {
									case 0:
										showMessage('success', "the queue was successfully removed", 5000);
										//window.location = starexecRoot+'secure/admin/cluster.jsp';
										setTimeout(function(){document.location.reload(true);}, 1000);
										break;
									case 1:
										showMessage('error', "queue was not deleted; please try again", 5000);
										break;
									case 2:
										showMessage('error', "only the admin can delete this queue", 5000);
										break;
									default:
										showMessage('error', "invalid parameters", 5000);
										break;
								}
							},
							"json"
					);
				},
				"cancel": function() {
					log('user canceled queue removal');
					$(this).dialog("close");
				}
			}
		});
	});	
}

function initDataTables(){
	// Setup the DataTable objects
	requests = $('#qreserves').dataTable( {
		"sDom"			: 'rt<"bottom"flpi><"clear">',
		"iDisplayStart"	: 0,
		"iDisplayLength": 10,
		"bServerSide"	: true,
		"sAjaxSource"	: starexecRoot+"services/",
		"sServerMethod" : 'POST',
		"fnServerData"	: fnPaginationHandler
	});
	reserved = $('#qreserved').dataTable( {
		"sDom"			: 'rt<"bottom"flpi><"clear">',
		"iDisplayStart"	: 0,
		"iDisplayLength": 10,
		"bServerSide"	: true,
		"sAjaxSource"	: starexecRoot+"services/",
		"sServerMethod" : 'POST',
		"fnServerData"	: fnPaginationHandler2
	});
	historic = $('#qhistoric').dataTable( {
		"sDom"			: 'rt<"bottom"flpi><"clear">',
		"iDisplayStart"	: 0,
		"iDisplayLength": 10,
		"bServerSide"	: true,
		"sAjaxSource"	: starexecRoot+"services/",
		"sServerMethod" : 'POST',
		"fnServerData"	: fnPaginationHandler3
	});
}

function fnPaginationHandler(sSource, aoData, fnCallback){
	// Request the next page of primitives from the server via AJAX
	$.post(  
			sSource + "queues/pending/pagination",
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
					$('#reservationExpd').children('span:first-child').text(nextDataTablePage.iTotalRecords);
				
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

function fnPaginationHandler2(sSource, aoData, fnCallback){
	// Request the next page of primitives from the server via AJAX
	$.post(  
			sSource + "queues/reserved/pagination",
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
					$('#reservedExpd').children('span:first-child').text(nextDataTablePage.iTotalRecords);
				
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

function fnPaginationHandler3(sSource, aoData, fnCallback){
	// Request the next page of primitives from the server via AJAX
	$.post(  
			sSource + "queues/historic/pagination",
			aoData,
			function(nextDataTablePage){
				switch(nextDataTablePage){
				case 1:
					showMessage('error', "failed to get the next page of results; please try again", 5000);
					break;
				case 2:		
					break;
				default:	// Have to use the default case since this process returns JSON objects to the client

					// Update the number displayed in this DataTable's fieldset
					$('#historicExpd').children('span:first-child').text(nextDataTablePage.iTotalRecords);
				
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


function cancelReservation(spaceId, queueId) {
	$.post(
		starexecRoot+"services/cancel/queueReservation/" + spaceId + "/" + queueId,
		function(returnCode) {
			switch (returnCode) {
				case 0:
					showMessage('success', "the queue reservation was successfuly cancelled", 5000);
					//setTimeout(function(){document.location.reload(true);}, 1000);
					setTimeout(function() {location.reload(true);}, 1000);
					break;
				case 1:
					showMessage('error', "the queue was not successfuclly cancelled", 5000);
					break;
				case 2:
					showMessage('error', "only a leader of this space can modify its details", 5000);
					break;
				case 7:
					showMessage('error', "names must be unique among subspaces. It is possible a subspace you do not have permission to see shares the same name",5000);
					break;
				default:
					showMessage('error', "invalid parameters", 5000);
					break;
			}
		},
		"json"
	);
}



