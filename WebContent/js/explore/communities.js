var leaderTable;
var memberTable;
var commName; // Current community's name

// When the document is ready to be executed on
$(document).ready(function(){
	// Set the path to the css theme fr the jstree plugin
	 $.jstree._themes = starexecRoot+"css/jstree/";
	 
	 var id = -1;
	 
	// Initialize the jstree plugin for the community list
	jQuery("#exploreList").jstree({  
		"json_data" : { 
			"ajax" : { 
				"url" : starexecRoot+"services/communities/all"	// Where we will be getting json data from 				
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
						"image" : starexecRoot+"images/jstree/users.png"
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
	
	// Make leaders and members expandable
	$('.expd').parent().expandable(true);
	
	// Hide all buttons initially
	$("#removeUser").fadeOut('fast');
	$("#makeLeader").fadeOut('fast');
	$("#joinComm").fadeOut('fast');
	$("#leaveComm").fadeOut('fast');
	$("#editComm").fadeOut('fast');
	$("#downloadPostProcessors").fadeOut('fast');
	$("#downloadBenchProcessors").fadeOut('fast');
	
	
	
	// Handles the removal of user(s) from a space
	$("#removeUser").click(function(){
		var selectedUsers = getSelectedRows(memberTable);
		$('#dialog-confirm-delete-txt').text('are you sure you want to remove the selected user(s) from ' + commName + '?');
		
		// Display the confirmation dialog
		$('#dialog-confirm-delete').dialog({
			modal: true,
			buttons: {
				'yes': function() {
					// If the user actually confirms, close the dialog right away
					$('#dialog-confirm-delete').dialog('close');
					
					$.post(  
						starexecRoot+"services/remove/user/" + id,
						{selectedUsers : selectedUsers},
						function(returnCode) {
							switch (returnCode) {
								case 0:
									// Remove the rows from the page and update the table size in the legend
									updateTable(memberTable);
									$("#removeUser").fadeOut("fast");
									break;
								case 1:
									showMessage('error', "an error occurred while processing your request; please try again", 5000);
								case 2:
									showMessage('error', "insufficient privileges; you must be a community leader to do that", 5000);
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
						showMessage('error',"Internal error removing user",5000);
					});
				},
				"cancel": function() {
					$(this).dialog("close");
				}
			}		
		});				
	});	
	
	$('#makeLeader').click(function(){
		var selectedUsers = getSelectedRows(memberTable);
		
		$.post(  
				starexecRoot+"services/makeLeader/" + id ,
				{selectedUsers : selectedUsers},
				function(returnCode) {
					switch (returnCode) {
						case 0:
							$("#makeLeader").fadeOut("fast");
							break;
						case 1:
							showMessage('error', "an error occurred while processing your request; please try again", 5000);
						case 2:
							showMessage('error', "insufficient privileges; you must be a community leader to do that", 5000);
							break;
						case 3:
							showMessage('error', "you are already a leader", 5000);
							break;
					}
				},
				"json"
			).error(function(){
				showMessage('error',"Internal error making user a leader",5000);
			});
    });
	
	$('#joinComm').button({
		icons: {
			secondary: "ui-icon-plus"
    }});
	
	$('#leaveComm').button({
		icons: {
			secondary: "ui-icon-close"
    }});
	
	$('#editComm').button({
		icons: {
			secondary: "ui-icon-pencil"
    }});
	
	$('#removeUser').button({
		icons: {
			secondary: "ui-icon-minus"
    }});
	
	$('#makeLeader').button({
		icons: {
			secondary: "ui-icon-star"
    }});
	
	$('#downloadPostProcessors').button({
		icons: {
			secondary: "ui-icon-arrowthick-1-s"
		}
	});
	
	$("#downloadBenchProcessors").button({
		icons: {
			secondary: "ui-icon-arrowthick-1-s"
		}
	});
	
	initDialogs();
});

/**
 * Hides all jquery ui dialogs for page startup
 */
function initDialogs() {
	$( "#dialog-confirm-leave" ).hide();
	$( "#dialog-confirm-delete" ).hide();
}

/**
 * Populates the community details panel with information on the given community
 */
function getCommunityDetails(id) {
	$('#loader').show();
	
	$.get(  
		starexecRoot+"services/communities/details/" + id,  
		function(data){  			
			populateDetails(data);			
		},  
		"json"
	).error(function(){
		showMessage('error',"Internal error getting community details",5000);
	});
}

/**
 * Takes in a json  response and populates the details panel with information
 * @param jsonData the json data to populate the details page with
 */
function populateDetails(jsonData) {
	commName = jsonData.space.name;

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
		var userLink = '<a href=starexecRoot+"secure/details/user.jsp?id=' + user.id + '" target="blank">' + fullName + '<img class="extLink" src=starexecRoot+"images/external.png"/></a>' + hiddenUserId;
		var emailLink = '<a href="mailto:' + user.email + '">' + user.email + '<img class="extLink" src=starexecRoot+"images/external.png"/></a>';			
		if (!user.isPublic) {
			memberTable.fnAddData([userLink, user.institution, emailLink]);
			} else {
				$('#memberField legend').children('span:first-child').text(jsonData.space.users.length-1);
			}
		
	});
	
	// Populate leaders table
	$('#leaderField legend').children('span:first-child').text(jsonData.leaders.length);
	leaderTable.fnClearTable();	
	$.each(jsonData.leaders, function(i, user) {
		var fullName = user.firstName + ' ' + user.lastName;
		var userLink = '<a href="'+starexecRoot+'secure/details/user.jsp?id=' + user.id + '" target="blank">' + fullName + '<img class="extLink" src="'+starexecRoot+'images/external.png" /></a>';
		var emailLink = '<a href="mailto:' + user.email + '">' + user.email + '<img class="extLink" src="'+starexecRoot+'images/external.png" /></a>';				
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
		var link = '<a href="' + site.url + '" target="blank">' + site.name+ '<img class="extLink" src=starexecRoot+"images/external.png"/></a>';					
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
		$('#joinComm').fadeIn('fast');
		$('#leaveComm').fadeOut('fast');
		$('#editComm').fadeOut('fast');
		$('#downloadPostProcessors').fadeOut('fast');
		$('#downloadBenchProcessors').fadeOut('fast');
		return;
	} else {
		$('#joinComm').fadeOut('fast');
		$('#leaveComm').fadeIn('fast');
		$('#downloadPostProcessors').fadeIn('fast');
		$('#downloadBenchProcessors').fadeIn('fast');
	}
	
	if(perms.isLeader) {
		$('#editComm').fadeIn('fast');
		$("#members").delegate("tr", "click", function(){
			updateButton(memberTable, $("#removeUser"));
		});
		$("#members").delegate("tr", "click", function(){
			updateButton(memberTable, $("#makeLeader"));
		});
	} else {
		$('#editComm').fadeOut('fast');
	}
}

/**
 * Updates the URLs to perform actions on the current community
 * @param id The id of the current community
 */
function updateActionId(id) {
	$('#joinComm').attr('href', starexecRoot+"secure/add/to_community.jsp?cid=" + id);
	$('#editComm').attr('href', starexecRoot+"secure/edit/community.jsp?cid=" + id);
	$("#leaveComm").click(function(){
		$('#dialog-confirm-leave-txt').text('are you sure you want to leave ' + commName + '?');
			
		// Display the confirmation dialog
		$('#dialog-confirm-leave').dialog({
			modal: true,
			buttons: {
				'yes': function() {
					// If the user actually confirms, close the dialog right away
					$('#dialog-confirm-leave').dialog('close');
					leaveCommunity(id);
				},
				"cancel": function() {
					$(this).dialog("close");
				}
			}		
		});
	});
	
	$("#downloadPostProcessors").click(function(){
		downloadProcs(id, "post");
	});
	$("#downloadBenchProcessors").click(function(){
		downloadProcs(id, "bench");
	});
}

function downloadProcs(id, procClass) {
	createDialog("Processing your download requeste, please wait. This will take some time for large processors.");
	token=Math.floor(Math.random()*1000000000);
	window.location.href=starexecRoot+"secure/download?type=proc&procClass="+procClass+"&id="+id+"&token="+token;
	destroyOnReturn(token);
}

/**
 * Sends an AJAX request for a user to leave a given community
 * @param id the id of the community the user wants to leave
 */
function leaveCommunity(id){
	$.post(
			starexecRoot+"services/leave/space/" + id,
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
		showMessage('error',"Internal error leaving community",5000);
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
