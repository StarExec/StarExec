var jobTable;
var benchTable;
var solverTable;
$(document).ready(function(){
	
	// Hide loading images by default
	$('legend img').hide();
	$("#dialog-confirm-delete").hide();
	$("#dialog-confirm-recycle").hide();
	$("fieldset:not(:first)").expandable(true);
	
	$('.popoutLink').button({
		icons: {
			secondary: "ui-icon-newwin"
    }});
	
	$(".recycleButton, .deleteButton").button({
		icons: {
			primary: "ui-icon-trash"
	}
	});
	
	$(".recycleButton").click(function() {
		recycleSelected($(this).attr("prim"));
	});
	$("#deleteJob").click(function() {
		deleteSelectedJobs();
	});
	
	$('#editButton').button({
		icons: {
			secondary: "ui-icon-pencil"
	}});
	
	$("#recycleBinButton").button({
		icons: {
			secondary: "ui-icon-pencil"
		}
	});
	
	$('img').click(function(event){
		PopUp($(this).attr('enlarge'));
	});


	//Initiate job table
	jobTable=$('#jobs').dataTable( {
        "sDom"			: 'rt<"bottom"flpi><"clear">',
        "iDisplayStart"	: 0,
        "iDisplayLength": defaultPageSize,
        "bServerSide"	: true,
        "sAjaxSource"	: starexecRoot+"services/users/",
        "sServerMethod" : "POST",
        "fnServerData"	: fnPaginationHandler 
    });
	
	//Initiate solver table
	solverTable=$('#solvers').dataTable( {
        "sDom"			: 'rt<"bottom"flpi><"clear">',
        "iDisplayStart"	: 0,
        "iDisplayLength": defaultPageSize,
        "bServerSide"	: true,
        "sAjaxSource"	: starexecRoot+"services/users/",
        "sServerMethod" : "POST",
        "fnServerData"	: fnPaginationHandler
    });
    
	
	//Initiate benchmark table
	benchTable=$('#benchmarks').dataTable( {
        "sDom"			: 'rt<"bottom"flpi><"clear">',
        "iDisplayStart"	: 0,
        "iDisplayLength": defaultPageSize,
        "bServerSide"	: true,
        "sAjaxSource"	: starexecRoot+"services/users/",
        "sServerMethod" : "POST",
        "fnServerData"	: fnPaginationHandler
    });
	
	$(".selectableTable").on("mousedown", "tr", function(){
		$(this).toggleClass("row_selected");
		handleSelectChange();
	});
	
});

function PopUp(uri) {
	imageDialog = $("#popDialog");
	imageTag = $("#popImage");
	
	imageTag.attr('src', uri);

	imageTag.load(function(){
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

function fnPaginationHandler(sSource, aoData, fnCallback) {
	
	var tableName = $(this).attr('id');
	var usrId = $(this).attr("uid");
	
	$.post(  
			sSource + usrId + "/" + tableName + "/pagination",
			aoData,
			function(nextDataTablePage){
				switch(nextDataTablePage){
					case 1:
						showMessage('error', "failed to get the next page of results; please try again", 5000);
						break;
					case 2:
						showMessage('error', "you do not have sufficient permissions to view primitives for this user", 5000);
						break;
					default:
						updateFieldsetCount(tableName, nextDataTablePage.iTotalRecords);
 						fnCallback(nextDataTablePage);
 						if('j' == tableName[0]){
 							colorizeJobStatistics();
 						} 
 						break;
				}
			},  
			"json"
	).error(function(){
		showMessage('error',"Internal error populating table",5000);
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
	case 'j':
		$('#jobExpd').children('span:first-child').text(value);
		break;
	case 's':
		if('o' == tableName[1]) {
			$('#solverExpd').children('span:first-child').text(value);
		} else {
			$('#spaceExpd').children('span:first-child').text(value);
		}
		break;
	case 'b':
		$('#benchExpd').children('span:first-child').text(value);
		break;
		
	}
}

/**
 * Colorize the job statistics in the jobTable
 */
function colorizeJobStatistics(){
	// Colorize the statistics in the job table for completed pairs
	$("#jobs p.asc").heatcolor(
			function() {
				// Return the floating point value of the stat
				var value = $(this).text();
				return eval(value.slice(0, -1));				
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
				return eval(1);				
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
				return eval(value.slice(0, -1));	
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
	if ($("#benchmarks tr.row_selected").length>0) {
			$("#recycleBenchmark").show();
		
	}   else {
		$("#recycleBenchmark").hide();

	}
	if ($("#solvers tr.row_selected").length>0) {
			$("#recycleSolver").show();	
	} else {
		$("#recycleSolver").hide();
	}
	if ($("#jobs tr.row_selected").length>0) {
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
function getSelectedRows(dataTable){
	var idArray = new Array();
	var rows = $(dataTable).children('tbody').children('tr.row_selected');
	$.each(rows, function(i, row) {
		idArray.push($(this).children('td:first').children('input').val());
	});
	return idArray;
}

function recycleSelected(prim) {
	$('#dialog-confirm-recycle-txt').text('Are you sure you want to recycle all the selected ' +prim +'(s)?');
	if (prim=="solver") {
		table=solverTable;
	} else {
		table=benchTable;
	}
	// Display the confirmation dialog
	$('#dialog-confirm-recycle').dialog({
		modal: true,
		height: 220,
		buttons: {
			'recycle': function() {
				$("#dialog-confirm-recycle").dialog("close");
				createDialog("Recycling the selected "+prim+"(s), please wait. This will take some time for large numbers of "+prim+"(s).");
				$.post(  
						starexecRoot +"services/recycle/"+prim, 
						{selectedIds : getSelectedRows(table)},
						function(code){
							destroyDialog();
							switch(code){
								case 1:
									showMessage('error', "Internal error recycling "+prim+"s", 5000);
									break;
								case 2:
									showMessage('error', "you do not have permission to recycle the given prims",5000);
								default:
									solverTable.fnDraw(false);
									benchTable.fnDraw(false);
									handleSelectChange();
			 						break;
							}
						},  
						"json"
				).error(function(){
					showMessage('error',"Internal error recycling "+prim+"s",5000);
				});	
			},
			"cancel": function() {
				$(this).dialog("close");
			}
		}		
	});
}

function deleteSelectedJobs() {
	$('#dialog-confirm-delete-txt').text('Are you sure you want to delete all the selected job(s)? After deletion, they can not be recovered');
	table=jobTable
	// Display the confirmation dialog
	$('#dialog-confirm-delete').dialog({
		modal: true,
		height: 220,
		buttons: {
			'delete permanently': function() {
				$("#dialog-confirm-delete").dialog("close");
				createDialog("Deleting the selected job(s), please wait. This will take some time for large numbers of jobs(s).");
				$.post(  
						starexecRoot +"services/delete/job", 
						{selectedIds : getSelectedRows(table)},
						function(nextDataTablePage){
							destroyDialog();
							switch(nextDataTablePage){
								case 1:
									showMessage('error', "Internal error deleting job(s)", 5000);
									break;
								default:
									jobTable.fnDraw(false);
									handleSelectChange();
			 						break;
							}
						},  
						"json"
				).error(function(){
					showMessage('error',"Internal error deleting job(s)",5000);
				});	
			},
			"cancel": function() {
				$(this).dialog("close");
			}
		}		
	});
}
