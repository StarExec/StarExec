/** Global Variables */
var userTable;
var benchTable;
var solverTable;
var spaceTable;
var jobTable;
var spaceId; // id of the current space
var spaceName; // name of the current space

$(document).ready(function(){	
	console.log('page ready!');
	
	// initializes custom tooltip styles
	initTooltipStyles();
	
	// builds the space explorer tree
	// creates tooltips for the space explorer, .expd class, and userTable (if applicable)
	initSpaceExplorer();
	
	// builds the DataTable objects and enables multi-select on them
	initDataTables();
	
	// Set up jQuery UI buttons
	initButtonUI();	
	
	// Set up jQuery UI dialog boxes
	initDialogs();
	
	// Enable/disable buttons based on permissions
	initButtons();
	
	$('.dataTables_wrapper').hide();
});

/**
 * Hides all jquery ui dialogs for page startup
 */
function initDialogs() {	
	$( "#dialog-confirm-copy" ).hide();
	$( "#dialog-confirm-delete" ).hide();
	console.log('all confirmation dialogs hidden');
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
	
	console.log('jQuery UI buttons initialized');
}

/**
 * Initializes a table so that elements can be dragged out of it and onto a space name
 * @param table The table to make draggable
 */
function initDraggable(table) {
	// Using jQuery UI, make the first column in each row draggable
	$(table).children('tbody').children('tr').draggable({
		cursorAt: { cursor: 'move', bottom: 10, left: 34},	// Set the cursor to the move icon and make it start in the corner of the helper		
		containment: 'window',	// Allow the element to be dragged anywhere in the browser window
		distance: 20,			// Only trigger a drag when the distanced dragged is > 20 pixels
		scroll: true,			// Scroll with the page as the item is dragged if needed
		helper: getDragClone,	// The method that returns the 'cloned' element that is dragged
		start: onDragStart,		// Method called when the dragging begins
		stop: onDragStop		// Method called when the dragging ends
	});
	
	// Make each space in the explorer list be a droppable target
	$('#exploreList').find('a').droppable( {
		// Function which is called when an item is dropped on a space
	    drop: onSpaceDrop,
	    hoverClass: 'hover',	// Class applied to the space element when something is being dragged over it
	    activeClass: 'active'	// Class applied to the space element when something is being dragged
	});
	
	console.log('table initialized as draggable');
}
 
/**
 * Called when any item is starting to be dragged within the browser
 */
function onDragStart(event, ui) {
	console.log('drag started');
}

/**
 * Called when there is no longer anything being dragged
 */
function onDragStop(event, ui) {
	console.log('drag stopped');
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
	
	console.log(ids.length + ' rows dropped onto ' + destName);
	
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
				console.log('user confirmed copy action');
				
				// If the user actually confirms, close the dialog right away
				$('#dialog-confirm-copy').dialog('close');
				
				// Make the request to the server				
				$.post(  	    		
					'/starexec/services/spaces/' + destSpace + '/add/' + ui.draggable.data('type'), // We use the type to denote copying a user/solver/benchmark/job
					{selectedIds : ids, fromSpace : spaceId},	// Attach the selected items to the data of the post along with the space it's coming from
					function(returnCode) {
						console.log('AJAX response recieved with code ' + returnCode);
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
				console.log('user canceled copy action');
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
 * Creates the space explorer tree for the left-hand side of the page
 */
function initSpaceExplorer(){
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
        console.log('Space explorer node ' + id + ' was clicked');
        
        updateButtonIds(id);
        getSpaceDetails(id);
        
        // Remove all non-permanent tooltips from the page; helps keep
        // the page from getting filled with hundreds of qtip divs
        $(".qtip-nonPermanentLeader").remove();
        $(".qtip-nonPermanentExpd").remove();
        
        // Hide all buttons that are selection-dependent
        hideButtons();
    }).delegate("a", "click", function (event, data) { event.preventDefault(); });// This just disable's links in the node title
	
	console.log('Space explorer node list initialized');
	
	// Create the space tooltips on <a> elements that are children of $(#exploreList)
	createTooltip($('#exploreList'), 'a', 'space');
}


/**
 * Initialize the click listener functions for the buttons of the 'actions' fieldset
 */
function initButtons(){
	// Handles removal of benchmark(s) from a space
	$("#removeBench").click(function(){
		var selectedBenches = getSelectedRows(benchTable);
		$('#dialog-confirm-delete-txt').text('are you sure you want to remove the selected benchmark(s) from ' + spaceName + '?');
		
		// Display the confirmation dialog
		$('#dialog-confirm-delete').dialog({
			modal: true,
			buttons: {
				'yes': function() {
					console.log('user confirmed benchmark deletion');
					// If the user actually confirms, close the dialog right away
					$('#dialog-confirm-delete').dialog('close');
					
					$.post(  
						"/starexec/services/remove/benchmark/" + spaceId,
						{selectedBenches : selectedBenches},
						function(returnCode) {
							console.log('AJAX response received with code ' + returnCode);
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
					console.log('user canceled benchmark deletion');
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
					console.log('user confirmed user deletion');
					// If the user actually confirms, close the dialog right away
					$('#dialog-confirm-delete').dialog('close');
					
					$.post(  
						"/starexec/services/remove/user/" + spaceId,
						{selectedUsers : selectedUsers},
						function(returnCode) {
							console.log('AJAX response received with code ' + returnCode);
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
					console.log('user canceled user deletion');
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
					console.log('user confirmed solver deletion');
					// If the user actually confirms, close the dialog right away
					$('#dialog-confirm-delete').dialog('close');
					
					$.post(  
						"/starexec/services/remove/solver/" + spaceId,
						{selectedSolvers : selectedSolvers},
						function(returnCode) {
							console.log('AJAX response received with code ' + returnCode);
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
					console.log('user canceled solver deletion');
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
					console.log('user confirmed job deletion');
					// If the user actually confirms, close the dialog right away
					$('#dialog-confirm-delete').dialog('close');
					
					$.post(  
						"/starexec/services/remove/job/" + spaceId,
						{selectedJobs : selectedJobs},
						function(returnCode) {
							console.log('AJAX response received with code ' + returnCode);
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
					console.log('user canceled job deletion');
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
					console.log('user confirmed subspace deletion');
					// If the user actually confirms, close the dialog right away
					$('#dialog-confirm-delete').dialog('close');
					
					$.post(  
						"/starexec/services/remove/subspace/" + spaceId,
						{selectedSubspaces : selectedSubspaces},
						function(returnCode) {
							console.log('AJAX response received with code ' + returnCode);
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
					console.log('user canceled subspace deletion');
					$(this).dialog("close");
				}
			}		
		});		
	});
	
	console.log('action buttons initialized');
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
	
	// User permission tooltip setup
	$("#users tbody").delegate("tr", "hover", function(){
		$(this).toggleClass("hovered");
	});
	
	console.log('all datatables initialized');
}

/**
 * Populates the space details panel with information on the given space
 */
function getSpaceDetails(id) {
	$('#loader').show();
	$.post(  
		"/starexec/services/space/" + id,  
		function(data){ 
			console.log('AJAX response received for details of space ' + id);
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
		var stats = jsonData.pairOverview[job.id];
		var status = stats.pendingPairs > 0 ? "incomplete" : "complete";	
		
		function getPairStatHtml(numerator, statType) {
			return "<p class='stat " + statType + "'>" + numerator + '/' + stats.totalPairs + "</p>";
		}
		
		jobTable.fnAddData([jobLink, status, getPairStatHtml(stats.completePairs, 'asc'), getPairStatHtml(stats.pendingPairs, 'desc'), getPairStatHtml(stats.errorPairs, 'desc')]);
		
		// Set up data about this primitive used for drag/drop
		$(jobTable.fnGetNodes(i)).data('id', job.id);
		$(jobTable.fnGetNodes(i)).data('type', 'job');
		$(jobTable.fnGetNodes(i)).data('name', job.name);
	});		
	initDraggable($('#jobs'));
	
	// Colorize the statistics in the job table for completed pairs
	$("#jobs p.stat, #jobs p.asc").heatcolor(
		function() {
			// Return the floating point value of the stat
			return eval($(this).text());
		},
		{ 
			maxval: 1,
			minval: 0,
			colorStyle: 'greentored',
			lightness: 0 
		}
	);
	
	// Colorize the statistics in the job table (for pending and error which use reverse color schemes)
	$("#jobs p.stat, #jobs p.desc").heatcolor(
		function() {
			return eval($(this).text());
		},
		{ 
			maxval: 1,
			minval: 0,
			colorStyle: 'greentored',
			reverseOrder: true,
			lightness: 0 
		}
	);
	
	// Populate user details	
	$('#userField legend').children('span:first-child').text(jsonData.space.users.length);
	userTable.fnClearTable();		
	$.each(jsonData.space.users, function(i, user) {
		var hiddenUserId;
		if(user.id == jsonData.perm.id){
			hiddenUserId = '<input type="hidden" value="'+user.id+'" name="currentUser">';
		} else {
			hiddenUserId = '<input type="hidden" value="'+user.id+'">';
		}
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
	
	console.log('Client side UI updated with details for ' + spaceName);
}


/**
 * Creates either a leader tooltip, a personal tooltip, a space tooltip, or a expd tooltip for a given element
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
	
	console.log('tooltip created of type ' + type);
}

/**
 * Checks the permissions for the current space and hides/shows buttons based on
 * the user's permissions
 * @param perms The JSON permission object representing permissions for the current space
 */
function checkPermissions(perms) {	
	// Check for no permission and hide entire action list if not present
	if(perms == null) {
		console.log('no permissions found, hiding action bar');
		$('#actionList').hide();		
		return;
	} else {
		$('#actionList').show();
	}
	
	if(perms.isLeader){
		// attach leader tooltips to every entry in the userTable 
		createTooltip($('#users tbody'), 'tr', 'leader');		
	} else {
		// otherwise only attach a personal tooltip to the current user's entry in the userTable
		createTooltip($('#users tbody'), 'tr', 'personal');
	}	
	
	if(perms.removeUser){
		$("#users").delegate("tr", "click", function(){
			updateButton(userTable, $("#removeUser"));
		});
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
	
	if(perms.removeBench){
		$("#benchmarks").delegate("tr", "click", function(){
			updateButton(benchTable, $("#removeBench"));
		});		
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
	
	if(perms.removeJob){
		$("#jobs").delegate("tr", "click", function(){
			updateButton(jobTable, $("#removeJob"));
		});		
	}
	
	// Create tooltips for the expd class
	createTooltip($("#userExpd"), null, 'expd', getSinglePermTable('user', perms.addUser, perms.removeUser));
	createTooltip($("#benchExpd"), null, 'expd', getSinglePermTable('bench', perms.addBench, perms.removeBench));
	createTooltip($("#solverExpd"), null, 'expd', getSinglePermTable('solver', perms.addSolver, perms.removeSolver));
	createTooltip($("#spaceExpd"), null, 'expd', getSinglePermTable('space', perms.addSpace, perms.removeSpace));
	createTooltip($("#jobExpd"), null, 'expd', getSinglePermTable('job', perms.addJob, perms.removeJob));
	
	console.log('permissions checked and processed');
}


/**
 * Updates the URLs to perform actions on the current space
 * @param id The id of the current space
 */
function updateButtonIds(id) {	
	$('#addSpace').attr('href', "/starexec/secure/add/space.jsp?sid=" + id);
	$('#uploadBench').attr('href', "/starexec/secure/add/benchmarks.jsp?sid=" + id);
	$('#uploadSolver').attr('href', "/starexec/secure/add/solver.jsp?sid=" + id);
	$('#addJob').attr('href', "/starexec/secure/add/job.jsp?sid=" + id);
	console.log('updated action button space ids to ' + id);
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
function hideButtons(){
	$("#removeBench").hide();
	$("#removeSolver").hide();
	$("#removeUser").hide();
	$("#removeJob").hide();
	$('#removeSubspace').hide();
	console.log('all remove buttons hidden');
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
 * 
 * @param dataTable the dataTable to update
 */
function updateTable(dataTable){
	var rowsToRemove = $(dataTable).children('tbody').children('tr.row_selected');
	var rowsRemaining = $(dataTable).children('tbody').children(':not(tr.row_selected)');
	$(dataTable).parent().parent().children('legend').children('span:first-child').text(rowsRemaining.length);
    $.each(rowsToRemove, function(i, row) {
    	dataTable.fnDeleteRow(row);
    });
    
    console.log('table updated. rows removed: ' + rowsToRemove + ' rows remaining: ' + rowsRemaining);
}

/**
 * Takes in a set of permissions and builds a pretty table to display to the user
 * @param perms The set of permissions to display in a table
 * @returns A jQuery object that is the table to display in the UI for the given permissions
 */
function getPermTable(perms, type) {	
	var permWrap = $('<div>');	// A wrapper for the table and leader info
	var table = $('<table>');	// The table where the permissions are displayed
	$(table).append('<tr><th>property</th><th>add</th><th>remove</th></tr>');
	
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
	} else if (type == 'leader') {		
		// If they're not a leader but the caller has specified to display the 'leader' button...
		
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
		
	console.log('user marked as leader');	
}

/**
 * Gives back HTML for a table containing only one permission
 * @param name The name of the single permission to display
 * @param add The permission's add value
 * @param remove The permission's remove value
 * @returns HTML representing a table to be displayed in a qtip for the table
 */
function getSinglePermTable(name, add, remove) {
	var table = $('<table></table>');
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
 */
function initTooltipStyles(){
	/**
	 * Custom tooltip style for tooltips that will never be deleted
	 * (i.e. tooltips on the spaces in #exploreList)
	 */
	$.fn.qtip.styles.permanentTooltip = {
			background: '#E1E1E1',
			padding: 10,
			height: 144,
			width: 220,
			title : {
				color : '#ae0000'
			}
	};
	
	/**
	 * Custom tooltip styles for tooltips that will be deleted everytime
	 * a new space is selected
	 */
	$.fn.qtip.styles.nonPermanentLeader = {
			background: '#E1E1E1',
			height: 144,
			width: 220,
			padding: 10,
			title : {
				color : '#ae0000'
			}
	};
	
	$.fn.qtip.styles.nonPermanentExpd = {
			background: '#E1E1E1',
			width: 220,
			padding: 10,
			title : {
				color : '#ae0000'
			}
	};
	
	console.log('tooltip styles initialized');
}


/**
 * Returns the desired qTip configuration, with the given message, depending on the inputted type
 * 
 * @param type users/benchmarks/solvers/subspaces
 */
function getTooltipConfig(type, message){
	// Leader tooltips
	if(type[0] == 'l'){
		return {
				content: {
	                text: message,
	                title: {
	                    text: '<center><a>permissions</a></center>'
	                }
	            },
				position: {
					// Place right middle portion of the tooltip
					// to the left middle portion of the row element
	    			corner: {
	    				target: 'leftMiddle',
	    				tooltip: 'rightMiddle'
	                }
				},
				hide :{
					fixed: true
				},
				show: { 
					// Ensures the tooltip is shown the first time it's moused over
					ready: true,
					// Ensures no other tooltip is displayed at the same time
					solo: true,
					// Every mouseover that occurs, after the first mouseover, will have to wait a second
					// before the tooltip is triggered
					delay: 1000,
					event: "mouseover",
					// CSS custom-effect trick to workaround the necessary ready:true flag,
					// which breaks the 'delay' during the first mouseover event
					effect: {
						type: function() {
						    var self = this;
						    self.css('visibility','visible');
						    self.css('opacity',1);
						    if (self.data('ready')) {
								self.show();
								return;
						    }
						    self.css('visibility','hidden').data('ready',1);						    						    
						    var userId = $(this.qtip('api').elements.target).children('td:first').children('input').val();						    
						    // Uses a timer to simulate the first delay=1000 that would occur here
						    // if ready=false
							setTimeout(function(){
									self.show('fast', function(){
										// If element is not being hovered over anymore when the timer ends, don't display it
										if($("#users").children('tbody').children('tr.hovered').children('td:first').children('input').val() == userId){
											self.css('visibility','visible');
					        			} else {
					        				// Fixes bug where elements that were initially hovered, but didn't stay long enough
					        				// to ensure a hover intent, wouldn't display a tooltip during the next hover event
					        				self.hide();
					        			}
										
									});
							}, 1000);
					        
					     }
					  }
				},
				style: {
					// Load custom color scheme
					name: "nonPermanentLeader",
					// Add a tip to the right middle portion of the tooltip
					tip: 'rightMiddle',
			   },			   
			   api:{
				   // Before rendering the tooltip, get the user's permissions for the given space
				   onRender: function(){
						var self = this;						
						var userId = $(this.elements.target).children('td:first').children('input').val();
						var url = '/starexec/services/space/' + spaceId + '/perm/' + userId;
						$.post(
								url,
								function(theResponse){
									console.log('AJAX response for permission tooltip received');
									if(1 == theResponse){
										showMessage('error', "only leaders of a space can edit the permissions of others", 5000);
									} else {
										// Replace current content (current = loader.gif)										
										self.updateContent(getPermTable(theResponse, 'leader'), true);										
									}
									return true;
								}
						).error(function(){
							alert('Session expired');
							window.location.reload(true);
						});	
				   },
				   // If a user modifies a tooltip but does not press the 'save' or 'cancel' button
				   // then this resets the tooltip once it loses focus and fades from view
				   onHide: function(){
					   var self = this;
					   if('p' != $(self.elements.title).text()[0]){
						   self.updateTitle('<center><a>permissions</a></center>');
						   var userId = $(this.elements.target).children('td:first').children('input').val();
						   $.post(
								   '/starexec/services/space/' + spaceId + '/perm/' + userId,
								   function(theResponse){
									   console.log('AJAX response for permission tooltip received');
									   self.updateContent(" ", true); // Have to clear it first to prevent it from appending (qtip bug?)
									   self.updateContent(getPermTable(theResponse, 'leader'), true);  
									   self.updateTitle('<center><a>permissions</a></center>');									   
									   return true;
								   }	
						   ).error(function(){
								alert('Session expired');
								window.location.reload(true);
						   });		
					   }
				   }
		      }
		};
	}
	// Space tooltips
	else if (type[0] == 's'){
		return {
				content: {
	                text: message,
	                
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
					solo: true,
					delay: 1000,
					event: "mouseover",
					effect: {
						type: function() {
						    var self = this;
						    self.css('visibility','visible');
						    self.css('opacity',1);
						    
						    var spaceBeingHovered = $('#exploreList').find('.jstree-hovered').parent().attr("id");
						    
						    if (self.data('ready')) {
								self.show();
								return;
						    }
						    self.css('visibility','hidden').data('ready',1);
							setTimeout(function(){
									self.show('fast', function(){
										if($('#exploreList').find('.jstree-hovered').parent().attr("id") == spaceBeingHovered){
											self.css('visibility','visible');
					        			} else {
					        				self.hide();
					        			}
									});
							}, 1000);
					        
					     }
					  }
				},
				style: {
					name: "permanentTooltip",
					tip: 'bottomLeft',
			   },
			   api:{
				   onRender: function(){
						var self = this;
						var hoveredSpaceId = $('#exploreList').find('.jstree-hovered').parent().attr("id");
						// Destroy the tooltip if the space being hovered is the root space
						if(hoveredSpaceId == 1 || hoveredSpaceId == undefined){
							$('div[qtip="'+self.id+'"]').qtip('destroy');
					    	return;
					    }
						
						// Get the user's permissions in the given space
						$.post(
								'/starexec/services/space/' + hoveredSpaceId,
								function(theResponse){
									console.log('AJAX response for permission tooltip received');
									self.updateContent("");
									self.updateContent(getPermTable(theResponse.perm), true);
									return true;
								}
						).error(function(){
							alert('Session expired');
							window.location.reload(true);
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
				event: "mouseover"
			},
			style: {
				name: 'nonPermanentExpd'
			}
		};
	}
	// Personal tooltips
	else if (type[0] == 'p'){
		return {
				content: {
	                text: message,
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
					solo: true,
					delay: 1000,
					event: "mouseover",
					effect: {
						type: function() {
						    var self = this;
						    self.css('visibility','visible');
						    self.css('opacity',1);
						    if (self.data('ready')) {
								self.show();
								return;
						    }
						    self.css('visibility','hidden').data('ready',1);
						    var userId = $("#users tbody tr").find('td:first input[name="currentUser"]').val();
							setTimeout(function(){
									self.show('fast', function(){
										if($("#users").children('tbody').children('tr.hovered').children('td:first').children('input').val() == userId){
											self.css('visibility','visible');
					        			} else {
					        				self.hide();
					        			}
										
									});
							}, 1000);
					        
					     }
					  }
				},
				style: {
					name: "nonPermanentLeader",
					tip: 'rightMiddle',
			   },
			   api:{
				   onRender: function(){
						var self = this;
						var userId =  $("#users tbody tr").find('td:first input[name="currentUser"]').val();
						if($(this.elements.target).children('td:first').children('input').val() != userId){
							self.destroy();
						} else {
							var url = '/starexec/services/space/' + spaceId + '/perm/' + userId;
							$.post(
									url,
									function(theResponse){
										console.log('AJAX response for permission tooltip received');
										// Replace current content (current = loader.gif)
										self.updateContent(getPermTable(theResponse), true);
										return true;
									}
							).error(function(){
								alert('Session expired');
								window.location.reload(true);
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
		console.log('Updated add permission to ' + newVal + ' for ' + perm);
	} else {		
		if(perm == "space") { permData.removeSpace = newVal; }
		else if(perm == "job") { permData.removeJob = newVal; }
		else if(perm == "user") { permData.removeUser = newVal; }
		else if(perm == "solver") { permData.removeSolver = newVal; }
		else if(perm == "bench") { permData.removeBench = newVal; }
		console.log('Updated remove permission to ' + newVal + ' for ' + perm);
	}
}

/**
 * Handles actions for the 'save' and 'cancel' buttons that appear on leader tooltips whenever
 * a permission is changed
 * 
 * @param obj the title div of the qtip from which this method was called
 * @param save true = save button, false = cancel button
 * @param userId the id of the user to save the new permissions for
 */
function saveChanges(obj, save){
	// Get the currently hovered user
	var userId = $($(obj).parents('.qtip').qtip('api').elements.target).children('td:first').children('input').val();
	console.log('saving permissions for user ' + userId);
	
	// 'SAVE' option
	if(true == save){  
		// Collect the permission images from the tooltip
		var tooltip = $(obj).parents('.qtip').qtip('api');
		
		// Get the stored permissions from the DOM for this table
		var perms = $(obj).parents('.qtip').find('table').data('perms');
		
		// Update database to reflect new permissions
		$.post(
				'/starexec/services/space/' + spaceId + '/edit/perm/' + userId,
				{ addUser: perms.addUser,
				  removeUser: perms.removeUser,
				  addSolver: perms.addSolver,
				  removeSolver: perms.removeSolver,
				  addBench: perms.addBenchmark,
				  removeBench: perms.removeBench,
				  addJob: perms.addJob,
				  removeJob: perms.removeJob,
				  addSpace: perms.addSpace,
				  removeSpace: perms.removeSpace,
				  isLeader: perms.isLeader },
				function(theResponse){
					console.log('AJAX response received for permission edit request with code ' + theResponse);
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
			alert('Session expired');
			window.location.reload(true);
		});	
	} else {  
		console.log('user canceled edit permission action');
		$(obj).parents('.qtip').qtip('api').hide();
	}
}