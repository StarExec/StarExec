$(document).ready(function(){
	$('#pairTbl').dataTable( {
        "sDom": getDataTablesDom()
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
								parseReturnCode(returnCode);
								
							},
							"json"
					);
	});
	
	$('#fieldDetails').expandable(false);
	$('#fieldStats').expandable(true);
	$('#fieldAttrs').expandable(true);
	$("#fieldActions").expandable(true);
	$('#fieldOutput').expandable(true);		
	$('#stageStats').expandable(true);
	
	$('#fieldLog').expandable(true);
	
	// Hide loading images by default
	$('legend img').hide();
});
