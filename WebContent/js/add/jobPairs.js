
$(document).ready(function(){
	'use strict';
	initUI();

});

function initUI() {
	'use strict';
    $('#btnDone').button({
		icons: {
			secondary: 'ui-icon-check'
		}
	});
	setupSolverConfigDataTable();

	$('#addJobPairsForm').submit( function(e) {
		var confirmationUrl = starexecRoot+'services/jobs/addJobPairs/confirmation';
		createDialog("Getting number of pairs to be added/deleted. Please wait. (May take a minute or two for large jobs.)");
		$.ajax({
			type: 'POST',
			url: confirmationUrl,
			data: $('#addJobPairsForm').serialize(),
			success: function(data) {
				if ( data.success ) {
					destroyDialog();
					$('#dialog-confirm-add-delete-txt').text( data.pairsToBeDeleted + ' job pairs will be deleted.\n'+
						data.pairsToBeAdded + ' job pairs will be added.\nWould you like to continue?' );		

					$('#dialog-confirm-add-delete').dialog({
						modal: true,
						width: 500,
						height: 300,
						buttons: {
							'continue': function() {
								createDialog("Adding/deleting job pairs. Please wait. (May take a minute or two for large jobs.)");
								$('#addJobPairsForm').unbind('submit');
								$('#addJobPairsForm').submit();
							},
							'cancel': function() {
								$(this).dialog('close');
							}
						}
					});
				} else {
					alert ( data.message );
				}
			}
		});

		e.preventDefault();
	});
	
	$('.addToAllCheckbox').click(function() {
		'use strict';
		console.log('addToAllCheckbox clicked.');

		if ( $(this).prop('checked') ) {
			// Check the other box if the user clicks it.
			$(this).siblings('.addToPairedCheckbox').first().prop('checked', false);
		} else {
			// Don't allow the user to uncheck a box.
			$(this).prop('checked', true);
		}
	});

	$('.addToPairedCheckbox').click(function() {
		'use strict';
		
		console.log('addToPairedCheckbox clicked.');
		
		if ( $(this).prop('checked') ) {
			// Check the other box if the user clicks it.
			$(this).siblings('.addToAllCheckbox').first().prop('checked', false);
		} else {
			// Don't allow the user to uncheck a box.
			$(this).prop('checked', true);
		}
	});


    $('.selectAllSolvers').click(function() {
    	$('.config').prop('checked', true);
    }); 
    $('.selectNoneSolvers').click(function() {
    	$('.config').prop('checked', false);
    }); 
	registerSelectAllConfigsEventHandler();
	registerSelectNoneConfigsEventHandler();
	registerSolverConfigTableRowSelectionEventHandler();
}
