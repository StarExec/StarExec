/** Global Variables */
var userTable;
var addUsersTable;
var spaceId = 1;            // id of the current space
var spaceName;              // name of the current space
var currentUserId;
var lastSelectedUserId = null;

// utility - space chain (spaces.js)
var spaceChain;
var spaceChainIndex = 0;
var spaceChainInterval;
var openDone = true;
var usingSpaceChain = false;

var curIsLeader = false;

var communityIdList;
var currentSpacePublic = false; // is the space we are currently in public (true) or private (false)

$(document).ready(function() {
	log("spacePermissions log start");

	currentUserId = parseInt($("#userId").attr("value"));
	lastSelectedUserId = null;
	$("#exploreSpaces").button({
		icons: {
			primary: "ui-icon-arrowthick-1-w"
		}
	});
	usingSpaceChain = (getSpaceChain("#spaceChain").length > 1);

	communityIdList = getCommunityIdList();

	// Build left-hand side of page (space explorer)
	initSpaceExplorer();

	// Build right-hand side of page (space details)
	initSpaceDetails();
});

function isAdmin() {
	return $("#isAdmin").attr("value") === "true";
}

/**
 * utility function
 * community
 **/
function getCommunityIdList() {
	var list = [];
	var spaces = $("#communityIdList").attr("value").split(",");
	for (i = 0; i < spaces.length; i++) {
		if (spaces[i].trim().length > 0) {
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
	current = window.location.pathname;
	newURL = current.substring(0, current.indexOf("?"));
	window.history.replaceState("object or string", "", newURL + "?id=" + id);
}

/**
 * utility function
 * returns an object representing the query string in url
 **/
function getQueryString() {
	var query_string = {};
	var query = window.location.search.substring(1);
	var vars = query.split("&");

	for (var i = 0; i < vars.length; i++) {
		var pair = vars[i].split("=");
		// If first entry with this name
		if (typeof query_string[pair[0]] === "undefined") {
			query_string[pair[0]] = pair[1];
			// If second entry with this name
		} else if (typeof query_string[pair[0]] === "string") {
			query_string[pair[0]] = [query_string[pair[0]], pair[1]];
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
function initSpaceDetails() {
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
		}
	});

	$('.resetButton').button({
		icons: {
			secondary: "ui-icon-closethick"
		}
	});

	$('.backButton').button({
		icons: {
			primary: "ui-icon-arrowthick-1-w"
		}
	});
	$("#makePublic").button();
}

/**
 * Creates the space explorer tree for the left-hand side of the page, also
 * creates tooltips for the space explorer, .expd class, and userTable (if applicable)
 * @author Tyler Jensen & Todd Elvers & Skylar Stark changes Julio Cervantes
 */
function initSpaceExplorer() {
	// Set the path to the css theme for the jstreeplugin
	jsTree = makeSpaceTree("#exploreList", !usingSpaceChain);
	jsTree.bind("select_node.jstree", function(event, data) {
		// When a node is clicked, get its ID and display the info in the details pane
		id = data.rslt.obj.attr("id");

		getSpaceDetails(id);
		setUpButtons();
		$('#permCheckboxes').hide();
		$('#currentPerms').hide();

	}).bind("loaded.jstree", function(event, data) {
		handleSpaceChain("#spaceChain");
	}).bind("open_node.jstree", function(event, data) {
		openDone = true;
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
	if (idOfSelectedSpace == null || typeof idOfSelectedSpace == 'undefined') {
		return;
	}
	fillTableWithPaginatedPrimitives(tableName,
		'usersTable',
		idOfSelectedSpace,
		sSource,
		aoData,
		fnCallback);
}

function addUsersPaginationHandler(sSource, aoData, fnCallback) {
	var tableName = $(this).attr('id');
	log("Populating table " + tableName);

	// Extract the id of the currently selected space from the DOM
	var idOfSelectedSpace = getIdOfSelectedSpace();

	// If we can't find the id of the space selected from the DOM, just do not populate the table
	if (idOfSelectedSpace == null || typeof idOfSelectedSpace == 'undefined') {
		return;
	}

	$.get(starexecRoot + 'services/space/community/' + idOfSelectedSpace,
		function(communityIdOfSelectedSpace) {
			fillTableWithPaginatedPrimitives(tableName,
				'usersTable',
				communityIdOfSelectedSpace,
				sSource,
				aoData,
				fnCallback);
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
function fillTableWithPaginatedPrimitives(
	tableName, primitiveType, spaceId, sSource, aoData, fnCallback) {
	$.post(
		sSource + spaceId + "/" + primitiveType + "/pagination",
		aoData,
		function(nextDataTablePage) {
			var s = parseReturnCode(nextDataTablePage);
			if (s) {
				// Replace the current page with the newly received page
				fnCallback(nextDataTablePage);
			}
		},
		"json"
	);
}

function getIdOfSelectedSpace() {
	return $('#exploreList').find('.jstree-clicked').parent().attr("id");
}

/**
 * Initializes the DataTable objects and adds multi-select to them
 */
function initDataTables() {
	// Extend the DataTables api and add our custom features
	addFilterOnDoneTyping();

	// Setup the DataTable objects
	userTable = $('#usersTable').dataTable(new window.star.DataTableConfig({
		"bServerSide": true,
		"sAjaxSource": starexecRoot + "services/space/",
		"fnServerData": fnPaginationHandler // included in this file
	}));
	addUsersTable = $('#addUsers').dataTable(new window.star.DataTableConfig({
		"bServerSide": true,
		"sAjaxSource": starexecRoot + "services/space/",
		"fnServerData": addUsersPaginationHandler // included in this file
	}));

	/* Only one user can be selected at a time */
	$('#userField .selectWrap').hide();

	var tables = ["#usersTable", "#addUsers"];

	function unselectAll(except) {
		var tables = ["#usersTable"];
		for (x = 0; x < tables.length; x++) {
			if (except == tables[x]) {
				continue;
			}
			$(tables[x]).find("tr").removeClass("row_selected");
		}
	}

	for (x = 0; x < tables.length; x++) {
		$(tables[x]).on("mousedown", "tr", function() {
			unselectAll();
			$(this).toggleClass("row_selected");
		});
	}

	//setup user click event
	$('#usersTable tbody').on("mousedown", "tr", function() {
		var uid = $(($(this).find(":input"))[0]).attr('value');
		var sid = spaceId;
		lastSelectedUserId = uid;
		getPermissionDetails(uid, sid);
	});

	//Move select all/none buttons to the footer of the Table
	$('#userField div.selectWrap').detach().prependTo('#userField div.bottom');
	$('#addUsersField div.selectWrap')
	.detach()
	.prependTo('#addUsersField div.bottom');

	//Hook up select all/ none buttons

	$('.selectAllUsers').click(function() {
		$(this)
		.parents('.dataTables_wrapper')
		.find('tbody>tr')
		.addClass('row_selected');
	});
	$('.unselectAllUsers').click(function() {
		$(this)
		.parents('.dataTables_wrapper')
		.find('tbody>tr')
		.removeClass('row_selected');
	});

	// Set the DataTable filters to only query the server when the user finishes typing
	userTable.fnFilterOnDoneTyping();
	addUsersTable.fnFilterOnDoneTyping();

	log('all datatables initialized');
}

function redrawAllTables() {
	userTable.fnDraw();
	addUsersTable.fnDraw();
}

function isRoot(space_id) {
	return space_id == "1";
}

function isCommunity(space_id) {
	return ($.inArray(space_id.toString(), communityIdList) != -1);
}

function canChangePermissions(user_id) {
	return curIsLeader && !isRoot(spaceId) && (user_id != currentUserId);
}

function populatePermissionDetails(data, user_id) {
	if (data.perm == null) {
		showMessage("error", "permissions seem to be null", 5000);
	} else {
		$('#permCheckboxes').hide();
		$('#currentPerms').hide();

		$('#communityLeaderStatusRow').hide();
		$('#leaderStatusRow').hide();
		log("current user selected: " + (user_id == currentUserId));

		var leaderStatus = data.perm.isLeader;

		if (isCommunity(spaceId)) {
			if (canChangePermissions(user_id) && (leaderStatus != true || isAdmin())) {
				$('#permCheckboxes').show();
				if (isAdmin()) {
					$('#leaderStatusRow').show();
				} else {
					$('#communityLeaderStatusRow').show();
				}
			} else {
				$('#currentPerms').show();
			}
		} else if (canChangePermissions(user_id)) {
			$('#permCheckboxes').show();
			$('#leaderStatusRow').show();
		} else {
			$("#currentPerms").show();
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

		if (leaderStatus == true) {
			$("#uleaderStatus").attr("class", "ui-icon ui-icon-check");
			$("#leaderStatus").attr("value", "demote");
			$("#communityLeaderStatus").attr("class", "ui-icon ui-icon-check");
		} else {
			$("#uleaderStatus").attr("class", "ui-icon ui-icon-close");
			$("#leaderStatus").attr("value", "promote");
			$("#communityLeaderStatus").attr("class", "ui-icon ui-icon-close");
		}
	}

}

function checkBoxes(name, value) {
	if (value == true) {
		$("#u" + name).prop('class', 'ui-icon ui-icon-check');
		$("#" + name).prop('checked', true);
	} else {
		$("#u" + name).prop('class', 'ui-icon ui-icon-close');
		$("#" + name).prop('checked', false);
	}
}

/**
 *helper function for changePermissions
 *
 **/
function makeDemoteData() {
	return {
		addBench: true,
		addJob: true,
		addSolver: true,
		addSpace: true,
		addUser: true,
		removeBench: true,
		removeJob: true,
		removeSolver: true,
		removeSpace: true,
		removeUser: true,
		isLeader: false
	};
}

function makePromoteData() {
	return {
		addBench: true,
		addJob: true,
		addSolver: true,
		addSpace: true,
		addUser: true,
		removeBench: true,
		removeJob: true,
		removeSolver: true,
		removeSpace: true,
		removeUser: true,
		isLeader: true
	};
}

/**
 *
 * @param hier : boolean - whether or not to behave hierarchically
 **/
function changePermissions(hier, changingLeadership) {

	var url = starexecRoot + "services/space/" + spaceId + "/edit/perm/";
	if (hier) {
		url = url + "hier/";
	}
	url = url + lastSelectedUserId;

	$('#dialog-confirm-update').dialog('close');

	var data = null;

	if (!changingLeadership) {
		data = {
			addBench: $("#addBench").is(':checked'),
			addJob: $("#addJob").is(':checked'),
			addSolver: $("#addSolver").is(':checked'),
			addSpace: $("#addSpace").is(':checked'),
			addUser: $("#addUser").is(':checked'),
			removeBench: $("#removeBench").is(':checked'),
			removeJob: $("#removeJob").is(':checked'),
			removeSolver: $("#removeSolver").is(':checked'),
			removeSpace: $("#removeSpace").is(':checked'),
			removeUser: $("#removeUser").is(':checked'),
			//isLeader 	: $("#leaderStatus").is(':checked'),
			isLeader: ($("#leaderStatus").attr("value") == "demote")
		};
	} else if ($("#leaderStatus").attr("value") == "demote") {
		data = makeDemoteData();
	} else {
		data = makePromoteData();
	}

	// Pass data to server via AJAX
	$.post(
		url,
		data,
		function(returnCode) {
			var s = parseReturnCode(returnCode);
			if (s) {
				getPermissionDetails(lastSelectedUserId, spaceId);
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
	$("#savePermChanges").unbind("click");
	$("#savePermChanges").click(function() {
		$("#dialog-confirm-update-txt")
		.text("do you want the changes to be hierarchical?");

		$("#dialog-confirm-update").dialog({
			modal: true,
			width: 380,
			height: 265,
			buttons: {
				"yes": function() {changePermissions(true, false)},
				"no": function() { changePermissions(false, false)},
				"cancel": function() {
					$(this).dialog("close");
				}
			}
		});
	});

	$("#resetPermChanges").unbind("click");
	$("#resetPermChanges").click(function(e) {

		if (lastSelectedUserId == null) {
			showMessage('error', 'No user selected', 5000);
		} else {
			getPermissionDetails(lastSelectedUserId, spaceId);
		}

	});

	$("#leaderStatus").unbind('click');
	$("#leaderStatus").click(function(e) {
		$("#dialog-confirm-update-txt")
		.text("how should the leadership change take effect?");

		$("#dialog-confirm-update").dialog({
			modal: true,
			width: 380,
			height: 265,
			buttons: {
				"change only this space": function() {
					changePermissions(false, true)
				},
				"change this space's hierarchy": function() {
					changePermissions(true, true)
				},
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
			$('#dialog-confirm-update-txt')
			.text('do you want to copy the selected users to '
				+ spaceName + ' and all of its subspaces or just to ' + spaceName + '?');
			$('#dialog-confirm-update').dialog({
				modal: true,
				width: 380,
				height: 265,
				buttons: {
					'space hierarchy': function() {
						// If the user actually confirms, close the dialog right away
						$('#dialog-confirm-update').dialog('close');
						// Get the community id of the selected space and make the request to the server
						$.get(starexecRoot + 'services/space/community/' + selectedSpace,
							function(communityIdOfSelectedSpace) {
								doUserCopyPost(selectedUsersIds,
									selectedSpace,
									communityIdOfSelectedSpace,
									true,
									doUserCopyPostCB);
							});
					},
					'space': function() {
						// If the user actually confirms, close the dialog right away
						$('#dialog-confirm-update').dialog('close');
						// Get the community id of the selected space and make the request to the server
						$.get(starexecRoot + 'services/space/community/' + selectedSpace,
							function(communityIdOfSelectedSpace) {
								doUserCopyPost(selectedUsersIds,
									selectedSpace,
									communityIdOfSelectedSpace,
									false,
									doUserCopyPostCB);
							});
					},
					"cancel": function() {
						log('user canceled copy action');
						$(this).dialog("close");
					}
				}
			});
		} else {
			$('#dialog-confirm-update-txt')
			.text('select users to add to space.');
			$('#dialog-confirm-update').dialog({
				modal: true,
				width: 380,
				height: 265,
				buttons: {
					'ok': function() {
						$(this).dialog("close");
					}
				}
			});
		}
	});

	$("#makePublic").click(function() {
		var changingToPublic = !currentSpacePublic;
		var doPost = (function(hierarchy) {
			var postUrl = starexecRoot + "services/space/changePublic/" + spaceId + "/" + hierarchy + "/" + changingToPublic;
			return (function() {
				$.post(
					postUrl,
					{},
					star.reloadOnSucess.bind(this),
					"json"
				);
			});
		});
		var message =
			"Do you want to make " + spaceName + 
			(changingToPublic ? " public" : " private") +
			", or " + spaceName + " and all subspaces."
		;

		// Display the confirmation dialog
		$('#dialog-confirm-change-txt').text(message);
		$('#dialog-confirm-change').dialog({
			modal: true,
			width: 380,
			height: 265,
			buttons: {
				"space": doPost(false).bind(this),
				"space and all subspaces": doPost(true).bind(this),
				"cancel": function() {
					log('user canceled making public action');
					$(this).dialog("close");
				}
			}
		});
	});
}

function doUserCopyPost(ids, destSpace, spaceId, copyToSubspaces, callback) {
	$.post(
		starexecRoot + 'services/spaces/' + destSpace + '/add/user',
		{
			selectedIds: ids,
			fromSpace: spaceId,
			copyToSubspaces: copyToSubspaces
		},
		parseReturnCode,
		"json"
	).done(function() {
		if (callback) {
			callback();
		}
	}).fail(function() {
		showMessage('error', "Internal error copying users", 5000);
	});
}

/**
 * helper function that redraws the userTable after a copy post.
 */
function doUserCopyPostCB() {
	userTable.fnDraw();
}

function checkPermissions(jsonData, id) {
	if (jsonData.isLeader) {
		$('#loader').show();
		$.post(
			starexecRoot + "services/space/isSpacePublic/" + id,
			function(returnCode) {
				$("#makePublic").show(); //the button may be hidden if the user is coming from another space
				switch (returnCode) {
				case 0:
					currentSpacePublic = false;
					setJqueryButtonText("#makePublic", "make public");
					break;
				case 1:
					currentSpacePublic = true;
					setJqueryButtonText("#makePublic", "make private");
					break;
				}
			},
			"json"
		).error(function() {
			showMessage('error',
				"Internal error getting determining whether space is public",
				5000);
			$('#makePublic').fadeOut('fast');
		});
	} else {
		$('#makePublic').fadeOut('fast');
	}
}
