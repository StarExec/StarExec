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
	$("#exploreList").bind("loaded.jstree", function(e, data) {
		 // Register a callback for when the jstree has finished loading
		addNodeCountsToTree();
	}).jstree({  
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
	   var global = data.rslt.obj.attr("global");
	   defaultQueueId = data.rslt.obj.attr("defaultQueueId");
	   updateActionId(id, type, global);
	}).on( "click", "a", function (event, data) { event.preventDefault(); });	// This just disable's links in the node title
	initUI(id);
	initDataTables();
	
});

function initUI(id){
		
	$("#newQueue").button({
		icons: {
			primary: "ui-icon-plusthick"
		}
    });
	
	$("#clearErrorStates").button({
		icons: {
			primary: "ui-icon-pencil"
		}
	});
	
	$("#clearErrorStates").click(function() {
		log('clearing error states');
		clearErrorStates();
	});
	
	$("#newQueue").button({
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
	
	$("#makeTest").button({
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
	
	
	$("#makeTest").click(function(){
		$.post(
			starexecRoot+"services/test/queue/" + curQueueId,
			function(returnCode) {
				s=parseReturnCode(returnCode);
								
			},
		"json"
		);
			
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
							},
					
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

function updateActionId(id, type, global) {	
	curQueueId=id;
	if (id == -1) {
		$("#removeQueue").hide();
		$("#moveNodes").hide();
		$("#CommunityAssoc").hide();
		$("#makeGlobal").hide();
		$("#makeTest").hide();

		$("#removeGlobal").hide();
		$("#editQueue").hide();
		
	} else {
		$("#editQueue").show();
	}
	
	if (type == "active_queue" || type=="inactive_queue") {
		$("#makeTest").show();

		if (id == defaultQueueId) {
			$("#removeQueue").hide();
			$("#moveNodes").show();
			$("#CommunityAssoc").hide();
			$("#makeGlobal").hide();
			$("#removeGlobal").hide();
		} else {
			$("#removeQueue").show();

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
				
			
			
		}
	} else {
		$("#removeQueue").hide();
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
	);
}

function clearErrorStates() {
	$.post(
			starexecRoot+"services/cluster/clearerrors",
			function(returnCode) {
				parseReturnCode(returnCode);	
			},
			"json"
		);
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



