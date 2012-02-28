$(document).ready(function(){
	$('#pairTbl').dataTable( {
        "sDom": 'rt<"bottom"flpi><"clear">'
    });
	
	$('#detailTbl').dataTable( {
        "sDom": 'rt<"bottom"f><"clear">',
        "aaSorting": [],
        "bPaginate": false,        
        "bSort": true        
    });	
	
	// Set up row click to send to pair details page
	$("#pairTbl").delegate("tr", "click", function(){
		var pairId = $(this).find('input').val();
		window.location.assign("/starexec/secure/details/pair.jsp?id=" + pairId);
	});
	
	$('fieldset').expandable(false);
});