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
	
	//TODO : abstract space chain
	usingSpaceChain=(getSpaceChain().length>1);

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
 * jstree utility
 * space chain (in spaces.js)
 **/

function openSpace(curSp,childId) {
	$("#exploreList").jstree("open_node", "#" + curSp, function() {
		$.jstree._focused().select_node("#" + childId, true);	
	});	
}
/**
 *utility function (abstract space chain)
 *in spaces.js
 **/
function getSpaceChain(){
	chain=new Array();
	spaces=$("#spaceChain").attr("value").split(",");
	index=0;
	for (i=0;i<spaces.length;i++) {
		if (spaces[i].trim().length>0) {
			chain[index]=spaces[i];
		}
		index=index+1;
	}

	return chain;
}


/**
 *utility function
 * in spaces.js
 **/
function handleSpaceChain(){
	spaceChain=getSpaceChain();
	if (spaceChain.length<2) {
		return;
	}
	p=spaceChain[0];

	spaceChainIndex=1;
	spaceChainInterval=setInterval(function() {
		if (spaceChainIndex>=spaceChain.length) {
			clearInterval(spaceChainInterval);
		}
		if (openDone) {
			openDone=false;
			c=spaceChain[spaceChainIndex];
			openSpace(p,c);
			spaceChainIndex=spaceChainIndex+1;
			p=c;	
		}
		},100);
		
		
	
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
 * TODO : utility function? (used in spaces.js) 
 * @author Tyler Jensen & Todd Elvers & Skylar Stark changes Julio Cervantes
 */
function initSpaceExplorer(){
	// Set the path to the css theme for the jstree plugin
	$.jstree._themes = starexecRoot+"css/jstree/";
	var id;
	// Initialize the jstree plugin for the explorer list
	$("#exploreList").jstree({  
		"json_data" : { 
			"ajax" : { 
				"url" : starexecRoot+"services/space/subspaces",	// Where we will be getting json data from 
				"data" : function (n) {
					return { id : n.attr ? n.attr("id") : -1 }; 	// What the default space id should be
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
						"image" : starexecRoot+"images/jstree/db.png"
						    
						    }
				}
			}
		},
		"ui" : {			
			"select_limit" : 1,			
			"selected_parent_close" : "select_parent"		
		},
		"plugins" : [ "types", "themes", "json_data", "ui"] ,
		"core" : { animation : 200 }
	        }).bind("select_node.jstree", function (event, data) {
			

			// When a node is clicked, get its ID and display the info in the details pane
			id = data.rslt.obj.attr("id");
			console.log('Space explorer node ' + id + ' was clicked');

			getSpaceDetails(id);
			setUpButtons();
			//setURL(id);
			$('#permCheckboxes').hide();
			$('#currentPerms').hide();
		


		    }).bind("loaded.jstree", function(event,data) {
			    handleSpaceChain();
			}).bind("open_node.jstree",function(event,data) {
				
				openDone=true;
			    }).on("click", "a",  function (event, data) { event.preventDefault();  });// This just disable's links in the node title

	log('Space explorer node list initialized');
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
	console.log("selected space: " + idOfSelectedSpace);

	// If we can't find the id of the space selected from the DOM, get it from the cookie instead
	if(idOfSelectedSpace == null || idOfSelectedSpace == undefined){
		idOfSelectedSpace = $.cookie("jstree_select");
		console.log("cookies!  " + idOfSelectedSpace);
		// If we also can't find the cookie, then just set the space selected to be the root space
		if(idOfSelectedSpace == null || idOfSelectedSpace == undefined){
		    console.log("no cookies");
		    $('#exploreList').jstree('select_node', '#1', false);
			idOfSelectedSpace = 15;
		} else {
			idOfSelectedSpace = idOfSelectedSpace[1];
		} 
	}


	// Request the next page of primitives from the server via AJAX
	$.post(  
			sSource + idOfSelectedSpace + "/" + tableName + "/pagination",
			aoData,
			function(nextDataTablePage){
				switch(nextDataTablePage){
				case 1:
					showMessage('error', "failed to get the next page of results; please try again", 5000);
					break;
				case 2:		
					// This error is a nuisance and the fieldsets are already hidden on spaces where the user lacks permissions
//					showMessage('error', "you do not have sufficient permissions to view primitives in this space", 5000);
					break;
				default:	// Have to use the default case since this process returns JSON objects to the client

					// Update the number displayed in this DataTable's fieldset
					updateFieldsetCount(tableName, nextDataTablePage.iTotalRecords);
				
				// Replace the current page with the newly received page
				fnCallback(nextDataTablePage);
				
				

				break;
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
	       switch (returnCode) {
	       case 0:
		   showMessage('success', "user's permission were successfuly updated", 5000);
		   
		   //TODO : inefficient since I should already have all information I need
		   getPermissionDetails(lastSelectedUserId,spaceId);
		   //window.location = starexecRoot+'secure/admin/permissions.jsp?id=' + userId;
		   break;
	       case 1:
		   showMessage('error', "space details were not updated; please try again", 5000);
		   break;
	       case 2:
		   showMessage('error', "only a leader of this space can modify its details", 5000);
		   break;
	       case 3:
		   showMessage('error',"you must first demote a leader before you can change their permissions",5000);
	       case 7:
		   showMessage('error', "names must be unique among subspaces. It is possible a subspace you do not have permission to see shares the same name",5000);
		   break;
	       default:
		   showMessage('error', "invalid parameters", 5000);
		   break;
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
	    $("#dialog-confirm-update-txt").text("how do you want the permission changes to take effect?");
		
	    $("#dialog-confirm-update").dialog({
		    modal: true,
			width: 380,
			height: 165,
			buttons: {
			"change only this space": function(){ changePermissions(false,false)},
			    "change this space's hierarchy" : function(){changePermissions(true,false)},
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
	



