

$(document).ready(function(){
	initUI();
	
	// Remove all unselected rows from the DOM before submitting
	$('#addForm').submit(function() {
		$('#tblCommunities tbody').children('tr').not('.row_selected').find('input').remove();
	});
		
});

function initUI(){
	
	$("#btnDone").button({
		icons: {
			primary: "ui-icon-locked"
		}
	});
	
	// Set up datatables
	$('#tblCommunities').dataTable( {
        "sDom": 'rt<"bottom"f><"clear">',        
        "bPaginate": false,        
        "bSort": true        
    });
	
	$("#tblCommunities").on( "click", "tr", function() {
		$(this).toggleClass("row_selected");
	});

}

