$(document).ready(function(){
	$('#pairTbl').dataTable( {
        "sDom": 'rt<"bottom"flpi><"clear">'
    });
	
	$('#downLink').button({
		icons: {
			secondary: "ui-icon-arrowthick-1-s"
    }});
});