$(document).ready(function(){

	initUI();
	
});

function initUI(){
	$('#dialog-confirm-restart').hide();

	$("#restartStarExec").button({
		icons: {
			primary: "ui-icon-power"
		}
    });
	
	$("#manageCache").button( {
		icons: {
			primary: "ui-icon-document"
		}
	});
	
	$("#manageLogging").button( {
		icons: {
			primary: "ui-icon-document"
		}
	});
	
		
	$("#restartStarExec").click(function(){
		$('#dialog-confirm-restart-txt').text('are you sure you want to restart StarExec?');
		
		$('#dialog-confirm-restart').dialog({
			modal: true,
			width: 380,
			height: 165,
			buttons: {
				'OK': function() {
					log('user confirmed starexec restart.');
					$('#dialog-confirm-restart').dialog('close');
					$.post(
							starexecRoot+"services/restart/starexec/",
							function(returnCode) {
								s=parseReturnCode(returnCode);
								if (s) {
									window.location = starexecRoot+'secure/explore/spaces.jsp';
								}
							},
							"json"
					);
				},
				"cancel": function() {
					log('user canceled StarExec restart');
					$(this).dialog("close");
				}
			}
		});
	});	
}


