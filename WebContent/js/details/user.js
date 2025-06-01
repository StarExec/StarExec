var jTable;
var benchTable;
var solverTable;
var userId;
var uploadTable;
var spaceId;
var spaceName;

/**
* Event listener for page load. We are using https://datatables.net/
*/
$(document).ready(function() {
	userId = $("#userId").attr("value");
	// Hide loading images by default
	$('legend img').hide();

	$("#explorer").hide();

	$("#detailPanel").css("width", "100%");

	$(".expd").expandable(true);

	$('.popoutLink').button({
		icons: {
			secondary: "ui-icon-newwin"
		}
	});

	$("#linkOrphanedButton")
	.button({
		icons: {
			primary: "ui-icon-check"
		}
	})
	.click(linkAllOrphaned)
	.hide()
	;

	$(".recycleButton, .deleteButton").button({
		icons: {
			primary: "ui-icon-trash"
		}
	});

	$(".recycleSelected").click(function() {
		recycleSelected($(this).attr("prim"));
	});

	$(".recycleOrphaned").click(function() {
		recycleOrphaned($(this).attr("prim"));
	});

	$("#deleteJob").click(deleteSelectedJobs);

	$("#deleteOrphanedJob").click(function() {
		deleteOrphanedJobs();
	});

	$('#editButton').button({
		icons: {
			secondary: "ui-icon-pencil"
		}
	});

	$("#recycleBinButton").button({
		icons: {
			secondary: "ui-icon-pencil"
		}
	});

	$('img').click(function(event) {
		PopUp($(this).attr('enlarge'));
	});

	jsTree = makeSpaceTree("#exploreList");
	// Initialize the jstree plugin for the explorer list
	jsTree.bind("select_node.jstree", function(event, data) {
		// When a node is clicked, get its ID and display the info in the details pane
		spaceId = data.rslt.obj.attr("id");
		spaceName = $('.jstree-clicked').text();
	});

	var dataTableConfig = new star.DataTableConfig({
		//use serverside processing
		"bServerSide": true,
		//this is arguement sSource for fnpangnationhandler
		"sAjaxSource": starexecRoot + "services/users/",
		//This parameter allows you to override the default function which 
		//obtains the data from the server ($.getJSON) so something more suitable 
		//for your application. For example you could use POST data, or pull in
		//formation from a Gears or AIR database.
		"fnServerData": fnPaginationHandler
	});

	jTable = $('#jobs').dataTable(dataTableConfig);
	solverTable = $('#solvers').dataTable(dataTableConfig);
	benchTable = $('#benchmarks').dataTable(dataTableConfig);
        uploadTable = $('#uploads').dataTable(dataTableConfig);
    
	$(".selectableTable").on("mousedown", "tr", function() {
		$(this).toggleClass("row_selected");
		handleSelectChange();
	});

	$("#subscribeToErrorLogs").button({
		icons: {
			primary: "ui-icon-check"
		}
	}).click(function() {
		'use strict';
		$.post(
			starexecRoot + 'services/subscribe/user/errorLogs/' + userId,
			{},
			function(returnCode) {
				var success = parseReturnCode(returnCode);
				if (success) {
					location.reload();
				}
			},
			'json'
		);
	});

	$("#unsubscribeFromErrorLogs").button({
		icons: {
			primary: "ui-icon-check"
		}
	}).click(function() {
		'use strict';
		$.post(
			starexecRoot + 'services/unsubscribe/user/errorLogs/' + userId,
			{},
			function(returnCode) {
				var success = parseReturnCode(returnCode);
				if (success) {
					location.reload();
				}
			},
			'json'
		);
	});

	$("#showSpaceExplorer").button({
		icons: {
			primary: "ui-icon-check"
		}
	}).click(function() {
		if (!$("#explorer").is(":visible")) {
			$("#detailPanel").css("width", "65%");
			$("#showSpaceExplorer .ui-button-text").html("hide space explorer");
			$("#linkOrphanedButton").show();
		}
		$("#explorer").toggle("slide", function() {
			if (!$("#explorer").is(":visible")) {
				$("#detailPanel").css("width", "100%");
				$("#showSpaceExplorer .ui-button-text")
				.html("show space explorer");
				$("#linkOrphanedButton").hide();
			}
		});
	});

});

function PopUp(uri) {
	imageDialog = $("#popDialog");
	imageTag = $("#popImage");

	imageTag.attr('src', uri);

	imageTag.load(function() {
		$('#popDialog').dialog({
			dialogClass: "popup",
			modal: true,
			resizable: false,
			draggable: false,
			height: 'auto',
			width: 'auto'
		});
	});
}

/*String of ao to see contents
@author aguo2
*/
function printaoElement(aoElement) {
		return "{Name: " +aoElement.name + ", Value: " + aoElement.value + "}";
}

/* try to get a stack trace for debugging.
/* @author aguo2
*/
function stackTrace() {
    var err = new Error();
    return err.stack;
}

//prints ao to see contents
function printao(elements) {
	String = '{';
	var len = elements.length
	for (let i = 0; i < len - 1; i++) {
		String += printaoElement(elements[i]) + ",";
	}
	String += elements[len - 1];
	console.log(elements); 
}

/*this handles the pagination of the page.
* this is called from the datatables lib,
* the source code of which is heavily obsfucated 
* @param sSource part ofwhere we get the json data from
* @param aoData data sent to the server in the form of name value pair
* Here's an example of the type of stuff that aoData contains
0
: 
{name: 'sEcho', value: 1}
1
: 
{name: 'iColumns', value: 2}
2
: 
{name: 'sColumns', value: ','}
3
: 
{name: 'iDisplayStart', value: 0}
4
: 
{name: 'iDisplayLength', value: 10}
5
: 
{name: 'mDataProp_0', value: 0}
6
: 
{name: 'sSearch_0', value: ''}
7
: 
{name: 'bRegex_0', value: false}
8
: 
{name: 'bSearchable_0', value: true}
9
: 
{name: 'bSortable_0', value: true}
10
: 
{name: 'mDataProp_1', value: 1}
11
: 
{name: 'sSearch_1', value: ''}
12
: 
{name: 'bRegex_1', value: false}
13
: 
{name: 'bSearchable_1', value: true}
14
: 
{name: 'bSortable_1', value: true}
15
: 
{name: 'sSearch', value: ''}
16
: 
{name: 'bRegex', value: false}
17
: 
{name: 'iSortCol_0', value: 0}
18
: 
{name: 'sSortDir_0', value: 'asc'}
19
: 
{name: 'iSortingCols', value: 1}

* @param callback function
* @author Hawks
* @docs auguo2
*/
function fnPaginationHandler(sSource, aoData, fnCallback) {
	var tableName = $(this).attr('id');
	var usrId = $(this).attr("uid");
	//https://api.jquery.com/jquery.post/
	
	$.post(
		sSource + usrId + "/" + tableName + "/pagination",
		aoData,
		//datatable page contains the data in the table
		function(nextDataTablePage) {
			s = parseReturnCode(nextDataTablePage);
			if (s) {
				fnCallback(nextDataTablePage);
				makeTableDraggable("#" + tableName, onDragStart, getDragClone);

				if ('j' == tableName[0]) {
					colorizeJobStatistics();
				}

			}
		},
		"json"
	).error(function() {
		showMessage('error', "Internal error populating table", 5000);
	});
}

/**
 * Colorize the job statistics in the jobTable
 */
function colorizeJobStatistics() {
	// Colorize the statistics in the job table for completed pairs
	$("#jobs p.asc").heatcolor(
		function() {
			// Return the floating point value of the stat
			var value = $(this).text();
			return parseInt(value.slice(0, -1));
		},
		{
			maxval: 100,
			minval: 0,
			colorStyle: 'greentored',
			lightness: 0
		}
	);
	//colorize the unchanging totals
	$("#jobs p.static").heatcolor(
		function() {
			// Return the floating point value of the stat
			return 1;
		},
		{
			maxval: 1,
			minval: 0,
			colorStyle: 'greentored',
			lightness: 0
		}
	);
	// Colorize the statistics in the job table (for pending and error which use reverse color schemes)
	$("#jobs p.desc").heatcolor(
		function() {
			var value = $(this).text();
			return parseInt(value.slice(0, -1));
		},
		{
			maxval: 100,
			minval: 0,
			colorStyle: 'greentored',
			reverseOrder: true,
			lightness: 0
		}
	);
}

function handleSelectChange() {
	if ($("#benchmarks tr.row_selected").length > 0) {
		$("#recycleBenchmark").show();
	} else {
		$("#recycleBenchmark").hide();
	}

	if ($("#solvers tr.row_selected").length > 0) {
		$("#recycleSolver").show();
	} else {
		$("#recycleSolver").hide();
	}

	if ($("#jobs tr.row_selected").length > 0) {
		$("#deleteJob").show();
	} else {
		$("#deleteJob").hide();
	}
}

/**
 * For a given dataTable, this extracts the id's of the rows that have been
 * selected by the user
 *
 * @param dataTable the particular dataTable to extract the id's from
 * @returns {Array} list of id values for the selected rows
 * @author Todd Elvers
 */
function getSelectedRows(dataTable) {
	var idArray = [];
	var rows = $(dataTable).children('tbody').children('tr.row_selected');
	$.each(rows, function(i, row) {
		idArray.push($(this).children('td:first').children('input').val());
	});
	return idArray;
}

function recycleSelected(prim) {
	$('#dialog-confirm-recycle-txt')
	.text('Are you sure you want to move all the selected ' + prim + '(s) to the trash?');
	if (prim == "solver") {
		table = solverTable;
	} else {
		table = benchTable;
	}
	// Display the confirmation dialog
	$('#dialog-confirm-recycle').dialog({
		modal: true,
		height: 220,
		buttons: {
			'Move to Trash': function() {
				$("#dialog-confirm-recycle").dialog("close");
				createDialog("Moving the selected " + prim + "(s) to the trash, please wait. This will take some time for large numbers of " + prim + "(s).");
				$.post(
					starexecRoot + "services/recycle/" + prim,
					{selectedIds: getSelectedRows(table)},
					function(returnCode) {
						destroyDialog();
						s = parseReturnCode(returnCode);
						if (s) {
							solverTable.fnDraw(false);
							benchTable.fnDraw(false);
							handleSelectChange();
						}

					},
					"json"
				).error(function() {
					showMessage('error', "Internal error trashing " + prim + "s",
						5000);
				});
			},
			"cancel": function() {
				$(this).dialog("close");
			}
		}
	});
}

function linkAllOrphaned() {
	$('#dialog-confirm-copy-txt')
	.text('Are you sure you want to put all of your orphaned benchmarks, solvers, and jobs into ' + spaceName + '?');

	// Display the confirmation dialog
	$('#dialog-confirm-copy').dialog({
		modal: true,
		height: 220,
		buttons: {
			'link all': function() {
				$("#dialog-confirm-copy").dialog("close");
				createDialog(
					"Linking the orphaned primitives, please wait. This will take some time for large numbers of primitives.");
				$.post(
					starexecRoot + "services/linkAllOrphaned/" + userId + "/" + spaceId,
					{},
					function(returnCode) {
						destroyDialog();
						s = parseReturnCode(returnCode);
						if (s) {
							solverTable.fnDraw(false);
							benchTable.fnDraw(false);
							handleSelectChange();
						}

					},
					"json"
				).error(function() {
					showMessage('error',
						"Internal error linking primitives",
						5000);
				});
			},
			"cancel": function() {
				$(this).dialog("close");
			}
		}
	});
}

function recycleOrphaned(prim) {
	$('#dialog-confirm-recycle-txt')
	.text('Are you sure you want to move all of your orphaned ' + prim + '(s) to the trash?');

	// Display the confirmation dialog
	$('#dialog-confirm-recycle').dialog({
		modal: true,
		height: 220,
		buttons: {
			'Move to Trash': function() {
				$("#dialog-confirm-recycle").dialog("close");
				createDialog("Moving the selected " + prim + "(s) to the trash, please wait. This will take some time for large numbers of " + prim + "(s).");
				$.post(
					starexecRoot + "services/recycleOrphaned/" + prim + "/" + userId,
					{},
					function(returnCode) {
						destroyDialog();
						s = parseReturnCode(returnCode);
						if (s) {
							solverTable.fnDraw(false);
							benchTable.fnDraw(false);
							handleSelectChange();
						}

					},
					"json"
				).error(function() {
					showMessage('error', "Internal error trashing " + prim + "s",
						5000);
				});
			},
			"cancel": function() {
				$(this).dialog("close");
			}
		}
	});
}

function deleteSelectedJobs() {
	$('#dialog-confirm-delete-txt')
	.text(
		'Are you sure you want to delete all the selected job(s)? After deletion, they can not be recovered');

	// Display the confirmation dialog
	$('#dialog-confirm-delete').dialog({
		modal: true,
		height: 220,
		buttons: {
			'delete permanently': function() {
				$("#dialog-confirm-delete").dialog("close");
				createDialog(
					"Deleting the selected job(s), please wait. This will take some time for large numbers of jobs(s).");
				$.post(
					starexecRoot + "services/delete/job",
					{selectedIds: getSelectedRows(jTable)},
					function(nextDataTablePage) {
						destroyDialog();
						s = parseReturnCode(nextDataTablePage);
						if (s) {
							jTable.fnDraw(false);
							handleSelectChange();
						}
					},
					"json"
				).error(function() {
					showMessage('error',
						"Internal error deleting job(s)",
						5000);
				});
			},
			"cancel": function() {
				$(this).dialog("close");
			}
		}
	});
}

function deleteOrphanedJobs() {
	$('#dialog-confirm-delete-txt')
	.text(
		'Are you sure you want to delete all the selected job(s)? After deletion, they can not be recovered');
	// Display the confirmation dialog
	$('#dialog-confirm-delete').dialog({
		modal: true,
		height: 220,
		buttons: {
			'delete permanently': function() {
				$("#dialog-confirm-delete").dialog("close");
				createDialog(
					"Deleting the selected job(s), please wait. This will take some time for large numbers of jobs(s).");
				$.post(
					starexecRoot + "services/deleteOrphaned/job/" + userId,
					{},
					function(nextDataTablePage) {
						destroyDialog();
						s = parseReturnCode(nextDataTablePage);
						if (s) {
							jTable.fnDraw(false);
							handleSelectChange();
						}
					},
					"json"
				).error(function() {
					showMessage('error',
						"Internal error deleting job(s)",
						5000);
				});
			},
			"cancel": function() {
				$(this).dialog("close");
			}
		}
	});
}

/**
 * Called when any item is starting to be dragged within the browser
 */
function onDragStart(event, ui) {
	// Make each space in the explorer list be a droppable target; moving this from the initDraggable()
	// fixed the bug where spaces that were expanded after initDraggable() was called would not be
	// recognized as a viable drop target
	$('#exploreList').find('a').droppable({
		drop: onSpaceDrop,
		tolerance: 'pointer',	// Use the pointer to determine drop position instead of the middle of the drag clone element

		activeClass: 'active'		// Class applied to the space element when something is being dragged
	});
}

/**
 * Called when a draggable item (primitive) is dropped on a space
 */
function onSpaceDrop(event, ui) {


	// Collect the selected elements from the table being dragged from
	var ids = getSelectedRows($(ui.draggable).parents('table:first'));

	// Get the destination space id and name
	var destSpace = $(event.target).parent().attr('id');
	var destName = $(event.target).text();

	if (ids.length < 2) {
		// If 0 or 1 things are selected in the table, just use the element that is being dragged
		ids = [ui.draggable.data('id')];

		// Customize the confirmation message for the copy operation to the primitives/spaces involved
		if (ui.draggable.data('type')[0] == 's') {
			$('#dialog-confirm-copy-txt')
			.text('do you want to link ' + ui.draggable.data('name') + ' to' + destName + ' and all of its subspaces or just to' + destName + '?');
		}
		//job or benchmark
		else {
			$('#dialog-confirm-copy-txt')
			.text('do you want to link ' + ui.draggable.data('name') + ' to' + destName + '?');
		}
	} else {
		if (ui.draggable.data('type')[0] == 's' || ui.draggable.data('type')[0] == 'u') {
			$('#dialog-confirm-copy-txt')
			.text('do you want to link the ' + ids.length + ' selected ' + ui.draggable.data(
				'type') + 's to' + destName + ' and all of its subspaces or just to' + destName + '?');
		}
		//job or benchmark
		else {
			$('#dialog-confirm-copy-txt')
			.text('do you want to link the ' + ids.length + ' selected ' + ui.draggable.data(
				'type') + 's to' + destName + '?');
		}
	}

	// If primitive being copied to another space is a solver...
	if (ui.draggable.data('type')[0] == 's' && ui.draggable.data('type')[1] != 'p') {
		// Display the confirmation dialog
		$('#dialog-confirm-copy').dialog({
			modal: true,
			width: 500,
			height: 200,

			//depending on what the user
			buttons: {
				'link in space hierarchy': function() {
					$('#dialog-confirm-copy').dialog('close');
					doSolverLinkPost(ids, destSpace, true);
				},
				'link in space': function() {
					$('#dialog-confirm-copy').dialog('close');
					doSolverLinkPost(ids, destSpace, false);
				},
				"cancel": function() {
					$(this).dialog("close");
				}

			}
		});
	}
	// Otherwise, if the primitive being copied to another space is a benchmark
	else if (ui.draggable.data('type')[0] == 'b') {
		// Display the confirmation dialog
		$('#dialog-confirm-copy').dialog({
			modal: true,
			buttons: {
				'link': function() {
					$('#dialog-confirm-copy').dialog('close');
					$.post(
						starexecRoot + 'services/spaces/' + destSpace + '/add/benchmark', // We use the type to denote copying a benchmark/job
						{selectedIds: ids, copy: false},
						function(returnCode) {
							parseReturnCode(returnCode);
						},
						"json"
					).error(function() {
						showMessage('error',
							"Internal error copying benchmarks",
							5000);
					});
				},
				"cancel": function() {
					$(this).dialog("close");
				}
			}
		});

	}

	// Otherwise, if the primitive being copied to another space is a job
	else {
		// Display the confirmation dialog
		$('#dialog-confirm-copy').dialog({
			modal: true,
			buttons: {
				'yes': function() {
					// If the user actually confirms, close the dialog right away
					$('#dialog-confirm-copy').dialog('close');

					// Make the request to the server
					$.post(
						starexecRoot + 'services/spaces/' + destSpace + '/add/job',
						{selectedIds: ids},
						function(returnCode) {
							parseReturnCode(returnCode);
						},
						"json"
					).error(function() {
						showMessage('error',
							"Internal error copying jobs",
							5000);
					});
				},
				"cancel": function() {
					log('user canceled copy action');
					$(this).dialog("close");
				}
			}
		});

	}
}

/**
 * Sends a copy solver request to the server
 * @param ids The IDs of the solvers to copy
 * @param destSpace The ID of the destination space
 * @param copy A boolean indicating whether to copy (true) or link (false).
 * @param destName The name of the destination space
 * @author Eric Burns
 */

function doSolverLinkPost(ids, destSpace, hierarchy) {
	// Make the request to the server
	$.post(
		starexecRoot + 'services/spaces/' + destSpace + '/add/solver',
		{selectedIds: ids, copyToSubspaces: hierarchy, copy: false},
		function(returnCode) {
			parseReturnCode(returnCode);
		},
		"json"
	).error(function() {
		showMessage('error', "Internal error copying solvers", 5000);
	});
}
