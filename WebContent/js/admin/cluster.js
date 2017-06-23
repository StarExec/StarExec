var type;
var defaultQueueId;
var curQueueId;

jQuery(function($) {
	"use strict";

	// Set the path to the css theme fr the jstree plugin
	 $.jstree._themes = starexecRoot + "css/jstree/";

	var reloadOnSucess = function(returnCode) {
		if (parseReturnCode(returnCode)) {
			setTimeout(function(){document.location.reload(true);}, 1000);
		}
	}
	var id = -1;
	curQueueId = -1;

	$confirmRemove = $("<div title='confirm removal' class='hiddenDialog'>")
		.append("<p><span class='ui-icon ui-icon-alert'></span></p>")
		.appendTo("body")
	;
	$confirmRemoveMessage = $("<span>")
		.appendTo($confirmRemove.contents("p"))
	;

	// Initialize the jstree plugin for the community list
	$("#exploreList")
		.bind("loaded.jstree", addNodeCountsToTree)
		.jstree({
			"json_data" : {
				"ajax" : {
					"url" : starexecRoot + "services/cluster/queues", // Where we will be getting json data from
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
			"plugins" : ["types", "themes", "json_data", "ui"],
			"core" : { animation : 200 }
		})
		.bind("select_node.jstree", function (event, data) {
			// When a node is clicked, get its ID and display the info in the details pane
			id = data.rslt.obj.attr("id");
			window['type'] = data.rslt.obj.attr("rel");
			var global = data.rslt.obj.attr("global");
			defaultQueueId = data.rslt.obj.attr("defaultQueueId");
			updateActionId(id, type, global);
		})
		.on("click", "a", function (event, data) {
			// This just disable's links in the node title
			event.preventDefault();
		})
	;

	$("#clearErrorStates a")
		.button({
			icons: {
				primary: "ui-icon-pencil"
			}
		})
		.click(function() {
			$.post(
				starexecRoot + "services/cluster/clearerrors",
				parseReturnCode,
				"json"
			);
		})
	;

	$("#CommunityAssoc a").button({
		icons: {
			primary: "ui-icon-clipboard"
		}
	});

	$("#editQueue a")
		.button({
			icons: {
				primary: "ui-icon-pencil"
			}
		})
		.click(function() {
			window.open(starexecRoot+"secure/edit/queue.jsp?id="+curQueueId);
		})
	;

	$("#makeGlobal a")
		.button({
			icons: {
					primary: "ui-icon-unlocked"
			}
		})
		.click(function() {
			$confirmRemoveMessage.text('are you sure you want to give global access to this queue?');
			$confirmRemove.dialog({
				modal: true,
				width: 380,
				height: 165,
				buttons: {
					'OK': function() {
						log('user confirmed giving global access.');
						$confirmRemove.dialog('close');
						$.post(
							starexecRoot + "services/queue/global/" + curQueueId,
							reloadOnSucess,
							"json"
						);
					},
					"cancel": function() {
						log('user canceled giving global access');
						$confirmRemove.dialog("close");
					}
				}
			});
		})
	;

	$("#makeTest a")
		.button({
			icons: {
				primary: "ui-icon-clipboard"
			}
		})
		.click(function(){
			$.post(
				starexecRoot + "services/test/queue/" + curQueueId,
				parseReturnCode,
				"json"
			);
		})
	;

	$("#manageNodes")
		.button({
			icons: {
				primary: "ui-icon-clipboard"
			}
		})
	;

	$("#moveNodes a")
		.button({
			icons: {
				primary: "ui-icon-locked"
			}
		})
	;

	$("#newQueue")
		.button({
			icons: {
				primary: "ui-icon-plusthick"
			}
		})
	;

	$("#removeGlobal a")
		.button({
			icons: {
				primary: "ui-icon-locked"
			}
		})
		.click(function() {
			$confirmRemoveMessage.text('are you sure you want to remove global access from this queue?');
			$confirmRemove.dialog({
				modal: true,
				width: 380,
				height: 165,
				buttons: {
					'OK': function() {
						log('user confirmed removing global access.');
						$confirmRemove.dialog('close');
						$.post(
							starexecRoot+"services/queue/global/remove/" + curQueueId,
							reloadOnSucess,
							"json"
						);
					},
					"cancel": function() {
						log('user canceled removing global access');
						$confirmRemove.dialog("close");
					}
				}
			});
		})
	;

	$("#removeQueue a")
		.button({
			icons: {
				primary: "ui-icon-minusthick"
			}
		})
		.click(function() {
			$confirmRemoveMessage.text('are you sure you want to remove this queue?');
			$confirmRemove.dialog({
				modal: true,
				width: 380,
				height: 165,
				buttons: {
					'OK': function() {
						log('user confirmed queue removal.');
						$confirmRemove.dialog('close');
						$.post(
							starexecRoot+"services/remove/queue/" + curQueueId,
							reloadOnSucess,
							"json"
						);
					},
					"cancel": function() {
						log('user canceled queue removal');
						$confirmRemove.dialog("close");
					}
				}
			});
		})
	;

	// Setup the DataTable objects
	$('#qreserves')
		.dataTable(new star.DataTableConfig({
			"bServerSide"   : true,
			"sAjaxSource"   : starexecRoot + "services/queues/pending/pagination",
			"fnServerData"  : fnPaginationHandler
		}))
	;

});

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

	$('#moveNodes').attr('href', starexecRoot+"secure/admin/moveNodes a.jsp?id=" + id);
	$('#CommunityAssoc').attr('href', starexecRoot + "secure/admin/assocCommunity.jsp?id=" + id);

}

function fnPaginationHandler(sSource, aoData, fnCallback){
	// Request the next page of primitives from the server via AJAX
	$.post(
			sSource,
			aoData,
			function(nextDataTablePage) {
				if (parseReturnCode(nextDataTablePage)) {
					// Update the number displayed in this DataTable's fieldset
					$('#reservationExpd').children('span:first-child').text(nextDataTablePage.iTotalRecords);
					// Replace the current page with the newly received page
					fnCallback(nextDataTablePage);
				}
			},
			"json"
	);
}
