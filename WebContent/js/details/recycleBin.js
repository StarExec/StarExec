var solverTable;
var benchTable;

jQuery(function($) {
	"use strict";

	var trashIcon = {"icons": {"secondary": "ui-icon-trash"}};
	var refreshIcon = {"icons": {"secondary": "ui-icon-refresh"}};
	var restoreIcon = {"icons": {"secondary": "ui-icon-pencil"}};

	$("fieldset").expandable(false);

	$('#clearSolvers')
	.button(trashIcon)
	.click(function() {
		deleteAll("solver");
	})
	;

	$('#clearBenchmarks')
	.button(trashIcon)
	.click(function() {
		deleteAll("benchmark");
	})
	;

	$("#deleteSelectedBenchmarks")
	.button(trashIcon)
	.click(function() {
		deleteSelected("benchmark");
	})
	;

	$("#deleteSelectedSolvers")
	.button(trashIcon)
	.click(function() {
		deleteSelected("solver");
	})
	;

	$("#restoreSelectedBenchmarks")
	.button(refreshIcon)
	.click(function() {
		restoreSelected("benchmark");
	})
	;

	$("#restoreSelectedSolvers")
	.button(refreshIcon)
	.click(function() {
		restoreSelected("solver");
	})
	;

	$('#restoreSolvers')
	.button(restoreIcon)
	.click(function() {
		restoreAll("solver");
	})
	;

	$('#restoreBenchmarks')
	.button(restoreIcon)
	.click(function() {
		restoreAll("benchmark");
	})
	;

	solverTable = $('#rsolvers').dataTable(
		new window.star.DataTableConfig({
			"bServerSide": true,
			"sAjaxSource": starexecRoot + "services/users/",
			"fnServerData": fnRecycledPaginationHandler, // included in this file
			"language": {"emptyTable": "No Solvers in Trash Bin"},
			"columns": [
				{"title": "Name"},
				{"title": "Description"},
				{
					"title": "Type",
					"width": "8em"
				}
			]
		})
	);

	benchTable = $('#rbenchmarks').dataTable(
		new window.star.DataTableConfig({
			"bServerSide": true,
			"sAjaxSource": starexecRoot + "services/users/",
			"fnServerData": fnRecycledPaginationHandler, // included in this file
			"language": {"emptyTable": "No Benchmarks in Trash Bin"},
			"columns": [
				{"title": "Name"},
				{"title": "Type"}
			]
		})
	);

	$("#rbenchmarks, #rsolvers")
	.on("mousedown", "tr:not(:has(.dataTables_empty))", function() {
		$(this).toggleClass("row_selected");
		handleClassChange();
	});

	handleClassChange();

});

function handleClassChange() {
	if ($("#rbenchmarks tr.row_selected").length > 0) {
		$("#deleteSelectedBenchmarks").show();
		$("#restoreSelectedBenchmarks").show();
	} else {
		$("#deleteSelectedBenchmarks").hide();
		$("#restoreSelectedBenchmarks").hide();
	}

	if ($("#rsolvers tr.row_selected").length > 0) {
		$("#deleteSelectedSolvers").show();
		$("#restoreSelectedSolvers").show();
	} else {
		$("#deleteSelectedSolvers").hide();
		$("#restoreSelectedSolvers").hide();
	}
}

function fnRecycledPaginationHandler(sSource, aoData, fnCallback) {
	var tableName = $(this).attr('id');
	var usrId = $(this).attr("uid");

	$.post(
		sSource + usrId + "/" + tableName + "/pagination",
		aoData,
		function(nextDataTablePage) {
			var s = parseReturnCode(nextDataTablePage);
			if (s) {
				fnCallback(nextDataTablePage);
			}
		},
		"json"
	).error(function() {
		showMessage('error', "Internal error populating table", 5000);
	});
}

var postCallback = function(nextDataTablePage) {
	"use strict";
	destroyDialog();
	if (parseReturnCode(nextDataTablePage)) {
		solverTable.fnDraw(false);
		benchTable.fnDraw(false);
		handleClassChange();
	}
};

function deleteAll(prim) {
	var message = 'Are you sure you want to delete all the ' + prim + '(s) from the trash bin? After deletion, they can not be recovered';

	// Display the confirmation dialog
	star.openDialog({
		title: "Confirm Delete",
		modal: true,
		height: 220,
		buttons: {
			'delete permanently': function() {
				$(this).dialog("close");
				createDialog("Clearing your trashed " + prim + "(s), please wait. This will take some time for large numbers of " + prim + "(s).");
				$.post(
					starexecRoot + "services/deleterecycled/" + prim + "s",
					postCallback,
					"json"
				).error(function() {
					showMessage('error', "Internal error deleting " + prim + "s",
						5000);
				});
			},
			"cancel": function() {
				$(this).dialog("close");
			}
		}
	}, message);
}

function restoreAll(prim) {
	var message = 'Are you sure you want to restore all the ' + prim + '(s) from the trash bin?';

	// Display the confirmation dialog
	star.openDialog({
		title: "Confirm Restore",
		modal: true,
		height: 220,
		buttons: {
			'restore': function() {
				$(this).dialog("close");
				createDialog("Restoring your trashed " + prim + "(s), please wait. This will take some time for large numbers of " + prim + "(s).");
				$.post(
					starexecRoot + "services/restorerecycled/" + prim + "s",
					postCallback,
					"json"
				).error(function() {
					showMessage('error', "Internal error restoring " + prim + "s",
						5000);
				});
			},
			"cancel": function() {
				$(this).dialog("close");
			}
		}
	}, message);
}

function deleteSelected(prim) {
	var message = 'Are you sure you want to delete all the selected ' + prim + '(s) from the trash bin? After deletion, they can not be recovered';
	if (prim == "solver") {
		table = solverTable;
	} else {
		table = benchTable;
	}
	// Display the confirmation dialog
	star.openDialog({
		title: "Confirm Delete",
		modal: true,
		height: 220,
		buttons: {
			'delete permanently': function() {
				$(this).dialog("close");
				createDialog("Clearing your trashed " + prim + "(s), please wait. This will take some time for large numbers of " + prim + "(s).");
				$.post(
					starexecRoot + "services/delete/" + prim,
					{selectedIds: getSelectedRows(table)},
					postCallback,
					"json"
				).error(function() {
					showMessage('error', "Internal error deleting " + prim + "s",
						5000);
				});
			},
			"cancel": function() {
				$(this).dialog("close");
			}
		}
	}, message);
}

function restoreSelected(prim) {
	var message = 'Are you sure you want to restore all the selected ' + prim + '(s) from the trash bin?';
	if (prim == "solver") {
		table = solverTable;
	} else {
		table = benchTable;
	}
	// Display the confirmation dialog
	star.openDialog({
		title: "Confirm Restore",
		modal: true,
		height: 220,
		buttons: {
			'restore': function() {
				$(this).dialog("close");
				createDialog("Restoring your trashed " + prim + "(s), please wait. This will take some time for large numbers of " + prim + "(s).");
				$.post(
					starexecRoot + "services/restore/" + prim,
					{selectedIds: getSelectedRows(table)},
					postCallback,
					"json"
				).error(function() {
					showMessage('error', "Internal error restoring " + prim + "s",
						5000);
				});
			},
			"cancel": function() {
				$(this).dialog("close");
			}
		}
	}, message);
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
