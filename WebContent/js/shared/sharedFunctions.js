/*-------------------------------BEGIN SHARED BETWEEN admin/cluster AND explore/cluster------------------------------------*/

// Adds some text that says how many nodes are in each queue next to each
// queue's node list.
// Author: Albert Giegerich
function addNodeCountsToTree() {
	'use strict';
	// Iterate through all the queue nodes.
	$('#exploreList').children('ul').children('li').each(function() {
		var queueId = $(this).attr('id');
		// Set that to this so we can use it in the callback.
		var that = this;
		$.get(
			starexecRoot + 'services/cluster/queues/details/nodeCount/' + queueId,
			'',
			function(numberOfNodes) {
				log('numberOfNodes: ' + numberOfNodes);
				// Insert the node count span inside the node list.
				$(that)
				.prepend('<span class="nodeCount">(' + numberOfNodes + ')</span>');
			},
			'json'
		);
	});
}

/*-------------------------------END SHARED BETWEEN admin/cluster AND explore/cluster--------------------------------------*/

/*-------------------------------BEGIN SHARED BETWEEN admin/community AND explore/communities------------------------------*/

function setupHandlersForCommunityRequestAcceptDeclineButtons() {
	'use strict';
	// Delegate the event handling to the commRequests table because the table is paginated.
	$('#commRequests').on('click', '.declineRequestButton', function() {
		handleRequest($(this).attr('data-code'), false);
	});

	$('#commRequests').on('click', '.acceptRequestButton', function() {
		handleRequest($(this).attr('data-code'), true);
	});
}

// Shared by explore/communities and admin/community
// Returns the new data table
function initCommunityRequestsTable(
	tableSelector, getAllCommunities, communityId) {
	"use strict";

	var getUrl = starexecRoot + "services/community/pending/requests/";

	if (!getAllCommunities) {
		getUrl += communityId;
	}

	var paginator = function(sSource, aoData, fnCallback) {
		$.get(
			sSource,
			aoData,
			function(nextDataTablePage) {
				var s = parseReturnCode(nextDataTablePage, false);
				if (s) {
					// Update the number displayed in this DataTable's fieldset
					$('#communityExpd')
					.children('span:first-child')
					.text(nextDataTablePage.iTotalRecords);
					// Replace the current page with the newly received page
					fnCallback(nextDataTablePage);
				}
			},
			"json"
		);
	};

	return $(tableSelector).dataTable(new star.DataTableConfig({
		"bServerSide": true,
		"sAjaxSource": getUrl,
		"fnServerData": paginator,
		"columns": [
			null,
			null,
			null,
			{"searchable": false, "orderable": false},
			{"searchable": false, "orderable": false}
		]
	}));
}

function handleRequest(code, isApproved) {
	'use strict';
	var constants = getCommunityRequestConstants();
	log(constants);

	var approveOrDeclineCode = (isApproved ? constants.approveCommunityRequest : constants.declineCommunityRequest);

	log('approveOrDeclineCode: ' + approveOrDeclineCode);

	var requestData = {};
	requestData[constants.leaderResponseParameter] = approveOrDeclineCode;
	requestData[constants.emailCodeParameter] = code;
	requestData[constants.sentFromCommunityPage] = true;

	$.get(
		starexecRoot + 'public/verification/email',
		requestData,
		function(data) {
			parseReturnCode(data);
			$("#commRequests").DataTable().ajax.reload();
		},
		'json'
	);
}

function getCommunityRequestConstants() {
	'use strict';
	return {
		"emailCodeParameter":      $('#emailCode'     ).attr('value'),
		"leaderResponseParameter": $('#leaderResponse').attr('value'),
		"approveCommunityRequest": $('#approveRequest').attr('value'),
		"declineCommunityRequest": $('#declineRequest').attr('value'),
		"sentFromCommunityPage":   $('#communityPage' ).attr('value')
	};
}

/*-------------------------------END SHARED BETWEEN admin/community AND explore/communities------------------------------*/

/*-------------------------------BEGIN SHARED BETWEEN explore/spaces AND edit/spacePermissions---------------------------*/

/**
 * Populates the space details of the currently selected space and queries
 * for the primitives of any fieldsets that are expanded
 * @param jsonData the basic information about the currently selected space
 */
function populateSpaceDetails(jsonData, id) {
	// If the space is null, the user can see the space but is not a member
	if (jsonData.space == null) {
		// Go ahead and show the space's name
		$('.spaceName').fadeOut('fast', function() {
			var spaceName = $('.jstree-clicked').text();
			$('.spaceName').text(spaceName).fadeIn('fast');
			document.title = spaceName + " - StarExec";
		});

		// Show a message why they can't see the space's details
		$('#spaceDesc').fadeOut('fast', function() {
			$('#spaceDesc')
			.text(
				'you cannot view this space\'s details since you are not a member. you can see this space exists because you are a member of one of its descendants.')
			.fadeIn('fast');
		});
		$('#spaceID').fadeOut('fast');
		// Hide all the info table fieldsets
		$('#detailPanel fieldset').fadeOut('fast');
		$('#loader').hide();

		// Stop executing the rest of this function
		return;
	} else {
		// Or else the user can see the space, make sure the info table fieldsets are visible
		$('#userField').show(); // this fieldset is present only on the spacePermissions page
		$('#detailPanel fieldset').show();
	}

	// Update the selected space id
	spaceId = jsonData.space.id;
	spaceName = jsonData.space.name;

	// Populate space defaults
	$('.spaceName').fadeOut('fast', function() {
		$('.spaceName').text(spaceName).fadeIn('fast');
		document.title = spaceName + " - StarExec";
	});
	$('#spaceDesc').fadeOut('fast', function() {
		$('#spaceDesc').text(jsonData.space.description).fadeIn('fast');
	});
	$('#spaceID').fadeOut('fast', function() {
		$('#spaceID').text("id = " + spaceId).fadeIn('fast');
	});

	// on the space permissions page, we display when the user is the space leader
	if (spaceId != "1") {
		curIsLeader = jsonData.perm.isLeader;
	} else {
		curIsLeader = false;
	}
	$('#spaceLeader').fadeOut('fast', function() {
		if (curIsLeader) {
			$('#spaceLeader').text("leader of current space").fadeIn('fast');
		}
	});
	/*
	 * Issue a redraw to all DataTable objects to force them to requery for
	 * the newly selected space's primitives.  This will effectively clear
	 * all entries in every table, update every table with the current space's
	 * primitives, and update the number displayed in every table's fieldset.
	 */
	redrawAllTables();

	// Check the new permissions for the loaded space. Varies between spaces.js and spacePermissions.js
	checkPermissions(jsonData.perm, id);

	// Done loading, hide the loader
	$('#loader').hide();

	log('Client side UI updated with details for ' + spaceName);
}

/**
 * Populates the space details panel with the basic information about the space
 * (e.g. the name, description) but does not query for details about primitives
 */
function getSpaceDetails(id) {
	$('#loader').show();
	$.post(
		starexecRoot + "services/space/" + id,
		function(data) {
			log('AJAX response received for details of space ' + id);
			populateSpaceDetails(data, id);
		},
		"json"
	).error(function() {
		showMessage('error', "Internal error getting space details", 5000);
	});
}

/*-------------------------------END SHARED BETWEEN explore/spaces AND edit/spacePermissions-----------------------------*/

/*-------------------------------BEGIN SHARED BETWEEN admin/permissions AND edit/spacePermissions------------------------*/

function getPermissionDetails(user_id, space_id) {
	$.get(
		starexecRoot + "services/permissions/details/" + user_id + "/" + space_id,
		function(data) {
			populatePermissionDetails(data, user_id);
		},
		"json"
	).error(function() {
		showMessage('error',
			"Internal error getting selectd user's permission details",
			5000);
	});
}

/*-------------------------------END SHARED BETWEEN admin/permissions AND edit/spacePermissions-------------------------*/

