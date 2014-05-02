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
	
	$('#fieldDetails').expandable(false);
	$('#fieldStats').expandable(true);
	$('#fieldAttrs').expandable(true);
	$("#fieldActions").expandable(true);
	$('#fieldOutput').expandable(true);		
	
	$('#fieldLog').expandable(true);
	
	// Hide loading images by default
	$('legend img').hide();
});