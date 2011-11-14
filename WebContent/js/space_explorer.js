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
	
	// Populate job details (fadeout current jobs)
	$('#jobs').fadeOut('fast', function(){
		// Clear existing items in the jobs div
		$('#jobs').html('');	
		
		// For each returned job, create a element and add it to the div
		$.each(jsonData.space.jobs, function(i, job) {
			var jobElement = $('<a></a>').attr({
				id: 'job_' + job.id,
				class: 'job round',
				title: job.description,
				href: '/starexec/secure/details/job.jsp?id=' + job.id,
				target: '_blank'
			});		
			var jobNameElement = $('<p></p>').text(job.name);
			var jobStatElement = $('<p></p>').text(job.status);
			
			$(jobNameElement).appendTo(jobElement);
			$(jobStatElement).appendTo(jobElement);
			$(jobElement).appendTo('#jobs');
		});
		
		// If there are no jobs, put 'none' in the div
		if($(jsonData.space.jobs).length == 0) {
			$('#jobs').html('<p>none</p>');
		}
		
		// Fade in new jobs
		$('#jobs').fadeIn('fast');
	});

	// The following population methods follow the same form as the one commented above
	
	// Populate user details
	$('#users').fadeOut('fast', function() {
		$('#users').html('');			
		$.each(jsonData.space.users, function(i, user) {
			var userElement = $('<a></a>').attr({
				id: 'user_' + user.id,
				class: 'user round',				
				title: user.email,
				href: '/starexec/secure/details/user.jsp?id=' + user.id,
				target: '_blank'
			});		
			var userNameElement = $('<p></p>').text(user.firstName + ' ' + user.lastName);
			var userInstElement = $('<p></p>').text(user.institution);
			
			$(userNameElement).appendTo(userElement);
			$(userInstElement).appendTo(userElement);
			$(userElement).appendTo('#users');
		});
		if($(jsonData.space.users).length == 0) {
			$('#users').html('<p>none</p>');
		}
		$('#users').fadeIn('fast');
	});	
			
	// Populate solver details
	$('#solvers').fadeOut('fast', function(){
		$('#solvers').html('');
		$.each(jsonData.space.solvers, function(i, solver) {
			var solverElement = $('<a></a>').attr({
				id: 'solver_' + solver.id,
				class: 'solver round',
				title: solver.description,
				href: '/starexec/secure/details/solver.jsp?id=' + solver.id,
				target: '_blank'
			});		
			var solverNameElement = $('<p></p>').text(solver.name);
			
			$(solverNameElement).appendTo(solverElement);
			$(solverElement).appendTo('#solvers');
		});
		if($(jsonData.space.solvers).length == 0) {
			$('#solvers').html('<p>none</p>');
		}
		$('#solvers').fadeIn('fast');
	});	
	
	// Populate benchmark details
	$('#benchmarks').fadeOut('fast', function(){
		$('#benchmarks').html('');
		$.each(jsonData.space.benchmarks, function(i, bench) {
			var benchElement = $('<a></a>').attr({
				id: 'bench_' + bench.id,
				class: 'benchmark round',
				title: bench.description,
				href: '/starexec/secure/details/benchmark.jsp?id=' + bench.id,
				target: '_blank'
			});		
			var benchNameElement = $('<p></p>').text(bench.name);
			
			$(benchNameElement).appendTo(benchElement);
			$(benchElement).appendTo('#benchmarks').hide().fadeIn('fast');
		});
		if($(jsonData.space.benchmarks).length == 0) {
			$('#benchmarks').html('<p>none</p>');
		}	
		$('#benchmarks').fadeIn('fast');
	});
			
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