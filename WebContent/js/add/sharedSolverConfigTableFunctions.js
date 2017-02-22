
function setupSolverConfigDataTable() {
	'use strict';
	$('#tblSolverConfig').dataTable( {
        "sDom": 'rt<"bottom"f><"clear">',        
        "bPaginate": false,        
        "bSort": true        
    });
}

function registerSelectAllSolversEventHandler() {
	'use strict';
    $('.selectAllSolvers').click(function() {
		// Select all default configurations when select all solvers is clicked.
		$('.config').prop('checked', true);
		// Give every row the row_selected class so they are highlighted.
    	$(this).parents('.dataTables_wrapper').find('tbody>tr').addClass('row_selected');
    });
}

function registerSelectNoneSolversEventHandler() {
	'use strict';
    $('.selectNoneSolvers').click(function() {
    	$(this).parents('.dataTables_wrapper').find('tbody>tr').removeClass('row_selected');
    	$(this).parents('.dataTables_wrapper').find('input[type=checkbox]').prop('checked', false);
    }); 
}

function registerSelectAllConfigsEventHandler() {
	'use strict';
	$('.selectAllConfigs').click(function() {
		$(this).parent().siblings('.config').prop('checked', true);
	});
}

function registerSelectNoneConfigsEventHandler() {
	'use strict';
	$('.selectNoneConfigs').click(function(e) {
		$(this).parent().siblings('.config').prop('checked', false);

		var numCheck = $(this).closest('tr').find('input[type=checkbox]:checked').length;

		if (numCheck == 0) {
			$(this).closest('tr').removeClass('row_selected');
		}

		// Don't allow a click event to fire for the ancestor elements
		e.stopPropagation();
	});
}

function registerSolverConfigTableRowSelectionEventHandler() {
	'use strict';
    // Enable row selection
	$("#tblSolverConfig").on( "click", "tr", function() {

	    var numCheck = $(this).find('input[type=checkbox]:checked').length;

	    if(!$(this).hasClass("row_selected")) {

			$(this).addClass('row_selected');
			
			if (numCheck != 1) {
				// Only check the default checkbox if the user clicked on the row,
				// and not another checkbox.
				$(this).find('.default').prop('checked', true);

				var numberOfConfigsNamedDefault = $(this).find('.default').length;

				log("numberOfConfigsNamedDefault: " + numberOfConfigsNamedDefault);
				if ( numberOfConfigsNamedDefault > 0 ) {
					// Add the number of boxes that were checked to numCheck.
					numCheck += numberOfConfigsNamedDefault;
				} else if (numberOfConfigsNamedDefault === 0) {
					// If their are no default configs and there is only one config then select that one
					// by default.
					var numberOfConfigsForSelectedSolver = $(this).find('input[type=checkbox]').length;
					if (numberOfConfigsForSelectedSolver === 1) {
						$(this).find('input[type=checkbox]').prop('checked', true);
						numCheck += 1;
					}
				}
				log('Total number of configs for selected solver: ' + numberOfConfigsForSelectedSolver);
			}

    		//$(this).find('div>input[type=checkbox]').prop('checked', true);
			
	    }
		if (numCheck == 0) {
			$(this).removeClass("row_selected");
	    };
	    
	});
}
