/** Global Variables */
var userTable;
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
	
	// Extract the id of the currently selected space from the DOM
	var idOfSelectedSpace = $('#exploreList').find('.jstree-clicked').parent().attr("id");

	// If we can't find the id of the space selected from the DOM, just do not populate the table
	if(idOfSelectedSpace == null || typeof idOfSelectedSpace == 'undefined'){
		return;
	}
	

	// Request the next page of primitives from the server via AJAX
	$.post(  
			sSource + idOfSelectedSpace + "/" + tableName + "/pagination",
			aoData,
			function(nextDataTablePage){
				s=parseReturnCode(nextDataTablePage);
				if (s) {
				// Update the number displayed in this DataTable's fieldset
				updateFieldsetCount(tableName, nextDataTablePage.iTotalRecords);
				
				// Replace the current page with the newly received page
				fnCallback(nextDataTablePage);
				}

			},  
			"json"
	).error(function(){
		//showMessage('error',"Internal error populating table",5000); Seems to show up on redirects
	});
}

/**
 * Helper function for fnPaginationHandler; since the proper fieldset to update
 * cannot be reliably found via jQuery DOM navigation from fnPaginationHandler,
 * this method providemanually updates the appropriate fieldset to the new value
 * 
 * @param tableName the name of the table whose fieldset we want to update (not in jQuery id format)
 * @param primCount the new value to update the fieldset with
 * @author Todd Elvers
 */
function updateFieldsetCount(tableName, value){
	switch(tableName[0]){
	case 'j':
		$('#jobExpd').children('span:first-child').text(value);
		break;
	case 'u':
		$('#userExpd').children('span:first-child').text(value);
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

	

	var tables=["#users"];

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
	
	//Move to the footer of the Table
	$('#userField div.selectWrap').detach().prependTo('#userField div.bottom');

	
	//Hook up select all/ none buttons
	
	$('.selectAllUsers').click(function () {
		$(this).parents('.dataTables_wrapper').find('tbody>tr').addClass('row_selected');
	});
	$('.unselectAllUsers').click(function() {
		$(this).parents('.dataTables_wrapper').find('tbody>tr').removeClass('row_selected');
	});
	

	// Set the DataTable filters to only query the server when the user finishes typing
	userTable.fnFilterOnDoneTyping();


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
		$('#spaceName').fadeOut('fast', function(){
			$('#spaceName').text($('.jstree-clicked').text()).fadeIn('fast');
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
	$('#spaceName').fadeOut('fast', function(){
		$('#spaceName').text(jsonData.space.name).fadeIn('fast');
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

	/*
	 * Issue a redraw to all DataTable objects to force them to requery for
	 * the newly selected space's primitives.  This will effectively clear
	 * all entries in every table, update every table with the current space's
	 * primitives, and update the number displayed in every table's fieldset.
	 */
	userTable.fnDraw();

	// Done loading, hide the loader
	$('#loader').hide();

	log('Client side UI updated with details for ' + spaceName);
}


function getPermissionDetails(user_id, space_id) {	
	$.get(  
		starexecRoot+"services/permissions/details/" + user_id + "/" + space_id,  
		function(data){  			
		    populateDetails(data, user_id);			
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
function populateDetails(data, user_id) {
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
		}
		else{
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
			isLeader        : false,
			leaderStatusChange : true
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
			isLeader        : true,
			leaderStatusChange : true
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
			isLeader        : ($("#leaderStatus").attr("value") == "demote"),
			leaderStatusChange : false
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
}
	



