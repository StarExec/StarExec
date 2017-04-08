"use strict";

$(document).ready(function() {
	$('.copyToStarDev').button({
		icons: {
			primary: "ui-icon-arrowthick-1-n"
		}
	}).click(function() {
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
					log('Id was: '+primId);
					var type = $('.thisPrimitivesType').attr('value');
					log('Type was: '+type);
					var url = starexecRoot+'services/copy-to-stardev/'+instance+'/'+username+'/'+password+'/'+type+'/'+primId+'/'+spaceId;
					log('Url was: '+url);
					$.get(
						url,
						{},
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
