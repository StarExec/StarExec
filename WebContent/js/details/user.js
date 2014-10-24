var jTable;
var benchTable;
var solverTable;
var userId;

var spaceId;
var spaceName;
$(document).ready(function(){
	userId=$("#userId").attr("value");
	// Hide loading images by default
	$('legend img').hide();
	
	$("#explorer").hide();
	$("#linkOrphanedButton").hide();

	$("#detailPanel").css("width","100%");

	$(".dialog").hide(); // hide all dialogs
	$("fieldset:not(:first)").expandable(true);
	
	$('.popoutLink').button({
		icons: {
			secondary: "ui-icon-newwin"
    	}
	});
	$("#linkOrphanedButton").button({
		icons: {
			primary: "ui-icon-check"
		}
	});
	$("#linkOrphanedButton").click(function() {
		linkAllOrphaned();
	});
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
	$("#deleteJob").click(function() {
		deleteSelectedJobs();
	});
	
	$("#deleteOrphanedJob").click(function() {
		deleteOrphanedJobs();
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
	
	jsTree=makeSpaceTree("#exploreList");
	// Initialize the jstree plugin for the explorer list
	jsTree.bind("select_node.jstree", function (event, data) {
		// When a node is clicked, get its ID and display the info in the details pane
		spaceId = data.rslt.obj.attr("id");
		spaceName=$('.jstree-clicked').text();
	});
	//Initiate job table
	jTable=$('#jobs').dataTable( {
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
	
	$("#showSpaceExplorer").button({
		icons: {
			primary: "ui-icon-check"
	}
	});
	
	$("#showSpaceExplorer").click(function() {
		if (!$("#explorer").is(":visible")) {
			$("#detailPanel").css("width","65%");
			$("#showSpaceExplorer .ui-button-text").html("hide space explorer");
			$("#linkOrphanedButton").show();
		}
		$( "#explorer" ).toggle( "slide", function() {
			if (!$("#explorer").is(":visible")) {
				$("#detailPanel").css("width","100%");
				$("#showSpaceExplorer .ui-button-text").html("show space explorer");
				$("#linkOrphanedButton").hide();
			}
		} );
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
				s=parseReturnCode(nextDataTablePage);
				if (s) {
					updateFieldsetCount(tableName, nextDataTablePage.iTotalRecords);
						fnCallback(nextDataTablePage);
						makeTableDraggable("#"+tableName,onDragStart,getDragClone);

						if('j' == tableName[0]){
							colorizeJobStatistics();
						} 

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
						function(returnCode){
							destroyDialog();
							s=parseReturnCode(returnCode);
							if (s) {
								solverTable.fnDraw(false);
								benchTable.fnDraw(false);
								handleSelectChange();
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

function linkAllOrphaned() {
	$('#dialog-confirm-copy-txt').text('Are you sure you want to put all of your orphaned benchmarks, solvers, and jobs into ' +spaceName+'?');

	// Display the confirmation dialog
	$('#dialog-confirm-copy').dialog({
		modal: true,
		height: 220,
		buttons: {
			'link all': function() {
				$("#dialog-confirm-copy").dialog("close");
				createDialog("Linking the orphaned primitives, please wait. This will take some time for large numbers of primitives.");
				$.post(  
						starexecRoot +"services/linkAllOrphaned/"+userId+"/"+spaceId, 
						{},
						function(returnCode){
							destroyDialog();
							s=parseReturnCode(returnCode);
							if (s) {
								solverTable.fnDraw(false);
								benchTable.fnDraw(false);
								handleSelectChange();
							}
							
						},  
						"json"
				).error(function(){
					showMessage('error',"Internal error linking primitives",5000);
				});	
			},
			"cancel": function() {
				$(this).dialog("close");
			}
		}		
	});
}

function recycleOrphaned(prim) {
	$('#dialog-confirm-recycle-txt').text('Are you sure you want to recycle all of your orphaned ' +prim +'(s)?');

	// Display the confirmation dialog
	$('#dialog-confirm-recycle').dialog({
		modal: true,
		height: 220,
		buttons: {
			'recycle': function() {
				$("#dialog-confirm-recycle").dialog("close");
				createDialog("Recycling the selected "+prim+"(s), please wait. This will take some time for large numbers of "+prim+"(s).");
				$.post(  
						starexecRoot +"services/recycleOrphaned/"+prim+"/"+userId, 
						{},
						function(returnCode){
							destroyDialog();
							s=parseReturnCode(returnCode);
							if (s) {
								solverTable.fnDraw(false);
								benchTable.fnDraw(false);
								handleSelectChange();
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
						{selectedIds : getSelectedRows(jTable)},
						function(nextDataTablePage){
							destroyDialog();
							s=parseReturnCode(nextDataTablePage);
							if (s) {
								jTable.fnDraw(false);
								handleSelectChange();
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

function deleteOrphanedJobs() {
	$('#dialog-confirm-delete-txt').text('Are you sure you want to delete all the selected job(s)? After deletion, they can not be recovered');
	// Display the confirmation dialog
	$('#dialog-confirm-delete').dialog({
		modal: true,
		height: 220,
		buttons: {
			'delete permanently': function() {
				$("#dialog-confirm-delete").dialog("close");
				createDialog("Deleting the selected job(s), please wait. This will take some time for large numbers of jobs(s).");
				$.post(  
						starexecRoot +"services/deleteOrphaned/job/"+userId, 
						{},
						function(nextDataTablePage){
							destroyDialog();
							s=parseReturnCode(nextDataTablePage);
							if (s) {
								jTable.fnDraw(false);
								handleSelectChange();
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

/**
 * Called when any item is starting to be dragged within the browser
 */
function onDragStart(event, ui) {
	// Make each space in the explorer list be a droppable target; moving this from the initDraggable()
	// fixed the bug where spaces that were expanded after initDraggable() was called would not be 
	// recognized as a viable drop target
	$('#exploreList').find('a').droppable( {
		drop		: onSpaceDrop,
		tolerance	: 'pointer',	// Use the pointer to determine drop position instead of the middle of the drag clone element
        
		activeClass	: 'active'		// Class applied to the space element when something is being dragged
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

	
	if(ids.length < 2) {
		// If 0 or 1 things are selected in the table, just use the element that is being dragged
		ids = [ui.draggable.data('id')];

		// Customize the confirmation message for the copy operation to the primitives/spaces involved
		if(ui.draggable.data('type')[0] == 's'){
			$('#dialog-confirm-copy-txt').text('do you want to link ' + ui.draggable.data('name') + ' to' + destName + ' and all of its subspaces or just to' + destName +'?');
		}
		//job or benchmark
		else {
			$('#dialog-confirm-copy-txt').text('do you want to link ' + ui.draggable.data('name') + ' to' + destName + '?');
		}
	} else {
		if(ui.draggable.data('type')[0] == 's' || ui.draggable.data('type')[0] == 'u'){
			$('#dialog-confirm-copy-txt').text('do you want to link the ' + ids.length + ' selected '+ ui.draggable.data('type') + 's to' + destName + ' and all of its subspaces or just to' + destName +'?');
		}
		//job or benchmark
		else {
			$('#dialog-confirm-copy-txt').text('do you want to link the ' + ids.length + ' selected ' + ui.draggable.data('type') + 's to' + destName + '?');		
		}
	}		

	// If primitive being copied to another space is a solver...
	if(ui.draggable.data('type')[0] == 's' && ui.draggable.data('type')[1] != 'p'){
		// Display the confirmation dialog
		$('#dialog-confirm-copy').dialog({
			modal: true,
			width: 500,
			height: 200,
			
			//depending on what the user 
			buttons: {
				'link in space hierarchy': function() {
					$('#dialog-confirm-copy').dialog('close'); 
					doSolverLinkPost(ids,destSpace,true);
				},
				'link in space': function(){
					$('#dialog-confirm-copy').dialog('close');
					doSolverLinkPost(ids,destSpace,false);
				},
				"cancel": function() {
					$(this).dialog("close");
				}
				
			}		
		});	
	}
	// Otherwise, if the primitive being copied to another space is a benchmark
	else if(ui.draggable.data('type')[0] == 'b') {
		// Display the confirmation dialog
		$('#dialog-confirm-copy').dialog({
			modal: true,
			buttons: {
				'link':function() {
					$('#dialog-confirm-copy').dialog('close');
					$.post(  	    		
							starexecRoot+'services/spaces/' + destSpace + '/add/benchmark', // We use the type to denote copying a benchmark/job
							{selectedIds : ids, copy:false},	
							function(returnCode) {
								parseReturnCode(returnCode);
							},
							"json"
					).error(function(){
						showMessage('error',"Internal error copying benchmarks",5000);
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
							starexecRoot+'services/spaces/' + destSpace + '/add/job',
							{selectedIds : ids},	
							function(returnCode) {
								parseReturnCode(returnCode);
							},
							"json"
					).error(function(){
						showMessage('error',"Internal error copying jobs",5000);
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

function doSolverLinkPost(ids,destSpace,hierarchy) {
	// Make the request to the server
	$.post(  	    		
			starexecRoot+'services/spaces/' + destSpace + '/add/solver',
			{selectedIds : ids,copyToSubspaces: hierarchy, copy : false},
			function(returnCode) {
				parseReturnCode(returnCode);
			},
			"json"
	).error(function(){
		showMessage('error',"Internal error copying solvers",5000);
	});	
}
