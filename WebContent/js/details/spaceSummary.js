var summaryTable;
var pairTable;
$(document).ready(function(){
	initUI();
	initDataTables();
});

/**
 * Initializes the user-interface
 */
function initUI(){
	
	$("#goToParent").button({
		icons: {
			primary: "ui-icon-arrowthick-1-n"
		}
    });
	
	
	//set all fieldsets as expandable
	$('fieldset').expandable(true);

	}

/**
 * Initializes the DataTable objects
 */
function initDataTables(){
	//summary table
	summaryTable=$('#solveTbl').dataTable( {
        "sDom"			: 'rt<"bottom"flpi><"clear">',
        "iDisplayStart"	: 0,
        "iDisplayLength": 10,
        "bSort": true,
        "bPaginate": true
    });
	
	$(".subspaceTable").dataTable( {
		"sDom"			: 'rt<"bottom"flpi><"clear">',
        "iDisplayStart"	: 0,
        "iDisplayLength": 10,
        "bSort": true,
        "bPaginate": true
	});
	
	
}
