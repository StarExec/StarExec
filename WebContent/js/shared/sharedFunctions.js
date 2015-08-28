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
