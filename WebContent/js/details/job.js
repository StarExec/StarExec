$(document).ready(function(){
	$('#pairTbl').dataTable( {
        "sDom": 'rt<"bottom"flpi><"clear">'
    });
	
	// Set up row click to send to pair details page
	$("#pairTbl").delegate("tr", "click", function(){
		var pairId = $(this).find('input').val();
		window.location.assign("/starexec/secure/details/pair.jsp?id=" + pairId);
	});
});