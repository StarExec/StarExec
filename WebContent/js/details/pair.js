$(document).ready(function(){
	$('#pairTbl').dataTable( {
        "sDom": 'rt<"bottom"flpi><"clear">'
    });
	
	$('#detailTable').dataTable( {
        "sDom": 'rt<"bottom"f><"clear">',
        "aaSorting": [],
        "bPaginate": false,        
        "bSort": true        
    });
	
	$('#pairAttrs').dataTable( {
        "sDom": 'rt<"bottom"f><"clear">',        
        "bPaginate": false
    });
	
	$('#downLink').button({
		icons: {
			secondary: "ui-icon-arrowthick-1-s"
    }});
	$("#rerunPair").button( {
		icons: {
			primary: "ui-icon-arrowrefresh-1-e"
	}});
	
	$("#rerunPair").click(function() {
					$.post(
							starexecRoot+"services/jobs/pairs/rerun/" + $("#pairId").attr("value"),
							function(returnCode) {
								switch (returnCode) {
									case 0:
										showMessage("success", "pair successfully submitted to be rerun",5000)
										break;
									case 1:
										showMessage('error', "There was an internal error rerunning the pair.", 5000);
										break;
									case 2:
										showMessage('error', "Only the owner of this job can rerun pairs", 5000);
										break;
									default:
										showMessage('error', "Invalid parameters.", 5000);
										break;
								}
							},
							"json"
					);
	});
	
	$('#fieldDetails').expandable(false);
	$('#fieldStats').expandable(true);
	$('#fieldAttrs').expandable(true);
	$("#fieldActions").expandable(true);
	$('#fieldOutput').expandable(true);		
	
	$('#fieldLog').expandable(true);
	
	// Hide loading images by default
	$('legend img').hide();
});