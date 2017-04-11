"use strict";

$(document).ready(function() {
	$('.copyToStarDev').button({
		icons: {
			primary: "ui-icon-arrowthick-1-n"
		}
	}).click(function() {
		var type = $('.thisPrimitivesType').attr('value');
		log('Type was: '+type);
		if ( type === $('.processorType').attr('value') ) {
			$('.stardevSpaceIdText').text('stardev community id');
		} else {
			$('.stardevSpaceIdText').text('stardev space id');
		}
		$('.dialog-copy-to-stardev').dialog({
			modal: true,
			width: 600,
			height: 400,
			buttons: {
				'submit': function() {
					$(this).dialog('close');
					var instance = $('.instanceName').val();
					var username = $('.stardevUsername').val();
					var password = $('.stardevPassword').val();
					var spaceId = $('.stardevSpaceId').val();
					var primId = $('.thisPrimitivesId').attr('value');
					var procType = $('.procType').attr('value');
					log('Id was: '+primId);
					var url = starexecRoot+'services/copy-to-stardev/'+instance+'/'+type+'/'+primId+'/'+spaceId;
					log('Url was: '+url);
					$.post(
						url,
						{
							username: username,
							password: password,
							procType: procType
						},
						function(data) {
							parseReturnCode(data, true);
							log(data);
						},
						'json'
					);
				},
				'cancel': function() {
					$(this).dialog('close');
				}
			}
		});
	});
});
