var userTable;
var benchTable;
var solverTable;
var spaceTable;
var jobTable;
var spaceId; // id of the current space
var spaceName; // name of the current space

$(document).ready(function(){		
	// Builds the space explorer tree and attaches click listeners to the 'action' buttons
	initTree();
	
	// Builds the DataTable objects and enables multi-select on them
	initDataTables();
	
	// Hide tables by default (make users expand them manually)
	$('.dataTables_wrapper').hide();
	
	// Set up jQuery UI buttons
	initButtonUI();	
	
	// Set up jQuery UI dialog boxes
	initDialogs();
});

/**
 * Hides all jquery ui dialogs for page startup
 */
function initDialogs() {
	$( "#dialog-confirm-copy" ).hide();
	$( "#dialog-confirm-delete" ).hide();
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
	
	$('.btnRun').button({
		icons: {
			secondary: "ui-icon-gear"
    }});
	
	$('.btnRemove').button({
		icons: {
			secondary: "ui-icon-minus"
    }});	
}

/**
 * Initializes a table so that elements can be dragged out of it and onto a space name
 * @param table The table to make draggable
 */
function initDraggable(table) {
	// Using jQuery UI, make the first column in each row draggable
	$(table).children('tbody').children('tr').draggable({
		cursorAt: { cursor: 'move', bottom: 10, left: 34},	// Set the cursor to the move icon and make it start in the corner of the helper		
		containment: '#content',	// Allow the element to be dragged anywhere in the browser window
		distance: 20,			// Only trigger a drag when the distanced dragged is > 20 pixels
		scroll: true,			// Scroll with the page as the item is dragged if needed
		helper: getDragClone,	// The method that returns the 'cloned' element that is dragged
		start: onDragStart,		// Method called when the dragging begins
		stop: onDragStop		// Method called when the dragging ends
	});
	
	// Make each space in the explorer list be a droppable target
	$('#exploreList').find('a').droppable( {
		// Function which is called when an item is dropped on a space
	    drop: onDrop,
	    hoverClass: 'hover',	// Class applied to the space element when something is being dragged over it
	    activeClass: 'active'	// Class applied to the space element when something is being dragged
	});
}
 
/**
 * Called when any item is starting to be dragged within the browser
 */
function onDragStart(event, ui) {
	// Alter our droppable components when a drag starts to they appear bigger
	$('#exploreList').find('a').css('padding', '10px');	
}

/**
 * Called when there is no longer anything being dragged
 */
function onDragStop(event, ui) {
	// Return the droppable components to their normal state so they go back to normal	
	$('#exploreList').find('a').css('padding', '0px');
}

/**
 * Called when a draggable item (primitive) is dropped on a space
 */
function onDrop(event, ui) {
	// Collect the selected elements from the table being dragged from
	var ids = getSelectedRows($(ui.draggable).parents('table:first'));	    	
	
	// Get the destination space id and name
	var destSpace = $(event.target).parent().attr('id');
	var destName = $(event.target).text();
	
	if(ids.length < 2) {
		// If 0 or 1 things are selected in the table, just use the element that is being dragged
		ids = [ui.draggable.data('id')];
		
		// Customize the confirmation message for the copy operation to the primitives/spaces involved
		$('#dialog-confirm-copy-txt').text('are you sure you want to copy ' + ui.draggable.data('name') + ' to' + destName + '?');
	} else {
		$('#dialog-confirm-copy-txt').text('are you sure you want to copy the ' + ids.length + ' selected ' + ui.draggable.data('type') + 's to' + destName + '?');		
	}		
	
	// Display the confirmation dialog
	$('#dialog-confirm-copy').dialog({
		modal: true,
		buttons: {
			'yes': function() {
				// If the user actually confirms, close the dialog right away
				$('#dialog-confirm-copy').dialog('close');
				
				// Make the request to the server
				$.post(  	    		
					'/starexec/services/spaces/' + destSpace + '/add/' + ui.draggable.data('type'), // We use the type to denote copying a user/solver/benchmark/job
					{selectedIds : ids, fromSpace : spaceId},	// Attach the selected items to the data of the post along with the space it's coming from
					function(returnCode) {
						switch (returnCode) {
							case 0:	// Success
								if(ids.length > 1) {								
									showMessage('success', ids.length + ' ' + ui.draggable.data('type') + 's successfully copied to' + destName, 2000);
						    	} else {					    		
						    		showMessage('success', ui.draggable.data('name') + ' successfully copied to' + destName, 2000);	
						    	}
								break;
							case 1: // Database error
								showMessage('error', "a database error occurred while processing your request", 5000);
								break;
							case 2: // Invalid parameters
								showMessage('error', "invalid parameters supplied to server, operation failed", 5000);
								break;
							case 3: // No add permission in dest space
								showMessage('error', "you do not have permission to add " + ui.draggable.data('type') + "s to" + destName, 5000);
								break;
							case 4: // User doesn't belong to from space
								showMessage('error', "you do not belong to the space that is being copied from", 5000);
								break;
							case 5: // From space is locked
								showMessage('error', "the space leader has indicated the current space is locked. you cannot copy from locked spaces.", 5000);
								break;
							default:
								showMessage('error', "the operation failed with an unknown return code", 5000);	
						}
					},
					"json"
				).error(function(){
					alert('Session expired');
					window.location.reload(true);
				});	 									
			},
			"cancel": function() {
				$(this).dialog("close");
			}
		}		
	});			   		    	    	
}

/**
 * Returns the html of an element that is dragged along with the mouse when an item is dragged on the page
 */
function getDragClone(event) {	
	var src = $(event.currentTarget);	
	var ids = getSelectedRows($(src).parents('table:first'));
	var txtDisplay = $(src).children(':first-child').text();
	var icon = 'ui-icon ';	
	var primType = $(src).data('type');
	
	if(ids.length > 1) {
		txtDisplay = ids.length + ' ' + primType + 's';
	}
	
	// Change the drag icon based on what the type of object being dragged is
	if(primType == 'user') {
		icon += 'ui-icon-person';
	} else if (primType == 'benchmark') {
		icon += 'ui-icon-script';
	} else if (primType == 'job') {
		icon += 'ui-icon-gear';
	} else {
		icon += 'ui-icon-newwin';
	}
	
	// Return a styled div with the name of the element that was originally dragged
	return '<div class="dragClone"><span class="' + icon + '"></span>' + txtDisplay + '</div>';
}

/**
 * Creates the space explorer tree and adds click listeners to the 'action' buttons
 */
function initTree(){
	var id = -1;
	// Set the path to the css theme for the jstree plugin
	 $.jstree._themes = "/starexec/css/jstree/";
	 
	 // Initialize the jstree plugin for the explorer list
	jQuery("#exploreList").jstree({  
		"json_data" : { 
			"ajax" : { 
				"url" : "/starexec/services/space/subspaces",	// Where we will be getting json data from 
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
						"image" : "/starexec/images/jstree/db.png"
					}
				}
			}
		},
		"initially_select" : [ "1" ],
		"plugins" : [ "types", "themes", "json_data", "ui", "cookies"] ,
		"core" : { animation : 200 }
	}).bind("select_node.jstree", function (event, data) {
		// When a node is clicked, get its ID and display the info in the details pane		
        id = data.rslt.obj.attr("id");
        updateAddId(id);
        getSpaceDetails(id);
        
        // Hide all buttons that are selection-dependent
        initButtons();
    }).delegate("a", "click", function (event, data) { event.preventDefault(); });	// This just disable's links in the node title
	
	
	// Handles removal of benchmark(s) from a space
	$("#removeBench").click(function(){
		var selectedBenches = getSelectedRows(benchTable);
		$('#dialog-confirm-delete-txt').text('are you sure you want to remove the selected benchmark(s) from ' + spaceName + '?');
		
		// Display the confirmation dialog
		$('#dialog-confirm-delete').dialog({
			modal: true,
			buttons: {
				'yes': function() {
					// If the user actually confirms, close the dialog right away
					$('#dialog-confirm-delete').dialog('close');
					
					$.post(  
						"/starexec/services/remove/benchmark/" + id,
						{selectedBenches : selectedBenches},
						function(returnCode) {
							switch (returnCode) {
								case 0:
									// Remove the rows from the page and update the table size in the legend
									updateTable(benchTable);
									$("#removeBench").fadeOut("fast");
									break;
								case 1:
									showMessage('error', "an error occurred while processing your request; please try again", 5000);
									break;
								case 2:
									showMessage('error', "you do not have sufficient privileges to remove benchmarks from this space", 5000);
									break;
							}
						},
						"json"
					).error(function(){
						alert('Session expired');
						window.location.reload(true);
					});		
				},
				"cancel": function() {
					$(this).dialog("close");
				}
			}		
		});				
	});
	
	// Handles removal of user(s) from a space
	$("#removeUser").click(function(){
		var selectedUsers = getSelectedRows(userTable);			
		$('#dialog-confirm-delete-txt').text('are you sure you want to remove the selected users(s) from ' + spaceName + '?');
		
		// Display the confirmation dialog
		$('#dialog-confirm-delete').dialog({
			modal: true,
			buttons: {
				'yes': function() {
					// If the user actually confirms, close the dialog right away
					$('#dialog-confirm-delete').dialog('close');
					
					$.post(  
						"/starexec/services/remove/user/" + id,
						{selectedUsers : selectedUsers},
						function(returnCode) {
							switch (returnCode) {
								case 0:
									// Remove the rows from the page and update the table size in the legend
									updateTable(userTable);
									$("#removeUser").fadeOut("fast");
									break;
								case 1:
									showMessage('error', "an error occurred while processing your request; please try again", 5000);
									break;
								case 2:
									showMessage('error', "you do not have sufficient privileges to remove other users from this space", 5000);
									break;
								case 3:
									showMessage('error', "you can not remove yourself from this space in that way, " +
											"instead use the 'leave' button to leave this community", 5000);
									break;
								case 4:
									showMessage('error', "you can not remove other leaders of this space", 5000);
									break;
							}
						},
						"json"
					).error(function(){
						alert('Session expired');
						window.location.reload(true);
					});	
				},
				"cancel": function() {
					$(this).dialog("close");
				}
			}		
		});		
	});
	
	// Handles removal of solver(s) from a space
	$("#removeSolver").click(function(){
		var selectedSolvers = getSelectedRows(solverTable);		
		$('#dialog-confirm-delete-txt').text('are you sure you want to remove the selected solver(s) from ' + spaceName + '?');
		
		// Display the confirmation dialog
		$('#dialog-confirm-delete').dialog({
			modal: true,
			buttons: {
				'yes': function() {
					// If the user actually confirms, close the dialog right away
					$('#dialog-confirm-delete').dialog('close');
					
					$.post(  
						"/starexec/services/remove/solver/" + id,
						{selectedSolvers : selectedSolvers},
						function(returnCode) {
							switch (returnCode) {
								case 0:
									// Remove the rows from the page and update the table size in the legend
									updateTable(solverTable);
									$("#removeSolver").fadeOut("fast");
									break;
								case 1:
									showMessage('error', "an error occurred while processing your request; please try again", 5000);
									break;
								case 2:
									showMessage('error', "you do not have sufficient privileges to remove solvers from this space", 5000);
									break;
							}
						},
						"json"
					).error(function(){
						alert('Session expired');
						window.location.reload(true);
					});
				},
				"cancel": function() {
					$(this).dialog("close");
				}
			}		
		});		
	});
	
	// Handles removal of job(s) from a space
	$("#removeJob").click(function(){
		var selectedJobs = getSelectedRows(jobTable);	
		$('#dialog-confirm-delete-txt').text('are you sure you want to remove the selected job(s) from ' + spaceName + '?');
		
		// Display the confirmation dialog
		$('#dialog-confirm-delete').dialog({
			modal: true,
			buttons: {
				'yes': function() {
					// If the user actually confirms, close the dialog right away
					$('#dialog-confirm-delete').dialog('close');
					
					$.post(  
						"/starexec/services/remove/job/" + id,
						{selectedJobs : selectedJobs},
						function(returnCode) {
							switch (returnCode) {
								case 0:
									// Remove the rows from the page and update the table size in the legend
									updateTable(jobTable);
									$("#removeJob").fadeOut("fast");
									break;
								case 1:
									showMessage('error', "an error occurred while processing your request; please try again", 5000);
									break;
								case 2:
									showMessage('error', "you do not have sufficient privileges to remove jobs from this space", 5000);
									break;
							}
						},
						"json"
					).error(function(){
						alert('Session expired');
						window.location.reload(true);
					});
				},
				"cancel": function() {
					$(this).dialog("close");
				}
			}		
		});		
	});
	
	// Handles removal of subspace(s) from a space
	$("#removeSubspace").click(function(){
		var selectedSubspaces = getSelectedRows(spaceTable);			
		$('#dialog-confirm-delete-txt').text('are you sure you want to remove the selected subspace(s) from ' + spaceName + '?');
		
		// Display the confirmation dialog
		$('#dialog-confirm-delete').dialog({
			modal: true,
			buttons: {
				'yes': function() {
					// If the user actually confirms, close the dialog right away
					$('#dialog-confirm-delete').dialog('close');
					
					$.post(  
						"/starexec/services/remove/subspace/" + id,
						{selectedSubspaces : selectedSubspaces},
						function(returnCode) {
							switch (returnCode) {
								case 0:
									// Remove the rows from the page and update the table size in the legend
									updateTable(spaceTable);
									$("#removeSubspace").fadeOut("fast");
									initTree();
									break;
								case 1:
									showMessage('error', "an error occurred while processing your request; please try again", 5000);
									break;
								case 2:
									showMessage('error', "you do not have sufficient privileges to remove subspaces from this space", 5000);
									break;
								case 3:
									showMessage('error', "you can only delete subspaces that themselves have no subspaces", 5000);
									break;
							}
						},
						"json"
					).error(function(){
						alert('Session expired');
						window.location.reload(true);
					});
				},
				"cancel": function() {
					$(this).dialog("close");
				}
			}		
		});		
	});
}

/**
 * Initializes the DataTable objects and adds multi-select to them
 */
function initDataTables(){
	// DataTables setup
	userTable = $('#users').dataTable( {
        "sDom": 'rt<"bottom"flpi><"clear">'
    });
	solverTable = $('#solvers').dataTable( {
        "sDom": 'rt<"bottom"flpi><"clear">'
    });
	benchTable = $('#benchmarks').dataTable( {
        "sDom": 'rt<"bottom"flpi><"clear">'
    });
	jobTable = $('#jobs').dataTable( {
        "sDom": 'rt<"bottom"flpi><"clear">'
    });
	spaceTable = $('#spaces').dataTable( {
        "sDom": 'rt<"bottom"flpi><"clear">'
    });
	
	// Mulit-select setup
	$("#users").delegate("tr", "click", function(){
		$(this).toggleClass("row_selected");
	});
	$("#solvers").delegate("tr", "click", function(){
		$(this).toggleClass("row_selected");
	});
	$("#benchmarks").delegate("tr", "click", function(){
		$(this).toggleClass("row_selected");
	});
	$("#jobs").delegate("tr", "click", function(){
		$(this).toggleClass("row_selected");
	});
	$("#spaces").delegate("tr", "click", function(){
		$(this).toggleClass("row_selected");
	});
}

/**
 * Populates the space details panel with information on the given space
 */
function getSpaceDetails(id) {
	$('#loader').show();
	$.post(  
		"/starexec/services/space/" + id,  
		function(data){  			
			populateDetails(data);			
		},  
		"json"
	).error(function(){
		alert('Session expired');
		window.location.reload(true);
	});
}

/**
 * Takes in a json  response and populates the details panel with information
 * @param jsonData the json data to populate the details page with
 */
function populateDetails(jsonData) {
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
	
	// Populate job details
	$('#jobField legend').children('span:first-child').text(jsonData.space.jobs.length);
	jobTable.fnClearTable();	
	$.each(jsonData.space.jobs, function(i, job) {	
		var hiddenJobId = '<input type="hidden" value="' + job.id + '" >';
		var jobLink = '<a href="/starexec/secure/details/job.jsp?id=' + job.id + '" target="blank">' + job.name + '<img class="extLink" src="/starexec/images/external.png"/></a>' + hiddenJobId;		
		jobTable.fnAddData([jobLink, job.status, job.description]);
		// Set up data about this primitive used for drag/drop
		$(jobTable.fnGetNodes(i)).data('id', job.id);
		$(jobTable.fnGetNodes(i)).data('type', 'job');
		$(jobTable.fnGetNodes(i)).data('name', job.name);
	});	
	initDraggable($('#jobs'));
	
	// Populate user details	
	$('#userField legend').children('span:first-child').text(jsonData.space.users.length);
	userTable.fnClearTable();		
	$.each(jsonData.space.users, function(i, user) {
		var hiddenUserId = '<input type="hidden" value="'+user.id+'" >';
		var fullName = user.firstName + ' ' + user.lastName;
		var userLink = '<a href="/starexec/secure/details/user.jsp?id=' + user.id + '" target="blank">' + fullName + '<img class="extLink" src="/starexec/images/external.png"/></a>' + hiddenUserId;
		var emailLink = '<a href="mailto:' + user.email + '">' + user.email + '<img class="extLink" src="/starexec/images/external.png"/></a>';
		userTable.fnAddData([userLink, user.institution, emailLink]);
		// Set up data about this primitive used for drag/drop
		$(userTable.fnGetNodes(i)).data('id', user.id);
		$(userTable.fnGetNodes(i)).data('type', 'user');
		$(userTable.fnGetNodes(i)).data('name', fullName);
	});
	initDraggable($('#users'));
	
	// Populate solver details
	$('#solverField legend').children('span:first-child').text(jsonData.space.solvers.length);
	solverTable.fnClearTable();
	$.each(jsonData.space.solvers, function(i, solver) {
		var hiddenSolverId = '<input type="hidden" value="' + solver.id + '" >';
		var solverLink = '<a href="/starexec/secure/details/solver.jsp?id=' + solver.id + '" target="blank">' + solver.name + '<img class="extLink" src="/starexec/images/external.png"/></a>' + hiddenSolverId;
		solverTable.fnAddData([solverLink, solver.description]);
		// Set up data about this primitive used for drag/drop
		$(solverTable.fnGetNodes(i)).data('id', solver.id);
		$(solverTable.fnGetNodes(i)).data('type', 'solver');
		$(solverTable.fnGetNodes(i)).data('name', solver.name);
	});	
	initDraggable($('#solvers'));
		
	// Populate benchmark details
	$('#benchField legend').children('span:first-child').text(jsonData.space.benchmarks.length);
	benchTable.fnClearTable();
	$.each(jsonData.space.benchmarks, function(i, bench) {
		var hiddenBenchId = '<input type="hidden" value="' + bench.id + '" >';
		var benchLink = '<a href="/starexec/secure/details/benchmark.jsp?id=' + bench.id + '" target="blank">' + bench.name + '<img class="extLink" src="/starexec/images/external.png"/></a>' + hiddenBenchId;
		benchTable.fnAddData([benchLink, bench.type.name, bench.description]);		
		// Set up data about this primitive used for drag/drop
		$(benchTable.fnGetNodes(i)).data('id', bench.id);
		$(benchTable.fnGetNodes(i)).data('type', 'benchmark');
		$(benchTable.fnGetNodes(i)).data('name', bench.name);
	});
	initDraggable($('#benchmarks'));
	
	// Populate subspace details
	$('#spaceField legend').children('span:first-child').text(jsonData.space.subspaces.length);
	spaceTable.fnClearTable();
	$.each(jsonData.space.subspaces, function(i, subspace) {
		var hiddenSubspaceId = '<input type="hidden" value="' + subspace.id + '" >';
		var spaceLink = '<a href="/starexec/secure/details/space.jsp?id=' + subspace.id + '" target="blank">' + subspace.name + '<img class="extLink" src="/starexec/images/external.png"/></a>' + hiddenSubspaceId;
		spaceTable.fnAddData([spaceLink, subspace.description]);		
	});
	
	// Check the new permissions for the loaded space
	checkPermissions(jsonData.perm);
	
	// Done loading, hide the loader
	$('#loader').hide();
}

/**
 * Checks the permissions for the current space and hides/shows buttons based on
 * the user's permissions
 * @param perms The JSON permission object representing permissions for the current space
 */
function checkPermissions(perms) {
	// Check for no permission and hide entire action list if not present
	if(perms == null) {
		$('#actionList').hide();		
		return;
	} else {
		$('#actionList').show();
	}
	
	if(perms.addSpace) {		
		$('#addSpace').fadeIn('fast');
	} else {
		$('#addSpace').fadeOut('fast');
	}
	
	if(perms.removeSpace) {
		$("#spaces").delegate("tr", "click", function(){
			updateButton(spaceTable, $("#removeSubspace"));
		});
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
	
	if(perms.removeUser){
		$("#users").delegate("tr", "click", function(){
			updateButton(userTable, $("#removeUser"));
		});
	}
	
	if(perms.removeSolver){
		$("#solvers").delegate("tr", "click", function(){
			updateButton(solverTable, $("#removeSolver"));
		});
	}
	
	if(perms.removeBench){
		$("#benchmarks").delegate("tr", "click", function(){
			updateButton(benchTable, $("#removeBench"));
		});
	}

	// Not yet implemented; for future use
//	if(perms.removeJob){
//		$("#jobs").delegate("tr", "click", function(){
//			updateButton(jobTable, $("#removeJob"));
//		});
//	}
	
}

/**
 * Updates the URLs to perform actions on the current space
 * @param id The id of the current space
 */
function updateAddId(id) {	
	$('#addSpace').attr('href', "/starexec/secure/add/space.jsp?sid=" + id);
	$('#uploadBench').attr('href', "/starexec/secure/add/benchmarks.jsp?sid=" + id);
	$('#uploadSolver').attr('href', "/starexec/secure/add/solver.jsp?sid=" + id);
	$('#addJob').attr('href', "/starexec/secure/add/job.jsp?sid=" + id);
}

function toggleTable(sender) {
	$(sender).parent().children('.dataTables_wrapper').slideToggle('fast');	
	
	if($(sender).children('span:last-child').text() == '(+)') {
		$(sender).children('span:last-child').text('(-)');
	} else {
		$(sender).children('span:last-child').text('(+)');
	}
}

/**
 * Hides all buttons that are selection-dependent
 */
function initButtons(){
	$("#removeBench").hide();
	$("#removeSolver").hide();
	$("#removeUser").hide();
	$("#removeJob").hide();
	$('#removeSubspace').hide();
}


/**
 * For a given dataTable, this extracts the id's of the rows that have been
 * selected by the user
 * 
 * @param dataTable the particular dataTable to extract the id's from
 * @returns {Array} list of id values for the selected rows
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
 * This handles the showing and hiding of selection-specific buttons
 *  
 * @param dataTable the dataTable to check for selected items
 * @param button the button to handle
 */
function updateButton(dataTable, button){
	var selectedRows = $(dataTable).children('tbody').children('tr.row_selected');
    var btnTxt = $(button).children('.ui-button-text');
    
    if(selectedRows.length == 0){
    	$(button).fadeOut('fast');
    }
    else if(selectedRows.length >= 2 ){
    	$(button).fadeIn('fast');
    	if($(btnTxt).text()[$(btnTxt).text().length - 1] != "s"){
    		$(btnTxt).text($(btnTxt).text() + "s").append();
    	}
    } else {
    	$(button).fadeIn('fast');
    	if($(btnTxt).text()[$(btnTxt).text().length - 1] == "s"){
    		$(btnTxt).text($(btnTxt).text().substring(0, $(btnTxt).text().length - 1)).append();
    	}
    }
}


/**
 * Updates a table by removing selected rows and updating the table's legend to match the new table size.
 * NOTE: This way of updating a given table is preferable to re-querying the database for the space's details
 * (i.e. calling getCommunityDetails(id)) because:
 * - The fields don't minimize
 * - Doesn't ever desync (sometimes re-querying the database for a space's details didn't show that users had been removed)
 * @param dataTable the dataTable to update
 */
function updateTable(dataTable){
	var rowsToRemove = $(dataTable).children('tbody').children('tr.row_selected');
	var rowsRemaining = $(dataTable).children('tbody').children(':not(tr.row_selected)');
	$(dataTable).parent().parent().children('legend').children('span:first-child').text(rowsRemaining.length);
    $.each(rowsToRemove, function(i, row) {
    	dataTable.fnDeleteRow(row);
    });
}