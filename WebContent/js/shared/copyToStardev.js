"use strict";



$(document).ready(function() {
	log('root: '+starexecRoot);
	function makeCopyPost(instance, type, primId, username, password, spaceId, procId, callback) {
		var url = starexecRoot+'services/copy-to-stardev/'+instance+'/'+type+'/'+primId;
		$.post(
			url,
			{
				username: username,
				password: password,
				spaceId: spaceId,
				procId: procId
			},
			callback,
			'json'
		).fail(function() {
			var sc = {
				success: false,
				message: 'Failed to contact server, app name is likely invalid.'
			};
			parseReturnCode(sc);
		});
	}
	$('.uploadProcessorWithBenchmarkCheckbox').click(function() {
		var boxChecked = $('.uploadProcessorWithBenchmarkCheckbox').is(':checked');
		if (boxChecked) {
			$('.stardevProcIdDiv').hide();
		} else {
			$('.stardevProcIdDiv').show();
		}
	});

	$('.copyToStarDev').button({
		icons: {
			primary: "ui-icon-arrowthick-1-n"
		}
	}).click(function() {
		var type = $('.thisPrimitivesType').attr('value');
		log('Type was: '+type);
		var processorType = $('.processorPrimitiveType').attr('value');
		var benchmarkType = $('.benchmarkPrimitiveType').attr('value');
		var solverType = $('.solverPrimitiveType').attr('value');
		if ( type ===  processorType) {
			$('.stardevSpaceIdText').text('stardev community id');
			$('.instanceNameText').removeClass('stardevInstanceNameText');
			$('.instanceNameText').addClass('stardevInstanceNameTextProcessor');
			$('.passwordText').removeClass('stardevPasswordText');
			$('.passwordText').addClass('stardevPasswordTextProcessor');
			$('.usernameText').removeClass('stardevUsernameText');
			$('.usernameText').addClass('stardevUsernameTextProcessor');
		} else {
			$('.stardevSpaceIdText').text('stardev space id');
		}
		if ( type === benchmarkType ) {
			$('.spaceIdText').removeClass('stardevSpaceIdText');
			$('.spaceIdText').addClass('stardevSpaceIdTextBenchmark');
			$('.uploadProcessorWithBenchmarkDiv').show();
		}
		if ( type === solverType ) {
			$('.spaceIdText').removeClass('stardevSpaceIdText');
			$('.spaceIdText').addClass('stardevSpaceIdTextSolver');
		}
		$('.dialog-copy-to-stardev').dialog({
			modal: true,
			width: 480,
			height: 330,
			buttons: {
				'submit': function() {
					$(this).dialog('close');
					var instance = $('.instanceName').val();
					var username = $('.stardevUsername').val();
					var password = $('.stardevPassword').val();
					var spaceId = $('.stardevSpaceId').val();
					var primId = $('.thisPrimitivesId').attr('value');
					var copyWithProcessor = $('.uploadProcessorWithBenchmarkCheckbox').is(':checked');
					log('Id was: '+primId);
					if (copyWithProcessor && type === benchmarkType) {
						log('uploading benchmark with processor')
						var benchProcessorId = $('.benchProcessorId').attr('value');
						// Get the community ID of the space we'll be uploading the benchmark to
						var stardevUrl= "https://stardev.cs.uiowa.edu/"+instance+"/"
						$.get(
							stardevUrl+'services/space/community/'+spaceId,
							{},
							function(communityId) {
								log('Community id was: '+communityId);
								// Make sure it was a success.
								if (communityId > -1) {
									// Upload the processor to the community that contains the space the benchmark will be uploaded to
									makeCopyPost(instance, processorType, benchProcessorId, username, password, communityId, 1, function(procSc) {
										if (procSc && procSc.success) {
											var newProcId = procSc.statusCode;
											log('New proc ID was: '+newProcId);
											makeCopyPost(instance, benchmarkType, primId, username, password, spaceId, newProcId, function (benchSc) {
												parseReturnCode(benchSc);
											});
										} else {
											log('Failed to upload processor.');
										}
									});
								}
							},
							'json'
						);
					} else {
						log('copying to stardev')
						// by default use the no type proc ID, this won't matter for anything other than benchmark uploads.
						var noTypeProcId = 1;
						makeCopyPost(instance, type, primId, username, password, spaceId, noTypeProcId, function(statusCode) {
							if (statusCode) {
								parseReturnCode(statusCode);
							} else {
								log('statusCode was undefined');
							}
						});
					}
				},
				'cancel': function() {
					$(this).dialog('close');
				}
			}
		});
	});
});
