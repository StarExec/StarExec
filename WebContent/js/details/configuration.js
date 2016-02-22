$(document).ready(function(){
	// Setup the UI buttons/actions
	initUI();
});


/**
 * Sets up the UI buttons/actions 
 */
function initUI(){
	initButtons();
}


/**
 * Sets up button icons and actions
 */
function initButtons(){
	$('#deleteConfig').button({
		icons: {
			secondary: "ui-icon-minus"
		}
	});
	
	// Handles the deletion of this configuration file
	$("#deleteConfig").click(function(){
		$('#dialog-confirm-delete-txt').text('are you sure you want to delete this configuration?');
		
		// Display the confirmation dialog
		$('#dialog-confirm-delete').dialog({
			modal: true,
			buttons: {
				'yes': function() {
					// If the user actually confirms, close the dialog right away
					$('#dialog-confirm-delete').dialog('close');
					
					$.post(  
						starexecRoot+"services/delete/configuration",
						{selectedIds: [getParameterByName("id")]},
						function(returnCode) {
							s=parseReturnCode(returnCode);
							if (s) {
								window.location = starexecRoot+"secure/details/solver.jsp?id=" + $('#solverId').val();

							}
							
						},
						"json"
					).error(function(){
						showMessage('error',"Internal error deleting configuration.",5000);
					});		
				},
				"cancel": function() {
					$(this).dialog("close");
				}
			}		
		});				
	});
}
