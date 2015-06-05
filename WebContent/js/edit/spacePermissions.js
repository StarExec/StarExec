/** Global Variables */
var userTable;
var addUsersTable;
var spaceId = 1;			// id of the current space
var spaceName;			// name of the current space
var currentUserId;

// utility - space chain (spaces.js)
var spaceChain;
var spaceChainIndex=0;
var spaceChainInterval;
var openDone = true;
var usingSpaceChain = false;


var curIsLeader = false;
var curIsAdmin = false;

var communityIdList = null;
var currentSpacePublic=false; // is the space we are currently in public (true) or private (false)

//logger allows me to enable or disable console.log() lines
var logger = function()
{
    var oldConsoleLog = null;
    var pub = {};

    pub.enableLogger =  function enableLogger() 
                        {
                            if(oldConsoleLog == null)
                                return;

                            window['console']['log'] = oldConsoleLog;
                        };

    pub.disableLogger = function disableLogger()
                        {
                            oldConsoleLog = console.log;
                            window['console']['log'] = function() {};
                        };

    return pub;
}();


$(document).ready(function(){

	//logger.disableLogger();
	console.log("spacePermissions log start");
	$("#dialog-confirm-change").hide();

	currentUserId=parseInt($("#userId").attr("value"));
	curIsAdmin = isAdmin();
	lastSelectedUserId = null;
	$("#exploreSpaces").button( {
		icons: {
			primary: "ui-icon-arrowthick-1-w"
	}
	});
	usingSpaceChain=(getSpaceChain("#spaceChain").length>1);

	communityIdList=getCommunityIdList();
	
	 // Build left-hand side of page (space explorer)
	 initSpaceExplorer();

	 // Build right-hand side of page (space details)
	 initSpaceDetails();

	console.log("this is a test: " + spaceId);


});



function stringToBoolean(s){
    switch(s){
    case "true" : return true;
    case "false" : return false;
    default : return false;
    }
}

function isAdmin(){
    admin = $("#isAdmin").attr("value");

    return stringToBoolean(admin);
}
/**
 * utility function
 * community
 **/
function getCommunityIdList(){
    list = new Array();
    spaces = $("#communityIdList").attr("value").split(",");
    for(i=0;i < spaces.length; i++){
		if(spaces[i].trim().length > 0){
			list[i] = spaces[i];
		}
    }
    return list

}



/**
 * utility function (also in spaces.js)
 *
 **/
function setURL(id) {
	current=window.location.pathname;
	newURL=current.substring(0,current.indexOf("?"));
	window.history.replaceState("object or string", "",newURL+"?id="+id);
}

/**
 * utility function
 * returns an object representing the query string in url
 **/
function getQueryString(){
  var query_string = {};
  var query = window.location.search.substring(1);
  var vars = query.split("&");

  for (var i=0;i<vars.length;i++) {
    var pair = vars[i].split("=");
    	// If first entry with this name
    if (typeof query_string[pair[0]] === "undefined") {
      query_string[pair[0]] = pair[1];
    	// If second entry with this name
    } else if (typeof query_string[pair[0]] === "string") {
      var arr = [ query_string[pair[0]], pair[1] ];
      query_string[pair[0]] = arr;
    	// If third or later entry with this name
    } else {
      query_string[pair[0]].push(pair[1]);
    }
  }

  return query_string;


}

/**
 * Sets up the 'space details' that consumes the right-hand side of the page
 */
function initSpaceDetails(){

	// builds the DataTable objects and enables multi-select on them
	initDataTables();

	// Set up jQuery button UI
	initButtonUI();
	
}


/**
 * Basic initialization for jQuery UI buttons (sets style and icons)
 */
function initButtonUI() {
	$('.btnUp').button({
		icons: {
			secondary: "ui-icon-arrowthick-1-n"
		}});

	$('.resetButton').button({
		icons: {
			secondary: "ui-icon-closethick"
		}});

	$('.backButton').button({
		icons: {
		    primary: "ui-icon-arrowthick-1-w"
		}});
	$("#makePublic").button();
}



/**
 * Creates the space explorer tree for the left-hand side of the page, also
 * creates tooltips for the space explorer, .expd class, and userTable (if applicable)
 * @author Tyler Jensen & Todd Elvers & Skylar Stark changes Julio Cervantes
 */
function initSpaceExplorer(){
	// Set the path to the css theme for the jstreeplugin
    jsTree = makeSpaceTree("#exploreList", !usingSpaceChain);
	jsTree.bind("select_node.jstree", function (event, data) {
			

			// When a node is clicked, get its ID and display the info in the details pane
			id = data.rslt.obj.attr("id");

			getSpaceDetails(id);
			setUpButtons();
			$('#permCheckboxes').hide();
			$('#currentPerms').hide();
		


		    }).bind("loaded.jstree", function(event,data) {
			    handleSpaceChain("#spaceChain");
			}).bind("open_node.jstree",function(event,data) {
				openDone=true;
			});
	$('#exploreList').click(function() {
		
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
	log("Populating table " + tableName);
	
	// Extract the id of the currently selected space from the DOM
	var idOfSelectedSpace = getIdOfSelectedSpace();

	// If we can't find the id of the space selected from the DOM, just do not populate the table
	if(idOfSelectedSpace == null || typeof idOfSelectedSpace == 'undefined'){
		return;
	}
	
	fillTableWithPaginatedPrimitives(tableName, 'users', idOfSelectedSpace, sSource, aoData, fnCallback);
}

function addUsersPaginationHandler(sSource, aoData, fnCallback) {
	var tableName = $(this).attr('id');
	log("Populating table " + tableName);

	// Extract the id of the currently selected space from the DOM
	var idOfSelectedSpace = getIdOfSelectedSpace();

	// If we can't find the id of the space selected from the DOM, just do not populate the table
	if(idOfSelectedSpace == null || typeof idOfSelectedSpace == 'undefined'){
		return;
	}

	$.get(starexecRoot + 'services/space/community/' + idOfSelectedSpace, function(communityIdOfSelectedSpace) {
		fillTableWithPaginatedPrimitives(tableName, 'users', communityIdOfSelectedSpace, sSource, aoData, fnCallback);
	});
}

/**
 * Gets paginated primitives from the server and fills the table with it. Called from pagination handlers.
 * @param tableName The name of the table being filled.
 * @param primitiveType The type of primitive the table holds ('users', 'benchmarks', etc)
 * @param spaceId The space id of the space to get the primitives from.
 * @param sSource the "sAjaxSource" of the calling table
 * @param aoData the parameters of the DataTable object to send to the server
 * @param fnCallback the function that actually maps the returned page to the DataTable object
 */
function fillTableWithPaginatedPrimitives(tableName, primitiveType, spaceId, sSource, aoData, fnCallback) {
	$.post(  
			sSource + spaceId + "/" + primitiveType + "/pagination",
			aoData,
			function(nextDataTablePage){
				s=parseReturnCode(nextDataTablePage);
				if (s) {
					// Update the number displayed in this DataTable's fieldset
					updateFieldsetCount(tableName, nextDataTablePage.iTotalRecords, 'user');
					
					// Replace the current page with the newly received page
					fnCallback(nextDataTablePage);
				}
			},  
			"json"
	).error(function(){
		//showMessage('error',"Internal error populating table",5000); Seems to show up on redirects
	});
}

function getIdOfSelectedSpace() {
	return $('#exploreList').find('.jstree-clicked').parent().attr("id");
}

/**
 * Helper function for the pagination handlers; since the proper fieldset to update
 * cannot be reliably found via jQuery DOM navigation from pagination handlers,
 * this method providemanually updates the appropriate fieldset to the new value
 * 
 * @param tableName the name of the table whose fieldset we want to update (not in jQuery id format)
 * @param value the new value to update the fieldset with
 * @param primType the type of primitive the table holds
 * @author Todd Elvers
 */
function updateFieldsetCount(tableName, value, primType){
	switch(primType[0]){
	case 'j':
		$('#jobExpd').children('span:first-child').text(value);
		break;
	case 'u':
		// Base selector on table's legend as well as userExpd class since there are
		// multiple elements with userExpd class.
		var legendSelector = '#'+tableName+'Legend';
		$(legendSelector+'.userExpd').children('span:first-child').text(value);
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
 * Initializes the DataTable objects and adds multi-select to them
 */
function initDataTables(){
	
	// Extend the DataTables api and add our custom features
	extendDataTableFunctions();

	// Setup the DataTable objects
	userTable = $('#users').dataTable( {
		"sDom"			: 'rt<"bottom"flpi><"clear">',
		"iDisplayStart"	: 0,
		"iDisplayLength": defaultPageSize,
		"bServerSide"	: true,
		"sAjaxSource"	: starexecRoot+"services/space/",
		"sServerMethod" : 'POST',
		"fnServerData"	: fnPaginationHandler
	});


	addUsersTable = $('#addUsers').dataTable({
		"sDom"			: 'rt<"bottom"flpi><"clear">',
		"iDisplayStart"	: 0,
		"iDisplayLength": defaultPageSize,
		"bServerSide"	: true,
		"sAjaxSource"	: starexecRoot+"services/space/",
		"sServerMethod" : 'POST',
		"fnServerData"	: addUsersPaginationHandler
	});

	var tables=["#users", "#addUsers"];

	function unselectAll(except) {
		var tables=["#users"];
		for (x=0;x<tables.length;x++) {

			if (except==tables[x]) {
				continue;
			}
			$(tables[x]).find("tr").removeClass("row_selected");
		}
	}
	
	
	for (x=0;x<tables.length;x++) {
		$(tables[x]).on("mousedown", "tr", function(){
			unselectAll();
			$(this).toggleClass("row_selected");
		});
	}
	
	//setup user click event
	$('#users tbody').on("mousedown", "tr", function(){
		var uid = $(($(this).find(":input"))[0]).attr('value');
		var sid = spaceId;
		lastSelectedUserId = uid;
		getPermissionDetails(uid,sid);
	});
	
	//Move select all/none buttons to the footer of the Table
	$('#userField div.selectWrap').detach().prependTo('#userField div.bottom');
	$('#addUsersField div.selectWrap').detach().prependTo('#addUsersField div.bottom');

	
	//Hook up select all/ none buttons
	
	$('.selectAllUsers').click(function () {
		$(this).parents('.dataTables_wrapper').find('tbody>tr').addClass('row_selected');
	});
	$('.unselectAllUsers').click(function() {
		$(this).parents('.dataTables_wrapper').find('tbody>tr').removeClass('row_selected');
	});
	

	// Set the DataTable filters to only query the server when the user finishes typing
	userTable.fnFilterOnDoneTyping();
	addUsersTable.fnFilterOnDoneTyping();

	log('all datatables initialized');
}

/**
 * Adds fnProcessingIndicator and fnFilterOnDoneTyping to dataTables api
 */
function extendDataTableFunctions(){

	// Changes the filter so that it only queries when the user is done typing
	jQuery.fn.dataTableExt.oApi.fnFilterOnDoneTyping = function (oSettings) {
		var _that = this;
		this.each(function (i) {
			$.fn.dataTableExt.iApiIndex = i;
			var anControl = $('input', _that.fnSettings().aanFeatures.f);
			anControl.unbind('keyup').bind('keyup', $.debounce( 400, function (e) {
				$.fn.dataTableExt.iApiIndex = i;
				_that.fnFilter(anControl.val());
			}));
			return this;
		});
		return this;
	};
}



/**
 * Populates the space details panel with the basic information about the space
 * (e.g. the name, description) but does not query for details about primitives 
 */
function getSpaceDetails(id) {
	$('#loader').show();
	$.post(  
			starexecRoot+"services/space/" + id,  
			function(data){ 
				log('AJAX response received for details of space ' + id);
				populateSpaceDetails(data, id);			
			},  
			"json"
	).error(function(){
		showMessage('error',"Internal error getting space details",5000);
	});
}

function getPermissionDetails(user_id, space_id) {	
	$.get(  
		starexecRoot+"services/permissions/details/" + user_id + "/" + space_id,  
		function(data){  			
		    populatePermissionDetails(data, user_id);			
		},  
		"json"
	).error(function(){
		showMessage('error',"Internal error getting selectd user's permission details",5000);
	});
}




/**
 * Populates the space details of the currently selected space and queries
 * for the primitives of any fieldsets that are expanded
 * TODO : basically the same as in space.js
 * @param jsonData the basic information about the currently selected space
 */
function populateSpaceDetails(jsonData, id) {
	// If the space is null, the user can see the space but is not a member
	if(jsonData.space == null) {
		// Go ahead and show the space's name
		$('.spaceName').fadeOut('fast', function(){
			$('.spaceName').text($('.jstree-clicked').text()).fadeIn('fast');
		});

		// Show a message why they can't see the space's details
		$('#spaceDesc').fadeOut('fast', function(){
			$('#spaceDesc').text('you cannot view this space\'s details since you are not a member. you can see this space exists because you are a member of one of its descendants.').fadeIn('fast');
		});		
		$('#spaceID').fadeOut('fast');
		// Hide all the info table fieldsets
		$('#detailPanel fieldset').fadeOut('fast');		
		$('#loader').hide();

		// Stop executing the rest of this function
		return;
	} else {
		// Or else the user can see the space, make sure the info table fieldsets are visible
		$('#userField').show();
		$('#permissionActions').show();

	}

	// Update the selected space id
	spaceId = jsonData.space.id;
	spaceName = jsonData.space.name;
	//if not root
	if(spaceId != "1"){
	    curIsLeader = jsonData.perm.isLeader
	}

	// Populate space defaults
	$('.spaceName').fadeOut('fast', function(){
		$('.spaceName').text(jsonData.space.name).fadeIn('fast');
	});
	$('#spaceLeader').fadeOut('fast', function(){
		if(curIsLeader && (spaceId != "1")){
		    $('#spaceLeader').text("leader of current space").fadeIn('fast');
		}
	    });
	$('#spaceDesc').fadeOut('fast', function(){
		$('#spaceDesc').text(jsonData.space.description).fadeIn('fast');
	});	
	$('#spaceID').fadeOut('fast', function() {
		$('#spaceID').text("id = "+spaceId).fadeIn('fast');
	});
	
	if (jsonData.perm.isLeader) {
		handlePublicButton(id);

    } else {
		$('#makePublic').fadeOut('fast');
    }
	
	
	/*
	 * Issue a redraw to all DataTable objects to force them to requery for
	 * the newly selected space's primitives.  This will effectively clear
	 * all entries in every table, update every table with the current space's
	 * primitives, and update the number displayed in every table's fieldset.
	 */
	userTable.fnDraw();
	addUsersTable.fnDraw();

	// Done loading, hide the loader
	$('#loader').hide();

	log('Client side UI updated with details for ' + spaceName);
}

function getPermissionDetails(user_id, space_id) {	
	$.get(  
		starexecRoot+"services/permissions/details/" + user_id + "/" + space_id,  
		function(data){  			
		    populatePermissionDetails(data, user_id);			
		},  
		"json"
	).error(function(){
		showMessage('error',"Internal error getting selectd user's permission details",5000);
	});
}

function isRoot(space_id){
    return space_id == "1";
}

function isCommunity(space_id){
    return ($.inArray(space_id.toString(),communityIdList) != -1);
}

function canChangePermissions(user_id){
    if(curIsLeader && !isRoot(spaceId) && (user_id != currentUserId)){
    	return true;
    }
    else{
    	return false;
    }
}
function populatePermissionDetails(data, user_id) {
	if (data.perm == null) {
	    showMessage("error","permissions seem to be null",5000);
	} else {
	    $('#permCheckboxes').hide();
	    $('#currentPerms').hide();

	    $('#communityLeaderStatusRow').hide();
	    $('#leaderStatusRow').hide();
	    console.log("current user selected: " + (user_id == currentUserId));

	    var leaderStatus = data.perm.isLeader;
	    
	    if(isCommunity(spaceId)){

			if(canChangePermissions(user_id) && (leaderStatus!=true || curIsAdmin)){
			    $('#permCheckboxes').show();
	
			    if(curIsAdmin){
			    	$('#leaderStatusRow').show();
				
			    }
			    else{
			    	$('#communityLeaderStatusRow').show();
			    }
			} else {
			    $('#currentPerms').show();
	
			}
	    }
	    else{

			if(canChangePermissions(user_id)){
			    $('#permCheckboxes').show();
			    $('#leaderStatusRow').show();

			}
			else{
			    $("#currentPerms").show();

			}
	    }

	    var addSolver = data.perm.addSolver;
	    var addBench = data.perm.addBenchmark;
	    var addUser = data.perm.addUser;
	    var addSpace = data.perm.addSpace;
	    var addJob = data.perm.addJob;
	    var removeSolver = data.perm.removeSolver;
	    var removeBench = data.perm.removeBench;
	    var removeUser = data.perm.removeUser;
	    var removeSpace = data.perm.removeSpace;
	    var removeJob = data.perm.removeJob;
	    


	    checkBoxes("addSolver", addSolver);
	    checkBoxes("addBench", addBench);
	    checkBoxes("addUser", addUser);
	    checkBoxes("addSpace", addSpace);
	    checkBoxes("addJob", addJob);
	    checkBoxes("removeSolver", removeSolver);
	    checkBoxes("removeBench", removeBench);
	    checkBoxes("removeUser", removeUser);
	    checkBoxes("removeJob", removeJob);
	    checkBoxes("removeSpace", removeSpace);


	    if(leaderStatus == true){
		$("#uleaderStatus").attr("class","ui-icon ui-icon-check");
		$("#leaderStatus").attr("value","demote");
		$("#communityLeaderStatus").attr("class","ui-icon ui-icon-check");
	    }
	    else{
		$("#uleaderStatus").attr("class","ui-icon ui-icon-close");
		$("#leaderStatus").attr("value","promote");
		$("#communityLeaderStatus").attr("class","ui-icon ui-icon-close");
	    }
	}
	
}

function isRoot(space_id){
    return space_id == "1";
}

function isCommunity(space_id){
    return ($.inArray(space_id.toString(),communityIdList) != -1);
}

function canChangePermissions(user_id){
    if(curIsLeader && !isRoot(spaceId) && (user_id != currentUserId)){
	return true;
    }
    else{
	return false;
    }
}

function checkBoxes(name, value) {



	if (value == true) {
	    $("#u" + name).attr('class','ui-icon ui-icon-check');
	    $("#" + name).attr('checked', 'checked');
	} else {
	    $("#u" + name).attr('class', 'ui-icon ui-icon-close');
	    $("#" + name).removeAttr('checked');

	}
    
}


/**
 *helper function for changePermissions
 *
 **/
function makeDemoteData(){
    var data =
	{		addBench	: true,
			addJob		: true,
			addSolver	: true,
			addSpace	: true,
			addUser		: true,
			removeBench	: true,
			removeJob	: true,
			removeSolver    : true,
			removeSpace	: true,
			removeUser	: true,
			isLeader        : false
	};
    return data;
}

function makePromoteData(){
    var data =
	{		addBench	: true,
			addJob		: true,
			addSolver	: true,
			addSpace	: true,
			addUser		: true,
			removeBench	: true,
			removeJob	: true,
			removeSolver    : true,
			removeSpace	: true,
			removeUser	: true,
			isLeader        : true
	};
    return data;
}

/**
 * 
 * @param hier : boolean - whether or not to behave hierarchically
 **/
function changePermissions(hier,changingLeadership){

    var url = starexecRoot+"services/space/" + spaceId + "/edit/perm/";
    if(hier){
	url = url + "hier/";
    }
    url = url + lastSelectedUserId;

    $('#dialog-confirm-update').dialog('close');

    var data = null;

    if(!changingLeadership){
	data = 
	    {		addBench	: $("#addBench").is(':checked'),
			addJob		: $("#addJob").is(':checked'),
			addSolver	: $("#addSolver").is(':checked'),
			addSpace	: $("#addSpace").is(':checked'),
			addUser		: $("#addUser").is(':checked'),
			removeBench	: $("#removeBench").is(':checked'),
			removeJob	: $("#removeJob").is(':checked'),
			removeSolver: $("#removeSolver").is(':checked'),
			removeSpace	: $("#removeSpace").is(':checked'),
			removeUser	: $("#removeUser").is(':checked'),
			//isLeader 	: $("#leaderStatus").is(':checked'),
			isLeader        : ($("#leaderStatus").attr("value") == "demote")
	    };
    }
    else{
	if($("#leaderStatus").attr("value") == "demote"){
	    data = makeDemoteData();
	}
	else{
	    data = makePromoteData();
	}
    }
    // Pass data to server via AJAX
    $.post(
	   url,
	   data,
	   function(returnCode) {
		   s=parseReturnCode(returnCode);
		   if (s) {
			   getPermissionDetails(lastSelectedUserId,spaceId);
		   }
	   },
	   "json"
	   );

}

/**
 * sets up buttons in space permissions page
 * @author Julio Cervantes
 */
function setUpButtons() {
    $('#dialog-confirm-update').hide();

    $("#savePermChanges").unbind("click");
    $("#savePermChanges").click(function(){
	    $("#dialog-confirm-update-txt").text("do you want the changes to be hierarchical?");
		
	    $("#dialog-confirm-update").dialog({
		    modal: true,
			width: 380,
			height: 165,
			buttons: {
			"yes" : function(){changePermissions(true,false)},
			    "no" : function(){ changePermissions(false,false)},
			    "cancel": function() {
				
				$(this).dialog("close");
			    }
		    }
		});
	});
	

    $("#resetPermChanges").unbind("click");
    $("#resetPermChanges").click(function(e) {
	    
	    
	    if(lastSelectedUserId == null){
	    	showMessage('error','No user selected',5000);
	    }
	    else{
	    	getPermissionDetails(lastSelectedUserId,spaceId);
	    }
	    

	});
    /**
    $("#exploreSpaces").unbind("click");
    $("#exploreSpaces").click(function(e) {
	
	});
    **/

    $("#leaderStatus").unbind('click');
    $("#leaderStatus").click(function(e) {
	    $("#dialog-confirm-update-txt").text("how should the leadership change take effect?");
		
	    $("#dialog-confirm-update").dialog({
		    modal: true,
			width: 380,
			height: 165,
			buttons: {
			"change only this space": function(){ changePermissions(false,true)},
				"change this space's hierarchy" : function(){changePermissions(true,true)},
			    "cancel": function() {
				
				$(this).dialog("close");
			    }
		    }
		});
	});
	
	$('#addUsersButton').unbind('click');
	$('#addUsersButton').click(function(e) {
		var selectedUsersIds = getSelectedRows(addUsersTable);
		var selectedSpace = spaceId;
		if (selectedUsersIds.length > 0) {
			$('#dialog-confirm-update-txt').text('do you want to copy the selected users to '
				+ spaceName + ' and all of its subspaces or just to ' + spaceName + '?');
			$('#dialog-confirm-update').dialog({
				modal: true,
				width: 380,
				height: 165,
				buttons: {
					'space hierarchy': function() {
						// If the user actually confirms, close the dialog right away
						$('#dialog-confirm-update').dialog('close');
						// Get the community id of the selected space and make the request to the server	
						$.get(starexecRoot + 'services/space/community/' + selectedSpace, function(communityIdOfSelectedSpace) {
							doUserCopyPost(selectedUsersIds,selectedSpace,communityIdOfSelectedSpace,true,doUserCopyPostCB);
						});
					},
					'space': function(){
						// If the user actually confirms, close the dialog right away
						$('#dialog-confirm-update').dialog('close');
						// Get the community id of the selected space and make the request to the server	
						$.get(starexecRoot + 'services/space/community/' + selectedSpace, function(communityIdOfSelectedSpace) {
							doUserCopyPost(selectedUsersIds,selectedSpace,communityIdOfSelectedSpace,false,doUserCopyPostCB);
						});
					},
					"cancel": function() {
						log('user canceled copy action');
						$(this).dialog("close");
					}
				}		
			});		
		} else {
			$('#dialog-confirm-update-txt').text('select users to add to space.'); 
			$('#dialog-confirm-update').dialog({
				modal: true,
				width: 380,
				height: 165,
				buttons: {
					'ok': function() {
						$(this).dialog("close");
					}
				}		
			});
		}
	});
    
    $("#makePublic").click(function(){
		// Display the confirmation dialog
		$('#dialog-confirm-change-txt').text('do you want to make the single space public or the hierarchy?');
		$('#dialog-confirm-change').dialog({
			modal: true,
			width: 380,
			height: 165,
			buttons: {
				'space': function(){
					$.post(
							starexecRoot+"services/space/changePublic/" + spaceId + "/" + false+"/"+!currentSpacePublic,
							{},
							function(returnCode) {
								s=parseReturnCode(returnCode);
								if (s) {
									window.location.reload(true);
								} else {
									$(this).dialog("close");
								}
							},
							"json"
					);
				},
				'hierarchy': function(){
					$.post(
							starexecRoot+"services/space/changePublic/" + spaceId + "/" + true+"/"+!currentSpacePublic,
							{},
							function(returnCode) {
								s=parseReturnCode(returnCode);
								if (s) {
									window.location.reload(true);
								} else {
									$(this).dialog("close");
								}
							},
							"json"
					);
				},
				"cancel": function() {
					log('user canceled making public action');
					$(this).dialog("close");
				}
			}
		});
	});
}

function doUserCopyPost(ids,destSpace,spaceId,copyToSubspaces, callback){
	$.post(  	    		
		starexecRoot+'services/spaces/' + destSpace + '/add/user',
		{selectedIds : ids, fromSpace : spaceId, copyToSubspaces: copyToSubspaces},	
		function(returnCode) {
			parseReturnCode(returnCode);
		},
		"json"
	).done(function() {
		if (callback) {
			callback();
		}
	}).fail(function(){
		showMessage('error',"Internal error copying users",5000);
	});				
}

/**
 * helper function that redraws the userTable after a copy post.
 */
function doUserCopyPostCB() {
	userTable.fnDraw();
}

function handlePublicButton(id) {
	$('#loader').show();
	$.post(  
			starexecRoot+"services/space/isSpacePublic/" + id,  
			function(returnCode){
				$("#makePublic").show(); //the button may be hidden if the user is coming from another space
				switch(returnCode){
				case 0:

					currentSpacePublic=false;
					setJqueryButtonText("#makePublic","make public");
					break;
				case 1:
					currentSpacePublic=true;
					setJqueryButtonText("#makePublic","make private");
					break;
				}	
			},  
			"json"
	).error(function(){
		showMessage('error',"Internal error getting determining whether space is public",5000);
		$('#makePublic').fadeOut('fast');
	});
}

