var leaderTable;
var memberTable;

// When the document is ready to be executed on
$(document).ready(function(){
	// Set the path to the css theme fr the jstree plugin
	 $.jstree._themes = "/starexec/css/jstree/";
	 
	 var id;
	// Initialize the jstree plugin for the community list
	jQuery("#exploreList").jstree({  
		"json_data" : { 
			"ajax" : { 
				"url" : "/starexec/services/communities/all"	// Where we will be getting json data from 				
			} 
		}, 
		"themes" : { 
			"theme" : "default", 					
			"dots" : false, 
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
						"image" : "/starexec/images/jstree/users.png"
					}
				}
			}
		},
		"plugins" : ["types", "themes", "json_data", "ui", "cookies"] ,
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
	
	$("#members").delegate("tr", "click", function(){
		$(this).toggleClass("row_selected");
	});
	
	// Hide the 'remove user' button
	$("#removeUser").parent().parent().fadeOut('fast');
	
	// Handles the removal of user(s) from a space
	$("#removeUser").click(function(){
		var selectedRows = getSelectedRows(memberTable);
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
								showMessage('error', "you can not remove yourself from this space in that way, " +
										"instead use the 'leave' button to leave this community", 5000);
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
			// Retrieve details about space again
			getCommunityDetails(id);
			// Redraw the table
			memberTable.fnDraw();
			// Hide the button
			$("#removeUser").parent().parent().fadeOut("fast");
		}
	});
	
	$('.dataTables_wrapper').hide();
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
	$('#memberField legend').children('span:first-child').text(jsonData.space.users.length);
	memberTable.fnClearTable();	
	$.each(jsonData.space.users, function(i, user) {
		var hiddenUserId = '<input type="hidden" value="' + user.id + '" >';
		var fullName = user.firstName + ' ' + user.lastName;
		var userLink = '<a href="/starexec/secure/details/user.jsp?id=' + user.id + '" target="blank">' + fullName + '<img class="extLink" src="/starexec/images/external.png"/></a>' + hiddenUserId;
		var emailLink = '<a href="mailto:' + user.email + '">' + user.email + '<img class="extLink" src="/starexec/images/external.png"/></a>';				
		memberTable.fnAddData([userLink, user.institution, emailLink]);
	});
	
	// Populate leaders table
	$('#leaderField legend').children('span:first-child').text(jsonData.leaders.length);
	leaderTable.fnClearTable();	
	$.each(jsonData.leaders, function(i, user) {
		var fullName = user.firstName + ' ' + user.lastName;
		var userLink = '<a href="/starexec/secure/details/user.jsp?id=' + user.id + '" target="blank">' + fullName + '<img class="extLink" src="/starexec/images/external.png"/></a>';
		var emailLink = '<a href="mailto:' + user.email + '">' + user.email + '<img class="extLink" src="/starexec/images/external.png"/></a>';				
		leaderTable.fnAddData([userLink, user.institution, emailLink]);
	});

	// Show/hide the websites panel based on if there are websites or not
	if(jsonData.websites.length < 1) {
		$('#webDiv').fadeOut('fast');
	} else {
		$('#webDiv').fadeIn('fast');
	}
	
	// Populate website list
	$('#websiteField legend').children('span:first-child').text(jsonData.websites.length);
	$('#websites').html('');	
	$.each(jsonData.websites, function(i, site) {		
		var link = '<a href="' + site.url + '" target="blank">' + site.name+ '<img class="extLink" src="/starexec/images/external.png"/></a>';					
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
		$("#members").delegate("tr", "click", function(){
			updateButton(memberTable, $("#removeUser"));
		});
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
	$('#editComm').attr('href', "/starexec/secure/community/edit.jsp?cid=" + id);
	$("#leaveComm").click(function(){
		if(window.confirm("are you sure you want to leave this community?")){
			leaveCommunity(id);
		}
	});
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
 * Sends an AJAX request for a user to leave a given community
 * 
 * @param id the id of the community the user wants to leave
 */
function leaveCommunity(id){
	$.post(
			"/starexec/services/leave/space/" + id,
			function(returnCode) {
				switch (returnCode) {
					case 0:
						// Repopulate data on the page to indicate to the user
						// that they no longer are a member of that community
						getCommunityDetails(id);
						break;
					default:
						showMessage('error', "you are not a member of this community", 5000);
						break;
				}
			},
			"json"
	).error(function(){
		alert('Session expired');
		window.location.reload(true);
	});
	
	// Redraw the two tables to prevent the case where
	// a member was removed but the tables weren't updated
	leaderTable.fnDraw();
	memberTable.fnDraw();
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

