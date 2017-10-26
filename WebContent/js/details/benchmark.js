var bid;

$(document).ready(function() {
	bid = getParameterByName('id');
	$('#fieldType').expandable(true);
	$('#fieldAttributes').expandable(true);
	$('#fieldDepends').expandable(true);

	$('#fieldContents').expandable(true);
	$('#actions').expandable(true);

	PR.prettyPrint();

	registerDownloadLinkButtonEventHandler();
	registerAnonymousLinkButtonEventHandler();
});

function registerDownloadLinkButtonEventHandler() {
	$('#downLink').unbind("click");
	$('#downLink').click(function() {
		createDialog(
			"Processing your download request, please wait. This will take some time for large benchmarks.");
		var token = Math.floor(Math.random() * 100000000);
		log("isAnonymousPage: " + $('#isAnonymousPage').attr('value'));
		if ($('#isAnonymousPage').attr('value') === 'true') {
			var anonId = getParameterByName('anonId');
			log('anonId: ' + anonId);
			$('#downLink')
			.attr('href', starexecRoot + "secure/download?token=" + token + "&type=bench&anonId=" + anonId);
		} else {
			$('#downLink')
			.attr('href', starexecRoot + "secure/download?token=" + token + "&type=bench&id=" + $(
				"#benchId").attr("value"));
		}
		destroyOnReturn(token);
	});
}

function registerAnonymousLinkButtonEventHandler() {
	'use strict';
	$('#anonymousLink').unbind('click');
	$('#anonymousLink').click(function() {
		$('#dialog-confirm-anonymous-link')
		.text(
			"Do you want the benchmark's name to be hidden on the linked page?");
		$('#dialog-confirm-anonymous-link').dialog({
			modal: true,
			width: 600,
			height: 200,
			buttons: {
				'yes': function() {
					$(this).dialog('close');
					makeAnonymousLinkPost('bench',
						$('#benchId').attr('value'),
						'all');
				},
				'no': function() {
					$(this).dialog('close');
					makeAnonymousLinkPost('bench',
						$('#benchId').attr('value'),
						'none');
				}
			}
		});
	});
}
