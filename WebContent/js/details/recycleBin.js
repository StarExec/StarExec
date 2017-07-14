var solverTable;
var benchTable;

jQuery(function($) {
	"use strict";

	var trashIcon = { "icons": { "secondary": "ui-icon-trash" }};
	var refreshIcon = { "icons": { "secondary": "ui-icon-refresh" }};
	var restoreIcon = { "icons": { "secondary": "ui-icon-pencil" }};

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
		.click(function(){
			restoreAll("solver");
		})
	;

	$('#restoreBenchmarks')
		.button(restoreIcon)
		.click(function(){
			restoreAll("benchmark");
		})
	;

	solverTable = $('#rsolvers').dataTable(
		new window.star.DataTableConfig({
			"bServerSide"  : true,
			"sAjaxSource"  : starexecRoot + "services/users/",
			"fnServerData" : fnRecycledPaginationHandler, // included in this file
			"columns"      : [
				{"title"   : "Name"},
				{"title"   : "Description"},
				{"title"   : "Type",
				 "width"   : "8em"}
			]
		})
	);

	benchTable = $('#rbenchmarks').dataTable(
		new window.star.DataTableConfig({
			"bServerSide"  : true,
			"sAjaxSource"  : starexecRoot + "services/users/",
			"fnServerData" : fnRecycledPaginationHandler, // included in this file
			"columns"      : [
				{"title"   : "Name"},
				{"title"   : "Type"}
			]
		})
	);

	$("#rbenchmarks, #rsolvers").on("mousedown", "tr", function() {
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
			function(nextDataTablePage){
				s=parseReturnCode(nextDataTablePage);
				if (s) {
					updateFieldsetCount(tableName, nextDataTablePage.iTotalRecords);
					fnCallback(nextDataTablePage);
				}

			},
			"json"
	).error(function(){
		showMessage('error',"Internal error populating table",5000);
	});
}


function deleteAll(prim) {
	$('#dialog-confirm-delete-txt').text('Are you sure you want to delete all the ' +prim +'(s) from the recycle bin? After deletion, they can not be recovered');

	// Display the confirmation dialog
	$('#dialog-confirm-delete').dialog({
		modal: true,
		height: 220,
		buttons: {
			'delete permanently': function() {
				$("#dialog-confirm-delete").dialog("close");
				createDialog("Clearing your recycled "+prim+"(s), please wait. This will take some time for large numbers of "+prim+"(s).");
				$.post(
						starexecRoot +"services/deleterecycled/"+prim+"s",
						function(nextDataTablePage){
							destroyDialog();
							s=parseReturnCode(nextDataTablePage);
							if (s) {
								solverTable.fnDraw(false);
								benchTable.fnDraw(false);
								handleClassChange();
							}
						},
						"json"
				).error(function(){
					showMessage('error',"Internal error deleting "+prim+"s",5000);
				});
			},
			"cancel": function() {
				$(this).dialog("close");
			}
		}
	});
}

function restoreAll(prim) {
	$('#dialog-confirm-restore-txt').text('Are you sure you want to restore all the ' +prim +'(s) from the recycle bin?');

	// Display the confirmation dialog
	$('#dialog-confirm-restore').dialog({
		modal: true,
		height: 220,
		buttons: {
			'restore': function() {
				$("#dialog-confirm-restore").dialog("close");
				createDialog("Restoring your recycled "+prim+"(s), please wait. This will take some time for large numbers of "+prim+"(s).");
				$.post(
						starexecRoot +"services/restorerecycled/"+prim+"s",
						function(nextDataTablePage){
							destroyDialog();
							s=parseReturnCode(nextDataTablePage);
							if (s) {
								solverTable.fnDraw(false);
								benchTable.fnDraw(false);
								handleClassChange();
							}
						},
						"json"
				).error(function(){
					showMessage('error',"Internal error restoring "+prim+"s",5000);
				});
			},
			"cancel": function() {
				$(this).dialog("close");
			}
		}
	});
}

function deleteSelected(prim) {
	$('#dialog-confirm-delete-txt').text('Are you sure you want to delete all the selected ' +prim +'(s) from the recycle bin? After deletion, they can not be recovered');
	if (prim=="solver") {
		table=solverTable;
	} else {
		table=benchTable;
	}
	// Display the confirmation dialog
	$('#dialog-confirm-delete').dialog({
		modal: true,
		height: 220,
		buttons: {
			'delete permanently': function() {
				$("#dialog-confirm-delete").dialog("close");
				createDialog("Clearing your recycled "+prim+"(s), please wait. This will take some time for large numbers of "+prim+"(s).");
				$.post(
						starexecRoot +"services/delete/"+prim,
						{selectedIds : getSelectedRows(table)},
						function(nextDataTablePage){
							destroyDialog();
							s=parseReturnCode(nextDataTablePage);
							if (s) {
								solverTable.fnDraw(false);
								benchTable.fnDraw(false);
								handleClassChange();
							}
						},
						"json"
				).error(function(){
					showMessage('error',"Internal error deleting "+prim+"s",5000);
				});
			},
			"cancel": function() {
				$(this).dialog("close");
			}
		}
	});
}

function restoreSelected(prim) {
	$('#dialog-confirm-restore-txt').text('Are you sure you want to restore all the selected ' +prim +'(s) from the recycle bin?');
	if (prim=="solver") {
		table=solverTable;
	} else {
		table=benchTable;
	}
	// Display the confirmation dialog
	$('#dialog-confirm-restore').dialog({
		modal: true,
		height: 220,
		buttons: {
			'restore': function() {
				$("#dialog-confirm-restore").dialog("close");
				createDialog("Restoring your recycled "+prim+"(s), please wait. This will take some time for large numbers of "+prim+"(s).");
				$.post(
						starexecRoot +"services/restore/"+prim,
						{selectedIds : getSelectedRows(table)},
						function(returnCode){
							destroyDialog();
							s=parseReturnCode(returnCode);
							if (s) {
								solverTable.fnDraw(false);
								benchTable.fnDraw(false);
								handleClassChange();
							}
						},
						"json"
				).error(function(){
					showMessage('error',"Internal error restoring "+prim+"s",5000);
				});
			},
			"cancel": function() {
				$(this).dialog("close");
			}
		}
	});
}

/**
 * Helper function for fnPaginationHandler; since the proper fieldset to update
 * cannot be reliably found via jQuery DOM navigation from fnPaginationHandler,
 * this method provides manually updates the appropriate fieldset to the new value
 *
 * @param tableName the name of the table whose fieldset we want to update (not in jQuery id format)
 * @param primCount the new value to update the fieldset with
 * @author Todd Elvers
 */
function updateFieldsetCount(tableName, value){
	switch(tableName[0]){
	case 'r':
		if ('s'==tableName[1]) {
			$("#recycledSolverExpd").children('span:first-child').text(value);
		} else {
			$("#recycledBenchExpd").children('span:first-child').text(value);
		}
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
function getSelectedRows(dataTable){
	var idArray = new Array();
	var rows = $(dataTable).children('tbody').children('tr.row_selected');
	$.each(rows, function(i, row) {
		idArray.push($(this).children('td:first').children('input').val());
	});
	return idArray;
}
