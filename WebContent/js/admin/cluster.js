var reserved;
var requests;
var type;
var defaultQueueId;
var curQueueId;
$(document).ready(function(){
	
	// Set the path to the css theme fr the jstree plugin
	 $.jstree._themes = starexecRoot+"css/jstree/";
	 
	 var id = -1;
	 curQueueId=-1;
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
		"plugins" : ["types", "themes", "json_data", "ui"] ,
		"core" : { animation : 200 }
	}).bind("select_node.jstree", function (event, data) {
		// When a node is clicked, get its ID and display the info in the details pane		
	   id = data.rslt.obj.attr("id");
	   window['type'] = data.rslt.obj.attr("rel");
	   var permanent = data.rslt.obj.attr("permanent");
	   var global = data.rslt.obj.attr("global");
	   defaultQueueId = data.rslt.obj.attr("defaultQueueId");
	   updateActionId(id, type, permanent, global);
	}).on( "click", "a", function (event, data) { event.preventDefault(); });	// This just disable's links in the node title

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
	
	$("#CommunityAssoc").button({
		icons: {
			primary: "ui-icon-clipboard"
		}
	});
	
	$("#makeGlobal").button({
		icons: {
			primary: "ui-icon-unlocked"
		}
	});
	
	$("#removeGlobal").button({
		icons: {
			primary: "ui-icon-locked"
		}
	});
	
	$("#editQueue").button({
		icons: {
			primary: "ui-icon-pencil"
		}
	});
	
	//Make tables expandable/collapsable
	$('#reservationField').expandable(false);
	$('#reservedField').expandable(false);
	$('#historicField').expandable(true);

	
	
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
							starexecRoot+"services/permanent/queue/" + curQueueId,
							function(returnCode) {
								parseReturnCode(returnCode);

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
	
	$("#editQueue").click(function() {
		window.open(starexecRoot+"secure/edit/queue.jsp?id="+curQueueId);
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
					       starexecRoot+"services/remove/queue/" + curQueueId,
					       function(returnCode) {
					       s=parseReturnCode(returnCode);
					       if (s) {
						       setTimeout(function(){document.location.reload(true);}, 1000);
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
	
	$("#makeGlobal").click(function(){
		$('#dialog-confirm-remove-txt').text('are you sure you want to give global access to this queue?');
		
		$('#dialog-confirm-remove').dialog({
			modal: true,
			width: 380,
			height: 165,
			buttons: {
				'OK': function() {
					log('user confirmed giving global access.');
					$('#dialog-confirm-remove').dialog('close');
					$.post(
							starexecRoot+"services/queue/global/" + curQueueId,
							function(returnCode) {
								s=parseReturnCode(returnCode);
								if (s) {
									setTimeout(function(){document.location.reload(true);}, 1000);

								}
									
							},
							"json"
					);
				},
				"cancel": function() {
					log('user canceled giving global access');
					$(this).dialog("close");
				}
			}
		});
	});	
	
	$("#removeGlobal").click(function(){
		$('#dialog-confirm-remove-txt').text('are you sure you want to remove global access from this queue?');
		
		$('#dialog-confirm-remove').dialog({
			modal: true,
			width: 380,
			height: 165,
			buttons: {
				'OK': function() {
					log('user confirmed removing global access.');
					$('#dialog-confirm-remove').dialog('close');
					$.post(
							starexecRoot+"services/queue/global/remove/" + curQueueId,
							function(returnCode) {
								s=parseReturnCode(returnCode);
								if (s) {
									setTimeout(function(){document.location.reload(true);}, 1000);

								}
							"json"
					);
				},
				"cancel": function() {
					log('user canceled removing global access');
					$(this).dialog("close");
				}
			}
		});
	});	


}

function updateActionId(id, type, permanent, global) {	
	curQueueId=id;
	if (id == -1) {
		$("#removeQueue").hide();
		$("#makePermanent").hide();
		$("#moveNodes").hide();
		$("#CommunityAssoc").hide();
		$("#makeGlobal").hide();
		$("#removeGlobal").hide();
		$("#editQueue").hide();
	} else {
		$("#editQueue").show();
	}
	
	if (type == "active_queue" || type=="inactive_queue") {
		if (id == defaultQueueId) {
			$("#removeQueue").hide();
			$("#makePermanent").hide();
			$("#moveNodes").show();
			$("#CommunityAssoc").hide();
			$("#makeGlobal").hide();
			$("#removeGlobal").hide();
		} else {
			$("#removeQueue").show();

			if (permanent == 'true') {
				$("#makePermanent").hide();
				$("#moveNodes").show();

				if (global == 'true') {
					$("#makeGlobal").hide();
					$("#removeGlobal").show();
					$("#CommunityAssoc").hide();
				} else {
					$("#makeGlobal").show();
					$("#removeGlobal").hide();
					$("#CommunityAssoc").show();
				}
				
			} else {
				$("#makePermanent").show();
				$("#moveNodes").hide();
				$("#CommunityAssoc").hide();
				$("#makeGlobal").hide();
				$("#removeGlobal").hide();
			}
			
		}
	} else {
		$("#removeQueue").hide();
		$("#makePermanent").hide();
		$("#moveNodes").hide();
		$("#CommunityAssoc").hide();
		$("#makeGlobal").hide();
		$("#removeGlobal").hide();
	}
	
	
	$('#moveNodes').attr('href', starexecRoot+"secure/admin/moveNodes.jsp?id=" + id);
	$('#CommunityAssoc').attr('href', starexecRoot + "secure/admin/assocCommunity.jsp?id=" + id);

}

function initDataTables(){
	// Setup the DataTable objects
	requests = $('#qreserves').dataTable( {
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
			sSource + "queues/pending/pagination",
			aoData,
			function(nextDataTablePage){
				s=parseReturnCode(nextDataTablePage);
				if (s) {
					// Update the number displayed in this DataTable's fieldset
					$('#reservationExpd').children('span:first-child').text(nextDataTablePage.iTotalRecords);
				
				// Replace the current page with the newly received page
				fnCallback(nextDataTablePage);
				}
			},  
			"json"
	)
}

function cancelReservation(spaceId, queueId) {
	$.post(
		starexecRoot+"services/cancel/queueReservation/" + spaceId + "/" + queueId,
		function(returnCode) {
			s=parseReturnCode(returnCode);
			if (s) {
				setTimeout(function() {location.reload(true);}, 1000);
			}
				
		},
		"json"
	);
}



