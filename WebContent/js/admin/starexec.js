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
								switch (returnCode) {
									case 0:
										window.location = starexecRoot+'secure/explore/spaces.jsp';
										break;
									case 1:
										showMessage('error', "job was not deleted; please try again", 5000);
										break;
									case 2:
										showMessage('error', "only the owner of this job can delete it", 5000);
										break;
									default:
										showMessage('error', "invalid parameters", 5000);
										break;
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


