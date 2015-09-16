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

/*
// Shared by explore/communities and admin/community
// Returns the new data table
function initCommunityRequestsTable(communityId) {
	return $('#commRequests').dataTable( {
		"sDom"			: 'rt<"bottom"flpi><"clear">',
		"iDisplayStart"	: 0,
		"iDisplayLength": defaultPageSize,
		"bServerSide"	: true,
		"sAjaxSource"	: starexecRoot+"services/",
		"sServerMethod" : 'POST',
		"fnServerData"	: fnPaginationHandler 
	});
}

function fnPaginationHandler(sSource, aoData, fnCallback) {
		// Request the next page of primitives from the server via AJAX
		$.post(  
				sSource + "community/pending/requests/",
				aoData,
				function(nextDataTablePage){
					s=parseReturnCode(nextDataTablePage);
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
*/
