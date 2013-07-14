/** Global Variables */
var userTable;
var benchTable;
var solverTable;
var spaceTable;
var commentTable;
var resultTable;
var jobTable;
var spaceId;			// id of the current space
var spaceName;			// name of the current space


$(document).ready(function(){	

	// Build the tooltip styles (i.e. dimensions, color, etc)
	initTooltipStyles();

	// Build left-hand side of page (space explorer)
	initSpaceExplorer();

	// Build right-hand side of page (space details)
	initSpaceDetails();
	
	//redraw the job table every 10 seconds so we can see continuous results
	setInterval(function() {
		if (spaceId!=1 && spaceId!=undefined) {
			rows = $(jobTable).children('tbody').children('tr.row_selected');
			if (rows.length==0) {
				jobTable.fnDraw(false);
			}	
		}
		
	},10000);


});


/**
 * Convenience method for determining if a given fieldset has been expanded or not
 *  
 * @param fieldset the id of the fieldset to check for expansion (first letter must be a '#' symbol)
 * @returns {Boolean} true iff the given fieldset has been expanded
 * @author Todd Elvers
 */
function isFieldsetOpen(fieldset){
	if($(fieldset + ' span:last-child').text() == ' (+)'){
		return false;
	} else { 
		return true;
	}
}

/**
 * Hides all jquery ui dialogs for page startup
 */
function initDialogs() {	
	$( "#dialog-confirm-copy" ).hide();
	$( "#dialog-confirm-delete" ).hide();
	log('all confirmation dialogs hidden');
}

/**
 * Sets up the 'space details' that consumes the right-hand side of the page
 */
function initSpaceDetails(){

	// builds the DataTable objects and enables multi-select on them
	initDataTables();

	// Set up jQuery UI dialog boxes
	initDialogs();

	// Set up jQuery button UI
	initButtonUI();

	// Set up comment table
	initCommentUI();

	// This hides comment table and action list if the space is root space or we aren't looking at a space
	if (spaceId == 1 || spaceId == undefined){
		$('#commentDiv').hide();
		$('#actionList').hide();
	}

	pbc = false;
}

/**
 * Initialize UI needed for commenting system
 * Also, display comments in the table
 */
function initCommentUI(){

	// Setup '+ add comment' animation
	$('#toggleComment').click(function() {
		$('#new_comment').slideToggle('fast');
		togglePlusMinus(this);
	});	

	$('#new_comment').hide();

	// Handles adding a new comment - Vivek
	$("#addComment").click(function(){
		var comment = HtmlEncode($("#comment_text").val());
		if(comment.trim().length == 0) {
			showMessage('error', 'comments can not be empty', 6000);
			return;
		}	
		var data = {comment: comment};
		$.post(
				starexecRoot+"services/comments/add/space/" + spaceId,
				data,
				function(returnCode) {
					if(returnCode == '0') {
						$("#comment_text").val("");
						$.getJSON(starexecRoot+'services/comments/space/' + spaceId, displayComments).error(function(){
							showMessage('error',"Internal error getting comments",5000);
						});
					} else {
						showMessage('error', "adding your comment was unsuccessful; please try again", 5000);

					}
				},
				"json"
		);

	});
}
/**
 * Basic initialization for jQuery UI buttons (sets style and icons)
 */
function initButtonUI() {

	$('.btnAdd').button({
		icons: {
			secondary: "ui-icon-plus"
		}});

	$('.btnUp').button({
		icons: {
			secondary: "ui-icon-arrowthick-1-n"
		}});

	$('.btnDown').button({
		icons: {
			secondary: "ui-icon-arrowthick-1-s"
		}});

	$('.btnRun').button({
		icons: {
			secondary: "ui-icon-gear"
		}});

	$('.btnRemove').button({
		icons: {
			secondary: "ui-icon-minus"
		}});	

	$('.btnEdit').button({
		icons: {
			secondary: "ui-icon-pencil"
		}});

	$('#trashcan').button({
		icons: {
			secondary: "ui-icon-trash"
		}});
	$('#trashcan').hide();

	$('#addComment').button({
		icons: {
			secondary: "ui-icon-plus"
		}});

	$('.resetButton').button({
		icons: {
			secondary: "ui-icon-closethick"
		}});

	$("#makePublic").click(function(){
		// Display the confirmation dialog
		$('#dialog-confirm-copy-txt').text('do you want to make the single space public or the hierarchy?');
		$('#dialog-confirm-copy').dialog({
			modal: true,
			width: 380,
			height: 165,
			buttons: {
				'space': function(){
					$.post(
							starexecRoot+"services/space/makePublic/" + spaceId + "/" + false,
							{},
							function(returnCode) {
								window.location.reload(true);
							},
							"json"
					);
				},
				'hierarchy': function(){
					$.post(
							starexecRoot+"services/space/makePublic/" + spaceId + "/" + true,
							{},
							function(returnCode) {
								window.location.reload(true);
							},
							"json"
					);
				},
				"cancel": function() {
					log('user canceled making public action');
					$(this).dialog("close");
				}
			}
		});
	});

	$("#makePrivate").click(function(){
		// Display the confirmation dialog
		$('#dialog-confirm-copy-txt').text('do you want to make the single space private or the hierarchy?');
		$('#dialog-confirm-copy').dialog({
			modal: true,
			width: 380,
			height: 165,
			buttons: {
				'space': function(){
					$.post(
							starexecRoot+"services/space/makePrivate/" + spaceId + "/" + false,
							{},
							function(returnCode) {
								window.location.reload(true);
							},
							"json"
					);
				},
				'hierarchy': function(){
					$.post(
							starexecRoot+"services/space/makePrivate/" + spaceId + "/" + true,
							{},
							function(returnCode) {
								window.location.reload(true);
							},
							"json"
					);
				},
				"cancel": function() {
					log('user canceled making private action');
					$(this).dialog("close");
				}
			}
		});
	});

	log('jQuery UI buttons initialized');
}

/**
 * Initializes a table so that elements can be dragged out of it and onto a space name
 * @param table The table to make draggable
 * @author Tyler Jensen & Todd Elvers
 */
function initDraggable(table) {
	var rows = $(table).children('tbody').children('tr');

	// Using jQuery UI, make the first column in each row draggable
	rows.draggable({
		cursorAt: { cursor: 'move', left: -1, bottom: -1},	// Set the cursor to the move icon and make it start in the corner of the helper		
		containment: 'document',							// Allow the element to be dragged anywhere in the document
		distance: 20,										// Only trigger a drag when the distanced dragged is > 20 pixels
		scroll: true,										// Scroll with the page as the item is dragged if needed
		helper: getDragClone,								// The method that returns the 'cloned' element that is dragged
		start: onDragStart,									// Method called when the dragging begins
		stop: onDragStop									// Method called when the dragging ends
	});
	

	// Set the JQuery variables used during the drag/drop process
	$.each(rows, function(i, row){
		$(row).data("id", $(row).children('td:first-child').children('input').val());
		$(row).data("type", $(row).children('td:first-child').children('input').attr('prim'));

		// if it is comment then do not display the first field
		if($(row).data('type') !== undefined && $(row).data('type')[0] == 'c'){
			$(row).data("name","this comment");
		}else{
			$(row).data("name", $(row).children('td:first-child').children('a').text());
		}
	});

	// Make the trash can in the explorer list be a droppable target
	$('#trashcan').droppable({
		drop		: onTrashDrop,
		tolerance	: 'touch',	// Use the pointer to determine drop position instead of the middle of the drag clone element
		hoverClass	: 'hover',		// Class applied to the space element when something is being dragged over it
		activeClass	: 'active'		// Class applied to the space element when something is being dragged
	});
	

	log($(table).attr('id') + ' table initialized as draggable');
}

/*
 * @Author Eric Burns
 * The following function is executed while the page scrolls
 * and moves the trashcan draggable target along with the page
 */


$(window).scroll(function(){
	var scrolldown= ($(document).scrollTop());
	$("#trashcan").css("top", scrolldown+"px");
	if (!$("#trashcan").css("display")=="none") {
		$("#trashcan").hide();
		$("#trashcan").show(); //required to move drop target
	}
});

/**
 * Called when any item is starting to be dragged within the browser
 */
function onDragStart(event, ui) {
	log('drag started');

	// Make each space in the explorer list be a droppable target; moving this from the initDraggable()
	// fixed the bug where spaces that were expanded after initDraggable() was called would not be 
	// recognized as a viable drop target
	$('#exploreList').find('a').droppable( {
		drop		: onSpaceDrop,
		tolerance	: 'pointer',	// Use the pointer to determine drop position instead of the middle of the drag clone element
        
		activeClass	: 'active'		// Class applied to the space element when something is being dragged
	});

	$('#trashcan').show();
}

/**
 * Called when there is no longer anything being dragged
 */
function onDragStop(event, ui) {
	log('drag stopped');
	$('#trashcan').hide();
}

/**
 * Called when a draggable item (primitive) is dropped on the trash can
 * @author Todd Elvers
 */
function onTrashDrop(event, ui){
	// Collect the selected elements from the table being dragged from
	var ids = getSelectedRows($(ui.draggable).parents('table:first'));

	if(ids.length < 2) {
		// If 0 or 1 things are selected in the table, just use the element that is being dragged
		ids = [ui.draggable.data('id')];
	}

	// Call the appropriate primitive removal function
	switch(ui.draggable.data('type')[0]){
	case 'u':
		removeUsers(ids);
		break;
	case 's':
		if(ui.draggable.data('type')[1] == 'o'){
			removeSolvers(ids);
		} else {
			quickRemove(ids);//actual Remove called within here
		}
		break;
	case 'b':
		removeBenchmarks(ids);
		break;
	case 'j':
		removeJobs(ids);
		break;
	case 'c' :
		removeComment(ids);
		break;
	}
}

/**
 * Shows an error message given an error code returned by any of the delete requests
 * @param errorCode An integer error code
 * @param prim The type of the primitive that was being deleted when the error was triggered 
 * (solver, user, benchmark, etc.)
 * @author Eric Burns
 */

function processDeleteErrorCode(errorCode,prim) {
	switch (errorCode) {
	case 1: 
		showMessage('error', "an error occurred while processing your request; please try again", 5000);
		break;
	case 2:
		showMessage('error', "only the owner of a " +prim+ " can delete it", 5000);
		break;
	}
}

/**
 * Shows an error message given an error code returned by any of the remove requests
 * @param errorCode An integer error code
 * @param prim The type of the primitive that was being removed from a space when the error was triggered 
 * (solver, user, benchmark, etc.)
 * @author Eric Burns
 */

function processRemoveErrorCode(errorCode,prim) {
	switch (errorCode) {
	case 1:
		showMessage('error', "an error occurred while processing your request; please try again", 5000);
		break;
	case 2:
		showMessage('error', "you do not have sufficient privileges to remove " +prim+ "s from this space", 5000);
		break;
	case 3:
		showMessage('error', "you do not have permission to remove "+prim+"s from one of the subspaces", 5000);
		break;
	case 4:
		showMessage('error', "you can not remove other leaders of this space", 5000);
		break;
	case 5:
		showMessage('error', "you can not remove yourself from this space in that way, " +
				"instead use the 'leave' button to leave this community", 5000);
		break;
	case 6:
		showMessge('error', "one of the users you are trying to remove is a leader of one of the subspaces", 5000);
		break;
	}
}

/**
 * Shows an error message given an error code returned by any of the copying or linking requests
 * @param errorCode An integer error code
 * @param prim The type of the primitive that was being copied or linked when the error was triggered 
 * (solver, user, benchmark, etc.)
 * @param destName The name of the space that was being copied too
 * @author Eric Burns
 */

function processCopyErrorCode(errorCode, prim, destName) {
	switch (errorCode) {
	case 1: // Database error
		showMessage('error', "a database error occurred while processing your request", 5000);
		break;
	case 3: // Invalid parameters
		showMessage('error', "invalid parameters supplied to server, operation failed", 5000);
		break;
	case 2: // No add permission in dest space
		showMessage('error', "you do not have permission to add " +prim+ " to" + destName, 5000);
		break;
	case 4: // User doesn't belong to from space
		showMessage('error', "you do not belong to the space that is being copied or linked from", 5000);
		break;
	case 5: // From space is locked
		showMessage('error', "the space leader has indicated the current space is locked. you cannot copy or link from locked spaces.", 5000);
		break;
	case 6: // User doesn't have addSolver permission in one or more of the subspaces of the 'from space'
		showMessage('error', "you do not have permissions to copy or link " +prim+ " to one of the subspaces of" + destName, 5000);
		break;
	case 7: // There exists a solver with the same name
		showMessage('error', "there exists a " +prim.substring(0,prim.length-1)+ " with the same name in " + destName, 5000);
		break;
	case 8: //user tried to copy without having enough disk quota
		showMessage('error',"you do not have sufficient disk quota to copy the selected "+prim,5000);
		break;
	case 9:
		showMessage('error',"one or more of the selected "+prim+"(s) could not be copied correctly", 5000);
		break;
	case 11:
		showMessage('error',"one or more of the selected "+prim+"(s) have already been deleted",5000);
		break;
	default:
		showMessage('error', "the operation failed with an unknown return code", 5000);	
	}
}


/**
 * Called when a draggable item (primitive) is dropped on a space
 */
function onSpaceDrop(event, ui) {
	// Prevents comments from being copied to other spaces
	if (ui.draggable.data('type')[0] == 'c'){
		showMessage('error','you can not copy a comment to another space',5000);
		return;
	}

	// Collect the selected elements from the table being dragged from
	var ids = getSelectedRows($(ui.draggable).parents('table:first'));

	// Get the destination space id and name
	var destSpace = $(event.target).parent().attr('id');
	var destName = $(event.target).text();

	log(ids.length + ' rows dropped onto ' + destName);
	
	if(ids.length < 2) {
		// If 0 or 1 things are selected in the table, just use the element that is being dragged
		ids = [ui.draggable.data('id')];

		// Customize the confirmation message for the copy operation to the primitives/spaces involved
		if(ui.draggable.data('type')[0] == 's' && ui.draggable.data('type')[1] == 'p'){
			$('#dialog-confirm-copy-txt').text('do you want to copy the ' + ui.draggable.data('name') + ' only or the hierarchy to' + destName +'?');
		}
		else if(ui.draggable.data('type')[0] == 's' || ui.draggable.data('type')[0] == 'u'){
			$('#dialog-confirm-copy-txt').text('do you want to copy ' + ui.draggable.data('name') + ' to' + destName + ' and all of its subspaces or just to' + destName +'?');
		} else if (ui.draggable.data('type')[0]=='j') {
			$('#dialog-confirm-copy-txt').text('do you want to link ' + ui.draggable.data('name') + ' in' + destName + '?');
		}
		else {
			$('#dialog-confirm-copy-txt').text('do you want to copy or link ' + ui.draggable.data('name') + ' to' + destName + '?');
		}
	} else {
		if(ui.draggable.data('type')[0] == 's' && ui.draggable.data('type')[1] == 'p'){
			$('#dialog-confirm-copy-txt').text('do you want to copy the ' + ids.length + ' selected spaces only or the hierarchy to' + destName +'?');
		}
		else if(ui.draggable.data('type')[0] == 's' || ui.draggable.data('type')[0] == 'u'){
			$('#dialog-confirm-copy-txt').text('do you want to copy the ' + ids.length + ' selected '+ ui.draggable.data('type') + 's to' + destName + ' and all of its subspaces or just to' + destName +'?');
		} else if (ui.draggable.data('type')[0]=='j') {
			$('#dialog-confirm-copy-txt').text('do you want to link the ' + ids.length + ' selected ' + ui.draggable.data('type') + 's in' + destName + '?');		

		}else {
			$('#dialog-confirm-copy-txt').text('do you want to copy or link the ' + ids.length + ' selected ' + ui.draggable.data('type') + 's to' + destName + '?');		
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
					doSolverCopyPost(ids,destSpace,spaceId,true,false,destName);
				},
				'copy to space hierarchy': function() {
					$('#dialog-confirm-copy').dialog('close'); 
					doSolverCopyPost(ids,destSpace,spaceId,true,true,destName);
				},
				'link in space': function(){
					$('#dialog-confirm-copy').dialog('close');
					doSolverCopyPost(ids,destSpace,spaceId,false,false,destName);
				},
				'copy to space': function() {
					$('#dialog-confirm-copy').dialog('close');	
					doSolverCopyPost(ids,destSpace,spaceId,false,true,destName);
				},
				"cancel": function() {
					$(this).dialog("close");
				}
				
			}		
		});	
	}
	// If primitive being copied to another space is a user...
	else if(ui.draggable.data('type')[0] == 'u'){
		$('#dialog-confirm-copy').dialog({
			modal: true,
			width: 380,
			height: 165,
			buttons: {
				'space hierarchy': function() {
					// If the user actually confirms, close the dialog right away
					$('#dialog-confirm-copy').dialog('close');
					// Make the request to the server	
					doUserCopyPost(ids,destSpace,spaceId,true,destName);
								
											
				},
				'space': function(){
					// If the user actually confirms, close the dialog right away
					$('#dialog-confirm-copy').dialog('close');
					doUserCopyPost(ids,destSpace,spaceId,false,destName);
						
				},
				"cancel": function() {
					log('user canceled copy action');
					$(this).dialog("close");
				}
			}		
		});		
	}

	// If copying subspaces to other spaces
	else if(ui.draggable.data('type')[0] == 's' && ui.draggable.data('type')[1] == 'p'){
		// Display the confirmation dialog
		$('#dialog-confirm-copy').dialog({
			modal: true,
			width: 380,
			height: 165,
			buttons: {
				'space': function(){
					// If the user actually confirms, close the dialog right away
					$('#dialog-confirm-copy').dialog('close');

					// Making the request		
					doSpaceCopyPost(ids,destSpace,spaceId,false,destName);
					
				},
				'hierarchy': function(){
					// If the user actually confirms, close the dialog right away
					$('#dialog-confirm-copy').dialog('close');

					// Making the request
					doSpaceCopyPost(ids,destSpace,spaceId,true,destName);
				},
				"cancel": function() {
					log('user canceled copy action');
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
				'copy': function() {
					// If the user actually confirms, close the dialog right away
					$('#dialog-confirm-copy').dialog('close');
					doBenchmarkCopyPost(ids,destSpace,spaceId,true,destName);
						 									
				},
				'link':function() {
					$('#dialog-confirm-copy').dialog('close');
					doBenchmarkCopyPost(ids,destSpace,spaceId,false,destName);
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
					log('user confirmed copy action');

					// If the user actually confirms, close the dialog right away
					$('#dialog-confirm-copy').dialog('close');

					// Make the request to the server				
					$.post(  	    		
							starexecRoot+'services/spaces/' + destSpace + '/add/job',
							{selectedIds : ids, fromSpace : spaceId},	
							function(returnCode) {
								log('AJAX response recieved with code ' + returnCode);
								if (returnCode==0) {
									if(ids.length > 1) {								
										showMessage('success', ids.length + ' ' + 'jobs successfully linked in' + destName, 2000);
									} else {					    		
										showMessage('success', 'job successfully copied to' + destName, 2000);	
									}
								}else {
									processCopyErrorCode(returnCode, "jobs",destName);
								}
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

function doSpaceCopyPost(ids,destSpace,spaceId,copyHierarchy,destName) {
	$.post(  	    		
			starexecRoot+'services/spaces/' + destSpace + '/copySpace',
			{selectedIds : ids, fromSpace : spaceId, copyHierarchy: copyHierarchy},
			function(returnCode) {
				log('AJAX response recieved with code ' + returnCode);
				if (returnCode==0) {							
					showMessage('success', ids.length + ' subSpaces successfully copied to' + destName, 2000);
					$('#exploreList').jstree("refresh");
				} else {
					processCopyErrorCode(returnCode, "subspaces", destName);
				}
			},
			"json"
	).error(function(){
		showMessage('error',"Internal error copying spaces",5000);
	});
}

function doUserCopyPost(ids,destSpace,spaceId,copyToSubspaces,destName){
	$.post(  	    		
			starexecRoot+'services/spaces/' + destSpace + '/add/user',
			{selectedIds : ids, fromSpace : spaceId, copyToSubspaces: copyToSubspaces},	
			function(returnCode) {
				log('AJAX response recieved with code ' + returnCode);
				if (returnCode==0) {
					if(ids.length > 1) {								
						showMessage('success', ids.length + ' users successfully copied to' + destName + ' and its subspaces', 2000);
					} else {					    		
						showMessage('success', ui.draggable.data('name') + ' successfully copied to' + destName + ' and its subspaces', 2000);	
					}
				}else {
						processCopyErrorCode(returnCode, "users",destName);
					}
			},
			"json"
	).error(function(){
		showMessage('error',"Internal error copying users",5000);
	});				
}

/**
 * Sends a copy benchmark request to the server
 * @param ids The IDs of the benchmarks to copy
 * @param destSpace The ID of the destination space
 * @param spaceId The ID of the from space
 * @param copy A boolean indicating whether to copy (true) or link (false).
 * @param destName The name of the destination space
 * @author Eric Burns
 */

function doBenchmarkCopyPost(ids,destSpace,spaceId,copy,destName) {
	// Make the request to the server		
	copyOrLink="linked";
	if (copy) {
		copyOrLink="copied";
	}
	$.post(  	    		
			starexecRoot+'services/spaces/' + destSpace + '/add/benchmark', // We use the type to denote copying a benchmark/job
			{selectedIds : ids, fromSpace : spaceId, copy:copy},	
			function(returnCode) {
				log('AJAX response recieved with code ' + returnCode);
				if (returnCode==0) {
					if(ids.length > 1) {								
						showMessage('success', ids.length + ' ' + 'benchmarks successfully ' +copyOrLink+ ' to ' + destName, 2000);
					} else {					    		
						showMessage('success', 'benchmark successfully '+copyOrLink+' to ' + destName, 2000);	
					}
				}else {
					processCopyErrorCode(returnCode,"benchmarks",destName);
				}
			},
			"json"
	).error(function(){
		showMessage('error',"Internal error copying benchmarks",5000);
	});
}

/**
 * Sends a copy solver request to the server
 * @param ids The IDs of the solvers to copy
 * @param destSpace The ID of the destination space
 * @param spaceId The ID of the from space
 * @param copy A boolean indicating whether to copy (true) or link (false).
 * @param destName The name of the destination space
 * @author Eric Burns
 */

function doSolverCopyPost(ids,destSpace,spaceId,hierarchy,copy,destName) {
	// Make the request to the server
	copyOrLink="linked";
	if (copy) {
		copyOrLink="copied";
	}
	$.post(  	    		
			starexecRoot+'services/spaces/' + destSpace + '/add/solver',
			{selectedIds : ids, fromSpace : spaceId, copyToSubspaces: hierarchy, copy : copy},
			function(returnCode) {
				
				if (returnCode==0) {
					
					if(ids.length > 1) {	
						if (hierarchy) {
							showMessage('success', ids.length + ' solvers successfully ' +copyOrLink+ ' to ' + destName + ' and its subspaces', 2000);
						} else {
							showMessage('success', ids.length + ' solvers successfully ' +copyOrLink+ ' to ' + destName, 2000);
						}
						
					} else {	
						
						if (hierarchy) {
							showMessage('success', ids.length + ' solvers successfully ' +copyOrLink+  ' to ' + destName + ' and its subspaces', 2000);
						} else {
							
							showMessage('success', ids.length + ' solvers successfully ' +copyOrLink+ ' to ' + destName, 2000);
						}
					}
				}else {
						processCopyErrorCode(returnCode,"solvers", destName);
				}
			},
			"json"
	).error(function(){
		showMessage('error',"Internal error copying solvers",5000);
	});	
}

/**
 * Returns the html of an element that is dragged along with the mouse when an item is dragged on the page
 * @author Tyler Jensen
 */
function getDragClone(event) {	
	var src = $(event.currentTarget);	
	if(false == $(src).hasClass('row_selected')){ //change
		$(src).addClass('row_selected');
	}
	var ids = getSelectedRows($(src).parents('table:first'));
	var txtDisplay = $(src).children(':first-child').text();
	var icon = 'ui-icon ';	
	var primType = $(src).data('type');
	log(src);


	if(ids.length > 1) {
		txtDisplay = ids.length + ' ' + primType + 's';
	}

	// Change the drag icon based on what the type of object being dragged is
	switch(primType[0]){
	case 'u':
		icon += 'ui-icon-person';
		break;
	case 'b':
		icon += 'ui-icon-script';
		break;
	case 'j':
		icon += 'ui-icon-gear';
		break;
	case 'c':
		icon += 'ui-icon-newwin';
		if(ids.length <= 1){
			txtDisplay = 'comment';
		}
		break;
	default:
		icon += 'ui-icon-newwin';
	break;
	}

	// Return a styled div with the name of the element that was originally dragged
	return '<div class="dragClone"><span class="' + icon + '"></span>' + txtDisplay + '</div>';
}

/**
 * Creates the space explorer tree for the left-hand side of the page, also
 * creates tooltips for the space explorer, .expd class, and userTable (if applicable)
 * @author Tyler Jensen & Todd Elvers & Skylar Stark
 */
function initSpaceExplorer(){
	// Set the path to the css theme for the jstree plugin
	$.jstree._themes = starexecRoot+"css/jstree/";
	var id;
	// Initialize the jstree plugin for the explorer list
	$("#exploreList").jstree({  
		"json_data" : { 
			"ajax" : { 
				"url" : starexecRoot+"services/space/subspaces",	// Where we will be getting json data from 
				"data" : function (n) {
					return { id : n.attr ? n.attr("id") : -1 }; // What the default space id should be
				} 
			} 
		}, 
		"themes" : { 
			"theme" : "default", 					
			"dots" : true, 
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
						"image" : starexecRoot+"images/jstree/db.png"
					}
				}
			}
		},
		"ui" : {			
			"select_limit" : 1,			
			"selected_parent_close" : "select_parent",			
			"initially_select" : [ "1" ]			
		},
		"plugins" : [ "types", "themes", "json_data", "ui", "cookies"] ,
		"core" : { animation : 200 }
	}).bind("select_node.jstree", function (event, data) {
		// When a node is clicked, get its ID and display the info in the details pane
		id = data.rslt.obj.attr("id");
		log('Space explorer node ' + id + ' was clicked');

		updateButtonIds(id);
		getSpaceDetails(id);

		/* Commenting out the comment system until it is more polished */
//		getSpaceComments(id);

		// Remove all non-permanent tooltips from the page; helps keep
		// the page from getting filled with hundreds of qtip divs
		$(".qtip-userTooltip").remove();
		$(".qtip-expdTooltip").remove();
	}).delegate("a", "click", function (event, data) { event.preventDefault();  });// This just disable's links in the node title

	log('Space explorer node list initialized');
}

/**
 * Handles removal of benchmark(s) from a space
 * @author Todd Elvers
 */
function removeBenchmarks(selectedBenches){
	$('#dialog-confirm-delete-txt').text('Do you want to remove the selected benchmark(s) from ' + spaceName + ', or would you like  to delete them permanently?');

	// Display the confirmation dialog
	$('#dialog-confirm-delete').dialog({
		modal: true,
		height: 220,
		buttons: {
			'remove benchmark': function() {
				log('user confirmed benchmark removal');
				// If the user actually confirms, close the dialog right away
				$('#dialog-confirm-delete').dialog('close');

				$.post(  
						starexecRoot+"services/remove/benchmark/" + spaceId,
						{selectedIds : selectedBenches},
						function(returnCode) {
							log('AJAX response received with code ' + returnCode);
							switch (returnCode) {
							case 0:
								// Remove the rows from the page and update the table size in the legend
								updateTable(benchTable);
								break;
							default:
								processRemoveErrorCode(returnCode,"benchmark");
							} 
						},
						"json"
				).error(function(){
					showMessage('error',"Internal error removing benchmarks",5000);
				});		
			},
			'delete permanently': function() {
				log('user confirmed benchmark deletion');
				// If the user actually confirms, close the dialog right away
				$('#dialog-confirm-delete').dialog('close');

				$.post(  
						starexecRoot+"services/delete/benchmark",
						{selectedIds : selectedBenches},
						function(returnCode) {
							log('AJAX response received with code ' + returnCode);
							switch (returnCode) {
							case 0:
								// Remove the rows from the page and update the table size in the legend
								updateTable(benchTable);
								break;
							default:
								processDeleteErrorCode(returnCode,"benchmark");
							} 
						},
						"json"
				).error(function(){
					showMessage('error',"Internal error removing benchmarks",5000);
				});		
			},
			"cancel": function() {
				log('user canceled benchmark deletion');
				$(this).dialog("close");
			}
		}		
	});				
}	

/**
 * Handles removal of user(s) from a space
 * @author Todd Elvers & Skylar Stark
 */
function removeUsers(selectedUsers){
	$('#dialog-confirm-delete-txt').text('do you want to remove the user(s) from ' + spaceName + ' and its hierarchy or just from ' +spaceName + '?');

	// Display the confirmation dialog
	$('#dialog-confirm-delete').dialog({
		modal: true,
		width: 380,
		height: 165,
		buttons: {
			'space hierarchy': function() {
				log('user confirmed user deletion from space and its hierarchy');
				// If the user actually confirms, close the dialog right away
				$('#dialog-confirm-delete').dialog('close');

				$.post(  
						starexecRoot+"services/remove/user/" + spaceId,
						{selectedIds : selectedUsers, hierarchy : true},
						function(returnCode) {
							log('AJAX response received with code ' + returnCode);
							switch (returnCode) {
							case 0:
								// Remove the rows from the page and update the table size in the legend
								updateTable(userTable);
								break;
							default:
								processRemoveErrorCode(returnCode,"user");
						}
							},
						"json"
				).error(function(){
					showMessage('error',"Internal error removing users",5000);
				});	
			},
			"space": function() {
				log('user confirmed user deletion');
				// If the user actually confirms, close the dialog right away
				$('#dialog-confirm-delete').dialog('close');

				$.post(  
						starexecRoot+"services/remove/user/" + spaceId,
						{selectedIds : selectedUsers, hierarchy : false},
						function(returnCode) {
							log('AJAX response received with code ' + returnCode);
							switch (returnCode) {
							case 0:
								// Remove the rows from the page and update the table size in the legend
								updateTable(userTable);
								break;
							default:
								processRemoveErrorCode(returnCode,"user");
							}
						},
						"json"
				).error(function(){
					showMessage('error',"Internal error removing users",5000);
				});	
			},
			"cancel": function() {
				log('user canceled user deletion');
				$(this).dialog("close");
			}
		}		
	});
}

/**
 * Handles removal of solver(s) from a space
 * @author Todd Elvers & Skylar Stark
 */
function removeSolvers(selectedSolvers){
	$('#dialog-confirm-delete-txt').text('do you want to remove the solver(s) from ' + spaceName + ', from ' +spaceName +' and its hierarchy, or would you like to delete them permanently?');

	// Display the confirmation dialog
	$('#dialog-confirm-delete').dialog({
		modal: true,
		width: 380,
		height: 250,
		buttons: {
			'remove from space hierarchy': function() {
				log('user confirmed solver removal from space and its hierarchy');
				// If the user actually confirms, close the dialog right away
				$('#dialog-confirm-delete').dialog('close');

				$.post(  
						starexecRoot+"services/remove/solver/" + spaceId,
						{selectedIds : selectedSolvers, hierarchy : true},
						function(returnCode) {
							log('AJAX response received with code ' + returnCode);
							switch (returnCode) {
							case 0:
								// Remove the rows from the page and update the table size in the legend
								updateTable(solverTable);
								break;
							default:
								processRemoveErrorCode(returnCode,"solver");
							}
						},
						"json"
				).error(function(){
					showMessage('error',"Internal error removing solvers",5000);
				});
			},
			'remove from space': function() {
				log('user confirmed solver removal');
				// If the user actually confirms, close the dialog right away
				$('#dialog-confirm-delete').dialog('close');

				$.post(  
						starexecRoot+"services/remove/solver/" + spaceId,
						{selectedIds : selectedSolvers, hierarchy : false},
						function(returnCode) {
							log('AJAX response received with code ' + returnCode);
							switch (returnCode) {
							case 0:
								// Remove the rows from the page and update the table size in the legend
								updateTable(solverTable);
								break;
							default:
								processRemoveErrorCode(returnCode,"solver");
							}
						},
						"json"
				).error(function(){
					showMessage('error',"Internal error removing solvers",5000);
				});
			},
			'delete permanently': function() {
				log('user confirmed solver deletion');
				// If the user actually confirms, close the dialog right away
				$('#dialog-confirm-delete').dialog('close');

				$.post(  
						starexecRoot+"services/delete/solver",
						{selectedIds : selectedSolvers, hierarchy : true},
						function(returnCode) {
							log('AJAX response received with code ' + returnCode);
							switch (returnCode) {
							case 0:
								// Remove the rows from the page and update the table size in the legend
								updateTable(solverTable);
								break;
							default:
								processDeleteErrorCode(returnCode,"solver");
							}
						},
						"json"
				).error(function(){
					showMessage('error',"Internal error removing solvers",5000);
				});
			},
			"cancel": function() {
				log('user canceled solver deletion');
				$(this).dialog("close");
			}
		}		
	});		
}

/**
 * Handles removal of job(s) from a space
 * @author Todd Elvers
 */
function removeJobs(selectedJobs){
	$('#dialog-confirm-delete-txt').text('do you want to remove the selected job(s) from ' + spaceName + ', or do you want to delete them permanently?');

	// Display the confirmation dialog
	$('#dialog-confirm-delete').dialog({
		modal: true,
		height: 250,
		buttons: {
			'remove jobs': function() {
				jobTable.fnProcessingIndicator();
				log('user confirmed job deletion');
				// If the user actually confirms, close the dialog right away
				$('#dialog-confirm-delete').dialog('close');

				$.post(  
						starexecRoot+"services/remove/job/" + spaceId,
						{selectedIds : selectedJobs},
						function(returnCode) {
							log('AJAX response received with code ' + returnCode);
							switch (returnCode) {
							case 0:
								// Remove the rows from the page and update the table size in the legend
								updateTable(jobTable);
								break;
							default:
								processRemoveErrorCode(returnCode,"job");
							}
							jobTable.fnProcessingIndicator(false);
						},
						"json"
				).error(function(){
					showMessage('error',"Internal error removing jobs",5000);
				});
			},
			'delete permanently': function() {
				jobTable.fnProcessingIndicator();
				log('user confirmed job deletion');
				// If the user actually confirms, close the dialog right away
				$('#dialog-confirm-delete').dialog('close');

				$.post(  
						starexecRoot+"services/delete/job",
						{selectedIds : selectedJobs},
						function(returnCode) {
							log('AJAX response received with code ' + returnCode);
							switch (returnCode) {
							case 0:
								// Remove the rows from the page and update the table size in the legend
								updateTable(jobTable);
								break;
							default:
								processDeleteErrorCode(returnCode,"job");
							}
							jobTable.fnProcessingIndicator(false);
						},
						"json"
				).error(function(){
					showMessage('error',"Internal error removing jobs",5000);
				});
			},
			"cancel": function() {
				log('user canceled job deletion');
				$(this).dialog("close");
			}
		}		
	});		
}

/**
 * Handles removal of subspace(s) from a space
 * @author Todd Elvers
 */
function removeSubspaces(selectedSubspaces,deletePrims){
	$('#dialog-confirm-delete-txt').text('are you sure you want to remove the selected subspace(s), and all their subspaces, from ' + spaceName + '?');
	log('user confirmed (in quickremove dialog) subspace deletion');


	$.post(  
			starexecRoot+"services/remove/subspace/" + spaceId,
			{selectedIds : selectedSubspaces, deletePrims : deletePrims},					
			function(returnCode) {
				log('AJAX response received with code ' + returnCode);
				switch (returnCode) {
				case 0:
					// Remove the rows from the page and update the table size in the legend
					log('actual delete done');
					break;
				default:
					processRemoveErrorCode(returnCode,"subspace");
				}
				//jobTable.fnProcessingIndicator(false);
			},
			"json"
	).error(function(){
		log('remove subspace error');
	});
}




/**
 * Quickly removes association between spaces so actual
 * removal can go on behind the scenes without delay to user.
 * @author Ben McCune
 */
function quickRemove(selectedSubspaces){
	$('#dialog-confirm-delete-txt').text('Do you want to delete the solvers, benchmarks, and jobs in the selected subspace(s), and all their subspaces, or do you only want to remove the selected subspace(s) from ' + spaceName + '?');
	// Display the confirmation dialog
	$('#dialog-confirm-delete').dialog({
		modal: true,
		height: 400,
		width: 400,
		buttons: {
			'delete primitives and remove subspace(s)': function() {
				//spaceTable.fnProcessingIndicator();
				log('user confirmed subspace deletion');
				// If the user actually confirms, close the dialog right away
				$('#dialog-confirm-delete').dialog('close');

				$.post(  
						starexecRoot+"services/quickRemove/subspace/" + spaceId,
						{selectedIds : selectedSubspaces},
						function(returnCode) {
							log('AJAX response received with code ' + returnCode);
							switch (returnCode) {
							case 0:
								// Remove the rows from the page and update the table size in the legend
								updateTable(spaceTable);
								initSpaceExplorer();
								removeSubspaces(selectedSubspaces,true);
								break;
							default:
								processRemoveErrorCode(returnCode,"subspace");
							}
							//jobTable.fnProcessingIndicator(false);
						},
						"json"
				).error(function(){
					log('quick remove subspace error');
					window.location.reload(true);
				});
			},
			"remove subspace(s) only" : function() {
				//spaceTable.fnProcessingIndicator();
				log('user confirmed subspace deletion');
				// If the user actually confirms, close the dialog right away
				$('#dialog-confirm-delete').dialog('close');

				$.post(  
						starexecRoot+"services/quickRemove/subspace/" + spaceId,
						{selectedIds : selectedSubspaces},
						function(returnCode) {
							log('AJAX response received with code ' + returnCode);
							switch (returnCode) {
							case 0:
								// Remove the rows from the page and update the table size in the legend
								updateTable(spaceTable);
								initSpaceExplorer();
								removeSubspaces(selectedSubspaces,false);
								break;
							default:
								processRemoveErrorCode(returnCode,"subspace");
							}
							//jobTable.fnProcessingIndicator(false);
						},
						"json"
				).error(function(){
					log('quick remove subspace error');
					window.location.reload(true);
				});
			},
			"cancel": function() {
				log('user canceled subspace deletion');
				$(this).dialog("close");
			}
		}		
	});		
}

/**
 * Handles removal of a comment from a space
 * @author Vivek Sardeshmukh
 */
function removeComment(ids){
	//only allow a single comment deletion - no mass deletion is allowed
	if(ids.length > 1){
		showMessage('error', "please select only one comment at a time", 5000);
	}
	else{
		var idArray = ids[0].split('-');
		var cid = idArray[0]; 	
		var uid = idArray[1];
		$('#dialog-confirm-delete-txt').text('are you sure you want to delete this comment?');
		$('#dialog-confirm-delete').dialog({
			modal: true,
			buttons: {
				'yes': function() {
					$('#dialog-confirm-delete').dialog('close');
					$.post(
							starexecRoot+"services/comments/delete/space/" + spaceId + "/" + uid + "/" + cid, 
							function(returnData){
								if (returnData == 0) {
									updateTable(commentTable);
								} else if (returnData == 2) {
									showMessage('error',"deleting comments is restricted to the owner of the space and the comment's owner", 5000);
								} else {
									showMessage('error', "adding your comment was unsuccessful, please try again later", 5000);
								}
							},
							"json"
					).error(function(){
						showMessage('error',"Internal error removing comment",5000);
					});
				},
				"cancel": function() {
					$(this).dialog("close");
				}
			}		
		});	
	}

}
/**
 * Handles querying for pages in a given DataTable object
 * 
 * @param sSource the "sAjaxSource" of the calling table
 * @param aoData the parameters of the DataTable object to send to the server
 * @param fnCallback the function that actually maps the returned page to the DataTable object
 * @author Todd Elvers
 */
function fnPaginationHandler(sSource, aoData, fnCallback) {
	var tableName = $(this).attr('id');
	
	// Extract the id of the currently selected space from the DOM
	var idOfSelectedSpace = $('#exploreList').find('.jstree-clicked').parent().attr("id");

	// If we can't find the id of the space selected from the DOM, get it from the cookie instead
	if(idOfSelectedSpace == null || idOfSelectedSpace == undefined){
		idOfSelectedSpace = $.cookie("jstree_select");
		// If we also can't find the cookie, then just set the space selected to be the root space
		if(idOfSelectedSpace == null || idOfSelectedSpace == undefined){
			$('#exploreList').jstree('select_node', '#1', true);
			idOfSelectedSpace = 1;
		} else {
			idOfSelectedSpace = idOfSelectedSpace[1];
		} 
	}


	// Request the next page of primitives from the server via AJAX
	$.post(  
			sSource + idOfSelectedSpace + "/" + tableName + "/pagination",
			aoData,
			function(nextDataTablePage){
				switch(nextDataTablePage){
				case 1:
					showMessage('error', "failed to get the next page of results; please try again", 5000);
					break;
				case 2:		
					// This error is a nuisance and the fieldsets are already hidden on spaces where the user lacks permissions
//					showMessage('error', "you do not have sufficient permissions to view primitives in this space", 5000);
					break;
				default:	// Have to use the default case since this process returns JSON objects to the client

					// Update the number displayed in this DataTable's fieldset
					updateFieldsetCount(tableName, nextDataTablePage.iTotalRecords);
				
				// Replace the current page with the newly received page
				fnCallback(nextDataTablePage);
				
				// If the primitive type is 'job', then color code the results appropriately
				if('j' == tableName[0]){
					colorizeJobStatistics();
				} 
				
				// Make the table that was just populated draggable too
				initDraggable('#' + tableName);

				break;
				}
			},  
			"json"
	).error(function(){
		//showMessage('error',"Internal error populating table",5000); Seems to show up on redirects
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
	case 'u':
		$('#userExpd').children('span:first-child').text(value);
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

/**
 * Displays comments related to this space
 * @param data - json response 
 * @author Vivek Sardeshmukh
 */
function displayComments(data) {
	//comments are made inivisible for root, now make them visible
	$('#commentDiv').show();

	$('#commentField legend').children('span:first-child').text(data.length);
	commentTable.fnClearTable();
	$.each(data, function(i, comment) {
		var ids= comment.id + '-' + comment.userId; //we need user id and comment id to delete that comment
		var hiddenIds= '<input type="hidden" value="'+ids+'">';
		var fullName = comment.firstName + ' ' + comment.lastName;
		var userLink = '<a href=starexecRoot+"secure/details/user.jsp?id=' + comment.userId + '" target="blank">' + fullName + 
		'<img class="extLink" src=starexecRoot+"images/external.png"/></a>' + hiddenIds;
		var cmt = comment.description;
		var brcmt = cmt.replace(/\n/g, "<br />"); //replace all newlines to <br>
		commentTable.fnAddData([userLink,  comment.uploadDate, brcmt]);
		$(commentTable.fnGetNodes(i)).data('type', 'comment');
	});
	initDraggable('#comments');
}


/**
 * Initializes the DataTable objects and adds multi-select to them
 */
function initDataTables(){
	
	// Extend the DataTables api and add our custom features
	extendDataTableFunctions();

	// Setup the DataTable objects
	userTable = $('#users').dataTable( {
		"sDom"			: 'rt<"bottom"flpi><"clear">',
		"iDisplayStart"	: 0,
		"iDisplayLength": 10,
		"bServerSide"	: true,
		"sAjaxSource"	: starexecRoot+"services/space/",
		"sServerMethod" : 'POST',
		"fnServerData"	: fnPaginationHandler
	});
	solverTable = $('#solvers').dataTable( {
		"sDom"			: 'rt<"bottom"flpi><"clear">',
		"iDisplayStart"	: 0,
		"iDisplayLength": 10,
		"bServerSide"	: true,
		"sAjaxSource"	: starexecRoot+"services/space/",
		"sServerMethod" : 'POST',
		"fnServerData"	: fnPaginationHandler
	});
	benchTable = $('#benchmarks').dataTable( {
		"sDom"			: 'rt<"bottom"flpi><"clear">',
		"iDisplayStart"	: 0,
		"iDisplayLength": 10,
		"bServerSide"	: true,
		"sAjaxSource"	: starexecRoot+"services/space/",
		"sServerMethod" : "POST",
		"fnServerData"	: fnPaginationHandler
	});
	jobTable = $('#jobs').dataTable( {
		"sDom"			: 'rt<"bottom"flpi><"clear">',
		"iDisplayStart"	: 0,
		"iDisplayLength": 10,
		"bServerSide"	: true,
		"sAjaxSource"	: starexecRoot+"services/space/",
		"bProcessing"	: false,
		"oLanguage": {
			"sProcessing": getProcessingMessage()
		},
		"sServerMethod" : "POST",
		"aaSorting"		: [],	// On page load, don't sort by any column - tells server to sort by 'created'
		"fnServerData"	: fnPaginationHandler 
	});
	resultTable = $('#results').dataTable( {
		"sDom"			: 'rt<"bottom"flpi><"clear">',
		"iDisplayStart"	: 0,
		"iDisplayLength": 10,
		"bServerSide"	: true,
		"sAjaxSource"	: starexecRoot+"services/space/",
		"sServerMethod" : "POST",
		"fnServerData"	: fnPaginationHandler
	});
	spaceTable = $('#spaces').dataTable( {
		"sDom"			: 'rt<"bottom"flpi><"clear">',
		"iDisplayStart"	: 0,
		"iDisplayLength": 10,
		"bServerSide"	: true,
		"sAjaxSource"	: starexecRoot+"services/space/",
		"sServerMethod" : "POST",
		"bProcessing"	: false,
		"oLanguage": {
			"sProcessing": getProcessingMessage()
		},
		"fnServerData"	: fnPaginationHandler
	});
	commentTable = $('#comments').dataTable( {
		"sDom": 'rt<"bottom"flpi><"clear">',
		"aaSorting": [[ 1, "asc" ]]
	}); 

	var tables=["#users","#solvers","#benchmarks","#jobs","#spaces","#comments"];

	function unselectAll(except) {
		var tables=["#users","#solvers","#benchmarks","#jobs","#spaces","#comments"];
		for (x=0;x<6;x++) {

			if (except==tables[x]) {
				continue;
			}
			$(tables[x]).find("tr").removeClass("row_selected");
		}
	}
	
	
	for (x=0;x<6;x++) {
		$(tables[x]).delegate("tr","mousedown", function(){
			unselectAll("#"+$(this).parent().parent().attr("id"));
			$(this).toggleClass("row_selected");
		});
	}
		// Setup user permission tooltip
	$('#users tbody').delegate('tr', 'hover', function(){
		$(this).toggleClass('hovered');
	});
	
	//Move to the footer of the Table
	$('#jobField div.selectWrap').detach().prependTo('#jobField div.bottom');
	$('#solverField div.selectWrap').detach().prependTo('#solverField div.bottom');
	$('#benchField div.selectWrap').detach().prependTo('#benchField div.bottom');
	$('#userField div.selectWrap').detach().prependTo('#userField div.bottom');

	
	//Hook up select all/ none buttons
	$('.selectAllJobs').click(function () {
		$(this).parents('.dataTables_wrapper').find('tbody>tr').addClass('row_selected');
	});
	$('.unselectAllJobs').click(function() {
		$(this).parents('.dataTables_wrapper').find('tbody>tr').removeClass('row_selected');
	});
	
	$('.selectAllSolvers').click(function () {
		$(this).parents('.dataTables_wrapper').find('tbody>tr').addClass('row_selected');
	});
	$('.unselectAllSolvers').click(function() {
		$(this).parents('.dataTables_wrapper').find('tbody>tr').removeClass('row_selected');
	});
	
	$('.selectAllBenchmarks').click(function () {
		$(this).parents('.dataTables_wrapper').find('tbody>tr').addClass('row_selected');
	});
	$('.unselectAllBenchmarks').click(function() {
		$(this).parents('.dataTables_wrapper').find('tbody>tr').removeClass('row_selected');
	});
	
	$('.selectAllUsers').click(function () {
		$(this).parents('.dataTables_wrapper').find('tbody>tr').addClass('row_selected');
	});
	$('.unselectAllUsers').click(function() {
		$(this).parents('.dataTables_wrapper').find('tbody>tr').removeClass('row_selected');
	});
	



	// Set all fieldsets as expandable (except for action fieldset)
	$('fieldset:not(:#actions)').expandable(true);

	// Set the DataTable filters to only query the server when the user finishes typing
	jobTable.fnFilterOnDoneTyping();
	solverTable.fnFilterOnDoneTyping();
	benchTable.fnFilterOnDoneTyping();
	userTable.fnFilterOnDoneTyping();
	spaceTable.fnFilterOnDoneTyping();

	log('all datatables initialized');
}

/**
 * Adds fnProcessingIndicator and fnFilterOnDoneTyping to dataTables api
 */
function extendDataTableFunctions(){
	// Allows manually turning on and off of the processing indicator (used for jobs table)
	jQuery.fn.dataTableExt.oApi.fnProcessingIndicator = function (oSettings, onoff)	{
		if( typeof(onoff) == 'undefined' ) {
			onoff = true;
		}
		this.oApi._fnProcessingDisplay(oSettings, onoff);
	};

	// Changes the filter so that it only queries when the user is done typing
	jQuery.fn.dataTableExt.oApi.fnFilterOnDoneTyping = function (oSettings) {
		var _that = this;
		this.each(function (i) {
			$.fn.dataTableExt.iApiIndex = i;
			var anControl = $('input', _that.fnSettings().aanFeatures.f);
			anControl.unbind('keyup').bind('keyup', $.debounce( 400, function (e) {
				$.fn.dataTableExt.iApiIndex = i;
				_that.fnFilter(anControl.val());
			}));
			return this;
		});
		return this;
	};
}

/**
 * Returns the 'processing...' image with the loading gif which is used
 * on the jobs table when deleting/paginating
 */
function getProcessingMessage(){
	//this is not popular with PI's
	//return '<center><img alt="loading" src=starexecRoot+"images/loader.gif" class="processing">processing...</img></center>';
	return;
}

/**
 * Removes the default 'query on keypress' functionality of a given DataTable
 * filter and queries on the enter key or if the client types 4 or more characters
 * @author Todd Elvers
 */
function changeFilter(primTable){
	$(primTable + '_filter input').unbind('keyup');
	$(primTable + '_filter input').bind('keyup', function(e) {

		if(e.keyCode == 13 || e.currentTarget.value.length > 3) {
			$(primTable).dataTable().fnFilter(this.value);    
		}
		if(e.currentTarget.value.length == 0) {
			$(primTable).dataTable().fnFilter("");    
		}
	});
}




/**
 * Populates the space details panel with the basic information about the space
 * (e.g. the name, description) but does not query for details about primitives 
 */
function getSpaceDetails(id) {
	$('#loader').show();
	$.post(  
			starexecRoot+"services/space/" + id,  
			function(data){ 
				log('AJAX response received for details of space ' + id);
				populateSpaceDetails(data, id);			
			},  
			"json"
	).error(function(){
		showMessage('error',"Internal error getting space details",5000);
	});
}

function handlePublicButton(id) {
	$('#loader').show();
	$.post(  
			starexecRoot+"services/space/isSpacePublic/" + id,  
			function(returnCode){
				switch(returnCode){
				case 0:
					$('#makePublic').fadeIn('fast');
					$('#makePrivate').fadeOut('fast');
					break;
				case 1:
					$('#makePublic').fadeOut('fast');
					$('#makePrivate').fadeIn('fast');
					break;
				}	
			},  
			"json"
	).error(function(){
		showMessage('error',"Internal error getting determining whether space is public",5000);
		$('#makePublic').fadeOut('fast');
		$('#makePrivate').fadeOut('fast');
	});
}

/**
 * Populates the space details of the currently selected space and queries
 * for the primitives of any fieldsets that are expanded
 * @param jsonData the basic information about the currently selected space
 */
function populateSpaceDetails(jsonData, id) {
	// If the space is null, the user can see the space but is not a member
	if(jsonData.space == null) {
		// Go ahead and show the space's name
		$('#spaceName').fadeOut('fast', function(){
			$('#spaceName').text($('.jstree-clicked').text()).fadeIn('fast');
		});

		// Show a message why they can't see the space's details
		$('#spaceDesc').fadeOut('fast', function(){
			$('#spaceDesc').text('you cannot view this space\'s details since you are not a member. you can see this space exists because you are a member of one of its descendants.').fadeIn('fast');
		});		
		$('#spaceID').fadeOut('fast');
		// Hide all the info table fieldsets
		$('#detailPanel fieldset').fadeOut('fast');		
		$('#loader').hide();

		// Stop executing the rest of this function
		return;
	} else {
		// Or else the user can see the space, make sure the info table fieldsets are visible
		$('#detailPanel fieldset').show();
	}

	// Update the selected space id
	spaceId = jsonData.space.id;
	spaceName = jsonData.space.name;

	// Populate space defaults
	$('#spaceName').fadeOut('fast', function(){
		$('#spaceName').text(jsonData.space.name).fadeIn('fast');
	});
	$('#spaceDesc').fadeOut('fast', function(){
		$('#spaceDesc').text(jsonData.space.description).fadeIn('fast');
	});	
	$('#spaceID').fadeOut('fast', function() {
		$('#spaceID').text("id = "+spaceId).fadeIn('fast');
	});
	$('#chartPicture').attr('src', starexecRoot+"secure/get/pictures?type=corg&Id=" + spaceId);

	/*
	 * Issue a redraw to all DataTable objects to force them to requery for
	 * the newly selected space's primitives.  This will effectively clear
	 * all entries in every table, update every table with the current space's
	 * primitives, and update the number displayed in every table's fieldset.
	 */
	benchTable.fnDraw();
	jobTable.fnDraw();
	userTable.fnDraw();
	solverTable.fnDraw();
	spaceTable.fnDraw();

	// Check the new permissions for the loaded space
	checkPermissions(jsonData.perm, id);

	// Done loading, hide the loader
	$('#loader').hide();

	log('Client side UI updated with details for ' + spaceName);
}

/**
 * Populates comments related to a space
 * @param id - space id
 * @author Vivek Sardeshmukh
 */
function getSpaceComments(id) {
	if (id == 1 || id == undefined){
		$('#commentDiv').hide();
		return;
	}
	//get comment information for the given space
	$.getJSON(starexecRoot+'services/comments/space/' + id, displayComments).error(function(){
		showMessage('error',"Internal error getting comments",5000);
	});	
}

/**
 * Expands the given parent space and selects the given child space in the jsTree
 */
function openSpace(parentId, childId) {
	$("#exploreList").jstree("open_node", "#" + parentId, function() {
		$.jstree._focused().select_node("#" + childId, true);	
	});	
}


/**
 * Creates either a leader tooltip, a personal tooltip, a space tooltip, or a expd tooltip for a given element
 * @author Todd Elvers
 */
function createTooltip(element, selector, type, message){	
	/**
	 * Tooltips for displaying to a leader what a particular user's permissions are in a given space; 
	 * these persist until a new space is selected in the space explorer
	 */
	if(type[0] == 'l'){
		$(element).delegate(selector, 'hover', function(){
			// Check and see if a qtip object already exists
			if(!$(this).data("qtip")){
				// If not, create one with the relevant configuration
				$(this).qtip(getTooltipConfig(type, message));
			}
		});
	}
	/**
	 * Tooltips for displaying to a user what their permission are for a given space in the space explorer; 
	 * these persist forever and are never removed from the page
	 */
	else if(type[0] == 's' || type[0] == 'p'){
		$(element).delegate(selector, 'hover', function(){
			if(!$(this).data("qtip")){
				$(this).qtip(getTooltipConfig(type, message));
			}
		});
	} 
	/**
	 * Tooltips for displaying to the user what their permissions are in a given fieldset, shown from
	 * the expd class; these are removed from the page when a new space in the space explorer is selected 
	 */
	else if(type[0] == 'e'){
		if(!$(element).data("qtip")){
			$(element).qtip(getTooltipConfig(type, message));
		}
	}

	log('tooltip created of type ' + type);
}


/**
 * Checks the permissions for the current space and hides/shows buttons based on
 * the user's permissions
 * @param perms The JSON permission object representing permissions for the current space
 */
function checkPermissions(perms, id) {
	// Check for no permission and hide entire action list if not present
	if(perms == null) {
		log('no permissions found, hiding action bar');
		$('#actionList').hide();		
		return;
	} else {
		$('#actionList').show();
	}

	if(perms.isLeader){
		// attach leader tooltips to every entry 
		createTooltip($('#users tbody'), 'tr', 'leader');

		$('#editSpace').fadeIn('fast');

		handlePublicButton(id);
	} else {
		// Otherwise only attach a personal tooltip to the current user's entry in the userTable
		createTooltip($('#users tbody'), 'tr', 'personal');
		$('#editSpace').fadeOut('fast');
		$('#makePublic').fadeOut('fast');
		$('#makePrivate').fadeOut('fast');
	}	

	if(perms.addSpace) {		
		$('#addSpace').fadeIn('fast');	
		$('#uploadXML').fadeIn('fast');	
	} else {
		$('#addSpace').fadeOut('fast');
		$('#uploadXML').fadeOut('fast');
	}

	if(perms.addBenchmark) {
		$('#uploadBench').fadeIn('fast');		
	} else {
		$('#uploadBench').fadeOut('fast');
	}

	if(perms.addSolver) {
		$('#uploadSolver').fadeIn('fast');		
	} else {
		$('#uploadSolver').fadeOut('fast');
	}

	if(perms.addJob) {
		$('#addJob').fadeIn('fast');		
	} else {
		$('#addJob').fadeOut('fast');
	}


	// Create tooltips for the expd class
	createTooltip($("#userExpd"), null, 'expd', getSinglePermTable('user', perms.addUser, perms.removeUser));
	log('permissions checked and processed');
}



/**
 * Updates the URLs to perform actions on the current space
 * @param id The id of the current space
 */
function updateButtonIds(id) {
	$('#editSpace').attr('href', starexecRoot+"secure/edit/space.jsp?id=" + id);
	$('#addSpace').attr('href', starexecRoot+"secure/add/space.jsp?sid=" + id);
	$('#uploadBench').attr('href', starexecRoot+"secure/add/benchmarks.jsp?sid=" + id);
	$('#uploadSolver').attr('href', starexecRoot+"secure/add/solver.jsp?sid=" + id);
	$('#addJob').attr('href', starexecRoot+"secure/add/job.jsp?sid=" + id);
	$('#downloadXML').attr('href', starexecRoot+"secure/download?token=test&type=spaceXML&id="+id);
	
	
	
	$("#downloadXML").unbind("click");
	$('#downloadXML').click(function() {
		createDialog("Processing your download request, please wait. This will take some time for large spaces.");
		token=Math.floor(Math.random()*100000000);
		$('#downloadXML').attr('href', starexecRoot+"secure/download?token=" +token+ "&type=spaceXML&id="+id);
		destroyOnReturn(token);
	});
	
	
	$('#uploadXML').attr('href', starexecRoot+"secure/add/batchSpace.jsp?sid=" + id);
	$('#generateResultChart').attr('href', starexecRoot+"secure/generateResultChart?sid=" + id);
	$("#downloadSpace").unbind("click");
	$("#downloadSpace").click(function(){		
		// Display the confirmation dialog
		$('#dialog-confirm-copy-txt').text('do you want to download the single space or the hierarchy?');
		$('#dialog-confirm-copy').dialog({
			modal: true,
			width: 380,
			height: 165,
			buttons: {
				'space': function(){
					$(this).dialog("close");
					createDialog("Processing your download request, please wait. This will take some time for large spaces.");
					token=Math.floor(Math.random()*100000000);
					window.location.href=starexecRoot+"secure/download?token="+token+"&type=space&hierarchy=false&id="+id;
					destroyOnReturn(token);
					
				},
				'hierarchy': function(){
					$(this).dialog("close");
					createDialog("Processing your download request, please wait. This will take some time for large spaces.");
					token=Math.floor(Math.random()*100000000);
					window.location.href=starexecRoot+"secure/download?token="+token+"&type=space&hierarchy=true&id="+id;
					destroyOnReturn(token);
				},
				"cancel": function() {
					log('user canceled copy action');
					$(this).dialog("close");
				}
			}
		});
	});
	log('updated action button space ids to ' + id);
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


/**
 * Updates a table by removing selected rows and updating the table's legend to match the new table size.
 * 
 * @param dataTable the dataTable to update
 * @author Todd Elvers
 */
function updateTable(dataTable){
	var rowsToRemove = $(dataTable).children('tbody').children('tr.row_selected');
	var rowsRemaining = $(dataTable).children('tbody').children(':not(tr.row_selected)');
	$.each(rowsToRemove, function(i, row) {
		dataTable.fnDeleteRow(row);
	});
	$(dataTable).parent().parent().parent().children('legend').children('span:first-child').text(rowsRemaining.length);

	log('table updated. rows removed: ' + rowsToRemove.length + ' rows remaining: ' + rowsRemaining.length);
}

/**
 * Takes in a set of permissions and builds a pretty table to display to the user
 * @param tooltip the tooltip we are creating a permissions table for
 * @param perms The set of permissions to display in a table
 * @returns A jQuery object that is the table to display in the UI for the given permissions
 */
function getPermTable(tooltip, perms, type) {	
	var permWrap = $('<div>');	// A wrapper for the table and leader info
	var table = $('<table class="tooltipTable">');	// The table where the permissions are displayed
	$(table).append('<tr><th>property</th><th>add</th><th>remove</th></tr>');

	// Resolves bug where tooltip is empty
	if(undefined === perms || null == perms){
		perms = {
				isLeader		: false,
				addJob			: false,
				removeJob		: false,
				addUser			: false,
				removeUser		: false,
				addSolver		: false,
				removeSolver	: false,
				addBenchmark	: false,
				removeBenchmark	: false,
				addSpace		: false,
				removeSpace		: false
		};
	}

	// Build a row for each permission
	$(table).append(wrapPermRow('job', perms.addJob, perms.removeJob));
	$(table).append(wrapPermRow('solver', perms.addSolver, perms.removeSolver));
	$(table).append(wrapPermRow('bench', perms.addBenchmark, perms.removeBench));
	$(table).append(wrapPermRow('user', perms.addUser, perms.removeUser));
	$(table).append(wrapPermRow('space', perms.addSpace, perms.removeSpace));

	// Save the data on the table for use throughout the page
	table.data('perms', perms);

	// Add the table to the wrapper
	$(permWrap).append(table);

	// HTML to add to the wrapper to indicate someone is a leader
	var leaderDiv = '<div class="leaderWrap"><span class="ui-icon ui-icon-star"></span><h2 class="leaderTitle">leader</h2></div>';

	if(perms.isLeader) {
		// If this person is a leader, add the leader div to the wrapper
		$(permWrap).append(leaderDiv);
	} 
	// If they're not a leader but the caller has specified to display the 'leader' button...
	else if (type == 'leader') {		
		// Create a button to make the user a leader
		var leadBtn = $('<span class="leaderBtn">make leader</span>').button({
			icons: {
				secondary: "ui-icon-star"
			}
		}).click(function(){
			// Update the title to save/cancel
			$(this).parents('.qtip').qtip('api').updateTitle('<center><a class="tooltipButton" onclick="saveChanges(this,true);">save</a>&nbsp;&nbsp;|&nbsp;&nbsp;<a class="tooltipButton" onclick="saveChanges(this,false);">cancel</a></center>');

			// Add the leader div to indicate they are now a leader
			$(this).parents('div:first').append(leaderDiv);			

			// Update their permissions and permission icons
			makeLeader(this);

			// Get rid of the button
			$(this).remove();
		});

		// Add the leader button to the wrapper
		$(permWrap).append(leadBtn);
	}
	// Shrink the space tooltip size if its for a non-leader
	else if (type == 'space'){
		tooltip.updateStyle('spaceTooltipNormal');
	}
	// Shrink the user tooltip size if its for a non-leader
	else if (!type){
		tooltip.updateStyle('userTooltipNormal');
	}

	// Return the resulting DOM element to be inserted
	return permWrap;
}

/**
 * Changes the data on the user's permission table to make the user a leader
 * and changes the UI to show all "true" values for all permissions
 * @param e The button element that was clicked to make the user a leader
 */
function makeLeader(e) {		
	var icons = $(e).siblings('table').find('span');
	$(icons).removeClass('ui-icon-closethick');
	$(icons).removeClass('ui-icon-check');
	$(icons).addClass('ui-icon-check');

	var permData = $(e).siblings('table').data('perms');
	permData.isLeader = true;
	permData.addSpace = true;
	permData.addJob = true;
	permData.addUser = true;
	permData.addSolver = true;
	permData.addBenchmark = true;
	permData.removeSpace = true;
	permData.removeJob = true;
	permData.removeUser = true;
	permData.removeSolver = true;
	permData.removeBench = true;	

	log('user marked as leader');	
}

/**
 * Gives back HTML for a table containing only one permission
 * @param name The name of the single permission to display
 * @param add The permission's add value
 * @param remove The permission's remove value
 * @returns HTML representing a table to be displayed in a qtip for the table
 */
function getSinglePermTable(name, add, remove) {
	var table = $('<table class="tooltipTable"></table>');
	$(table).append('<tr><th>property</th><th>add</th><th>remove</th></tr>');	
	$(table).append(wrapPermRow(name, add, remove));

	return $(table).toHTMLString();
}

/**
 * Wraps up a permission and it's value for display in a table. Includes onclicks for the images.
 * @param perm The permission type to display (job, user, solver, bench, space)
 * @param add The value for the add permission for the type
 * @param remove The value for the remove permission for the type
 * @returns HTML representing a row in a table display the type and it's permission values
 */
function wrapPermRow(perm, add, remove){
	var yes = $('<span>').css('margin', 'auto').addClass('ui-icon ui-icon-check').attr('onclick', 'togglePermImage(this,"' + perm + '");').toHTMLString();
	var no = $('<span>').css('margin', 'auto').addClass('ui-icon ui-icon-closethick').attr('onclick', 'togglePermImage(this,"' + perm + '");').toHTMLString();
	return "<tr><td>" + perm + "</td><td class='add'>" + (add ? yes : no) + "</td><td class='remove'>" + (remove ? yes : no) + "</td></tr>"; 
}

/**
 * Initializes custom tooltip styles; used to differentiate and customize
 * permanent and non-permanent tooltips (non-permanent tooltips elements 
 * are removed from the page whenever a new space in the space explorer is selected)
 * @author Todd Elvers
 */
function initTooltipStyles(){
	/**
	 * Custom tooltip style for tooltips that will never be deleted
	 * (i.e. tooltips on the spaces in #exploreList)
	 */
	$.fn.qtip.styles.spaceTooltipLeader = {
			background: '#E1E1E1',
			padding: 10,
			height: 144,
			width: 220,
			title : {
				color : '#ae0000'
			}
	};
	$.fn.qtip.styles.spaceTooltipNormal = {
			background: '#E1E1E1',
			padding: 10,
			height: 120,
			width: 220,
			title : {
				color : '#ae0000'
			}
	};

	/**
	 * Custom tooltip styles for tooltips that will be deleted everytime
	 * a new space is selected
	 */
	$.fn.qtip.styles.userTooltipLeader = {
			background: '#E1E1E1',
			height: 144,
			width: 220,
			padding: 10,
			title : {
				color : '#ae0000'
			}
	};
	$.fn.qtip.styles.userTooltipNormal = {
			background: '#E1E1E1',
			height: 120,
			width: 220,
			padding: 10,
			title : {
				color : '#ae0000'
			}
	};

	$.fn.qtip.styles.expdTooltip = {
			background: '#E1E1E1',
			width: 220,
			padding: 10,
			title : {
				color : '#ae0000'
			}
	};

	$.fn.qtip.styles.userTooltip = {
			background: '#E1E1E1',
			height: 144,
			width: 220,
			padding: 10,
			title : {
				color : '#ae0000'
			}
	};

	log('tooltip styles initialized');
}


/**
 * Returns the desired qTip configuration, with the given message, depending on the inputted type
 * 
 * @param type users/benchmarks/solvers/subspaces
 * @author Todd Elvers
 */
function getTooltipConfig(type, message){
	// Leader tooltips
	if(type[0] == 'l'){
		return {
			content: {
				text: getProcessingMessage(),
				title: {
					text: '<center><a>permissions</a></center>'
				}
			},
			position: {			// Place right middle portion of the tooltip to the left middle portion of the row element
				corner: {
					target: 'leftMiddle',
					tooltip: 'rightMiddle'
				}
			},
			hide :{
				fixed: true,
				effect: {
					type: 'fade',
					length: 100
				}
			},
			show: { 
				ready: true,	// Ensures the tooltip is shown the first time it's moused over
				solo: false,	// When this is false, all tooltip commands are applied only to the corresponding tooltip (what we want) instead of to all tooltips on the page (which causes weird artifacts to occur)
				delay: 1000,	// Every mouseover that occurs, after the first mouseover, will have to wait a second before the tooltip is triggered
				event: "mouseover",
				effect: {		// CSS custom-effect trick to workaround the necessary ready:true flag, which breaks the 'delay' during the first mouseover event
					type: function() {
						var tooltip = this;
						tooltip.css('visibility','visible');
						tooltip.css('opacity',1);

						if (tooltip.data('ready')) {
							tooltip.show('slide', 100);
							return;
						}

						tooltip.css('visibility','hidden').data('ready',1);						    						    
						var userId = $(this.qtip('api').elements.target).children('td:first').children('input').val();		

						// Uses a timer to simulate the first delay=1000 that would occur here if ready=false
						setTimeout(function(){
							// If element is not being hovered over anymore when the timer ends, don't display it
							if($("#users tbody tr.hovered td:first").children('input').val() == userId){
								tooltip.css('visibility','visible');
								tooltip.show('slide', 100);
							} else {
								// Fixes bug where elements that were initially hovered, but didn't stay long enough
								// to ensure a hover intent, wouldn't display a tooltip during the next hover event
								tooltip.hide();
							}
						}, 1000);

					}
				}
			},
			style: {
				name: "userTooltip",		// Load custom color scheme
				tip: 'rightMiddle'			// Add a tip to the right middle portion of the tooltip
			},			   
			api:{
				onRender: function(){	// Before rendering the tooltip, get the user's permissions for the given space
					var tooltip = this;
					var userId = $(this.elements.target).children('td:first').children('input').val();
					$.post(
							starexecRoot+'services/space/' + spaceId + '/perm/' + userId,
							function(theResponse){
								log('AJAX response for permission tooltip received');
								if(1 == theResponse){
									showMessage('error', "only leaders of a space can edit the permissions of others", 5000);
								} else {
									// Replace current content (current = loader.gif)		
									tooltip.updateContent(" ", true);
									tooltip.updateContent(getPermTable(tooltip, theResponse, 'leader'), true);										
								}
								return true;
							}
					).error(function(){
						//showMessage('error',"Internal error getting user permissions",5000); bother the user for a tooltip problem?
					});	

				},
				onHide: function(){			// If a user modifies a tooltip but does not press the 'save' or 'cancel' button then this resets the tooltip once it loses focus and fades from view
					var tooltip = this;
					log("permissions = " + $(tooltip.elements.title).text());
					if('p' != $(tooltip.elements.title).text()[0]){
						tooltip.updateTitle('<center><a>permissions</a></center>');
						var userId = $(this.elements.target).children('td:first').children('input').val();
						$.post(
								starexecRoot+'services/space/' + spaceId + '/perm/' + userId,
								function(theResponse){
									log('AJAX response for permission tooltip received');
									tooltip.updateContent(" ", true); // Have to clear it first to prevent it from appending (qtip bug?)
									tooltip.updateContent(getPermTable(tooltip, theResponse, 'leader'), true);  
									tooltip.updateTitle('<center><a>permissions</a></center>');	
									tooltip.hide();
									return true;
								}	
						).error(function(){
							//showMessage('error',"Internal error getting space details",5000); 
						});		
					}

					// Fixes bug where 'hovered' class doesn't get removed from the no-longer-hovered tr element
					var userId = $(tooltip.elements.target).children('td:first').children('input').val();
					$('#uid'+userId).parent().parent().removeClass('hovered');
				}

			}
		};
	}
	// Space tooltips
	else if (type[0] == 's'){
		return {
			content: {
				prerender: true,
				text: getProcessingMessage(),
				title: {
					text: '<center>permissions</center>'
				}
			},
			position: {
				corner: {
					target: 'topRight',
					tooltip: 'bottomLeft'
				},
				adjust: {
					screen: true
				}
			},
			show: { 
				ready: true,
				solo: false,		
				delay: 1000,
				event: "mouseover",
				effect: {
					type: function() {
						var tooltip = this;
						tooltip.css('visibility','visible').css('opacity',1);
						var spaceBeingHovered = $('#exploreList').find('.jstree-hovered').parent().attr("id");
						if (tooltip.data('ready')) {
							tooltip.show('slide', 150);
							return;
						} else {
							tooltip.css('visibility','hidden').data('ready',1);
							setTimeout(function(){
								if($('#exploreList').find('.jstree-hovered').parent().attr("id") == spaceBeingHovered){
									tooltip.css('visibility','visible');
									tooltip.show('slide', 150);
								} else {
									tooltip.hide();
								}
							}, 1000);
						}
					}
				}
			},
			hide:{
				effect: {
					type: 'slide',
					length: 200
				}
			},
			style: {
				name: "spaceTooltipLeader",
				tip: 'bottomLeft'
			},
			api:{
				onRender: function(){
					var tooltip = this;
					var hoveredSpaceId = $('#exploreList').find('.jstree-hovered').parent().attr("id");

					// Destroy the tooltip if the space being hovered is the root space
					if(hoveredSpaceId == 1 || hoveredSpaceId === undefined){
						tooltip.destroy();
						return;
					}

					// Get the user's permissions in the given space
					$.post(
							starexecRoot+'services/space/' + hoveredSpaceId,
							function(theResponse){
								log('AJAX response for permission tooltip received');
								tooltip.updateContent(" ");
								tooltip.updateContent(getPermTable(tooltip, theResponse.perm), true);
								return true;
							}
					).error(function(){
						//showMessage('error',"Internal error getting space details",5000);
					});	
				}
			}
		};
	}
	// Expd tooltips
	else if (type[0] == 'e'){
		return {
			content: {
				text: message,
				title: {
					text: '<center>permissions</center>'
				}
			},
			position: {
				corner:{
					target: 'topLeft',
					tooltip: 'bottomLeft'
				},
				adjust:{
					screen: true
				}
			},
			show: { 
				solo: true,
				delay: 1000,
				event: "mouseover",
				effect : function() {

					$(this).show('slide', 150);
				}
			},
			hide:{
				effect: {
					type: 'slide',
					length: 150
				}
			},
			style: {
				name: 'expdTooltip'
			}
		};
	}
	// Personal tooltips
	else if (type[0] == 'p'){
		return {
			content: {
				text: getProcessingMessage(),
				title: {
					text: '<center><a>permissions</a></center>'
				}
			},
			position: {
				corner: {
					target: 'leftMiddle',
					tooltip: 'rightMiddle'
				}
			},
			show: { 
				ready: true,
				solo: false,
				delay: 1000,
				event: "mouseover",
				effect: {
					type: function() {
						var tooltip = this;
						tooltip.css('visibility','visible').css('opacity',1);
						if (tooltip.data('ready')) {
							tooltip.show('slide', 150);
							return;
						}
						tooltip.css('visibility','hidden').data('ready',1);
						var userId = $('#users tbody tr').find('td:first input[name="currentUser"]').val();
						setTimeout(function(){
							// Only show the 'personal' tooltip when the user hovers over themselves in the userTable
							if(userId != undefined && $("#users tbody tr.hovered td:first input").val() == userId){
								tooltip.css('visibility','visible');
								tooltip.show('slide', 150);
							} else {
								tooltip.qtip("destroy");
							}
						}, 1000);
					}
				}
			},
			hide:{
				effect: {
					type: 'fade',
					length: 100
				}
			},
			style: {
				name: "userTooltip",
				tip: 'rightMiddle'
			},
			api:{
				onRender: function(){
					var tooltip = this;
					var userId =  $("#users tbody tr").find('td:first input[name="currentUser"]').val();
					if(userId != undefined && $(this.elements.target).children('td:first').children('input').val() == userId){
						var url = starexecRoot+'services/space/' + spaceId + '/perm/' + userId;
						$.post(
								url,
								function(theResponse){
									log('AJAX response for permission tooltip received');
									// Replace current content (current = loader.gif)
									tooltip.updateContent(" ", true);
									tooltip.updateContent(getPermTable(tooltip, theResponse), true);
									return true;
								}
						).error(function(){
							//showMessage('error',"Internal error getting space details",5000);
						});	
					}
				}
			}
		};
	}	
}

/**
 * When a leader tooltip permissions icon is clicked, this cycles to the next png in order: 
 * none->add->remove->add/remove->none
 * 
 * @param image the icon span DOM element containing the png that triggered this method when it was clicked
 * @param perm the string name of the permission being toggled (user, solver, bench, job, space)
 */
function togglePermImage(image, perm){
	// If the user is a leader, you can't edit their permissions, return
	if($(image).parents('table').data('perms').isLeader) {
		showMessage('warn', 'you cannot edit a leader\'s permission', 1500);
		return;
	}	

	// Change the title to 'save | cancel'
	$(image).parents('.qtip').qtip('api').updateTitle('<center><a class="tooltipButton" onclick="saveChanges(this,true);">save</a>&nbsp;&nbsp;|&nbsp;&nbsp;<a class="tooltipButton" onclick="saveChanges(this,false);">cancel</a></center>');

	// Get the current permissions associated with the table
	var permData = $(image).parents('table').data('perms');	

	// Determine if this is an add or remove permission being toggled
	var isAddPerm = $(image).parent().attr('class').indexOf('add') >= 0;
	var newVal = false;

	// Toggle the permission icon and update the new value to be set
	if($(image).attr('class').indexOf('ui-icon-check') >= 0) {
		$(image).removeClass('ui-icon-check');
		$(image).addClass('ui-icon-closethick');
		newVal = false;
	} else {
		$(image).removeClass('ui-icon-closethick');
		$(image).addClass('ui-icon-check');
		newVal = true;
	}

	// Set the new value based on the permission name and whether it's add/remove
	if(isAddPerm) {
		if(perm == "space") { permData.addSpace = newVal; }
		else if(perm == "job") { permData.addJob = newVal; }
		else if(perm == "user") { permData.addUser = newVal; }
		else if(perm == "solver") { permData.addSolver = newVal; }
		else if(perm == "bench") { permData.addBenchmark = newVal; }
		log('Updated add permission to ' + newVal + ' for ' + perm);
	} else {		
		if(perm == "space") { permData.removeSpace = newVal; }
		else if(perm == "job") { permData.removeJob = newVal; }
		else if(perm == "user") { permData.removeUser = newVal; }
		else if(perm == "solver") { permData.removeSolver = newVal; }
		else if(perm == "bench") { permData.removeBench = newVal; }
		log('Updated remove permission to ' + newVal + ' for ' + perm);
	}
}

/**
 * Handles actions for the 'save' and 'cancel' buttons that appear on leader tooltips whenever
 * a permission is changed
 * 
 * @param obj the title div of the qtip from which this method was called
 * @param save true = save button, false = cancel button
 * @param userId the id of the user to save the new permissions for
 * @author Todd Elvers
 */
function saveChanges(obj, save){
	// Get the currently hovered user
	var userId = $($(obj).parents('.qtip').qtip('api').elements.target).children('td:first').children('input').val();
	log('saving permissions for user ' + userId);

	// 'SAVE' option
	if(true == save){  
		// Collect the permission images from the tooltip
		var tooltip = $(obj).parents('.qtip').qtip('api');

		// Get the stored permissions from the DOM for this table
		var perms = $(obj).parents('.qtip').find('table').data('perms');

		// Update database to reflect new permissions
		$.post(
				starexecRoot+'services/space/' + spaceId + '/edit/perm/' + userId,
				{ addUser		: perms.addUser,
					removeUser	: perms.removeUser,
					addSolver		: perms.addSolver,
					removeSolver	: perms.removeSolver,
					addBench		: perms.addBenchmark,
					removeBench	: perms.removeBench,
					addJob		: perms.addJob,
					removeJob		: perms.removeJob,
					addSpace		: perms.addSpace,
					removeSpace	: perms.removeSpace,
					isLeader		: perms.isLeader},
					function(theResponse){
						log('AJAX response received for permission edit request with code ' + theResponse);
						switch(theResponse){					
						case 0:
							// Change the title to 'permissions'
							tooltip.updateTitle('<center><a>permissions</a></center>');  
							break;
						case 1:
							showMessage('error', "an error occured while editing permissions; please try again", 5000);
							break;
						case 2:
							showMessage('error', "only leaders of a space can edit the permissions of others", 5000);
							break;
						case 3:
							showMessage('error', "you cannot modify the permissions for yourself or other leaders of " + spaceName, 5000);
							break;
						}
						return true;
					}
		).error(function(){
			//showMessage('error',"Internal error getting space details",5000);
		});	
	} else {  
		log('user canceled edit permission action');
		$(obj).parents('.qtip').qtip('api').hide();
	}
}

/**
 * Toggles the plus-minus text of the "+ add new" comment button
 * @author Todd Elvers
 */
function togglePlusMinus(addCommentButton){
	if($(addCommentButton).children('span:first-child').text() == "+"){
		$(addCommentButton).children('span:first-child').text("-");
	} else {
		$(addCommentButton).children('span:first-child').text("+");
	}
}
