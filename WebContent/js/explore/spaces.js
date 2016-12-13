/** Global Variables */
var userTable;
var benchTable;
var solverTable;
var spaceTable;
var resultTable;
var jobTable;
var spaceId;			// id of the current space
var spaceName;			// name of the current space
var currentUserId;
var spaceChain;   // array of space ids to trigger in order
var spaceChainIndex=0; //the current index of the space chain
var openDone=true;
var spaceChainInterval;
var usingSpaceChain=false;
var isLeafSpace=false;
var userIsDeveloper=false;
$(document).ready(function(){	
	currentUserId=parseInt($("#userId").attr("value"));
	usingSpaceChain=(getSpaceChain("#spaceChain").length>1); //check whether to turn off cookies

	determineIfUserIsDeveloper();

	// Build left-hand side of page (space explorer)
	initSpaceExplorer();

	// Build right-hand side of page (space details)
	initSpaceDetails();
});

// Set the userIsDeveloper variable using a GET
function determineIfUserIsDeveloper() {
	'use strict';
	$.get(
		starexecRoot+'services/users/isDeveloper',
		'',
		function(data) {
			userIsDeveloper = data;
		},
		'json'
	);
}


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
 * Sets up the 'space details' that consumes the right-hand side of the page
 */
function initSpaceDetails(){

	// builds the DataTable objects and enables multi-select on them
	initDataTables();

	// Set up jQuery button UI
	initButtonUI();

	

	// This hides the action list if the space is root space or we aren't looking at a space
	if (spaceId == 1 || spaceId == undefined){
		$('.actionList').hide();
	}

	pbc = false;
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

	$('.resetButton').button({
		icons: {
			secondary: "ui-icon-closethick"
		}});
	attachSortButtonFunctions();

	
	
	log('jQuery UI buttons initialized');
}

/**
 * Initializes a table so that elements can be dragged out of it and onto a space name
 * @param table The table to make draggable
 * @author Tyler Jensen & Todd Elvers
 */
function initDraggable(table) {
	makeTableDraggable(table,onDragStart,getDragClone);
	// Make the trash can in the explorer list be a droppable target
	$('#trashcan').droppable({
		drop		: onTrashDrop,
		tolerance	: 'touch',	// Use the pointer to determine drop position instead of the middle of the drag clone element
		hoverClass	: 'hover',		// Class applied to the space element when something is being dragged over it
		activeClass	: 'active'		// Class applied to the space element when something is being dragged
	});
	$("#trashcan").click(function(){
		window.location.href=starexecRoot+"secure/details/recycleBin.jsp";
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
}

/**
 * Called when a draggable item (primitive) is dropped on the trash can
 * @author Todd Elvers
 */
function onTrashDrop(event, ui){
	// Collect the selected elements from the table being dragged from
	var ids = getSelectedRows($(ui.draggable).parents('table:first'));
	ownsAll=userCanDeleteAll($(ui.draggable).parents('table:first'));
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
			removeSolvers(ids,ownsAll);
		} else {
			removeSubspaces(ids);//actual Remove called within here
		}
		break;
	case 'b':
		removeBenchmarks(ids,ownsAll);
		break;
	case 'j':
		removeJobs(ids,ownsAll);
		break;

	}
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
	var destIsLeafSpace = spaceIsLeaf(destSpace);
	var allSpacesBeingCopiedAreLeaves = null;

	log(ids.length + ' rows dropped onto ' + destName);
	
	if(ids.length < 2) {
		log ("ids.length was < 2");
		// If 0 or 1 things are selected in the table, just use the element that is being dragged
		ids = [ui.draggable.data('id')];

		// Customize the confirmation message for the copy operation to the primitives/spaces involved
		log("ui.draggable.data('type')[0]="+ui.draggable.data('type')[0]+" , ui.draggable.data('type')[1]="+ui.draggable.data('type')[1]);
		if(ui.draggable.data('type')[0] == 's' && ui.draggable.data('type')[1] == 'p'){
			allSpacesBeingCopiedAreLeaves = ids.every(function(idOfSpaceBeingCopied) {
				return spaceIsLeaf(idOfSpaceBeingCopied);
			});
            $('#copy-primitives-options').removeClass('copy-options-hidden');
			if (allSpacesBeingCopiedAreLeaves) {
				$('#dialog-confirm-copy-txt').text(
						'about to copy ' + ui.draggable.data('name') + ' to' + destName +'.');
			} else {
				$('#dialog-confirm-copy-txt').text(
						'would you like to copy ' + ui.draggable.data('name') + ' only or the hierarchy to' + destName +'?');
                $('#hier-copy-options').removeClass('copy-options-hidden');
            }
		}
		else if(ui.draggable.data('type')[0] == 's'){
			if (destIsLeafSpace) {
				$('#dialog-confirm-copy-txt').text('do you want to copy or link ' + ui.draggable.data('name') + ' to' + destName+'?');

			} else {
				$('#dialog-confirm-copy-txt').text(
						'do you want to copy ' + ui.draggable.data('name') + ' to' + destName + 
						' and all of its subspaces or just to' + destName +'?');
			}
		} else if (ui.draggable.data('type')[0] == 'u') {
			if (destIsLeafSpace) {
				$('#dialog-confirm-copy-txt').text('do you want to copy ' + ui.draggable.data('name') + ' to' + destName+'?');

			} else {
				$('#dialog-confirm-copy-txt').text(
						'do you want to copy ' + ui.draggable.data('name') + ' to' + destName + 
						' and all of its subspaces or just to' + destName +'?');
			}
		} else if (ui.draggable.data('type')[0]=='j') {

			$('#dialog-confirm-copy-txt').text('do you want to link ' + ui.draggable.data('name') + ' in' + destName + '?');
		}
		else {
			$('#dialog-confirm-copy-txt').text('do you want to copy or link ' + ui.draggable.data('name') + ' to' + destName + '?');
		}
	} else {
		if(ui.draggable.data('type')[0] == 's' && ui.draggable.data('type')[1] == 'p'){
            $('#copy-primitives-options').removeClass('copy-options-hidden');
			allSpacesBeingCopiedAreLeaves = ids.every(function(idOfSpaceBeingCopied) {
				return spaceIsLeaf(idOfSpaceBeingCopied);
			});
			if (allSpacesBeingCopiedAreLeaves) {
				$('#dialog-confirm-copy-txt').text('do you want to copy the '+ ids.length + ' selected spaces to' + destName + '?');
			} else {
                $('#hier-copy-options').removeClass('copy-options-hidden');
				$('#dialog-confirm-copy-txt').text(
						'do you want to copy the ' + ids.length + ' selected spaces only or the hierarchy to' + destName +'?');
			}
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
		var solverCopyDialogButtons = {
			'link in space': function(){
				$('#dialog-confirm-copy').dialog('close');
				doSolverCopyPost(ids,destSpace,spaceId,false,false);
			},
			'copy to space': function() {
				$('#dialog-confirm-copy').dialog('close');	
				doSolverCopyPost(ids,destSpace,spaceId,false,true);
			},
			"cancel": function() {
				$(this).dialog("close");
			}
			
		};		
		if (!spaceIsLeaf(destSpace)) {
			solverCopyDialogButtons['link in space hierarchy'] = function() {
				$('#dialog-confirm-copy').dialog('close'); 
				doSolverCopyPost(ids,destSpace,spaceId,true,false);
			};
			solverCopyDialogButtons['copy to space hierarchy'] = function() {
				$('#dialog-confirm-copy').dialog('close'); 
				doSolverCopyPost(ids,destSpace,spaceId,true,true);
			};
		} 			
		// Display the confirmation dialog
		$('#dialog-confirm-copy').dialog({
			modal: true,
			width: 600,
			height: 400,
			
			//depending on what the user 
			buttons: solverCopyDialogButtons
		});	
	}
	// If primitive being copied to another space is a user...
	else if(ui.draggable.data('type')[0] == 'u'){
		setupUserCopyDialog(ids, destSpace, destName, ui, destIsLeafSpace);
	}

	// If copying subspaces to other spaces
	else if(ui.draggable.data('type')[0] == 's' && ui.draggable.data('type')[1] == 'p'){
		setupSpaceCopyDialog(ids, destSpace, destName);
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
			height: 200,
			width: 500,
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

function setupSpaceCopyDialog(ids, destSpace, destName) {
	'use strict';

	// True if every space in ids is a leaf.
	var allSpacesBeingCopiedAreLeaves = ids.every(function(idOfSpaceBeingCopied) {
		return spaceIsLeaf(idOfSpaceBeingCopied);
	});

	var singleSpaceCopy = function() {
		// If the user actually confirms, close the dialog right away
		$('#dialog-confirm-copy').dialog('close');

		// Making the request		
		doSpaceCopyPost(ids,destSpace,false,destName);
	};

	var spaceCopyDialogButtons = {};

	log('Deciding whether to copy hierarchy or space');
	if (allSpacesBeingCopiedAreLeaves) {
		log('Copying single space');
		spaceCopyDialogButtons['confirm'] = singleSpaceCopy;
	} else {
        spaceCopyDialogButtons['confirm'] = function() {
            var copyHierOption = $("input[type='radio'][name='copySpace']:checked").val(); 
			log('copyHierOption: ' + copyHierOption);
            if(copyHierOption) {
                $('#dialog-confirm-copy').dialog('close');
				log('Copying hierarchy');
                doSpaceCopyPost(ids,destSpace,true,destName);
            } else {
				log('Copying single space');
                singleSpaceCopy();
            }
            $('#hier-copy-options').addClass('copy-options-hidden');
        } 
	}

	spaceCopyDialogButtons['cancel'] = function() {
		log('user canceled copy action');
        $('#hier-copy-options').addClass('copy-options-hidden');
        $('#copy-primitives-options').addClass('copy-options-hidden');
		$(this).dialog('close');
	};

	// Display the confirmation dialog
	$('#dialog-confirm-copy').dialog({
		modal: true,
		width: 700,
		height: 300,
		buttons: spaceCopyDialogButtons
	});
}

function setupUserCopyDialog(ids, destSpace, destName, ui, destIsLeafSpace) {
	var userCopyDialogButtons = {};
	if (!destIsLeafSpace) {
		userCopyDialogButtons['space hierarchy'] = function() {
			// If the user actually confirms, close the dialog right away
			$('#dialog-confirm-copy').dialog('close');
			// Make the request to the server	
			doUserCopyPost(ids,destSpace,true,destName,ui);
		};
		userCopyDialogButtons['space'] = function(){
			// If the user actually confirms, close the dialog right away
			$('#dialog-confirm-copy').dialog('close');
			doUserCopyPost(ids,destSpace,false,destName,ui);
				
		};
	} else {
		userCopyDialogButtons['confirm'] = function(){
			// If the user actually confirms, close the dialog right away
			$('#dialog-confirm-copy').dialog('close');
			doUserCopyPost(ids,destSpace,false,destName,ui);
		};
	}
	userCopyDialogButtons["cancel"] = function() {
		log('user canceled copy action');
		$(this).dialog("close");
	}
	$('#dialog-confirm-copy').dialog({
		modal: true,
		width: 500,
		height: 200,
		buttons: userCopyDialogButtons
	});		
}

function doSpaceCopyPost(ids,destSpace,copyHierarchy,destName) {
    var copyPrimitives = $("input[type='radio'][name='copyPrimitives']:checked").val();
	log('copyPrimitives: ' + copyPrimitives);
    $('#copy-primitives-options').addClass('copy-options-hidden');
	$.post(  	    		
			starexecRoot+'services/spaces/' + destSpace + '/copySpace',
			{selectedIds : ids, copyHierarchy: copyHierarchy, copyPrimitives: copyPrimitives},
			function(returnCode) {
				s=parseReturnCode(returnCode);
				if (s) {							
					$('#exploreList').jstree("refresh");
				} 
			},
			"json"
	).error(function(){
		showMessage('error',"Internal error copying spaces",5000);
	});
}

function doUserCopyPost(ids,destSpace,copyToSubspaces,destName,ui){
	$.post(  	    		
			starexecRoot+'services/spaces/' + destSpace + '/add/user',
			{selectedIds : ids, copyToSubspaces: copyToSubspaces},	
			function(returnCode) {
				parseReturnCode(returnCode);
			},
			"json"
	).error(function(){
		showMessage('error',"Internal error copying users",5000);
	});				
}


//adds the space id to the url as a parameter
function setURL(i) {
	current=window.location.pathname;
	newURL=current.substring(0,current.indexOf("?"));
	window.history.replaceState("object or string", "",newURL+"?id="+i);
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
	
	$.post(  	    		
			starexecRoot+'services/spaces/' + destSpace + '/add/benchmark', // We use the type to denote copying a benchmark/job
			{selectedIds : ids, fromSpace : spaceId, copy:copy},	
			function(returnCode) {
				parseReturnCode(returnCode);
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
 * @author Eric Burns
 */

function doSolverCopyPost(ids,destSpace,spaceId,hierarchy,copy) {
	// Make the request to the server
	
	$.post(  	    		
			starexecRoot+'services/spaces/' + destSpace + '/add/solver',
			{selectedIds : ids, fromSpace : spaceId, copyToSubspaces: hierarchy, copy : copy},
			function(returnCode) {
				parseReturnCode(returnCode);
			},
			"json"
	).error(function(){
		showMessage('error',"Internal error copying solvers",5000);
	});	
}

function spaceIsLeaf(spaceId) {
	return $('#'+spaceId).hasClass('jstree-leaf');
}



/**
 * Creates the space explorer tree for the left-hand side of the page, also
 * creates tooltips for the space explorer, .expd class, and userTable (if applicable)
 * @author Tyler Jensen & Todd Elvers & Skylar Stark
 */
function initSpaceExplorer(){
	// Initialize the jstree plugin for the explorer list
	jsTree=makeSpaceTree("#exploreList",!usingSpaceChain);
	jsTree.bind("select_node.jstree", function (event, data) {
		// When a node is clicked, get its ID and display the info in the details pane
		id = data.rslt.obj.attr("id");
		isLeafSpace = spaceIsLeaf(id);
		log('Selected space isLeafSpace='+isLeafSpace);
		log('Space explorer node ' + id + ' was clicked');

		updateButtonIds(id);
		getSpaceDetails(id);

		// Remove all non-permanent tooltips from the page; helps keep
		// the page from getting filled with hundreds of qtip divs
		$(".qtip-userTooltip").remove();
		$(".qtip-expdTooltip").remove();
	}).bind("loaded.jstree", function(event,data) {
		handleSpaceChain("#spaceChain");
	}).bind("open_node.jstree",function(event,data) {
		openDone=true;
	});

	log('Space explorer node list initialized');
}

/**
 * Handles removal of benchmark(s) from a space
 * @author Todd Elvers
 */
function removeBenchmarks(selectedBenches,ownsAll){
	if (ownsAll) {
		$('#dialog-confirm-delete-txt').text('Do you want to remove the selected benchmark(s) from ' + spaceName + ', or would you like  to send them to the recycle bin?');
		
		// Display the confirmation dialog
		$('#dialog-confirm-delete').dialog({
			modal: true,
			height: 220,
			buttons: {
				'remove from space': function() {
					log('user confirmed benchmark removal');
					// If the user actually confirms, close the dialog right away
					$('#dialog-confirm-delete').dialog('close');

					$.post(  
							starexecRoot+"services/remove/benchmark/" + spaceId,
							{selectedIds : selectedBenches},
							function(returnCode) {
								s=parseReturnCode(returnCode);
								if (s) {
									updateTable(benchTable);
								}
							},
							"json"
					).error(function(){
						showMessage('error',"Internal error removing benchmarks",5000);
					});		
				},
				'move to recycle bin': function() {
					// If the user actually confirms, close the dialog right away
					$('#dialog-confirm-delete').dialog('close');

					$.post(  
							starexecRoot+"services/recycleandremove/benchmark/"+spaceId,
							{selectedIds : selectedBenches},
							function(returnCode) {
								s=parseReturnCode(returnCode);
								if (s) {
									updateTable(benchTable);
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
	} else {
		$('#dialog-confirm-delete-txt').text('Do you want to remove the selected benchmark(s) from ' + spaceName + '?');
		
		// Display the confirmation dialog
		$('#dialog-confirm-delete').dialog({
			modal: true,
			height: 220,
			buttons: {
				'remove from space': function() {
					log('user confirmed benchmark removal');
					// If the user actually confirms, close the dialog right away
					$('#dialog-confirm-delete').dialog('close');

					$.post(  
							starexecRoot+"services/remove/benchmark/" + spaceId,
							{selectedIds : selectedBenches},
							function(returnCode) {
								s=parseReturnCode(returnCode);
								if (s) {
									updateTable(benchTable);
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
				
}	

function cancelRemoveUsers() {
	log('user canceled user deletion');
	$('#dialog-confirm-delete').dialog('close');
}

function removeUsersFromSpace(selectedUsers) {
	log('user confirmed user deletion');
	// If the user actually confirms, close the dialog right away
	$('#dialog-confirm-delete').dialog('close');

	$.post(  
			starexecRoot+"services/remove/user/" + spaceId,
			{selectedIds : selectedUsers, hierarchy : false},
			function(returnCode) {
				s=parseReturnCode(returnCode);
				if (s) {
					updateTable(userTable);
				}
			},
			"json"
	).error(function(){
		showMessage('error',"Internal error removing users",5000);
	});	
}

/**
 * Handles removal of user(s) from a space
 * @author Todd Elvers & Skylar Stark
 */
function removeUsers(selectedUsers){
	var dialogButtons = null;
	if (isLeafSpace) {
		$('#dialog-confirm-delete-txt').text('Are you sure you want to remove the user(s)?');
		dialogButtons = {
			'confirm': function() { 
				removeUsersFromSpace(selectedUsers) 
			}, 
			'cancel': function() { 
				cancelRemoveUsers() 
			}
		};	
	} else {
		$('#dialog-confirm-delete-txt').text(
			'do you want to remove the user(s) from ' + spaceName + ' and its hierarchy or just from ' +spaceName + '?');
		dialogButtons = {
			'space hierarchy': function() {
				log('user confirmed user deletion from space and its hierarchy');
				// If the user actually confirms, close the dialog right away
				$('#dialog-confirm-delete').dialog('close');

				$.post(  
						starexecRoot+"services/remove/user/" + spaceId,
						{selectedIds : selectedUsers, hierarchy : true},
						function(returnCode) {
							s=parseReturnCode(returnCode);
							if (s) {
								updateTable(userTable);
							}
						},
						"json"
				).error(function(){
					showMessage('error',"Internal error removing users",5000);
				});	
			},
			'space': function() { 
				removeUsersFromSpace(selectedUsers) 
			},
			'cancel': function() { 
				cancelRemoveUsers() 
			} 
		};		
	}

	// Display the confirmation dialog
	$('#dialog-confirm-delete').dialog({
		modal: true,
		width: 380,
		height: 200,
		buttons: dialogButtons
	});
}

function removeSolversFromSpaceHierarchy(selectedSolvers) {
	log('user confirmed solver removal from space and its hierarchy');
	// If the user actually confirms, close the dialog right away
	$('#dialog-confirm-delete').dialog('close');

	$.post(  
			starexecRoot+"services/remove/solver/" + spaceId,
			{selectedIds : selectedSolvers, hierarchy : true},
			function(returnCode) {
				s=parseReturnCode(returnCode);
				if (s) {
					updateTable(solverTable);
				}
			},
			"json"
	).error(function(){
		showMessage('error',"Internal error removing solvers",5000);
	});
}

function removeSolversFromSpace(selectedSolvers) {
	log('user confirmed solver removal');
	// If the user actually confirms, close the dialog right away
	$('#dialog-confirm-delete').dialog('close');

	$.post(  
			starexecRoot+"services/remove/solver/" + spaceId,
			{selectedIds : selectedSolvers, hierarchy : false},
			function(returnCode) {
				s=parseReturnCode(returnCode);
				if (s) {
					updateTable(solverTable);
				}
			},
			"json"
	).error(function(){
		showMessage('error',"Internal error removing solvers",5000);
	});
}

function moveSolversToRecycleBin(selectedSolvers) {
	// If the user actually confirms, close the dialog right away
	$('#dialog-confirm-delete').dialog('close');

	$.post(  
			starexecRoot+"services/recycleandremove/solver/"+spaceId,
			{selectedIds : selectedSolvers, hierarchy : true},
			function(returnCode) {
				s=parseReturnCode(returnCode);
				if (s) {
					updateTable(solverTable);
				}
			},
			"json"
	).error(function(){
		showMessage('error',"Internal error removing solvers",5000);
	});
}

/**
 * Handles removal of solver(s) from a space
 * @author Todd Elvers & Skylar Stark
 */
function removeSolvers(selectedSolvers,ownsAll){
	var removeSolverButtons = {
		// The remove from space button and cancel button will be in the dialog no matter what.
		'remove from space': function() {
			removeSolversFromSpace(selectedSolvers);
		},
	}; 

	if (!isLeafSpace) {
		// Only add the hierarchy option if the space is not a leaf.
		removeSolverButtons['remove from space hierarchy'] = function() { 
			removeSolversFromSpaceHierarchy(selectedSolvers); 
		};
	} 

	var dialogText = null;
	if (ownsAll) {
		// Add the move to recycle bin button if the user owns the solvers.
		removeSolverButtons['move to recycle bin'] = function() {
			moveSolversToRecycleBin(selectedSolvers);
		};
	} 
	if (ownsAll && isLeafSpace) {
		dialogText = 'do you want to remove the solver(s) from ' + spaceName + " or would you like to move them to the recycle bin?";
	} else if (ownsAll && !isLeafSpace) {
		dialogText = 'do you want to remove the solver(s) from ' + spaceName + ', from ' +spaceName 
			+' and its hierarchy, or would you like to move them to the recycle bin?'; 
	} else if (!ownsAll && isLeafSpace) {
		dialogText = 'do you want to remove the solver(s) from ' + spaceName + '?';
	} else {
		dialogText = 'do you want to remove the solver(s) from ' + spaceName+' or from '+spaceName+' and its hierarchy?';
	}

	// Add cancel button last
	removeSolverButtons['cancel'] = function() {
		$(this).dialog("close");
	};

	
	$('#dialog-confirm-delete-txt').text(dialogText);

	// Display the confirmation dialog
	$('#dialog-confirm-delete').dialog({
		modal: true,
		width: 430,
		height: 250,
		buttons: removeSolverButtons
	});		
}

/**
 * Handles removal of job(s) from a space
 * @author Todd Elvers
 */
function removeJobs(selectedJobs,ownsAll){
	if (ownsAll) {
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
								s=parseReturnCode(returnCode);
								if (s) {
									updateTable(jobTable);
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
							starexecRoot+"services/deleteandremove/job/"+spaceId,
							{selectedIds : selectedJobs},
							function(returnCode) {
								s=parseReturnCode(returnCode);
								if (s) {
									updateTable(jobTable);
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
	} else {
		$('#dialog-confirm-delete-txt').text('do you want to remove the selected job(s) from ' + spaceName + '?');

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
								s=parseReturnCode(returnCode);
								if (s) {
									updateTable(jobTable);
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
		
}

/**
 * Handles removal of subspace(s) from a space
 * @author Todd Elvers
 */
function removeSubspaces(selectedSubspaces){
	$('#dialog-confirm-delete-txt').text('Do you want to recycle the solvers and benchmarks, and delete the jobs in the selected subspace(s), and all their subspaces, or do you only want to remove the selected subspace(s) from ' + spaceName + '?'); // Display the confirmation dialog
	$('#dialog-confirm-delete').dialog({
		modal: true,
		height: 400,
		width: 400,
		buttons: {
			"remove subspace(s) only" : function() {
				log('user confirmed subspace deletion');
				// If the user actually confirms, close the dialog right away
				$('#dialog-confirm-delete').dialog('close');
				makeRemoveSubspacesPost(selectedSubspaces, false);
				selectedSubspaces.forEach(function(subspace) {
					$('#exploreList').jstree("remove", "#"+subspace);
				}); 
				$('#exploreList').jstree("refresh");
			},
			'remove subspace(s), and recycle primitives': function() {
				log('user confirmed subspace deletion');
				// If the user actually confirms, close the dialog right away
				$('#dialog-confirm-delete').dialog('close');
				makeRemoveSubspacesPost(selectedSubspaces, true);
				selectedSubspaces.forEach(function(subspace) {
					$('#exploreList').jstree("remove", "#"+subspace);
				}); 
				$('#exploreList').jstree("refresh");
				
			},
			"cancel": function() {
				log('user canceled subspace deletion');
				$(this).dialog("close");
			}
		}		
	});		
}



function makeRemoveSubspacesPost(selectedSubspaces, recyclePrims) {
	$.post(  starexecRoot+"services/remove/subspace",
			{selectedIds : selectedSubspaces, recyclePrims : recyclePrims},					
			function(returnCode) {
				parseReturnCode(returnCode);
				
			},
			"json"
	).error(function(){
		log('remove subspace error');
	});
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
	if(idOfSelectedSpace == null || typeof idOfSelectedSpace == 'undefined'){
		idOfSelectedSpace = $.cookie("jstree_select");
		// If we also can't find the cookie, then just set the space selected to be the root space
		if(idOfSelectedSpace == null || typeof idOfSelectedSpace == 'undefined'){
			$('#exploreList').jstree('select_node', '#1', true);
			idOfSelectedSpace = 1;
		} else {
			idOfSelectedSpace = idOfSelectedSpace[1];
		} 
	}

	if (sortOverride!=null && tableName=="benchmarks") {
		aoData.push( { "name": "sort_by", "value":getSelectedSort() } );
		aoData.push( { "name": "sort_dir", "value":isASC() } );
	}
	// Request the next page of primitives from the server via AJAX
	log('Source: '+sSource +idOfSelectedSpace + "/" + tableName + "/pagination");
	$.ajax({
		type: 'POST',
		url: sSource + idOfSelectedSpace + "/" + tableName + "/pagination",
		data: aoData,
		dataType: "json"
	}).done(function(nextDataTablePage) {
		var s=parseReturnCode(nextDataTablePage,false);
		if (s) {
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
		}
	}).always(function() {
		// Reload the jobs table 10 seconds after receiving the response.
		if (spaceId != 1 && typeof spaceId != 'undefined' && 'j' == tableName[0]) {
			log('Setting new call to happen in 10 seconds.');
			setTimeout(function() {
				var rows = $(jobTable).children('tbody').children('tr.row_selected');
				if (rows.length == 0) {
					jobTable.fnDraw(false);
				}
			}, 10000);
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
 * Initializes the DataTable objects and adds multi-select to them
 */
function initDataTables(){
	
	// Extend the DataTables api and add our custom features
	extendDataTableFunctions();

	// Setup the DataTable objects
	userTable = $('#users').dataTable( {
		"sDom"			: getDataTablesDom(),
		"iDisplayStart"	: 0,
		"iDisplayLength": defaultPageSize,
		"bServerSide"	: true,
		"pagingType"    : "full_numbers",

		"sAjaxSource"	: starexecRoot+"services/space/",
		"sServerMethod" : 'POST',
		"fnServerData"	: fnPaginationHandler
	});
	solverTable = $('#solvers').dataTable( {
		"sDom"			: getDataTablesDom(),
		"iDisplayStart"	: 0,
		"iDisplayLength": defaultPageSize,
		"bServerSide"	: true,
		"pagingType"    : "full_numbers",

		"sAjaxSource"	: starexecRoot+"services/space/",
		"sServerMethod" : 'POST',
		"fnServerData"	: fnPaginationHandler
	});
	benchTable = $('#benchmarks').dataTable( {
		"sDom"			: getDataTablesDom(),
		"iDisplayStart"	: 0,
		"iDisplayLength": defaultPageSize,
		"bServerSide"	: true,
		"pagingType"    : "full_numbers",

		"sAjaxSource"	: starexecRoot+"services/space/",
		"sServerMethod" : "POST",
		"fnServerData"	: fnPaginationHandler
	});
	
	setSortTable(benchTable);
	
	$("#benchmarks thead").click(function(){
		resetSortButtons();
	});
	
	jobTable = $('#jobs').dataTable( {
		"sDom"			: getDataTablesDom(),
		"iDisplayStart"	: 0,
		"iDisplayLength": defaultPageSize,
		"bServerSide"	: true,
		"pagingType"    : "full_numbers",

		"sAjaxSource"	: starexecRoot+"services/space/",
		"bProcessing"	: false,
		"oLanguage": {
			"sProcessing": getProcessingMessage()
		},
		"sServerMethod" : "POST",
		"aaSorting"		: [],	// On page load, don't sort by any column - tells server to sort by 'created'
		"fnServerData"	: fnPaginationHandler 
	});

	spaceTable = $('#spaces').dataTable( {
		"sDom"			: getDataTablesDom(),
		"iDisplayStart"	: 0,
		"iDisplayLength": defaultPageSize,
		"bServerSide"	: true,
		"pagingType"    : "full_numbers",
		"sAjaxSource"	: starexecRoot+"services/space/",
		"sServerMethod" : "POST",
		"bProcessing"	: false,
		"oLanguage": {
			"sProcessing": getProcessingMessage()
		},
		"fnServerData"	: fnPaginationHandler
	});
	

	var tables=["#users","#solvers","#benchmarks","#jobs","#spaces"];

	function unselectAll(except) {
		var tables=["#users","#solvers","#benchmarks","#jobs","#spaces"];
		for (x=0;x<6;x++) {

			if (except==tables[x]) {
				continue;
			}
			$(tables[x]).find("tr").removeClass("row_selected");
		}
	}
	
	
	for (x=0;x<6;x++) {
		$(tables[x]).on("mousedown","tr", function(){
			unselectAll("#"+$(this).parent().parent().attr("id"));
			$(this).toggleClass("row_selected");
		});
	}
		// Setup user permission tooltip
	$('#users tbody').on( 'hover', 'tr', function(){
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
	$('fieldset:not(.actions)').expandable(true);
	$('fieldset.advancedActions').expandable(true);

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
	addProcessingIndicator();
	addFilterOnDoneTyping();
}

/**
 * Returns the 'processing...' image with the loading gif which is used
 * on the jobs table when deleting/paginating
 */
function getProcessingMessage(){
	return "processing request";
}

function redrawAllTables() {
	benchTable.fnDraw();
	jobTable.fnDraw();
	userTable.fnDraw();
	solverTable.fnDraw();
	spaceTable.fnDraw();
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
		
		$(element).on('mouseenter mouseleave',selector, function(){
			// Check and see if a qtip object already exists
			if(!$(this).data("qtip")){
				// If not, create one with the relevant configuration
				configuration=getTooltipConfig(type,message);
				$(this).qtip(configuration);
			}
		});
	}
	/**
	 * Tooltips for displaying to a user what their permission are for a given space in the space explorer; 
	 * these persist forever and are never removed from the page
	 */
	else if(type[0] == 'p'){
		$(element).on('mouseenter mouseleave',selector,  function(){
			//only add this to the row for the current user
			if ($(this).find("td input[name=\"currentUser\"]").size()>0) {
				if(!$(this).data("qtip")){
					$(this).qtip(getTooltipConfig(type, message));
				}
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
	// Don't hide if user is developer
	if (userIsDeveloper) {
		$('.actionList').show();
		return;
	} else if (perms == null) {
		log('no permissions found, hiding action bar');
		$('.actionList').hide();		
		retu.n;
	} else {
		$('.actionList').show();
	}

	if(perms.isLeader){
		// attach leader tooltips to every entry 
		createTooltip($('#users tbody'), 'tr', 'leader');
		
		$('#editSpace').fadeIn('fast');
		//$('#editSpacePermissions').fadeIn('fast');

	} else {
		// Otherwise only attach a personal tooltip to the current user's entry in the userTable
		createTooltip($('#users tbody'), 'tr', 'personal');
		$('#editSpace').fadeOut('fast');
		//$('#editSpacePermissions').fadeOut('fast');
	}	

	log('perms.addSpace='+perms.addSpace);
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
		$("#addQuickJob").fadeIn('fast');
		$('#uploadJobXML').fadeIn('fast');
		
	} else {
		$('#addJob').fadeOut('fast');
		$('#uploadJobXML').fadeOut('fast');
		$("#addQuickJob").fadeOut('fast');
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
	$('#editSpacePermissions').attr('href', starexecRoot+"secure/edit/spacePermissions.jsp?id=" + id);
	$('#addSpace').attr('href', starexecRoot+"secure/add/space.jsp?sid=" + id);
	$('#uploadBench').attr('href', starexecRoot+"secure/add/benchmarks.jsp?sid=" + id);
	$('#uploadSolver').attr('href', starexecRoot+"secure/add/solver.jsp?sid=" + id);
	$('#addJob').attr('href', starexecRoot+"secure/add/job.jsp?sid=" + id);
	$('#addQuickJob').attr('href', starexecRoot+"secure/add/quickJob.jsp?sid=" + id);

	$("#processBenchmarks").attr("href",starexecRoot+"secure/edit/processBenchmarks.jsp?sid="+id);
	



        $("#downloadXML").unbind("click");
        $('#downloadXML').click(function (e) {
	    $('#dialog-spacexml-attributes-txt').text('Do you want benchmark attributes included in the XML?');
	    

	    $('#dialog-spacexml').dialog({
		modal: true,
		width: 380,
		height: 300,
		buttons: {
		    "download": function () {
			var attVal = $('input[name=att]:checked').val();
			attBool = attVal == "true";
			
			createDownloadSpaceXMLRequest(attBool, false,-1, id);
			$(this).dialog("close");
		    },
                    "cancel": function () {
			$(this).dialog("close");
		    }
		}
	    });
	});

    $('#showUpdateDialog').click(function(){
        $("#dialog-spaceUpdateXml").dialog();
        $('#dialog-spacexml').dialog("close");
        $('#dialog-spacexml-updates-txt').text('Enter default update processor id');
        $('#dialog-spaceUpdateXml').dialog({
		modal: true,
		width: 380,
		height: 300,
		buttons: {
		    "download": function () {
			var updatePID = $('#updateID').val();
			createDownloadSpaceXMLRequest(false, true,updatePID, id);
			$(this).dialog("close");
		    },
                    "cancel": function () {
			$(this).dialog("close");
		    }
		}
	    });
    });
	
	$('#uploadJobXML').attr('href', starexecRoot+"secure/add/batchJob.jsp?sid=" + id);
	$('#uploadXML').attr('href', starexecRoot+"secure/add/batchSpace.jsp?sid=" + id);
	$("#downloadSpace").unbind("click");
	$("#downloadSpace").click(function(){		
		// Display the confirmation dialog
		$("#downloadBoth").prop("checked","checked");
		$('#noIdDirectories').prop('checked','checked');
		var dialogHeight=500;
		if (isLeafSpace) {
			$('#downloadHierarchyOptionContainer').hide();
			dialogHeight=400;
		} else {
			$('#downloadHierarchyOptionContainer').show();
			dialogHeight=500;
		}
		$('#dialog-download-space').dialog({
			modal: true,
			width: 380,
			height: dialogHeight,
			buttons: {
				'submit': function(){
					createDownloadSpacePost(id);
					$(this).dialog("close");
				}
			}
		});
	});
	log('updated action button space ids to ' + id);
}


function createDownloadSpaceXMLRequest(includeAttrs,updates,upid,id) {
  createDialog("Processing your download request, please wait. This will take some time for large spaces.");
  token=Math.floor(Math.random()*100000000);
  myhref = starexecRoot+"secure/download?token=" +token+ "&type=spaceXML&id="+id+"&includeattrs="+includeAttrs+"&updates="+updates+"&upid="+upid;
  destroyOnReturn(token);
  window.location.href = myhref;
 
}

function createDownloadSpacePost(id) {
	var hierarchy = $('#downloadSpaceHierarchy').prop("checked");
	var downloadSolvers=($("#downloadSolvers").prop("checked") || $("#downloadBoth").prop("checked"));
	
	var downloadBenchmarks=($("#downloadBenchmarks").prop("checked") || $("#downloadBoth").prop("checked"));
	var useIdDirectories = $('#yesIdDirectories').prop('checked');
	log('hierarchy: ' + hierarchy);
	log('useIdDirectories: ' + useIdDirectories);
	log('downloadSolvers: ' + downloadSolvers);
	log('downloadBenchmarks: ' + downloadBenchmarks);
	log('useIdDirectories: ' + useIdDirectories);
	createDialog("Processing your download request, please wait. This will take some time for large spaces.");
	token=Math.floor(Math.random()*100000000);
	window.location.href=starexecRoot+"secure/download?includesolvers="+downloadSolvers+"&includebenchmarks="+downloadBenchmarks+
		"&useIdDirectories="+useIdDirectories+"&token="+token+"&type=space&hierarchy="+hierarchy+"&id="+id;
	destroyOnReturn(token);
}


/**
 * For a given dataTable, this returns true if the user is allowed to delete
 * every selected primitive. This occurs if they own all of them and none of them
 * have already been recycled / deleted
 * 
 * @param dataTable the particular dataTable the selections are in
 * @author Eric Burns
 */
function userCanDeleteAll(dataTable){
	allMatch=true;
	var rows = $(dataTable).children('tbody').children('tr.row_selected');
	$.each(rows, function(i, row) {
		if (!allMatch) {
			return;
		}
		input=$(this).children('td:first').children("input");
		if(parseInt(input.attr("userId"))!=currentUserId) {
			allMatch=false;
		}
		
		if (parseBoolean(input.attr("recycled")) || parseBoolean(input.attr("deleted"))) {
			allMatch=false;
		}
	});
	return allMatch;
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
function getPermTable(tooltip, perms, type, isCommunity) {	
	var permWrap = $('<div>');	// A wrapper for the table and leader info
	var table = $('<table class="tooltipTable">');	// The table where the permissions are displayed
	$(table).append('<tr><th>property</th><th>add</th><th>remove</th></tr>');

	// Resolves bug where tooltip is empty
	if('undefined' == typeof perms || null == perms){
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

	$(permWrap).append("<div><input class=\"permButton\" type='button' value='edit' onClick='editPermissions()'></input></div>");
	$(".permButton").button();
	// HTML to add to the wrapper to indicate someone is a leader
	var leaderDiv = '<div class="leaderWrap"><span class="ui-icon ui-icon-star"></span><h2 class="leaderTitle">leader</h2></div>';
	
	if(perms.isLeader) {
		// If this person is a leader, add the leader div to the wrapper
		$(permWrap).append(leaderDiv);
		
	} 

	

	// Return the resulting DOM element to be inserted
	return permWrap;
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
 * used in tooltip, links to edit permissions page
 *
 **/
function editPermissions(){
    location = starexecRoot+"secure/edit/spacePermissions.jsp?id=" + spaceId;
}
/**
 * Wraps up a permission and it's value for display in a table. Includes onclicks for the images.
 * @param perm The permission type to display (job, user, solver, bench, space)
 * @param add The value for the add permission for the type
 * @param remove The value for the remove permission for the type
 * @returns HTML representing a row in a table display the type and it's permission values
 */
function wrapPermRow(perm, add, remove){
    var yes = $('<span>').css('margin', 'auto').addClass('ui-icon ui-icon-check').toHTMLString();
    var no = $('<span>').css('margin', 'auto').addClass('ui-icon ui-icon-closethick').toHTMLString();
    return "<tr><td>" + perm + "</td><td class='add'>" + (add ? yes : no) + "</td><td class='remove'>" + (remove ? yes : no) + "</td></tr>"; 
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
				title: '<center><a>permissions</a></center>'	
			},
			position: {			// Place right middle portion of the tooltip to the left middle portion of the row element
				target: "mouse",
				my: "right center",
				at: "left center",
				adjust: {
					mouse: false
				}
				
			},
			hide :{
				fixed: true		
			},
			show: { 
				ready: true,	// Ensures the tooltip is shown the first time it's moused over
				solo: false,	// When this is false, all tooltip commands are applied only to the corresponding tooltip (what we want) instead of to all tooltips on the page (which causes weird artifacts to occur)
				delay: 1000,	// Every mouseover that occurs, after the first mouseover, will have to wait a second before the tooltip is triggered
				event: "mouseover"
				
			},
			style: {
				classes: "userTooltip",		// Load custom color scheme
				tip: 'rightMiddle'			// Add a tip to the right middle portion of the tooltip
			},			   
			events:{
				render: function(){	// Before rendering the tooltip, get the user's permissions for the given space
					var tooltip = this;
					api=$(this).qtip("api");
					var userId = $(api.elements.target).children('td:first').children('input').val();
					$.get(
							starexecRoot+'services/permissions/details/' + userId + '/' + spaceId,
							function(theResponse){
								s=parseReturnCode(theResponse);
								if (s) {
									// Replace current content (current = loader.gif)		
									$(tooltip).qtip('option', 'content.text', ' ');
									if (theResponse.requester.role == "admin") {
										$(tooltip).qtip('option', 'content.text', getPermTable(tooltip, theResponse.perm, 'admin', theResponse.isCommunity));

									} else {
										$(tooltip).qtip('option', 'content.text', getPermTable(tooltip, theResponse.perm, 'leader', theResponse.isCommunity));
									}
									$(".permButton").button();
								}
								return true;
							}
					).error(function(){
						//showMessage('error',"Internal error getting user permissions",5000); bother the user for a tooltip problem?
					});	

				},
				hide: function(){			// If a user modifies a tooltip but does not press the 'save' or 'cancel' button then this resets the tooltip once it loses focus and fades from view
					var tooltip = this;
					api=$(this).qtip("api");
					if('p' != $(api.elements.title).text()[0]){
						$(tooltip).qtip('option', 'content.title', '<center><a>permissions</a></center>');

						var userId = $(api.elements.target).children('td:first').children('input').val();
						$.post(
								starexecRoot+'services/permissions/details/' + userId + '/' + spaceId,
								function(theResponse){
									log('AJAX response for permission tooltip received');
									$(tooltip).qtip('option', 'content.text', ' ');
									if (theResponse.requester.role == "admin") {
										$(tooltip).qtip('option', 'content.text', getPermTable(tooltip, theResponse.perm, 'admin'));

									} else {
										$(tooltip).qtip('option', 'content.text', getPermTable(tooltip, theResponse.perm, 'leader'));

									}
									$(tooltip).qtip('option', 'content.title', '<center><a>permissions</a></center>');

									tooltip.hide();
									return true;
								}	
						).error(function(){
							//showMessage('error',"Internal error getting space details",5000); 
						});		
					}

					// Fixes bug where 'hovered' class doesn't get removed from the no-longer-hovered tr element
					var userId = $(api.elements.target).children('td:first').children('input').val();
					$('#uid'+userId).parent().parent().removeClass('hovered');
				}

			}
		};
	}
	
	// Expd tooltips
	else if (type[0] == 'e'){
		return {
			content: {
				text: message,
				title:  '<center>permissions</center>'
				
			},
			position: {
				target: "mouse",
				my: "right center",
				at: "left center",
				adjust: {
					mouse: false
				}
			},
			show: { 
				solo: true,
				delay: 1000,
				event: "mouseover"
			},
			hide:{
				effect: {
					type: 'slide',
					length: 150
				}
			},
			style: {
				classes: 'expdTooltip'
			}
		};
	}
	// Personal tooltips
	else if (type[0] == 'p'){
		return {
			content: {
				text: getProcessingMessage(),
				title: '<center><a>permissions</a></center>'
				
			},
			position: {
				target: "mouse",
				my: "right center",
				at: "left center",
				adjust: {
					mouse: false
				}
			},
			show: { 
				ready: true,
				solo: false,
				delay: 1000,
				event: "mouseover"
				
			},
			hide:{
				effect: {
					type: 'fade',
					length: 100
				}
			},
			style: {
				classes: "userTooltip",
				tip: 'rightMiddle'
			},
			events:{
				render: function(){
					var tooltip = this;
					api=$(this).qtip("api");
					var userId =  $("#users tbody tr").find('td:first input[name="currentUser"]').val();
					if(typeof userId != 'undefined' && $(api.elements.target).children('td:first').children('input').val() == userId){
						var url = starexecRoot+'services/space/' + spaceId + '/perm/' + userId;
						$.post(
								url,
								function(theResponse){
									log('AJAX response for permission tooltip received');
									$(tooltip).qtip('option', 'content.text', ' ');
									$(tooltip).qtip('option', 'content.text', getPermTable(tooltip, theResponse));
									$(".permButton").button();

									return true;
								}
						)
					}
				}
			}
		};
	}	
}





