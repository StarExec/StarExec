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
	
	// Hide JQuery UI confirm/delete dialog
	$( "#dialog-confirm-delete" ).hide();
	
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
						"/starexec/services/delete/configuration/" + getParameterByName("id"),
						function(returnCode) {
							switch (returnCode) {
								case 0:
									// Deletion was successful; return user to parent solver's 'details' page
									window.location = "/starexec/secure/details/solver.jsp?id=" + $('#solverId').val();
									break;
								case 2:
									showMessage('error', "you do not have sufficient privileges to remove configurations from this solver", 5000);
									break;
								default:
									showMessage('error', "an error occurred while processing your request; please try again", 5000);
									break;
							}
						},
						"json"
					).error(function(){
						showMessage('error',"Internal error deleting configuration",5000);
					});		
				},
				"cancel": function() {
					$(this).dialog("close");
				}
			}		
		});				
	});
}
