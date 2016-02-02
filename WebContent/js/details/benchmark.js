var bid;

$(document).ready(function(){
	bid = getParameterByName('id');	
	$('#anonymousLink').hide();
	$('#dialog-anonymous-link').hide();
	$('#fieldType').expandable(true);
	$('#fieldAttributes').expandable(true);
	$('#fieldDepends').expandable(true);
	$( "#dialog-warning").hide();
	
	$('#fieldContents').expandable(true);
	$('#actions').expandable(true);

	

	registerDownloadLinkButtonEventHandler();
	registerAnonymousLinkButtonEventHandler();

	
	
});

function registerDownloadLinkButtonEventHandler() {
	$('#downLink').unbind("click");
	$('#downLink').click(function() {
		createDialog("Processing your download request, please wait. This will take some time for large benchmarks.");
		var token=Math.floor(Math.random()*100000000);
		$('#downLink').attr('href', starexecRoot+"secure/download?token=" +token+ "&type=bench&id="+$("#benchId").attr("value"));
		destroyOnReturn(token);
	});
}

function registerAnonymousLinkButtonEventHandler() {
	'use strict';
	$('#anonymousLink').unbind('click');
	$('#anonymousLink').click( function() {
		$('#dialog-confirm-anonymous-link').text( "Do you want the benchmark's name to be hidden on the linked page?" );
		$('#dialog-confirm-anonymous-link').dialog({
			modal: true,
			width: 600,
			height: 200,
			buttons: {
				'yes': function() { 
					$(this).dialog('close');
					makeAnonymousLinkPost( true );
				},
				'no': function() {
					$(this).dialog('close');
					makeAnonymousLinkPost( false );
				}
			}
		});	
	});
}

function makeAnonymousLinkPost( hidePrimitiveName ) {
	'use strict';
	$.post(
		starexecRoot + 'services/anonymousLink/bench/' + $('#benchId').attr('value') + '/' + hidePrimitiveName,
		'',
		function( returnCode ) {
			log( 'Anonymous Link Return Code: ' + returnCode );
			if ( returnCode.success ) {
				$('#dialog-show-anonymous-link').text( 'anonymous link for this benchmark:\n' + returnCode.message );
				$('#dialog-show-anonymous-link').dialog({
					width: 750,
					height: 200,
				});	
			} else {
				parseReturnCode( returnCode );
			}
		},
		'json'
	);
}


