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
    }).on( "click", "a", function (event, data) { event.preventDefault(); });	// This just disable's links in the node title
	
	memberTable = $('#members').dataTable( {
        "sDom": 'rt<"bottom"flpi><"clear">'
    });
	
	leaderTable = $('#leaders').dataTable( {
        "sDom": 'rt<"bottom"flpi><"clear">'
    });	
	
	$("#members").on( "click", "tr", function(){
		$(this).toggleClass("row_selected");
	});
	
	// Make leaders and members expandable
	$('.expd').parent().expandable(true);
	
	// Hide all buttons initially
	$("#joinComm").fadeOut('fast');
	$("#leaveComm").fadeOut('fast');
	$("#editComm").fadeOut('fast');
	$("#downloadPostProcessors").fadeOut('fast');
	$("#downloadBenchProcessors").fadeOut('fast');
	$("#downloadPreProcessors").fadeOut('fast');
	
	
	$('#communityOverviewUpdate').button( {
		icons: {
		    primary: "ui-icon-arrowrefresh-1-e"
			
    }});

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
	$("#downloadPreProcessors").button({
		icons: {
			secondary: "ui-icon-arrowthick-1-s"
		}
	});
	
	initDialogs();
	$("#communityOverviewUpdate").click(function() {
	  	updateCommunityOverviewGraph();
	});
	$("#leaveComm").click(function(){
		$('#dialog-confirm-leave-txt').text('are you sure you want to leave ' + commName + '? This will remove you from every space in the communiity. Your primitives will not be affected.');
			
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
	$("#downloadPreProcessors").click(function(){
		downloadProcs(id, "pre");
	});
});

/**
 * refreshes graph in community page
 */
function updateCommunityOverviewGraph() {

	$.post(
			starexecRoot+"services/secure/explore/testgraph",
			function(returnCode) {
				s=parseReturnCode(returnCode);
				if (s) {
				    jsonObject=$.parseJSON(returnCode);
				    src=jsonObject.src;
				    $("#communityOverview").attr("src",src);
				} else {
					$("#communityOverview").attr("src",starexecRoot+"/images/noDisplayGraph.png");

				}
			},
			"text"
	);
}

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
		var userLink = '<a href="'+starexecRoot+'secure/details/user.jsp?id=' + user.id + '" target="blank">' + fullName + '<img class="extLink" src="'+starexecRoot+'images/external.png"/></a>' + hiddenUserId;
		var emailLink = '<a href="mailto:' + user.email + '">' + user.email + '<img class="extLink" src="'+starexecRoot+'images/external.png"/></a>';			
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
	/*if(jsonData.websites.length < 1) {
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
	});*/
	
	// Check the new permissions for the loaded space
	checkPermissions(jsonData.perm,jsonData.isMember);	
	
	// Done loading, hide the loader
	$('#loader').hide();	
}

/**
 * Checks the permissions for the current community and hides/shows buttons based on
 * the user's permissions
 * @param perms The JSON permission object representing permissions for the current space
 */
function checkPermissions(perms,isMember) {	
	//we can have permissions even if we are not a member, if the community is public
	if(perms == null) {
		$('#downloadPostProcessors').fadeOut('fast');
		$('#downloadBenchProcessors').fadeOut('fast');
		$('#downloadPreProcessors').fadeOut('fast');
		return;
	} else {
		$('#downloadPostProcessors').fadeIn('fast');
		$('#downloadBenchProcessors').fadeIn('fast');
		$('#downloadPreProcessors').fadeIn('fast');
	}
	if (!isMember) {
		$('#joinComm').fadeIn('fast');
		$('#leaveComm').fadeOut('fast');
	} else {
		$('#joinComm').fadeOut('fast');
		$('#leaveComm').fadeIn('fast');
	}
	if(perms.isLeader) {
		$('#editComm').fadeIn('fast');
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
				s=parseReturnCode(returnCode);
				if (s) {
					getCommunityDetails(id);

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
