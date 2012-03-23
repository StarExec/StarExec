$(document).ready(function(){
	// Builds the user interface
	initUI();
	
});

function initUI(){
	$('#fieldSites').expandable(true);		
	
	// Setup datatable of configurations
	$('#tblSolverConfig').dataTable( {
        "sDom": 'rt<"bottom"flpi><"clear">',        
        "bPaginate": true,        
        "bSort": true        
    });
	
	// Setup button icons
	$('#uploadConfig, #uploadConfigMargin').button({
		icons: {
			primary: "ui-icon-arrowthick-1-n"
		}
    });
}