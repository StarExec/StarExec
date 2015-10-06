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
			starexecRoot+'services/cluster/queues/details/nodeCount/'+queueId,
			'',
			function(numberOfNodes) {
				log('numberOfNodes: '+numberOfNodes);
				// Insert the node count span inside the node list.
				$(that).prepend('<span class="nodeCount">('+numberOfNodes+')</span>');
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
		log('Decline button clicked');
		handleRequest($(this).attr('data-code'), false);
	});

	$('#commRequests').on('click', '.acceptRequestButton', function() {
		log('Decline button clicked');
		handleRequest($(this).attr('data-code'), true);
	});
}

// Shared by explore/communities and admin/community
// Returns the new data table
function initCommunityRequestsTable(tableSelector, getAllCommunities, communityId) {
	'use strict';
	return $(tableSelector).dataTable( {
		"sDom"			: 'rt<"bottom"flpi><"clear">',
		"iDisplayStart"	: 0,
		"iDisplayLength": defaultPageSize,
		"bServerSide"	: true,
		"sAjaxSource"	: starexecRoot+"services/",
		"sServerMethod" : 'POST',
		"fnServerData"	: buildPaginationHandler(getAllCommunities, communityId)
	});
}

function handleRequest(code, isApproved) {
	'use strict';
	var constants = getCommunityRequestConstants();
	log('leaderResponseParameter: '+constants.leaderResponseParameter+'\nemailCodeParameter: '+constants.emailCodeParameter
			+'\napproveCommunityRequest: '+constants.approveCommunityRequest+'\ndeclineCommunityRequest: '+constants.declineCommunityRequest);

	var approveOrDeclineCode = (isApproved ? constants.approveCommunityRequest : constants.declineCommunityRequest);

	log('approveOrDeclineCode: '+approveOrDeclineCode);

	var requestData = {};
	requestData[constants.leaderResponseParameter] = approveOrDeclineCode;
	requestData[constants.emailCodeParameter] = code;
	requestData[constants.sentFromCommunityPage] = true;

	$.get(
		starexecRoot+'public/verification/email',
		requestData,
		function(data) {
			parseReturnCode(data);
			setTimeout(function() {
				location.reload();
			}, 1000);
		},
		'json'
	);
}

function getCommunityRequestConstants() {
	'use strict';

	log('emailCode: '+$('#emailCode').attr('value'));

	var communityRequestConstants = {};
	communityRequestConstants.emailCodeParameter = $('#emailCode').attr('value');
	communityRequestConstants.leaderResponseParameter = $('#leaderResponse').attr('value');
	communityRequestConstants.approveCommunityRequest = $('#approveRequest').attr('value');
	communityRequestConstants.declineCommunityRequest = $('#declineRequest').attr('value');
	communityRequestConstants.sentFromCommunityPage = $('#communityPage').attr('value');

	return communityRequestConstants;

}

function buildPaginationHandler(getAllCommunities, communityId) {
	'use strict';

	var communityRequestPaginationHandler = function(sSource, aoData, fnCallback) {
		var getUrl = 'community/pending/requests/';
		if (!getAllCommunities) {
			// append the community id if we're not getting all community ids.
			getUrl += communityId;
		}
		// Request the next page of primitives from the server via AJAX
		$.get(  
				sSource + getUrl,
				aoData,
				function(nextDataTablePage){
					s=parseReturnCode(nextDataTablePage, false);
					if (s) {

						// Update the number displayed in this DataTable's fieldset
						$('#communityExpd').children('span:first-child').text(nextDataTablePage.iTotalRecords);
					
					// Replace the current page with the newly received page
					fnCallback(nextDataTablePage);
					}

				},  
				"json"
		);
	};
	return communityRequestPaginationHandler;
}

/*-------------------------------END SHARED BETWEEN admin/community AND explore/communities------------------------------*/
