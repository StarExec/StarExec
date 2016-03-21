var solverId;
$(document).ready(function(){
	initUI();
	attachButtonActions();
	
	$("#dialog-confirm-copy").css("display", "none");


	$('img').click(function(event){
		popUp($(this).attr('enlarge'));
	});
	
	$('.confirmation').on('click', function () {
        return confirm('Are you sure?');
    });
});

function initUI(){
	$('#downLink3').button({
		icons: {
			secondary: "ui-icon-arrowthick-1-s"
    }});


	$('#srcLink').button({
		icons: {
			secondary: "ui-icon-arrowthick-1-s"
    }});

	registerAnonymousLinkButtonEventHandler();

	$('#fieldSites').expandable(true);
	$('#actions').expandable(true);
	
	
	// Setup datatable of configurations
	$('#tblSolverConfig').dataTable( {
        "sDom": getDataTablesDom(),        
        "bPaginate": true,        
        "bSort": true        
    });

	$('#downBuildInfo').button({
		icons: {
			secondary: "ui-icon-newwin"
    }});
	// Setup button icons
	
	$('#uploadConfig, #uploadConfigMargin').button({
		icons: {
			primary: "ui-icon-arrowthick-1-n"
		}
    });
	
	$('#uploadPicture').button({
		icons: {
			primary: "ui-icon-gear"
		}
    });

	//Warn if there was some error during upload (no configs, running test job failed).
	var msg = getParameterByName("msg");
	if (stringExists(msg)) {
		$('#dialog-warning-txt').text(msg);
		
		$('#dialog-warning').dialog({
			modal: true,
			width: 380,
			height: 165,
			buttons: {
				'OK': function() {
					$('#dialog-warning').dialog('close');
				}
			}
		});
	}


	//Display feedback if the solver is being built on starexec.
	var msg = getParameterByName("buildmsg");
	if (stringExists(msg)) {
		$('#dialog-building-job').dialog({
			modal: true,
			width: 380,
			height: 165,
			buttons: {
				'OK': function() {
					$('#dialog-building-job').dialog('close');
				}
			}
		});
	}
}

function attachButtonActions() {
    $("#srcLink").click(function(){
        var token=Math.floor(Math.random()*100000000);
        window.location.href = starexecRoot+"secure/download?token=" + token + "&type=solverSrc&id="+$("#solverId").attr("value");
        destroyOnReturn(token);})
	$("#downLink3").click(function(){
		$('#dialog-confirm-copy-txt').text('How would you like to download the solver?');		
		$('#dialog-confirm-copy').dialog({
			modal: true,
			width: 380,
			height: 165,
			buttons: {
				'default': function() {
					log("default selected");
					$(this).dialog("close");
					createDialog("Processing your download request, please wait. This will take some time for large solvers.");
					var token=Math.floor(Math.random()*100000000);
					if ( $('#isAnonymousPage').attr('value') === 'true' ) {
						var anonId = getParameterByName('anonId');
						window.location.href = starexecRoot+"secure/download?token=" + token + "&type=solver&anonId="+anonId;
					} else {
						window.location.href = starexecRoot+"secure/download?token=" + token + "&type=solver&id="+$("#solverId").attr("value");
					}
					destroyOnReturn(token);
				},
				'Re-upload': function() {
					log("upload selected");
					$(this).dialog("close");
					createDialog("Processing your download request, please wait. This will take some time for large solvers.");
					token=Math.floor(Math.random()*100000000);
					if ( $('#isAnonymousPage').attr('value') === 'true' ) {
						var anonId = getParameterByName('anonId');
						window.location.href = starexecRoot+"secure/download?token=" + token + "&reupload=true&type=solver&anonId=" + anonId;
					} else {
						window.location.href = starexecRoot+"secure/download?token=" + token + "&reupload=true&type=solver&id="+$("#solverId").attr("value");
					}
					destroyOnReturn(token);
				},
				"cancel": function() {
					log('user canceled copy action');
					$(this).dialog("close");
				}
			}
		});
	});
}

function popUp(uri) {
	imageDialog = $("#popDialog");
	imageTag = $("#popImage");
	
	imageTag.attr('src', uri);

	imageTag.load(function(){
		$('#popDialog').dialog({
			dialogClass: "popup",
			modal: true,
			resizable: false,
			draggable: false,
			height: 'auto',
			width: 'auto'
		});
	});  
}

function registerAnonymousLinkButtonEventHandler() {
	'use strict';
	$('#anonymousLink').unbind('click');
	$('#anonymousLink').click( function() {
		$('#dialog-confirm-anonymous-link').text( "Do you want the solver's name to be hidden on the linked page?" );
		$('#dialog-confirm-anonymous-link').dialog({
			modal: true,
			width: 600,
			height: 200,
			buttons: {
				'yes': function() { 
					$(this).dialog('close');
					makeAnonymousLinkPost( 'solver', $('#solverId').attr('value'), 'all');
				},
				'no': function() {
					$(this).dialog('close');
					makeAnonymousLinkPost( 'solver', $('#solverId').attr('value'), 'none');
				}
			}
		});	
	});
}

/*
function makeAnonymousLinkPost( hidePrimitiveName ) {
	'use strict';
	$.post(
		starexecRoot + 'services/anonymousLink/solver/' + $('#solverId').attr('value') + '/' + hidePrimitiveName,
		'',
		function( returnCode ) {
			log( 'Anonymous Link Return Code: ' + returnCode );
			if ( returnCode.success ) {
				$('#dialog-show-anonymous-link').html('<a href="'+returnCode.message+'">'+returnCode.message+'</a>');
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
*/


