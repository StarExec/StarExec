var userTable;
var benchTable;
var solverTable;
var spaceTable;
var jobTable;

// When the document is ready to be executed on
$(document).ready(function(){
	// Set the path to the css theme fr the jstree plugin
	 $.jstree._themes = "/starexec/css/jstree/";
	 var id;
	 // Initialize the jstree plugin for the explorer list
	jQuery("#spaceList").jstree({  
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
						"image" : "/starexec/images/tree_level.png"
					}
				}
			}
		},
		"plugins" : [ "types", "themes", "json_data", "ui", "cookies"] ,
		"core" : { animation : 200 }
	}).bind("select_node.jstree", function (event, data) {
		// When a node is clicked, get its ID and display the info in the details pane		
        id = data.rslt.obj.attr("id");
        updateAddId(id);
        getSpaceDetails(id);
    }).delegate("a", "click", function (event, data) { event.preventDefault(); });	// This just disable's links in the node title
	
	// Hide all buttons that are selection-dependent
	initButtons();
	
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
	
	// Handles removal of benchmark(s) from a space
	$("#removeBench").click(function(){
		var selectedRows = getSelectedRows(benchTable);
		var remove;
		
		if(selectedRows.length >= 2){
			remove = window.confirm("are you sure you want remove these benchmarks?");
		} else {
			remove = window.confirm("are you sure you want remove this benchmark?");
		}
		
		if(remove){
			for(var i = 0; i < selectedRows.length; i++){
				$.post(  
					"/starexec/services/remove/benchmark/" + id + "/" + selectedRows[i],  
					function(returnCode) {
						switch (returnCode) {
							case 0:
								break;
							default:
								showMessage('error', "only the leader of a community can modify it", 5000);
								break;
						}
					},
					"json"
				).error(function(){
					alert('Session expired');
					window.location.reload(true);
				});
			}
			updateAddId(id);
	        getSpaceDetails(id);
	        fadeOutAll();
		}
	});
	
	// Handles removal of user(s) from a space
	$("#removeUser").click(function(){
		var selectedRows = getSelectedRows(userTable);
		var remove;
		
		if(selectedRows.length >= 2){
			remove = window.confirm("are you sure you want remove these users?");
		} else {
			remove = window.confirm("are you sure you want remove this user?");
		}
		
		if(remove){
			for(var i = 0; i < selectedRows.length; i++){
				$.post(  
					"/starexec/services/remove/user/" + id + "/" + selectedRows[i],  
					function(returnCode) {
						switch (returnCode) {
							case 0:
								break;
							case 4:
								showMessage('error', "you can not remove yourself from a space on this page, " +
										"instead use the 'leave' button on the communities page", 5000);
								break;
							case 5:
								showMessage('error', "you can not remove other leaders of this space", 5000);
								break;
							default:
								showMessage('error', "only the leader of a community can modify it", 5000);
								break;
						}
					},
					"json"
				).error(function(){
					alert('Session expired');
					window.location.reload(true);
				});
			}
			// Retrieve data for this space
			getSpaceDetails(id);
			// Redraw the table
			userTable.fnDraw();
			// Hide the all selection-dependent buttons
			fadeOutAll();
		}
	});
	
	// Handles removal of solver(s) from a space
	$("#removeSolver").click(function(){
		var selectedRows = getSelectedRows(solverTable);
		var remove;
		
		if(selectedRows.length >= 2){
			remove = window.confirm("are you sure you want remove these solvers?");
		} else {
			remove = window.confirm("are you sure you want remove this solver?");
		}
		
		if(remove){
			for(var i = 0; i < selectedRows.length; i++){
				$.post(  
					"/starexec/services/remove/solver/" + id + "/" + selectedRows[i],  
					function(returnCode) {
						switch (returnCode) {
							case 0:
								break;
							default:
								showMessage('error', "only the leader of a community can modify it", 5000);
								break;
						}
					},
					"json"
				).error(function(){
					alert('Session expired');
					window.location.reload(true);
				});
			}
			// Retrieve data for this space
			getSpaceDetails(id);
			// Redraw the table
			solverTable.fnDraw();
			// Hide the all selection-dependent buttons
			fadeOutAll();
		}
	});
	
	// Handles removal of job(s) from a space
	$("#removeJob").click(function(){
		var selectedRows = getSelectedRows(jobTable);
		var remove;
		
		if(selectedRows.length >= 2){
			remove = window.confirm("are you sure you want remove these jobs?");
		} else {
			remove = window.confirm("are you sure you want remove this job?");
		}
		
		if(remove){
			for(var i = 0; i < selectedRows.length; i++){
				$.post(  
					"/starexec/services/remove/job/" + id + "/" + selectedRows[i],  
					function(returnCode) {
						switch (returnCode) {
							case 0:
								break;
							default:
								showMessage('error', "only the leader of a community can modify it", 5000);
								break;
						}
					},
					"json"
				).error(function(){
					alert('Session expired');
					window.location.reload(true);
				});
			}
			// Retrieve data for this space
			getSpaceDetails(id);
			// Redraw the table
			jobTable.fnDraw();
			// Hide the all selection-dependent buttons
			fadeOutAll();
		}
	});
	
	$('.dataTables_wrapper').hide();
});
 
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
		var jobLink = '<a href="/starexec/secure/details/job.jsp?id=' + job.id + '" target="blank">' + job.name + '</a>' + hiddenJobId;		
		jobTable.fnAddData([jobLink, job.status, job.description]);
	});	
	
	// Populate user details	
	$('#userField legend').children('span:first-child').text(jsonData.space.users.length);
	userTable.fnClearTable();		
	$.each(jsonData.space.users, function(i, user) {
		var hiddenUserId = '<input type="hidden" value="'+user.id+'" >';
		var fullName = user.firstName + ' ' + user.lastName;
		var userLink = '<a href="/starexec/secure/details/user.jsp?id=' + user.id + '" target="blank">' + fullName + '</a>' + hiddenUserId;
		var emailLink = '<a href="mailto:' + user.email + '">' + user.email + '</a>';
		userTable.fnAddData([userLink, user.institution, emailLink]);
	});
		
	// Populate solver details
	$('#solverField legend').children('span:first-child').text(jsonData.space.solvers.length);
	solverTable.fnClearTable();
	$.each(jsonData.space.solvers, function(i, solver) {
		var hiddenSolverId = '<input type="hidden" value="' + solver.id + '" >';
		var solverLink = '<a href="/starexec/secure/details/solver.jsp?id=' + solver.id + '" target="blank">' + solver.name + '</a>' + hiddenSolverId;
		solverTable.fnAddData([solverLink, solver.description]);		
	});	
		
	// Populate benchmark details
	$('#benchField legend').children('span:first-child').text(jsonData.space.benchmarks.length);
	benchTable.fnClearTable();
	$.each(jsonData.space.benchmarks, function(i, bench) {
		var hiddenBenchId = '<input type="hidden" value="' + bench.id + '" >';
		var benchLink = '<a href="/starexec/secure/details/benchmark.jsp?id=' + bench.id + '" target="blank">' + bench.name + '</a>' + hiddenBenchId;
		benchTable.fnAddData([benchLink, bench.type.name, bench.description]);		
	});
	
	// Populate subspace details
	$('#spaceField legend').children('span:first-child').text(jsonData.space.subspaces.length);
	spaceTable.fnClearTable();
	$.each(jsonData.space.subspaces, function(i, subspace) {
		var hiddenSubspaceId = '<input type="hidden" value="' + subspace.id + '" >';
		var spaceLink = '<a href="/starexec/secure/details/space.jsp?id=' + subspace.id + '" target="blank">' + subspace.name + '</a>' + hiddenSubspaceId;
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
	if(perms.addSpace) {		
		$('#addSpace').parent().parent().fadeIn('fast');
	} else {
		$('#addSpace').parent().parent().fadeOut('fast');
	}
	
	if(perms.removeSpace) {
		$("#spaces").delegate("tr", "click", function(){
			updateButton(spaceTable, $("#removeSubspace"));
		});
	}
	
	if(perms.addBenchmark) {
		$('#uploadBench').parent().parent().fadeIn('fast');
	} else {
		$('#uploadBench').parent().parent().fadeOut('fast');
	}
	
	if(perms.addSolver) {
		$('#uploadSolver').parent().parent().fadeIn('fast');
	} else {
		$('#uploadSolver').parent().parent().fadeOut('fast');
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
	$("#removeBench").parent().parent().hide();
	$("#removeSolver").parent().parent().hide();
	$("#removeUser").parent().parent().hide();
	$("#removeJob").parent().parent().hide();
	$('#removeSubspace').parent().parent().hide();
}

function fadeOutAll(){
	$("#removeBench").parent().parent().fadeOut('fast');
	$("#removeSolver").parent().parent().fadeOut('fast');
	$("#removeUser").parent().parent().fadeOut('fast');
	$("#removeJob").parent().parent().fadeOut('fast');
	$('#removeSubspace').parent().parent().fadeOut('fast');	
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
    
    if(selectedRows.length == 0){
    	$(button).parent().parent().fadeOut('fast');
    }
    else if(selectedRows.length >= 2 ){
    	$(button).parent().parent().fadeIn('fast');
    	if($(button).text()[$(button).text().length - 1] != "s"){
    		$(button).text($(button).text() + "s").append();
    	}
    } else {
    	$(button).parent().parent().fadeIn('fast');
    	if($(button).text()[$(button).text().length - 1] == "s"){
    		$(button).text($(button).text().substring(0, $(button).text().length - 1)).append();
    	}
    }
}
