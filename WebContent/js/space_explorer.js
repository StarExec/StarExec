var userTable;
var benchTable;
var solverTable;
var spaceTable;
var jobTable;

// When the document is ready to be executed on
$(document).ready(function(){
	// Set the path to the css theme fr the jstree plugin
	 $.jstree._themes = "/starexec/css/jstree/";
	 
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
		var jobLink = '<a href="/starexec/secure/details/job.jsp?id=' + job.id + '" target="blank">' + job.name + '</a>';		
		jobTable.fnAddData([jobLink, job.status, job.description]);
	});	
	
	// Populate user details	
	$('#userField legend').children('span:first-child').text(jsonData.space.users.length);
	userTable.fnClearTable();		
	$.each(jsonData.space.users, function(i, user) {
		var fullName = user.firstName + ' ' + user.lastName;
		var userLink = '<a href="/starexec/secure/details/user.jsp?id=' + user.id + '" target="blank">' + fullName + '</a>';
		var emailLink = '<a href="mailto:' + user.email + '">' + user.email + '</a>';				
		userTable.fnAddData([userLink, user.institution, emailLink]);
	});
		
	// Populate solver details
	$('#solverField legend').children('span:first-child').text(jsonData.space.solvers.length);
	solverTable.fnClearTable();
	$.each(jsonData.space.solvers, function(i, solver) {
		var solverLink = '<a href="/starexec/secure/details/solver.jsp?id=' + solver.id + '" target="blank">' + solver.name + '</a>';
		solverTable.fnAddData([solverLink, solver.description]);		
	});	
		
	// Populate benchmark details
	$('#benchField legend').children('span:first-child').text(jsonData.space.benchmarks.length);
	benchTable.fnClearTable();
	$.each(jsonData.space.benchmarks, function(i, bench) {
		var benchLink = '<a href="/starexec/secure/details/benchmark.jsp?id=' + bench.id + '" target="blank">' + bench.name + '</a>';
		benchTable.fnAddData([benchLink, bench.type.name, bench.description]);		
	});
	
	// Populate subspace details
	$('#spaceField legend').children('span:first-child').text(jsonData.space.subspaces.length);
	spaceTable.fnClearTable();
	$.each(jsonData.space.subspaces, function(i, subspace) {
		var spaceLink = '<a href="/starexec/secure/details/space.jsp?id=' + subspace.id + '" target="blank">' + subspace.name + '</a>';
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
		$('#removeSpace').parent().parent().fadeIn('fast');
	} else {
		$('#removeSpace').parent().parent().fadeOut('fast');
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
}

/**
 * Updates the URLs to perform actions on the current space
 * @param id The id of the current space
 */
function updateAddId(id) {	
	$('#addSpace').attr('href', "/starexec/secure/add/space.jsp?sid=" + id);
}

function toggleTable(sender) {
	$(sender).parent().children('.dataTables_wrapper').slideToggle('fast');	
	
	if($(sender).children('span:last-child').text() == '(+)') {
		$(sender).children('span:last-child').text('(-)');
	} else {
		$(sender).children('span:last-child').text('(+)');
	}
}