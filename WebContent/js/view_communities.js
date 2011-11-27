var leaderTable;
var memberTable;

// When the document is ready to be executed on
$(document).ready(function(){
	// Set the path to the css theme fr the jstree plugin
	 $.jstree._themes = "/starexec/css/jstree/";
	 
	 // Initialize the jstree plugin for the community list
	jQuery("#commList").jstree({  
		"json_data" : { 
			"ajax" : { 
				"url" : "/starexec/services/communities/all"	// Where we will be getting json data from 				
			} 
		}, 
		"themes" : { 
			"theme" : "default", 					
			"dots" : false, 
			"icons" : false
		},				
		"plugins" : ["themes", "json_data", "ui", "cookies"] ,
		"core" : { animation : 200 }
	}).bind("select_node.jstree", function (event, data) {
		// When a node is clicked, get its ID and display the info in the details pane		
        id = data.rslt.obj.attr("id");
        updateActionId(id);
        getCommunityDetails(id);
    }).delegate("a", "click", function (event, data) { event.preventDefault(); });	// This just disable's links in the node title
	
	memberTable = $('#members').dataTable( {
        "sDom": 'rt<"bottom"flpi><"clear">'
    });
	
	leaderTable = $('#leaders').dataTable( {
        "sDom": 'rt<"bottom"flpi><"clear">'
    });	
});
 
/**
 * Populates the community details panel with information on the given community
 */
function getCommunityDetails(id) {
	$('#loader').show();
	$.get(  
		"/starexec/services/communities/details/" + id,  
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
	$('#commName').fadeOut('fast', function(){
		$('#commName').text(jsonData.space.name).fadeIn('fast');
	});
	$('#commDesc').fadeOut('fast', function(){
		$('#commDesc').text(jsonData.space.description).fadeIn('fast');
	});	
	
	// Populate members table	
	memberTable.fnClearTable();	
	$.each(jsonData.space.users, function(i, user) {
		var fullName = user.firstName + ' ' + user.lastName;
		var userLink = '<a href="/starexec/secure/details/user.jsp?id=' + user.id + '" target="blank">' + fullName + '</a>';
		var emailLink = '<a href="mailto:' + user.email + '">' + user.email + '</a>';				
		memberTable.fnAddData([userLink, user.institution, emailLink]);
	});
	
	// Populate leaders table
	leaderTable.fnClearTable();	
	$.each(jsonData.leaders, function(i, user) {
		var fullName = user.firstName + ' ' + user.lastName;
		var userLink = '<a href="/starexec/secure/details/user.jsp?id=' + user.id + '" target="blank">' + fullName + '</a>';
		var emailLink = '<a href="mailto:' + user.email + '">' + user.email + '</a>';				
		leaderTable.fnAddData([userLink, user.institution, emailLink]);
	});

	// Show/hide the websites panel based on if there are websites or not
	if(jsonData.websites.length < 1) {
		$('#webDiv').fadeOut('fast');
	} else {
		$('#webDiv').fadeIn('fast');
	}
	
	// Populate website list
	$('#websites').html('');
	$.each(jsonData.websites, function(i, site) {		
		var link = '<a href="' + site.url + '" target="blank">' + site.name+ '</a>';					
		$('#websites').append('<li>' + link + '</li>');
	});
	
	// Check the new permissions for the loaded space
	checkPermissions(jsonData.perm);
	
	// Done loading, hide the loader
	$('#loader').hide();
}

/**
 * Checks the permissions for the current community and hides/shows buttons based on
 * the user's permissions
 * @param perms The JSON permission object representing permissions for the current space
 */
function checkPermissions(perms) {	
	if(perms == null) {
		$('#joinComm').parent().parent().fadeIn('fast');
		$('#leaveComm').parent().parent().fadeOut('fast');
		$('#editComm').parent().parent().fadeOut('fast');
		return;
	} else {
		$('#joinComm').parent().parent().fadeOut('fast');
		$('#leaveComm').parent().parent().fadeIn('fast');
	}
	
	if(perms.isLeader) {
		$('#editComm').parent().parent().fadeIn('fast');
	} else {
		$('#editComm').parent().parent().fadeOut('fast');
	}
}

/**
 * Updates the URLs to perform actions on the current community
 * @param id The id of the current community
 */
function updateActionId(id) {	
	$('#joinComm').attr('href', "/starexec/secure/community/join.jsp?cid=" + id);
	$('#leaveComm').attr('href', "/starexec/secure/community/leave.jsp?cid=" + id);
	$('#editComm').attr('href', "/starexec/secure/community/edit.jsp?cid=" + id);
}