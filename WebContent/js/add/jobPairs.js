
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

		$.ajax({
			type: 'POST',
			url: confirmationUrl,
			data: $('#addJobPairsForm').serialize(),
			success: function(data) {
				if ( data.success ) {
					$('#dialog-confirm-add-delete-txt').text( data.pairsToBeDeleted + ' job pairs will be deleted.\n'+
						data.pairsToBeAdded + ' job pairs will be added.\nWould you like to continue?' );		
					$('#dialog-confirm-add-delete').dialog({
						modal: true,
						width: 500,
						height: 300,
						buttons: {
							'continue': function() {
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
